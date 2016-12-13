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
import ch.ge.ve.commons.crypto.exceptions.CryptoConfigurationRuntimeException;
import ch.ge.ve.commons.fileutils.OutputFilesPattern;
import ch.ge.ve.commons.properties.PropertyConfigurationException;
import ch.ge.ve.commons.properties.PropertyConfigurationService;
import ch.ge.ve.offlineadmin.exception.MissingKeyFilesException;

import com.google.common.base.Preconditions;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static ch.ge.ve.offlineadmin.util.SecurityConstants.*;

/**
 * This factory creates instances of {@link ch.ge.ve.commons.crypto.ballot.BallotCipherService} either for encryption or
 * for decryption, by being given a directory where the encryption/decryption keys are located.
 * It verifies that the expected files are present in the directory and instantiates the
 * {@link ch.ge.ve.commons.crypto.ballot.BallotCipherService} accordingly.
 */
public class BallotCipherServiceFactory {

    private final OutputFilesPattern outputFilesPattern = new OutputFilesPattern();
    private final PropertyConfigurationService propertyConfigurationService;
    private String integrityKeyFilenamePatternProperty;
    private String privateKeyFilenamePatternProperty;
    private String publicKeyFilenamePatternProperty;

    public BallotCipherServiceFactory(PropertyConfigurationService propertyConfigurationService) {
        this.propertyConfigurationService = propertyConfigurationService;
        init(propertyConfigurationService);
    }

    private void init(PropertyConfigurationService propertyConfigurationService) {
        try {
            integrityKeyFilenamePatternProperty = propertyConfigurationService.getConfigValue(INTEGRITY_KEY_FILENAME_PATTERN);
            publicKeyFilenamePatternProperty = propertyConfigurationService.getConfigValue(CERT_PUBLIC_KEY_FILENAME_PATTERN);
            privateKeyFilenamePatternProperty = propertyConfigurationService.getConfigValue(CERT_PRIVATE_KEY_FILENAME_PATTERN);
        } catch (PropertyConfigurationException e) {
            throw new CryptoConfigurationRuntimeException("key files names configuration missing", e);
        }
    }

    /**
     * Creates an instance of a {@link ch.ge.ve.commons.crypto.ballot.BallotCipherService} for encryption
     *
     * @param keyDirectory the directory containing the keys
     * @return an initialized {@link ch.ge.ve.commons.crypto.ballot.BallotCipherService} ready for encryption
     * @throws MissingKeyFilesException if the directory does not contain the expected files
     */
    public BallotCipherService encryptionBallotCipherService(File keyDirectory) throws MissingKeyFilesException {
        Preconditions.checkNotNull(keyDirectory);
        Preconditions.checkArgument(keyDirectory.isDirectory());

        EncryptionBallotCiphersProvider ciphersProvider = new EncryptionBallotCiphersProvider();

        List<Pattern> expectedFilesNamePatterns = new ArrayList<>();
        Pattern integrityKeyFileNamePattern = Pattern.compile(integrityKeyFilenamePatternProperty);
        expectedFilesNamePatterns.add(integrityKeyFileNamePattern);
        Pattern publicKeyFilenamePattern = Pattern.compile(publicKeyFilenamePatternProperty);
        expectedFilesNamePatterns.add(publicKeyFilenamePattern);

        directoryContainsRequiredKeys(keyDirectory, expectedFilesNamePatterns);

        BallotCipherService ballotCipherService = new BallotCipherService(ciphersProvider, propertyConfigurationService);

        final Optional<Path> integrityKeyPath = outputFilesPattern.findFirstFileByPattern(integrityKeyFileNamePattern, keyDirectory.toPath());
        ballotCipherService.setIntegrityKeyFileName(
                integrityKeyPath
                        .orElseThrow(() -> new MissingKeyFilesException("integrity key file not found in directory: " + keyDirectory.getAbsolutePath()))
                        .toString()
        );

        final Optional<Path> pubKeyPath = outputFilesPattern.findFirstFileByPattern(publicKeyFilenamePattern, keyDirectory.toPath());
        ballotCipherService.setPublicKeyFileName(
                pubKeyPath
                        .orElseThrow(() -> new MissingKeyFilesException("public key file not found in directory: " + keyDirectory.getAbsolutePath()))
                        .toString()
        );

        return ballotCipherService;
    }

    /**
     * Creates an instance of a {@link ch.ge.ve.commons.crypto.ballot.BallotCipherService} for decryption
     *
     * @param keyDirectory the directory containing the keys
     * @return an initialized {@link ch.ge.ve.commons.crypto.ballot.BallotCipherService} ready for decryption
     * @throws MissingKeyFilesException if the directory does not contain the expected files
     */
    public BallotCipherService decryptionBallotCipherService(File keyDirectory) throws MissingKeyFilesException {
        Preconditions.checkNotNull(keyDirectory);
        Preconditions.checkArgument(keyDirectory.isDirectory());

        DecryptionBallotCiphersProvider ciphersProvider = new DecryptionBallotCiphersProvider(keyDirectory);

        List<Pattern> expectedFilesNamesPatterns = new ArrayList<>();
        Pattern integrityKeyFileNamePattern = Pattern.compile(integrityKeyFilenamePatternProperty);
        expectedFilesNamesPatterns.add(integrityKeyFileNamePattern);
        Pattern privateKeyFilenamePattern = Pattern.compile(privateKeyFilenamePatternProperty);
        expectedFilesNamesPatterns.add(privateKeyFilenamePattern);

        directoryContainsRequiredKeys(keyDirectory, expectedFilesNamesPatterns);

        BallotCipherService ballotCipherService = new BallotCipherService(ciphersProvider, propertyConfigurationService);

        final Optional<Path> integrityKeyPath = outputFilesPattern.findFirstFileByPattern(integrityKeyFileNamePattern, keyDirectory.toPath());
        ballotCipherService.setIntegrityKeyFileName(
                integrityKeyPath
                        .orElseThrow(() -> new MissingKeyFilesException("integrity key file not found in directory: " + keyDirectory.getAbsolutePath()))
                        .toString()
        );

        final Optional<Path> privKeyPath = outputFilesPattern.findFirstFileByPattern(privateKeyFilenamePattern, keyDirectory.toPath());
        ballotCipherService.setPrivateKeyFileName(
                privKeyPath
                        .orElseThrow(() -> new MissingKeyFilesException("private key file not found in directory: " + keyDirectory.getAbsolutePath()))
                        .toString()
        );

        return ballotCipherService;
    }

    private void directoryContainsRequiredKeys(File keyDirectory, List<Pattern> expectedFilesNames) throws MissingKeyFilesException {
        List<String> filenames = Arrays.asList(keyDirectory.list());
        if (!expectedFilesNames.stream().allMatch(pattern -> filenames.stream().anyMatch(pattern.asPredicate()))) {
            throw new MissingKeyFilesException("key files not found in directory: " + keyDirectory.getAbsolutePath());
        }
    }

}
