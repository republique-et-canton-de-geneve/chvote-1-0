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

/**
 * Runtime exception to be thrown if an unrecoverable error is found when
 * trying to access the configuration defined in the application and its modules
 * properties files.
 */
public class PropertyConfigurationRuntimeException extends RuntimeException {
    public PropertyConfigurationRuntimeException(String message) {
        super(message);
    }

    public PropertyConfigurationRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
