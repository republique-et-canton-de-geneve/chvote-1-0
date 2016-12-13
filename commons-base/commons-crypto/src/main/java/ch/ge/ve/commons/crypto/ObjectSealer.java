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

import ch.ge.ve.commons.crypto.exceptions.CryptoOperationRuntimeException;
import ch.ge.ve.commons.crypto.utils.SecureRandomFactory;
import ch.ge.ve.commons.streamutils.SafeObjectReader;
import com.google.common.base.Preconditions;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SealedObject;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * This utility class offers the mechanisms to seal and unseal objects using a pre-determined cipher and key
 */
public class ObjectSealer {
    /**
     * 4 instances are needed for an ObjectSealer.
     */
    private static final int MAX_OBJECTS = 4;

    private final Cipher cipher;
    private final Key key;

    public ObjectSealer(Cipher cipher, Key key) {
        Preconditions.checkNotNull(cipher, "A valid cipher must be defined");
        Preconditions.checkNotNull(key, "A valid key must be defined");
        this.cipher = cipher;
        this.key = key;
    }

    /**
     * Wraps any serializable object into a SealedObject and returns the corresponding byte array
     *
     * @param object the object to seal
     * @return the byte array representing the SealedObject (locked with the cipher and key provided to the constructor)
     * @throws CryptoOperationRuntimeException
     * @see #unsealObject(byte[], long) the matching unwrapping method
     */
    public byte[] sealObject(Serializable object) {
        ByteArrayOutputStream byteArrayOutputStream = null;
        try {
            cipher.init(Cipher.ENCRYPT_MODE, key, SecureRandomFactory.createPRNG());
            SealedObject sealedObject = new SealedObject(object, cipher);
            byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(sealedObject);
        } catch (InvalidKeyException | IOException | IllegalBlockSizeException e) {
            throw new CryptoOperationRuntimeException("cannot seal object", e);
        }

        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Parses a SealedObject from the given byte array and retrieves the original wrapped object
     *
     * @param encryptedObject a byte array representing a SealedObject
     * @param maxBytes        the maximum size allowed for the read object
     * @return the original Serializable object
     * @throws CryptoOperationRuntimeException
     * @see #sealObject(java.io.Serializable) the matching wrapping operation
     */
    public Object unsealObject(byte[] encryptedObject, long maxBytes) {
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(encryptedObject);
            SealedObject sealedObject = SafeObjectReader.safeReadObject(SealedObject.class, new ArrayList<>(), MAX_OBJECTS, maxBytes, byteArrayInputStream);
            return sealedObject.getObject(key);
        } catch (IOException | ClassNotFoundException | InvalidKeyException | NoSuchAlgorithmException e) {
            throw new CryptoOperationRuntimeException("cannot unseal object", e);
        }

    }
}
