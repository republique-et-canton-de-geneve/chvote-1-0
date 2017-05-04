package ch.ge.ve.commons.fileutils

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


import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

/**
 * This test suit aims at covering the {@link OutputFilesPattern} utility class.
 */
class OutputFilesPatternTest extends Specification {
    private OutputFilesPattern ofp = new OutputFilesPattern()
    private DateTimeFormatter dateTimeFormatter
    private ZonedDateTime dateTime
    private String username;

    void setup() {
        dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        dateTime = LocalDateTime.parse("2016-11-21 23:25:54", dateTimeFormatter).atZone(ZoneId.of("Europe/Zurich"));
        username = System.getProperty("user.name")
        System.setProperty("user.name", "MyUser")
    }

    void cleanup() {
        System.setProperty("user.name", username)
    }

    def "injectParam should correctly inject user and date"() {
        when:
        def s = ofp.injectParams("{user} made something on {date}", "TheUser", dateTime)

        then:
        s == "TheUser made something on 20161121"
    }

    def "injectParam should inject system user and date if user parameter is not specified"() {
        when:
        def s = ofp.injectParams("{user} made something on {date}", dateTime)

        then:
        s == "MyUser made something on 20161121"
    }

    def "injectParam should correctly inject user and datetime"() {
        when:
        def s = ofp.injectParams("{user} made something on {datetime}", "TheUser", dateTime)

        then:
        s == "TheUser made something on 2016-11-21-23h25m54s"
    }

    def "injectParam should inject system user and datetime if user parameter is not specified"() {
        when:
        def s = ofp.injectParams("{user} made something on {datetime}", dateTime)

        then:
        s == "MyUser made something on 2016-11-21-23h25m54s"
    }

    def "injectParam should correctly inject user, date and operation code"() {
        when:
        def s = ofp.injectParams("{user} made something on {date} for operation {operationCode}", "TheUser", dateTime, "201611VP")

        then:
        s == "TheUser made something on 20161121 for operation 201611VP"
    }

    def "injectParam should correctly inject system user, date and operation code"() {
        when:
        def s = ofp.injectParams("{user} made something on {date} for operation {operationCode}", dateTime, "201611VP")

        then:
        s == "MyUser made something on 20161121 for operation 201611VP"
    }

    def "injectParam should correctly inject user, date, operation code and canton"() {
        when:
        def s = ofp.injectParams("{user} made something on {date} for operation {operationCode} and canton {canton}", "TheUser", dateTime, "201611VP", "GE")

        then:
        s == "TheUser made something on 20161121 for operation 201611VP and canton GE"
    }

    def "injectParam should correctly inject system user, date, operation code and canton"() {
        when:
        def s = ofp.injectParams("{user} made something on {date} for operation {operationCode} and canton {canton}", dateTime, "201611VP", "GE")

        then:
        s == "MyUser made something on 20161121 for operation 201611VP and canton GE"
    }

    def "injectParam should correctly inject system user, date, operation code, canton and voters name"() {
        when:
        def s = ofp.injectParams("{user} made something on {date} for operation {operationCode} and canton {canton}, for voters {votersName}", dateTime, "201611VP", "GE", "SE")

        then:
        s == "MyUser made something on 20161121 for operation 201611VP and canton GE, for voters SE"
    }

    @Unroll
    def "findFirstFileByPattern should find file '#filename' for pattern '#pattern'"() {
        given:
        def rootPath = Paths.get(this.getClass().getClassLoader().getResource("aFile.txt").toURI()).getParent()

        expect:
        def foundPath = ofp.findFirstFileByPattern(Pattern.compile(pattern), rootPath)
        isFilePresent(foundPath, filename)

        where:
        pattern          || filename
        /.*\.txt/        || "aFile.txt"
        /another.*\.txt/ || "anotherFile.txt"
        /unknown.*\.txt/ || null
    }

    private static boolean isFilePresent(Optional<Path> foundPath, String filename) {
        if (foundPath.isPresent()) {
            return filename == foundPath.get().getFileName().toString()
        } else {
            return null == filename
        }
    }

    def "error while trying to find the first file matching a pattern should raise a FileOperationRuntimeException"() {
        given:
        def rootPath = Paths.get("unknown_path")

        when:
        ofp.findFirstFileByPattern(Pattern.compile(/.*\.txt/ ), rootPath)

        then:
        thrown(FileOperationRuntimeException)
    }
}
