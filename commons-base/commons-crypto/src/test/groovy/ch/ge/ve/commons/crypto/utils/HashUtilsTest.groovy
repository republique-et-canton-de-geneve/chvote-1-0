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

import ch.ge.ve.commons.properties.PropertyConfigurationService
import org.bouncycastle.jce.provider.BouncyCastleProvider
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.charset.StandardCharsets
import java.security.Security

/**
 * This test suit aims at covering the {@link HashUtils} utility class.
 */
class HashUtilsTest extends Specification {
    private PropertyConfigurationService pcs
    private HashUtils hashUtils

    void setup() {
        Security.addProvider(new BouncyCastleProvider())

        pcs = Stub(PropertyConfigurationService)
        pcs.getConfigValue("common.crypto.digest.algorithm") >> "SHA-256"
        pcs.getConfigValue("common.crypto.digest.provider") >> "BC"

        hashUtils = new HashUtils(pcs)
    }

    @Unroll
    def "compute the hash of a byte array (#usecase)"() {
        expect:
        output == hashUtils.computeHash(input.getBytes(StandardCharsets.UTF_8)).encodeHex().toString()

        where:
        //  using shell to generate expected values: echo -n "A small text" | sha256sum
        usecase        | input                               || output
        "short string" | "A small text"                      || "319f4265092ec13b8728b66784489839199aced000672eb305d59540fe9c47ee"
        "long string"  | "Déchiffrement des données en échec, merci de vérifier " +
                "que le fichier utilisé est bien correct et " +
                "que les mots de passe saisis sont corrects" || "6b4415b47c939228df94dda0ca2b95ea1f3abcf304f07ac5dc9d79ae02f6849f"

    }

    def "compute the hash of a file"() {
        given:
        def file = HashUtils.class.getClassLoader().getResource("integrity.key").getFile()

        when:
        def output = hashUtils.computeFileHash(file).encodeHex().toString()

        then:
        //  using shell to generate expected values: sha256sum <file>
        output == "cfe9824d7c201e207fd5fe11007173ae9ce78b6273f045f2b1529382fe3dfaa8"
    }
}
