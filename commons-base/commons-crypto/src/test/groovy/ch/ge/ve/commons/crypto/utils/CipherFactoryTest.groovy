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

package ch.ge.ve.commons.crypto.utils

import ch.ge.ve.commons.crypto.exceptions.CryptoConfigurationRuntimeException
import ch.ge.ve.commons.properties.PropertyConfigurationService
import org.bouncycastle.jce.provider.BouncyCastleProvider
import spock.lang.Specification

import java.security.Security

/**
 * This test suit aims at covering the {@link CipherFactory} class.
 */
class CipherFactoryTest extends Specification {
    private PropertyConfigurationService pcs

    void setup() {
        Security.addProvider(new BouncyCastleProvider())
        pcs = Stub(PropertyConfigurationService)
    }

    def "getInstance with known cipher and provider should return the relevant cipher"() {
        given:
        pcs.getConfigValue("common.crypto.storage.algorithm") >> "AES"
        pcs.getConfigValue("common.crypto.storage.blockmode") >> "/GCM/NoPadding"
        pcs.getConfigValue("common.crypto.ciphers.list") >> "AES/GCM/NoPadding"
        pcs.getConfigValue("common.crypto.security.provider.AES/GCM/NoPadding") >> "BC"
        def sut = new CipherFactory(pcs)

        when:
        def cipher = sut.getInstance("AES/GCM/NoPadding")

        then:
        cipher.getAlgorithm() == "AES/GCM/NoPadding"
        cipher.getProvider().getName() == "BC"
    }

    def "getInstance with unknown provider should throw CryptoConfigurationRuntimeException"() {
        given:
        pcs.getConfigValue("common.crypto.storage.algorithm") >> "CRYPTIX"
        pcs.getConfigValue("common.crypto.storage.blockmode") >> "/CBC"
        pcs.getConfigValue("common.crypto.ciphers.list") >> "CRYPTIX/CBC"
        pcs.getConfigValue("common.crypto.security.provider.CRYPTIX/CBC") >> "CryptoProvider"
        def sut = new CipherFactory(pcs)

        when:
        sut.getInstance("CRYPTIX/CBC")

        then:
        thrown(CryptoConfigurationRuntimeException)
    }

    def "getInstance with unknown cipher should throw CryptoConfigurationRuntimeException"() {
        given:
        pcs.getConfigValue("common.crypto.storage.algorithm") >> "CRYPTIX"
        pcs.getConfigValue("common.crypto.storage.blockmode") >> "/CBC"
        pcs.getConfigValue("common.crypto.ciphers.list") >> "CRYPTIX/CBC"
        pcs.getConfigValue("common.crypto.security.provider.CRYPTIX/CBC") >> "SunJCE"
        def sut = new CipherFactory(pcs)

        when:
        sut.getInstance("CRYPTIX/CBC")

        then:
        thrown(CryptoConfigurationRuntimeException)
    }
}
