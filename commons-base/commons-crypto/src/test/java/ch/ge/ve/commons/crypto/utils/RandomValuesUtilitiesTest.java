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

import ch.ge.ve.commons.properties.PropertyConfigurationException;
import ch.ge.ve.commons.properties.PropertyConfigurationService;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;

import static ch.ge.ve.commons.crypto.matchers.CodeFormatMatchers.validCodeFormat;
import static ch.ge.ve.commons.crypto.matchers.CodeFormatMatchers.validFinalizationCodeFormat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * This test suit aims at covering the {@link RandomValuesUtilities} utility class.
 */
public class RandomValuesUtilitiesTest {
    private static final Logger LOGGER = Logger.getLogger(RandomValuesUtilitiesTest.class);

    private static final long PREFIX_MULTIPLIER = 100000000000000L;

    @Mock
    private PropertyConfigurationService propertyConfigurationService;

    @Before
    public void setUp() throws PropertyConfigurationException {
        initMocks(this);
    }

    /**
     * should be able to generate a large number of cards, logging the execution time
     */
    @Test
    public void should_be_able_to_generate_many_cardnumbers() throws IOException {
        RandomValuesUtilities randomValuesUtilities = RandomValuesUtilities.createRandomValuesUtilities();

        long t0 = System.currentTimeMillis();
        long t1 = t0;
        int j = 0;
        for (int i = 0; i < 50000; i++) {
            String cardNumber = randomValuesUtilities.generateUniqueCardNumber(16, 15);
            assertThat(cardNumber, not(isEmptyOrNullString()));
            if (++j == 10000) {
                long t2 = System.currentTimeMillis();
                LOGGER.info((i + 1) + " cards produced in " + (t2 - t0) + "ms");
                LOGGER.info(" (last 10000 cards produced in " + (t2 - t1) + "ms)");
                t1 = t2;
                j = 0;
            }
        }
    }

    /**
     * if a prefix is specified, should not generate a card number with that prefix
     */
    @Test
    public void given_vota_prefix_generateUniqueCardNumber_should_not_give_card_numbers_begining_with_the_prefix() throws IOException {
        final int votaPrefix = 15;
        // Since there are 100 different 2-digit prefixes, not getting the vota prefix out of 10000 attempts
        // would have a probability 0.99^10000 < 3/10^44 if the generator isn't biased -> negligible
        final int numberOfTries = 10000;

        RandomValuesUtilities randomValuesUtilities = RandomValuesUtilities.createRandomValuesUtilities();

        final int cardNumberLength = 16;

        for (int i = 1; i < numberOfTries; i++) {
            String ucn = randomValuesUtilities.generateUniqueCardNumber(cardNumberLength, votaPrefix);
            assertThat(String.format("A number with the votaPrefix was generated on attempt %d", i), Long.valueOf(ucn), anyOf(lessThan(votaPrefix * PREFIX_MULTIPLIER), greaterThan((votaPrefix + 1) * PREFIX_MULTIPLIER - 1L)));
        }
    }

    /**
     * confirmation code should have A2B3 format
     */
    @Test
    public void confirmation_code_should_have_A2B3_format() throws Exception {
        RandomValuesUtilities randomValuesUtilities = RandomValuesUtilities.createRandomValuesUtilities();
        final String confirmationCode = randomValuesUtilities.generateConfirmationCode();
        assertThat(confirmationCode, validCodeFormat());
    }

    /**
     * finalization code should be a 5 digits string number whithout '0'
     */
    @Test
    public void finalization_code_should_be_5_numbers_caracters_without_zero() throws Exception {
        RandomValuesUtilities randomValuesUtilities = RandomValuesUtilities.createRandomValuesUtilities();
        final String finalizationCode = randomValuesUtilities.generateFinalizationCode();
        assertThat(finalizationCode, validFinalizationCodeFormat());
    }
}
