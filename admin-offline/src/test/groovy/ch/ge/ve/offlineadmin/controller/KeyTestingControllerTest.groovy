package ch.ge.ve.offlineadmin.controller

import ch.ge.ve.commons.crypto.ballot.AuthenticatedBallot
import ch.ge.ve.commons.crypto.ballot.BallotCipherService
import ch.ge.ve.commons.crypto.ballot.EncryptedBallotAndWrappedKey
import ch.ge.ve.commons.crypto.exceptions.AuthenticationTagMismatchException
import ch.ge.ve.commons.crypto.exceptions.PrivateKeyPasswordMismatchException
import ch.ge.ve.commons.fileutils.StreamHasher
import ch.ge.ve.offlineadmin.GuiSpecification
import ch.ge.ve.offlineadmin.controller.matchers.AdditionalTableViewMatchers
import ch.ge.ve.offlineadmin.controller.matchers.GlyphIconMatchers
import ch.ge.ve.offlineadmin.exception.MissingKeyFilesException
import ch.ge.ve.offlineadmin.services.BallotCipherServiceFactory
import ch.ge.ve.offlineadmin.util.FileUtils
import ch.ge.ve.offlineadmin.util.LogLevel
import javafx.beans.property.StringProperty
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.stage.Modality
import javafx.stage.Stage
import org.hamcrest.Matchers
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.testfx.api.FxToolkit
import org.testfx.matcher.base.NodeMatchers
import org.testfx.matcher.control.ListViewMatchers
import org.testfx.matcher.control.TableViewMatchers

import javax.xml.bind.DatatypeConverter
import java.nio.file.Files
import java.nio.file.Paths

/*-
* #%L
 * * Admin offline
 * *
 * %%
 * Copyright (C) 2015 - 2016 République et Canton de Genève
 * *
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
import static org.testfx.api.FxAssert.verifyThat

/**
 * This test suit aims at covering the {@link KeyTestingController} controller.
 */
class KeyTestingControllerTest extends GuiSpecification {
    @Rule
    TemporaryFolder temporaryFolder

    def KeyTestingController keyTestingController
    def resources = ResourceBundle.getBundle("ch.ge.ve.offlineadmin.bundles.offlineadmin-messages")

    private mockPasswordDialogController = Mock(PasswordDialogController)
    private mockBallotCipherServiceFactory = Mock(BallotCipherServiceFactory)
    private mockFileUtils = Mock(FileUtils)
    private mockStreamHasher = Mock(StreamHasher)
    def File keyFolder

    void setup() {
        keyFolder = temporaryFolder.newFolder()

        for (keyFile in ["integrity_key.key", "private_key.p12", "public_key.der"]) {
            def stream = KeyTestingControllerTest.class.getClassLoader().getResourceAsStream("keys/" + keyFile)
            def keyPath = Paths.get(keyFolder.toPath().toString(), keyFile)
            Files.copy(stream, keyPath)
        }

        setupStage({ stage ->
            def loader = new FXMLLoader()
            def resource = KeyTestingController.class.getResource("/ch/ge/ve/offlineadmin/view/KeyTesting.fxml")
            loader.location = resource
            loader.resources = resources
            Parent parent = loader.load()

            keyTestingController = loader.controller as KeyTestingController
            keyTestingController.fileUtils = mockFileUtils
            keyTestingController.streamHasher = mockStreamHasher
            keyTestingController.ballotCipherServiceFactory = mockBallotCipherServiceFactory
            keyTestingController.passwordDialogController = mockPasswordDialogController

            return parent
        }, 1024d, 680d)
    }

    void cleanup() {
        FxToolkit.cleanupStages()
    }

    def "testEncryption should perform all required operations"() {
        setup: "set up the mocks"
        def publicKeyHash = "848E149AFE82814131BF013CAFDC6D75DD149A98"

        def ballotCipherService = Mock(BallotCipherService)

        def authenticatedBallot1 = Mock(AuthenticatedBallot)
        def authenticatedBallot2 = Mock(AuthenticatedBallot)
        def authenticatedBallot3 = Mock(AuthenticatedBallot)
        def authenticatedBallot4 = Mock(AuthenticatedBallot)
        def authenticatedBallot5 = Mock(AuthenticatedBallot)
        authenticatedBallot1.getAuthenticatedEncryptedBallot() >> new byte[0]
        authenticatedBallot2.getAuthenticatedEncryptedBallot() >> new byte[0]
        authenticatedBallot3.getAuthenticatedEncryptedBallot() >> new byte[0]
        authenticatedBallot4.getAuthenticatedEncryptedBallot() >> new byte[0]
        authenticatedBallot5.getAuthenticatedEncryptedBallot() >> new byte[0]

        when: "testing encryption"
        fx.interact { keyTestingController.testEncryption() }

        then: "prompt for the key folder"
        1 * mockFileUtils.promptKeyDirectory() >> keyFolder

        and: "log the start of the encryption test"
        verifyThat("#logTable", TableViewMatchers.hasTableCell(resources.getString("key_testing.encryption.start")))

        and: "compute and log the public key hash"
        1 * mockStreamHasher.computeHash(*_) >> DatatypeConverter.parseHexBinary(publicKeyHash)
        verifyThat("#logTable", TableViewMatchers.hasTableCell(String.format(resources.getString("key_testing.public_key_hash"), publicKeyHash)))

        and: "build the ballot cipher service"
        1 * mockBallotCipherServiceFactory.encryptionBallotCipherService(keyFolder) >> ballotCipherService

        and: "encrypt 5 ballots"
        1 * ballotCipherService.encryptBallotThenWrapForAuthentication(_, 1) >> authenticatedBallot1
        1 * ballotCipherService.encryptBallotThenWrapForAuthentication(_, 2) >> authenticatedBallot2
        1 * ballotCipherService.encryptBallotThenWrapForAuthentication(_, 3) >> authenticatedBallot3
        1 * ballotCipherService.encryptBallotThenWrapForAuthentication(_, 4) >> authenticatedBallot4
        1 * ballotCipherService.encryptBallotThenWrapForAuthentication(_, 5) >> authenticatedBallot5

        and: "populate the ciphertext list with the encrypted ballots"
        verifyThat("#cipherTextList", ListViewMatchers.hasItems(5))
        verifyThat("#cipherTextList", ListViewMatchers.hasListCell(authenticatedBallot1))
        verifyThat("#cipherTextList", ListViewMatchers.hasListCell(authenticatedBallot2))
        verifyThat("#cipherTextList", ListViewMatchers.hasListCell(authenticatedBallot3))
        verifyThat("#cipherTextList", ListViewMatchers.hasListCell(authenticatedBallot4))
        verifyThat("#cipherTextList", ListViewMatchers.hasListCell(authenticatedBallot5))
    }

    def "testDecryption should perform all required operations"() {
        setup: "set up the mocks"
        def publicKeyHash = "848E149AFE82814131BF013CAFDC6D75DD149A98"
        def passwd1 = "Test123456"
        def passwd2 = "Test654321"

        def ballotCipherService = Mock(BallotCipherService)

        def authenticatedBallot1 = Mock(AuthenticatedBallot)
        def authenticatedBallot2 = Mock(AuthenticatedBallot)
        def authenticatedBallot3 = Mock(AuthenticatedBallot)
        def authenticatedBallot4 = Mock(AuthenticatedBallot)
        def authenticatedBallot5 = Mock(AuthenticatedBallot)
        authenticatedBallot1.getAuthenticatedEncryptedBallot() >> new byte[0]
        authenticatedBallot2.getAuthenticatedEncryptedBallot() >> new byte[0]
        authenticatedBallot3.getAuthenticatedEncryptedBallot() >> new byte[0]
        authenticatedBallot4.getAuthenticatedEncryptedBallot() >> new byte[0]
        authenticatedBallot5.getAuthenticatedEncryptedBallot() >> new byte[0]

        def encryptedBallot1 = Mock(EncryptedBallotAndWrappedKey)
        def encryptedBallot2 = Mock(EncryptedBallotAndWrappedKey)
        def encryptedBallot3 = Mock(EncryptedBallotAndWrappedKey)
        def encryptedBallot4 = Mock(EncryptedBallotAndWrappedKey)
        def encryptedBallot5 = Mock(EncryptedBallotAndWrappedKey)


        keyTestingController.cipherTexts = [authenticatedBallot1, authenticatedBallot2, authenticatedBallot3, authenticatedBallot4, authenticatedBallot5]

        when: "testing decryption"
        fx.interact { keyTestingController.testDecryption() }

        then: "log start of decryption test"
        verifyThat("#logTable", TableViewMatchers.hasTableCell(resources.getString("key_testing.decryption.start")))

        and: "prompt for the key folder"
        1 * mockFileUtils.promptKeyDirectory() >> keyFolder

        and: "initiate the ballot decryption service"
        1 * mockBallotCipherServiceFactory.decryptionBallotCipherService(keyFolder) >> ballotCipherService

        and: "compute and log the public key hash"
        1 * mockStreamHasher.computeHash(*_) >> DatatypeConverter.parseHexBinary(publicKeyHash)
        verifyThat("#logTable", TableViewMatchers.hasTableCell(String.format(resources.getString("key_testing.public_key_hash"), publicKeyHash)))

        and: "verify authentication and unwrap ballots"
        1 * ballotCipherService.verifyAuthenticationThenUnwrap(authenticatedBallot1) >> encryptedBallot1
        1 * ballotCipherService.verifyAuthenticationThenUnwrap(authenticatedBallot2) >> encryptedBallot2
        1 * ballotCipherService.verifyAuthenticationThenUnwrap(authenticatedBallot3) >> encryptedBallot3
        1 * ballotCipherService.verifyAuthenticationThenUnwrap(authenticatedBallot4) >> encryptedBallot4
        1 * ballotCipherService.verifyAuthenticationThenUnwrap(authenticatedBallot5) >> encryptedBallot5


        and: "prompt for the passwords"
        1 * mockPasswordDialogController.promptForPasswords(_, _, _) >> { arguments ->
            def p1 = arguments[0] as StringProperty
            def p2 = arguments[1] as StringProperty

            p1.setValue(passwd1)
            p2.setValue(passwd2)
        }

        and: "load the private key using the given passwords"
        1 * ballotCipherService.loadBallotKeyCipherPrivateKey(passwd1 + passwd2)

        and: "decrypt all the ballots"
        1 * ballotCipherService.decryptBallot(encryptedBallot1) >> "Test1"
        1 * ballotCipherService.decryptBallot(encryptedBallot2) >> "Test2"
        1 * ballotCipherService.decryptBallot(encryptedBallot3) >> "Test3"
        1 * ballotCipherService.decryptBallot(encryptedBallot4) >> "Test4"
        1 * ballotCipherService.decryptBallot(encryptedBallot5) >> "Test5"

        and: "populate the decrypted items list with the newly decrypted ballots"
        verifyThat("#decryptedTextList", ListViewMatchers.hasItems(5))
        verifyThat("#decryptedTextList", ListViewMatchers.hasListCell("Test1"))
        verifyThat("#decryptedTextList", ListViewMatchers.hasListCell("Test2"))
        verifyThat("#decryptedTextList", ListViewMatchers.hasListCell("Test3"))
        verifyThat("#decryptedTextList", ListViewMatchers.hasListCell("Test4"))
        verifyThat("#decryptedTextList", ListViewMatchers.hasListCell("Test5"))
    }

    def "testEncryption with missing key files should display adequate message"() {
        setup: "set up the mocks"
        mockBallotCipherServiceFactory.encryptionBallotCipherService(keyFolder) >> { throw new MissingKeyFilesException("no keys") }

        when: "testing encryption"
        fx.interact { keyTestingController.testEncryption() }

        then: "prompt for the key folder"
        1 * mockFileUtils.promptKeyDirectory() >> keyFolder

        and: "log the start of the encryption test"
        verifyThat("#logTable", TableViewMatchers.hasTableCell(resources.getString("key_testing.encryption.start")))

        and: "stops before displaying public key hash"
        0 * mockStreamHasher.computeHash(*_)
        verifyThat("#logTable", Matchers.not(AdditionalTableViewMatchers.hasTableCell(Matchers.containsString(resources.getString("key_testing.public_key_hash").replace("%s", "")))))

        and: "missing key message shown in console"
        verifyThat("#logTable", TableViewMatchers.hasTableCell(resources.getString("keys_not_found_in_directory")))
    }


    def "testDecryption with missing key files should display adequate message"() {
        setup: "set up the mocks"
        def publicKeyHash = "848E149AFE82814131BF013CAFDC6D75DD149A98"
        mockBallotCipherServiceFactory.decryptionBallotCipherService(keyFolder) >> { throw new MissingKeyFilesException("no keys") }

        when: "testing decryption"
        fx.interact { keyTestingController.testDecryption() }

        then: "prompt for the key folder"
        1 * mockFileUtils.promptKeyDirectory() >> keyFolder

        and: "log the start of the encryption test"
        verifyThat("#logTable", TableViewMatchers.hasTableCell(resources.getString("key_testing.decryption.start")))

        and: "stops before displaying public key hash"
        0 * mockStreamHasher.computeHash(*_)
        verifyThat("#logTable", Matchers.not(TableViewMatchers.hasTableCell(String.format(resources.getString("key_testing.public_key_hash"), publicKeyHash))))

        and: "missing key message shown in console"
        verifyThat("#logTable", AdditionalTableViewMatchers.hasTableCell(GlyphIconMatchers.isGlyphIconOf(LogLevel.WARN.glyphName)))
        verifyThat("#logTable", TableViewMatchers.hasTableCell(resources.getString("keys_not_found_in_directory")))
    }

    def "testDecryption mismatching passwords should display adequate message"() {
        setup: "set up the mocks"
        def publicKeyHash = "848E149AFE82814131BF013CAFDC6D75DD149A98"
        def ballotCipherService = Mock(BallotCipherService)
        mockBallotCipherServiceFactory.decryptionBallotCipherService(keyFolder) >> ballotCipherService
        mockPasswordDialogController.promptForPasswords(_, _, _) >> {
            throw new PrivateKeyPasswordMismatchException("passwords don't match")
        }

        def authenticatedBallot1 = Mock(AuthenticatedBallot)
        authenticatedBallot1.getAuthenticatedEncryptedBallot() >> new byte[0]

        def encryptedBallot1 = Mock(EncryptedBallotAndWrappedKey)

        keyTestingController.cipherTexts = [authenticatedBallot1]

        when: "testing decryption"
        fx.interact { keyTestingController.testDecryption() }

        then: "log start of decryption test"
        verifyThat("#logTable", TableViewMatchers.hasTableCell(resources.getString("key_testing.decryption.start")))

        and: "prompt for the key folder"
        1 * mockFileUtils.promptKeyDirectory() >> keyFolder

        and: "initiate the ballot decryption service"
        1 * mockBallotCipherServiceFactory.decryptionBallotCipherService(keyFolder) >> ballotCipherService

        and: "compute and log the public key hash"
        1 * mockStreamHasher.computeHash(*_) >> DatatypeConverter.parseHexBinary(publicKeyHash)
        verifyThat("#logTable", TableViewMatchers.hasTableCell(String.format(resources.getString("key_testing.public_key_hash"), publicKeyHash)))

        and: "verify authentication and unwrap ballots"
        1 * ballotCipherService.verifyAuthenticationThenUnwrap(authenticatedBallot1) >> encryptedBallot1

        and: "stop before loading the private key"
        0 * ballotCipherService.loadBallotKeyCipherPrivateKey(_)

        and: "password mismatch message shown in console with warning icon"
        verifyThat("#logTable", AdditionalTableViewMatchers.hasTableCell(GlyphIconMatchers.isGlyphIconOf(LogLevel.WARN.glyphName)))
        verifyThat("#logTable", TableViewMatchers.hasTableCell(resources.getString("key_password_mismatch")))
    }


    def "testDecryption mismatching authentication tags should show an alert"() {
        setup: "set up the mocks"
        mockFileUtils.promptKeyDirectory() >> keyFolder
        def ballotCipherService = Mock(BallotCipherService)
        mockBallotCipherServiceFactory.decryptionBallotCipherService(keyFolder) >> ballotCipherService
        def publicKeyHash = "848E149AFE82814131BF013CAFDC6D75DD149A98"
        mockStreamHasher.computeHash(*_) >> DatatypeConverter.parseHexBinary(publicKeyHash)

        def authenticatedBallot1 = Mock(AuthenticatedBallot)
        authenticatedBallot1.getAuthenticatedEncryptedBallot() >> ([255, 255, 255, 254] as byte[])

        fx.interact { keyTestingController.cipherTexts = [authenticatedBallot1] }

        ballotCipherService.verifyAuthenticationThenUnwrap(authenticatedBallot1) >> {
            throw new AuthenticationTagMismatchException("tag doesn't match")
        }

        when: "testing decryption"
        fx.clickOn("#decryptButton")

        then:
        // 2 windows: main stage + alert dialog
        fx.listWindows().size() == 2
        // find alert dialog by modality
        def Stage alertDialog = fx.listWindows().collect { it as Stage }.find { it.modality == Modality.APPLICATION_MODAL }
        // check the content label
        def nodeQuery = fx.robotContext().nodeFinder.from(alertDialog.scene.root).lookup(".label.content")
        verifyThat(nodeQuery.query(), NodeMatchers.hasText(resources.getString("key_testing.exception_occurred")))
    }
}
