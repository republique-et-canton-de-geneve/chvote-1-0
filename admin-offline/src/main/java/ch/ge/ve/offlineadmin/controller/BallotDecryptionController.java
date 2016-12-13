package ch.ge.ve.offlineadmin.controller;

/*-
 * #%L
 * Admin offline
 * %%
 * Copyright (C) 2015 - 2016 République et Canton de Genève
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import ch.ge.ve.commons.crypto.ballot.BallotCipherService;
import ch.ge.ve.commons.crypto.ballot.EncryptedBallotAndWrappedKey;
import ch.ge.ve.commons.crypto.exceptions.PrivateKeyPasswordMismatchException;
import ch.ge.ve.commons.fileutils.OutputFilesPattern;
import ch.ge.ve.commons.fileutils.StreamHasher;
import ch.ge.ve.commons.properties.PropertyConfigurationException;
import ch.ge.ve.commons.properties.PropertyConfigurationService;
import ch.ge.ve.commons.streamutils.SafeObjectReader;
import ch.ge.ve.offlineadmin.exception.MissingKeyFilesException;
import ch.ge.ve.offlineadmin.exception.ProcessInterruptedException;
import ch.ge.ve.offlineadmin.services.BallotCipherServiceFactory;
import ch.ge.ve.offlineadmin.services.DecryptionService;
import ch.ge.ve.offlineadmin.util.FileUtils;
import ch.ge.ve.offlineadmin.util.LogLevel;
import ch.ge.ve.offlineadmin.util.PropertyConfigurationServiceFactory;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import javax.crypto.SealedObject;
import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static ch.ge.ve.commons.crypto.SensitiveDataCryptoUtilsConfigurationDefaultImpl.COMMON_CRYPTO_STREAM_MAX_BYTES;
import static ch.ge.ve.offlineadmin.util.SecurityConstants.BALLOTS_FILENAME;
import static ch.ge.ve.offlineadmin.util.SecurityConstants.STREAM_MAX_OBJECTS;

/**
 * This controller handles the management of the display of the ballot decryption tab, including the decryption process.
 */
public class BallotDecryptionController extends InterruptibleProcessController {
    private static final Logger LOGGER = Logger.getLogger(BallotDecryptionController.class);
    private final Executor exec = Executors.newCachedThreadPool(runnable -> {
        Thread t = new Thread(runnable);
        t.setDaemon(true);
        return t;
    });

    @FXML
    private ResourceBundle resources;

    @FXML
    private BorderPane mainPane;

    /*
     * By appending <tt>Controller</tt> to the <tt>fx:id</tt> of the node, its controller is resolved automatically
     */
    @FXML
    private ConsoleOutputControl consoleOutputController;

    private FileUtils fileUtils;
    private StreamHasher streamHasher;
    private OutputFilesPattern outputFilesPattern;
    private PropertyConfigurationService propertyConfigurationService;
    private PeriodFormatter periodFormatter;
    private PasswordDialogController passwordDialogController;
    private BallotCipherServiceFactory ballotCipherServiceFactory;

    @FXML
    public void initialize() throws IOException {
        fileUtils = new FileUtils(resources);
        propertyConfigurationService = new PropertyConfigurationServiceFactory().propertyConfigurationService();
        streamHasher = new StreamHasher(propertyConfigurationService);
        outputFilesPattern = new OutputFilesPattern();

        periodFormatter = new PeriodFormatterBuilder()
                .printZeroAlways()
                .appendHours().appendSuffix(" h ")
                .appendMinutes().appendSuffix(" min ")
                .appendSeconds().appendSuffix(" sec ")
                .toFormatter();

        passwordDialogController = new PasswordDialogController(resources, consoleOutputController);
        ballotCipherServiceFactory = new BallotCipherServiceFactory(propertyConfigurationService);
    }

    /**
     * Perform the decryption of the ballots
     */
    public void startDecryption() {
        try {
            File keyDirectory = fileUtils.promptKeyDirectory();
            if (keyDirectory == null) {
                throw new ProcessInterruptedException("action cancelled");
            }

            BallotCipherService ballotCipherService = ballotCipherServiceFactory.decryptionBallotCipherService(keyDirectory);

            StringProperty password1 = new SimpleStringProperty();
            StringProperty password2 = new SimpleStringProperty();
            passwordDialogController.promptForPasswords(password1, password2, false);

            decryptBallots(ballotCipherService, password1, password2);

        } catch (MissingKeyFilesException e) {
            consoleOutputController.logOnScreen(resources.getString("keys_not_found_in_directory"), LogLevel.WARN);
            LOGGER.warn(PROCESS_INTERRUPTED_MESSAGE, e);
        } catch (PrivateKeyPasswordMismatchException e) {
            consoleOutputController.logOnScreen(resources.getString("key_password_mismatch"), LogLevel.WARN);
            LOGGER.warn("key password mismatch", e);
        } catch (ProcessInterruptedException e) {
            consoleOutputController.logOnScreen(resources.getString("ballot_decryption.process_interrupted"), LogLevel.WARN);
            LOGGER.warn(PROCESS_INTERRUPTED_MESSAGE, e);
        }
    }

    private void decryptBallots(BallotCipherService ballotCipherService, StringProperty password1, StringProperty password2) throws PrivateKeyPasswordMismatchException, ProcessInterruptedException {
        ballotCipherService.loadBallotKeyCipherPrivateKey(password1.getValue() + password2.getValue());

        File encryptedBallotsFile = fileUtils.promptEncryptedBallotsFile();
        if (encryptedBallotsFile == null) {
            throw new ProcessInterruptedException("action cancelled");
        }

        logEncryptedBallotsFileHash(encryptedBallotsFile);

        unserializeAndDecryptEncryptedBallots(encryptedBallotsFile, ballotCipherService);
    }

    private void logEncryptedBallotsFileHash(File encryptedBallotsFile) throws ProcessInterruptedException {
        try (InputStream encBallotsInputStream = Files.newInputStream(encryptedBallotsFile.toPath(), StandardOpenOption.READ)) {
            byte[] encBallotsFileHash = streamHasher.threadSafeComputeHash(encBallotsInputStream);
            consoleOutputController.logOnScreen(
                    String.format(resources.getString("ballot_decryption.enc_ballots_file_hash"),
                            DatatypeConverter.printHexBinary(encBallotsFileHash)));
        } catch (IOException e) {
            throw new ProcessInterruptedException(resources.getString("ballot_decryption.exception_occurred"), e);
        }
    }

    private void unserializeAndDecryptEncryptedBallots(File encryptedBallotsFile, BallotCipherService ballotCipherService) throws ProcessInterruptedException {
        // Using a task here, so as to perform decryption without blocking the UI.
        final long maxObjects;
        try {
            maxObjects = propertyConfigurationService.getConfigValueAsLong(STREAM_MAX_OBJECTS);
        } catch (PropertyConfigurationException e) {
            throw new ProcessInterruptedException(String.format(resources.getString("ballot_decryption.undefined_property"), STREAM_MAX_OBJECTS), e);
        }

        final long maxBytes;
        try {
            maxBytes = propertyConfigurationService.getConfigValueAsLong(COMMON_CRYPTO_STREAM_MAX_BYTES);
        } catch (PropertyConfigurationException e) {
            throw new ProcessInterruptedException(String.format(resources.getString("ballot_decryption.undefined_property"), COMMON_CRYPTO_STREAM_MAX_BYTES), e);
        }

        Task<List<EncryptedBallotAndWrappedKey>> unserializeEncryptedBallotsTask = new UnserializeEncryptedBallotsTask(
                encryptedBallotsFile,
                maxObjects,
                maxBytes);

        Stopwatch fileOpening = Stopwatch.createStarted();

        // Handle success
        unserializeEncryptedBallotsTask.setOnSucceeded(event -> {
            List<EncryptedBallotAndWrappedKey> encryptedBallots = unserializeEncryptedBallotsTask.getValue();
            fileOpening.stop();
            consoleOutputController.logOnScreen(
                    String.format(resources.getString("ballot_decryption.enc_ballots_loaded"),
                            formatElapsedTime(fileOpening)));
            consoleOutputController.logOnScreen(
                    String.format(resources.getString("ballot_decryption.number_of_ballots"),
                            encryptedBallots.size())
            );
            consoleOutputController.setStepCount(encryptedBallots.size() / DecryptionService.STEP_SIZE);

            // Once the ballots are deserialized, they can be decrypted
            performBallotDecryption(ballotCipherService, encryptedBallots);
        });

        // Handle failure
        unserializeEncryptedBallotsTask.exceptionProperty().addListener((observable, oldValue, newException) -> {
            if (newException != null) {
                LOGGER.error(resources.getString("ballot_decryption.exception_occurred"), newException);
                consoleOutputController.logOnScreen(resources.getString("ballot_decryption.exception_occurred"), LogLevel.ERROR);
            }
        });

        // Start execution
        exec.execute(unserializeEncryptedBallotsTask);
    }

    private void performBallotDecryption(BallotCipherService ballotCipherService, List<EncryptedBallotAndWrappedKey> encryptedBallots) {
        DecryptionService decryptionService = new DecryptionService(ballotCipherService, consoleOutputController);
        // Using a task here, so as to perform decryption without blocking the UI.
        Task<List<String>> ballotDecryptionTask = new BallotDecryptionTask(decryptionService, encryptedBallots);

        final Stopwatch ballotDecryption = Stopwatch.createStarted();

        // Handle success
        ballotDecryptionTask.setOnSucceeded(event -> {
            ballotDecryption.stop();
            consoleOutputController.logOnScreen(
                    String.format(resources.getString("ballot_decryption.decryption_finished"),
                            formatElapsedTime(ballotDecryption)));

            consoleOutputController.progressMessage(String.format(resources.getString("ballot_decryption.invalid_ballots_text"), decryptionService.getInvalidCounter()));      //"Count of invalid ballots : " +
            saveCleartextBallots(ballotDecryptionTask.getValue());
            consoleOutputController.incrementStepCount();
        });

        // Handle failure
        ballotDecryptionTask.exceptionProperty().addListener((observable, oldValue, newException) -> {
            if (newException != null) {
                LOGGER.error(resources.getString("ballot_decryption.exception_occurred"), newException);
                consoleOutputController.logOnScreen(resources.getString("ballot_decryption.exception_occurred"), LogLevel.ERROR);
            }
        });

        // Start execution
        exec.execute(ballotDecryptionTask);
    }

    private void saveCleartextBallots(List<String> decryptedBallots) {
        try {
            File selectedDirectory = selectDirectory();
            final String ballotsFilename = propertyConfigurationService.getConfigValue(BALLOTS_FILENAME);

            Path cleartextBallotsFilename = Paths.get(selectedDirectory.toString(), outputFilesPattern.injectParams(ballotsFilename, DateTime.now()));
            Files.write(cleartextBallotsFilename, decryptedBallots);

            byte[] cleartextBallotsFileHash;
            try (InputStream cleartextBallotsInputStream = Files.newInputStream(cleartextBallotsFilename, StandardOpenOption.READ)) {
                cleartextBallotsFileHash = streamHasher.threadSafeComputeHash(cleartextBallotsInputStream);
            }
            consoleOutputController.logOnScreen(
                    String.format(resources.getString("ballot_decryption.output_file_hash"),
                            DatatypeConverter.printHexBinary(cleartextBallotsFileHash)));

            consoleOutputController.logOnScreen(String.format(resources.getString("ballot_decryption.file_saved"), cleartextBallotsFilename));
        } catch (ProcessInterruptedException | IOException | PropertyConfigurationException e) {
            consoleOutputController.logOnScreen(resources.getString("ballot_decryption.process_interrupted"), LogLevel.WARN);
            LOGGER.warn(PROCESS_INTERRUPTED_MESSAGE, e);
        }
    }

    private File selectDirectory() throws ProcessInterruptedException {
        String title = resources.getString("ballot_decryption.dir_chooser.title");
        File selectedDirectory = fileUtils.getDirectory(title, fileUtils.getUserHome());
        if (selectedDirectory == null) {
            throw new ProcessInterruptedException("Directory selection cancelled");
        }
        return selectedDirectory;
    }

    private String formatElapsedTime(Stopwatch stopwatch) {
        Period period = Period
                .seconds((int) stopwatch.elapsed(TimeUnit.SECONDS))
                // Need to normalize here so that 179 seconds will be seen as 2 minutes and 59 seconds
                .normalizedStandard();
        return periodFormatter.print(period);
    }

    @Override
    protected ResourceBundle getResourceBundle() {
        return resources;
    }

    void setPasswordDialogController(PasswordDialogController passwordDialogController) {
        this.passwordDialogController = passwordDialogController;
    }

    void setBallotCipherServiceFactory(BallotCipherServiceFactory ballotCipherServiceFactory) {
        this.ballotCipherServiceFactory = ballotCipherServiceFactory;
    }

    void setFileUtils(FileUtils fileUtils) {
        this.fileUtils = fileUtils;
    }

    void setStreamHasher(StreamHasher streamHasher) {
        this.streamHasher = streamHasher;
    }

    private static class UnserializeEncryptedBallotsTask extends Task<List<EncryptedBallotAndWrappedKey>> {
        private final File encryptedBallotsFile;
        private final long maxObjects;
        private final long maxBytes;

        public UnserializeEncryptedBallotsTask(File encryptedBallotsFile, long maxObjects, long maxBytes) {
            this.encryptedBallotsFile = encryptedBallotsFile;
            this.maxObjects = maxObjects;
            this.maxBytes = maxBytes;
        }

        @Override
        protected List<EncryptedBallotAndWrappedKey> call() throws Exception {
            // Need to create the stream here, so it'll be available to the executor thread
            try (InputStream encBallotsInputStream = Files.newInputStream(encryptedBallotsFile.toPath(), StandardOpenOption.READ)) {
                return (List<EncryptedBallotAndWrappedKey>) SafeObjectReader.safeReadObject(
                        ArrayList.class,
                        Arrays.asList(EncryptedBallotAndWrappedKey.class, SealedObject.class),
                        maxObjects,
                        maxBytes,
                        encBallotsInputStream);
            }
        }
    }

    private class BallotDecryptionTask extends Task<List<String>> {
        private final DecryptionService decryptionService;
        private final List<EncryptedBallotAndWrappedKey> encryptedBallots;

        public BallotDecryptionTask(DecryptionService decryptionService, List<EncryptedBallotAndWrappedKey> encryptedBallots) {
            this.decryptionService = decryptionService;
            this.encryptedBallots = encryptedBallots;
        }

        @Override
        protected List<String> call() throws Exception {
            return decryptionService.decrypt(ImmutableList.copyOf(encryptedBallots));
        }
    }
}
