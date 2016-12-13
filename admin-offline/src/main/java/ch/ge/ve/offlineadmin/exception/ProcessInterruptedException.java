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
 * This exception is thrown whenever the process flow is interrupted, either manually by the user ("Cancel" buttons for
 * instance), or due to an underlying exception.
 */
public class ProcessInterruptedException extends Exception {
    /**
     * Constructor usually used when the interruption was caused by a user action
     *
     * @param message the message explaining the interruption
     */
    public ProcessInterruptedException(String message) {
        super(message);
    }

    /**
     * Contructor used when the interruption is caused by a previous exception
     *
     * @param message the message explaining the interruption
     * @param cause   the cause of the interruption
     */
    public ProcessInterruptedException(String message, Throwable cause) {
        super(message, cause);
    }
}
