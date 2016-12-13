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

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;

/**
 *  Class defining the log levels and their display properties
 */
public enum LogLevel {
    OK("ok-text", FontAwesomeIcon.CHECK.name()),
    WARN("warn-text", FontAwesomeIcon.EXCLAMATION_TRIANGLE.name()),
    ERROR("error-text", FontAwesomeIcon.EXCLAMATION_CIRCLE.name());


    private final String styleClass;
    private final String glyphName;

    /**
     * @param styleClass css style
     * @param glyphName icon name
     */
    LogLevel(String styleClass, String glyphName) {
        this.styleClass = styleClass;
        this.glyphName = glyphName;
    }

    /**
     * Getter on the css style
     * @return the css style
     */
    public String getStyleClass() {
        return styleClass;
    }

    /**
     * Getter on the icon name
     * @return the icon name
     */
    public String getGlyphName() {
        return glyphName;
    }
}
