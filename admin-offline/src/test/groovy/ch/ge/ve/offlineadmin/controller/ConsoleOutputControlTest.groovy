package ch.ge.ve.offlineadmin.controller

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
import ch.ge.ve.offlineadmin.GuiSpecification
import ch.ge.ve.offlineadmin.controller.matchers.AdditionalTableViewMatchers
import ch.ge.ve.offlineadmin.util.LogLevel
import javafx.fxml.FXMLLoader
import javafx.scene.Parent

import static ch.ge.ve.offlineadmin.controller.matchers.GlyphIconMatchers.isGlyphIconOf
import static ch.ge.ve.offlineadmin.controller.matchers.ProgressBarMatchers.hasProgress
import static org.testfx.api.FxAssert.verifyThat
import static org.testfx.matcher.base.NodeMatchers.isVisible
import static org.testfx.matcher.control.TableViewMatchers.hasTableCell

/**
 * This test suite aims to verify correctness of the {@link ConsoleOutputControl} implementation.
 */
class ConsoleOutputControlTest extends GuiSpecification {
    ConsoleOutputControl consoleOutputControl

    void setup() {
        setupStage { stage ->
            def loader = new FXMLLoader()
            def resource = ConsoleOutputControl.class.getResource("/ch/ge/ve/offlineadmin/view/ConsoleOutput.fxml")
            loader.location = resource
            Parent parent = loader.load()

            consoleOutputControl = loader.controller as ConsoleOutputControl

            return parent
        }
    }

    def "logTable should be visible"() {
        expect:
        verifyThat("#logTable", isVisible())
    }

    def "logMessage(String, LogLevel) should add a message with appropriate icon"() {
        when:
        fx.interact { consoleOutputControl.logOnScreen("Testing log", LogLevel.ERROR) }

        then:
        verifyThat("#logTable", hasTableCell("Testing log"))
        verifyThat("#logTable", AdditionalTableViewMatchers.hasTableCell(isGlyphIconOf(LogLevel.ERROR.glyphName)))
    }

    def "logMessage(String) should add a message with OK level"() {
        when:
        fx.interact { consoleOutputControl.logOnScreen("Another test") }

        then:
        verifyThat("#logTable", hasTableCell("Another test"))
        verifyThat("#logTable", AdditionalTableViewMatchers.hasTableCell(isGlyphIconOf(LogLevel.OK.glyphName)))
    }

    def "progressMessage(String, LogLevel) should add a message with appropriate icon"() {
        when:
        fx.interact { consoleOutputControl.progressMessage("test message", LogLevel.WARN) }

        then:
        verifyThat("#logTable", hasTableCell("test message"))
        verifyThat("#logTable", AdditionalTableViewMatchers.hasTableCell(isGlyphIconOf(LogLevel.WARN.glyphName)))
    }

    def "progressMessage(String) should add a message with OK level"() {
        when:
        fx.interact { consoleOutputControl.progressMessage("another test message") }

        then:
        verifyThat("#logTable", hasTableCell("another test message"))
        verifyThat("#logTable", AdditionalTableViewMatchers.hasTableCell(isGlyphIconOf(LogLevel.OK.glyphName)))
    }

    def "incrementStepCount() should advance progression"() {
        def progressBar = "#progressBar"
        def progress = 0.0
        def stepCount = 6.0

        when:
        consoleOutputControl.stepCount = stepCount
        then:
        verifyThat(progressBar, hasProgress(progress / stepCount)) // 0

        when:
        fx.interact { consoleOutputControl.incrementStepCount() }
        then:
        verifyThat(progressBar, hasProgress(++progress / stepCount)) // 1/6

        when:
        fx.interact { consoleOutputControl.incrementStepCount() }
        then:
        verifyThat(progressBar, hasProgress(++progress / stepCount)) // 2/6

        when:
        fx.interact { consoleOutputControl.incrementStepCount() }
        then:
        verifyThat(progressBar, hasProgress(++progress / stepCount)) // 3/6

        when:
        fx.interact { consoleOutputControl.incrementStepCount() }
        then:
        verifyThat(progressBar, hasProgress(++progress / stepCount)) // 4/6

        when:
        fx.interact { consoleOutputControl.incrementStepCount() }
        then:
        verifyThat(progressBar, hasProgress(++progress / stepCount)) // 5/6

        when:
        fx.interact { consoleOutputControl.incrementStepCount() }
        then:
        verifyThat(progressBar, hasProgress(++progress / stepCount)) // 6/6
    }

    def "incrementStepCount() may be called from outside the JavaFX thread"() {
        when:
        consoleOutputControl.stepCount = 1
        consoleOutputControl.incrementStepCount()

        then:
        notThrown(IllegalStateException)
    }
}
