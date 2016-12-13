package ch.ge.ve.offlineadmin.services

import ch.ge.ve.commons.crypto.exceptions.PrivateKeyPasswordMismatchException
import ch.ge.ve.commons.properties.PropertyConfigurationService
import spock.lang.Specification

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
import static ch.ge.ve.offlineadmin.util.SecurityConstants.CERT_PRIVATE_KEY_FILENAME_PATTERN

/**
 * This test suit aims at covering the {@link DecryptionBallotCiphersProvider} provider.
 */
class DecryptionBallotCiphersProviderTest extends Specification {

    private File keysDir
    private PropertyConfigurationService conf

    void setup() {
        keysDir = new File(this.getClass().getClassLoader().getResource("keys").toURI())

        Properties props = new Properties()
        props.setProperty(CERT_PRIVATE_KEY_FILENAME_PATTERN, "private_key.*\\.p12")

        conf = new PropertyConfigurationService(props)
    }

    def "Load an existing ballot encryption key then invalidate the cache"() {
        given:
        def provider = new DecryptionBallotCiphersProvider(keysDir)
        provider.setPropertyConfigurationService(conf)

        when: "the key is loaded"
        provider.loadBallotKeyCipherPrivateKey("Test123456Test123456")
        def privateKey1 = provider.getBallotKeyCipherPrivateKey()

        then: "the key should have been loaded"
        privateKey1 != null
        privateKey1.getAlgorithm() == "RSA"

        when: "the cache is invalidated"
        provider.invalidatePrivateKeyCache()
        def privateKey2 = provider.getBallotKeyCipherPrivateKey()

        then: "the key should be cleared"
        privateKey2 == null
    }

    def "Throw an exception if the password does not match"() {
        given:
        def provider = new DecryptionBallotCiphersProvider(keysDir)
        provider.setPropertyConfigurationService(conf)

        when: "the key is loaded"
        provider.loadBallotKeyCipherPrivateKey("Test123456Test123457")

        then: "the key should not have been loaded"
        provider.getBallotKeyCipherPrivateKey() == null

        and: "an exception should be thrown"
        thrown(PrivateKeyPasswordMismatchException)
    }

}
