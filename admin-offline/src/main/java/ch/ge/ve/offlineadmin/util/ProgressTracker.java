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

/**
 * Application's processes progress tracker
 */
public interface ProgressTracker {
    /**
     * Adds a message with specific log level indicator to the progress tracker
     * @param message
     * @param logLevel
     */
    void progressMessage(String message, LogLevel logLevel);

    /**
     * Adds a message to the progress tracker, with level {@link LogLevel#OK}
     * @param message
     */
    void progressMessage(String message);

    /**
     * Sets the steps count
     * @param stepCount
     */
    void setStepCount(int stepCount);

    /**
     * Increments the steps count
     */
    void incrementStepCount();
}
