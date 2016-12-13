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

import ch.ge.ve.commons.properties.PropertyConfigurationService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

/**
 * This utility class contains method used to compute hashes from various sources.
 */
public class HashUtils {
    private final MessageDigest digest;

    /**
     * The algorithm used for computing hashes
     */
    public HashUtils(PropertyConfigurationService propertyConfigurationService) {
        MessageDigestFactory messageDigestFactory = new MessageDigestFactory(propertyConfigurationService);
        digest = messageDigestFactory.getInstance();
    }

    /**
     * Generates a hash
     *
     * @param input the data to hash
     * @return the salt
     */
    public byte[] computeHash(byte[] input) {
        digest.reset();
        digest.update(input);
        return digest.digest();
    }

    /**
     * Compute the hash of the file at the given filepath
     *
     * @param filePath the path to the file
     * @return a digest of the file using the default algorithm as configured in common-crypto.properties
     * @throws IOException
     */
    public byte[] computeFileHash(String filePath) throws IOException {
        return computeFileHash(new File(filePath));
    }

    /**
     * Compute the hash of the given file
     *
     * @param file the file for which a hash is requested
     * @return a digest of the file using the default algorithm as configured in common-crypto.properties
     * @throws IOException
     */
    private byte[] computeFileHash(File file) throws IOException {
        return computePathHash(file.toPath());
    }

    /**
     * Compute the hash of the file at the given path
     *
     * @param path the path to the file
     * @return a digest of the file using the default algorithm as configured in common-crypto.properties
     * @throws IOException
     */
    private byte[] computePathHash(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            return computeInputStreamHash(input);
        }
    }

    /**
     * Compute the hash of the given input stream
     *
     * @param inputStream the input for which the hash should be computed
     * @return a digest of the file using the default algorithm as configured in common-crypto.properties
     * @throws IOException
     */
    private byte[] computeInputStreamHash(InputStream inputStream) throws IOException {
        digest.reset();
        byte[] bytes = new byte[1024];
        int bytesRead;

        while ((bytesRead = inputStream.read(bytes)) != -1) {
            digest.update(bytes, 0, bytesRead);
        }

        return digest.digest();
    }
}
