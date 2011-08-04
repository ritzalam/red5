package org.red5.server.net.filter;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 *
 * Copyright (c) 2006-2011 by respective authors (see below). All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 2.1 of the License, or (at your option) any later
 * version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along
 * with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.mina.core.buffer.IoBuffer;

/**
 * A default {@link MessageSizeEstimator} implementation.
 * <p>
 * <a href="http://martin.nobilitas.com/java/sizeof.html">Martin's Java Notes</a>
 * was used for estimation of the size of non-{@link IoBuffer}s.  For unknown
 * types, it inspects declaring fields of the class of the specified message.
 * The size of unknown declaring fields are approximated to the specified
 * <tt>averageSizePerField</tt> (default: 64).
 * <p>
 * All the estimated sizes of classes are cached for performance improvement.
 * 
 * <br />
 * This originated from the Mina sandbox.
 */
public class DefaultMessageSizeEstimator implements MessageSizeEstimator {

	private final ConcurrentMap<Class<?>, Integer> class2size = new ConcurrentHashMap<Class<?>, Integer>();

	public DefaultMessageSizeEstimator() {
		class2size.put(boolean.class, 4); // Probably an integer.
		class2size.put(byte.class, 1);
		class2size.put(char.class, 2);
		class2size.put(short.class, 2);
		class2size.put(int.class, 4);
		class2size.put(long.class, 8);
		class2size.put(float.class, 4);
		class2size.put(double.class, 8);
		class2size.put(void.class, 0);
	}

	public int estimateSize(Object message) {
		if (message == null) {
			return 8;
		}

		int answer = 8 + estimateSize(message.getClass(), null);

		if (message instanceof IoBuffer) {
			answer += ((IoBuffer) message).remaining();
		} else if (message instanceof CharSequence) {
			answer += ((CharSequence) message).length() << 1;
		} else if (message instanceof Iterable<?>) {
			for (Object m : (Iterable<?>) message) {
				answer += estimateSize(m);
			}
		}

		return align(answer);
	}

	private int estimateSize(Class<?> clazz, Set<Class<?>> visitedClasses) {
		Integer objectSize = class2size.get(clazz);
		if (objectSize != null) {
			return objectSize;
		}

		if (visitedClasses != null) {
			if (visitedClasses.contains(clazz)) {
				return 0;
			}
		} else {
			visitedClasses = new HashSet<Class<?>>();
		}

		visitedClasses.add(clazz);

		int answer = 8; // Basic overhead.
		for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
			Field[] fields = c.getDeclaredFields();
			for (Field f : fields) {
				if ((f.getModifiers() & Modifier.STATIC) != 0) {
					// Ignore static fields.
					continue;
				}

				answer += estimateSize(f.getType(), visitedClasses);
			}
		}

		visitedClasses.remove(clazz);

		// Some alignment.
		answer = align(answer);

		// Put the final answer.
		class2size.putIfAbsent(clazz, answer);
		return answer;
	}

	private static int align(int size) {
		if (size % 8 != 0) {
			size /= 8;
			size++;
			size *= 8;
		}

		return size;
	}
}
