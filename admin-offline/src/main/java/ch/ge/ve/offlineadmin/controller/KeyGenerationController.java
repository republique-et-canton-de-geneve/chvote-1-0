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

import ch.ge.ve.commons.fileutils.OutputFilesPattern;
import ch.ge.ve.commons.fileutils.StreamHasher;
import ch.ge.ve.commons.properties.PropertyConfigurationException;
import ch.ge.ve.commons.properties.PropertyConfigurationService;
import ch.ge.ve.offlineadmin.exception.KeyGenerationRuntimeException;
import ch.ge.ve.offlineadmin.exception.ProcessInterruptedException;
import ch.ge.ve.offlineadmin.services.KeyGenerator;
import ch.ge.ve.offlineadmin.util.FileUtils;
import ch.ge.ve.offlineadmin.util.LogLevel;
import ch.ge.ve.offlineadmin.util.PropertyConfigurationServiceFactory;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import javax.crypto.SecretKey;
import javax.xml.bind.DatatypeConverter;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ResourceBundle;

import static ch.ge.ve.offlineadmin.util.SecurityConstants.*;

/**
 * This controller manages the display of the KeyGeneration tab
 */
public class KeyGenerationController extends InterruptibleProcessController {
    private static final Logger LOGGER = Logger.getLogger(KeyGenerationController.class);
    @FXML
    private ResourceBundle resources;

    /*
     * By appending <tt>Controller</tt> to the <tt>fx:id</tt> of the node, its controller is resolved automatically
     */
    @FXML
    private ConsoleOutputControl consoleOutputController;

    private StringProperty password1 = new SimpleStringProperty();
    private StringProperty password2 = new SimpleStringProperty();
    private String keySavedMessage;
    private FileUtils fileUtils;
    private PropertyConfigurationService propertyConfigurationService;
    private StreamHasher streamHasher;
    private PasswordDialogController passwordDialogController;
    private KeyGenerator keyGenerator;

    @FXML
    private void initialize() {
        fileUtils = new FileUtils(resources);
        propertyConfigurationService = new PropertyConfigurationServiceFactory().propertyConfigurationService();
        streamHasher = new StreamHasher(propertyConfigurationService);
        passwordDialogController = new PasswordDialogController(resources, consoleOutputController);
        keyGenerator = new KeyGenerator(propertyConfigurationService);
    }

    /**
     * Generate the encryption keys. The steps are
     * <ol>
     * <li>password definition for group 1</li>
     * <li>password definition for group 2</li>
     * <li>generation of a symmetric key (for integrity verification)</li>
     * <li>generation of an asymmetric key pair</li>
     * <li>wrapping of the public key in a certificate signed with the private key</li>
     * <li>wrapping of the private key in a password protected keystore (using the concatenation of both passwords)</li>
     * <li>saving of the keys at a user defined location</li>
     * </ol>
     */
    public void startKeyGeneration() {
        consoleOutputController.logOnScreen(resources.getString("key_generation.started"));

        try {
            passwordDialogController.promptForPasswords(password1, password2, true);

            SecretKey secretKey = keyGenerator.generateSecretKey();
            KeyPair keyPair = keyGenerator.generateKeyPair();

            X509Certificate certificate = keyGenerator.generateCertificate(keyPair);
            char[] password = (password1.getValue() + password2.getValue()).toCharArray();
            KeyStore store = keyGenerator.createKeyStore(keyPair.getPrivate(), certificate, password);

            consoleOutputController.logOnScreen(resources.getString("key_generation.keys_generated"));

            saveKeys(secretKey, certificate, store, password);
        } catch (ProcessInterruptedException e) {
            consoleOutputController.logOnScreen(resources.getString("key_generation.process_interrupted"), LogLevel.WARN);
            LOGGER.warn(PROCESS_INTERRUPTED_MESSAGE, e);
        } catch (PropertyConfigurationException e) {
            handleInterruption(new ProcessInterruptedException(resources.getString("key_generation.exception_occurred"), e));
            LOGGER.error(e);
        }
    }

    @Override
    protected ResourceBundle getResourceBundle() {
        return resources;
    }

    private void saveKeys(SecretKey secretKey, X509Certificate certificate, KeyStore store, char[] password) throws ProcessInterruptedException, PropertyConfigurationException {
        File selectedDirectory = fileUtils.getDirectory(resources.getString("key_generation.dir_chooser.title"), fileUtils.getUserHome());

        if (selectedDirectory == null) {
            throw new ProcessInterruptedException("Directory selection cancelled");
        }

        OutputFilesPattern outputFilesPattern = new OutputFilesPattern();

        final DateTime now = DateTime.now();

        final String keysFolder = outputFilesPattern.injectParams(propertyConfigurationService.getConfigValue("keys_folder"), now);
        Path keyFolderPath = Paths.get(selectedDirectory.toString(), keysFolder);

        if (!keyFolderPath.toFile().mkdir()) {
            consoleOutputController.logOnScreen(String.format(resources.getString("already.existing_dir"), keyFolderPath), LogLevel.WARN);
        }

        String ctrlP12Filename = keyFolderPath + "/" + outputFilesPattern.injectParams(propertyConfigurationService.getConfigValue(CERT_PRIVATE_KEY_FILENAME), now);
        String ctrlDerFilename = keyFolderPath + "/" + outputFilesPattern.injectParams(propertyConfigurationService.getConfigValue(CERT_PUBLIC_KEY_FILENAME), now);
        String integrityKeyFilename = keyFolderPath + "/" + outputFilesPattern.injectParams(propertyConfigurationService.getConfigValue(INTEGRITY_KEY_FILENAME), now);

        keySavedMessage = resources.getString("key_generation.key_saved");

        storePrivateKey(store, password, ctrlP12Filename);
        storeCertificate(certificate, ctrlDerFilename);
        computePublicKeyHash(propertyConfigurationService, ctrlDerFilename);
        saveIntegrityKey(secretKey, integrityKeyFilename);
    }

    private void storePrivateKey(KeyStore store, char[] password, String ctrlP12Filename) {
        try {
            FileOutputStream ctrlP12OutputStream = new FileOutputStream(ctrlP12Filename);
            store.store(ctrlP12OutputStream, password);
            ctrlP12OutputStream.close();
            consoleOutputController.logOnScreen(String.format(keySavedMessage, ctrlP12Filename));
        } catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException e) {
            throw new KeyGenerationRuntimeException("error while storing the private key", e);
        }
    }

    private void storeCertificate(X509Certificate certificate, String ctrlDerFilename) {
        try (FileOutputStream ctrlDerOutputStream = new FileOutputStream(ctrlDerFilename)){
            ctrlDerOutputStream.write(certificate.getEncoded());
            ctrlDerOutputStream.close();
            consoleOutputController.logOnScreen(String.format(keySavedMessage, ctrlDerFilename));
        } catch (IOException | CertificateEncodingException e) {
            throw new KeyGenerationRuntimeException("error while storing the certificate", e);
        }
    }

    private void computePublicKeyHash(PropertyConfigurationService propertyConfigurationService, String ctrlDerFilename) {
        try (FileInputStream ctrlDerInputStream = new FileInputStream(ctrlDerFilename)){
            // needs to be SHA-1, since windows only displays the sha1 hash when viewing a certificate's details
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] keyHash = streamHasher.computeHash(ctrlDerInputStream, digest);
            ctrlDerInputStream.close();
            String hashString = DatatypeConverter.printHexBinary(keyHash);
            consoleOutputController.logOnScreen(String.format(resources.getString("key_generation.public_key_hash"), hashString));
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new KeyGenerationRuntimeException("error while generating the public key hash", e);
        }
    }

    private void saveIntegrityKey(SecretKey secretKey, String integrityKeyFilename) {
        try (ObjectOutputStream integrityKeyOutputStream = new ObjectOutputStream(new FileOutputStream(integrityKeyFilename))){
            integrityKeyOutputStream.writeObject(secretKey);
            integrityKeyOutputStream.close();
            consoleOutputController.logOnScreen(String.format(keySavedMessage, integrityKeyFilename));
        } catch (IOException e) {
            throw new KeyGenerationRuntimeException("error while saving the integrity key", e);
        }
    }

    protected void setFileUtils(FileUtils fileUtils) {
        this.fileUtils = fileUtils;
    }

    protected void setStreamHasher(StreamHasher streamHasher) {
        this.streamHasher = streamHasher;
    }

    protected void setPasswordDialogController(PasswordDialogController passwordDialogController) {
        this.passwordDialogController = passwordDialogController;
    }

    protected void setKeyGenerator(KeyGenerator keyGenerator) {
        this.keyGenerator = keyGenerator;
    }
}
