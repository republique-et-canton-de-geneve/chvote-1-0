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

import com.google.common.base.Preconditions;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class provides the centralized way of creating Cipher instances.
 * <p/>
 * The goal is to create Ciphers that specify their algorithm and implementation provider, so that
 * the system behaves consistently whichever is the target operation system and jdk:
 * <ul>
 * <li>should not the implementation provider be provided, the OS native one could be used, and we do not want it</li>
 * <li>should not the algorithm provider be provided, the default one of the jdk could be used, and we do not want it</li>
 * </ul>
 * <p/>
 * As a secure coding rule, the direct creation of Cipher is prohibited throughout the application.
 */
public class CipherFactory {

    private final Map<String, String> providerByAlgo;

    public CipherFactory(PropertyConfigurationService propertyConfigurationService) {
        try {
            providerByAlgo = new HashMap<>();
            for (String algo : propertyConfigurationService.getConfigValue("common.crypto.ciphers.list").split(",")) {
                providerByAlgo.put(algo.trim(), propertyConfigurationService.getConfigValue("common.crypto.security.provider." + algo.trim()));
            }
        } catch (PropertyConfigurationException e) {
            throw new CryptoConfigurationRuntimeException("Error retrieving cipher providers", e);
        }
    }

    /**
     * Returns an instance of Cipher for the given algorithm
     * <p/>
     * The Security Provider is chosen depending on the algorithm
     *
     * @return an instance of Cipher
     */
    public Cipher getInstance(String algo) {
        Preconditions.checkNotNull(algo);
        try {
            final String provider = providerByAlgo.get(algo);
            return Cipher.getInstance(algo, provider);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException e) {
            throw new CryptoConfigurationRuntimeException("Error creating Cipher", e);
        }
    }

}
