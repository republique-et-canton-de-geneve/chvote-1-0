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

import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * X509 certificate utility
 */
public class CertificateUtils {
    /**
     *
     * @return a new keystore suitable for the ballot box encryption keys certificate.
     * @throws KeyStoreException
     */
    public static KeyStore createPKCS12KeyStore() throws KeyStoreException {
        return KeyStore.getInstance("PKCS12");
    }

    /**
     * Retrieves the certificate passed in inputStream
     *
     * @param inputStream the inputStream
     * @return the certificate
     * @throws java.security.cert.CertificateException
     *
     */
    public X509Certificate getCertificate(InputStream inputStream) throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X509");
        return (X509Certificate) cf.generateCertificate(inputStream);
    }
}