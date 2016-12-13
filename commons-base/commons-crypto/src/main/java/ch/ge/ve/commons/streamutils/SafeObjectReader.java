package ch.ge.ve.commons.streamutils;

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

import java.io.*;
import java.util.List;

/**
 * This class provides for safer object deserialization, by limiting length of input, number and type of objects read.
 */
public class SafeObjectReader {

    private SafeObjectReader() {
        // utility class, do not allow to instantiate it
    }

    /**
     * This method should be used to replace unsafe calls to ObjectInputStream.readObject() built into Java.
     * It checks that only allowed classes are read, that the number of objects and bytes read stay within the given parameters.
     * Also, it casts the read object to the expected type.
     *
     * @param expectedType Class of the expected object
     * @param safeClasses  The list of Classes allowed to be read (on top of primitive arrays, numbers and Strings which are always considered safe)
     * @param maxObjects   The maximum number of objects allowed to be read
     * @param maxBytes     The maximum number of bytes allowed to be read
     * @param in           The InputStream containing an object from an untrusted source
     * @param <T>          The type the object will be cast to.
     * @return the object read from the stream, cast to the type parameter
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    public static <T> T safeReadObject(final Class<? extends T> expectedType, final List<Class<?>> safeClasses, final long maxObjects, final long maxBytes, InputStream in) throws IOException, ClassNotFoundException {
        // Create a FilterInputStream that checks the length of the input as it is being read.
        InputStream fis = new LimitedLengthFilterInputStream(in, maxBytes);

        ObjectInputStream ois = new SafeObjectInputStream(fis, maxObjects, expectedType, safeClasses);

        return (T) ois.readObject();

    }

    /**
     * This class limits the allowed length for reading objects
     */
    private static class LimitedLengthFilterInputStream extends FilterInputStream {
        private final long maxBytes;
        private long length;

        public LimitedLengthFilterInputStream(InputStream in, long maxBytes) {
            super(in);
            this.maxBytes = maxBytes;
            length = 0;
        }

        @Override
        public int read() throws IOException {
            int val = super.read();
            if (val != -1) {
                length++;
                checkLength();
            }
            return val;
        }

        @Override
        public int read(byte[] b, int off, int length) throws IOException {
            int val = super.read(b, off, length);
            if (val > 0) {
                this.length += val;
                checkLength();
            }
            return val;
        }

        private void checkLength() {
            if (length > maxBytes) {
                throw new SafeObjectSecurityRuntimeException("Security violation: attempt to deserialize too many bytes from stream. Limit is " + maxBytes);
            }
        }
    }

    /**
     * This specialized ObjectInputStream prevents too many objects from being unserialized, as well as filtering the
     * types of objects allowed.
     */
    private static class SafeObjectInputStream<T> extends ObjectInputStream {
        private final long maxObjects;
        private final Class<? extends T> type;
        private final List<Class<?>> safeClasses;
        boolean shouldResolveObjects;
        private int objectCount;

        public SafeObjectInputStream(InputStream fis, long maxObjects, Class<? extends T> type, List<Class<?>> safeClasses) throws IOException {
            super(fis);
            this.maxObjects = maxObjects;
            this.type = type;
            this.safeClasses = safeClasses;
            shouldResolveObjects = enableResolveObject(true);
            objectCount = 0;
        }

        @Override
        protected Object resolveObject(Object obj) throws IOException {
            if (objectCount++ > maxObjects) {
                throw new SafeObjectSecurityRuntimeException("Security violation: attempt to deserialize too many objects from stream. Limit is " + maxObjects);
            }
            return super.resolveObject(obj);
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            Class<?> clazz = super.resolveClass(desc);

            if (isSafeClass(clazz)) {
                return clazz;
            } else {
                throw new SafeObjectSecurityRuntimeException("Security violation: attempt to deserialize unauthorized " + clazz);
            }
        }

        private boolean isSafeClass(Class<?> clazz) {
            return clazz.isArray() ||
                    clazz.equals(type) ||
                    clazz.equals(String.class) ||
                    Number.class.isAssignableFrom(clazz) ||
                    safeClasses.contains(clazz);
        }
    }
}
