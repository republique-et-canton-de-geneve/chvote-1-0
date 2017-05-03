package ch.ge.ve.commons.fileutils;

/*-
 * #%L
 * Common file utilities
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

import ch.ge.ve.commons.crypto.utils.MessageDigestFactory;
import ch.ge.ve.commons.properties.PropertyConfigurationService;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * This utility class is used to perform the computation of the hashes of files.
 */
public class StreamHasher {
    private final PropertyConfigurationService propertyConfigurationService;
    private final ThreadLocal<MessageDigest> messageDigestThreadLocal = new ThreadLocal<>();

    /**
     * Constructor
     *
     * @param propertyConfigurationService the service defining the required properties
     */
    public StreamHasher(PropertyConfigurationService propertyConfigurationService) {
        this.propertyConfigurationService = propertyConfigurationService;
    }

    /**
     * Compute in a thread safe manner the hash of the file for which an inputStream is
     * provided with the default configured message digest, as in common-crypto.properties.
     *
     * @param inputStream an input stream for the file for which the hash should be computed
     * @return the hash of the file
     * @throws IOException
     */
    public byte[] threadSafeComputeHash(InputStream inputStream) throws IOException {
        return computeHash(inputStream, getThreadSafeMessageDigest());
    }

    /**
     * Compute the hash of the file for which an inputStream is provided, using the provided digest
     *
     * @param inputStream an input stream for which the hash should be computed
     * @param digest      the digest to be used for computing the hash
     * @return the hash of the stream contents
     * @throws IOException
     */
    public byte[] computeHash(InputStream inputStream, MessageDigest digest) throws IOException {
        byte[] bytes = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(bytes)) != -1) {
            digest.update(bytes, 0, bytesRead);
        }

        return digest.digest();
    }

    private MessageDigest getThreadSafeMessageDigest() {
        if (messageDigestThreadLocal.get() == null) {
            messageDigestThreadLocal.set(new MessageDigestFactory(propertyConfigurationService).getInstance());
        }
        return messageDigestThreadLocal.get();
    }
}
