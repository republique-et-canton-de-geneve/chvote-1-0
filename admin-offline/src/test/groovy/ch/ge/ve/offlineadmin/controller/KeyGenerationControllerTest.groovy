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
package ch.ge.ve.offlineadmin.controller

import ch.ge.ve.commons.fileutils.StreamHasher
import ch.ge.ve.commons.properties.PropertyConfigurationService
import ch.ge.ve.offlineadmin.GuiSpecification
import ch.ge.ve.offlineadmin.services.KeyGenerator
import ch.ge.ve.offlineadmin.util.FileUtils
import ch.ge.ve.offlineadmin.util.PropertyConfigurationServiceFactory
import javafx.beans.property.StringProperty
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.testfx.matcher.control.TableViewMatchers

import static org.testfx.api.FxAssert.verifyThat

/**
 * This test suit aims at covering the {@link KeyGenerationController} controller.
 */
class KeyGenerationControllerTest extends GuiSpecification {
    @Rule
    TemporaryFolder temporaryFolder

    private KeyGenerationController keyGenerationController

    ResourceBundle resources = ResourceBundle.getBundle("ch.ge.ve.offlineadmin.bundles.offlineadmin-messages")
    private mockFileUtils = Mock(FileUtils)
    private mockStreamHasher = Spy(StreamHasher)
    private mockPasswordDialogController = Mock(PasswordDialogController)
    private PropertyConfigurationService propertyConfigurationService = new PropertyConfigurationServiceFactory().propertyConfigurationService()
    // Can't mock java final classes properly, therefore need to delegate calls involving KeyStore...
    private KeyGenerator spyKeyGenerator = Spy(KeyGenerator, constructorArgs: [propertyConfigurationService])
    def File keyFolder

    void setup() {
        keyFolder = temporaryFolder.newFolder()

        setupStage({ stage ->
            def loader = new FXMLLoader()
            def resource = KeyTestingController.class.getResource("/ch/ge/ve/offlineadmin/view/KeyGeneration.fxml")
            loader.location = resource
            loader.resources = resources
            Parent parent = loader.load() as Parent

            keyGenerationController = loader.controller as KeyGenerationController
            keyGenerationController.fileUtils = mockFileUtils
            keyGenerationController.streamHasher = mockStreamHasher
            keyGenerationController.passwordDialogController = mockPasswordDialogController
            keyGenerationController.keyGenerator = spyKeyGenerator

            return parent
        }, 1024d, 680d)
    }

    def "generateKeys should perform all collaborator calls"() {
        setup:
        def passwd1 = "Test123456"
        def passwd2 = "Test654321"

        when:
        fx.interact { keyGenerationController.startKeyGeneration() }

        then:
        verifyThat("#logTable", TableViewMatchers.hasTableCell(resources.getString("key_generation.started")))

        and:
        1 * mockPasswordDialogController.promptForPasswords(*_) >> { List arguments ->
            def p1 = arguments[0] as StringProperty
            def p2 = arguments[1] as StringProperty

            p1.setValue(passwd1)
            p2.setValue(passwd2)
        }

        and:
        1 * spyKeyGenerator.generateSecretKey()
        1 * spyKeyGenerator.generateKeyPair()

        and:
        1 * spyKeyGenerator.generateCertificate(_)

        and:
        1 * spyKeyGenerator.createKeyStore(*_)

        and:
        verifyThat("#logTable", TableViewMatchers.hasTableCell(resources.getString("key_generation.keys_generated")))

        and:
        1 * mockFileUtils.getUserHome()
        1 * mockFileUtils.getDirectory(*_) >> keyFolder

        and:
        FileFilter fileFilter = { it.name.contains("keys") && it.directory }
        def files = keyFolder.listFiles().find { fileFilter }.listFiles()
        files.size() == 3
        def filenames = files.collect{ it.name }
        filenames.find { it.matches("integrity_key_\\S*.key") } != null
        filenames.find { it.matches("public_key_\\S*.der") } != null
        filenames.find { it.matches("private_key_\\S*.p12") } != null
        files.findAll { it.length() > 0} .size() == 3
    }
}
