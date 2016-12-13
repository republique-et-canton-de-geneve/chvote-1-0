package ch.ge.ve.commons.crypto;

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

import ch.ge.ve.commons.crypto.exceptions.CryptoConfigurationRuntimeException;
import ch.ge.ve.commons.properties.PropertyConfigurationProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Publishes the properties of the security/cryptographic configurations.
 */
public class PropertyConfigurationProviderSecurityImpl implements PropertyConfigurationProvider {

    private static final String PROPS_FILE = "common-crypto.properties";

    private Properties properties;

    public PropertyConfigurationProviderSecurityImpl() {
        properties = new Properties();
        try {
            final InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(PROPS_FILE);
            if (resourceAsStream == null) {
                throw new CryptoConfigurationRuntimeException("Unable to load the configuration file from the classpath: " + PROPS_FILE);
            }
            properties.load(resourceAsStream);
        } catch (IOException e) {
            throw new CryptoConfigurationRuntimeException("Unable to load the user configuration: " + PROPS_FILE, e);
        }
    }

    @Override
    public Properties getProperties() {
        return properties;
    }
}
