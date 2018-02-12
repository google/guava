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

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
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
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * Scans the source of a {@link ClassLoader} and finds all loadable classes and resources.
 *
 * <p><b>Warning:</b> Current limitations:
 *
 * <ul>
 *   <li>Looks only for files and JARs in URLs available from {@link URLClassLoader} instances or
 *       the {@linkplain ClassLoader#getSystemClassLoader() system class loader}.
 *   <li>Only understands {@code file:} URLs.
 * </ul>
 *
 * <p>In the case of directory classloaders, symlinks are supported but cycles are not traversed.
 * This guarantees discovery of each <em>unique</em> loadable resource. However, not all possible
 * aliases for resources on cyclic paths will be listed.
 *
 * @author Ben Yu
 * @since 14.0
 */
@Beta
public final class ClassPath {
  private static final Logger logger = Logger.getLogger(ClassPath.class.getName());

  private static final Predicate<ClassInfo> IS_TOP_LEVEL =
      new Predicate<ClassInfo>() {
        @Override
        public boolean apply(ClassInfo info) {
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
    DefaultScanner scanner = new DefaultScanner();
    scanner.scan(classloader);
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

    @Override
    public int hashCode() {
      return resourceName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
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
        return CharMatcher.digit().trimLeadingFrom(innerClassName);
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
   * Abstract class that scans through the class path represented by a {@link ClassLoader} and calls
   * {@link #scanDirectory} and {@link #scanJarFile} for directories and jar files on the class path
   * respectively.
   */
  abstract static class Scanner {

    // We only scan each file once independent of the classloader that resource might be associated
    // with.
    private final Set<File> scannedUris = Sets.newHashSet();

    public final void scan(ClassLoader classloader) throws IOException {
      for (Entry<File, ClassLoader> entry : getClassPathEntries(classloader).entrySet()) {
        scan(entry.getKey(), entry.getValue());
      }
    }

    @VisibleForTesting
    final void scan(File file, ClassLoader classloader) throws IOException {
      if (scannedUris.add(file.getCanonicalFile())) {
        scanFrom(file, classloader);
      }
    }

    /** Called when a directory is scanned for resource files. */
    protected abstract void scanDirectory(ClassLoader loader, File directory) throws IOException;

    /** Called when a jar file is scanned for resource entries. */
    protected abstract void scanJarFile(ClassLoader loader, JarFile file) throws IOException;

    private void scanFrom(File file, ClassLoader classloader) throws IOException {
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
        scanDirectory(classloader, file);
      } else {
        scanJar(file, classloader);
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
        for (File path : getClassPathFromManifest(file, jarFile.getManifest())) {
          scan(path, classloader);
        }
        scanJarFile(classloader, jarFile);
      } finally {
        try {
          jarFile.close();
        } catch (IOException ignored) {
        }
      }
    }

    /**
     * Returns the class path URIs specified by the {@code Class-Path} manifest attribute, according
     * to <a
     * href="http://docs.oracle.com/javase/8/docs/technotes/guides/jar/jar.html#Main_Attributes">JAR
     * File Specification</a>. If {@code manifest} is null, it means the jar file has no manifest,
     * and an empty set will be returned.
     */
    @VisibleForTesting
    static ImmutableSet<File> getClassPathFromManifest(
        File jarFile, @NullableDecl Manifest manifest) {
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
     * File Specification</a>. Even though the specification only talks about relative urls,
     * absolute urls are actually supported too (for example, in Maven surefire plugin).
     */
    @VisibleForTesting
    static URL getClassPathEntry(File jarFile, String path) throws MalformedURLException {
      return new URL(jarFile.toURI().toURL(), path);
    }
  }

  @VisibleForTesting
  static final class DefaultScanner extends Scanner {
    private final SetMultimap<ClassLoader, String> resources =
        MultimapBuilder.hashKeys().linkedHashSetValues().build();

    ImmutableSet<ResourceInfo> getResources() {
      ImmutableSet.Builder<ResourceInfo> builder = ImmutableSet.builder();
      for (Entry<ClassLoader, String> entry : resources.entries()) {
        builder.add(ResourceInfo.of(entry.getValue(), entry.getKey()));
      }
      return builder.build();
    }

    @Override
    protected void scanJarFile(ClassLoader classloader, JarFile file) {
      Enumeration<JarEntry> entries = file.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        if (entry.isDirectory() || entry.getName().equals(JarFile.MANIFEST_NAME)) {
          continue;
        }
        resources.get(classloader).add(entry.getName());
      }
    }

    @Override
    protected void scanDirectory(ClassLoader classloader, File directory) throws IOException {
      Set<File> currentPath = new HashSet<>();
      currentPath.add(directory.getCanonicalFile());
      scanDirectory(directory, classloader, "", currentPath);
    }

    /**
     * Recursively scan the given directory, adding resources for each file encountered. Symlinks
     * which have already been traversed in the current tree path will be skipped to eliminate
     * cycles; otherwise symlinks are traversed.
     *
     * @param directory the root of the directory to scan
     * @param classloader the classloader that includes resources found in {@code directory}
     * @param packagePrefix resource path prefix inside {@code classloader} for any files found
     *     under {@code directory}
     * @param currentPath canonical files already visited in the current directory tree path, for
     *     cycle elimination
     */
    private void scanDirectory(
        File directory, ClassLoader classloader, String packagePrefix, Set<File> currentPath)
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
            scanDirectory(deref, classloader, packagePrefix + name + "/", currentPath);
            currentPath.remove(deref);
          }
        } else {
          String resourceName = packagePrefix + name;
          if (!resourceName.equals(JarFile.MANIFEST_NAME)) {
            resources.get(classloader).add(resourceName);
          }
        }
      }
    }
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
