package ch.ge.ve.commons.crypto;

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
import ch.ge.ve.commons.crypto.utils.CipherFactory;
import ch.ge.ve.commons.crypto.utils.MacFactory;
import ch.ge.ve.commons.crypto.utils.SecureRandomFactory;
import ch.ge.ve.commons.properties.PropertyConfigurationException;
import ch.ge.ve.commons.properties.PropertyConfigurationService;
import org.apache.commons.io.IOUtils;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.io.*;
import java.security.SecureRandom;

/**
 * This class provides the default implementation for a CryptoUtilsConfiguration
 */
public class SensitiveDataCryptoUtilsConfigurationDefaultImpl implements SensitiveDataCryptoUtilsConfiguration {
    public static final String COMMON_CRYPTO_STORAGE_ALGORITHM = "common.crypto.storage.algorithm";
    public static final String COMMON_CRYPTO_STORAGE_BLOCKMODE = "common.crypto.storage.blockmode";
    public static final String COMMON_CRYPTO_STREAM_MAX_BYTES = "common.crypto.stream.max.bytes";
    private final SecureRandom SECURE_RANDOM = SecureRandomFactory.createPRNG();
    private final PropertyConfigurationService propertyConfigurationService;

    // Mac and Cipher are not thread safe and thus cannot be declared as a static member used concurrently,
    // but we need to limit the instance creations for performance optimization. ThreadLocal scope
    // is the best way to ensure that there is no sharing between threads of the same object, and
    // that they are reused as much as possible (for voting cards generation for example).
    private final ThreadLocal<Mac> macThreadLocal = new ThreadLocal<>();
    private final ThreadLocal<Cipher> cipherThreadLocal = new ThreadLocal<>();

    // no thread safety concern on the secret key
    private SecretKey secretKey;

    public SensitiveDataCryptoUtilsConfigurationDefaultImpl(PropertyConfigurationService propertyConfigurationService) {
        this.propertyConfigurationService = propertyConfigurationService;
    }

    @Override
    public Cipher getCipher() {
        if (cipherThreadLocal.get() == null) {
            try {
                String algorithm = propertyConfigurationService.getConfigValue(COMMON_CRYPTO_STORAGE_ALGORITHM);
                String blockMode = propertyConfigurationService.getConfigValue(COMMON_CRYPTO_STORAGE_BLOCKMODE);
                cipherThreadLocal.set(new CipherFactory(propertyConfigurationService).getInstance(algorithm + blockMode));
            } catch (PropertyConfigurationException e) {
                throw new CryptoConfigurationRuntimeException("Unable to load the sensitive data cipher:", e);
            }
        }
        return cipherThreadLocal.get();
    }

    @Override
    public Mac getMac() {
        if (macThreadLocal.get() == null) {
            macThreadLocal.set(new MacFactory(propertyConfigurationService).getInstance());
        }
        return macThreadLocal.get();
    }

    @Override
    public SecretKey getSecretKey() {
        if (secretKey == null) {
            ObjectInputStream ois = null;

            try {
                final InputStream hmacKeyInputStream = getPasswordHMACKeyInputStream();
                ois = new ObjectInputStream(hmacKeyInputStream);
                secretKey = (SecretKey) ois.readObject();
            } catch (ClassNotFoundException | PropertyConfigurationException | IOException e) {
                throw new CryptoConfigurationRuntimeException("Unable to retrieve the secret key from the file system:", e);
            } finally {
                if (ois != null) {
                    IOUtils.closeQuietly(ois);
                }
            }
        }
        return secretKey;
    }

    @Override
    public int getIterations() {
        try {
            int minIterations = propertyConfigurationService.getConfigValueAsInt("common.crypto.pbkdf.min.iterations");
            int maxIterations = propertyConfigurationService.getConfigValueAsInt("common.crypto.pbkdf.max.iterations");
            return minIterations + SECURE_RANDOM.nextInt(maxIterations - minIterations + 1);
        } catch (PropertyConfigurationException e) {
            throw new CryptoConfigurationRuntimeException("cannot find pbkdf2 iterations configuration", e);
        }
    }

    @Override
    public String getPbkdf2Algorithm() {
        try {
            return propertyConfigurationService.getConfigValue("common.crypto.pbkdf.algorithm");
        } catch (PropertyConfigurationException e) {
            throw new CryptoConfigurationRuntimeException("cannot find pbkdf2 algorithm configuration", e);
        }
    }

    @Override
    public long getSealMaxBytes() {
        try {
            return propertyConfigurationService.getConfigValueAsLong(COMMON_CRYPTO_STREAM_MAX_BYTES);
        } catch (PropertyConfigurationException e) {
            throw new CryptoConfigurationRuntimeException("cannot find seal object max bytes configuration", e);
        }
    }

    private InputStream getPasswordHMACKeyInputStream() throws FileNotFoundException, PropertyConfigurationException {
        final String hmacKeyPath = propertyConfigurationService.getConfigValue("password.hmac.key.filename");
        if (new File(hmacKeyPath).exists()) {
            // first try to find on file system
            return new FileInputStream(hmacKeyPath);
        } else {
            // try to find in classpath (useful for test classes)
            final InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(hmacKeyPath);
            if (resourceAsStream != null) {
                return resourceAsStream;
            } else {
                throw new CryptoConfigurationRuntimeException("Cannot find password hmac key from filesystem or classpath: " + hmacKeyPath);
            }
        }
    }
}
