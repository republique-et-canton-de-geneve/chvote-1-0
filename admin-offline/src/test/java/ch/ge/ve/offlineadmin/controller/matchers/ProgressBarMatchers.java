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

import javafx.scene.Node;
import javafx.scene.control.ProgressBar;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Custom matcher used to verify the current value of a given progress bar.
 */
public class ProgressBarMatchers {
    @Factory
    public static Matcher<Node> hasProgress(double expectedProgress) {
        return new TypeSafeMatcher<Node>(ProgressBar.class) {
            @Override
            protected boolean matchesSafely(Node item) {
                double actualProgress = ((ProgressBar) item).getProgress();
                double difference = Math.abs(actualProgress - expectedProgress);

                // Since we're comparing percentages of advancement, an absolute difference lower than 0.001 (i.e. 0.1%) is satisfactory
                return difference == 0 || difference < 0.001;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("ProgressBar with progress ")
                        .appendValue(expectedProgress);
            }

            @Override
            protected void describeMismatchSafely(Node item, Description mismatchDescription) {
                mismatchDescription.appendText("had progress ").appendValue(((ProgressBar) item).getProgress());
            }
        };
    }
}
