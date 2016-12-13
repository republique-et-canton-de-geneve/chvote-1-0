package ch.ge.ve.commons.properties;

/*-
 * #%L
 * Common properties
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

import java.util.Properties;

/**
 * Any module of the application can register the publication of its own {@code Properties}
 * that will be provided through the {@code PropertyConfigurationService}.
 *
 * The registration uses the Java {@code ServiceLoader} mechanism.
 *
 */
public interface PropertyConfigurationProvider {
    /**
     * The properties of the module
     *
     * @return an non null Properties object
     */
    Properties getProperties();
}
