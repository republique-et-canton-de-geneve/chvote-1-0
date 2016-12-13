package ch.ge.ve.commons.fileutils;

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

import org.joda.time.DateTime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Utility class to manage the parametrized output file names.
 * <p/>
 * The supported parameters are the following and are replaced at runtime with the given values:
 * <ul>
 * <li>{date}: the date of the file generation in 'yyyyMMdd' format</li>
 * <li>{datetime}: date and time of the file generation in 'yyyy-MM-dd-HH'h'mm'm'ss's'' format</li>
 * <li>{user}: the user of the application: the windows account name</li>
 * <li>{operationCode}: the code of the operation for which the file is generated</li>
 * <li>{canton}: the canton of the operation for which the file is generated</li>
 * </ul>
 */
public class OutputFilesPattern {
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd-HH'h'mm'm'ss's'";
    public static final String DATE_FORMAT = "yyyyMMdd";
    private static final int FIND_FILES_MAX_DEPTH = 10;

    private final SimpleDateFormat datetimeFormat = new SimpleDateFormat(DATE_TIME_FORMAT);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);


    /**
     * Resolves the parameters {date}, {datetime} and {user}
     *
     * @param pattern
     * @param user
     * @param date
     * @return
     */
    public String injectParams(String pattern, String user, DateTime date) {
        String result = pattern.replace("{date}", getDateAsString(date));
        result = result.replace("{datetime}", getDateTimeAsString(date));
        result = result.replace("{user}", user);
        return result;
    }

    /**
     * Resolves the parameters {date}, {datetime} and {user}.</br>
     * user is resolved from environment variable 'user.name'
     *
     * @param pattern parametrized file name
     * @param date    the date and time of the file generation
     * @return the final file name
     */
    public String injectParams(String pattern, DateTime date) {
        return injectParams(pattern, getSystemUserName(), date);
    }


    /**
     * Resolves the parameters {date}, {datetime}, {user} and {operationCode}
     *
     * @param pattern       parametrized file name
     * @param user          user
     * @param date          the date and time of the file generation
     * @param operationCode the code of the operation
     * @return the final file name
     */
    public String injectParams(String pattern, String user, DateTime date, String operationCode) {
        String result = injectParams(pattern, user, date);
        result = result.replace("{operationCode}", operationCode);
        return result;
    }

    /**
     * Resolves the parameters {date}, {datetime}, {user} and {operationCode}
     *
     * @param pattern       parametrized file name
     * @param date          the date and time of the file generation
     * @param operationCode the code of the operation
     * @return the final file name
     */
    public String injectParams(String pattern, DateTime date, String operationCode) {
        return injectParams(pattern, getSystemUserName(), date, operationCode);
    }


    /**
     * Resolves all the parameters.
     *
     * @param pattern       parametrized file name
     * @param user          user
     * @param date          the date and time of the file generation
     * @param operationCode the code of the operation
     * @param hostingCode   the canton of the operation
     * @return the final file name
     */
    public String injectParams(String pattern, String user, DateTime date, String operationCode, String hostingCode) {
        String result = injectParams(pattern, user, date, operationCode);
        result = result.replace("{canton}", hostingCode);
        return result;
    }

    /**
     * Resolves all the parameters.
     *
     * @param pattern       parametrized file name
     * @param user          user
     * @param date          the date and time of the file generation
     * @param operationCode the code of the operation
     * @param hostingCode   the canton of the operation
     * @param votersName    the name of the voters group
     * @return the final file name
     */
    public String injectParams(String pattern, String user, DateTime date, String operationCode, String hostingCode, String votersName) {
        String result = injectParams(pattern, user, date, operationCode, hostingCode);
        result = result.replace("{votersName}", votersName);
        return result;
    }

    /**
     * Resolves all the parameters.
     *
     * @param pattern       parametrized file name
     * @param date          the date and time of the file generation
     * @param operationCode the code of the operation
     * @param hostingCode   the canton of the operation
     * @return the final file name
     */
    public String injectParams(String pattern, DateTime date, String operationCode, String hostingCode) {
        return injectParams(pattern, getSystemUserName(), date, operationCode, hostingCode);
    }

    /**
     * Resolves all the parameters.
     *
     * @param pattern       parametrized file name
     * @param date          the date and time of the file generation
     * @param operationCode the code of the operation
     * @param hostingCode   the canton of the operation
     * @param votersName    the name of the voters group
     * @return the final file name
     */
    public String injectParams(String pattern, DateTime date, String operationCode, String hostingCode, String votersName) {
        return injectParams(pattern, getSystemUserName(), date, operationCode, hostingCode, votersName);
    }

    /**
     * Tries to find a file matching a given <i>filenamePattern</i> regular expression.
     *
     * @param filenamePattern regular expression pattern that the file should match
     * @param rootPath        path from which to recursively search for the file
     * @return the first file matching the pattern from the root directory, or an empty optional if none matched
     */
    public Optional<Path> findFirstFileByPattern(final Pattern filenamePattern, Path rootPath) {
        try (Stream<Path> pathStream = Files.find(
                rootPath,
                FIND_FILES_MAX_DEPTH,
                (path, attr) -> filenamePattern.matcher(path.getFileName().toString()).matches())) {
            return pathStream.sorted().findFirst();
        } catch (IOException e) {
            throw new FileOperationRuntimeException("Cannot walk file tree", e);
        }
    }

    private String getDateAsString(DateTime date) {
        return dateFormat.format(date.getMillis());
    }

    private String getDateTimeAsString(DateTime date) {
        return datetimeFormat.format(date.getMillis());
    }

    private static String getSystemUserName() {
        return System.getProperty("user.name");
    }
}
