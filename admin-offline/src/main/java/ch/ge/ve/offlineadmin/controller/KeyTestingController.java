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

import ch.ge.ve.commons.crypto.ballot.AuthenticatedBallot;
import ch.ge.ve.commons.crypto.ballot.BallotCipherService;
import ch.ge.ve.commons.crypto.ballot.EncryptedBallotAndWrappedKey;
import ch.ge.ve.commons.crypto.exceptions.AuthenticationTagMismatchException;
import ch.ge.ve.commons.crypto.exceptions.CryptoConfigurationRuntimeException;
import ch.ge.ve.commons.crypto.exceptions.PrivateKeyPasswordMismatchException;
import ch.ge.ve.commons.crypto.utils.SecureRandomFactory;
import ch.ge.ve.commons.fileutils.OutputFilesPattern;
import ch.ge.ve.commons.fileutils.StreamHasher;
import ch.ge.ve.commons.properties.PropertyConfigurationException;
import ch.ge.ve.commons.properties.PropertyConfigurationService;
import ch.ge.ve.offlineadmin.exception.KeyProvisioningRuntimeException;
import ch.ge.ve.offlineadmin.exception.MissingKeyFilesException;
import ch.ge.ve.offlineadmin.exception.ProcessInterruptedException;
import ch.ge.ve.offlineadmin.services.BallotCipherServiceFactory;
import ch.ge.ve.offlineadmin.util.FileUtils;
import ch.ge.ve.offlineadmin.util.LogLevel;
import ch.ge.ve.offlineadmin.util.PropertyConfigurationServiceFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;
import org.apache.log4j.Logger;

import javax.xml.bind.DatatypeConverter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static ch.ge.ve.offlineadmin.util.SecurityConstants.CERT_PUBLIC_KEY_FILENAME_PATTERN;

/**
 * This controller manages the key testing tab
 */
public class KeyTestingController extends InterruptibleProcessController {
    private static final Logger LOGGER = Logger.getLogger(KeyTestingController.class);

    private final ObservableList<String> plainTexts = FXCollections.observableArrayList();
    private final ObservableList<AuthenticatedBallot> cipherTexts = FXCollections.observableArrayList();
    private final ObservableList<String> decryptedTexts = FXCollections.observableArrayList();
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz;: ";
    private static final int LENGTH = 256;
    private final SecureRandom random = SecureRandomFactory.createPRNG();
    @FXML
    private ResourceBundle resources;
    @FXML
    private GridPane mainGrid;
    /*
     * By appending <tt>Controller</tt> to the <tt>fx:id</tt> of the node, its controller is resolved automatically
     */
    @FXML
    private ConsoleOutputControl consoleOutputController;
    @FXML
    private ListView<String> plainTextList;
    @FXML
    private ListView<AuthenticatedBallot> cipherTextList;
    @FXML
    private ListView<String> decryptedTextList;
    @FXML
    private Button encryptButton;
    @FXML
    private Button decryptButton;
    private FileUtils fileUtils;
    private StreamHasher streamHasher;
    private PropertyConfigurationService propertyConfigurationService;
    private BallotCipherServiceFactory ballotCipherServiceFactory;
    private PasswordDialogController passwordDialogController;

    @FXML
    private void initialize() throws IOException {
        propertyConfigurationService = new PropertyConfigurationServiceFactory().propertyConfigurationService();
        streamHasher = new StreamHasher(propertyConfigurationService);
        ballotCipherServiceFactory = new BallotCipherServiceFactory(propertyConfigurationService);
        passwordDialogController = new PasswordDialogController(resources, consoleOutputController);

        plainTextList.setItems(plainTexts);
        cipherTextList.setItems(cipherTexts);
        customizeCipherTextCellFactory();
        decryptedTextList.setItems(decryptedTexts);

        initializeBindings();

        plainTexts.add("");
        plainTexts.addAll(Stream.generate(this::createRandomString).limit(4).collect(Collectors.toList()));
        fileUtils = new FileUtils(resources);
    }

    private String createRandomString() {
        IntStream intStream = random.ints(LENGTH, 0, ALPHABET.length());
        Stream<Character> characterStream = intStream.boxed().map(ALPHABET::charAt);
        return characterStream
                .map(Object::toString).reduce((acc, e) -> acc + e)
                .orElseThrow(() -> new CryptoConfigurationRuntimeException("Alphabet or random size misconfiguration"));
    }

    private void customizeCipherTextCellFactory() {
        cipherTextList.setCellFactory(new Callback<ListView<AuthenticatedBallot>, ListCell<AuthenticatedBallot>>() {
            @Override
            public ListCell<AuthenticatedBallot> call(ListView<AuthenticatedBallot> param) {
                return new ListCell<AuthenticatedBallot>() {
                    @Override
                    protected void updateItem(AuthenticatedBallot item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null) {
                            setText(DatatypeConverter.printHexBinary(item.getAuthenticatedEncryptedBallot()));
                        }
                    }
                };
            }
        });
    }

    private void initializeBindings() {
        encryptButton.setDisable(true);
        BooleanBinding plainTextsEmpty = Bindings.createBooleanBinding(plainTexts::isEmpty, plainTexts);
        plainTextsEmpty.addListener(
                (observable, oldValue, isPlainTextListEmpty) ->
                        encryptButton.setDisable(isPlainTextListEmpty)
        );

        decryptButton.setDisable(true);
        cipherTexts.addListener((ListChangeListener<AuthenticatedBallot>) c -> decryptButton.setDisable(c.getList().isEmpty()));
    }

    private void bindScrollBars() {
        ScrollBar plainTextScrollBar = plainTextList.lookupAll(".scroll-bar").stream().map(e -> (ScrollBar) e).filter(e -> e.getOrientation().equals(Orientation.HORIZONTAL)).findFirst().orElse(null);
        ScrollBar decryptedTextScrollBar = decryptedTextList.lookupAll(".scroll-bar").stream().map(e -> (ScrollBar) e).filter(e -> e.getOrientation().equals(Orientation.HORIZONTAL)).findFirst().orElse(null);

        if (plainTextScrollBar != null && decryptedTextScrollBar != null) {
            plainTextScrollBar.valueProperty().bindBidirectional(decryptedTextScrollBar.valueProperty());
        } else {
            LOGGER.error("couldn't find scrollbars");
        }
    }

    @Override
    protected ResourceBundle getResourceBundle() {
        return resources;
    }

    /**
     * Perform a test encryption using the keys chosen by the user, and store the encrypted values in a local buffer.
     */
    @FXML
    public void testEncryption() {
        cipherTexts.clear();

        File selectedFile = fileUtils.promptKeyDirectory();

        if (selectedFile != null) {
            try {
                consoleOutputController.logOnScreen(resources.getString("key_testing.encryption.start"));

                BallotCipherService ballotCipherService = ballotCipherServiceFactory.encryptionBallotCipherService(selectedFile);

                logPublicKeyHash(selectedFile);

                int index = 1;
                for (String plainText : plainTexts) {
                    cipherTexts.add(ballotCipherService.encryptBallotThenWrapForAuthentication(plainText, index++));
                }

                consoleOutputController.logOnScreen(resources.getString("key_testing.encryption.end"));
            } catch (MissingKeyFilesException e) {
                consoleOutputController.logOnScreen(resources.getString("keys_not_found_in_directory"), LogLevel.WARN);
                LOGGER.warn(PROCESS_INTERRUPTED_MESSAGE, e);
            } catch (IOException | PropertyConfigurationException e) {
                handleInterruption(new ProcessInterruptedException(resources.getString("key_testing.exception_occurred"), e));
                LOGGER.error(PROCESS_INTERRUPTED_MESSAGE, e);
            }
        }
    }

    /**
     * Perform a decryption test, using the keys chosen by the user, and the cipher texts stored in the temporary buffer
     * during a previous execution of the encryption test
     */
    @FXML
    public void testDecryption() {
        consoleOutputController.logOnScreen(resources.getString("key_testing.decryption.start"));
        decryptedTexts.clear();

        File selectedFile = fileUtils.promptKeyDirectory();

        if (selectedFile != null) {
            try {
                BallotCipherService ballotCipherService = ballotCipherServiceFactory.decryptionBallotCipherService(selectedFile);

                logPublicKeyHash(selectedFile);

                List<EncryptedBallotAndWrappedKey> encryptedBallotAndWrappedKeys = new ArrayList<>();
                for (AuthenticatedBallot cipherText : cipherTexts) {
                    encryptedBallotAndWrappedKeys.add(ballotCipherService.verifyAuthenticationThenUnwrap(cipherText));
                }

                StringProperty password1 = new SimpleStringProperty();
                StringProperty password2 = new SimpleStringProperty();
                passwordDialogController.promptForPasswords(password1, password2, false);
                ballotCipherService.loadBallotKeyCipherPrivateKey(password1.getValue() + password2.getValue());

                for (EncryptedBallotAndWrappedKey encryptedBallotAndWrappedKey : encryptedBallotAndWrappedKeys) {
                    decryptedTexts.add(ballotCipherService.decryptBallot(encryptedBallotAndWrappedKey));
                }

                bindScrollBars();

                consoleOutputController.logOnScreen(resources.getString("key_testing.decryption.end"));
            } catch (MissingKeyFilesException e) {
                consoleOutputController.logOnScreen(resources.getString("keys_not_found_in_directory"), LogLevel.WARN);
                LOGGER.warn(PROCESS_INTERRUPTED_MESSAGE, e);
            } catch (PrivateKeyPasswordMismatchException e) {
                consoleOutputController.logOnScreen(resources.getString("key_password_mismatch"), LogLevel.WARN);
                LOGGER.warn("key password mismatch", e);
            } catch (ProcessInterruptedException e) {
                consoleOutputController.logOnScreen(resources.getString("key_testing.process_interrupted"), LogLevel.WARN);
                LOGGER.warn(PROCESS_INTERRUPTED_MESSAGE, e);
            } catch (PropertyConfigurationException | IOException | AuthenticationTagMismatchException e) {
                handleInterruption(new ProcessInterruptedException(resources.getString("key_testing.exception_occurred"), e));
                LOGGER.error(PROCESS_INTERRUPTED_MESSAGE, e);
            }
        }
    }

    private void logPublicKeyHash(File selectedDirectory) throws PropertyConfigurationException, IOException {
        Pattern pubKeyPattern = Pattern.compile(propertyConfigurationService.getConfigValue(CERT_PUBLIC_KEY_FILENAME_PATTERN));
        final Optional<Path> pubKey = new OutputFilesPattern().findFirstFileByPattern(pubKeyPattern, selectedDirectory.toPath());

        if (!pubKey.isPresent()) {
            throw new KeyProvisioningRuntimeException("Public key was not found in directory:" + selectedDirectory.toPath());
        } else {

            // needs to be SHA-1, since windows only displays the sha1 hash when viewing a certificate's details
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                throw new CryptoConfigurationRuntimeException("Cannot instantiate message digest for file hashing", e);
            }

            try (FileInputStream fileInputStream = new FileInputStream(pubKey.get().toFile())) {
                byte[] hash = streamHasher.computeHash(fileInputStream, digest);
                String hashString = DatatypeConverter.printHexBinary(hash);
                consoleOutputController.logOnScreen(String.format(resources.getString("key_testing.public_key_hash"), hashString));
            }
        }
    }

    void setFileUtils(FileUtils fileUtils) {
        this.fileUtils = fileUtils;
    }

    void setStreamHasher(StreamHasher streamHasher) {
        this.streamHasher = streamHasher;
    }

    void setBallotCipherServiceFactory(BallotCipherServiceFactory ballotCipherServiceFactory) {
        this.ballotCipherServiceFactory = ballotCipherServiceFactory;
    }

    void setPasswordDialogController(PasswordDialogController passwordDialogController) {
        this.passwordDialogController = passwordDialogController;
    }

    void setCipherTexts(List<AuthenticatedBallot> authenticatedBallots) {
        cipherTexts.clear();
        cipherTexts.addAll(authenticatedBallots);
    }
}
