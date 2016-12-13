package ch.ge.ve.commons.crypto.matchers;

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

import org.hamcrest.Factory;
import org.hamcrest.Matcher;

import java.util.regex.Pattern;

/**
 * This factory class contains matchers for verification codes.
 */
public class CodeFormatMatchers {
    /**
     * @return a Matcher with will ascertain that the given String respects the format
     * of a verification code
     */
    @Factory
    public static Matcher validCodeFormat() {
        return new PatternMatcher(Pattern.compile("([A-Z&&[^IO]][0-9&&[^10]]){2}"));
    }

    /**
     * @return a Matcher with will ascertain that the given String respects the format
     * of a verification code
     */
    @Factory
    public static Matcher validFinalizationCodeFormat() {
        return new PatternMatcher(Pattern.compile("[1-9]{6}"));
    }
}
