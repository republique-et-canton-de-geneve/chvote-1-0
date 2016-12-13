package ch.ge.ve.commons.streamutils

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

import com.google.common.collect.Lists
import spock.lang.Specification

/**
 * This test suit aims at covering the {@link SafeObjectReader} utility class.
 */
class SafeObjectReaderTest extends Specification {

    def "read of a simple object should succeed"() {
        given:
        def simpleSample = new SimpleSampleClass(42, 42L, true)

        def byteArrayOutputStream = new ByteArrayOutputStream()
        def objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)

        objectOutputStream.writeObject(simpleSample)

        when:
        def inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray())
        def safeReadObject = SafeObjectReader.safeReadObject(SimpleSampleClass, new ArrayList<>(), 1, 136, inputStream)

        then:
        safeReadObject.someInt == simpleSample.someInt
        safeReadObject.someLong == simpleSample.someLong
        safeReadObject.someBoolean == simpleSample.someBoolean
    }

    def "read of an unsafe object should fail"() {
        given:
        def simpleSample = new SimpleSampleClass(42, 42L, true)
        def nestedSample = new NestedSampleClass(simpleSample)

        def byteArrayOutputStream = new ByteArrayOutputStream()
        def objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)

        objectOutputStream.writeObject(nestedSample)

        when:
        def inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray())
        SafeObjectReader.safeReadObject(NestedSampleClass, new ArrayList<>(), 2, 312, inputStream)

        then:
        thrown(SafeObjectSecurityRuntimeException)
    }

    def "read of a nested object with appropriate permissions should succeed"() {
        given:
        def simpleSample = new SimpleSampleClass(42, 42L, true)
        def nestedSample = new NestedSampleClass(simpleSample)

        def byteArrayOutputStream = new ByteArrayOutputStream()
        def objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)

        objectOutputStream.writeObject(nestedSample)

        when:
        def inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray())
        def nestedReadSample = SafeObjectReader.safeReadObject(NestedSampleClass, Lists.asList(SimpleSampleClass), 2, 312, inputStream)

        then:
        nestedReadSample.simpleSampleClass.someInt == simpleSample.someInt

    }

    private static class SimpleSampleClass implements Serializable {
        private static final long serialVersionUID = 1L;

        final int someInt;
        final long someLong;
        final boolean someBoolean;

        SimpleSampleClass(int someInt, long someLong, boolean someBoolean) {
            this.someInt = someInt
            this.someLong = someLong
            this.someBoolean = someBoolean
        }
    }

    private static class NestedSampleClass implements Serializable {
        private static final long serialVersionUID = 1L;

        final SimpleSampleClass simpleSampleClass;

        NestedSampleClass(SimpleSampleClass simpleSampleClass) {
            this.simpleSampleClass = simpleSampleClass
        }
    }
}
