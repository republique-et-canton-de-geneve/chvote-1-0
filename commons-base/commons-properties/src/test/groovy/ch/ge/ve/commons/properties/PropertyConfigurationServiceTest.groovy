package ch.ge.ve.commons.properties

/*-
 * #%L
 * Common properties
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
import spock.lang.Unroll

/**
 * This test suite aims at covering the {@link PropertyConfigurationService} service.
 */
class PropertyConfigurationServiceTest extends Specification {

    def "empty constructor should load the properties from the providers found in classpath"() {
        given:
        def pcs = new PropertyConfigurationService()

        when:
        def value = pcs.getConfigValue("crypto.algorithm")

        then:
        value == "AES"
    }

    def "constructor with unknown file path should raise a PropertyConfigurationRuntimeException"() {
        when:
        new PropertyConfigurationService("unknownFile.properties")

        then:
        thrown(PropertyConfigurationRuntimeException)
    }

    def "constructor with file path should raise PropertyConfigurationRuntimeException if the key is defined in the given file and in one of the providers"() {
        when:
        new PropertyConfigurationService("testWithDupKey.properties")

        then:
        thrown(PropertyConfigurationRuntimeException)
    }

    def "constructor with file path should merge the given file properties with the providers properties"() {
        given:
        def pcs = new PropertyConfigurationService("test.properties")

        when:
        def myvalue = pcs.getConfigValue("myvalue")
        def algo = pcs.getConfigValue("crypto.algorithm")

        then:
        myvalue == "Test"
        algo == "AES"

    }

    def "constructor with Properties should not raise PropertyConfigurationRuntimeException if the key is defined in the Properties and in one of the providers"() {
        given:
        def properties = new Properties()
        properties.setProperty("crypto.keysize", "256")
        properties.setProperty("crypto.provider", "BC")

        when: "given two new defaults values"
        def pcs = new PropertyConfigurationService(properties)
        def keysize = pcs.getConfigValue("crypto.keysize")
        def provider = pcs.getConfigValue("crypto.provider")

        then: "keysize is already defined in a provider and do not get overridden"
        keysize == "128"
        provider == "BC"
    }

    def "constructor with Properties should merge the given Properties with the providers properties"() {
        given:
        def properties = new Properties()
        properties.setProperty("myvalue", "Test")
        def pcs = new PropertyConfigurationService(properties)

        when:
        def myvalue = pcs.getConfigValue("myvalue")
        def algo = pcs.getConfigValue("crypto.algorithm")

        then:
        myvalue == "Test"
        algo == "AES"
    }

    def "getConfigValue should raise an exception if the configuration key is unknown"() {
        given:
        def properties = new Properties()
        properties.setProperty("myvalue", "Test")
        def pcs = new PropertyConfigurationService(properties)

        when:
        pcs.getConfigValue("unknown_property")

        then:
        thrown(PropertyConfigurationException)
    }

    def key = "testProp"

    @Unroll
    def "getConfigValueAsInt should return an int if the format is correct for #configValue"() {
        expect:
        expectedResult == getPropertyConfigurationService(key, configValue).getConfigValueAsInt(key)

        where:
        configValue   || expectedResult
        "0"           || 0
        "1"           || 1
        "10000000"    || 10000000
        "-17"         || -17
        "+23"         || 23
        "2147483647"  || Integer.MAX_VALUE
        "-2147483648" || Integer.MIN_VALUE
    }

    @Unroll
    def "getConfigValueAsInt should raise a PropertyConfigurationException if the format is not correct: #configValue"() {
        when:
        getPropertyConfigurationService(key, configValue).getConfigValueAsInt(key)

        then:
        def error = thrown(PropertyConfigurationException)
        error.message == expectedMessage

        where:
        configValue   || expectedMessage
        "a"           || "The value [a] for key [testProp] is not an integer"
        "10.0"        || "The value [10.0] for key [testProp] is not an integer"
        "2147483648"  || "The value [2147483648] for key [testProp] is not an integer"
        "-2147483649" || "The value [-2147483649] for key [testProp] is not an integer"
    }

    @Unroll
    def "getConfigValueAsBoolean should return a boolean if the format is correct for #configValue"() {
        expect:
        expectedResult == getPropertyConfigurationService(key, configValue).getConfigValueAsBoolean(key)

        where:
        configValue || expectedResult
        "true"      || true
        "True"      || true
        "TRUE"      || true
        "false"     || false
        "0"         || false
        "1"         || false
    }

    @Unroll
    def "getConfigValueAsLong should return a long if the format is correct for #configValue"() {
        expect:
        expectedResult == getPropertyConfigurationService(key, configValue).getConfigValueAsLong(key)

        where:
        configValue  || expectedResult
        "0"          || 0L
        "1"          || 1L
        "10000000"   || 10000000L
        "-17"        || -17L
        "+23"        || 23L
        "2147483648" || 2147483648
    }

    @Unroll
    def "getConfigValueAsLong should raise a PropertyConfigurationException if the format is not correct: #configValue"() {
        when:
        getPropertyConfigurationService(key, configValue).getConfigValueAsLong(key)

        then:
        def error = thrown(PropertyConfigurationException)
        error.message == expectedMessage

        where:
        configValue || expectedMessage
        "a"         || "The value [a] for key [testProp] is not a long"
        "10.0"      || "The value [10.0] for key [testProp] is not a long"
    }

    @Unroll
    def "getConfigValueAsArray should return an array of String if the format is correct for #configValue"() {
        expect:
        expectedResult == getPropertyConfigurationService(key, configValue).getConfigValueAsArray(key)

        where:
        configValue             || expectedResult
        "single value"          || ["single value"]
        "0,value with spaces,c" || ["0", "value with spaces", "c"]
        "A, B, C"               || ["A", "B", "C"]
    }

    def "getConfigValueAsArray should raise an exception if the configuration key is unknown"() {
        given:
        def properties = new Properties()
        properties.setProperty("myvalue", "Test")
        def pcs = new PropertyConfigurationService(properties)

        when:
        pcs.getConfigValueAsArray("unknown_property")

        then:
        thrown(PropertyConfigurationException)
    }

    @Unroll
    def "getConfigValueAsArrayLong should return an array of Long if the format is correct for #configValue"() {
        expect:
        expectedResult == getPropertyConfigurationService(key, configValue).getConfigValueAsArrayLong(key)

        where:
        configValue              || expectedResult
        "-17"                    || [-17L]
        "0,17,-90,2147483648"    || [0L, 17L, -90L, 2147483648]
        "0, 17, -90, 2147483648" || [0L, 17L, -90L, 2147483648]
    }

    @Unroll
    def "getConfigValueAsArrayLong should raise a PropertyConfigurationException if the format is not correct: #configValue"() {
        when:
        getPropertyConfigurationService(key, configValue).getConfigValueAsArrayLong(key)

        then:
        def error = thrown(PropertyConfigurationException)
        error.message == expectedMessage

        where:
        configValue            || expectedMessage
        "-17.0"                || "The value [-17.0] in [-17.0] for key [testProp] is not a long"
        "0, 17, A, 2147483648" || "The value [A] in [0, 17, A, 2147483648] for key [testProp] is not a long"
    }

    @Unroll
    def "getSubConfigValue should accept null prefix: #prefix, #key"() {
        given:
        def properties = new Properties()
        properties.setProperty("my.subvalue", "1")
        properties.setProperty("myvalue", "2")
        def pcs = new PropertyConfigurationService(properties)

        expect:
        value == pcs.getSubConfigValue(prefix, key)

        where:
        prefix | key        || value
        "my"   | "subvalue" || "1"
        null   | "myvalue"  || "2"
    }

    def "getProperties should return all the configured properties via a provider or a file, but not via a properties"() {
        given:
        def properties = new Properties()
        properties.setProperty("my.subvalue", "1")
        properties.setProperty("myvalue", "2")
        def pcs = new PropertyConfigurationService(properties)

        when:
        def resultProperties = pcs.getProperties()

        then:
        resultProperties["crypto.algorithm"] == "AES"
        resultProperties["crypto.keysize"] == "128"
        // The values for "my.subvalue" and "myvalue" would be accessible using
        // pcs.getConfigValueAsInt(...), since they are defined as defaults, but should not be listed with
        // the explicitly defined properties.
        resultProperties["my.subvalue"] == null
        resultProperties["myvalue"] == null
    }

    @Unroll
    def "isDefined(#key) should return true only if the property is found in the configuration"() {
        given:
        def properties = new Properties()
        properties.setProperty("my.subvalue", "1")
        properties.setProperty("myvalue", "2")
        def pcs = new PropertyConfigurationService(properties)

        expect:
        value == pcs.isDefined(key)

        where:
        key           || value
        "my.subvalue" || true
        "myvalue"     || true
        "unknown"     || false
    }

    @Unroll
    def "isDefined(#prefix, #key) should return true only if the property is found in the configuration"() {
        given:
        def properties = new Properties()
        properties.setProperty("my.subvalue", "1")
        properties.setProperty("myvalue", "2")
        def pcs = new PropertyConfigurationService(properties)

        expect:
        value == pcs.isDefined(prefix, key)

        where:
        prefix     | key        || value
        "my"       | "subvalue" || true
        null       | "myvalue"  || true
        null       | "unknown"  || false
        "unknown1" | "unknown2" || false
    }

    @Unroll
    def "isDefined(#key) should return true only if the property is found (using provider)"() {
        given:
        def pcs = new PropertyConfigurationService()

        expect:
        value == pcs.isDefined(key)

        where:
        key           || value
        "folder_name" || true
        "absent"      || false
    }

    @Unroll
    def "isDefined(#prefix, #key) should return true only if the property is found (using provider)"() {
        given:
        def pcs = new PropertyConfigurationService()

        expect:
        value == pcs.isDefined(prefix, key)

        where:
        prefix   | key         || value
        "crypto" | "algorithm" || true
        "crypto" | "keysize"   || true
        "birds"  | "fly"       || false
    }

    @Unroll
    def "isDefined(#key) should return true only if the property is found (using file)"() {
        given:
        def pcs = new PropertyConfigurationService("test.properties")

        expect:
        value == pcs.isDefined(key)

        where:
        key       || value
        "myvalue" || true
        "absent"  || false
    }

    @Unroll
    def "isDefined(#prefix, #key) should return true only if the property is found (using file)"() {
        given:
        def pcs = new PropertyConfigurationService("test.properties")

        expect:
        value == pcs.isDefined(prefix, key)

        where:
        prefix   | key        || value
        "sample" | "subvalue" || true
        "birds"  | "fly"      || false
    }

    def "addConfigValue should add a key/value pair or overwrite the value if the key already exists"() {
        given:
        def properties = new Properties()
        properties.setProperty("my.subvalue", "1")
        properties.setProperty("myvalue", "2")
        def pcs = new PropertyConfigurationService(properties)

        when:
        pcs.addConfigValue("myvalue", "3")
        pcs.addConfigValue("anothervalue", "4")

        then:
        pcs.getConfigValue("myvalue") == "3"
        pcs.getConfigValue("anothervalue") == "4"
    }


    private static PropertyConfigurationService getPropertyConfigurationService(String key, String value) {
        def properties = new Properties()
        properties.setProperty(key, value)
        def pcs = new PropertyConfigurationService(properties)
        return pcs
    }


}
