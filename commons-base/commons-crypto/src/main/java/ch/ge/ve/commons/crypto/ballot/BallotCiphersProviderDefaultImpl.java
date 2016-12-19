package ch.ge.ve.commons.crypto.ballot;

/*-
 * #%L
 * Common crypto utilities
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

import ch.ge.ve.commons.crypto.exceptions.CryptoConfigurationRuntimeException;
import ch.ge.ve.commons.crypto.utils.CertificateUtils;
import ch.ge.ve.commons.crypto.utils.CipherFactory;
import ch.ge.ve.commons.properties.PropertyConfigurationException;
import ch.ge.ve.commons.properties.PropertyConfigurationService;

import org.apache.log4j.Logger;

import javax.crypto.Cipher;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.Key;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * This abstract class provides a default implementation for most methods declared in the {@link BallotCiphersProvider} interface.
 */
public abstract class BallotCiphersProviderDefaultImpl implements BallotCiphersProvider {
    public static final int GCM_MAC_LENGTH = 128;
    private static final Logger log = Logger.getLogger(BallotCiphersProviderDefaultImpl.class);
    private final CertificateUtils certificateUtils = new CertificateUtils();
    protected PropertyConfigurationService propertyConfigurationService;
    private Key ballotKeyCipherPublicKey;
    private Key integrityCheckSecretKey;

    /**
     * Instantiates a ballotCipher with the algorithm defined as {@link #BALLOT_CRYPTING_ALGORITHM} and the block mode defined as {@link #BALLOT_CRYPTING_BLOCK_MODE}.
     *
     * @return the ballotCipher
     */
    @Override
    public Cipher getBallotCipher() {
        String ballotCipherAlgorithm;
        String ballotBlockmode;
        try {
            ballotCipherAlgorithm = propertyConfigurationService.getConfigValue(BALLOT_CRYPTING_ALGORITHM);
            ballotBlockmode = propertyConfigurationService.getConfigValue(BALLOT_CRYPTING_BLOCK_MODE);
        } catch (PropertyConfigurationException e) {
            throw new CryptoConfigurationRuntimeException("cannot initialize the ballot cipher", e);
        }

        if (log.isDebugEnabled()) {
            log.debug("ballot cipher : " + ballotCipherAlgorithm + ballotBlockmode);
        }

        return new CipherFactory(propertyConfigurationService).getInstance(ballotCipherAlgorithm + ballotBlockmode);

    }

    /**
     * Retrieves the ballotCipher key size from the PropertyConfigurationService ({@link #BALLOT_CRYPTING_KEY_SIZE}).
     *
     * @return the retrieved key size
     */
    @Override
    public int getBallotCipherSize() {
        try {
            return Integer.parseInt(propertyConfigurationService.getConfigValue(BALLOT_CRYPTING_KEY_SIZE));
        } catch (PropertyConfigurationException e) {
            throw new CryptoConfigurationRuntimeException("cannot get the ballot cipher size configuration", e);
        }
    }

    /**
     * Instantiates a ballotKey Cipher with the algorithm defined as {@link #BALLOT_KEY_CRYPTING_ALGORITHM}.
     *
     * @return the new ballotKey Cipher
     */
    @Override
    public Cipher getBallotKeyCipher() {
        String ballotKeyCipherAlgorithm;
        try {
            ballotKeyCipherAlgorithm = propertyConfigurationService.getConfigValue(BALLOT_KEY_CRYPTING_ALGORITHM)
                    + propertyConfigurationService.getConfigValue(BALLOT_KEY_CRYPTING_BLOCKMODE);
        } catch (PropertyConfigurationException e) {
            throw new CryptoConfigurationRuntimeException("cannot get the ballot key cipher algo configuration", e);
        }
        return new CipherFactory(propertyConfigurationService).getInstance(ballotKeyCipherAlgorithm);
    }

    @Override
    public Cipher getIntegrityCipher(PropertyConfigurationService propertyConfigurationService) {
        String integrityCipherAlgorithmWithBlockmode;
        try {
            integrityCipherAlgorithmWithBlockmode =
                    propertyConfigurationService.getConfigValue(BALLOT_INTEGRITY_CHECK_CRYPTING_ALGORITHM) +
                            propertyConfigurationService.getConfigValue(BALLOT_INTEGRITY_CHECK_CRYPTING_BLOCK_MODE);
        } catch (PropertyConfigurationException e) {
            log.error("Configuration error", e);
            throw new CryptoConfigurationRuntimeException("cannot retrieve the integrity cipher configuration");
        }
        return new CipherFactory(propertyConfigurationService).getInstance(integrityCipherAlgorithmWithBlockmode);
    }

    /**
     * Retrieves the publicKey from the path defined as {@link #PUBLIC_KEY_FILE_NAME}
     *
     * @return the retrieved publicKey
     */
    @Override
    public Key getBallotKeyCipherPublicKey() {
        if (ballotKeyCipherPublicKey == null) {
            String ballotKeyPath;
            try {
                ballotKeyPath = propertyConfigurationService.getConfigValue(PUBLIC_KEY_FILE_NAME);
            } catch (PropertyConfigurationException e) {
                throw new CryptoConfigurationRuntimeException("cannot get the ballot key cipher public key filename configuration", e);
            }

            if (log.isDebugEnabled()) {
                log.debug("loadBallotEncryptionKey with ballotKeyPath : " + ballotKeyPath);
            }

            try {
                FileInputStream fis = new FileInputStream(ballotKeyPath);
                X509Certificate x509Certificate = certificateUtils.getCertificate(fis);
                ballotKeyCipherPublicKey = x509Certificate.getPublicKey();
            } catch (FileNotFoundException | CertificateException e) {
                throw new CryptoConfigurationRuntimeException("cannot load the ballot key cipher public key from file", e);
            }
        }

        return ballotKeyCipherPublicKey;
    }

    /**
     * Retrieves the integrityCheck secretKey, from the path defined as {@link #INTEGRITY_KEY_FILE_NAME}
     *
     * @return the retrieved key
     */
    @Override
    public Key getIntegrityCheckSecretKey() {
        if (integrityCheckSecretKey == null) {
            integrityCheckSecretKey = loadIntegrityKey();
        }
        return integrityCheckSecretKey;
    }

    protected Key loadIntegrityKey() {
        String secretKeyPath;
        try {
            secretKeyPath = propertyConfigurationService.getConfigValue(INTEGRITY_KEY_FILE_NAME);
        } catch (PropertyConfigurationException e) {
            throw new CryptoConfigurationRuntimeException("cannot get the integrity key filename", e);
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(secretKeyPath))) {
            return (Key) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new CryptoConfigurationRuntimeException("cannot load the secret key from file", e);
        }
    }

    /**
     * Getter for the configuration service
     *
     * @return the configuration service used
     */
    @Override
    public PropertyConfigurationService getPropertyConfigurationService() {
        return propertyConfigurationService;
    }

    /**
     * Setter for the configuration service
     *
     * @param propertyConfigurationService the configuration service to use
     */
    @Override
    public void setPropertyConfigurationService(PropertyConfigurationService propertyConfigurationService) {
        this.propertyConfigurationService = propertyConfigurationService;
    }

    @Override
    public int getMacLength() {
        return GCM_MAC_LENGTH;
    }

    @Override
    public void invalidatePrivateKeyCache() {
        throw new UnsupportedOperationException("Cannot retrieve private key for this module");
    }

    @Override
    public void invalidatePublicKeyCache() {
        ballotKeyCipherPublicKey = null;
    }

    @Override
    public void invalidateIntegrityKeyCache() {
        integrityCheckSecretKey = null;
    }
}
