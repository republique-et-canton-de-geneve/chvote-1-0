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

package ch.ge.ve.commons.crypto

import ch.ge.ve.commons.crypto.exceptions.CryptoConfigurationRuntimeException
import ch.ge.ve.commons.properties.PropertyConfigurationService
import org.bouncycastle.jce.provider.BouncyCastleProvider
import spock.lang.Specification

import javax.crypto.Cipher
import java.security.Security
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask

/**
 * This test suit aims at covering the default implementation for a CryptoUtilsConfiguration.
 */
class SensitiveDataCryptoUtilsConfigurationDefaultImplTest extends Specification {
    private PropertyConfigurationService pcs

    void setup() {
        Security.addProvider(new BouncyCastleProvider())
        pcs = new PropertyConfigurationService()
    }

    def "getCipher should return the same relevant cipher when calling twice from the same thread"() {
        given:
        def sut = new SensitiveDataCryptoUtilsConfigurationDefaultImpl(pcs)

        when:
        def cipher1 = sut.getCipher()
        def cipher2 = sut.getCipher()

        then:
        cipher1.getAlgorithm() == "AES/GCM/NoPadding"
        cipher1.getProvider().getName() == "BC"
        cipher1 == cipher2
    }

    def "getCipher should return different ciphers when calling from different threads as the class is not thread safe"() {
        given:
        def sut = new SensitiveDataCryptoUtilsConfigurationDefaultImpl(pcs)

        when:
        def future1 = createCipherTask(sut)
        def future2 = createCipherTask(sut)

        Executors.newSingleThreadExecutor().execute(future1)
        Executors.newSingleThreadExecutor().execute(future2)

        def cipher1 = future1.get()
        def cipher2 = future2.get()

        then:
        cipher1 != cipher2
    }

    private static FutureTask<Cipher> createCipherTask(sut) {
        new FutureTask<>(new Callable<Cipher>() {
            @Override
            Cipher call() throws Exception {
                return sut.getCipher()
            }
        })
    }

    def "getMac should return the same relevant mac when calling twice from the same thread"() {
        given:
        def sut = new SensitiveDataCryptoUtilsConfigurationDefaultImpl(pcs)

        when:
        def mac1 = sut.getMac()
        def mac2 = sut.getMac()

        then:
        mac1.getAlgorithm() == "HmacSHA256"
        mac1.getProvider().getName() == "SunJCE"
        mac1 == mac2
    }

    def "getMac should return different macs when calling from different threads as the class is not thread safe"() {
        given:
        def sut = new SensitiveDataCryptoUtilsConfigurationDefaultImpl(pcs)

        when:
        def future1 = createMacTask(sut)
        def future2 = createMacTask(sut)

        Executors.newSingleThreadExecutor().execute(future1)
        Executors.newSingleThreadExecutor().execute(future2)

        def mac1 = future1.get()
        def mac2 = future2.get()

        then:
        mac1 != mac2
    }

    private static FutureTask<Cipher> createMacTask(sut) {
        new FutureTask<>(new Callable<Cipher>() {
            @Override
            Cipher call() throws Exception {
                return sut.getCipher()
            }
        })
    }

    def "getSecretKey should throw exception if key path is not defined in configuration"() {
        given:
        def sut = new SensitiveDataCryptoUtilsConfigurationDefaultImpl(pcs)

        when:
        sut.getSecretKey()

        then:
        thrown(CryptoConfigurationRuntimeException)
    }

    def "getSecretKey should throw exception if configured key path is not found"() {
        given:
        pcs = Spy(PropertyConfigurationService)
        pcs.getConfigValue("password.hmac.key.filename") >> "my-key-does-not-exist.key"
        def sut = new SensitiveDataCryptoUtilsConfigurationDefaultImpl(pcs)

        when:
        sut.getSecretKey()

        then:
        thrown(CryptoConfigurationRuntimeException)
    }

    def "getSecretKey should return the relevant key if configured key path is found"() {
        given:
        pcs = Spy(PropertyConfigurationService)
        pcs.getConfigValue("password.hmac.key.filename") >> "integrity.key"
        def sut = new SensitiveDataCryptoUtilsConfigurationDefaultImpl(pcs)

        when:
        def secretKey = sut.getSecretKey()

        then:
        secretKey.getAlgorithm() == "AES"
    }

    def "getIterations should return a relevant number of iterations for PBKDF"() {
        given:
        def sut = new SensitiveDataCryptoUtilsConfigurationDefaultImpl(pcs)

        when:
        def iterations = sut.getIterations()

        then:
        iterations >= 34000
        iterations <= 36000
    }
}
