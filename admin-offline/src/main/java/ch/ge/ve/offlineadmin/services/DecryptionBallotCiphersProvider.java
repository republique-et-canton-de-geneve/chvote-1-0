package ch.ge.ve.offlineadmin.services;

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
import ch.ge.ve.commons.crypto.ballot.BallotCiphersProviderDefaultImpl;
import ch.ge.ve.commons.crypto.exceptions.CryptoConfigurationRuntimeException;
import ch.ge.ve.commons.crypto.exceptions.PrivateKeyPasswordMismatchException;
import ch.ge.ve.commons.crypto.utils.CertificateUtils;
import ch.ge.ve.commons.fileutils.OutputFilesPattern;
import ch.ge.ve.commons.properties.PropertyConfigurationException;
import ch.ge.ve.commons.properties.PropertyConfigurationService;
import ch.ge.ve.offlineadmin.exception.KeyProvisioningRuntimeException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Optional;
import java.util.regex.Pattern;

import static ch.ge.ve.offlineadmin.util.SecurityConstants.CERT_PRIVATE_KEY_FILENAME_PATTERN;

/**
 * Ballot decryption ciphers provider, used by {@link BallotCipherService}
 */
public class DecryptionBallotCiphersProvider extends BallotCiphersProviderDefaultImpl {
    private final File keyDirectory;
    private PrivateKey privateKey;

    /**
     * Constructor with the keys directory
     *
     * @param keyDirectory
     */
    public DecryptionBallotCiphersProvider(File keyDirectory) {
        this.keyDirectory = keyDirectory;
    }

    @Override
    public Key getBallotKeyCipherPrivateKey() {
        return privateKey;
    }

    @Override
    public void loadBallotKeyCipherPrivateKey(String password) throws PrivateKeyPasswordMismatchException {
        PropertyConfigurationService propertyConfigurationService = getPropertyConfigurationService();

        Pattern privateKeyPathPattern;
        String privateKeyAlias;
        try {
            privateKeyPathPattern = Pattern.compile(propertyConfigurationService.getConfigValue(CERT_PRIVATE_KEY_FILENAME_PATTERN));
            privateKeyAlias = propertyConfigurationService.getConfigValue(PRIVATE_KEY_ALIAS);
        } catch (PropertyConfigurationException e) {
            throw new CryptoConfigurationRuntimeException("cannot get the private key configuration", e);
        }

        final Path privateKeyPath = getPrivateKeyPath(privateKeyPathPattern);
        final KeyStore caKs = getKeyStore(password, privateKeyPath);
        loadPrivateKey(password, privateKeyAlias, caKs);
    }

    private Path getPrivateKeyPath(Pattern privateKeyPathPattern) {
        final Optional<Path> privateKeyPath = new OutputFilesPattern().findFirstFileByPattern(privateKeyPathPattern, keyDirectory.toPath());
        return privateKeyPath.orElseThrow(() -> new KeyProvisioningRuntimeException(String.format("Cannot find the private key matching %s in directory %s", privateKeyPathPattern, keyDirectory)));
    }

    private static KeyStore getKeyStore(String password, Path privateKeyPath) throws PrivateKeyPasswordMismatchException {
        KeyStore caKs;
        try (FileInputStream fileInputStream = new FileInputStream(privateKeyPath.toFile())) {
            caKs = buildKeyStore(fileInputStream, password.toCharArray());
        } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new KeyProvisioningRuntimeException("cannot load or open the key store", e);
        }
        return caKs;
    }

    private static KeyStore buildKeyStore(FileInputStream fileInputStream, char[] password) throws PrivateKeyPasswordMismatchException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        KeyStore caKs = CertificateUtils.createPKCS12KeyStore();
        try {
            caKs.load(fileInputStream, password);
        } catch (IOException e) {
            throw new PrivateKeyPasswordMismatchException("Password mismatch", e);
        }
        return caKs;
    }

    private void loadPrivateKey(String password, String privateKeyAlias, KeyStore caKs) throws PrivateKeyPasswordMismatchException {
        try {
            privateKey = (PrivateKey) caKs.getKey(privateKeyAlias, password.toCharArray());
        } catch (KeyStoreException | NoSuchAlgorithmException e) {
            throw new KeyProvisioningRuntimeException("cannot get the private key from the key store", e);
        } catch (UnrecoverableKeyException e) {
            throw new PrivateKeyPasswordMismatchException("Password mismatch", e);
        }
    }

    @Override
    public void invalidatePrivateKeyCache() {
        privateKey = null;
    }
}
