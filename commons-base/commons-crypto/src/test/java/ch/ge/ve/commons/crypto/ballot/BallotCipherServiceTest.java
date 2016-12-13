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
import ch.ge.ve.commons.crypto.exceptions.ProtocolNotRespectedException;
import ch.ge.ve.commons.crypto.utils.CertificateUtils;
import ch.ge.ve.commons.crypto.utils.CipherFactory;
import ch.ge.ve.commons.properties.PropertyConfigurationException;
import ch.ge.ve.commons.properties.PropertyConfigurationService;
import com.google.common.base.Strings;
import org.apache.log4j.Logger;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.Cipher;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import static ch.ge.ve.commons.crypto.ballot.BallotCiphersProvider.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for the {@link BallotCipherService} class
 */
public class BallotCipherServiceTest {
    private final Logger log = Logger.getLogger(this.getClass());

    private BallotCiphersProvider ballotCiphersProvider;
    private BallotCipherService ballotCipherService;
    private PropertyConfigurationService propertyConfigurationService;

    @Before
    public void setUp() {
        ballotCiphersProvider = mock(BallotCiphersProvider.class);
        propertyConfigurationService = mock(PropertyConfigurationService.class);

        ballotCipherService = new BallotCipherService(ballotCiphersProvider, propertyConfigurationService);

        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * plain ballot encryption should work
     */
    @Test
    public void testEncryptBallot() throws Exception {
        initBallotCiphersProviderMock();

        StringBuilder sb = new StringBuilder();
        sb.append("GE;6699:9903");
        sb.append("370001;0;37000101;p=DEP");
        sb.append(Strings.repeat(";1001", 545));

        AuthenticatedBallot authenticatedBallot = ballotCipherService.encryptBallotThenWrapForAuthentication(sb.toString(), 1);

        assertThat("The encrypted ballot byte array length should be less than 4000 chars", authenticatedBallot.getAuthenticatedEncryptedBallot().length, lessThan(4000));
        assertThat("The encrypted ballot key should be 256 bytes long", authenticatedBallot.getWrappedKey().length, is(256));
        assertThat("The encryption authentication tag should be 128bit / 16 bytes long", authenticatedBallot.getTag().length, is(16));
        assertThat("The authenticated ballot should have the proper index attached", authenticatedBallot.getBallotIndex(), is(1));
    }

    /**
     * encryption followed by decryption of a ballot should return the same object
     */
    @Test
    public void testEncryptAndDecryptBallot() throws Exception {
        initBallotCiphersProviderMock();

        String plainText = "plainText";

        AuthenticatedBallot authenticatedBallot = ballotCipherService.encryptBallotThenWrapForAuthentication(plainText, 1);

        when(ballotCiphersProvider.getBallotKeyCipherPrivateKey()).thenReturn(null);
        EncryptedBallotAndWrappedKey encryptedBallotAndWrappedKey = ballotCipherService.verifyAuthenticationThenUnwrap(authenticatedBallot);

        initializePrivateKey();
        String decryptedText = ballotCipherService.decryptBallot(encryptedBallotAndWrappedKey);

        assertThat("decryptBallot(encryptBallotThenWrapForAuthentication(...)) should be identical", decryptedText, equalTo(plainText));
    }

    /**
     * verification of an altered ballot cipher should fail
     */
    @Test(expected = AuthenticationTagMismatchException.class)
    public void testDecryptBallotWithTagMismatch() throws Exception {
        initBallotCiphersProviderMock();

        String plainText = "plainText";

        AuthenticatedBallot authenticatedBallot = ballotCipherService.encryptBallotThenWrapForAuthentication(plainText, 1);

        when(ballotCiphersProvider.getBallotKeyCipherPrivateKey()).thenReturn(null);

        // alter the tag
        // The tag doesn't need to be kept in the Authenticated ballot anymore, since it is included in the cipher text
        //        authenticatedBallot.getTag()[0] = (byte)(authenticatedBallot.getTag()[0] ^ (byte)1);
        int length = authenticatedBallot.getAuthenticatedEncryptedBallot().length;
        authenticatedBallot.getAuthenticatedEncryptedBallot()[length - 1] = (byte) (authenticatedBallot.getAuthenticatedEncryptedBallot()[length - 1] ^ (byte) 1);

        // should raise an exception
        ballotCipherService.verifyAuthenticationThenUnwrap(authenticatedBallot);
    }

    /**
     * stress test the encrypt and decrypt methods; verification are done manually by analysing the provided logs
     */
    @Test
    public void testEncryptDecryptPerf() throws Exception {
        initBallotCiphersProviderMock();

        int nbIterations = 200;

        List<String> inputs = new ArrayList<String>(3);
        inputs.add("testString");
        inputs.add("somewhat long string");
        inputs.add(Strings.repeat("testString", nbIterations));

        String result = String.format("| %-15s | %-15s | %-8s (%3dx) | %-15s | %-8s (%3dx) | %-15s | %-8s (%3dx) |", "input length", "encrypt mean", "encrypt", nbIterations, "verify mean", "verify", nbIterations, "decryptBallot mean", "decryptBallot", nbIterations);
        log.info(result);
        for (String input : inputs) {
            iterateEncryptDecrypt(input, nbIterations);
        }
    }

    private void iterateEncryptDecrypt(String input, int nbIterations) throws ClassNotFoundException, GeneralSecurityException, InvalidCipherTextException, IOException, ProtocolNotRespectedException, AuthenticationTagMismatchException {
        List<AuthenticatedBallot> authenticatedBallots = new ArrayList<AuthenticatedBallot>();

        // Run a first encryption to initialize the cipher, we want to discard the initialization time
        AuthenticatedBallot initBallot = ballotCipherService.encryptBallotThenWrapForAuthentication("test", 0);

        final long startEncrypt = System.currentTimeMillis();
        for (int i = 0; i < nbIterations; i++) {
            authenticatedBallots.add(ballotCipherService.encryptBallotThenWrapForAuthentication(input, i));
        }
        final long endEncrypt = System.currentTimeMillis();

        when(ballotCiphersProvider.getBallotKeyCipherPrivateKey()).thenReturn(null);
        // Again, a first run to initialize the cipher
        EncryptedBallotAndWrappedKey initVerif = ballotCipherService.verifyAuthenticationThenUnwrap(initBallot);

        List<EncryptedBallotAndWrappedKey> encryptedBallotAndWrappedKeys = new ArrayList<EncryptedBallotAndWrappedKey>();
        final long startVerify = System.currentTimeMillis();
        for (AuthenticatedBallot authenticatedBallot : authenticatedBallots) {
            encryptedBallotAndWrappedKeys.add(ballotCipherService.verifyAuthenticationThenUnwrap(authenticatedBallot));
        }
        final long endVerify = System.currentTimeMillis();

        initializePrivateKey();

        String init = ballotCipherService.decryptBallot(initVerif);
        assertThat(init, equalTo("test"));

        List<String> retrievedPlaintexts = new ArrayList<String>();
        final long startDecrypt = System.currentTimeMillis();
        for (EncryptedBallotAndWrappedKey encryptedBallotAndWrappedKey : encryptedBallotAndWrappedKeys) {
            retrievedPlaintexts.add(ballotCipherService.decryptBallot(encryptedBallotAndWrappedKey));
        }
        final long endDecrypt = System.currentTimeMillis();

        assertThat("retrieved plaintexts should all be equal to the input", retrievedPlaintexts, everyItem(equalTo(input)));

        final long totalEncryptionTime = endEncrypt - startEncrypt;
        final long meanEncryptionTime = totalEncryptionTime / authenticatedBallots.size();
        assertThat(meanEncryptionTime, lessThan(500l));

        final long totalVerificationTime = endVerify - startVerify;
        final long meanVerificationTime = totalVerificationTime / authenticatedBallots.size();
        assertThat(meanVerificationTime, lessThan(500l));

        final long totalDecryptionTime = endDecrypt - startDecrypt;
        final long meanDecryptionTime = totalDecryptionTime / authenticatedBallots.size();
        assertThat(meanDecryptionTime, lessThan(1000l));

        String result = String.format("| %15d | %15d | %15d | %15d | %15d | %15d | %15d |", input.length(), meanEncryptionTime, totalEncryptionTime, meanVerificationTime, totalVerificationTime, meanDecryptionTime, totalDecryptionTime);
        log.info(result);
    }

    private void initBallotCiphersProviderMock() throws GeneralSecurityException, IOException, ClassNotFoundException, PropertyConfigurationException {
        // Instantiate ballotCipher
        PropertyConfigurationService propertyConfigurationService1 = new PropertyConfigurationService();
        String ballotCipherAlgo = propertyConfigurationService1.getConfigValue(BALLOT_CRYPTING_ALGORITHM);
        String ballotBlockmode = propertyConfigurationService1.getConfigValue(BALLOT_CRYPTING_BLOCK_MODE);
        Cipher ballotCipher = new CipherFactory(propertyConfigurationService1).getInstance(ballotCipherAlgo + ballotBlockmode);
        when(ballotCiphersProvider.getBallotCipher()).thenReturn(ballotCipher);

        // Define ballotCipher key size
        int ballotCipherSize = 256;
        when(ballotCiphersProvider.getBallotCipherSize()).thenReturn(ballotCipherSize);

        // Instantiate ballotKeyCipher

        String ballotKeyCipherAlgo = propertyConfigurationService1.getConfigValue(BALLOT_KEY_CRYPTING_ALGORITHM);
        String ballotKeyCipherBlockmode = propertyConfigurationService1.getConfigValue(BALLOT_KEY_CRYPTING_BLOCKMODE);
        Cipher ballotKeyCipher = new CipherFactory(propertyConfigurationService1).getInstance(ballotKeyCipherAlgo + ballotKeyCipherBlockmode);
        when(ballotCiphersProvider.getBallotKeyCipher()).thenReturn(ballotKeyCipher);


        String ballotIntegrityAlgo = propertyConfigurationService1.getConfigValue(BALLOT_INTEGRITY_CHECK_CRYPTING_ALGORITHM);
        String ballotIntegrityBlockmode = propertyConfigurationService1.getConfigValue(BALLOT_INTEGRITY_CHECK_CRYPTING_BLOCK_MODE);
        Cipher integrityCipher = new CipherFactory(propertyConfigurationService1).getInstance(ballotIntegrityAlgo + ballotIntegrityBlockmode);
        when(ballotCiphersProvider.getIntegrityCipher(propertyConfigurationService)).thenReturn(integrityCipher);

        when(ballotCiphersProvider.getMacLength()).thenReturn(128);

        // Instantiate public key
        final InputStream publicKeyStream = BallotCipherServiceTest.class.getResourceAsStream("/ctrl.der");
        java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X509");
        X509Certificate certPublic = (X509Certificate) cf.generateCertificate(publicKeyStream);
        Key ballotKeyCipherPublicKey = certPublic.getPublicKey();
        when(ballotCiphersProvider.getBallotKeyCipherPublicKey()).thenReturn(ballotKeyCipherPublicKey);

        // Instantiate secret key
        final InputStream integrityKeyStream = BallotCipherServiceTest.class.getResourceAsStream("/integrity.key");
        ObjectInputStream ois = new ObjectInputStream(integrityKeyStream);
        Key integrityCheckSecretKey = (Key) ois.readObject();
        ois.close();
        when(ballotCiphersProvider.getIntegrityCheckSecretKey()).thenReturn(integrityCheckSecretKey);
    }

    private void initializePrivateKey() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        // Instantiate private key
        final InputStream privateKeyStream = BallotCipherServiceTest.class.getResourceAsStream("/ctrl.p12");
        KeyStore caKs = CertificateUtils.createPKCS12KeyStore();
        caKs.load(privateKeyStream, "testtest".toCharArray());
        Key ballotKeyCipherPrivateKey = caKs.getKey("ctrl", "testtest".toCharArray());
        when(ballotCiphersProvider.getBallotKeyCipherPrivateKey()).thenReturn(ballotKeyCipherPrivateKey);
    }

    /**
     * loadBallotKeyCipherPrivateKey should be delegated to BallotCiphersProvider
     */
    @Test
    public void testLoadBallotKeyCipherPrivateKey() throws Exception {
        ballotCipherService.loadBallotKeyCipherPrivateKey("password");
        verify(ballotCiphersProvider, times(1)).loadBallotKeyCipherPrivateKey("password");
    }

    /**
     * setPrivateKeyFileName should be delegated to BallotCiphersProvider
     */
    @Test
    public void testSetPrivateKeyFileName() throws Exception {
        ballotCipherService.setPrivateKeyFileName("priv_key");
        verify(ballotCiphersProvider, times(1)).invalidatePrivateKeyCache();
    }

    /**
     * setPublicKeyFileName should be delegated to BallotCiphersProvider
     */
    @Test
    public void testSetPublicKeyFileName() throws Exception {
        ballotCipherService.setPublicKeyFileName("pub_key");
        verify(ballotCiphersProvider, times(1)).invalidatePublicKeyCache();
    }

    /**
     * setIntegrityKeyFileName should be delegated to BallotCiphersProvider
     */
    @Test
    public void testSetIntegrityKeyFileName() throws Exception {
        ballotCipherService.setIntegrityKeyFileName("integrity_key");
        verify(ballotCiphersProvider, times(1)).invalidateIntegrityKeyCache();
    }
}
