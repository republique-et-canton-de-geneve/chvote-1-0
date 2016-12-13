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

import ch.ge.ve.commons.crypto.utils.SecureRandomFactory;
import com.google.common.base.Joiner;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.crypto.SecretKey;
import java.io.*;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;
import java.util.Random;

import static ch.ge.ve.commons.crypto.SensitiveDataCryptoUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;

/**
 * This test suit aims at covering the {@link SensitiveDataCryptoUtils} utility class.
 */
public class SensitiveDataCryptoUtilsTest {
    private static final int SECRETKEY_LENGTH = 256;
    private static final TestSensitiveDataCryptoUtilsConfiguration configuration = new TestSensitiveDataCryptoUtilsConfiguration();

    @BeforeClass
    public static void init() {
        Security.addProvider(new BouncyCastleProvider());
        SensitiveDataCryptoUtils.configure(configuration);
    }

    /**
     * saving a key and retrieving it from a file should give the same object
     */
    @Test
    public void saveAndRetrieveSecretKey() throws IOException, ClassNotFoundException {
        SecretKey sK = SensitiveDataCryptoUtils.buildSecretKey(SECRETKEY_LENGTH, configuration.getPbkdf2Algorithm());
        String secretKeyFingerPrint = Base64.getEncoder().encodeToString(sK.getEncoded());

        File keyFile = File.createTempFile("keyfile", "key");
        keyFile.deleteOnExit();

        saveInFile(keyFile, sK);

        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(keyFile));
        SecretKey readKey = (SecretKey) ois.readObject();
        ois.close();

        assertTrue(readKey.getEncoded().length * 8 == SECRETKEY_LENGTH);
        assertTrue(Base64.getEncoder().encodeToString(readKey.getEncoded()).equals(secretKeyFingerPrint));
    }

    private void saveInFile(File keyFile, Object sK) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(keyFile));
        oos.writeObject(sK);
        oos.flush();
        oos.close();
    }

    /**
     * buildMAC() should generate a correct MAC
     */
    @Test
    public void testMac() throws InvalidKeyException, NoSuchAlgorithmException, IOException, ClassNotFoundException {
        String inputData = "This is the text we want to get the MAC for";
        String hmac = Base64.getEncoder().encodeToString(buildMAC(inputData));
        assertTrue(hmac.equals("kJ5sdJg8C490B16kmZhE+druaVTWTUXvEKwL643w6dI="));
    }

    /**
     * verifyMAC() should validate a message with its correct MAC
     */
    @Test
    public void testMessageAuthenticated() {
        String message = "This is the text we want to get the MAC for";
        final String mac = buildMACAsBase64String(message);
        assertTrue("message should be authenticated", verifyMAC(message, mac));
    }

    /**
     * verifySaltedMAC() should validate a message with its correct salted MAC
     */
    @Test
    public void testMessageAuthenticatedWithSalt() {
        String message = "This is the text we want to get the MAC for";
        final String mac = buildSaltedMACAsBase64String(message);
        assertTrue("message should be authenticated", verifySaltedMAC(message, mac));
    }

    /**
     * encryption followed by a decryption should return the same object
     */
    @Test
    public void encrypt_then_decrypt_byte_array_should_be_identity() {
        byte[] input = "The lazy fox jumps over the ".getBytes();
        byte[] cipherText = encrypt(input);
        assertThat(decrypt(cipherText), is(input));
    }

    /**
     * encryption followed by a decryption of a large object should return the same object
     */
    @Test
    public void encrypt_then_decrypt_large_byte_array_should_be_identity() {
        byte[] input = new byte[4080];
        Random random = SecureRandomFactory.createPRNG();
        random.nextBytes(input);
        byte[] cipherText = encrypt(input);
        assertThat(decrypt(cipherText), is(input));
    }

    /**
     * encryption followed by a decryption of a long object should return the same object
     */
    @Test
    public void encrypt_then_decrypt_long_should_be_identity() {
        long input = 123456789L;
        String cipherText = encryptAsBase64String(input);
        assertThat(decryptAsLong(cipherText), is(input));
    }

    /**
     * encryption followed by a decryption of a string object should return the same object
     */
    @Test
    public void encrypt_then_decrypt_string_should_be_identity() {
        String input = "This is a very secret plaintext";
        String cipherText = encryptAsBase64String(input);
        assertThat(decryptAsString(cipherText), is(input));
    }

    /**
     * HMAC of a long representation and a sting representation of the same number should be the same
     */
    @Test
    public void hmacOfCardNumberShouldBeStable() {
        String cardNumberString = "1234567890123456";

        String hmac = buildMACAsBase64String(cardNumberString);

        long cardNumber = Long.valueOf(cardNumberString);

        assertThat(buildMACAsBase64String(String.valueOf(cardNumber)), is(hmac));
    }

    /**
     * encryption of the date of birth should be randomized
     */
    @Test
    public void encryptionOfDateOfBirthShouldBeRandomized() {
        Long dob = 19850101L;
        String encryptedDOB = encryptAsBase64String(dob);

        assertThat(encryptAsBase64String(dob), not(encryptedDOB));
    }

    /**
     * encryption followed by decryption of the date of birth should return the same object
     */
    @Test
    public void encryptThenDecryptDateOfBirthShouldBeStable() {
        Long dob = 19850101L;
        String encryptedDOB = encryptAsBase64String(dob);

        assertThat(decryptAsLong(encryptedDOB), is(dob));
    }

    /**
     * validation of a secure password should work
     */
    @Test
    public void securePasswordHashShouldValidate() {
        char[] passwd = "A random te$ting p#s$w0rd!!!".toCharArray();
        String passwordHash = generateStrongPasswordHash(passwd);

        assertThat("The validation of the password should work", validateStrongPasswordHash(passwd, passwordHash));
    }

    /**
     * validation of a password hash with the wrong password should fail
     */
    @Test
    public void securePasswordHashShouldNotValidateWrongPassword() {
        char[] passwd = "A random te$ting p#s$w0rd!!!".toCharArray();
        String passwordHash = generateStrongPasswordHash(passwd);
        char[] wrongPasswd = "Not this one!".toCharArray();

        assertThat("A wrong password should not be validated", !validateStrongPasswordHash(wrongPasswd, passwordHash));
    }

    /**
     * alidation of the password should fail when iteration count has been altered
     */
    @Test
    public void securePasswordHashWithWrongIterationsCountShouldNotValidate() {
        char[] passwd = "The lazy fox jumps over...".toCharArray();
        String passwordHash = generateStrongPasswordHash(passwd);

        String[] parts = passwordHash.split(":");

        int iterations = Integer.parseInt(parts[0]);

        String manipulatedIterationsHash = Joiner.on(":").join(iterations + 1, parts[1], parts[2]);
        assertThat("The validation of the password should fail when iteration count has been altered", !validateStrongPasswordHash(passwd, manipulatedIterationsHash));
    }

    /**
     * validation of the password should fail when the salt has been altered
     */
    @Test
    public void securePasswordHashWithWrongSaltShouldNotValidate() {
        char[] passwd = "A random te$ting p#s$w0rd!!!".toCharArray();
        String passwordHash = generateStrongPasswordHash(passwd);

        String[] parts = passwordHash.split(":");

        String salt = new BigInteger(parts[1].length() / 2, new SecureRandom()).toString(16);
        if (salt.length() < parts[1].length()) {
            salt = String.format("%0" + (parts[1].length() - salt.length()) + "d%s", 0, salt);
        }
        String manipulatedSaltHash = Joiner.on(":").join(parts[0], salt, parts[2]);

        assertThat("The validation of the password should fail when the salt has been altered", !validateStrongPasswordHash(passwd, manipulatedSaltHash));
    }

}
