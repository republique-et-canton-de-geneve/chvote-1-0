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

import javax.crypto.SealedObject;

import java.io.Serializable;

/**
 * <p>
 * This class is a pojo holding an encrypted ballot and the key used to encrypt it, itself wrapped with another layer of
 * encryption.
 * </p>
 * <h2>Usage</h2>
 * <h3>Goal</h3>
 * This is an intermediate representation of the ballot, only used as a transition between the plain text ballot
 * contents and the doubly encrypted, authenticated ballot stored in the database, which also contains the ballotIndex
 * as associated data.
 * To prevent any possibility of linking the ballot content to a voter, the list of all
 * <tt>EncryptedBallotAndWrappedKey</tt>s is shuffled randomly after the first layer of decryption has been performed.
 * <h3>Encryption</h3>
 * <p>
 * When encrypting a ballot, instances of EncryptedBallotAndWrappedKey are created by:
 * <ul>
 * <li>generating a random symmetric key <tt>k_i</tt></li>
 * <li>the {@link #encryptedBallot} is obtained by encrypting the ballot with k_i into a SealedObject</li>
 * <li>the {@link #wrappedKey} is obtained by wrapping the key k_i using the Election officers public key</li>
 * </ul>
 * </p>
 * <h3>Decryption</h3>
 * <p>
 * When decrypting an authenticated ballot, instances of EncryptedBallotAndWrappedKey are created by:
 * <ul>
 * <li>retrieving an instance of AuthenticatedBallot from the ballot box</li>
 * <li>the {@link #encryptedBallot} is obtained by decrypting the
 * {@link ch.ge.ve.commons.crypto.ballot.AuthenticatedBallot#authenticatedEncryptedBallot}, using the ballotIndex as
 * authenticated data</li>
 * <li>the {@link #wrappedKey} is copied from the {@link ch.ge.ve.commons.crypto.ballot.AuthenticatedBallot#wrappedKey}</li>
 * </ul>
 * </p>
 */
public class EncryptedBallotAndWrappedKey implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * a ballot encrypted with a random key k_i
     */
    private final SealedObject encryptedBallot;

    /**
     * the random key k_i, wrapped with the Electoral Officers public key
     */
    private final byte[] wrappedKey;

    /**
     * @param encryptedBallot a sealed object containing the text representation of the ballot, encrypted with key <tt>k_i</tt>
     * @param wrappedKey      an array of bytes containing the key k_i wrapped with an asymmetric encryption algorithm
     */
    public EncryptedBallotAndWrappedKey(SealedObject encryptedBallot, byte[] wrappedKey) {
        this.encryptedBallot = encryptedBallot;
        this.wrappedKey = wrappedKey;
    }

    /**
     * @return the encryptedBallot
     */
    public SealedObject getEncryptedBallot() {
        return encryptedBallot;
    }

    /**
     * @return the wrappedKey
     */
    public byte[] getWrappedKey() {
        return wrappedKey;
    }
}
