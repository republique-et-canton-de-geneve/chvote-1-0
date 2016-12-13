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
import ch.ge.ve.commons.crypto.utils.CipherFactory;
import ch.ge.ve.commons.crypto.utils.SecureRandomFactory;
import ch.ge.ve.commons.properties.PropertyConfigurationException;
import ch.ge.ve.commons.properties.PropertyConfigurationService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.*;

import static ch.ge.ve.commons.crypto.SensitiveDataCryptoUtilsConfigurationDefaultImpl.*;

/**
 * This test suit aims at covering the {@link ObjectSealer} utility class.
 */
public class ObjectSealerTest {
    private PropertyConfigurationService pcs = new PropertyConfigurationService();
    private String algo;

    private ObjectSealer objectSealer;
    private Cipher cipher;

    @BeforeClass
    public static void init() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Before
    public void setUp() throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, PropertyConfigurationException {
        algo = pcs.getConfigValue(COMMON_CRYPTO_STORAGE_ALGORITHM);
        String blockMode = pcs.getConfigValue(COMMON_CRYPTO_STORAGE_BLOCKMODE);
        cipher = new CipherFactory(new PropertyConfigurationService()).getInstance(algo + blockMode);
        SecureRandom random = SecureRandomFactory.createPRNG();
        byte[] keyBytes = new byte[cipher.getBlockSize()];
        random.nextBytes(keyBytes);
        SecretKey key = new SecretKeySpec(keyBytes, algo);

        objectSealer = new ObjectSealer(cipher, key);
    }

    /**
     * seal() followed by unseal() should give the same object.
     */
    @Test
    public void sealUnsealShouldBeIdentity() throws InvalidKeyException, IOException, IllegalBlockSizeException, NoSuchAlgorithmException, ClassNotFoundException, PropertyConfigurationException {
        String someString = "Whichever string, it doesn't really matter...";
        byte[] sealedObject = objectSealer.sealObject(someString);

        Object o = objectSealer.unsealObject(sealedObject, pcs.getConfigValueAsLong(COMMON_CRYPTO_STREAM_MAX_BYTES));

        MatcherAssert.assertThat("Type shouldn't be lost when sealing", o, Matchers.instanceOf(String.class));
        MatcherAssert.assertThat("Unseal of sealed object should provide the original object", o, Matchers.is(someString));
    }

    /**
     * unseal() with a different key should raise an error.
     */
    @Test(expected = CryptoOperationRuntimeException.class)
    public void unsealWithAnotherKeyShouldFail() throws InvalidKeyException, IOException, IllegalBlockSizeException, NoSuchAlgorithmException, ClassNotFoundException, PropertyConfigurationException {
        String someString = "This is the secret we're trying to protect";
        byte[] sealedObject = objectSealer.sealObject(someString);

        SecureRandom random = SecureRandomFactory.createPRNG();
        byte[] otherKeyBytes = new byte[cipher.getBlockSize()];
        random.nextBytes(otherKeyBytes);
        SecretKey otherKey = new SecretKeySpec(otherKeyBytes, algo);

        ObjectSealer otherObjectSealer = new ObjectSealer(cipher, otherKey);
        otherObjectSealer.unsealObject(sealedObject, pcs.getConfigValueAsLong(COMMON_CRYPTO_STREAM_MAX_BYTES));
    }
}