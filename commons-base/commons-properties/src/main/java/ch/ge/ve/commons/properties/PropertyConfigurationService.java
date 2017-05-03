package ch.ge.ve.commons.properties;

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

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

/**
 * Service to get the module or application configuration values from a properties file.
 */
public class PropertyConfigurationService {
    private static final Logger LOG = Logger.getLogger(PropertyConfigurationService.class);

    private final Properties properties;

    /**
     * Creates the configuration service, sourcing itself from the declared configuration providers
     * (using the Java ServiceLoader).
     */
    public PropertyConfigurationService() {
        this.properties = loadConfigurationProviders(new Properties());
    }

    /**
     * Creates a new configuration service that sources itself from a properties file, that
     * is retrieved from the classpath, and from the declared configuration providers
     * (using the Java ServiceLoader).
     *
     * @param propertiesFilePath path to the properties file to use as a source, in the format needed by the classloader.
     * @throws PropertyConfigurationRuntimeException if fails to load config
     */
    public PropertyConfigurationService(String propertiesFilePath) {
        final InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(propertiesFilePath);
        if (resourceAsStream == null) {
            throw new PropertyConfigurationRuntimeException("Properties file cannot be found: " + propertiesFilePath);
        }
        this.properties = loadConfig(resourceAsStream);
    }


    /**
     * Creates a new configuration service that sources itself from from the declared configuration
     * providers (using the Java ServiceLoader), and using its default values from a properties object
     *
     * @param properties properties object to be used as defaults
     */
    public PropertyConfigurationService(final Properties properties) {
        this.properties = loadConfigurationProviders(new Properties(properties));
    }

    private static Properties loadConfig(InputStream inputStream) {
        Properties props = new Properties();
        try {
            props.load(inputStream);
        } catch (IOException e) {
            throw new PropertyConfigurationRuntimeException("Unable to load the base configuration", e);
        }
        return loadConfigurationProviders(props);
    }

    private static Properties loadConfigurationProviders(Properties properties) {
        // load complementary properties from the service loader
        ServiceLoader<PropertyConfigurationProvider> providers = ServiceLoader.load(PropertyConfigurationProvider.class);
        for (PropertyConfigurationProvider provider : providers) {
            final Properties providerProperties = provider.getProperties();
            for (Map.Entry<Object, Object> entry : providerProperties.entrySet()) {
                // check if property is not already defined
                if (properties.get(entry.getKey()) != null) {
                    throw new PropertyConfigurationRuntimeException(
                            String.format("Property [%s] is already defined with value [%s]",
                                    entry.getKey(),
                                    entry.getValue()
                            ));
                } else {
                    properties.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return properties;
    }

    /**
     * Gets a property value as an Integer.
     *
     * @param key the property key
     * @return the property value
     * @throws PropertyConfigurationException if fails to find or convert the property
     */
    public int getConfigValueAsInt(String key) throws PropertyConfigurationException {
        int valeur;
        try {
            valeur = Integer.parseInt(getConfigValue(key));
        } catch (NumberFormatException nfe) {
            throw new PropertyConfigurationException(String.format("The value [%s] for key [%s] is not an integer", getConfigValue(key), key));
        }
        return valeur;
    }

    /**
     * Gets a property value as a Boolean.
     *
     * @param key the property key
     * @return the property value
     * @throws PropertyConfigurationException if fails to find or convert the property
     */
    public boolean getConfigValueAsBoolean(String key) throws PropertyConfigurationException {
        return Boolean.parseBoolean(getConfigValue(key));

    }


    /**
     * Gets a property value as a Long.
     *
     * @param key the property key
     * @return the property value
     * @throws PropertyConfigurationException if fails to find or convert the property
     */
    public long getConfigValueAsLong(String key) throws PropertyConfigurationException {
        long valeur;
        try {
            valeur = Long.parseLong(getConfigValue(key));
        } catch (NumberFormatException nfe) {
            throw new PropertyConfigurationException(String.format("The value [%s] for key [%s] is not a long", getConfigValue(key), key));
        }
        return valeur;
    }

    /**
     * Gets a property value as a String.
     *
     * @param key the property key
     * @return the property value
     * @throws PropertyConfigurationException if fails to find or convert the property
     */
    public String getConfigValue(String key) throws PropertyConfigurationException {
        String retour = properties.getProperty(key);
        if (retour == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Value %s does not exist", key));
            }
            throw new PropertyConfigurationException(String.format("The property [%s] does not exist", key));
        }
        return retour;
    }

    /**
     * Gets a property value as an array of String.
     * The property values have to be formatted with each value separated by a comma,
     * e.g "value1,value2"
     *
     * @param key the property key
     * @return the property value
     * @throws PropertyConfigurationException if fails to find or convert the property
     */
    public String[] getConfigValueAsArray(String key) throws PropertyConfigurationException {
        String configValue = properties.getProperty(key);
        if (configValue == null) {
            throw new PropertyConfigurationException("The property [" + key + "] does not exist");
        }
        ArrayList<String> elementList = Lists.newArrayList();
        Iterable<String> elements = Splitter.on(",").split(configValue);
        for (String element : elements) {
            elementList.add(element.trim());
        }
        return Iterables.toArray(elementList, String.class);
    }

    /**
     * Gets a property value as an array of Long.
     * The property values have to be formatted with each value separated by a comma,
     * e.g "value1,value2"
     *
     * @param key the property key
     * @return the property value
     * @throws PropertyConfigurationException if fails to find or convert the property
     */
    public long[] getConfigValueAsArrayLong(String key) throws PropertyConfigurationException {
        final String[] strings = getConfigValueAsArray(key);
        long[] values = new long[strings.length];
        for (int i = 0; i < strings.length; i++) {
            final String string = strings[i];
            try {
                values[i] = Long.parseLong(string);
            } catch (NumberFormatException nfe) {
                throw new PropertyConfigurationException(String.format("The value [%s] in [%s] for key [%s] is not a long", string, getConfigValue(key), key));
            }
        }
        return values;
    }

    /**
     * Gets a property value as a String, using a prefix and a key to define the actual key in the properties.
     *
     * @param subConfigPrefix prefix of the configuration key
     * @param key             other part of the configuration key
     * @return the property value
     * @throws PropertyConfigurationException PropertyConfigurationException if fails to find or convert the property
     */
    public String getSubConfigValue(String subConfigPrefix, String key) throws PropertyConfigurationException {
        return getConfigValue(buildKey(subConfigPrefix, key));
    }

    private static String buildKey(String subConfigPrefix, String key) {
        return subConfigPrefix != null ? subConfigPrefix + "." + key : key;
    }

    /**
     * @return all the properties
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Tells whether the key is defined in the properties.
     *
     * @param key the property key
     * @return <code>true</code> if the property id defined, <code>false</code> otherwise
     */
    public boolean isDefined(String key) {
        return properties.getProperty(key) != null;
    }

    /**
     * Tells whether the key is defined in the properties.
     *
     * @param subConfigPrefix prefix of the configuration key
     * @param key             other part of the configuration key
     * @return <code>true</code> if the property id defined, <code>false</code> otherwise
     */
    public boolean isDefined(String subConfigPrefix, String key) {
        return isDefined(buildKey(subConfigPrefix, key));
    }

    /**
     * Add a key/value pair to the configuration properties
     *
     * @param key   property key
     * @param value property value
     */
    public void addConfigValue(String key, String value) {
        assert properties != null;
        properties.put(key, value);
    }
}
