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

import ch.ge.ve.commons.crypto.ballot.BallotCiphersProvider;
import ch.ge.ve.commons.crypto.utils.CertificateUtils;
import ch.ge.ve.commons.crypto.utils.SecureRandomFactory;
import ch.ge.ve.commons.properties.PropertyConfigurationException;
import ch.ge.ve.commons.properties.PropertyConfigurationService;
import ch.ge.ve.offlineadmin.exception.KeyGenerationRuntimeException;
import org.bouncycastle.asn1.DERBMPString;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static ch.ge.ve.offlineadmin.util.SecurityConstants.*;

/**
 * Generator of the keys used for the ballots box
 */
public class KeyGenerator {
    public static final String DN_FORMAT = "cn=%1$s, ou=%2$s, o=%3$s, c=%4$s";
    /**
     * As per RFC-3280, section 4.1.2.2, a certificate serial number must be a positive integer fitting in 20 bytes.
     * Therefore it's bitLength can be of <tt>160 (20 * 8)</tt> and max value is <tt>2^160 - 1</tt>.
     */
    public static final int CERT_SERIAL_NUMBER_BIT_SIZE = 160;
    private final SecureRandom secureRandom;
    private final PropertyConfigurationService propertyConfigurationService;

    public KeyGenerator(PropertyConfigurationService propertyConfigurationService) {
        this.propertyConfigurationService = propertyConfigurationService;
        secureRandom = SecureRandomFactory.createPRNG();
    }

    /**
     * Generates the ballots box integrity key
     *
     * @return the integrity key
     * @throws PropertyConfigurationException
     */
    public SecretKey generateSecretKey() throws PropertyConfigurationException {
        String algorithm = propertyConfigurationService.getConfigValue(BallotCiphersProvider.BALLOT_INTEGRITY_CHECK_CRYPTING_ALGORITHM);
        Integer keyLengthInBits = propertyConfigurationService.getConfigValueAsInt(BallotCiphersProvider.BALLOT_INTEGRITY_CHECK_CRYPTING_KEY_SIZE);

        byte[] keyBytes = new byte[keyLengthInBits / BITS_PER_BYTE];
        secureRandom.nextBytes(keyBytes);

        return new SecretKeySpec(keyBytes, algorithm);
    }

    /**
     * Generates the key pair
     *
     * @return the key pair
     * @throws KeyGenerationRuntimeException  thrown if the configuration of the key pair is wrong
     * @throws PropertyConfigurationException
     */
    public KeyPair generateKeyPair() throws PropertyConfigurationException {
        String algorithm = propertyConfigurationService.getConfigValue(BallotCiphersProvider.BALLOT_KEY_CRYPTING_ALGORITHM);
        Integer keyLengthInBits = propertyConfigurationService.getConfigValueAsInt(BallotCiphersProvider.BALLOT_KEY_CRYPTING_KEY_SIZE);

        KeyPairGenerator keyPairGenerator = null;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new KeyGenerationRuntimeException("key pair configuration error", e);
        }
        keyPairGenerator.initialize(keyLengthInBits, secureRandom);

        return keyPairGenerator.generateKeyPair();
    }

    /**
     * Creates a {@link KeyStore} for the secret key
     *
     * @param privateKey  the private key
     * @param certificate the certificate
     * @param password    the password
     * @return a keystore protected by password
     * @throws KeyGenerationRuntimeException  thrown if the key store cannot be created
     * @throws PropertyConfigurationException
     */
    public KeyStore createKeyStore(PrivateKey privateKey, X509Certificate certificate, char[] password) throws PropertyConfigurationException {
        String certAlias = propertyConfigurationService.getConfigValue(BallotCiphersProvider.PRIVATE_KEY_ALIAS);

        KeyStore store = null;
        try {
            store = CertificateUtils.createPKCS12KeyStore();
            store.load(null);

            X509Certificate[] chain = new X509Certificate[1];
            chain[0] = certificate;

            store.setKeyEntry(certAlias, privateKey, password, chain);
        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException e) {
            throw new KeyGenerationRuntimeException("keystore creation error", e);
        }

        return store;
    }

    /**
     * Generates a certificate corresponding to the given key pair
     *
     * @param keyPair the key pair
     * @return the certificate
     * @throws KeyGenerationRuntimeException  thrown if the x509 structure or certificate cannot be generated
     * @throws PropertyConfigurationException
     */
    public X509Certificate generateCertificate(KeyPair keyPair) throws PropertyConfigurationException {
        try {
            X509v3CertificateBuilder certificateBuilder = createCertificateBuilder(keyPair);
            ContentSigner signer = createSigner(keyPair);

            return (X509Certificate) createCertificate(certificateBuilder, signer);
        } catch (OperatorCreationException | CertificateException | IOException e) {
            throw new KeyGenerationRuntimeException("error when generating the x509 certificate", e);
        }
    }

    private X509v3CertificateBuilder createCertificateBuilder(KeyPair keyPair) throws PropertyConfigurationException, CertIOException {
        X500NameBuilder nameBuilder = new X500NameBuilder(BCStyle.INSTANCE);
        nameBuilder.addRDN(BCStyle.CN, propertyConfigurationService.getConfigValue(CERT_COMMON_NAME_PROPERTY));
        nameBuilder.addRDN(BCStyle.O, propertyConfigurationService.getConfigValue(CERT_ORGANISATION_PROPERTY));
        nameBuilder.addRDN(BCStyle.OU, propertyConfigurationService.getConfigValue(CERT_ORGANISATIONAL_UNIT_PROPERTY));
        nameBuilder.addRDN(BCStyle.C, propertyConfigurationService.getConfigValue(CERT_COUNTRY_PROPERTY));
        X500Name x500Name = nameBuilder.build();

        BigInteger serial = new BigInteger(CERT_SERIAL_NUMBER_BIT_SIZE, SecureRandomFactory.createPRNG());

        SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());

        Date startDate = new Date();
        Date endDate = Date.from(startDate.toInstant().plus(propertyConfigurationService.getConfigValueAsInt(CERT_VALIDITY_DAYS_PROPERTY), ChronoUnit.DAYS));

        X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(x500Name, serial, startDate, endDate, x500Name, publicKeyInfo);

        String certFriendlyName = propertyConfigurationService.getConfigValue(CERT_PRIVATE_FRIENDLY_NAME_PROPERTY);
        certificateBuilder.addExtension(PKCSObjectIdentifiers.pkcs_9_at_friendlyName, false, new DERBMPString(certFriendlyName));
        return certificateBuilder;
    }

    private ContentSigner createSigner(KeyPair keyPair) throws PropertyConfigurationException, OperatorCreationException {
        ContentSigner signer;
        String hashAlgo = propertyConfigurationService.getConfigValue(CERT_HASH_ALGORITHM);
        if (keyPair.getPrivate() instanceof RSAPrivateKey) {
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(hashAlgo + "withRSA");
            AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
            signer = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(
                    new RSAKeyParameters(true, privateKey.getModulus(), privateKey.getPrivateExponent())
            );
        } else {
            throw new KeyGenerationRuntimeException("Unsupported key type");
        }
        return signer;
    }

    private java.security.cert.Certificate createCertificate(X509v3CertificateBuilder certificateBuilder, ContentSigner signer) throws CertificateException, IOException {
        X509CertificateHolder certificateHolder = certificateBuilder.build(signer);

        return CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certificateHolder.getEncoded()));
    }
}
