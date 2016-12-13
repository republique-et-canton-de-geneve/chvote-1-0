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
import ch.ge.ve.commons.crypto.ballot.BallotCipherService
import ch.ge.ve.commons.crypto.ballot.EncryptedBallotAndWrappedKey
import ch.ge.ve.commons.fileutils.StreamHasher
import ch.ge.ve.offlineadmin.GuiSpecification
import ch.ge.ve.offlineadmin.services.BallotCipherServiceFactory
import ch.ge.ve.offlineadmin.util.FileUtils
import ch.ge.ve.offlineadmin.util.LogLevel
import javafx.beans.property.StringProperty
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.testfx.matcher.control.TableViewMatchers

import javax.xml.bind.DatatypeConverter
import java.nio.file.Files

import static ch.ge.ve.offlineadmin.controller.matchers.AdditionalTableViewMatchers.hasTableCell
import static ch.ge.ve.offlineadmin.controller.matchers.GlyphIconMatchers.isGlyphIconOf
import static org.hamcrest.CoreMatchers.not
import static org.testfx.api.FxAssert.verifyThat

/**
 * This test suit aims at covering the {@link BallotDecryptionController} controller.
 */
class BallotDecryptionControllerTest extends GuiSpecification {
    @Rule
    TemporaryFolder temporaryFolder

    private BallotDecryptionController ballotDecryptionController
    def resources = ResourceBundle.getBundle("ch.ge.ve.offlineadmin.bundles.offlineadmin-messages")
    private mockPasswordDialogController = Mock(PasswordDialogController)
    private mockBallotCipherServiceFactory = Mock(BallotCipherServiceFactory)
    private mockFileUtils = Mock(FileUtils)
    private mockStreamHasher = Mock(StreamHasher)

    void setup() {
        setupStage { stage ->
            def loader = new FXMLLoader()
            def resource = BallotDecryptionController.class.getResource("/ch/ge/ve/offlineadmin/view/BallotDecryption.fxml")
            loader.location = resource
            loader.resources = resources
            Parent parent = loader.load()

            ballotDecryptionController = loader.controller as BallotDecryptionController
            ballotDecryptionController.passwordDialogController = mockPasswordDialogController
            ballotDecryptionController.ballotCipherServiceFactory = mockBallotCipherServiceFactory
            ballotDecryptionController.fileUtils = mockFileUtils
            ballotDecryptionController.streamHasher = mockStreamHasher

            return parent
        }
    }

    def "startDecryption should call all required collaborators"() {
        given:
        def keyFolder = temporaryFolder.newFolder("keys")
        def encryptedBallotsFile = temporaryFolder.newFile("encryptedBallots.ser")
        def outputFolder = temporaryFolder.newFolder("output")

        List<EncryptedBallotAndWrappedKey> encryptedBallotAndWrappedKeyList = new ArrayList<>()
        def outputStream = Files.newOutputStream(encryptedBallotsFile.toPath())
        def stream = new ObjectOutputStream(outputStream)
        def encBallotsFileHash = BigInteger.valueOf(0x0123456789).toByteArray()

        stream.writeObject(encryptedBallotAndWrappedKeyList)
        stream.close()
        outputStream.close()

        mockFileUtils.promptKeyDirectory() >> keyFolder
        mockFileUtils.promptEncryptedBallotsFile() >> encryptedBallotsFile
        mockFileUtils.getDirectory(_, _) >> outputFolder
        mockPasswordDialogController.promptForPasswords(_, _, _) >> { StringProperty pwd1, StringProperty pwd2, conf ->
            pwd1.value = "Test56789"
            pwd2.value = "Test00000"
        }

        def ballotCipherService = Mock(BallotCipherService)
        mockBallotCipherServiceFactory.decryptionBallotCipherService(keyFolder) >> ballotCipherService

        when:
        fx.interact { ballotDecryptionController.startDecryption() }

        then:
        verifyThat("#logTable", hasTableCell(isGlyphIconOf(LogLevel.OK.glyphName)))
        verifyThat("#logTable", not(hasTableCell(isGlyphIconOf(LogLevel.WARN.glyphName))))
        verifyThat("#logTable", not(hasTableCell(isGlyphIconOf(LogLevel.ERROR.glyphName))))
        verifyThat("#logTable", TableViewMatchers.hasTableCell(String.format(resources.getString("ballot_decryption.enc_ballots_file_hash"), DatatypeConverter.printHexBinary(encBallotsFileHash))))
        2 * mockStreamHasher.threadSafeComputeHash(_) >> encBallotsFileHash
        1 * ballotCipherService.loadBallotKeyCipherPrivateKey("Test56789" + "Test00000")
        encryptedBallotAndWrappedKeyList.size() * ballotCipherService.decryptBallot(_)
        def ballotListFilter = { dir, name -> name.contains("ballot-list") } as FilenameFilter
        def ballotListFiles = outputFolder.listFiles(ballotListFilter)
        ballotListFiles.length == 1
        Files.readAllLines(ballotListFiles[0].toPath()).size() == encryptedBallotAndWrappedKeyList.size()
    }
}
