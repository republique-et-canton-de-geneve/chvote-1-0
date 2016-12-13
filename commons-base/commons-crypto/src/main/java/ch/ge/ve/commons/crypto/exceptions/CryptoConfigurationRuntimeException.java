package ch.ge.ve.commons.crypto.exceptions;

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

/**
 * This exception is thrown when the access to the crypto configuration file or one of its key/value pair fails.
 */
public class CryptoConfigurationRuntimeException extends RuntimeException {

    public CryptoConfigurationRuntimeException(String message) {
        super(message);
    }

    public CryptoConfigurationRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
