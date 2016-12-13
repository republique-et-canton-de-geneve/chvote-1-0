/*
 * -
 * #%L
 * Common crypto utilities
 * %%
 * Copyright (C) 2016 République et Canton de Genève
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

package ch.ge.ve.commons.fileutils;

/**
 * Exception to be thrown by the module in case of an unrecoverable error while accessing files.
 */
public class FileOperationRuntimeException extends RuntimeException {
    public FileOperationRuntimeException(String message) {
        super(message);
    }

    public FileOperationRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileOperationRuntimeException(Throwable cause) {
        super(cause);
    }

    public FileOperationRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
