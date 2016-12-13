package ch.ge.ve.offlineadmin.services

import ch.ge.ve.commons.properties.PropertyConfigurationService

/*-
* #%L
 * * Admin offline
 * *
 * %%
 * Copyright (C) 2015 - 2016 République et Canton de Genève
 * *
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
import ch.ge.ve.offlineadmin.exception.MissingKeyFilesException
import org.bouncycastle.jce.provider.BouncyCastleProvider
import spock.lang.Specification

import java.security.Security

import static ch.ge.ve.offlineadmin.util.SecurityConstants.*

/**
 * This test suit aims at covering the {@link BallotCipherServiceFactory} factory.
 */
class BallotCipherServiceFactoryTest extends Specification {
    private static final String KEY_PASSWORD = "Test123456Test123456"

    private File keysDir = new KeysProvider().getTestKeysDir()
    private Properties props
    private PropertyConfigurationService conf

    void setup() {
        Security.addProvider(new BouncyCastleProvider());

        props = new Properties()
        props.setProperty(CERT_PUBLIC_KEY_FILENAME_PATTERN, "public_key.*\\.der")
        props.setProperty(CERT_PRIVATE_KEY_FILENAME_PATTERN, "private_key.*\\.p12")
        props.setProperty(INTEGRITY_KEY_FILENAME_PATTERN, "integrity_key.*\\.key")

        conf = new PropertyConfigurationService(props)
    }

    def "The factory should provide two appropriate services to encrypt then decrypt a ballot content"() {
        given:
        def factory = new BallotCipherServiceFactory(conf)
        def ballotContent = "my ballot"

        when:
        def encryptionService = factory.encryptionBallotCipherService(keysDir)
        def encryptedBallot = encryptionService.encryptBallotThenWrapForAuthentication(ballotContent, 5)

        then:
        encryptedBallot.getBallotIndex() == 5

        when:
        def decryptionService = factory.decryptionBallotCipherService(keysDir)
        decryptionService.loadBallotKeyCipherPrivateKey(KEY_PASSWORD)
        def encryptedBallotAndWrappedKey = decryptionService.verifyAuthenticationThenUnwrap(encryptedBallot)
        def decryptedBallot = decryptionService.decryptBallot(encryptedBallotAndWrappedKey)

        then:
        decryptedBallot == ballotContent
    }

    def "The factory should provide an encryption service that do not allow to register the private key"() {
        given:
        def factory = new BallotCipherServiceFactory(conf)

        when:
        def encryptionService = factory.encryptionBallotCipherService(keysDir)
        encryptionService.loadBallotKeyCipherPrivateKey(KEY_PASSWORD)

        then:
        thrown UnsupportedOperationException
    }

    def "The factory should throw a MissingKeyFilesException if the key directory does not contain one of the expected keys"() {
        given:
        def localProps = new Properties(props)
        localProps.setProperty(CERT_PUBLIC_KEY_FILENAME_PATTERN, "my_public_key.*\\.der")
        localProps.setProperty(CERT_PRIVATE_KEY_FILENAME_PATTERN, "my_private_key.*\\.der")
        localProps.setProperty(INTEGRITY_KEY_FILENAME_PATTERN, "unknown_integrity_key.*\\.key")

        def localConf = new PropertyConfigurationService(localProps)
        def factory = new BallotCipherServiceFactory(localConf)

        when:
        factory.encryptionBallotCipherService(keysDir)

        then:
        thrown MissingKeyFilesException

        when:
        factory.decryptionBallotCipherService(keysDir)

        then:
        thrown MissingKeyFilesException
    }



}
