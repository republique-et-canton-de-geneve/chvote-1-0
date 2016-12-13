/*
 * -
 * #%L
 * Common crypto utilities
 * %%
 * Copyright (C) 2016 République et Canton de Genève
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

package ch.ge.ve.commons.crypto.ballot

import ch.ge.ve.commons.crypto.exceptions.PrivateKeyPasswordMismatchException
import ch.ge.ve.commons.properties.PropertyConfigurationService
import org.bouncycastle.jce.provider.BouncyCastleProvider
import spock.lang.Specification

import java.security.Key
import java.security.Security

/**
 * This test suit aims at covering the default implementations of the {@link BallotCiphersProviderDefaultImpl} class.
 */
class BallotCiphersProviderDefaultImplTest extends Specification {

    void setup() {
        Security.addProvider(new BouncyCastleProvider())
    }

    def "ballot cipher attributes should match the configured values"() {
        given:
        def pcs = Stub(PropertyConfigurationService)
        pcs.getConfigValue(BallotCiphersProvider.BALLOT_CRYPTING_ALGORITHM) >> "AES"
        pcs.getConfigValue(BallotCiphersProvider.BALLOT_CRYPTING_BLOCK_MODE) >> "/GCM/NoPadding"
        pcs.getConfigValue(BallotCiphersProvider.BALLOT_CRYPTING_KEY_SIZE) >> "256"
        pcs.getConfigValue("common.crypto.ciphers.list") >> "AES/GCM/NoPadding"
        pcs.getConfigValue("common.crypto.security.provider.AES/GCM/NoPadding") >> "BC"

        def ballotCipherProvider = new BallotCiphersProviderDefaultTestImpl()
        ballotCipherProvider.setPropertyConfigurationService(pcs)

        when:
        def cipher = ballotCipherProvider.getBallotCipher()
        def size = ballotCipherProvider.getBallotCipherSize()

        then:
        cipher.getAlgorithm() == "AES/GCM/NoPadding"
        cipher.getProvider().getName() == "BC"
        size == 256
    }

    def "ballot key cipher attributes should match the configured values"() {
        given:
        def pcs = Stub(PropertyConfigurationService)
        pcs.getConfigValue(BallotCiphersProvider.BALLOT_KEY_CRYPTING_ALGORITHM) >> "RSA"
        pcs.getConfigValue(BallotCiphersProvider.BALLOT_KEY_CRYPTING_BLOCKMODE) >> "/ECB/OAEPWithSHA1AndMGF1Padding"
        pcs.getConfigValue("common.crypto.ciphers.list") >> "RSA/ECB/OAEPWithSHA1AndMGF1Padding"
        pcs.getConfigValue("common.crypto.security.provider.RSA/ECB/OAEPWithSHA1AndMGF1Padding") >> "SunJCE"

        def ballotCipherProvider = new BallotCiphersProviderDefaultTestImpl()
        ballotCipherProvider.setPropertyConfigurationService(pcs)

        when:
        def cipher = ballotCipherProvider.getBallotKeyCipher()

        then:
        cipher.getAlgorithm() == "RSA/ECB/OAEPWithSHA1AndMGF1Padding"
        cipher.getProvider().getName() == "SunJCE"
    }

    def "should get a AEADCipher API"() {
        given:
        def ballotCipherProvider = new BallotCiphersProviderDefaultTestImpl()
        def pcs = Mock(PropertyConfigurationService)
        pcs.getConfigValue(BallotCiphersProvider.BALLOT_INTEGRITY_CHECK_CRYPTING_ALGORITHM) >> "AES"
        pcs.getConfigValue(BallotCiphersProvider.BALLOT_INTEGRITY_CHECK_CRYPTING_BLOCK_MODE) >> "/GCM/NoPadding"
        pcs.getConfigValue("common.crypto.ciphers.list") >> "AES/GCM/NoPadding"
        pcs.getConfigValue("common.crypto.security.provider.AES/GCM/NoPadding") >> "BC"

        when:
        def cipher = ballotCipherProvider.getIntegrityCipher(pcs)

        then:
        cipher.getAlgorithm() == "AES/GCM/NoPadding"
    }

    def "should return the same ballot public key on subsequent calls, unless the source is changed and the cache is invalidated"() {
        given:
        def pcs = Stub(PropertyConfigurationService)
        pcs.getConfigValue(BallotCiphersProvider.PUBLIC_KEY_FILE_NAME) >> this.getClass().getClassLoader().getResource("ctrl.der").getFile()

        def ballotCipherProvider = new BallotCiphersProviderDefaultTestImpl()
        ballotCipherProvider.setPropertyConfigurationService(pcs)

        when: "the configuration is changed between two calls"
        def key1 = ballotCipherProvider.getBallotKeyCipherPublicKey()

        pcs = Stub(PropertyConfigurationService)
        pcs.getConfigValue(BallotCiphersProvider.PUBLIC_KEY_FILE_NAME) >> this.getClass().getClassLoader().getResource("public_key_user_2016-04-19-12h07m32s.der").getFile()
        ballotCipherProvider.setPropertyConfigurationService(pcs)

        def key2 = ballotCipherProvider.getBallotKeyCipherPublicKey()

        then: "the key is not reloaded even if the configuration has been modified"

        key1 == key2
        key2.getAlgorithm() == "RSA"

        when: "the cache is invalidated"
        ballotCipherProvider.invalidatePublicKeyCache()
        def key3 = ballotCipherProvider.getBallotKeyCipherPublicKey()

        then: "this time, the key is reloaded"
        key3 != key2
    }

    def "should return the injected configuration service"() {
        given:
        def pcs = Stub(PropertyConfigurationService)
        def ballotCipherProvider = new BallotCiphersProviderDefaultTestImpl()
        ballotCipherProvider.setPropertyConfigurationService(pcs)

        when:
        def propertyConfigurationService = ballotCipherProvider.getPropertyConfigurationService()

        then:
        propertyConfigurationService == pcs
    }

    def "should return the same ballot integrity key on subsequent calls, unless the source is changed and the cache is invalidated"() {
        given:
        def pcs = Stub(PropertyConfigurationService)
        pcs.getConfigValue(BallotCiphersProvider.INTEGRITY_KEY_FILE_NAME) >> this.getClass().getClassLoader().getResource("integrity.key").getFile()

        def ballotCipherProvider = new BallotCiphersProviderDefaultTestImpl()
        ballotCipherProvider.setPropertyConfigurationService(pcs)

        when: "the configuration is changed between two calls"
        def key1 = ballotCipherProvider.getIntegrityCheckSecretKey()

        pcs = Stub(PropertyConfigurationService)
        pcs.getConfigValue(BallotCiphersProvider.INTEGRITY_KEY_FILE_NAME) >> this.getClass().getClassLoader().getResource("integrity_key_user_2016-04-19-12h07m32s.key").getFile()
        ballotCipherProvider.setPropertyConfigurationService(pcs)

        def key2 = ballotCipherProvider.getIntegrityCheckSecretKey()

        then: "the key is not reloaded even if the configuration has been modified"

        key1 == key2
        key2.getAlgorithm() == "AES"

        when: "the cache is invalidated"
        ballotCipherProvider.invalidateIntegrityKeyCache()
        def key3 = ballotCipherProvider.getIntegrityCheckSecretKey()

        then: "this time, the key is reloaded"
        key3 != key2
    }

    def "GCM MAC length should be 128 bits"() {
        given:
        def ballotCipherProvider = new BallotCiphersProviderDefaultTestImpl()

        when:
        def macLength = ballotCipherProvider.getMacLength()

        then:
        macLength == 128
    }

    def "Invalidate private key cache is not implemented"() {
        given:
        def ballotCipherProvider = new BallotCiphersProviderDefaultTestImpl()

        when:
        ballotCipherProvider.invalidatePrivateKeyCache()

        then:
        thrown(UnsupportedOperationException)
    }

    private class BallotCiphersProviderDefaultTestImpl extends BallotCiphersProviderDefaultImpl {
        @Override
        Key getBallotKeyCipherPrivateKey() {
            return null
        }

        @Override
        void loadBallotKeyCipherPrivateKey(String password) throws PrivateKeyPasswordMismatchException {

        }
    }
}
