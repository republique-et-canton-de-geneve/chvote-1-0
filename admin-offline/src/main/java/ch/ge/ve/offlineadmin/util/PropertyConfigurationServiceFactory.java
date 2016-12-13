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

import ch.ge.ve.commons.properties.PropertyConfigurationService;

import static ch.ge.ve.offlineadmin.util.SecurityConstants.ADMIN_OFFLINE_CONFIGURATION_FILE;

/**
 *
 */
public class PropertyConfigurationServiceFactory {
    /**
     * Creates a {@link PropertyConfigurationService} based on the default application's configuration file
     * @return the configuration service
     */
    public PropertyConfigurationService propertyConfigurationService() {
        return new PropertyConfigurationService(ADMIN_OFFLINE_CONFIGURATION_FILE);
    }

}
