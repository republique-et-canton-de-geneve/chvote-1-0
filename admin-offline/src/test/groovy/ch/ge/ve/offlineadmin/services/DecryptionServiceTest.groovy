package ch.ge.ve.offlineadmin.services

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
import ch.ge.ve.commons.crypto.exceptions.CryptoOperationRuntimeException
import ch.ge.ve.offlineadmin.util.ProgressTracker
import spock.lang.Specification

/**
 * This test suit aims at covering the {@link DecryptionService} service.
 */
class DecryptionServiceTest extends Specification {
    private BallotCipherService ballotCipherService

    void setup() {
        ballotCipherService = Stub(BallotCipherService)
    }

    def """If the ballot box contains an invalid ballot paper,
      the decryption should go until the end and signal that there is an invalid ballot paper"""() {
        given:
        ballotCipherService.decryptBallot(_) >>> ["a ballot", "another ballot"] >> {
            throw new CryptoOperationRuntimeException("error")
        } >> "a third ballot" >> {
            throw new CryptoOperationRuntimeException("error")
        } >> "a fourth ballot"

        def decryptionService = new DecryptionService(ballotCipherService, Stub(ProgressTracker))

        def ballotBox = [Stub(EncryptedBallotAndWrappedKey)]
        for (int i = 0; i < 6; i++) {
            ballotBox.add(Stub(EncryptedBallotAndWrappedKey))
        }

        when:
        def decryptedBallots = decryptionService.decrypt(ballotBox)

        then:
        decryptionService.getInvalidCounter() == 2
        decryptedBallots.containsAll(["a ballot", "another ballot", "a third ballot", "a fourth ballot"])
    }

    def "Increment the progression step every 100 ballot papers decryption"() {
        given:
        ballotCipherService.decryptBallot(_) >> "a ballot"

        def progressTracker = Mock(ProgressTracker)
        def decryptionService = new DecryptionService(ballotCipherService, progressTracker)

        def ballotBox = []
        for (int i = 0; i < 500; i++) {
            ballotBox.add(Stub(EncryptedBallotAndWrappedKey))
        }

        when:
        decryptionService.decrypt(ballotBox)

        then:
        5 * progressTracker.incrementStepCount()
    }
}
