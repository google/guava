/*
 * Copyright (C) 2020 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.net;

import com.google.common.collect.ImmutableSet;
import junit.framework.TestCase;
import java.lang.reflect.Field;

/**
 * Tests for the HttpStatus class.
 *
 * @author William Collishaw
 */
public class HttpStatusTest extends TestCase {

	public void testWithinValidStatusCodeRange() throws IllegalAccessException {
		for (Field field : relevantFields()) {
			int code = (int) field.get(null);
			assertTrue(code >= 100 && code <= 599);
		}
	}

	// Visible for other tests to use
	private static ImmutableSet<Field> relevantFields() {
		ImmutableSet.Builder<Field> builder = ImmutableSet.builder();
		for (Field field : HttpStatus.class.getDeclaredFields()) {
			/*
			 * Coverage mode generates synthetic fields.  If we ever add private
			 * fields, they will cause similar problems, and we may want to switch
			 * this check to isAccessible().
			 */
			if (!field.isSynthetic() && field.getType() == int.class) {
				builder.add(field);
			}
		}
		return builder.build();
	}
}