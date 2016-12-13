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
import ch.ge.ve.commons.crypto.ballot.BallotCiphersProviderDefaultImpl;

import java.security.Key;

/**
 * Ballot encryption ciphers provider, used by {@link BallotCipherService}
 */
public class EncryptionBallotCiphersProvider extends BallotCiphersProviderDefaultImpl {
    @Override
    public Key getBallotKeyCipherPrivateKey() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void loadBallotKeyCipherPrivateKey(String password) {
        throw new UnsupportedOperationException();
    }
}
