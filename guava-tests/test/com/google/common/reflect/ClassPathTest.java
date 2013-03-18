/*
 * Copyright (C) 2012 The Guava Authors
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

package com.google.common.reflect;

import static org.truth0.Truth.ASSERT;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Closer;
import com.google.common.io.Resources;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.google.common.reflect.ClassPath.ResourceInfo;
import com.google.common.reflect.subpackage.ClassInSubPackage;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;

import junit.framework.TestCase;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Functional tests of {@link ClassPath}.
 */
public class ClassPathTest extends TestCase {

  public void testGetResources() throws Exception {
    Map<String, ResourceInfo> byName = Maps.newHashMap();
    Map<String, ResourceInfo> byToString = Maps.newHashMap();
    ClassPath classpath = ClassPath.from(getClass().getClassLoader());
    for (ResourceInfo resource : classpath.getResources()) {
      ASSERT.that(resource.getResourceName()).isNotEqualTo(JarFile.MANIFEST_NAME);
      ASSERT.that(resource.toString()).isNotEqualTo(JarFile.MANIFEST_NAME);
      byName.put(resource.getResourceName(), resource);
      byToString.put(resource.toString(), resource);
      // TODO: This will fail on maven resources in the classes directory on a mac.
      // assertNotNull(resource.url());
    }
    String testResourceName = "com/google/common/reflect/test.txt";
    ASSERT.that(byName.keySet()).has().allOf(
        "com/google/common/reflect/ClassPath.class",
        "com/google/common/reflect/ClassPathTest.class",
        "com/google/common/reflect/ClassPathTest$Nested.class",
        testResourceName);
    ASSERT.that(byToString.keySet()).has().allOf(
        "com.google.common.reflect.ClassPath",
        "com.google.common.reflect.ClassPathTest",
        "com.google.common.reflect.ClassPathTest$Nested",
        testResourceName);
    assertEquals(getClass().getClassLoader().getResource(testResourceName),
        byName.get("com/google/common/reflect/test.txt").url());
  }

  public void testGetAllClasses() throws Exception {
    Set<String> names = Sets.newHashSet();
    Set<String> strings = Sets.newHashSet();
    Set<Class<?>> classes = Sets.newHashSet();
    Set<String> packageNames = Sets.newHashSet();
    Set<String> simpleNames = Sets.newHashSet();
    ClassPath classpath = ClassPath.from(getClass().getClassLoader());
    for (ClassInfo classInfo : classpath.getAllClasses()) {
      if (!classInfo.getPackageName().equals(ClassPathTest.class.getPackage().getName())) {
        continue;
      }
      names.add(classInfo.getName());
      strings.add(classInfo.toString());
      classes.add(classInfo.load());
      packageNames.add(classInfo.getPackageName());
      simpleNames.add(classInfo.getSimpleName());
    }
    class LocalClass {}
    Class<?> anonymousClass = new Object() {}.getClass();
    ASSERT.that(names).has().allOf(anonymousClass.getName(), LocalClass.class.getName(),
        ClassPath.class.getName(), ClassPathTest.class.getName());
    ASSERT.that(strings).has().allOf(anonymousClass.getName(), LocalClass.class.getName(),
        ClassPath.class.getName(), ClassPathTest.class.getName());
    ASSERT.that(classes).has().allOf(anonymousClass, LocalClass.class, ClassPath.class,
        ClassPathTest.class);
    ASSERT.that(packageNames).has().exactly(ClassPath.class.getPackage().getName());
    ASSERT.that(simpleNames).has().allOf("", "Local", "ClassPath", "ClassPathTest");
  }

  public void testGetTopLevelClasses() throws Exception {
    Set<String> names = Sets.newHashSet();
    Set<String> strings = Sets.newHashSet();
    Set<Class<?>> classes = Sets.newHashSet();
    Set<String> packageNames = Sets.newHashSet();
    Set<String> simpleNames = Sets.newHashSet();
    ClassPath classpath = ClassPath.from(getClass().getClassLoader());
    for (ClassInfo classInfo
        : classpath.getTopLevelClasses(ClassPathTest.class.getPackage().getName())) {
      names.add(classInfo.getName());
      strings.add(classInfo.toString());
      classes.add(classInfo.load());
      packageNames.add(classInfo.getPackageName());
      simpleNames.add(classInfo.getSimpleName());
    }
    ASSERT.that(names).has().allOf(ClassPath.class.getName(), ClassPathTest.class.getName());
    ASSERT.that(strings).has().allOf(ClassPath.class.getName(), ClassPathTest.class.getName());
    ASSERT.that(classes).has().allOf(ClassPath.class, ClassPathTest.class);
    ASSERT.that(packageNames).has().item(ClassPath.class.getPackage().getName());
    ASSERT.that(simpleNames).has().allOf("ClassPath", "ClassPathTest");
    assertFalse(classes.contains(ClassInSubPackage.class));
  }

  public void testGetTopLevelClassesRecursive() throws Exception {
    Set<Class<?>> classes = Sets.newHashSet();
    ClassPath classpath = ClassPath.from(ClassPathTest.class.getClassLoader());
    for (ClassInfo classInfo
        : classpath.getTopLevelClassesRecursive(ClassPathTest.class.getPackage().getName())) {
      if (classInfo.getName().contains("ClassPathTest")) {
        System.err.println("");
      }
      classes.add(classInfo.load());
    }
    ASSERT.that(classes).has().allOf(ClassPathTest.class, ClassInSubPackage.class);
  }

  public void testGetTopLevelClasses_diamond() throws Exception {
    ClassLoader parent = ClassPathTest.class.getClassLoader();
    ClassLoader sub1 = new ClassLoader(parent) {};
    ClassLoader sub2 = new ClassLoader(parent) {};
    assertEquals(findClass(ClassPath.from(sub1).getTopLevelClasses(), ClassPathTest.class),
        findClass(ClassPath.from(sub2).getTopLevelClasses(), ClassPathTest.class));
  }

  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(classInfo(ClassPathTest.class), classInfo(ClassPathTest.class))
        .addEqualityGroup(classInfo(Test.class), classInfo(Test.class, getClass().getClassLoader()))
        .addEqualityGroup(
            new ResourceInfo("a/b/c.txt", getClass().getClassLoader()),
            new ResourceInfo("a/b/c.txt", getClass().getClassLoader()))
        .addEqualityGroup(
            new ResourceInfo("x.txt", getClass().getClassLoader()))
        .testEquals();
  }

  public void testClassPathEntries_emptyURLClassLoader_noParent() {
    ASSERT.that(ClassPath.getClassPathEntries(new URLClassLoader(new URL[0], null)).keySet())
        .isEmpty();
  }

  public void testClassPathEntries_URLClassLoader_noParent() throws Exception {
    URL url1 = new URL("file:/a");
    URL url2 = new URL("file:/b");
    URLClassLoader classloader = new URLClassLoader(new URL[] {url1, url2}, null);
    assertEquals(
        ImmutableMap.of(url1.toURI(), classloader, url2.toURI(), classloader),
        ClassPath.getClassPathEntries(classloader));
  }

  public void testClassPathEntries_URLClassLoader_withParent() throws Exception {
    URL url1 = new URL("file:/a");
    URL url2 = new URL("file:/b");
    URLClassLoader parent = new URLClassLoader(new URL[] {url1}, null);
    URLClassLoader child = new URLClassLoader(new URL[] {url2}, parent) {};
    ImmutableMap<URI, ClassLoader> classPathEntries = ClassPath.getClassPathEntries(child);
    assertEquals(ImmutableMap.of(url1.toURI(), parent, url2.toURI(), child),  classPathEntries);
    ASSERT.that(classPathEntries.keySet()).has().exactly(url1.toURI(), url2.toURI()).inOrder();
  }

  public void testClassPathEntries_duplicateUri_parentWins() throws Exception {
    URL url = new URL("file:/a");
    URLClassLoader parent = new URLClassLoader(new URL[] {url}, null);
    URLClassLoader child = new URLClassLoader(new URL[] {url}, parent) {};
    assertEquals(ImmutableMap.of(url.toURI(), parent), ClassPath.getClassPathEntries(child));
  }

  public void testClassPathEntries_notURLClassLoader_noParent() {
    ASSERT.that(ClassPath.getClassPathEntries(new ClassLoader(null) {}).keySet()).isEmpty();
  }

  public void testClassPathEntries_notURLClassLoader_withParent() throws Exception {
    URL url = new URL("file:/a");
    URLClassLoader parent = new URLClassLoader(new URL[] {url}, null);
    assertEquals(
        ImmutableMap.of(url.toURI(), parent),
        ClassPath.getClassPathEntries(new ClassLoader(parent) {}));
  }

  public void testClassPathEntries_notURLClassLoader_withParentAndGrandParent() throws Exception {
    URL url1 = new URL("file:/a");
    URL url2 = new URL("file:/b");
    URLClassLoader grandParent = new URLClassLoader(new URL[] {url1}, null);
    URLClassLoader parent = new URLClassLoader(new URL[] {url2}, grandParent);
    assertEquals(
        ImmutableMap.of(url1.toURI(), grandParent, url2.toURI(), parent),
        ClassPath.getClassPathEntries(new ClassLoader(parent) {}));
  }

  public void testClassPathEntries_notURLClassLoader_withGrandParent() throws Exception {
    URL url = new URL("file:/a");
    URLClassLoader grandParent = new URLClassLoader(new URL[] {url}, null);
    ClassLoader parent = new ClassLoader(grandParent) {};
    assertEquals(
        ImmutableMap.of(url.toURI(), grandParent),
        ClassPath.getClassPathEntries(new ClassLoader(parent) {}));
  }

  public void testScan_classPathCycle() throws IOException {
    File jarFile = File.createTempFile("with_circular_class_path", ".jar");
    try {
      writeSelfReferencingJarFile(jarFile, "test.txt");
      ClassPath.Scanner scanner = new ClassPath.Scanner();
      scanner.scan(jarFile.toURI(), ClassPathTest.class.getClassLoader());
      assertEquals(1, scanner.getResources().size());
    } finally {
      jarFile.delete();
    }
  }

  public void testScanFromFile_fileNotExists() throws IOException {
    ClassLoader classLoader = ClassPathTest.class.getClassLoader();
    ClassPath.Scanner scanner = new ClassPath.Scanner();
    scanner.scanFrom(new File("no/such/file/anywhere"), classLoader);
    ASSERT.that(scanner.getResources()).isEmpty();
  }

  public void testScanFromFile_notJarFile() throws IOException {
    ClassLoader classLoader = ClassPathTest.class.getClassLoader();
    File notJar = File.createTempFile("not_a_jar", "txt");
    ClassPath.Scanner scanner = new ClassPath.Scanner();
    try {
      scanner.scanFrom(notJar, classLoader);
    } finally {
      notJar.delete();
    }
    ASSERT.that(scanner.getResources()).isEmpty();
  }

  public void testGetClassPathEntry() throws URISyntaxException {
    assertEquals(URI.create("file:/usr/test/dep.jar"),
        ClassPath.Scanner.getClassPathEntry(
            new File("/home/build/outer.jar"), "file:/usr/test/dep.jar"));
    assertEquals(URI.create("file:/home/build/a.jar"),
        ClassPath.Scanner.getClassPathEntry(new File("/home/build/outer.jar"), "a.jar"));
    assertEquals(URI.create("file:/home/build/x/y/z"),
        ClassPath.Scanner.getClassPathEntry(new File("/home/build/outer.jar"), "x/y/z"));
    assertEquals(URI.create("file:/home/build/x/y/z.jar"),
        ClassPath.Scanner.getClassPathEntry(new File("/home/build/outer.jar"), "x/y/z.jar"));
  }

  public void testGetClassPathFromManifest_nullManifest() {
    ASSERT.that(ClassPath.Scanner.getClassPathFromManifest(new File("some.jar"), null)).isEmpty();
  }

  public void testGetClassPathFromManifest_noClassPath() throws IOException {
    File jarFile = new File("base.jar");
    ASSERT.that(ClassPath.Scanner.getClassPathFromManifest(jarFile, manifest("")))
        .isEmpty();
  }

  public void testGetClassPathFromManifest_emptyClassPath() throws IOException {
    File jarFile = new File("base.jar");
    ASSERT.that(ClassPath.Scanner.getClassPathFromManifest(jarFile, manifestClasspath("")))
        .isEmpty();
  }

  public void testGetClassPathFromManifest_badClassPath() throws IOException {
    File jarFile = new File("base.jar");
    Manifest manifest = manifestClasspath("an_invalid^path");
    ASSERT.that(ClassPath.Scanner.getClassPathFromManifest(jarFile, manifest))
        .isEmpty();
  }

  public void testGetClassPathFromManifest_relativeDirectory() throws IOException {
    File jarFile = new File("base/some.jar");
    // with/relative/directory is the Class-Path value in the mf file.
    Manifest manifest = manifestClasspath("with/relative/dir");
    ASSERT.that(ClassPath.Scanner.getClassPathFromManifest(jarFile, manifest))
        .has().exactly(new File("base/with/relative/dir").toURI()).inOrder();
  }

  public void testGetClassPathFromManifest_relativeJar() throws IOException {
    File jarFile = new File("base/some.jar");
    // with/relative/directory is the Class-Path value in the mf file.
    Manifest manifest = manifestClasspath("with/relative.jar");
    ASSERT.that(ClassPath.Scanner.getClassPathFromManifest(jarFile, manifest))
        .has().exactly(new File("base/with/relative.jar").toURI()).inOrder();
  }

  public void testGetClassPathFromManifest_jarInCurrentDirectory() throws IOException {
    File jarFile = new File("base/some.jar");
    // with/relative/directory is the Class-Path value in the mf file.
    Manifest manifest = manifestClasspath("current.jar");
    ASSERT.that(ClassPath.Scanner.getClassPathFromManifest(jarFile, manifest))
        .has().exactly(new File("base/current.jar").toURI()).inOrder();
  }

  public void testGetClassPathFromManifest_absoluteDirectory() throws IOException {
    File jarFile = new File("base/some.jar");
    Manifest manifest = manifestClasspath("file:/with/absolute/dir");
    ASSERT.that(ClassPath.Scanner.getClassPathFromManifest(jarFile, manifest))
        .has().exactly(new File("/with/absolute/dir").toURI()).inOrder();
  }

  public void testGetClassPathFromManifest_absoluteJar() throws IOException {
    File jarFile = new File("base/some.jar");
    Manifest manifest = manifestClasspath("file:/with/absolute.jar");
    ASSERT.that(ClassPath.Scanner.getClassPathFromManifest(jarFile, manifest))
        .has().exactly(new File("/with/absolute.jar").toURI()).inOrder();
  }

  public void testGetClassPathFromManifest_multiplePaths() throws IOException {
    File jarFile = new File("base/some.jar");
    Manifest manifest = manifestClasspath("file:/with/absolute.jar relative.jar  relative/dir");
    ASSERT.that(ClassPath.Scanner.getClassPathFromManifest(jarFile, manifest))
        .has().exactly(
            new File("/with/absolute.jar").toURI(),
            new File("base/relative.jar").toURI(),
            new File("base/relative/dir").toURI())
        .inOrder();
  }

  public void testGetClassPathFromManifest_leadingBlanks() throws IOException {
    File jarFile = new File("base/some.jar");
    Manifest manifest = manifestClasspath(" relative.jar");
    ASSERT.that(ClassPath.Scanner.getClassPathFromManifest(jarFile, manifest))
        .has().exactly(new File("base/relative.jar").toURI()).inOrder();
  }

  public void testGetClassPathFromManifest_trailingBlanks() throws IOException {
    File jarFile = new File("base/some.jar");
    Manifest manifest = manifestClasspath("relative.jar ");
    ASSERT.that(ClassPath.Scanner.getClassPathFromManifest(jarFile, manifest))
        .has().exactly(new File("base/relative.jar").toURI()).inOrder();
  }

  public void testGetClassName() {
    assertEquals("abc.d.Abc", ClassPath.getClassName("abc/d/Abc.class"));
  }

  public void testResourceInfo_of() {
    assertEquals(ClassInfo.class, resourceInfo(ClassPathTest.class).getClass());
    assertEquals(ClassInfo.class, resourceInfo(ClassPath.class).getClass());
    assertEquals(ClassInfo.class, resourceInfo(Nested.class).getClass());
  }

  public void testGetSimpleName() {
    assertEquals("Foo",
        new ClassInfo("Foo.class", getClass().getClassLoader()).getSimpleName());
    assertEquals("Foo",
        new ClassInfo("a/b/Foo.class", getClass().getClassLoader()).getSimpleName());
    assertEquals("Foo",
        new ClassInfo("a/b/Bar$Foo.class", getClass().getClassLoader()).getSimpleName());
    assertEquals("",
        new ClassInfo("a/b/Bar$1.class", getClass().getClassLoader()).getSimpleName());
    assertEquals("Foo",
        new ClassInfo("a/b/Bar$Foo.class", getClass().getClassLoader()).getSimpleName());
    assertEquals("",
        new ClassInfo("a/b/Bar$1.class", getClass().getClassLoader()).getSimpleName());
    assertEquals("Local",
        new ClassInfo("a/b/Bar$1Local.class", getClass().getClassLoader()).getSimpleName());

  }

  public void testGetPackageName() {
    assertEquals("",
        new ClassInfo("Foo.class", getClass().getClassLoader()).getPackageName());
    assertEquals("a.b",
        new ClassInfo("a/b/Foo.class", getClass().getClassLoader()).getPackageName());
  }

  private static class Nested {}

  public void testNulls() throws IOException {
    new NullPointerTester().testAllPublicStaticMethods(ClassPath.class);
    new NullPointerTester()
        .testAllPublicInstanceMethods(ClassPath.from(getClass().getClassLoader()));
  }

  private static ClassPath.ClassInfo findClass(
      Iterable<ClassPath.ClassInfo> classes, Class<?> cls) {
    for (ClassPath.ClassInfo classInfo : classes) {
      if (classInfo.getName().equals(cls.getName())) {
        return classInfo;
      }
    }
    throw new AssertionError("failed to find " + cls);
  }

  private static ResourceInfo resourceInfo(Class<?> cls) {
    return ResourceInfo.of(cls.getName().replace('.', '/') + ".class", cls.getClassLoader());
  }

  private static ClassInfo classInfo(Class<?> cls) {
    return classInfo(cls, cls.getClassLoader());
  }

  private static ClassInfo classInfo(Class<?> cls, ClassLoader classLoader) {
    return new ClassInfo(cls.getName().replace('.', '/') + ".class", classLoader);
  }

  private static Manifest manifestClasspath(String classpath) throws IOException {
    return manifest("Class-Path: " + classpath + "\n");
  }

  private static void writeSelfReferencingJarFile(File jarFile, String... entries)
      throws IOException {
    Manifest manifest = new Manifest();
    // Without version, the manifest is silently ignored. Ugh!
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, jarFile.getName());

    Closer closer = Closer.create();
    try {
      FileOutputStream fileOut = closer.register(new FileOutputStream(jarFile));
      JarOutputStream jarOut = closer.register(new JarOutputStream(fileOut));
      for (String entry : entries) {
        jarOut.putNextEntry(new ZipEntry(entry));
        Resources.copy(ClassPathTest.class.getResource(entry), jarOut);
        jarOut.closeEntry();
      }
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  private static Manifest manifest(String content) throws IOException {
    InputStream in = new ByteArrayInputStream(content.getBytes(Charsets.US_ASCII.name()));
    Manifest manifest = new Manifest();
    manifest.read(in);
    return manifest;
  }
}
