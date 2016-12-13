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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * This class provides the centralized way of creating MessageDigest instances.
 * <p/>
 * The goal is to create MessageDigest that specify their algorithm and implementation provider, so that
 * the system behaves consistently whichever is the target operation system and jdk:
 * <ul>
 * <li>should not the implementation provider be provided, the OS native one could be used, and we do not want it</li>
 * <li>should not the algorithm provider be provided, the default one of the jdk could be used, and we do not want it</li>
 * </ul>
 * <p/>
 * As a secure coding rule, the direct creation of MessageDigest is prohibited throughout the application.
 */
public class MessageDigestFactory {

    private final String algorithm;
    private final String provider;

    public MessageDigestFactory(PropertyConfigurationService propertyConfigurationService) {
        try {
            algorithm = propertyConfigurationService.getConfigValue("common.crypto.digest.algorithm");
            provider = propertyConfigurationService.getConfigValue("common.crypto.digest.provider");
        } catch (PropertyConfigurationException e) {
            throw new CryptoConfigurationRuntimeException("Error retrieving mac properties", e);
        }
    }


    /**
     * Returns an instance of the MessageDigest with algorithm and providers as defined in configuration
     *
     * @return an instance of MessageDigest
     */
    public MessageDigest getInstance() {
        try {
            return MessageDigest.getInstance(algorithm, provider);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new CryptoConfigurationRuntimeException("Error creating MessageDigest", e);
        }
    }
}
