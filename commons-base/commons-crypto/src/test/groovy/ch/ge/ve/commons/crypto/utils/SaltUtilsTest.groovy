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

import spock.lang.Specification
import spock.lang.Unroll

/**
 * This test suit aims at covering the {@link SaltUtils} utility class.
 */
class SaltUtilsTest extends Specification {

    def "the requested salt length in bits should be a multiple of 8"() {
        when:
        SaltUtils.generateSalt(7)

        then:
        thrown(IllegalArgumentException)
    }

    @Unroll
    def "the generated salt length should be the requested length in bits divided by 8: #inputLength >> #generatedLength"() {
        expect:
        SaltUtils.generateSalt(inputLength).length == generatedLength

        where:
        inputLength || generatedLength
        0           || 0
        8           || 1
        16          || 2
    }
}
