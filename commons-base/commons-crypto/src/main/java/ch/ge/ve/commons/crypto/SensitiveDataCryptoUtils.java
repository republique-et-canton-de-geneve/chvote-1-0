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

import ch.ge.ve.commons.crypto.exceptions.CryptoOperationRuntimeException;
import ch.ge.ve.commons.crypto.utils.SaltUtils;
import ch.ge.ve.commons.crypto.utils.SecureRandomFactory;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Bytes;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;

/**
 * Crypto primitives for creating and verifying MAC, and for symmetric encryption and decryption.
 * Used only for securing sensitive data stored in the database, such as voting card numbers, municipality of origin, ..
 */
public class SensitiveDataCryptoUtils {
    public static final Charset ENCRYPTION_CHARSET = StandardCharsets.UTF_8;

    /**
     * Default size of the salts for the salted MACs, in bytes.
     */
    private static final int SALT_SIZE_BYTES = 16;
    private static final Base64.Decoder base64decoder = Base64.getDecoder();
    private static final Base64.Encoder base64encoder = Base64.getEncoder();

    /**
     * the configuration required for the encryption, decryption and hashing operations
     */
    private static SensitiveDataCryptoUtilsConfiguration config;

    /*
     * Due to the usage of this class in very varied places in the code both in the web application,
     * and in the administration console, it has been decided to keep its methods static and to
     * load a configuration when the application starts.
     * This way, the configuration of ciphers and authenticated digests can be aligned with the
     * way it is managed for the ballot encryption, while not needing a complete rewrite of various
     * sensitive parts of the code.
     *
     * In this case, using static methods is not an issue, most elements are retrieved through the
     * configuration.
     * The main impact is that tests cannot be run in parallel with different configurations, which
     * simply means that they take longer to run.
     */
    /**
     * Static methods only
     */
    private SensitiveDataCryptoUtils() {
    }

    /**
     * Loads the configurations.
     * <p>
     * The method is static, because the configuration needs to be, and synchronized, to avoid multiple simultaneous writes.
     * </p>
     *
     * @param configuration
     * @see #config
     */
    public static synchronized void configure(SensitiveDataCryptoUtilsConfiguration configuration) {
        config = configuration;
    }

    /**
     * Instantiates a secret key based on its encoded representation and requested algorithm
     *
     * @param keyBytes the encoded representation of the key
     * @param algo     the requested algorithm (e.g. AES)
     * @return a new secret key matching the encoded representation and compatible with the requested algorithm
     */
    public static SecretKey buildSecretKey(byte[] keyBytes, String algo) {
        return new SecretKeySpec(keyBytes, algo);
    }

    /**
     * Generates a new random key
     *
     * @param lengthInBits the length of the requested key, in bits
     * @param algo         the requested algorithm (e.g. AES)
     * @return a new secret key compatible with the requested algorithm, of the requested bitLength
     */
    public static SecretKey buildSecretKey(int lengthInBits, String algo) {
        Preconditions.checkArgument(lengthInBits % 8 == 0, String.format("Invalid length, not a multiple of 8: %d", lengthInBits));
        SecureRandom sr = SecureRandomFactory.createPRNG();
        byte[] keyBytes = new byte[lengthInBits / 8];
        sr.nextBytes(keyBytes);
        return new SecretKeySpec(keyBytes, algo);
    }

    /**
     * Builds the mac of the input string and returns it as a string
     *
     * @param input message to be MACed.
     * @return the MAC in base64
     */
    public static String buildMACAsBase64String(String input) {
        return base64encoder.encodeToString(buildMAC(input));
    }

    /**
     * Builds the mac of the input string and returns it as a string,
     * applying a generated {@link #SALT_SIZE_BYTES} bytes salt on the message.
     *
     * @param input message to be MACed.
     * @return the concatenation of the {@link #SALT_SIZE_BYTES} bytes salt and the MAC in base64
     */
    public static String buildSaltedMACAsBase64String(String input) {
        final byte[] salt = SaltUtils.generateSalt(SALT_SIZE_BYTES * 8);
        final byte[] mac = buildMAC(input, salt);
        return base64encoder.encodeToString(mac);
    }

    /**
     * Computes the unsalted MAC of the input
     *
     * @param input any string
     * @return the MAC (using algorithm defined in the {@link #config}) of the input string
     */
    public static byte[] buildMAC(String input) {
        return buildMAC(input, null);
    }

    /**
     * Computes a salted MAC of the input
     *
     * @param input any string
     * @param salt  the salt to be used by the MAC
     * @return the MAC (using algorithm defined in the {@link #config}) of the input string, using the provided salt
     */
    public static byte[] buildMAC(String input, byte[] salt) {
        return buildMAC(input.getBytes(), salt);
    }

    /**
     * Computes a salted MAC of the input
     *
     * @param input any byte array
     * @param salt  the salt to be used by the MAC
     * @return the MAC (using algorithm defined in the {@link #config}) of the input byte array, using the provided salt
     */
    public static byte[] buildMAC(byte[] input, byte[] salt) {
        try {
            Mac mac = config.getMac();
            mac.init(config.getSecretKey());
            if (salt != null) {
                mac.update(salt);
                final byte[] macText = mac.doFinal(input);
                return Bytes.concat(salt, macText);
            } else {
                return mac.doFinal(input);
            }
        } catch (GeneralSecurityException e) {
            throw new CryptoOperationRuntimeException(e);
        }
    }

    /**
     * Checks the authentication of a message.
     *
     * @param message     message to be authenticated
     * @param macAsBase64 MAC against which the message is to be authenticated
     * @return true if the computed MAC of the message is equal to the provided MAC
     */
    public static boolean verifyMAC(String message, String macAsBase64) {
        final byte[] knownMac = base64decoder.decode(macAsBase64);
        final byte[] calculatedMac = buildMAC(message);
        return MessageDigest.isEqual(knownMac, calculatedMac);
    }

    /**
     * Checks the authentication of a message using a salted MAC.
     *
     * @param message            message to be authenticated
     * @param macAndSaltAsBase64 16 byte salt and MAC against which the message is to be authenticated.
     * @return true if the computed MAC of the message is equal to the provided MAC
     */
    public static boolean verifySaltedMAC(String message, String macAndSaltAsBase64) {
        final byte[] knownMacAndSalt = base64decoder.decode(macAndSaltAsBase64);
        final byte[] salt = Arrays.copyOfRange(knownMacAndSalt, 0, SALT_SIZE_BYTES);
        final byte[] calculatedMac = buildMAC(message, salt);
        return MessageDigest.isEqual(knownMacAndSalt, calculatedMac);
    }

    /**
     * Encrypts the given Integer and encodes the resulting byte array into a Base64 String
     *
     * @param input any integer
     * @return the Base64 representation of the encrypted input (using the algorithm and key provided by the {@link #config})
     */
    public static String encryptAsBase64String(Integer input) {
        return encryptAsBase64String(input.longValue());
    }

    /**
     * Encrypts the given Long and encodes the resulting byte array into a Base64 String
     *
     * @param input any long
     * @return the Base64 representation of the encrypted input (using the algorithm and key provided by the {@link #config})
     * @see #decryptAsLong(String) the reverse operation
     */
    public static String encryptAsBase64String(Long input) {
        byte[] bytes = BigInteger.valueOf(input).toByteArray();
        return Base64.getEncoder().encodeToString(encrypt(bytes));
    }

    /**
     * Encrypts the given String and encodes the resulting byte array into a Base64 String
     *
     * @param input any String
     * @return the Base64 representation of the encrypted input (using the algorithm and key provided by the {@link #config})
     * @see #decryptAsString(String) the reverse operation
     */
    public static String encryptAsBase64String(String input) {
        return base64encoder.encodeToString(encrypt(input));
    }

    /**
     * Encrypts the given String
     *
     * @param input any String
     * @return the encrypted input as a byte array (using the algorithm and key provided by the {@link #config})
     * @see #decryptAsString(byte[]) the reverse operation
     */
    public static byte[] encrypt(String input) {
        return encrypt(input.getBytes(ENCRYPTION_CHARSET));
    }

    /**
     * @param input an byte array to encrypt
     * @return the concatenation of the IV followed by the cipher text
     * @see #decrypt(byte[]) the reverse operation
     */
    public static byte[] encrypt(byte[] input) {
        try {
            Cipher cipher = config.getCipher();
            SecretKey secretKey = config.getSecretKey();
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, SecureRandomFactory.createPRNG()); // init generates the IV
            byte[] iv = cipher.getIV();
            byte[] cipherText = cipher.doFinal(input);
            return Bytes.concat(iv, cipherText);
        } catch (GeneralSecurityException e) {
            throw new CryptoOperationRuntimeException(e);
        }
    }

    /**
     * Takes an encrypted Long, encoded as a Base64 String and decrypts it back into a Long
     *
     * @param base64Input a Base64 String representing an encrypted Long
     * @return the decrypted Long (using the algorithm and key defined in the {@link #config})
     * @see #encryptAsBase64String(Long) the reverse operation
     */
    public static Long decryptAsLong(String base64Input) {
        return decryptAsLong(base64decoder.decode(base64Input));
    }

    /**
     * Takes an encrypted Long and decrypts it back into a Long
     *
     * @param input a byte array containing an encrypted Long
     * @return the decrypted Long (using the algorithm and key defined in the {@link #config})
     */
    public static Long decryptAsLong(byte[] input) {
        byte[] bytes = decrypt(input);
        return new BigInteger(bytes).longValue();
    }

    /**
     * Takes an encrypted String, encoded as a Base64 String and decrypts it back into a String
     *
     * @param base64Input a Base64 String representing an encrypted String
     * @return the original plaintext String
     * @see #encryptAsBase64String(String) the reverse operation
     */
    public static String decryptAsString(String base64Input) {
        return decryptAsString(base64decoder.decode(base64Input));
    }

    /**
     * Takes a byte array representing an encrypted String and decrypts it back into a String
     *
     * @param input a byte array containing an encrypted String
     * @return the plaintext String
     * @see #encrypt(String) the reverse operation
     */
    public static String decryptAsString(byte[] input) {
        return new String(decrypt(input), ENCRYPTION_CHARSET);
    }

    /**
     * Takes an encrypted byte array and returns the corresponding decrypted byte array
     *
     * @param input the concatenation of the IV followed by the cipher text
     * @return the decrypted byte array
     * @see #encrypt(byte[]) the reverse operation
     */
    public static byte[] decrypt(byte[] input) {
        try {
            Cipher cipher = config.getCipher();
            int blockSize = cipher.getBlockSize();
            byte[] iv = Arrays.copyOfRange(input, 0, blockSize);
            byte[] cipherText = Arrays.copyOfRange(input, blockSize, input.length);
            SecretKey secretKey = config.getSecretKey();
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            return cipher.doFinal(cipherText);
        } catch (GeneralSecurityException e) {
            throw new CryptoOperationRuntimeException(e);
        }
    }

    /**
     * Wraps any serializable object into a SealedObject and returns the corresponding byte array
     *
     * @param object the object to seal
     * @return the byte array representing the SealedObject (locked with the algorithm and key provided in the {@link #config}
     * @throws CryptoOperationRuntimeException
     * @see #unsealObject(byte[]) the matching unwrapping method
     */
    public static byte[] sealObject(Serializable object) {
        ObjectSealer objectSealer = new ObjectSealer(config.getCipher(), config.getSecretKey());
        return objectSealer.sealObject(object);
    }

    /**
     * Parses a SealedObject from the given byte array and retrieves the original wrapped object
     *
     * @param encryptedObject a byte array representing a SealedObject
     * @return the original Serializable object
     * @throws CryptoOperationRuntimeException
     * @see #sealObject(java.io.Serializable) the matching wrapping operation
     */
    public static Object unsealObject(byte[] encryptedObject) {
        ObjectSealer objectSealer = new ObjectSealer(config.getCipher(), config.getSecretKey());
        return objectSealer.unsealObject(encryptedObject, config.getSealMaxBytes());
    }


    /**
     * Generates a strong hash from a given clear text password
     *
     * @param password the password
     * @return a String of 3 blocks: the number of iterations,
     * then the hexadecimal representation of the salt,
     * and then the hexadecimal representation of the password hash
     */
    public static String generateStrongPasswordHash(char[] password) {
        int iterations = config.getIterations();
        byte[] salt = SaltUtils.generateSalt(SALT_SIZE_BYTES * 8);

        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(config.getPbkdf2Algorithm());
            PBEKeySpec keySpec = new PBEKeySpec(password, salt, iterations, 64 * 8);
            SecretKey secretKey = skf.generateSecret(keySpec);

            return String.format("%d:%s:%s", iterations, DatatypeConverter.printHexBinary(salt), DatatypeConverter.printHexBinary(secretKey.getEncoded()));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new CryptoOperationRuntimeException("cannot generate strong password hash", e);
        }
    }

    /**
     * Validates a given clear text password against its stored expected hashed value in a time constant manner.
     *
     * @param passwd     the password to be tested
     * @param storedHash the stored expected hashed value
     * @return true if the password matches the stored hash
     */
    public static boolean validateStrongPasswordHash(char[] passwd, String storedHash) {
        String[] parts = storedHash.split(":");
        if (parts.length != 3) {
            return false;
        }

        int iterations = Integer.parseInt(parts[0]);
        byte[] salt = fromHex(parts[1]);
        byte[] hash = fromHex(parts[2]);

        try {
            PBEKeySpec keySpec = new PBEKeySpec(passwd, salt, iterations, hash.length * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(config.getPbkdf2Algorithm());
            byte[] testHash = skf.generateSecret(keySpec).getEncoded();
            return constantTimeArrayCompare(hash, testHash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new CryptoOperationRuntimeException("cannot validate strong password hash", e);
        }
    }

    private static boolean constantTimeArrayCompare(byte[] a1, byte[] a2) {
        if (a1.length != a2.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a1.length; i++) {
            result |= a1[i] ^ a2[i];
        }
        return result == 0;
    }

    private static byte[] fromHex(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }

        return bytes;
    }
}
