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

import ch.ge.ve.commons.crypto.exceptions.AuthenticationTagMismatchException;
import ch.ge.ve.commons.crypto.exceptions.CryptoConfigurationRuntimeException;
import ch.ge.ve.commons.crypto.exceptions.CryptoOperationRuntimeException;
import ch.ge.ve.commons.crypto.exceptions.PrivateKeyPasswordMismatchException;
import ch.ge.ve.commons.crypto.utils.SecureRandomFactory;
import ch.ge.ve.commons.properties.PropertyConfigurationService;

import org.apache.log4j.Logger;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.modes.AEADBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

import java.io.*;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * This class is responsible for managing the encryption and decryption of ballot related elements:
 * <ul>
 * <li>the ballot contents themselves, see {@link #encryptBallotThenWrapForAuthentication(String, int)}, {@link #verifyAuthenticationThenUnwrap(AuthenticatedBallot)} and {@link #decryptBallot(EncryptedBallotAndWrappedKey)} </li>
 * </ul>
 */
public class BallotCipherService {
    public static final int AEAD_TAG_SIZE = 128;
    private static final Logger log = Logger.getLogger(BallotCipherService.class);

    private final BallotCiphersProvider ciphersProvider;

    private final PropertyConfigurationService propertyConfigurationService;

    /**
     * The default constructor.
     *
     * @param ciphersProvider              the {@link BallotCiphersProvider} to use
     * @param propertyConfigurationService the {@link PropertyConfigurationService} defining the algorithms to use and other parameters.
     */
    public BallotCipherService(BallotCiphersProvider ciphersProvider, PropertyConfigurationService propertyConfigurationService) {
        this.ciphersProvider = ciphersProvider;
        this.propertyConfigurationService = propertyConfigurationService;
        ciphersProvider.setPropertyConfigurationService(propertyConfigurationService);
    }

    private static byte[] toByteArray(SealedObject sealedBallot) throws IOException {
        byte[] cipheredBallot;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(sealedBallot);
            cipheredBallot = bos.toByteArray();
        } finally {
            if (out != null) {
                out.close();
            }
            bos.close();
        }
        return cipheredBallot;
    }

    private static SealedObject toSealedObject(byte[] ballotContentBytes) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(ballotContentBytes));
        return (SealedObject) ois.readObject();
    }

    /**
     * Retrieves the plain name of the algorithm from the transformation description.
     *
     * @param transformation expected input examples: "AES/CBC/PKCS5PADDING", "AES", ...
     * @return the name of the underlying algorithm (<i>e.g.</i> "AES", "RSA", ...)
     */
    private static String getAlgoPlainName(String transformation) {
        return transformation.split("/")[0];
    }

    /**
     * Encrypts the ballot contents (supports any String), to an AuthenticatedBallot
     * <p>The process is two-fold</p>
     * <ul>
     * <li>First, the ballot is encrypted using the Election officials public key and standard mixed encryption</li>
     * <li>Second, the resulting encrypted ballot and wrapped key are encrypted a second time, using an AEAD cipher,
     * using the ballot index as associated data.</li>
     * </ul>
     * <p>The result is an AuthenticatedBallot</p>
     *
     * @param plainText   the plainText to be encrypted
     * @param ballotIndex the index of the ballot (since it is authenticated, it prevents the copy of one vote to another)
     * @return an AuthenticatedBallot ready for storage
     * @throws CryptoConfigurationRuntimeException
     * @throws CryptoOperationRuntimeException
     */
    public AuthenticatedBallot encryptBallotThenWrapForAuthentication(String plainText, int ballotIndex)  {
        Cipher ballotKeyCipher = ciphersProvider.getBallotKeyCipher();
        Cipher ballotCipher = ciphersProvider.getBallotCipher();

        // Generate a random symmetric key, renewed for each ballot
        Key plainSymmetricKey = createNewRandomSymmetricKey(ballotCipher);

        // Initialise the first layer symmetric cipher and  perform the first layer of symmetric encryption
        byte[] encryptedBallot = doFirstLayerEncryption(plainText, ballotCipher, plainSymmetricKey);

        Key integrityKey = ciphersProvider.getIntegrityCheckSecretKey();
        Cipher integrityCipher = ciphersProvider.getIntegrityCipher(propertyConfigurationService);

        byte[] authenticatedBallot = aeadEncrypt(integrityCipher, integrityKey, ballotIndex, encryptedBallot);

        // Due to the lack of an API to retrieve the tag from the cipher, it must be extracted from the resulting ciphertext
        // BouncyCastle simply appends the tag to the ciphertext and verifies it upon decryption
        byte[] tag = Arrays.copyOfRange(authenticatedBallot, authenticatedBallot.length - (AEAD_TAG_SIZE / Byte.SIZE), authenticatedBallot.length);

        // Wrapping of the random symmetric key using the Electoral Officers' public key
        byte[] wrappedKey = wrapKey(ballotKeyCipher, plainSymmetricKey);

        return new AuthenticatedBallot(wrappedKey, authenticatedBallot, ballotIndex, tag);
    }

    private Key createNewRandomSymmetricKey(Cipher ballotCipher) {
        KeyGenerator generator;
        try {
            generator = KeyGenerator.getInstance(getAlgoPlainName(ballotCipher.getAlgorithm()));
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoConfigurationRuntimeException("first layer symmetric cipher key algorithm is invalid", e);
        }
        generator.init(ciphersProvider.getBallotCipherSize(), SecureRandomFactory.createPRNG());
        return generator.generateKey();
    }

    private byte[] doFirstLayerEncryption(String plainText, Cipher ballotCipher, Key plainSymmetricKey) {
        try {
            ballotCipher.init(Cipher.ENCRYPT_MODE, plainSymmetricKey, SecureRandomFactory.createPRNG());
        } catch (InvalidKeyException e) {
            throw new CryptoConfigurationRuntimeException("first layer symmetric cipher key is invalid", e);
        }
        byte[] encryptedBallot;
        try {
            SealedObject sealedObject = new SealedObject(plainText, ballotCipher);
            encryptedBallot = toByteArray(sealedObject);
        } catch (IOException | IllegalBlockSizeException e) {
            throw new CryptoOperationRuntimeException("cannot seal message", e);
        }
        return encryptedBallot;
    }

    private byte[] aeadEncrypt(Cipher cipher, Key integrityKey, int ballotIndex, byte[] encryptedBallot) {
        byte[] ballotIndexBytes = BigInteger.valueOf(ballotIndex).toByteArray();
        GCMParameterSpec spec = new GCMParameterSpec(AEAD_TAG_SIZE, ballotIndexBytes);
        byte[] result;
        try {
            cipher.init(Cipher.ENCRYPT_MODE, integrityKey, spec);
            cipher.updateAAD(ballotIndexBytes);

            result = cipher.doFinal(encryptedBallot);
        } catch (BadPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException e) {
            throw new CryptoOperationRuntimeException("error while encrypting the cipher text", e);
        }

        return result;
    }

    private byte[] wrapKey(Cipher ballotKeyCipher, Key plainSymmetricKey) {
        byte[] wrappedKey;
        try {
            ballotKeyCipher.init(Cipher.WRAP_MODE, ciphersProvider.getBallotKeyCipherPublicKey(), SecureRandomFactory.createPRNG());
            wrappedKey = ballotKeyCipher.wrap(plainSymmetricKey);
        } catch (InvalidKeyException e) {
            throw new CryptoConfigurationRuntimeException("wrapping public key is invalid", e);
        } catch (IllegalBlockSizeException e) {
            throw new CryptoOperationRuntimeException("cannot wrap key", e);
        }
        return wrappedKey;
    }

    /**
     * Performs the first layer of decryption of a ballot, verifying its authenticity
     *
     * @param authenticatedBallot an AuthenticatedBallot, as previously built by {@link #encryptBallotThenWrapForAuthentication(String, int)} and stored in the database
     * @return an EncryptedBallotAndWrappedKey
     * @throws AuthenticationTagMismatchException
     * @throws CryptoOperationRuntimeException
     */
    public EncryptedBallotAndWrappedKey verifyAuthenticationThenUnwrap(AuthenticatedBallot authenticatedBallot) throws AuthenticationTagMismatchException {
        Key integrityKey = ciphersProvider.getIntegrityCheckSecretKey();
        Cipher integrityCipher = ciphersProvider.getIntegrityCipher(propertyConfigurationService);

        byte[] bytes;
        try {
            bytes = aeadDecrypt(integrityCipher, integrityKey, authenticatedBallot.getBallotIndex(), authenticatedBallot.getAuthenticatedEncryptedBallot());
        } catch (AEADBadTagException e) {
            log.error("Authentication tag mismatch", e);
            throw new AuthenticationTagMismatchException(e.getMessage());
        }

        try {
            return new EncryptedBallotAndWrappedKey(toSealedObject(bytes), authenticatedBallot.getWrappedKey());
        } catch (IOException | ClassNotFoundException e) {
            throw new CryptoOperationRuntimeException("second layer decryption error", e);
        }
    }

    private byte[] aeadDecrypt(Cipher cipher, Key integrityKey, int ballotIndex, byte[] authenticatedEncryptedBallot) throws AEADBadTagException {
        byte[] ballotIndexBytes = BigInteger.valueOf(ballotIndex).toByteArray();
        GCMParameterSpec spec = new GCMParameterSpec(AEAD_TAG_SIZE, ballotIndexBytes);
        byte[] result;
        try {
            cipher.init(Cipher.DECRYPT_MODE, integrityKey, spec);
            cipher.updateAAD(ballotIndexBytes);

            result = cipher.doFinal(authenticatedEncryptedBallot);
        } catch (AEADBadTagException e) {
            // In case of a tag mismatch, we want the exception to be handled by the caller
            throw e;
        } catch (BadPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException e) {
            throw new CryptoOperationRuntimeException("error while decrypting the cipher text", e);
        }
        return result;
    }

    /**
     * Performs the second layer of decryption of the ballot, using the Election Officers' private key and standard
     * mixed encryption
     *
     * @param encryptedBallotAndWrappedKey an EncryptedBallotAndWrappedKey, as provided per a previous call to {@link #verifyAuthenticationThenUnwrap(AuthenticatedBallot)}
     * @return the ballot's original contents
     * @throws CryptoConfigurationRuntimeException
     * @throws CryptoOperationRuntimeException
     */
    public String decryptBallot(EncryptedBallotAndWrappedKey encryptedBallotAndWrappedKey) {
        Cipher ballotKeyCipher = ciphersProvider.getBallotKeyCipher();
        Cipher ballotCipher = ciphersProvider.getBallotCipher();

        // Unwrap the random key k_i, using the Election Officers' private key
        try {
            ballotKeyCipher.init(Cipher.UNWRAP_MODE, ciphersProvider.getBallotKeyCipherPrivateKey());
        } catch (InvalidKeyException e) {
            throw new CryptoConfigurationRuntimeException("decryption key is invalid", e);
        }
        Key plainSymmetricKey;
        try {
            plainSymmetricKey = ballotKeyCipher.unwrap(encryptedBallotAndWrappedKey.getWrappedKey(), getAlgoPlainName(ballotCipher.getAlgorithm()), Cipher.SECRET_KEY);
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new CryptoOperationRuntimeException("key unwrapping error", e);
        }

        // Decrypt the ballot using the unwrapped key k_i
        SealedObject sealedBallot = encryptedBallotAndWrappedKey.getEncryptedBallot();
        try {
            return (String) sealedBallot.getObject(plainSymmetricKey);
        } catch (IOException | ClassNotFoundException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new CryptoOperationRuntimeException("ballot decryption error", e);
        }
    }

    /**
     * Unlocks the private key for decryption.
     *
     * @param password the password of the key file
     * @throws PrivateKeyPasswordMismatchException
     */
    public void loadBallotKeyCipherPrivateKey(String password) throws PrivateKeyPasswordMismatchException {
        ciphersProvider.loadBallotKeyCipherPrivateKey(password);
    }

    /**
     * Sets the path to the private key file.
     *
     * @param privateKeyFileName the path to the private key
     */
    public void setPrivateKeyFileName(String privateKeyFileName) {
        ciphersProvider.invalidatePrivateKeyCache();
        propertyConfigurationService.addConfigValue(BallotCiphersProvider.PRIVATE_KEY_FILE_NAME, privateKeyFileName);
    }

    /**
     * Sets the path to the public key file
     *
     * @param publicKeyFileName the path to the public key
     */
    public void setPublicKeyFileName(String publicKeyFileName) {
        ciphersProvider.invalidatePublicKeyCache();
        propertyConfigurationService.addConfigValue(BallotCiphersProvider.PUBLIC_KEY_FILE_NAME, publicKeyFileName);
    }

    /**
     * Sets the path to the integrity key file
     *
     * @param integrityKeyFileName the path to the integrity key
     */
    public void setIntegrityKeyFileName(String integrityKeyFileName) {
        ciphersProvider.invalidateIntegrityKeyCache();
        propertyConfigurationService.addConfigValue(BallotCiphersProvider.INTEGRITY_KEY_FILE_NAME, integrityKeyFileName);
    }

}
