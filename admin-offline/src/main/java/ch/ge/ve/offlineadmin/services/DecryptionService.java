package ch.ge.ve.offlineadmin.services;

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

import ch.ge.ve.commons.crypto.ballot.BallotCipherService;
import ch.ge.ve.commons.crypto.ballot.EncryptedBallotAndWrappedKey;
import ch.ge.ve.commons.crypto.exceptions.CryptoOperationRuntimeException;
import ch.ge.ve.commons.crypto.utils.SecureRandomFactory;
import ch.ge.ve.offlineadmin.util.ProgressTracker;

import com.google.common.base.Strings;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Ballots box decryption service
 */
public class DecryptionService {
    public static final int STEP_SIZE = 100;
    private static final Logger LOGGER = Logger.getLogger(DecryptionService.class);
    private final BallotCipherService ballotCipherService;
    private final ProgressTracker progressTracker;
    private AtomicInteger invalidCounter = new AtomicInteger();

    /**
     * @param ballotCipherService providing decryption service
     * @param progressTracker providing tracking utility
     */
    public DecryptionService(BallotCipherService ballotCipherService, ProgressTracker progressTracker) {
        this.ballotCipherService = ballotCipherService;
        this.progressTracker = progressTracker;
    }

    /**
     * Decrypts a list of encrypted ballots
     * @param encryptedBallots  list of encrypted ballots
     * @return list of decrypted ballots
     */
    public List<String> decrypt(List<EncryptedBallotAndWrappedKey> encryptedBallots) {
        AtomicInteger counter = new AtomicInteger();
        invalidCounter = new AtomicInteger();

        Stream<String> decryptedBallots = encryptedBallots.parallelStream().map(encryptedBallot -> decryptBallot(counter, encryptedBallot));
        List<String> validDecryptedBallots = decryptedBallots.filter(ballot -> !Strings.isNullOrEmpty(ballot)).collect(Collectors.toList());
        Collections.shuffle(validDecryptedBallots, SecureRandomFactory.createPRNG());

        return validDecryptedBallots;
    }

    private String decryptBallot(AtomicInteger counter, EncryptedBallotAndWrappedKey encryptedBallot) {
        try {
            if (counter.incrementAndGet() % STEP_SIZE == 0) {
                progressTracker.incrementStepCount();
            }
            return ballotCipherService.decryptBallot(encryptedBallot);
        } catch (CryptoOperationRuntimeException e) {
            LOGGER.error(e);
            invalidCounter.incrementAndGet();
            return null;
        }
    }

    /**
     * Getter for the number of invalid ballots. Those ballots generated an error when trying to decrypt them.
     * @return the number of undecryptable ballots
     */
    public int getInvalidCounter() {
        return invalidCounter.get();
    }
}
