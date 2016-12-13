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
import org.hamcrest.TypeSafeMatcher;

import java.util.regex.Pattern;

/**
 * Tests if the argument is a {@link CharSequence} that matches a regular expression.
 */
public class PatternMatcher extends TypeSafeMatcher<CharSequence> {

    /**
     * Creates a matcher that matches if the examined {@link CharSequence} matches the specified
     * regular expression.
     * <p/>
     * For example:
     * <pre>assertThat("myStringOfNote", pattern("[0-9]+"))</pre>
     *
     * @param regex the regular expression that the returned matcher will use to match any examined {@link CharSequence}
     */
    @Factory
    public static Matcher<CharSequence> pattern(String regex) {
        return pattern(Pattern.compile(regex));
    }

    /**
     * Creates a matcher that matches if the examined {@link CharSequence} matches the specified
     * {@link java.util.regex.Pattern}.
     * <p/>
     * For example:
     * <pre>assertThat("myStringOfNote", Pattern.compile("[0-9]+"))</pre>
     *
     * @param pattern the pattern that the returned matcher will use to match any examined {@link CharSequence}
     */
    @Factory
    public static Matcher<CharSequence> pattern(Pattern pattern) {
        return new PatternMatcher(pattern);
    }

    private final Pattern pattern;

    public PatternMatcher(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean matchesSafely(CharSequence item) {
        return pattern.matcher(item).matches();
    }

    @Override
    public void describeMismatchSafely(CharSequence item, org.hamcrest.Description mismatchDescription) {
        mismatchDescription.appendText("was \"").appendText(String.valueOf(item)).appendText("\"");
    }

    @Override
    public void describeTo(org.hamcrest.Description description) {
        description.appendText("a string with pattern \"").appendText(String.valueOf(pattern)).appendText("\"");
    }
}
