package ch.ge.ve.commons.crypto.utils;

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
import ch.ge.ve.commons.properties.PropertyConfigurationException;
import ch.ge.ve.commons.properties.PropertyConfigurationService;

import javax.crypto.Mac;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * This class provides the centralized way of creating Mac instances.
 * <p/>
 * The goal is to create Mac that specify their algorithm and implementation provider, so that
 * the system behaves consistently whichever is the target operation system and jdk:
 * <ul>
 * <li>should not the implementation provider be provided, the OS native one could be used, and we do not want it</li>
 * <li>should not the algorithm provider be provided, the default one of the jdk could be used, and we do not want it</li>
 * </ul>
 * <p/>
 * As a secure coding rule, the direct creation of Mac is prohibited throughout the application.
 */
public class MacFactory {

    private final String algorithm;
    private final String provider;

    public MacFactory(PropertyConfigurationService propertyConfigurationService) {
        try {
            algorithm = propertyConfigurationService.getConfigValue("common.crypto.hmac.algorithm");
            provider = propertyConfigurationService.getConfigValue("common.crypto.hmac.provider");
        } catch (PropertyConfigurationException e) {
            throw new CryptoConfigurationRuntimeException("Error retrieving hmac properties", e);
        }
    }

    /**
     * Returns an instance of the Mac with algorithm and providers as defined in configuration
     *
     * @return an instance of Mac
     */
    public Mac getInstance() {
        try {
            return Mac.getInstance(algorithm, provider);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new CryptoConfigurationRuntimeException("Error creating Mac", e);
        }
    }

}
