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
package ch.ge.ve.offlineadmin

import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage
import org.testfx.framework.junit.ApplicationTest
import spock.lang.Specification

/**
 * This wrapper allows for easy setup of JavaFX unit tests, using spock
 *
 * <p>Retrieved and packaged based on <a href="https://gist.github.com/siordache/10fb0a65749c9122edda">https://gist.github.com/siordache/10fb0a65749c9122edda</a></p>
 */
abstract class GuiSpecification extends Specification {
    ApplicationTest fx

    void setupStage(Closure<Parent> rootNodeFactory, def double width = 800.0d, def double height = 600.0d) {
        fx = new GuiTestMixin(rootNodeFactory, width, height)
        fx.internalBefore()
    }

    class GuiTestMixin extends ApplicationTest {
        final Closure<Parent> rootNodeFactory
        final double width
        final double height

        def GuiTestMixin(Closure<Parent> rootNodeFactory, double width, double height) {
            this.rootNodeFactory = rootNodeFactory
            this.width = width
            this.height = height
        }

        protected Parent getRootNode(stage) {
            return rootNodeFactory.call(stage) as Parent
        }

        void start(def Stage stage) throws Exception {
            def Scene scene = new Scene(getRootNode(stage), width, height)
            stage.scene = scene
            stage.show()
        }
    }
}
