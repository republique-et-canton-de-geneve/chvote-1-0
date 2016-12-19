package ch.ge.ve.offlineadmin.services

import ch.ge.ve.commons.properties.PropertyConfigurationService

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
import spock.lang.Specification

import java.security.interfaces.RSAKey
import java.time.Instant
import java.time.temporal.ChronoUnit

import static ch.ge.ve.offlineadmin.util.SecurityConstants.*

/**
 * This test suit aims at covering the {@link KeyGenerator} service.
 */
class KeyGeneratorTest extends Specification {

    static final int INTEGRITY_KEY_SIZE_IN_BYTES = 32
    static final int BALLOT_ENCRYPTION_KEY_SIZE_IN_BYTES = 256

    private PropertyConfigurationService conf

    void setup() {
        Properties props = new Properties()
        props.setProperty(CERT_COMMON_NAME_PROPERTY, "My common name")
        props.setProperty(CERT_ORGANISATION_PROPERTY, "My organisation")
        props.setProperty(CERT_ORGANISATIONAL_UNIT_PROPERTY, "My organisational unit")
        props.setProperty(CERT_COUNTRY_PROPERTY, "Switzerland")
        props.setProperty(CERT_VALIDITY_DAYS_PROPERTY, "365")
        props.setProperty(CERT_PRIVATE_FRIENDLY_NAME_PROPERTY, "My cert")
        props.setProperty(CERT_HASH_ALGORITHM, "SHA256")

        conf = new PropertyConfigurationService(props)
    }

    def "The secret key generation should respect the given properties"() {
        given:
        def generator = new KeyGenerator(conf)

        when:
        def secretKey = generator.generateSecretKey()

        then:
        secretKey.getAlgorithm() == "AES"
        secretKey.getEncoded().length == INTEGRITY_KEY_SIZE_IN_BYTES
    }

    def "Two consecutive secret key generation calls must return different keys"() {
        given:
        def generator = new KeyGenerator(conf)

        when:
        def secretKey1 = generator.generateSecretKey()
        def secretKey2 = generator.generateSecretKey()

        then:
        secretKey1.getEncoded() != secretKey2.getEncoded()
    }

    def "The ballot box encryption key pair generation should respect the given properties"() {
        given:
        def generator = new KeyGenerator(conf)

        when:
        def secretKey = generator.generateKeyPair()

        then:
        secretKey.getPublic().getAlgorithm() == "RSA"
        ((RSAKey)secretKey.getPublic()).getModulus().bitLength() == BALLOT_ENCRYPTION_KEY_SIZE_IN_BYTES * 8

        secretKey.getPrivate().getAlgorithm() == "RSA"
        ((RSAKey)secretKey.getPrivate()).getModulus().bitLength() == BALLOT_ENCRYPTION_KEY_SIZE_IN_BYTES * 8
    }

    def "Two consecutive ballot box encryption key pair generation calls must return different keys"() {
        given:
        def generator = new KeyGenerator(conf)

        when:
        def secretKey1 = generator.generateKeyPair()
        def secretKey2 = generator.generateKeyPair()

        then:
        secretKey1.getPublic().getEncoded() != secretKey2.getPublic().getEncoded()
        secretKey1.getPrivate().getEncoded() != secretKey2.getPrivate().getEncoded()
    }

    def "Generate a valid certificate"() {
        given:
        def generator = new KeyGenerator(conf)
        def keyPair = generator.generateKeyPair()

        // the certificate generation truncates the milliseconds, so just check one minute sooner
        def before = Instant.now().minus(1, ChronoUnit.MINUTES)

        when:
        def certificate = generator.generateCertificate(keyPair)
        def after = Instant.now().plus(365, ChronoUnit.DAYS)

        then:
        certificate.getSubjectDN().getName() == "C=Switzerland, OU=My organisational unit, O=My organisation, CN=My common name"
        before.isBefore(certificate.getNotBefore().toInstant())
        after.isAfter(certificate.getNotAfter().toInstant())
    }

    def "Create a key store protected by a password"() {
        given:
        def generator = new KeyGenerator(conf)
        def keyPair = generator.generateKeyPair()
        def certificate = generator.generateCertificate(keyPair)
        def password = "password".toCharArray()

        when:
        def keyStore = generator.createKeyStore(keyPair.getPrivate(), certificate, password)

        then:
        keyStore.getCertificate("ctrl").getPublicKey().equals(keyPair.getPublic())
        keyStore.getKey("ctrl", password).equals(keyPair.getPrivate())
    }
}
