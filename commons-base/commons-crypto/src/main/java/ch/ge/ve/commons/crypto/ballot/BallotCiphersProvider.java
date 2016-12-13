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

import ch.ge.ve.commons.crypto.exceptions.PrivateKeyPasswordMismatchException;
import ch.ge.ve.commons.properties.PropertyConfigurationService;

import javax.crypto.Cipher;

import java.security.Key;

/**
 * This interface defines the contracts for a {@link BallotCiphersProvider}, which is used by the {@link BallotCipherService}.
 */
public interface BallotCiphersProvider {
    /**
     * Nom de la propriété contenant l'algorithme de chiffrement utilisé pour chiffrer les votes.
     */
    String BALLOT_CRYPTING_ALGORITHM = "common.crypto.ballot.cipher.algorithm";

    /**
     * Nom de la propriété contenant la configuration des blocs pour le chiffrement du bulletin.
     */
    String BALLOT_CRYPTING_BLOCK_MODE = "common.crypto.ballot.cipher.blockmode";

    /**
     * Nom de la propriété contenant la taille de la clé de chiffrement utilisée pour chiffrer les votes
     */
    String BALLOT_CRYPTING_KEY_SIZE = "common.crypto.ballot.cipher.keySize";

    /**
     * Nom de la propriété contenant l'algorithme de chiffrement utilisé pour chiffrer les clés spécifiques à chaque bulletin.
     */
    String BALLOT_KEY_CRYPTING_ALGORITHM = "common.crypto.electoralBoard.cipher.algorithm";

    /**
     * Nom de la propriété contenant la configuration des blocs pour le chiffrement des clés spécifiques à chaque bulletin.
     */
    String BALLOT_KEY_CRYPTING_BLOCKMODE = "common.crypto.electoralBoard.cipher.blockmode";

    /**
     * Nom de la propriété contenant la taille de la clé de chiffrement utilisée pour chiffrer les clés spécifiques à chaque bulletin.
     */
    String BALLOT_KEY_CRYPTING_KEY_SIZE = "common.crypto.electoralBoard.cipher.keySize";
    /**
     * Nom de la propriété contenant l'algorithme de chiffrement utilisé pour chiffrer le compteur de votes.
     */
    String BALLOT_INTEGRITY_CHECK_CRYPTING_ALGORITHM = "common.crypto.integrity.algorithm";

    /**
     * Nom de la propriété contenant la configuration des blocs pour le chiffrement du compteur de votes.
     */
    String BALLOT_INTEGRITY_CHECK_CRYPTING_BLOCK_MODE = "common.crypto.integrity.blockmode";

    /**
     * Nom de la propriété contenant la taille de la clé de chiffrement utilisée pour chiffrer le compteur de votes.
     */
    String BALLOT_INTEGRITY_CHECK_CRYPTING_KEY_SIZE = "common.crypto.integrity.keySize";

    /**
     * Nom de la propriété contenant le chemin du fichier contenant la clé publique de chiffrement des votes.
     */
    String PUBLIC_KEY_FILE_NAME = "common.crypto.electoralBoard.cipher.publicKey.fileName";

    /**
     * Nom de la propriété contenant le chemin du fichier contenant la clé privée de déchiffrement des votes.
     */
    String PRIVATE_KEY_FILE_NAME = "common.crypto.electoralBoard.cipher.privateKey.fileName";

    /**
     * Name of the alias for the private key
     */
    String PRIVATE_KEY_ALIAS = "common.crypto.electoralBoard.cipher.privateKey.alias";

    /**
     * Nom de la propriété contenant le chemin du fichier contenant la clé symétrique utilisée
     * pour assurer l'intégrité de l'urne.
     */
    String INTEGRITY_KEY_FILE_NAME = "common.crypto.integrity.secretKey.fileName";

    /**
     * Getter for the ballot cipher
     *
     * @return the {@link Cipher} to be used for ballot encryption
     */
    Cipher getBallotCipher();

    /**
     * Getter for the ballot cipher size
     *
     * @return the length of the key to be used by the ballot cipher
     */
    int getBallotCipherSize();

    /**
     * Getter for the ballotKey Cipher
     *
     * @return the {@link Cipher} to be used for ballotKey encryption
     */
    Cipher getBallotKeyCipher();

    /**
     * Getter for the public key used for ballotKey encryption
     *
     * @return the {@link Key} to be used
     */
    Key getBallotKeyCipherPublicKey();

    /**
     * Getter for the private key used for ballotKey decryption.
     * <p>{@link #loadBallotKeyCipherPrivateKey(String)} needs to be called beforehand!</p>
     *
     * @return the {@link Key} to be used
     */
    Key getBallotKeyCipherPrivateKey();

    /**
     * Getter for the integrityCheck Key
     *
     * @return the {@link Key} to be used to encrypt the ballot for the integrity layer
     */
    Key getIntegrityCheckSecretKey();

    /**
     * Setter for the propertyConfigurationService, in which to find the configuration information
     *
     * @param propertyConfigurationService the configuration service to use
     */
    void setPropertyConfigurationService(PropertyConfigurationService propertyConfigurationService);

    /**
     * Getter for the configurationService from which this provider draws its settings
     *
     * @return the configuration service being used
     */
    PropertyConfigurationService getPropertyConfigurationService();

    /**
     * Unlocks the privateKey retrieved from the path defined in the configuration service under the {@link #PRIVATE_KEY_FILE_NAME} key.
     *
     * @param password the password needed to unlock the private key.
     * @throws PrivateKeyPasswordMismatchException
     */
    void loadBallotKeyCipherPrivateKey(String password) throws PrivateKeyPasswordMismatchException;

    /**
     * Creates a new AEAD block cipher.
     *
     * @return a Cipher with AEAD capabilities
     * @param propertyConfigurationService the configuration service to use
     */
    Cipher getIntegrityCipher(PropertyConfigurationService propertyConfigurationService);

    /**
     * @return the length of the MAC tag created by AEAD algorithm
     */
    int getMacLength();

    /**
     * Clears the private key in memory.
     */
    void invalidatePrivateKeyCache();

    /**
     * Clears the public key in memory.
     */
    void invalidatePublicKeyCache();

    /**
     * Clears the integrity key in memory.
     */
    void invalidateIntegrityKeyCache();
}
