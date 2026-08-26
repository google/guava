/*
 * Copyright (C) 2026 The Guava Authors
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

package com.google.common.testing;

import static com.google.common.base.StandardSystemProperty.JAVA_SPECIFICATION_VERSION;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.reflect.Reflection.initialize;
import static com.google.common.testing.ClassPathUtil.getClassPathUrls;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.google.errorprone.annotations.MustBeClosed;
import com.google.j2objc.annotations.J2ObjCIncompatible;
import java.io.Closeable;
import java.io.IOException;
import java.net.URLClassLoader;
import junit.framework.TestCase;
import org.jspecify.annotations.NullMarked;

/**
 * Abstract test class to be extended in any package in order to test that every class in that
 * package is safe to be the first class to be initialized.
 *
 * <p>While Guava has <a href="https://github.com/google/guava/issues/1977">resolved</a> the known
 * issues of this kind that were possible to trigger through publicly accessible APIs, it is
 * possible for <a href="https://github.com/google/guava/issues/8617">those issues</a> to arise in
 * other ways:
 *
 * <ul>
 *   <li>unexplained issues seen on Android: <a
 *       href="http://issuetracker.google.com/issues/40051933#comment6">Chromium issue 40051933's
 *       comment #6</a>,
 *   <li><a
 *       href="https://github.com/GoogleCloudPlatform/cloud-sql-jdbc-socket-factory/issues/2178#issuecomment-5346970152">GraalVM
 *       build-time initialization</a>
 *   <li>presumably other usages of reflection, such as a custom serialization library
 * </ul>
 *
 * <p>This test does <i>not</i> attempt to detect deadlocks that could arise with multithreading,
 * but it might manage to do so in some cases anyway.
 *
 * @since NEXT
 */
@GwtIncompatible
@J2ktIncompatible
@J2ObjCIncompatible
@NullMarked
public abstract class AbstractClassInitializationTest extends TestCase {
  public void testClassesInitialize() throws Exception {
    String packageName = getPackageName();
    ImmutableList<ClassInfo> classes = findClassesInPackage(packageName);
    assertFalse("No classes found in package " + packageName, classes.isEmpty());
    for (ClassInfo classInfo : classes) {
      if (isIgnored(classInfo)) {
        continue;
      }
      try (IsolatedClassLoader loader = createIsolatedClassLoader()) {
        Class<?> clazz = loader.loadClass(classInfo.getName());
        initialize(clazz);
      } catch (Throwable t) {
        throw new AssertionError("Failed to initialize " + classInfo, t);
      }
    }
  }

  protected String getPackageName() {
    return getClass().getPackage().getName();
  }

  protected boolean isIgnored(ClassInfo classInfo) {
    String name = classInfo.getName();
    return name.endsWith("Test")
        || name.endsWith("Tests")
        || name.endsWith("TestCase")
        || name.endsWith("TestSuite")
        || name.contains("Test$")
        || name.contains("Tests$")
        || name.contains("TestCase$")
        || name.contains("TestSuite$")
        || name.endsWith(".package-info")
        || name.equals("package-info")
        || name.endsWith(".module-info")
        || name.equals("module-info")
        // JDK8 has no VarHandle, so skip VarHandleAtomicHelper and VarHandleLittleEndianBytes there
        || (isJava8() && name.contains("VarHandle"));
  }

  @MustBeClosed
  private IsolatedClassLoader createIsolatedClassLoader() {
    ClassLoader parent = AbstractClassInitializationTest.class.getClassLoader();
    URLClassLoader urlClassLoader =
        URLClassLoader.newInstance(getClassPathUrls(), /* parent= */ null);
    return new IsolatedClassLoader(parent, urlClassLoader);
  }

  private static final class IsolatedClassLoader extends ClassLoader implements Closeable {
    final URLClassLoader delegate;

    IsolatedClassLoader(ClassLoader parent, URLClassLoader delegate) {
      super(parent);
      this.delegate = delegate;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      if (name.startsWith("com.google.common.")) {
        Class<?> c = findLoadedClass(name);
        if (c == null) {
          c = delegate.loadClass(name);
        }
        if (resolve) {
          resolveClass(c);
        }
        return c;
      }
      return super.loadClass(name, resolve);
    }

    @Override
    public void close() throws IOException {
      delegate.close();
    }
  }

  private ImmutableList<ClassInfo> findClassesInPackage(String packageName) throws IOException {
    return ClassPath.from(getClass().getClassLoader()).getAllClasses().stream()
        .filter(classInfo -> classInfo.getPackageName().equals(packageName))
        .collect(toImmutableList());
  }

  private static boolean isJava8() {
    return JAVA_SPECIFICATION_VERSION.value().equals("1.8");
  }
}
