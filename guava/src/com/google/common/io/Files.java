/*
 * Copyright (C) 2007 The Guava Authors
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

package com.google.common.io;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.io.FileWriteMode.APPEND;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.graph.SuccessorsFunction;
import com.google.common.graph.Traverser;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.InlineMe;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Provides utility methods for working with {@linkplain File files}.
 *
 * <p>{@link java.nio.file.Path} users will find similar utilities in {@link MoreFiles} and the
 * JDK's {@link java.nio.file.Files} class.
 *
 * @author Chris Nokleberg
 * @author Colin Decker
 * @since 1.0
 */
@GwtIncompatible
@ElementTypesAreNonnullByDefault
public final class Files {

  /** Maximum loop count when creating temp directories. */
  private static final int TEMP_DIR_ATTEMPTS = 10000;

  private Files() {}

  /**
   * Returns a buffered reader that reads from a file using the given character set.
   *
   * <p><b>{@link java.nio.file.Path} equivalent:</b> {@link
   * java.nio.file.Files#newBufferedReader(java.nio.file.Path, Charset)}.
   *
   * @param file the file to read from
   * @param charset the charset used to decode the input stream; see {@link StandardCharsets} for
   *     helpful predefined constants
   * @return the buffered reader
   */
  public static BufferedReader newReader(File file, Charset charset) throws FileNotFoundException {
    checkNotNull(file);
    checkNotNull(charset);
    return new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
  }

  /**
   * Returns a buffered writer that writes to a file using the given character set.
   *
   * <p><b>{@link java.nio.file.Path} equivalent:</b> {@link
   * java.nio.file.Files#newBufferedWriter(java.nio.file.Path, Charset,
   * java.nio.file.OpenOption...)}.
   *
   * @param file the file to write to
   * @param charset the charset used to encode the output stream; see {@link StandardCharsets} for
   *     helpful predefined constants
   * @return the buffered writer
   */
  public static BufferedWriter newWriter(File file, Charset charset) throws FileNotFoundException {
    checkNotNull(file);
    checkNotNull(charset);
    return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), charset));
  }

  /**
   * Returns a new {@link ByteSource} for reading bytes from the given file.
   *
   * @since 14.0
   */
  public static ByteSource asByteSource(File file) {
    return new FileByteSource(file);
  }

  private static final class FileByteSource extends ByteSource {

    private final File file;

    private FileByteSource(File file) {
      this.file = checkNotNull(file);
    }

    @Override
    public FileInputStream openStream() throws IOException {
      return new FileInputStream(file);
    }

    @Override
    public Optional<Long> sizeIfKnown() {
      if (file.isFile()) {
        return Optional.of(file.length());
      } else {
        return Optional.absent();
      }
    }

    @Override
    public long size() throws IOException {
      if (!file.isFile()) {
        throw new FileNotFoundException(file.toString());
      }
      return file.length();
    }

    @Override
    public byte[] read() throws IOException {
      Closer closer = Closer.create();
      try {
        FileInputStream in = closer.register(openStream());
        return ByteStreams.toByteArray(in, in.getChannel().size());
      } catch (Throwable e) {
        throw closer.rethrow(e);
      } finally {
        closer.close();
      }
    }

    @Override
    public String toString() {
      return "Files.asByteSource(" + file + ")";
    }
  }

  /**
   * Returns a new {@link ByteSink} for writing bytes to the given file. The given {@code modes}
   * control how the file is opened for writing. When no mode is provided, the file will be
   * truncated before writing. When the {@link FileWriteMode#APPEND APPEND} mode is provided, writes
   * will append to the end of the file without truncating it.
   *
   * @since 14.0
   */
  public static ByteSink asByteSink(File file, FileWriteMode... modes) {
    return new FileByteSink(file, modes);
  }

  private static final class FileByteSink extends ByteSink {

    private final File file;
    private final ImmutableSet<FileWriteMode> modes;

    private FileByteSink(File file, FileWriteMode... modes) {
      this.file = checkNotNull(file);
      this.modes = ImmutableSet.copyOf(modes);
    }

    @Override
    public FileOutputStream openStream() throws IOException {
      return new FileOutputStream(file, modes.contains(APPEND));
    }

    @Override
    public String toString() {
      return "Files.asByteSink(" + file + ", " + modes + ")";
    }
  }

  /**
   * Returns a new {@link CharSource} for reading character data from the given file using the given
   * character set.
   *
   * @since 14.0
   */
  public static CharSource asCharSource(File file, Charset charset) {
    return asByteSource(file).asCharSource(charset);
  }

  /**
   * Returns a new {@link CharSink} for writing character data to the given file using the given
   * character set. The given {@code modes} control how the file is opened for writing. When no mode
   * is provided, the file will be truncated before writing. When the {@link FileWriteMode#APPEND
   * APPEND} mode is provided, writes will append to the end of the file without truncating it.
   *
   * @since 14.0
   */
  public static CharSink asCharSink(File file, Charset charset, FileWriteMode... modes) {
    return asByteSink(file, modes).asCharSink(charset);
  }

  /**
   * Reads all bytes from a file into a byte array.
   *
   * <p><b>{@link java.nio.file.Path} equivalent:</b> {@link java.nio.file.Files#readAllBytes}.
   *
   * @param file the file to read from
   * @return a byte array containing all the bytes from file
   * @throws IllegalArgumentException if the file is bigger than the largest possible byte array
   *     (2^31 - 1)
   * @throws IOException if an I/O error occurs
   */
  public static byte[] toByteArray(File file) throws IOException {
    return asByteSource(file).read();
  }

  /**
   * Reads all characters from a file into a {@link String}, using the given character set.
   *
   * @param file the file to read from
   * @param charset the charset used to decode the input stream; see {@link StandardCharsets} for
   *     helpful predefined constants
   * @return a string containing all the characters from the file
   * @throws IOException if an I/O error occurs
   * @deprecated Prefer {@code asCharSource(file, charset).read()}.
   */
  @Deprecated
  @InlineMe(
      replacement = "Files.asCharSource(file, charset).read()",
      imports = "com.google.common.io.Files")
  public static String toString(File file, Charset charset) throws IOException {
    return asCharSource(file, charset).read();
  }

  /**
   * Overwrites a file with the contents of a byte array.
   *
   * <p><b>{@link java.nio.file.Path} equivalent:</b> {@link
   * java.nio.file.Files#write(java.nio.file.Path, byte[], java.nio.file.OpenOption...)}.
   *
   * @param from the bytes to write
   * @param to the destination file
   * @throws IOException if an I/O error occurs
   */
  public static void write(byte[] from, File to) throws IOException {
    asByteSink(to).write(from);
  }

  /**
   * Writes a character sequence (such as a string) to a file using the given character set.
   *
   * @param from the character sequence to write
   * @param to the destination file
   * @param charset the charset used to encode the output stream; see {@link StandardCharsets} for
   *     helpful predefined constants
   * @throws IOException if an I/O error occurs
   * @deprecated Prefer {@code asCharSink(to, charset).write(from)}.
   */
  @Deprecated
  @InlineMe(
      replacement = "Files.asCharSink(to, charset).write(from)",
      imports = "com.google.common.io.Files")
  public static void write(CharSequence from, File to, Charset charset) throws IOException {
    asCharSink(to, charset).write(from);
  }

  /**
   * Copies all bytes from a file to an output stream.
   *
   * <p><b>{@link java.nio.file.Path} equivalent:</b> {@link
   * java.nio.file.Files#copy(java.nio.file.Path, OutputStream)}.
   *
   * @param from the source file
   * @param to the output stream
   * @throws IOException if an I/O error occurs
   */
  public static void copy(File from, OutputStream to) throws IOException {
    asByteSource(from).copyTo(to);
  }

  /**
   * Copies all the bytes from one file to another.
   *
   * <p>Copying is not an atomic operation - in the case of an I/O error, power loss, process
   * termination, or other problems, {@code to} may not be a complete copy of {@code from}. If you
   * need to guard against those conditions, you should employ other file-level synchronization.
   *
   * <p><b>Warning:</b> If {@code to} represents an existing file, that file will be overwritten
   * with the contents of {@code from}. If {@code to} and {@code from} refer to the <i>same</i>
   * file, the contents of that file will be deleted.
   *
   * <p><b>{@link java.nio.file.Path} equivalent:</b> {@link
   * java.nio.file.Files#copy(java.nio.file.Path, java.nio.file.Path, java.nio.file.CopyOption...)}.
   *
   * @param from the source file
   * @param to the destination file
   * @throws IOException if an I/O error occurs
   * @throws IllegalArgumentException if {@code from.equals(to)}
   */
  public static void copy(File from, File to) throws IOException {
    checkArgument(!from.equals(to), "Source %s and destination %s must be different", from, to);
    asByteSource(from).copyTo(asByteSink(to));
  }

  /**
   * Copies all characters from a file to an appendable object, using the given character set.
   *
   * @param from the source file
   * @param charset the charset used to decode the input stream; see {@link StandardCharsets} for
   *     helpful predefined constants
   * @param to the appendable object
   * @throws IOException if an I/O error occurs
   * @deprecated Prefer {@code asCharSource(from, charset).copyTo(to)}.
   */
  @Deprecated
  @InlineMe(
      replacement = "Files.asCharSource(from, charset).copyTo(to)",
      imports = "com.google.common.io.Files")
  public
  static void copy(File from, Charset charset, Appendable to) throws IOException {
    asCharSource(from, charset).copyTo(to);
  }

  /**
   * Appends a character sequence (such as a string) to a file using the given character set.
   *
   * @param from the character sequence to append
   * @param to the destination file
   * @param charset the charset used to encode the output stream; see {@link StandardCharsets} for
   *     helpful predefined constants
   * @throws IOException if an I/O error occurs
   * @deprecated Prefer {@code asCharSink(to, charset, FileWriteMode.APPEND).write(from)}. This
   *     method is scheduled to be removed in October 2019.
   */
  @Deprecated
  @InlineMe(
      replacement = "Files.asCharSink(to, charset, FileWriteMode.APPEND).write(from)",
      imports = {"com.google.common.io.FileWriteMode", "com.google.common.io.Files"})
  public
  static void append(CharSequence from, File to, Charset charset) throws IOException {
    asCharSink(to, charset, FileWriteMode.APPEND).write(from);
  }

  /**
   * Returns true if the given files exist, are not directories, and contain the same bytes.
   *
   * @throws IOException if an I/O error occurs
   */
  public static boolean equal(File file1, File file2) throws IOException {
    checkNotNull(file1);
    checkNotNull(file2);
    if (file1 == file2 || file1.equals(file2)) {
      return true;
    }

    /*
     * Some operating systems may return zero as the length for files denoting system-dependent
     * entities such as devices or pipes, in which case we must fall back on comparing the bytes
     * directly.
     */
    long len1 = file1.length();
    long len2 = file2.length();
    if (len1 != 0 && len2 != 0 && len1 != len2) {
      return false;
    }
    return asByteSource(file1).contentEquals(asByteSource(file2));
  }

  /**
   * Atomically creates a new directory somewhere beneath the system's temporary directory (as
   * defined by the {@code java.io.tmpdir} system property), and returns its name.
   *
   * <p>Use this method instead of {@link File#createTempFile(String, String)} when you wish to
   * create a directory, not a regular file. A common pitfall is to call {@code createTempFile},
   * delete the file and create a directory in its place, but this leads a race condition which can
   * be exploited to create security vulnerabilities, especially when executable files are to be
   * written into the directory.
   *
   * <p>Depending on the environmment that this code is run in, the system temporary directory (and
   * thus the directory this method creates) may be more visible that a program would like - files
   * written to this directory may be read or overwritten by hostile programs running on the same
   * machine.
   *
   * <p>This method assumes that the temporary volume is writable, has free inodes and free blocks,
   * and that it will not be called thousands of times per second.
   *
   * <p><b>{@link java.nio.file.Path} equivalent:</b> {@link
   * java.nio.file.Files#createTempDirectory}.
   *
   * @return the newly-created directory
   * @throws IllegalStateException if the directory could not be created
   * @deprecated For Android users, see the <a
   *     href="https://developer.android.com/training/data-storage" target="_blank">Data and File
   *     Storage overview</a> to select an appropriate temporary directory (perhaps {@code
   *     context.getCacheDir()}). For developers on Java 7 or later, use {@link
   *     java.nio.file.Files#createTempDirectory}, transforming it to a {@link File} using {@link
   *     java.nio.file.Path#toFile() toFile()} if needed.
   */
  @Beta
  @Deprecated
  public static File createTempDir() {
    File baseDir = new File(System.getProperty("java.io.tmpdir"));
    @SuppressWarnings("GoodTime") // reading system time without TimeSource
    String baseName = System.currentTimeMillis() + "-";

    for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
      File tempDir = new File(baseDir, baseName + counter);
      if (tempDir.mkdir()) {
        return tempDir;
      }
    }
    throw new IllegalStateException(
        "Failed to create directory within "
            + TEMP_DIR_ATTEMPTS
            + " attempts (tried "
            + baseName
            + "0 to "
            + baseName
            + (TEMP_DIR_ATTEMPTS - 1)
            + ')');
  }

  /**
   * Creates an empty file or updates the last updated timestamp on the same as the unix command of
   * the same name.
   *
   * @param file the file to create or update
   * @throws IOException if an I/O error occurs
   */
  @SuppressWarnings("GoodTime") // reading system time without TimeSource
  public static void touch(File file) throws IOException {
    checkNotNull(file);
    if (!file.createNewFile() && !file.setLastModified(System.currentTimeMillis())) {
      throw new IOException("Unable to update modification time of " + file);
    }
  }

  /**
   * Creates any necessary but nonexistent parent directories of the specified file. Note that if
   * this operation fails it may have succeeded in creating some (but not all) of the necessary
   * parent directories.
   *
   * @throws IOException if an I/O error occurs, or if any necessary but nonexistent parent
   *     directories of the specified file could not be created.
   * @since 4.0
   */
  public static void createParentDirs(File file) throws IOException {
    checkNotNull(file);
    File parent = file.getCanonicalFile().getParentFile();
    if (parent == null) {
      /*
       * The given directory is a filesystem root. All zero of its ancestors exist. This doesn't
       * mean that the root itself exists -- consider x:\ on a Windows machine without such a drive
       * -- or even that the caller can create it, but this method makes no such guarantees even for
       * non-root files.
       */
      return;
    }
    parent.mkdirs();
    if (!parent.isDirectory()) {
      throw new IOException("Unable to create parent directories of " + file);
    }
  }

  /**
   * Moves a file from one path to another. This method can rename a file and/or move it to a
   * different directory. In either case {@code to} must be the target path for the file itself; not
   * just the new name for the file or the path to the new parent directory.
   *
   * <p><b>{@link java.nio.file.Path} equivalent:</b> {@link java.nio.file.Files#move}.
   *
   * @param from the source file
   * @param to the destination file
   * @throws IOException if an I/O error occurs
   * @throws IllegalArgumentException if {@code from.equals(to)}
   */
  public static void move(File from, File to) throws IOException {
    checkNotNull(from);
    checkNotNull(to);
    checkArgument(!from.equals(to), "Source %s and destination %s must be different", from, to);

    if (!from.renameTo(to)) {
      copy(from, to);
      if (!from.delete()) {
        if (!to.delete()) {
          throw new IOException("Unable to delete " + to);
        }
        throw new IOException("Unable to delete " + from);
      }
    }
  }

  /**
   * Reads the first line from a file. The line does not include line-termination characters, but
   * does include other leading and trailing whitespace.
   *
   * @param file the file to read from
   * @param charset the charset used to decode the input stream; see {@link StandardCharsets} for
   *     helpful predefined constants
   * @return the first line, or null if the file is empty
   * @throws IOException if an I/O error occurs
   * @deprecated Prefer {@code asCharSource(file, charset).readFirstLine()}.
   */
  @Deprecated
  @InlineMe(
      replacement = "Files.asCharSource(file, charset).readFirstLine()",
      imports = "com.google.common.io.Files")
  @CheckForNull
  public
  static String readFirstLine(File file, Charset charset) throws IOException {
    return asCharSource(file, charset).readFirstLine();
  }

  /**
   * Reads all of the lines from a file. The lines do not include line-termination characters, but
   * do include other leading and trailing whitespace.
   *
   * <p>This method returns a mutable {@code List}. For an {@code ImmutableList}, use {@code
   * Files.asCharSource(file, charset).readLines()}.
   *
   * <p><b>{@link java.nio.file.Path} equivalent:</b> {@link
   * java.nio.file.Files#readAllLines(java.nio.file.Path, Charset)}.
   *
   * @param file the file to read from
   * @param charset the charset used to decode the input stream; see {@link StandardCharsets} for
   *     helpful predefined constants
   * @return a mutable {@link List} containing all the lines
   * @throws IOException if an I/O error occurs
   */
  public static List<String> readLines(File file, Charset charset) throws IOException {
    // don't use asCharSource(file, charset).readLines() because that returns
    // an immutable list, which would change the behavior of this method
    return asCharSource(file, charset)
        .readLines(
            new LineProcessor<List<String>>() {
              final List<String> result = Lists.newArrayList();

              @Override
              public boolean processLine(String line) {
                result.add(line);
                return true;
              }

              @Override
              public List<String> getResult() {
                return result;
              }
            });
  }

  /**
   * Streams lines from a {@link File}, stopping when our callback returns false, or we have read
   * all of the lines.
   *
   * @param file the file to read from
   * @param charset the charset used to decode the input stream; see {@link StandardCharsets} for
   *     helpful predefined constants
   * @param callback the {@link LineProcessor} to use to handle the lines
   * @return the output of processing the lines
   * @throws IOException if an I/O error occurs
   * @deprecated Prefer {@code asCharSource(file, charset).readLines(callback)}.
   */
  @Deprecated
  @InlineMe(
      replacement = "Files.asCharSource(file, charset).readLines(callback)",
      imports = "com.google.common.io.Files")
  @CanIgnoreReturnValue // some processors won't return a useful result
  @ParametricNullness
  public
  static <T extends @Nullable Object> T readLines(
      File file, Charset charset, LineProcessor<T> callback) throws IOException {
    return asCharSource(file, charset).readLines(callback);
  }

  /**
   * Process the bytes of a file.
   *
   * <p>(If this seems too complicated, maybe you're looking for {@link #toByteArray}.)
   *
   * @param file the file to read
   * @param processor the object to which the bytes of the file are passed.
   * @return the result of the byte processor
   * @throws IOException if an I/O error occurs
   * @deprecated Prefer {@code asByteSource(file).read(processor)}.
   */
  @Deprecated
  @InlineMe(
      replacement = "Files.asByteSource(file).read(processor)",
      imports = "com.google.common.io.Files")
  @CanIgnoreReturnValue // some processors won't return a useful result
  @ParametricNullness
  public
  static <T extends @Nullable Object> T readBytes(File file, ByteProcessor<T> processor)
      throws IOException {
    return asByteSource(file).read(processor);
  }

  /**
   * Computes the hash code of the {@code file} using {@code hashFunction}.
   *
   * @param file the file to read
   * @param hashFunction the hash function to use to hash the data
   * @return the {@link HashCode} of all of the bytes in the file
   * @throws IOException if an I/O error occurs
   * @since 12.0
   * @deprecated Prefer {@code asByteSource(file).hash(hashFunction)}.
   */
  @Deprecated
  @InlineMe(
      replacement = "Files.asByteSource(file).hash(hashFunction)",
      imports = "com.google.common.io.Files")
  public
  static HashCode hash(File file, HashFunction hashFunction) throws IOException {
    return asByteSource(file).hash(hashFunction);
  }

  /**
   * Fully maps a file read-only in to memory as per {@link
   * FileChannel#map(java.nio.channels.FileChannel.MapMode, long, long)}.
   *
   * <p>Files are mapped from offset 0 to its length.
   *
   * <p>This only works for files ≤ {@link Integer#MAX_VALUE} bytes.
   *
   * @param file the file to map
   * @return a read-only buffer reflecting {@code file}
   * @throws FileNotFoundException if the {@code file} does not exist
   * @throws IOException if an I/O error occurs
   * @see FileChannel#map(MapMode, long, long)
   * @since 2.0
   */
  public static MappedByteBuffer map(File file) throws IOException {
    checkNotNull(file);
    return map(file, MapMode.READ_ONLY);
  }

  /**
   * Fully maps a file in to memory as per {@link
   * FileChannel#map(java.nio.channels.FileChannel.MapMode, long, long)} using the requested {@link
   * MapMode}.
   *
   * <p>Files are mapped from offset 0 to its length.
   *
   * <p>This only works for files ≤ {@link Integer#MAX_VALUE} bytes.
   *
   * @param file the file to map
   * @param mode the mode to use when mapping {@code file}
   * @return a buffer reflecting {@code file}
   * @throws FileNotFoundException if the {@code file} does not exist
   * @throws IOException if an I/O error occurs
   * @see FileChannel#map(MapMode, long, long)
   * @since 2.0
   */
  public static MappedByteBuffer map(File file, MapMode mode) throws IOException {
    return mapInternal(file, mode, -1);
  }

  /**
   * Maps a file in to memory as per {@link FileChannel#map(java.nio.channels.FileChannel.MapMode,
   * long, long)} using the requested {@link MapMode}.
   *
   * <p>Files are mapped from offset 0 to {@code size}.
   *
   * <p>If the mode is {@link MapMode#READ_WRITE} and the file does not exist, it will be created
   * with the requested {@code size}. Thus this method is useful for creating memory mapped files
   * which do not yet exist.
   *
   * <p>This only works for files ≤ {@link Integer#MAX_VALUE} bytes.
   *
   * @param file the file to map
   * @param mode the mode to use when mapping {@code file}
   * @return a buffer reflecting {@code file}
   * @throws IOException if an I/O error occurs
   * @see FileChannel#map(MapMode, long, long)
   * @since 2.0
   */
  public static MappedByteBuffer map(File file, MapMode mode, long size) throws IOException {
    checkArgument(size >= 0, "size (%s) may not be negative", size);
    return mapInternal(file, mode, size);
  }

  private static MappedByteBuffer mapInternal(File file, MapMode mode, long size)
      throws IOException {
    checkNotNull(file);
    checkNotNull(mode);

    Closer closer = Closer.create();
    try {
      RandomAccessFile raf =
          closer.register(new RandomAccessFile(file, mode == MapMode.READ_ONLY ? "r" : "rw"));
      FileChannel channel = closer.register(raf.getChannel());
      return channel.map(mode, 0, size == -1 ? channel.size() : size);
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  /**
   * Returns the lexically cleaned form of the path name, <i>usually</i> (but not always) equivalent
   * to the original. The following heuristics are used:
   *
   * <ul>
   *   <li>empty string becomes .
   *   <li>. stays as .
   *   <li>fold out ./
   *   <li>fold out ../ when possible
   *   <li>collapse multiple slashes
   *   <li>delete trailing slashes (unless the path is just "/")
   * </ul>
   *
   * <p>These heuristics do not always match the behavior of the filesystem. In particular, consider
   * the path {@code a/../b}, which {@code simplifyPath} will change to {@code b}. If {@code a} is a
   * symlink to {@code x}, {@code a/../b} may refer to a sibling of {@code x}, rather than the
   * sibling of {@code a} referred to by {@code b}.
   *
   * @since 11.0
   */
  public static String simplifyPath(String pathname) {
    checkNotNull(pathname);
    if (pathname.length() == 0) {
      return ".";
    }

    // split the path apart
    Iterable<String> components = Splitter.on('/').omitEmptyStrings().split(pathname);
    List<String> path = new ArrayList<>();

    // resolve ., .., and //
    for (String component : components) {
      switch (component) {
        case ".":
          continue;
        case "..":
          if (path.size() > 0 && !path.get(path.size() - 1).equals("..")) {
            path.remove(path.size() - 1);
          } else {
            path.add("..");
          }
          break;
        default:
          path.add(component);
          break;
      }
    }

    // put it back together
    String result = Joiner.on('/').join(path);
    if (pathname.charAt(0) == '/') {
      result = "/" + result;
    }

    while (result.startsWith("/../")) {
      result = result.substring(3);
    }
    if (result.equals("/..")) {
      result = "/";
    } else if ("".equals(result)) {
      result = ".";
    }

    return result;
  }

  /**
   * Returns the <a href="http://en.wikipedia.org/wiki/Filename_extension">file extension</a> for
   * the given file name, or the empty string if the file has no extension. The result does not
   * include the '{@code .}'.
   *
   * <p><b>Note:</b> This method simply returns everything after the last '{@code .}' in the file's
   * name as determined by {@link File#getName}. It does not account for any filesystem-specific
   * behavior that the {@link File} API does not already account for. For example, on NTFS it will
   * report {@code "txt"} as the extension for the filename {@code "foo.exe:.txt"} even though NTFS
   * will drop the {@code ":.txt"} part of the name when the file is actually created on the
   * filesystem due to NTFS's <a href="https://goo.gl/vTpJi4">Alternate Data Streams</a>.
   *
   * @since 11.0
   */
  public static String getFileExtension(String fullName) {
    checkNotNull(fullName);
    String fileName = new File(fullName).getName();
    int dotIndex = fileName.lastIndexOf('.');
    return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
  }

  /**
   * Returns the file name without its <a
   * href="http://en.wikipedia.org/wiki/Filename_extension">file extension</a> or path. This is
   * similar to the {@code basename} unix command. The result does not include the '{@code .}'.
   *
   * @param file The name of the file to trim the extension from. This can be either a fully
   *     qualified file name (including a path) or just a file name.
   * @return The file name without its path or extension.
   * @since 14.0
   */
  public static String getNameWithoutExtension(String file) {
    checkNotNull(file);
    String fileName = new File(file).getName();
    int dotIndex = fileName.lastIndexOf('.');
    return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
  }

  /**
   * Returns a {@link Traverser} instance for the file and directory tree. The returned traverser
   * starts from a {@link File} and will return all files and directories it encounters.
   *
   * <p><b>Warning:</b> {@code File} provides no support for symbolic links, and as such there is no
   * way to ensure that a symbolic link to a directory is not followed when traversing the tree. In
   * this case, iterables created by this traverser could contain files that are outside of the
   * given directory or even be infinite if there is a symbolic link loop.
   *
   * <p>If available, consider using {@link MoreFiles#fileTraverser()} instead. It behaves the same
   * except that it doesn't follow symbolic links and returns {@code Path} instances.
   *
   * <p>If the {@link File} passed to one of the {@link Traverser} methods does not exist or is not
   * a directory, no exception will be thrown and the returned {@link Iterable} will contain a
   * single element: that file.
   *
   * <p>Example: {@code Files.fileTraverser().depthFirstPreOrder(new File("/"))} may return files
   * with the following paths: {@code ["/", "/etc", "/etc/config.txt", "/etc/fonts", "/home",
   * "/home/alice", ...]}
   *
   * @since 23.5
   */
  @Beta
  public static Traverser<File> fileTraverser() {
    return Traverser.forTree(FILE_TREE);
  }

  private static final SuccessorsFunction<File> FILE_TREE =
      new SuccessorsFunction<File>() {
        @Override
        public Iterable<File> successors(File file) {
          // check isDirectory() just because it may be faster than listFiles() on a non-directory
          if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
              return Collections.unmodifiableList(Arrays.asList(files));
            }
          }

          return ImmutableList.of();
        }
      };

  /**
   * Returns a predicate that returns the result of {@link File#isDirectory} on input files.
   *
   * @since 15.0
   */
  public static Predicate<File> isDirectory() {
    return FilePredicate.IS_DIRECTORY;
  }

  /**
   * Returns a predicate that returns the result of {@link File#isFile} on input files.
   *
   * @since 15.0
   */
  public static Predicate<File> isFile() {
    return FilePredicate.IS_FILE;
  }

  private enum FilePredicate implements Predicate<File> {
    IS_DIRECTORY {
      @Override
      public boolean apply(File file) {
        return file.isDirectory();
      }

      @Override
      public String toString() {
        return "Files.isDirectory()";
      }
    },

    IS_FILE {
      @Override
      public boolean apply(File file) {
        return file.isFile();
      }

      @Override
      public String toString() {
        return "Files.isFile()";
      }
    }
  }
}
