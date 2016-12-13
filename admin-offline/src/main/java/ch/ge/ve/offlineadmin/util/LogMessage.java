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

import de.jensd.fx.glyphs.GlyphIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

/**
 *
 */
public class LogMessage {
    private final GlyphIcon glyphIcon;
    private final String message;

    public LogMessage(LogLevel logLevel, String message) {
        glyphIcon = new FontAwesomeIconView();
        glyphIcon.setGlyphName(logLevel.getGlyphName());
        glyphIcon.setGlyphSize(18);
        glyphIcon.setStyleClass(logLevel.getStyleClass());
        this.message = message;
    }

    public GlyphIcon getGlyphIcon() {
        return glyphIcon;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LogMessage that = (LogMessage) o;

        if (glyphIcon != null ? !glyphIcon.equals(that.glyphIcon) : that.glyphIcon != null) {
            return false;
        }
        return !(message != null ? !message.equals(that.message) : that.message != null);

    }

    @Override
    public int hashCode() {
        int result = glyphIcon != null ? glyphIcon.hashCode() : 0;
        result = 31 * result + (message != null ? message.hashCode() : 0);
        return result;
    }
}
