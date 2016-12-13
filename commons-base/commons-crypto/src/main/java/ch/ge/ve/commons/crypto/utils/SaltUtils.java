package ch.ge.ve.commons.crypto.utils;

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

import com.google.common.base.Preconditions;

import java.security.SecureRandom;

/**
 * This utilities class contains method used to generate random salt
 */
public class SaltUtils {
    private static final SecureRandom SECURE_RANDOM = SecureRandomFactory.createPRNG();

    // Mask default constructor, this class should not be instantiated
    private SaltUtils() {}

    /**
     * Generates a salt using Java SecureRandom
     *
     * @param lengthInBits desired length of the salt
     * @return the salt
     */
    public static byte[] generateSalt(int lengthInBits) {
        Preconditions.checkArgument(lengthInBits % 8 == 0, String.format("The salt length must be a multiple of 8, but was %d!", lengthInBits));
        byte[] salt = new byte[lengthInBits / 8];
        SECURE_RANDOM.nextBytes(salt);
        return salt;
    }
}
