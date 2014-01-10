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
import com.google.common.base.CharMatcher;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;

import javax.annotation.Nullable;

/**
 * Scans the source of a {@link ClassLoader} and finds all loadable classes and resources.
 *
 * @author Ben Yu
 * @since 14.0
 */
@Beta
public final class ClassPath {
  private static final Logger logger = Logger.getLogger(ClassPath.class.getName());

  private static final Predicate<ClassInfo> IS_TOP_LEVEL = new Predicate<ClassInfo>() {
    @Override public boolean apply(ClassInfo info) {
      return info.className.indexOf('$') == -1;
    }
  };

  /** Separator for the Class-Path manifest attribute value in jar files. */
  private static final Splitter CLASS_PATH_ATTRIBUTE_SEPARATOR =
      Splitter.on(" ").omitEmptyStrings();

  private static final String CLASS_FILE_NAME_EXTENSION = ".class";

  private final ImmutableSet<ResourceInfo> resources;

  private ClassPath(ImmutableSet<ResourceInfo> resources) {
    this.resources = resources;
  }

  /**
   * Returns a {@code ClassPath} representing all classes and resources loadable from {@code
   * classloader} and its parent class loaders.
   *
   * <p>Currently only {@link URLClassLoader} and only {@code file://} urls are supported.
   *
   * @throws IOException if the attempt to read class path resources (jar files or directories)
   *         failed.
   */
  public static ClassPath from(ClassLoader classloader) throws IOException {
    Scanner scanner = new Scanner();
    for (Map.Entry<URI, ClassLoader> entry : getClassPathEntries(classloader).entrySet()) {
      scanner.scan(entry.getKey(), entry.getValue());
    }
    return new ClassPath(scanner.getResources());
  }

  /**
   * Returns all resources loadable from the current class path, including the class files of all
   * loadable classes but excluding the "META-INF/MANIFEST.MF" file.
   */
  public ImmutableSet<ResourceInfo> getResources() {
    return resources;
  }

  /**
   * Returns all classes loadable from the current class path.
   *
   * @since 16.0
   */
  public ImmutableSet<ClassInfo> getAllClasses() {
    return FluentIterable.from(resources).filter(ClassInfo.class).toSet();
  }

  /** Returns all top level classes loadable from the current class path. */
  public ImmutableSet<ClassInfo> getTopLevelClasses() {
    return FluentIterable.from(resources).filter(ClassInfo.class).filter(IS_TOP_LEVEL).toSet();
  }

  /** Returns all top level classes whose package name is {@code packageName}. */
  public ImmutableSet<ClassInfo> getTopLevelClasses(String packageName) {
    checkNotNull(packageName);
    ImmutableSet.Builder<ClassInfo> builder = ImmutableSet.builder();
    for (ClassInfo classInfo : getTopLevelClasses()) {
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
    for (ClassInfo classInfo : getTopLevelClasses()) {
      if (classInfo.getName().startsWith(packagePrefix)) {
        builder.add(classInfo);
      }
    }
    return builder.build();
  }

  /**
   * Represents a class path resource that can be either a class file or any other resource file
   * loadable from the class path.
   *
   * @since 14.0
   */
  @Beta
  public static class ResourceInfo {
    private final String resourceName;
    final ClassLoader loader;

    static ResourceInfo of(String resourceName, ClassLoader loader) {
      if (resourceName.endsWith(CLASS_FILE_NAME_EXTENSION)) {
        return new ClassInfo(resourceName, loader);
      } else {
        return new ResourceInfo(resourceName, loader);
      }
    }
  
    ResourceInfo(String resourceName, ClassLoader loader) {
      this.resourceName = checkNotNull(resourceName);
      this.loader = checkNotNull(loader);
    }

    /** Returns the url identifying the resource. */
    public final URL url() {
      return checkNotNull(loader.getResource(resourceName),
          "Failed to load resource: %s", resourceName);
    }

    /** Returns the fully qualified name of the resource. Such as "com/mycomp/foo/bar.txt". */
    public final String getResourceName() {
      return resourceName;
    }

    @Override public int hashCode() {
      return resourceName.hashCode();
    }

    @Override public boolean equals(Object obj) {
      if (obj instanceof ResourceInfo) {
        ResourceInfo that = (ResourceInfo) obj;
        return resourceName.equals(that.resourceName)
            && loader == that.loader;
      }
      return false;
    }

    // Do not change this arbitrarily. We rely on it for sorting ResourceInfo.
    @Override public String toString() {
      return resourceName;
    }
  }

  /**
   * Represents a class that can be loaded through {@link #load}.
   *
   * @since 14.0
   */
  @Beta
  public static final class ClassInfo extends ResourceInfo {
    private final String className;

    ClassInfo(String resourceName, ClassLoader loader) {
      super(resourceName, loader);
      this.className = getClassName(resourceName);
    }

    /** 
     * Returns the package name of the class, without attempting to load the class.
     * 
     * <p>Behaves identically to {@link Package#getName()} but does not require the class (or 
     * package) to be loaded.
     */
    public String getPackageName() {
      return Reflection.getPackageName(className);
    }

    /** 
     * Returns the simple name of the underlying class as given in the source code.
     * 
     * <p>Behaves identically to {@link Class#getSimpleName()} but does not require the class to be
     * loaded.
     */
    public String getSimpleName() {
      int lastDollarSign = className.lastIndexOf('$');
      if (lastDollarSign != -1) {
        String innerClassName = className.substring(lastDollarSign + 1);
        // local and anonymous classes are prefixed with number (1,2,3...), anonymous classes are 
        // entirely numeric whereas local classes have the user supplied name as a suffix
        return CharMatcher.DIGIT.trimLeadingFrom(innerClassName);
      }
      String packageName = getPackageName();
      if (packageName.length() == 0) {
        return className;
      }

      // Since this is a top level class, its simple name is always the part after package name.
      return className.substring(packageName.length() + 1);
    }

    /** 
     * Returns the fully qualified name of the class. 
     * 
     * <p>Behaves identically to {@link Class#getName()} but does not require the class to be
     * loaded.
     */
    public String getName() {
      return className;
    }

    /**
     * Loads (but doesn't link or initialize) the class.
     *
     * @throws LinkageError when there were errors in loading classes that this class depends on.
     *         For example, {@link NoClassDefFoundError}.
     */
    public Class<?> load() {
      try {
        return loader.loadClass(className);
      } catch (ClassNotFoundException e) {
        // Shouldn't happen, since the class name is read from the class path.
        throw new IllegalStateException(e);
      }
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

  @VisibleForTesting static final class Scanner {

    private final ImmutableSortedSet.Builder<ResourceInfo> resources =
        new ImmutableSortedSet.Builder<ResourceInfo>(Ordering.usingToString());
    private final Set<URI> scannedUris = Sets.newHashSet();

    ImmutableSortedSet<ResourceInfo> getResources() {
      return resources.build();
    }

    void scan(URI uri, ClassLoader classloader) throws IOException {
      if (uri.getScheme().equals("file") && scannedUris.add(uri)) {
        scanFrom(new File(uri), classloader);
      }
    }
  
    @VisibleForTesting void scanFrom(File file, ClassLoader classloader)
        throws IOException {
      if (!file.exists()) {
        return;
      }
      if (file.isDirectory()) {
        scanDirectory(file, classloader);
      } else {
        scanJar(file, classloader);
      }
    }
  
    private void scanDirectory(File directory, ClassLoader classloader) throws IOException {
      scanDirectory(directory, classloader, "", ImmutableSet.<File>of());
    }
  
    private void scanDirectory(
        File directory, ClassLoader classloader, String packagePrefix,
        ImmutableSet<File> ancestors) throws IOException {
      File canonical = directory.getCanonicalFile();
      if (ancestors.contains(canonical)) {
        // A cycle in the filesystem, for example due to a symbolic link.
        return;
      }
      File[] files = directory.listFiles();
      if (files == null) {
        logger.warning("Cannot read directory " + directory);
        // IO error, just skip the directory
        return;
      }
      ImmutableSet<File> newAncestors = ImmutableSet.<File>builder()
          .addAll(ancestors)
          .add(canonical)
          .build();
      for (File f : files) {
        String name = f.getName();
        if (f.isDirectory()) {
          scanDirectory(f, classloader, packagePrefix + name + "/", newAncestors);
        } else {
          String resourceName = packagePrefix + name;
          if (!resourceName.equals(JarFile.MANIFEST_NAME)) {
            resources.add(ResourceInfo.of(resourceName, classloader));
          }
        }
      }
    }
  
    private void scanJar(File file, ClassLoader classloader) throws IOException {
      JarFile jarFile;
      try {
        jarFile = new JarFile(file);
      } catch (IOException e) {
        // Not a jar file
        return;
      }
      try {
        for (URI uri : getClassPathFromManifest(file, jarFile.getManifest())) {
          scan(uri, classloader);
        }
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
          JarEntry entry = entries.nextElement();
          if (entry.isDirectory() || entry.getName().equals(JarFile.MANIFEST_NAME)) {
            continue;
          }
          resources.add(ResourceInfo.of(entry.getName(), classloader));
        }
      } finally {
        try {
          jarFile.close();
        } catch (IOException ignored) {}
      }
    }
  
    /**
     * Returns the class path URIs specified by the {@code Class-Path} manifest attribute, according
     * to <a href="http://docs.oracle.com/javase/6/docs/technotes/guides/jar/jar.html#Main%20Attributes">
     * JAR File Specification</a>. If {@code manifest} is null, it means the jar file has no
     * manifest, and an empty set will be returned.
     */
    @VisibleForTesting static ImmutableSet<URI> getClassPathFromManifest(
        File jarFile, @Nullable Manifest manifest) {
      if (manifest == null) {
        return ImmutableSet.of();
      }
      ImmutableSet.Builder<URI> builder = ImmutableSet.builder();
      String classpathAttribute = manifest.getMainAttributes()
          .getValue(Attributes.Name.CLASS_PATH.toString());
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
     * <a href="http://docs.oracle.com/javase/6/docs/technotes/guides/jar/jar.html#Main%20Attributes">
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
  }

  @VisibleForTesting static String getClassName(String filename) {
    int classNameEnd = filename.length() - CLASS_FILE_NAME_EXTENSION.length();
    return filename.substring(0, classNameEnd).replace('/', '.');
  }
}
