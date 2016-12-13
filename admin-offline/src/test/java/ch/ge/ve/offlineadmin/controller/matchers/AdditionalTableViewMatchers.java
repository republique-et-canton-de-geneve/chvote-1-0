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

import com.google.common.base.Optional;
import javafx.scene.Node;
import javafx.scene.control.Cell;
import javafx.scene.control.TableView;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.testfx.api.FxAssert;
import org.testfx.service.finder.NodeFinder;
import org.testfx.service.query.NodeQuery;

/**
 * Custom matchers providing additional verifications for JavaFX Table node.
 */
public class AdditionalTableViewMatchers {
    @Factory
    public static <T> Matcher<Node> hasTableCell(final Matcher<T> contentsMatcher) {
        return new TypeSafeMatcher<Node>(TableView.class) {
            @Override
            protected boolean matchesSafely(Node item) {
                NodeFinder nodeFinder = FxAssert.assertContext().getNodeFinder();
                NodeQuery nodeQuery = nodeFinder.from(item);

                Optional<Node> result = nodeQuery.lookup(".table-cell").match(cellWithValue(contentsMatcher)).tryQuery();
                return result.isPresent();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(TableView.class.getSimpleName())
                        .appendText(" containing ")
                        .appendDescriptionOf(contentsMatcher);
            }

            @Override
            protected void describeMismatchSafely(Node item, Description mismatchDescription) {
                mismatchDescription.appendText("was ").appendValue(item);
            }
        };
    }

    @Factory
    public static <T> Matcher<Node> cellWithValue(final Matcher<T> contentsMatcher) {
        return new TypeSafeMatcher<Node>(Cell.class) {
            @Override
            protected boolean matchesSafely(Node item) {
                return contentsMatcher.matches(((Cell) item).getItem());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(Cell.class.getSimpleName())
                        .appendText(" ")
                        .appendText("with value")
                        .appendDescriptionOf(contentsMatcher);
            }
        };
    }
}
