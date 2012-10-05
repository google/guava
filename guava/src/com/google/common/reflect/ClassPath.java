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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;

import javax.annotation.Nullable;

/**
 * Scans the source of a {@link ClassLoader} and finds all the classes loadable.
 *
 * @author Ben Yu
 * @since 14.0
 */
@Beta
public final class ClassPath {

  private static final Logger logger = Logger.getLogger(ClassPath.class.getName());

  /** Separator for the Class-Path manifest attribute value in jar files. */
  private static final Splitter CLASS_PATH_ATTRIBUTE_SEPARATOR =
      Splitter.on(" ").omitEmptyStrings();

  private static final String CLASS_FILE_NAME_EXTENSION = ".class";

  private final ImmutableSet<ClassInfo> classes;

  private ClassPath(ImmutableSet<ClassInfo> classes) {
    this.classes = classes;
  }

  /**
   * Returns a {@code ClassPath} representing all classes loadable from {@code classloader} and its
   * parent class loaders.
   *
   * <p>Currently only {@link URLClassLoader} and only {@code file://} urls are supported.
   *
   * @throws IOException if the attempt to read class path resources (jar files or directories)
   *         failed.
   */
  public static ClassPath from(ClassLoader classloader) throws IOException {
    ImmutableSortedSet.Builder<ClassInfo> builder = new ImmutableSortedSet.Builder<ClassInfo>(
        Ordering.usingToString());
    for (Map.Entry<URI, ClassLoader> entry : getClassPathEntries(classloader).entrySet()) {
      builder.addAll(readClassesFrom(entry.getKey(), entry.getValue()));
    }
    return new ClassPath(builder.build());
  }

  /** Returns all top level classes loadable from the current class path. */
  public ImmutableSet<ClassInfo> getTopLevelClasses() {
    return classes;
  }

  /** Returns all top level classes whose package name is {@code packageName}. */
  public ImmutableSet<ClassInfo> getTopLevelClasses(String packageName) {
    checkNotNull(packageName);
    ImmutableSet.Builder<ClassInfo> builder = ImmutableSet.builder();
    for (ClassInfo classInfo : classes) {
      if (classInfo.getPackageName().equals(packageName)) {
        builder.add(classInfo);
      }
    }
    return builder.build();
  }

  /**
   * Returns all top level classes whose package name is {@code packageName} or starts with
   * {@code packageName} followed by a '.'.
   */
  public ImmutableSet<ClassInfo> getTopLevelClassesRecursive(String packageName) {
    checkNotNull(packageName);
    String packagePrefix = packageName + '.';
    ImmutableSet.Builder<ClassInfo> builder = ImmutableSet.builder();
    for (ClassInfo classInfo : classes) {
      if (classInfo.getName().startsWith(packagePrefix)) {
        builder.add(classInfo);
      }
    }
    return builder.build();
  }

  /** Represents a class that can be loaded through {@link #load}. */
  public static final class ClassInfo {
    private final String className;
    private final ClassLoader loader;

    @VisibleForTesting ClassInfo(String className, ClassLoader loader) {
      this.className = checkNotNull(className);
      this.loader = checkNotNull(loader);
    }

    /** Returns the package name of the class, without attempting to load the class. */
    public String getPackageName() {
      return Reflection.getPackageName(className);
    }

    /** Returns the simple name of the underlying class as given in the source code. */
    public String getSimpleName() {
      String packageName = getPackageName();
      if (packageName.isEmpty()) {
        return className;
      }
      // Since this is a top level class, its simple name is always the part after package name.
      return className.substring(packageName.length() + 1);
    }

    /** Returns the fully qualified name of the class. */
    public String getName() {
      return className;
    }

    /** Loads (but doesn't link or initialize) the class. */
    public Class<?> load() {
      try {
        return loader.loadClass(className);
      } catch (ClassNotFoundException e) {
        // Shouldn't happen, since the class name is read from the class path.
        throw new IllegalStateException(e);
      }
    }

    @Override public int hashCode() {
      return className.hashCode();
    }

    @Override public boolean equals(Object obj) {
      if (obj instanceof ClassInfo) {
        ClassInfo that = (ClassInfo) obj;
        return className.equals(that.className)
            && loader == that.loader;
      }
      return false;
    }

    @Override public String toString() {
      return className;
    }
  }

  @VisibleForTesting static ImmutableMap<URI, ClassLoader> getClassPathEntries(
      ClassLoader classloader) {
    LinkedHashMap<URI, ClassLoader> entries = Maps.newLinkedHashMap();
    // Search parent first, since it's the order ClassLoader#loadClass() uses.
    ClassLoader parent = classloader.getParent();
    if (parent != null) {
      entries.putAll(getClassPathEntries(parent));
    }
    if (classloader instanceof URLClassLoader) {
      URLClassLoader urlClassLoader = (URLClassLoader) classloader;
      for (URL entry : urlClassLoader.getURLs()) {
        URI uri;
        try {
          uri = entry.toURI();
        } catch (URISyntaxException e) {
          throw new IllegalArgumentException(e);
        }
        if (!entries.containsKey(uri)) {
          entries.put(uri, classloader);
        }
      }
    }
    return ImmutableMap.copyOf(entries);
  }

  private static ImmutableSet<ClassInfo> readClassesFrom(URI uri, ClassLoader classloader)
      throws IOException {
    if (uri.getScheme().equals("file")) {
      return readClassesFrom(new File(uri), classloader);
    } else {
      return ImmutableSet.of();
    }
  }

  @VisibleForTesting static ImmutableSet<ClassInfo> readClassesFrom(
      File file, ClassLoader classloader)
      throws IOException {
    if (!file.exists()) {
      return ImmutableSet.of();
    }
    if (file.isDirectory()) {
      return readClassesFromDirectory(file, classloader);
    } else {
      return readClassesFromJar(file, classloader);
    }
  }

  private static ImmutableSet<ClassInfo> readClassesFromDirectory(
      File directory, ClassLoader classloader) {
    ImmutableSet.Builder<ClassInfo> builder = ImmutableSet.builder();
    readClassesFromDirectory(directory, classloader, "", builder);
    return builder.build();
  }

  private static void readClassesFromDirectory(
      File directory, ClassLoader classloader,
      String packagePrefix, ImmutableSet.Builder<ClassInfo> builder) {
    for (File f : directory.listFiles()) {
      String name = f.getName();
      if (f.isDirectory()) {
        readClassesFromDirectory(f, classloader, packagePrefix + name + ".", builder);
      } else if (isTopLevelClassFile(name)) {
        String className = packagePrefix + getClassName(name);
        builder.add(new ClassInfo(className, classloader));
      }
    }
  }

  private static ImmutableSet<ClassInfo> readClassesFromJar(File file, ClassLoader classloader)
      throws IOException {
    ImmutableSet.Builder<ClassInfo> builder = ImmutableSet.builder();
    JarFile jarFile = new JarFile(file);
    for (URI uri : getClassPathFromManifest(file, jarFile.getManifest())) {
      builder.addAll(readClassesFrom(uri, classloader));
    }
    Enumeration<JarEntry> entries = jarFile.entries();
    while (entries.hasMoreElements()) {
      JarEntry entry = entries.nextElement();
      if (isTopLevelClassFile(entry.getName())) {
        String className = getClassName(entry.getName().replace('/', '.'));
        builder.add(new ClassInfo(className, classloader));
      }
    }
    return builder.build();
  }

  /**
   * Returns the class path URIs specified by the {@code Class-Path} manifest attribute, according
   * to <a href="http://docs.oracle.com/javase/1.4.2/docs/guide/jar/jar.html#Main%20Attributes">
   * JAR File Specification</a>. If {@code manifest} is null, it means the jar file has no manifest,
   * and an empty set will be returned.
   */
  @VisibleForTesting static ImmutableSet<URI> getClassPathFromManifest(
      File jarFile, @Nullable Manifest manifest) {
    if (manifest == null) {
      return ImmutableSet.of();
    }
    ImmutableSet.Builder<URI> builder = ImmutableSet.builder();
    String classpathAttribute = manifest.getMainAttributes().getValue("Class-Path");
    if (classpathAttribute != null) {
      for (String path : CLASS_PATH_ATTRIBUTE_SEPARATOR.split(classpathAttribute)) {
        URI uri;
        try {
          uri = getClassPathEntry(jarFile, path);
        } catch (URISyntaxException e) {
          // Ignore bad entry
          logger.warning("Invalid Class-Path entry: " + path);
          continue;
        }
        builder.add(uri);
      }
    }
    return builder.build();
  }

  /**
   * Returns the absolute uri of the Class-Path entry value as specified in
   * <a href="http://docs.oracle.com/javase/1.4.2/docs/guide/jar/jar.html#Main%20Attributes">
   * JAR File Specification</a>. Even though the specification only talks about relative urls,
   * absolute urls are actually supported too (for example, in Maven surefire plugin).
   */
  @VisibleForTesting static URI getClassPathEntry(File jarFile, String path)
      throws URISyntaxException {
    URI uri = new URI(path);
    if (uri.isAbsolute()) {
      return uri;
    } else {
      return new File(jarFile.getParentFile(), path.replace('/', File.separatorChar)).toURI();
    }
  }

  @VisibleForTesting static boolean isTopLevelClassFile(String filename) {
    return filename.endsWith(CLASS_FILE_NAME_EXTENSION) && filename.indexOf('$') < 0;
  }

  @VisibleForTesting static String getClassName(String filename) {
    int classNameEnd = filename.length() - CLASS_FILE_NAME_EXTENSION.length();
    return filename.substring(0, classNameEnd);
  }
}
