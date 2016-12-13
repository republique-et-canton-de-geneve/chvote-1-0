package ch.ge.ve.commons.crypto;

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

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;

/**
 * Interface to retrieve the crypto primitives needed for anything other than the ballots...
 * For instance: database storage of electoral rolls and code lists
 */
public interface SensitiveDataCryptoUtilsConfiguration {
    /**
     * The cipher used for data stored in the database, such as, but not limited to:
     * <ul>
     * <li>Elector id</li>
     * <li>date of birth</li>
     * </ul>
     * relevant documentation: {@literal VE - Design de sécurité du système}
     *
     * @return the cipher to be used for encrypting data stored in the database
     */
    Cipher getCipher();

    /**
     * The mac algorithm used for data stored in the database, such as, but not limited to:
     * <ul>
     * <li>Card number</li>
     * <li>Confirmation code</li>
     * </ul>
     *
     * @return the mac algorithm used for data stored in the database,
     */
    Mac getMac();

    /**
     * Retrieve the secret key used for encryption and authenticated digests.
     *
     * @return the secret key used for encryption and authenticated digests
     */
    SecretKey getSecretKey();

    /**
     * Retrieve the number of iterations to use for PBKDF
     */
    int getIterations();

    /**
     * @return the algorithm to use for password-based key derivations
     */
    String getPbkdf2Algorithm();

    /**
     * @return the maximum byte size of a sealed object.
     */
    long getSealMaxBytes();
}
