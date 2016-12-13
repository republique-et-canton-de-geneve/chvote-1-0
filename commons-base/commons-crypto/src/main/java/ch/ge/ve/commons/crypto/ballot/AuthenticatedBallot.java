package ch.ge.ve.commons.crypto.ballot;

/*-
 * #%L
 * Common crypto utilities
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

/**
 * <p>
 * This class holds an encrypted ballot as it is stored in the database.
 * </p>
 * <h2>Usage</h2>
 * <h3>Goal</h3>
 * <p>
 * This is the second layer of encryption for the ballot. The already encrypted ballot is encrypted a second time, with
 * the integrity key (a new one is generated for each test / real operation).
 * </p>
 * <p>
 * An encryption scheme compatible with AEAD is used, so that the ballot index can be associated to the already
 * encrypted ballot contents, in order to prevent vote replacement by copying an existing (and known) vote multiple
 * times (the unicity and sequence of ballot indices is verified upon opening the ballot box).
 * </p>
 * <h3>Encryption</h3>
 * <p>
 * When encrypting a ballot, the instance of AuthenticatedBallot that will be stored in the database must be build as
 * follows:
 * <ul>
 * <li>the {@link #wrappedKey} field is retrieved from the {@link ch.ge.ve.commons.crypto.ballot.EncryptedBallotAndWrappedKey#wrappedKey} field</li>
 * <li>the {@link #ballotIndex} field is provided as the number of ballots in the ballot box + 1</li>
 * <li>the {@link #authenticatedEncryptedBallot} field is build by encrypting the
 * {@link ch.ge.ve.commons.crypto.ballot.EncryptedBallotAndWrappedKey#encryptedBallot} with the integrity key, with
 * the ballotIndex as associated data</li>
 * <li>the {@link #tag} field contains the tag of the authenticated encryption performed above</li>
 * </ul>
 * </p>
 * <h3>Decryption</h3>
 * <p>
 * When decrypting a ballot, fields can simply be read from the database.
 * </p>
 * <p>
 * For security purposes, the following actions must be undertaken:
 * <ul>
 * <li>once all <tt>AuthenticatedBallot</tt>s have been read, the unicity and sequence of ballot indices must be verified</li>
 * <li>the number of retrieved ballots matches the expected number of ballots</li>
 * <li>upon decrypting this layer of encryption, all authentication tags must match the recorded tags</li>
 * <li>before the Electoral Officers' private key is inserted into the system, all references to the
 * <tt>AuthenticatedBallot</tt>s must be dropped and garbage collected, in order to prevent linking of a decrypted vote
 * to a voter (through the ballot index)</li>
 * </ul>
 * </p>
 */
public class AuthenticatedBallot {
    /**
     * the random key k_i, wrapped with the Electoral Officers public key
     */
    private final byte[] wrappedKey;

    /**
     * the doubly encrypted ballot (once with key k_i, once with the integrity key, with the ballot index as associated data)
     */
    private final byte[] authenticatedEncryptedBallot;

    /**
     * the ballot index (by order of insertion)
     */
    private final int ballotIndex;

    /**
     * the tag produced by the second layer of authenticated encryption
     */
    private final byte[] tag;


    /**
     * @param wrappedKey the random key k_i, wrapped with the Electoral Officers public key
     * @param authenticatedEncryptedBallot the doubly encrypted ballot (once with key k_i, once with the integrity key, with the ballot index as associated data)
     * @param ballotIndex the ballot index (by order of insertion)
     * @param tag the tag produced by the second layer of authenticated encryption
     */
    public AuthenticatedBallot(byte[] wrappedKey, byte[] authenticatedEncryptedBallot, int ballotIndex, byte[] tag) {
        this.wrappedKey = wrappedKey;
        this.authenticatedEncryptedBallot = authenticatedEncryptedBallot;
        this.ballotIndex = ballotIndex;
        this.tag = tag;
    }

    /**                                                      *

     * @return the wrappedKey
     */
    public byte[] getWrappedKey() {
        return wrappedKey;
    }

    /**
     * @return the authenticatedEncryptedBallot
     */
    public byte[] getAuthenticatedEncryptedBallot() {
        return authenticatedEncryptedBallot;
    }

    /**
     * @return the ballotIndex
     */
    public int getBallotIndex() {
        return ballotIndex;
    }

    /**
     * @return the authentication tag
     */
    public byte[] getTag() {
        return tag;
    }
}
