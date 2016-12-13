package ch.ge.ve.offlineadmin.util;

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

/**
 * Security ciphers constants and configuration file properties names
 */
public class SecurityConstants {

    public static final int BITS_PER_BYTE = 8;

    /* config constants */
    public static final String ADMIN_OFFLINE_CONFIGURATION_FILE = "offlineadmin-configuration.properties";
    public static final String PROPERTIES_LOG4J = "log4j.properties";

    /* Property keys */
    public static final String CERT_COMMON_NAME_PROPERTY = "cert.commonName";
    public static final String CERT_ORGANISATION_PROPERTY = "cert.organisation";
    public static final String CERT_ORGANISATIONAL_UNIT_PROPERTY = "cert.organisationalUnit";
    public static final String CERT_COUNTRY_PROPERTY = "cert.country";
    public static final String CERT_VALIDITY_DAYS_PROPERTY = "cert.validityDays";
    public static final String CERT_PRIVATE_FRIENDLY_NAME_PROPERTY = "cert.private.friendlyName";
    public static final String CERT_HASH_ALGORITHM = "cert.hash.algorithm";

    public static final String CERT_PUBLIC_KEY_FILENAME = "cert.public.filename";
    public static final String CERT_PUBLIC_KEY_FILENAME_PATTERN = "cert.public.filename.pattern";

    public static final String CERT_PRIVATE_KEY_FILENAME = "cert.private.filename";
    public static final String CERT_PRIVATE_KEY_FILENAME_PATTERN = "cert.private.filename.pattern";

    public static final String INTEGRITY_KEY_FILENAME = "integrity.filename";
    public static final String INTEGRITY_KEY_FILENAME_PATTERN = "integrity.filename.pattern";

    public static final String BALLOTS_FILENAME = "cleartext.ballots.filename";

    public static final String STREAM_MAX_OBJECTS = "stream.max.objects";

    private SecurityConstants() {
        // utility class, do not allow to instantiate it
    }
}
