/*
 * Copyright (C) 2023 The Guava Authors
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

package com.google.common.collect;

import static com.google.common.truth.Truth.assertWithMessage;
import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.PROTECTED;
import static java.lang.reflect.Modifier.PUBLIC;
import static java.util.Arrays.asList;

import com.google.common.base.Optional;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.google.common.reflect.TypeToken;
import java.lang.reflect.Method;
import junit.framework.TestCase;

/**
 * Tests that all package-private {@code writeReplace} methods are overridden in any existing
 * subclasses. Without such overrides, optimizers might put a {@code writeReplace}-containing class
 * and its subclass in different packages, causing the serialization system to fail to invoke {@code
 * writeReplace} when serializing an instance of the subclass. For an example of this problem, see
 * b/310253115.
 */
public class WriteReplaceOverridesTest extends TestCase {
  private static final ImmutableSet<String> GUAVA_PACKAGES =
      FluentIterable.of(
              "base",
              "cache",
              "collect",
              "escape",
              "eventbus",
              "graph",
              "hash",
              "html",
              "io",
              "math",
              "net",
              "primitives",
              "reflect",
              "util.concurrent",
              "xml")
          .transform("com.google.common."::concat)
          .toSet();

  public void testClassesHaveOverrides() throws Exception {
    for (ClassInfo info : ClassPath.from(getClass().getClassLoader()).getAllClasses()) {
      if (!GUAVA_PACKAGES.contains(info.getPackageName())) {
        continue;
      }
      if (info.getName().endsWith("GwtSerializationDependencies")) {
        continue; // These classes exist only for the GWT compiler, not to be used.
      }
      if (
      /*
       * At least one of the classes nested inside TypeResolverTest triggers a bug under older JDKs:
       * https://bugs.openjdk.org/browse/JDK-8215328 -> https://bugs.openjdk.org/browse/JDK-8215470
       * https://github.com/google/guava/blob/4f12c5891a7adedbaa1d99fc9f77d8cc4e9da206/guava-tests/test/com/google/common/reflect/TypeResolverTest.java#L201
       */
      info.getName().contains("TypeResolverTest")
          /*
           * And at least one of the classes inside TypeTokenTest ends up with a null value in
           * TypeMappingIntrospector.mappings. That happens only under older JDKs, too, so it may
           * well be a JDK bug.
           */
          || info.getName().contains("TypeTokenTest")
      /*
       * Luckily, we don't care about analyzing tests at all. We'd skip them all if we could do so
       * trivially, but it's enough to skip these ones.
       */
      ) {
        continue;
      }
      Class<?> clazz = info.load();
      try {
        Method unused = clazz.getDeclaredMethod("writeReplace");
        continue; // It overrides writeReplace, so it's safe.
      } catch (NoSuchMethodException e) {
        // This is a class whose supertypes we want to examine. We'll do that below.
      }
      Optional<Class<?>> supersWithPackagePrivateWriteReplace =
          FluentIterable.from(TypeToken.of(clazz).getTypes())
              .transform(TypeToken::getRawType)
              .transformAndConcat(c -> asList(c.getDeclaredMethods()))
              .firstMatch(
                  m ->
                      m.getName().equals("writeReplace")
                          && m.getParameterTypes().length == 0
                          // Only package-private methods are a problem.
                          && (m.getModifiers() & (PUBLIC | PROTECTED | PRIVATE)) == 0)
              .transform(Method::getDeclaringClass);
      if (!supersWithPackagePrivateWriteReplace.isPresent()) {
        continue;
      }
      assertWithMessage(
              "To help optimizers, any class that inherits a package-private writeReplace() method"
                  + " should override that method.\n"
                  + "(An override that delegates to the supermethod is fine.)\n"
                  + "%s has no such override despite inheriting writeReplace() from %s",
              clazz.getName(), supersWithPackagePrivateWriteReplace.get().getName())
          .fail();
    }
  }
}
