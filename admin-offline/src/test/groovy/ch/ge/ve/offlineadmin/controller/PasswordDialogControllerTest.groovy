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
import com.google.common.base.Predicate
import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.TextField
import org.testfx.api.FxAssert
import org.testfx.matcher.base.NodeMatchers
import org.testfx.matcher.control.LabeledMatchers
import org.testfx.util.WaitForAsyncUtils

/**
 * This test suite verifies the correct implementation of {@link PasswordDialogController}
 */
class PasswordDialogControllerTest extends GuiSpecification {
    def resources = ResourceBundle.getBundle("ch.ge.ve.offlineadmin.bundles.offlineadmin-messages")
    def console = Mock(ConsoleOutputControl)

    void setup() {
        setupStage { stage ->
            FXMLLoader loader = new FXMLLoader();

            URL resource = this.getClass().getResource("/ch/ge/ve/offlineadmin/view/RootLayout.fxml");
            loader.setLocation(resource);
            loader.setResources(resources);

            return loader.load() as Parent
        }
    }

    def "password verification prompt should update the password properties"() {
        def passwordDialogController = new PasswordDialogController(resources, console)
        def password1 = new SimpleStringProperty()
        def password2 = new SimpleStringProperty()

        given:
        Platform.runLater {
            passwordDialogController.promptForPasswords(password1, password2, false)
        }
        WaitForAsyncUtils.waitForFxEvents()

        when:
        def nodeFinder = FxAssert.assertContext().nodeFinder
        // First group
        def passwordField1 = nodeFinder.lookup("#passwordField1").query() as TextField
        fx.interact { passwordField1.text = "Test1Test1" }
        def button = nodeFinder.from(passwordField1.scene.root).lookup(".button").lookup({
            (it as Button).text == "Confirmer le mot de passe"
        } as Predicate).query()
        fx.interact { fx.clickOn(button) }

        // Second group
        passwordField1 = nodeFinder.lookup("#passwordField1").query() as TextField
        fx.interact { passwordField1.text = "Test2Test2" }
        button = nodeFinder.from(passwordField1.scene.root).lookup(".button").lookup({
            (it as Button).text == resources.getString("password_dialog.confirm_button")
        } as Predicate).query()
        fx.interact { fx.clickOn(button) }

        then:
        "Test1Test1" == password1.valueSafe
        "Test2Test2" == password2.valueSafe
        1 * console.progressMessage(resources.getString("key_generation.password1_entered"))
        1 * console.progressMessage(resources.getString("key_generation.password2_entered"))
    }

    def "mismatching passwords should display error message"() {
        def passwordDialogController = new PasswordDialogController(resources, console)
        def password1 = new SimpleStringProperty()
        def password2 = new SimpleStringProperty()

        given:
        Platform.runLater {
            passwordDialogController.promptForPasswords(password1, password2, true)
        }
        WaitForAsyncUtils.waitForFxEvents()

        when:
        def nodeFinder = FxAssert.assertContext().nodeFinder
        // First group
        def passwordField1 = nodeFinder.lookup("#passwordField1").query() as TextField
        fx.interact { passwordField1.text = "Test1Test1" }
        def passwordField2 = nodeFinder.lookup("#passwordField2").query() as TextField
        fx.interact { passwordField2.text = "Wrong password2" }

        then:
        FxAssert.verifyThat("#errorLabel", LabeledMatchers.hasText(resources.getString("password_dialog.strength_requirements")))
        // No calls to progress message
        0 * console.progressMessage(/.*/)
        FxAssert.verifyThat("#errorLabel", NodeMatchers.isVisible())
    }
}
