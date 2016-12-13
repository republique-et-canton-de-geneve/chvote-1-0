package ch.ge.ve.offlineadmin.controller.matchers;

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
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.testfx.matcher.base.GeneralMatchers;

/**
 * Custom matcher used to verify GlypIcon usage.
 */
public class GlyphIconMatchers {
    @Factory
    public static Matcher<GlyphIcon> isGlyphIconOf(String glyphName) {
        String descriptionText = String.format("of glyph \"%s\"", glyphName);
        return GeneralMatchers.typeSafeMatcher(GlyphIcon.class, descriptionText, glyphIcon -> glyphName.equals(glyphIcon.getGlyphName()));
    }
}
