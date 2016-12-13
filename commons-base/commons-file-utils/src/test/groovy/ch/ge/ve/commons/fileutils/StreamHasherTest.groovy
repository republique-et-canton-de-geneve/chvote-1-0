package ch.ge.ve.commons.fileutils

/*-
 * #%L
 * Common file utilities
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

import ch.ge.ve.commons.properties.PropertyConfigurationService
import org.apache.commons.io.IOUtils
import org.bouncycastle.jce.provider.BouncyCastleProvider
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.Security

/**
 * This test suit aims at covering the {@link StreamHasher} utility class.
 */
class StreamHasherTest extends Specification {
    private PropertyConfigurationService pcs
    private StreamHasher streamHasher

    void setup() {
        Security.addProvider(new BouncyCastleProvider())

        pcs = Stub(PropertyConfigurationService)
        pcs.getConfigValue("common.crypto.digest.algorithm") >> "SHA-256"
        pcs.getConfigValue("common.crypto.digest.provider") >> "BC"

        streamHasher = new StreamHasher(pcs)
    }

    @Unroll
    def "compute the hash of a byte array with the default configured digest (#usecase)"() {
        expect:
        def inputStream = IOUtils.toInputStream(input, StandardCharsets.UTF_8);
        output == streamHasher.threadSafeComputeHash(inputStream).encodeHex().toString()

        where:
        //  using shell to generate expected values: echo -n "A small text" | sha256sum
        usecase        | input                               || output
        "short string" | "A small text"                      || "319f4265092ec13b8728b66784489839199aced000672eb305d59540fe9c47ee"
        "long string"  | "Déchiffrement des données en échec, merci de vérifier " +
                "que le fichier utilisé est bien correct et " +
                "que les mots de passe saisis sont corrects" || "6b4415b47c939228df94dda0ca2b95ea1f3abcf304f07ac5dc9d79ae02f6849f"

    }

    @Unroll
    def "compute the hash of a byte array with a specific digest (#usecase)"() {
        given:
        def messageDigest = MessageDigest.getInstance("SHA-1", "BC")

        expect:
        def inputStream = IOUtils.toInputStream(input, StandardCharsets.UTF_8);

        output == streamHasher.computeHash(inputStream, messageDigest).encodeHex().toString()

        where:
        //  using shell to generate expected values: echo -n "A small text" | sha1sum
        usecase        | input                               || output
        "short string" | "A small text"                      || "166c10125e8b603454d6eb8bd3a4d470ce25cde4"
        "long string"  | "Déchiffrement des données en échec, merci de vérifier " +
                "que le fichier utilisé est bien correct et " +
                "que les mots de passe saisis sont corrects" || "a0230150333d84ce7530d232f6058f02b8e980e8"
    }
}
