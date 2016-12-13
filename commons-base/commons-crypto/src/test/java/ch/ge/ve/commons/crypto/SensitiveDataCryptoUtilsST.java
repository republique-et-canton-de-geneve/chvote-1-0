/*
 * -
 * #%L
 * Common crypto utilities
 * %%
 * Copyright (C) 2016 République et Canton de Genève
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

package ch.ge.ve.commons.crypto;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigInteger;
import java.security.Security;
import java.util.concurrent.TimeUnit;

import static ch.ge.ve.commons.crypto.SensitiveDataCryptoUtils.generateStrongPasswordHash;
import static ch.ge.ve.commons.crypto.SensitiveDataCryptoUtils.validateStrongPasswordHash;

/**
 * This test suit aims at stress testing the {@link SensitiveDataCryptoUtils} utility class.
 */
public class SensitiveDataCryptoUtilsST {
    @BeforeClass
    public static void init() {
        Security.addProvider(new BouncyCastleProvider());
        SensitiveDataCryptoUtils.configure(new TestSensitiveDataCryptoUtilsConfiguration());
    }

    /**
     * Runs several times the {@link SensitiveDataCryptoUtils#validateStrongPasswordHash} method and logs the
     * average execution time to identify a potential timing attack. This verification is manual, nothing is checked in
     * this test case.
     */
    @Test
    public void analyzePotentialTimingAttacksOnPasswordHashVerification() {
        char[] passwd = "A random te$ting p#s$w0rd!!!".toCharArray();
        String passwordHash = generateStrongPasswordHash(passwd);

        int runs = 100;

        System.out.printf("%-25s | %15s | %s (%d runs)%n", "Test", "average (ms)", "total", runs);

        Stopwatch validPasswordChecking = Stopwatch.createStarted();
        for (int i = 0; i < runs; i++) {
            validateStrongPasswordHash(passwd, passwordHash);
        }
        validPasswordChecking.stop();
        logRuns("Valid password checks", validPasswordChecking, runs);

        Stopwatch invalidPasswordChecking = Stopwatch.createStarted();
        for (int i = 0; i < runs; i++) {
            validateStrongPasswordHash("randomsoinhvakdjailsjdf".toCharArray(), passwordHash);
        }
        invalidPasswordChecking.stop();
        logRuns("Invalid password checks", invalidPasswordChecking, runs);

        String[] parts = passwordHash.split(":");

        byte[] hash = fromHex(parts[2]);
        // invert last byte
        hash[hash.length - 1] = (byte) (0xff ^ hash[hash.length - 1]);

        String alteredHash = Joiner.on(":").join(parts[0], parts[1], toHex(hash));

        Stopwatch alteredHashByteChecking = Stopwatch.createStarted();
        for (int i = 0; i < runs; i++) {
            validateStrongPasswordHash(passwd, alteredHash);
        }
        alteredHashByteChecking.stop();
        logRuns("Altered hash checks", alteredHashByteChecking, runs);
    }

    private static String toHex(byte[] byteArray) {
        BigInteger bi = new BigInteger(1, byteArray);
        String hexString = bi.toString(16);
        // should be 2 chars per byte
        int paddingLength = (byteArray.length * 2) - hexString.length();
        String padding = "";
        if (paddingLength > 0) {
            padding = String.format("%0" + paddingLength + "d", 0);
        }
        return padding + hexString;
    }

    private static byte[] fromHex(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }

        return bytes;
    }

    private void logRuns(String label, Stopwatch stopwatch, int runs) {
        long total = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        double average = (double) total / runs;
        System.out.printf("%-25s | %15.2f | %15d%n", label, average, total);

    }
}
