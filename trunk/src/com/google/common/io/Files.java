/*
 * Copyright (C) 2007 Google Inc.
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

package com.google.common.io;

import com.google.common.base.Preconditions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.List;
import java.util.zip.Checksum;

/**
 * Provides utility methods for working with files.
 *
 * <p>All method parameters must be non-null unless documented otherwise.
 *
 * @author Chris Nokleberg
 * @since 2009.09.15 <b>tentative</b>
 */
public final class Files {

  /** Maximum loop count when creating temp directories. */
  private static final int TEMP_DIR_ATTEMPTS = 10000;

  private Files() {}

  /**
   * Returns a buffered reader that reads from a file using the given
   * character set.
   *
   * @param file the file to read from
   * @param charset the character set used when writing the file
   * @return the buffered reader
   */
  public static BufferedReader newReader(File file, Charset charset)
      throws FileNotFoundException {
    return new BufferedReader(
        new InputStreamReader(new FileInputStream(file), charset));
  }

  /**
   * Returns a buffered writer that writes to a file using the given
   * character set.
   *
   * @param file the file to write to
   * @param charset the character set used when writing the file
   * @return the buffered writer
   */
  public static BufferedWriter newWriter(File file, Charset charset)
      throws FileNotFoundException {
   return new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(file), charset));
  }

  /**
   * Returns a factory that will supply instances of {@link FileInputStream}
   * that read from a file.
   *
   * @param file the file to read from
   * @return the factory
   */
  public static InputSupplier<FileInputStream> newInputStreamSupplier(
      final File file) {
    Preconditions.checkNotNull(file);
    return new InputSupplier<FileInputStream>() {
      public FileInputStream getInput() throws IOException {
        return new FileInputStream(file);
      }
    };
  }

  /**
   * Returns a factory that will supply instances of {@link FileOutputStream}
   * that write to a file.
   *
   * @param file the file to write to
   * @return the factory
   */
  public static OutputSupplier<FileOutputStream> newOutputStreamSupplier(
      File file) {
    return newOutputStreamSupplier(file, false);
  }

  /**
   * Returns a factory that will supply instances of {@link FileOutputStream}
   * that write to or append to a file.
   *
   * @param file the file to write to
   * @param append if true, the encoded characters will be appended to the file;
   *     otherwise the file is overwritten
   * @return the factory
   */
  public static OutputSupplier<FileOutputStream> newOutputStreamSupplier(
      final File file, final boolean append) {
    Preconditions.checkNotNull(file);
    return new OutputSupplier<FileOutputStream>() {
      public FileOutputStream getOutput() throws IOException {
        return new FileOutputStream(file, append);
      }
    };
  }

  /**
   * Returns a factory that will supply instances of
   * {@link InputStreamReader} that read a file using the given character set.
   *
   * @param file the file to read from
   * @param charset the character set used when reading the file
   * @return the factory
   */
  public static InputSupplier<InputStreamReader> newReaderSupplier(File file,
      Charset charset) {
    return CharStreams.newReaderSupplier(newInputStreamSupplier(file), charset);
  }

  /**
   * Returns a factory that will supply instances of {@link OutputStreamWriter}
   * that write to a file using the given character set.
   *
   * @param file the file to write to
   * @param charset the character set used when writing the file
   * @return the factory
   */
  public static OutputSupplier<OutputStreamWriter> newWriterSupplier(File file,
      Charset charset) {
    return newWriterSupplier(file, charset, false);
  }

  /**
   * Returns a factory that will supply instances of {@link OutputStreamWriter}
   * that write to or append to a file using the given character set.
   *
   * @param file the file to write to
   * @param charset the character set used when writing the file
   * @param append if true, the encoded characters will be appended to the file;
   *     otherwise the file is overwritten
   * @return the factory
   */
  public static OutputSupplier<OutputStreamWriter> newWriterSupplier(File file,
      Charset charset, boolean append) {
    return CharStreams.newWriterSupplier(newOutputStreamSupplier(file, append),
        charset);
  }

  /**
   * Reads all bytes from a file into a byte array.
   *
   * @param file the file to read from
   * @return a byte array containing all the bytes from file
   * @throws IllegalArgumentException if the file is bigger than the largest
   *     possible byte array (2^31 - 1)
   * @throws IOException if an I/O error occurs
   */
  public static byte[] toByteArray(File file) throws IOException {
    Preconditions.checkArgument(file.length() <= Integer.MAX_VALUE);
    if (file.length() == 0) {
      // Some special files are length 0 but have content nonetheless.
      return ByteStreams.toByteArray(newInputStreamSupplier(file));
    } else {
      // Avoid an extra allocation and copy.
      byte[] b = new byte[(int) file.length()];
      boolean threw = true;
      InputStream in = new FileInputStream(file);
      try {
        ByteStreams.readFully(in, b);
        threw = false;
      } finally {
        Closeables.close(in, threw);
      }
      return b;
    }
  }

  /**
   * Reads all characters from a file into a {@link String}, using the given
   * character set.
   *
   * @param file the file to read from
   * @param charset the character set used when reading the file
   * @return a string containing all the characters from the file
   * @throws IOException if an I/O error occurs
   */
  public static String toString(File file, Charset charset) throws IOException {
    return new String(toByteArray(file), charset.name());
  }

  /**
   * Copies to a file all bytes from an {@link InputStream} supplied by a
   * factory.
   *
   * @param from the input factory
   * @param to the destination file
   * @throws IOException if an I/O error occurs
   */
  public static void copy(InputSupplier<? extends InputStream> from, File to)
      throws IOException {
    ByteStreams.copy(from, newOutputStreamSupplier(to));
  }

  /**
   * Overwrites a file with the contents of a byte array.
   *
   * @param from the bytes to write
   * @param to the destination file
   * @throws IOException if an I/O error occurs
   */
  public static void write(byte[] from, File to) throws IOException {
    ByteStreams.write(from, newOutputStreamSupplier(to));
  }

  /**
   * Copies all bytes from a file to an {@link OutputStream} supplied by
   * a factory.
   *
   * @param from the source file
   * @param to the output factory
   * @throws IOException if an I/O error occurs
   */
  public static void copy(File from, OutputSupplier<? extends OutputStream> to)
      throws IOException {
    ByteStreams.copy(newInputStreamSupplier(from), to);
  }

  /**
   * Copies all bytes from a file to an output stream.
   *
   * @param from the source file
   * @param to the output stream
   * @throws IOException if an I/O error occurs
   */
  public static void copy(File from, OutputStream to) throws IOException {
    ByteStreams.copy(newInputStreamSupplier(from), to);
  }

  /**
   * Copies all the bytes from one file to another.
   *.
   * @param from the source file
   * @param to the destination file
   * @throws IOException if an I/O error occurs
   */
  public static void copy(File from, File to) throws IOException {
    copy(newInputStreamSupplier(from), to);
  }

  /**
   * Copies to a file all characters from a {@link Readable} and
   * {@link Closeable} object supplied by a factory, using the given
   * character set.
   *
   * @param from the readable supplier
   * @param to the destination file
   * @param charset the character set used when writing the file
   * @throws IOException if an I/O error occurs
   */
  public static <R extends Readable & Closeable> void copy(
      InputSupplier<R> from, File to, Charset charset) throws IOException {
    CharStreams.copy(from, newWriterSupplier(to, charset));
  }

  /**
   * Writes a character sequence (such as a string) to a file using the given
   * character set.
   *
   * @param from the character sequence to write
   * @param to the destination file
   * @param charset the character set used when writing the file
   * @throws IOException if an I/O error occurs
   */
  public static void write(CharSequence from, File to, Charset charset)
      throws IOException {
    write(from, to, charset, false);
  }

  /**
   * Appends a character sequence (such as a string) to a file using the given
   * character set.
   *
   * @param from the character sequence to append
   * @param to the destination file
   * @param charset the character set used when writing the file
   * @throws IOException if an I/O error occurs
   */
  public static void append(CharSequence from, File to, Charset charset)
      throws IOException {
    write(from, to, charset, true);
  }

  /**
   * Private helper method. Writes a character sequence to a file,
   * optionally appending.
   *
   * @param from the character sequence to append
   * @param to the destination file
   * @param charset the character set used when writing the file
   * @param append true to append, false to overwrite
   * @throws IOException if an I/O error occurs
   */
  private static void write(CharSequence from, File to, Charset charset,
      boolean append) throws IOException {
    CharStreams.write(from, newWriterSupplier(to, charset, append));
  }

  /**
   * Copies all characters from a file to a {@link Appendable} &
   * {@link Closeable} object supplied by a factory, using the given
   * character set.
   *
   * @param from the source file
   * @param charset the character set used when reading the file
   * @param to the appendable supplier
   * @throws IOException if an I/O error occurs
   */
  public static <W extends Appendable & Closeable> void copy(File from,
      Charset charset, OutputSupplier<W> to) throws IOException {
    CharStreams.copy(newReaderSupplier(from, charset), to);
  }

  /**
   * Copies all characters from a file to an appendable object,
   * using the given character set.
   *
   * @param from the source file
   * @param charset the character set used when reading the file
   * @param to the appendable object
   * @throws IOException if an I/O error occurs
   */
  public static void copy(File from, Charset charset, Appendable to)
      throws IOException {
    CharStreams.copy(newReaderSupplier(from, charset), to);
  }

  /**
   * Returns true if the files contains the same bytes.
   *
   * @throws IOException if an I/O error occurs
   */
  public static boolean equal(File file1, File file2) throws IOException {
    if (file1 == file2 || file1.equals(file2)) {
      return true;
    }

    /*
     * Some operating systems may return zero as the length for files
     * denoting system-dependent entities such as devices or pipes, in
     * which case we must fall back on comparing the bytes directly.
     */
    long len1 = file1.length();
    long len2 = file2.length();
    if (len1 != 0 && len2 != 0 && len1 != len2) {
      return false;
    }
    return ByteStreams.equal(newInputStreamSupplier(file1),
        newInputStreamSupplier(file2));
  }

  /**
   * Atomically creates a new directory somewhere beneath the system's
   * temporary directory (as defined by the {@code java.io.tmpdir} system
   * property), and returns its name.
   *
   * <p>Use this method instead of {@link File#createTempFile(String, String)}
   * when you wish to create a directory, not a regular file.  A common pitfall
   * is to call {@code createTempFile}, delete the file and create a
   * directory in its place, but this leads a race condition which can be
   * exploited to create security vulnerabilities, especially when executable
   * files are to be written into the directory.
   *
   * <p>This method assumes that the temporary volume is writable, has free
   * inodes and free blocks, and that it will not be called thousands of times
   * per second.
   *
   * @return the newly-created directory
   * @throws IllegalStateException if the directory could not be created
   */
  public static File createTempDir() {
    File baseDir = new File(System.getProperty("java.io.tmpdir"));
    String baseName = System.currentTimeMillis() + "-";

    for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
      File tempDir = new File(baseDir, baseName + counter);
      if (tempDir.mkdir()) {
        return tempDir;
      }
    }
    throw new IllegalStateException("Failed to create directory within "
        + TEMP_DIR_ATTEMPTS + " attempts (tried "
        + baseName + "0 to " + baseName + (TEMP_DIR_ATTEMPTS - 1) + ')');
  }

  /**
   * Creates an empty file or updates the last updated timestamp on the
   * same as the unix command of the same name.
   *
   * @param file the file to create or update
   * @throws IOException if an I/O error occurs
   */
  public static void touch(File file) throws IOException {
    if (!file.createNewFile()
        && !file.setLastModified(System.currentTimeMillis())) {
      throw new IOException("Unable to update modification time of " + file);
    }
  }

  /**
   * Moves the file from one path to another. This method can rename a file or
   * move it to a different directory, like the Unix {@code mv} command.
   *
   * @param from the source file
   * @param to the destination file
   * @throws IOException if an I/O error occurs
   */
  public static void move(File from, File to) throws IOException {
    Preconditions.checkNotNull(to);
    Preconditions.checkArgument(!from.equals(to),
        "Source %s and destination %s must be different", from, to);

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
   * Deletes all the files within a directory. Does not delete the
   * directory itself.
   *
   * <p>If the file argument is a symbolic link or there is a symbolic
   * link in the path leading to the directory, this method will do
   * nothing. Symbolic links within the directory are not followed.
   *
   * @param directory the directory to delete the contents of
   * @throws IllegalArgumentException if the argument is not a directory
   * @throws IOException if an I/O error occurs
   * @see #deleteRecursively
   */
  public static void deleteDirectoryContents(File directory)
      throws IOException {
    Preconditions.checkArgument(directory.isDirectory(),
        "Not a directory: %s", directory);
    // Symbolic links will have different canonical and absolute paths
    if (!directory.getCanonicalPath().equals(directory.getAbsolutePath())) {
      return;
    }
    File[] files = directory.listFiles();
    if (files == null) {
      throw new IOException("Error listing files for " + directory);
    }
    for (File file : files) {
      deleteRecursively(file);
    }
  }

  /**
   * Deletes a file or directory and all contents recursively.
   *
   * <p>If the file argument is a symbolic link the link will be deleted
   * but not the target of the link. If the argument is a directory,
   * symbolic links within the directory will not be followed.
   *
   * @param file the file to delete
   * @throws IOException if an I/O error occurs
   * @see #deleteDirectoryContents
   */
  public static void deleteRecursively(File file) throws IOException {
    if (file.isDirectory()) {
      deleteDirectoryContents(file);
    }
    if (!file.delete()) {
      throw new IOException("Failed to delete " + file);
    }
  }

  /**
   * Reads the first line from a file. The line does not include
   * line-termination characters, but does include other leading and
   * trailing whitespace.
   *
   * @param file the file to read from
   * @param charset the character set used when writing the file
   * @return the first line, or null if the file is empty
   * @throws IOException if an I/O error occurs
   */
  public static String readFirstLine(File file, Charset charset)
      throws IOException {
    return CharStreams.readFirstLine(Files.newReaderSupplier(file, charset));
  }

  /**
   * Reads all of the lines from a file. The lines do not include
   * line-termination characters, but do include other leading and
   * trailing whitespace.
   *
   * @param file the file to read from
   * @param charset the character set used when writing the file
   * @return a mutable {@link List} containing all the lines
   * @throws IOException if an I/O error occurs
   */
  public static List<String> readLines(File file, Charset charset)
      throws IOException {
    return CharStreams.readLines(Files.newReaderSupplier(file, charset));
  }

  /**
   * Streams lines from a {@link File}, stopping when our callback returns
   * false, or we have read all of the lines.
   *
   * @param file the file to read from
   * @param charset the character set used when writing the file
   * @param callback the {@link LineProcessor} to use to handle the lines
   * @return the output of processing the lines
   * @throws IOException if an I/O error occurs
   */
  public static <T> T readLines(File file, Charset charset,
      LineProcessor<T> callback) throws IOException {
    return CharStreams.readLines(Files.newReaderSupplier(file, charset),
        callback);
  }

  /**
   * Process the bytes of a file.
   *
   * <p>(If this seems too complicated, maybe you're looking for
   * {@link #toByteArray}.)
   *
   * @param file the file to read
   * @param processor the object to which the bytes of the file are passed.
   * @return the result of the byte processor
   * @throws IOException if an I/O error occurs
   */
  public static <T> T readBytes(File file, ByteProcessor<T> processor)
      throws IOException {
    return ByteStreams.readBytes(newInputStreamSupplier(file), processor);
  }

  /**
   * Computes and returns the checksum value for a file.
   * The checksum object is reset when this method returns successfully.
   *
   * @param file the file to read
   * @param checksum the checksum object
   * @return the result of {@link Checksum#getValue} after updating the
   *     checksum object with all of the bytes in the file
   * @throws IOException if an I/O error occurs
   */
  public static long getChecksum(File file, Checksum checksum)
      throws IOException {
    return ByteStreams.getChecksum(newInputStreamSupplier(file), checksum);
  }

  /**
   * Computes and returns the digest value for a file.
   * The digest object is reset when this method returns successfully.
   *
   * @param file the file to read
   * @param md the digest object
   * @return the result of {@link MessageDigest#digest()} after updating the
   *     digest object with all of the bytes in this file
   * @throws IOException if an I/O error occurs
   */
  public static byte[] getDigest(File file, MessageDigest md)
      throws IOException {
    return ByteStreams.getDigest(newInputStreamSupplier(file), md);
  }

  /**
   * Fully maps a file read-only in to memory as per
   * {@link FileChannel#map(java.nio.channels.FileChannel.MapMode, long, long)}.
   *
   * <p>Files are mapped from offset 0 to its length.
   *
   * <p>This only works for files <= {@link Integer#MAX_VALUE} bytes.
   *
   * @param file the file to map
   * @return a read-only buffer reflecting {@code file}
   * @throws FileNotFoundException if the {@code file} does not exist
   * @throws IOException if an I/O error occurs
   *
   * @see FileChannel#map(MapMode, long, long)
   * @since 2010.01.04 <b>tentative</b>
   */
  public static MappedByteBuffer map(File file) throws IOException {
    return map(file, MapMode.READ_ONLY);
  }

  /**
   * Fully maps a file in to memory as per
   * {@link FileChannel#map(java.nio.channels.FileChannel.MapMode, long, long)}
   * using the requested {@link MapMode}.
   *
   * <p>Files are mapped from offset 0 to its length.
   *
   * <p>This only works for files <= {@link Integer#MAX_VALUE} bytes.
   *
   * @param file the file to map
   * @param mode the mode to use when mapping {@code file}
   * @return a buffer reflecting {@code file}
   * @throws FileNotFoundException if the {@code file} does not exist
   * @throws IOException if an I/O error occurs
   *
   * @see FileChannel#map(MapMode, long, long)
   * @since 2010.01.04 <b>tentative</b>
   */
  public static MappedByteBuffer map(File file, MapMode mode)
      throws IOException {
    if (!file.exists()) {
      throw new FileNotFoundException(file.toString());
    }
    return map(file, mode, file.length());
  }

  /**
   * Maps a file in to memory as per
   * {@link FileChannel#map(java.nio.channels.FileChannel.MapMode, long, long)}
   * using the requested {@link MapMode}.
   *
   * <p>Files are mapped from offset 0 to {@code size}.
   *
   * <p>If the mode is {@link MapMode#READ_WRITE} and the file does not exist,
   * it will be created with the requested {@code size}. Thus this method is
   * useful for creating memory mapped files which do not yet exist.
   *
   * <p>This only works for files <= {@link Integer#MAX_VALUE} bytes.
   *
   * @param file the file to map
   * @param mode the mode to use when mapping {@code file}
   * @return a buffer reflecting {@code file}
   * @throws IOException if an I/O error occurs
   *
   * @see FileChannel#map(MapMode, long, long)
   * @since 2010.01.04 <b>tentative</b>
   */
  public static MappedByteBuffer map(File file, MapMode mode, long size)
      throws FileNotFoundException, IOException {
    RandomAccessFile raf =
        new RandomAccessFile(file, mode == MapMode.READ_ONLY ? "r" : "rw");

    boolean threw = true;
    try {
      MappedByteBuffer mbb = map(raf, mode, size);
      threw = false;
      return mbb;
    } finally {
      Closeables.close(raf, threw);
    }
  }

  private static MappedByteBuffer map(RandomAccessFile raf, MapMode mode,
      long size) throws IOException {
    FileChannel channel = raf.getChannel();

    boolean threw = true;
    try {
      MappedByteBuffer mbb = channel.map(mode, 0, size);
      threw = false;
      return mbb;
    } finally {
      Closeables.close(channel, threw);
    }
  }
}
