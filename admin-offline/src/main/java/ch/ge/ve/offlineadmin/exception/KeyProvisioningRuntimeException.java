package ch.ge.ve.offlineadmin.exception;

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
 * This exception is thrown when the provisioning of a key fails.
 */
public class KeyProvisioningRuntimeException extends RuntimeException {
    public KeyProvisioningRuntimeException(String message) {
        super(message);
    }

    public KeyProvisioningRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public KeyProvisioningRuntimeException(Throwable cause) {
        super(cause);
    }

    public KeyProvisioningRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
