/*
 * Copyright (C) 2012 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.reflect;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.StandardSystemProperty.JAVA_CLASS_PATH;
import static com.google.common.base.StandardSystemProperty.PATH_SEPARATOR;
import static java.util.logging.Level.WARNING;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;

/**
 * Scans the source of a {@link ClassLoader} and finds all loadable classes and resources.
 *
 * <h2>Prefer <a href="https://github.com/classgraph/classgraph/wiki">ClassGraph</a> over {@code
 * ClassPath}</h2>
 *
 * <p>We recommend using <a href="https://github.com/classgraph/classgraph/wiki">ClassGraph</a>
 * instead of {@code ClassPath}. ClassGraph improves upon {@code ClassPath} in several ways,
 * including addressing many of its limitations. Limitations of {@code ClassPath} include:
 *
 * <ul>
 *   <li>It looks only for files and JARs in URLs available from {@link URLClassLoader} instances or
 *       the {@linkplain ClassLoader#getSystemClassLoader() system class loader}. This means it does
 *       not look for classes in the <i>module path</i>.
 *   <li>It understands only {@code file:} URLs. This means that it does not understand <a
 *       href="https://openjdk.java.net/jeps/220">{@code jrt:/} URLs</a>, among <a
 *       href="https://github.com/classgraph/classgraph/wiki/Classpath-specification-mechanisms">others</a>.
 *   <li>It does not know how to look for classes when running under an Android VM. (ClassGraph does
 *       not support this directly, either, but ClassGraph documents how to <a
 *       href="https://github.com/classgraph/classgraph/wiki/Build-Time-Scanning">perform build-time
 *       classpath scanning and make the results available to an Android app</a>.)
 *   <li>Like all of Guava, it is not tested under Windows. We have gotten <a
 *       href="https://github.com/google/guava/issues/2130">a report of a specific bug under
 *       Windows</a>.
 *   <li>It <a href="https://github.com/google/guava/issues/2712">returns only one resource for a
 *       given path</a>, even if resources with that path appear in multiple jars or directories.
 *   <li>It assumes that <a href="https://github.com/google/guava/issues/3349">any class with a
 *       {@code $} in its name is a nested class</a>.
 * </ul>
 *
 * <h2>{@code ClassPath} and symlinks</h2>
 *
 * <p>In the case of directory classloaders, symlinks are supported but cycles are not traversed.
 * This guarantees discovery of each <em>unique</em> loadable resource. However, not all possible
 * aliases for resources on cyclic paths will be listed.
 *
 * @author Ben Yu
 * @since 14.0
 */
@ElementTypesAreNonnullByDefault
public final class ClassPath {
  private static final Logger logger = Logger.getLogger(ClassPath.class.getName());

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
   * classloader} and its ancestor class loaders.
   *
   * <p><b>Warning:</b> {@code ClassPath} can find classes and resources only from:
   *
   * <ul>
   *   <li>{@link URLClassLoader} instances' {@code file:} URLs
   *   <li>the {@linkplain ClassLoader#getSystemClassLoader() system class loader}. To search the
   *       system class loader even when it is not a {@link URLClassLoader} (as in Java 9), {@code
   *       ClassPath} searches the files from the {@code java.class.path} system property.
   * </ul>
   *
   * @throws IOException if the attempt to read class path resources (jar files or directories)
   *     failed.
   */
  public static ClassPath from(ClassLoader classloader) throws IOException {
    ImmutableSet<LocationInfo> locations = locationsFrom(classloader);

    // Add all locations to the scanned set so that in a classpath [jar1, jar2], where jar1 has a
    // manifest with Class-Path pointing to jar2, we won't scan jar2 twice.
    Set<File> scanned = new HashSet<>();
    for (LocationInfo location : locations) {
      scanned.add(location.file());
    }

    // Scan all locations
    ImmutableSet.Builder<ResourceInfo> builder = ImmutableSet.builder();
    for (LocationInfo location : locations) {
      builder.addAll(location.scanResources(scanned));
    }
    return new ClassPath(builder.build());
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

  /**
   * Returns all top level classes loadable from the current class path. Note that "top-level-ness"
   * is determined heuristically by class name (see {@link ClassInfo#isTopLevel}).
   */
  public ImmutableSet<ClassInfo> getTopLevelClasses() {
    return FluentIterable.from(resources)
        .filter(ClassInfo.class)
        .filter(ClassInfo::isTopLevel)
        .toSet();
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
   * Returns all top level classes whose package name is {@code packageName} or starts with {@code
   * packageName} followed by a '.'.
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
  public static class ResourceInfo {
    private final File file;
    private final String resourceName;

    final ClassLoader loader;

    static ResourceInfo of(File file, String resourceName, ClassLoader loader) {
      if (resourceName.endsWith(CLASS_FILE_NAME_EXTENSION)) {
        return new ClassInfo(file, resourceName, loader);
      } else {
        return new ResourceInfo(file, resourceName, loader);
      }
    }

    ResourceInfo(File file, String resourceName, ClassLoader loader) {
      this.file = checkNotNull(file);
      this.resourceName = checkNotNull(resourceName);
      this.loader = checkNotNull(loader);
    }

    /**
     * Returns the url identifying the resource.
     *
     * <p>See {@link ClassLoader#getResource}
     *
     * @throws NoSuchElementException if the resource cannot be loaded through the class loader,
     *     despite physically existing in the class path.
     */
    public final URL url() {
      URL url = loader.getResource(resourceName);
      if (url == null) {
        throw new NoSuchElementException(resourceName);
      }
      return url;
    }

    /**
     * Returns a {@link ByteSource} view of the resource from which its bytes can be read.
     *
     * @throws NoSuchElementException if the resource cannot be loaded through the class loader,
     *     despite physically existing in the class path.
     * @since 20.0
     */
    public final ByteSource asByteSource() {
      return Resources.asByteSource(url());
    }

    /**
     * Returns a {@link CharSource} view of the resource from which its bytes can be read as
     * characters decoded with the given {@code charset}.
     *
     * @throws NoSuchElementException if the resource cannot be loaded through the class loader,
     *     despite physically existing in the class path.
     * @since 20.0
     */
    public final CharSource asCharSource(Charset charset) {
      return Resources.asCharSource(url(), charset);
    }

    /** Returns the fully qualified name of the resource. Such as "com/mycomp/foo/bar.txt". */
    public final String getResourceName() {
      return resourceName;
    }

    /** Returns the file that includes this resource. */
    final File getFile() {
      return file;
    }

    @Override
    public int hashCode() {
      return resourceName.hashCode();
    }

    @Override
    public boolean equals(@CheckForNull Object obj) {
      if (obj instanceof ResourceInfo) {
        ResourceInfo that = (ResourceInfo) obj;
        return resourceName.equals(that.resourceName) && loader == that.loader;
      }
      return false;
    }

    // Do not change this arbitrarily. We rely on it for sorting ResourceInfo.
    @Override
    public String toString() {
      return resourceName;
    }
  }

  /**
   * Represents a class that can be loaded through {@link #load}.
   *
   * @since 14.0
   */
  public static final class ClassInfo extends ResourceInfo {
    private final String className;

    ClassInfo(File file, String resourceName, ClassLoader loader) {
      super(file, resourceName, loader);
      this.className = getClassName(resourceName);
    }

    /**
     * Returns the package name of the class, without attempting to load the class.
     *
     * <p>Behaves similarly to {@code class.getPackage().}{@link Package#getName() getName()} but
     * does not require the class (or package) to be loaded.
     *
     * <p>But note that this method may behave differently for a class in the default package: For
     * such classes, this method always returns an empty string. But under some version of Java,
     * {@code class.getPackage().getName()} produces a {@code NullPointerException} because {@code
     * class.getPackage()} returns {@code null}.
     */
    public String getPackageName() {
      return Reflection.getPackageName(className);
    }

    /**
     * Returns the simple name of the underlying class as given in the source code.
     *
     * <p>Behaves similarly to {@link Class#getSimpleName()} but does not require the class to be
     * loaded.
     *
     * <p>But note that this class uses heuristics to identify the simple name. See a related
     * discussion in <a href="https://github.com/google/guava/issues/3349">issue 3349</a>.
     */
    public String getSimpleName() {
      int lastDollarSign = className.lastIndexOf('$');
      if (lastDollarSign != -1) {
        String innerClassName = className.substring(lastDollarSign + 1);
        // local and anonymous classes are prefixed with number (1,2,3...), anonymous classes are
        // entirely numeric whereas local classes have the user supplied name as a suffix
        return CharMatcher.inRange('0', '9').trimLeadingFrom(innerClassName);
      }
      String packageName = getPackageName();
      if (packageName.isEmpty()) {
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
     * Returns true if the class name "looks to be" top level (not nested), that is, it includes no
     * '$' in the name. This method may return false for a top-level class that's intentionally
     * named with the '$' character. If this is a concern, you could use {@link #load} and then
     * check on the loaded {@link Class} object instead.
     *
     * @since 30.1
     */
    public boolean isTopLevel() {
      return className.indexOf('$') == -1;
    }

    /**
     * Loads (but doesn't link or initialize) the class.
     *
     * @throws LinkageError when there were errors in loading classes that this class depends on.
     *     For example, {@link NoClassDefFoundError}.
     */
    public Class<?> load() {
      try {
        return loader.loadClass(className);
      } catch (ClassNotFoundException e) {
        // Shouldn't happen, since the class name is read from the class path.
        throw new IllegalStateException(e);
      }
    }

    @Override
    public String toString() {
      return className;
    }
  }

  /**
   * Returns all locations that {@code classloader} and parent loaders load classes and resources
   * from. Callers can {@linkplain LocationInfo#scanResources scan} individual locations selectively
   * or even in parallel.
   */
  static ImmutableSet<LocationInfo> locationsFrom(ClassLoader classloader) {
    ImmutableSet.Builder<LocationInfo> builder = ImmutableSet.builder();
    for (Map.Entry<File, ClassLoader> entry : getClassPathEntries(classloader).entrySet()) {
      builder.add(new LocationInfo(entry.getKey(), entry.getValue()));
    }
    return builder.build();
  }

  /**
   * Represents a single location (a directory or a jar file) in the class path and is responsible
   * for scanning resources from this location.
   */
  static final class LocationInfo {
    final File home;
    private final ClassLoader classloader;

    LocationInfo(File home, ClassLoader classloader) {
      this.home = checkNotNull(home);
      this.classloader = checkNotNull(classloader);
    }

    /** Returns the file this location is from. */
    public final File file() {
      return home;
    }

    /** Scans this location and returns all scanned resources. */
    public ImmutableSet<ResourceInfo> scanResources() throws IOException {
      return scanResources(new HashSet<File>());
    }

    /**
     * Scans this location and returns all scanned resources.
     *
     * <p>This file and jar files from "Class-Path" entry in the scanned manifest files will be
     * added to {@code scannedFiles}.
     *
     * <p>A file will be scanned at most once even if specified multiple times by one or multiple
     * jar files' "Class-Path" manifest entries. Particularly, if a jar file from the "Class-Path"
     * manifest entry is already in {@code scannedFiles}, either because it was scanned earlier, or
     * it was intentionally added to the set by the caller, it will not be scanned again.
     *
     * <p>Note that when you call {@code location.scanResources(scannedFiles)}, the location will
     * always be scanned even if {@code scannedFiles} already contains it.
     */
    public ImmutableSet<ResourceInfo> scanResources(Set<File> scannedFiles) throws IOException {
      ImmutableSet.Builder<ResourceInfo> builder = ImmutableSet.builder();
      scannedFiles.add(home);
      scan(home, scannedFiles, builder);
      return builder.build();
    }

    private void scan(File file, Set<File> scannedUris, ImmutableSet.Builder<ResourceInfo> builder)
        throws IOException {
      try {
        if (!file.exists()) {
          return;
        }
      } catch (SecurityException e) {
        logger.warning("Cannot access " + file + ": " + e);
        // TODO(emcmanus): consider whether to log other failure cases too.
        return;
      }
      if (file.isDirectory()) {
        scanDirectory(file, builder);
      } else {
        scanJar(file, scannedUris, builder);
      }
    }

    private void scanJar(
        File file, Set<File> scannedUris, ImmutableSet.Builder<ResourceInfo> builder)
        throws IOException {
      JarFile jarFile;
      try {
        jarFile = new JarFile(file);
      } catch (IOException e) {
        // Not a jar file
        return;
      }
      try {
        for (File path : getClassPathFromManifest(file, jarFile.getManifest())) {
          // We only scan each file once independent of the classloader that file might be
          // associated with.
          if (scannedUris.add(path.getCanonicalFile())) {
            scan(path, scannedUris, builder);
          }
        }
        scanJarFile(jarFile, builder);
      } finally {
        try {
          jarFile.close();
        } catch (IOException ignored) { // similar to try-with-resources, but don't fail scanning
        }
      }
    }

    private void scanJarFile(JarFile file, ImmutableSet.Builder<ResourceInfo> builder) {
      Enumeration<JarEntry> entries = file.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        if (entry.isDirectory() || entry.getName().equals(JarFile.MANIFEST_NAME)) {
          continue;
        }
        builder.add(ResourceInfo.of(new File(file.getName()), entry.getName(), classloader));
      }
    }

    private void scanDirectory(File directory, ImmutableSet.Builder<ResourceInfo> builder)
        throws IOException {
      Set<File> currentPath = new HashSet<>();
      currentPath.add(directory.getCanonicalFile());
      scanDirectory(directory, "", currentPath, builder);
    }

    /**
     * Recursively scan the given directory, adding resources for each file encountered. Symlinks
     * which have already been traversed in the current tree path will be skipped to eliminate
     * cycles; otherwise symlinks are traversed.
     *
     * @param directory the root of the directory to scan
     * @param packagePrefix resource path prefix inside {@code classloader} for any files found
     *     under {@code directory}
     * @param currentPath canonical files already visited in the current directory tree path, for
     *     cycle elimination
     */
    private void scanDirectory(
        File directory,
        String packagePrefix,
        Set<File> currentPath,
        ImmutableSet.Builder<ResourceInfo> builder)
        throws IOException {
      File[] files = directory.listFiles();
      if (files == null) {
        logger.warning("Cannot read directory " + directory);
        // IO error, just skip the directory
        return;
      }
      for (File f : files) {
        String name = f.getName();
        if (f.isDirectory()) {
          File deref = f.getCanonicalFile();
          if (currentPath.add(deref)) {
            scanDirectory(deref, packagePrefix + name + "/", currentPath, builder);
            currentPath.remove(deref);
          }
        } else {
          String resourceName = packagePrefix + name;
          if (!resourceName.equals(JarFile.MANIFEST_NAME)) {
            builder.add(ResourceInfo.of(f, resourceName, classloader));
          }
        }
      }
    }

    @Override
    public boolean equals(@CheckForNull Object obj) {
      if (obj instanceof LocationInfo) {
        LocationInfo that = (LocationInfo) obj;
        return home.equals(that.home) && classloader.equals(that.classloader);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return home.hashCode();
    }

    @Override
    public String toString() {
      return home.toString();
    }
  }

  /**
   * Returns the class path URIs specified by the {@code Class-Path} manifest attribute, according
   * to <a
   * href="http://docs.oracle.com/javase/8/docs/technotes/guides/jar/jar.html#Main_Attributes">JAR
   * File Specification</a>. If {@code manifest} is null, it means the jar file has no manifest, and
   * an empty set will be returned.
   */
  @VisibleForTesting
  static ImmutableSet<File> getClassPathFromManifest(
      File jarFile, @CheckForNull Manifest manifest) {
    if (manifest == null) {
      return ImmutableSet.of();
    }
    ImmutableSet.Builder<File> builder = ImmutableSet.builder();
    String classpathAttribute =
        manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH.toString());
    if (classpathAttribute != null) {
      for (String path : CLASS_PATH_ATTRIBUTE_SEPARATOR.split(classpathAttribute)) {
        URL url;
        try {
          url = getClassPathEntry(jarFile, path);
        } catch (MalformedURLException e) {
          // Ignore bad entry
          logger.warning("Invalid Class-Path entry: " + path);
          continue;
        }
        if (url.getProtocol().equals("file")) {
          builder.add(toFile(url));
        }
      }
    }
    return builder.build();
  }

  @VisibleForTesting
  static ImmutableMap<File, ClassLoader> getClassPathEntries(ClassLoader classloader) {
    LinkedHashMap<File, ClassLoader> entries = Maps.newLinkedHashMap();
    // Search parent first, since it's the order ClassLoader#loadClass() uses.
    ClassLoader parent = classloader.getParent();
    if (parent != null) {
      entries.putAll(getClassPathEntries(parent));
    }
    for (URL url : getClassLoaderUrls(classloader)) {
      if (url.getProtocol().equals("file")) {
        File file = toFile(url);
        if (!entries.containsKey(file)) {
          entries.put(file, classloader);
        }
      }
    }
    return ImmutableMap.copyOf(entries);
  }

  private static ImmutableList<URL> getClassLoaderUrls(ClassLoader classloader) {
    if (classloader instanceof URLClassLoader) {
      return ImmutableList.copyOf(((URLClassLoader) classloader).getURLs());
    }
    if (classloader.equals(ClassLoader.getSystemClassLoader())) {
      return parseJavaClassPath();
    }
    return ImmutableList.of();
  }

  /**
   * Returns the URLs in the class path specified by the {@code java.class.path} {@linkplain
   * System#getProperty system property}.
   */
  @VisibleForTesting // TODO(b/65488446): Make this a public API.
  static ImmutableList<URL> parseJavaClassPath() {
    ImmutableList.Builder<URL> urls = ImmutableList.builder();
    for (String entry : Splitter.on(PATH_SEPARATOR.value()).split(JAVA_CLASS_PATH.value())) {
      try {
        try {
          urls.add(new File(entry).toURI().toURL());
        } catch (SecurityException e) { // File.toURI checks to see if the file is a directory
          urls.add(new URL("file", null, new File(entry).getAbsolutePath()));
        }
      } catch (MalformedURLException e) {
        logger.log(WARNING, "malformed classpath entry: " + entry, e);
      }
    }
    return urls.build();
  }

  /**
   * Returns the absolute uri of the Class-Path entry value as specified in <a
   * href="http://docs.oracle.com/javase/8/docs/technotes/guides/jar/jar.html#Main_Attributes">JAR
   * File Specification</a>. Even though the specification only talks about relative urls, absolute
   * urls are actually supported too (for example, in Maven surefire plugin).
   */
  @VisibleForTesting
  static URL getClassPathEntry(File jarFile, String path) throws MalformedURLException {
    return new URL(jarFile.toURI().toURL(), path);
  }

  @VisibleForTesting
  static String getClassName(String filename) {
    int classNameEnd = filename.length() - CLASS_FILE_NAME_EXTENSION.length();
    return filename.substring(0, classNameEnd).replace('/', '.');
  }

  // TODO(benyu): Try java.nio.file.Paths#get() when Guava drops JDK 6 support.
  @VisibleForTesting
  static File toFile(URL url) {
    checkArgument(url.getProtocol().equals("file"));
    try {
      return new File(url.toURI()); // Accepts escaped characters like %20.
    } catch (URISyntaxException e) { // URL.toURI() doesn't escape chars.
      return new File(url.getPath()); // Accepts non-escaped chars like space.
    }
  }
}
