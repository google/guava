/*
 * Copyright (C) 2007 The Guava Authors
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

import static com.google.common.hash.Hashing.sha512;
import static com.google.common.primitives.Bytes.asList;
import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_16LE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import junit.framework.TestSuite;
import org.jspecify.annotations.NullUnmarked;

/**
 * Unit test for {@link Files}.
 *
 * <p>Some methods are tested in separate files:
 *
 * <ul>
 *   <li>{@link Files#fileTraverser()} is tested in {@link FilesFileTraverserTest}.
 *   <li>{@link Files#createTempDir()} is tested in {@link FilesCreateTempDirTest}.
 * </ul>
 *
 * @author Chris Nokleberg
 */

@SuppressWarnings("InlineMeInliner") // many tests of deprecated methods
@NullUnmarked
public class FilesTest extends IoTestCase {

  @AndroidIncompatible // suites, ByteSourceTester (b/230620681)
  public static TestSuite suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(
        ByteSourceTester.tests(
            "Files.asByteSource[File]", SourceSinkFactories.fileByteSourceFactory(), true));
    suite.addTest(
        ByteSinkTester.tests("Files.asByteSink[File]", SourceSinkFactories.fileByteSinkFactory()));
    suite.addTest(
        ByteSinkTester.tests(
            "Files.asByteSink[File, APPEND]", SourceSinkFactories.appendingFileByteSinkFactory()));
    suite.addTest(
        CharSourceTester.tests(
            "Files.asCharSource[File, Charset]",
            SourceSinkFactories.fileCharSourceFactory(),
            false));
    suite.addTest(
        CharSinkTester.tests(
            "Files.asCharSink[File, Charset]", SourceSinkFactories.fileCharSinkFactory()));
    suite.addTest(
        CharSinkTester.tests(
            "Files.asCharSink[File, Charset, APPEND]",
            SourceSinkFactories.appendingFileCharSinkFactory()));
    suite.addTestSuite(FilesTest.class);
    return suite;
  }

  public void testRoundTripSources() throws Exception {
    File asciiFile = getTestFile("ascii.txt");
    ByteSource byteSource = Files.asByteSource(asciiFile);
    assertThat(byteSource.asCharSource(UTF_8).asByteSource(UTF_8)).isSameInstanceAs(byteSource);
  }

  public void testToByteArray() throws IOException {
    File asciiFile = getTestFile("ascii.txt");
    File i18nFile = getTestFile("i18n.txt");
    assertThat(Files.toByteArray(asciiFile)).isEqualTo(ASCII.getBytes(US_ASCII));
    assertThat(Files.toByteArray(i18nFile)).isEqualTo(I18N.getBytes(UTF_8));
    assertThat(Files.asByteSource(i18nFile).read()).isEqualTo(I18N.getBytes(UTF_8));
  }

  /** A {@link File} that provides a specialized value for {@link File#length()}. */
  private static class BadLengthFile extends File {

    private final long badLength;

    BadLengthFile(File delegate, long badLength) {
      super(delegate.getPath());
      this.badLength = badLength;
    }

    @Override
    public long length() {
      return badLength;
    }

    private static final long serialVersionUID = 0;
  }

  public void testToString() throws IOException {
    File asciiFile = getTestFile("ascii.txt");
    File i18nFile = getTestFile("i18n.txt");
    assertThat(Files.toString(asciiFile, US_ASCII)).isEqualTo(ASCII);
    assertThat(Files.toString(i18nFile, UTF_8)).isEqualTo(I18N);
    assertThat(Files.toString(i18nFile, US_ASCII)).isNotEqualTo(I18N);
  }

  public void testWriteString() throws IOException {
    File temp = createTempFile();
    Files.write(I18N, temp, UTF_16LE);
    assertThat(Files.toString(temp, UTF_16LE)).isEqualTo(I18N);
  }

  public void testWriteBytes() throws IOException {
    File temp = createTempFile();
    byte[] data = newPreFilledByteArray(2000);
    Files.write(data, temp);
    assertThat(Files.toByteArray(temp)).isEqualTo(data);

    assertThrows(NullPointerException.class, () -> Files.write(null, temp));
  }

  public void testAppendString() throws IOException {
    File temp = createTempFile();
    Files.append(I18N, temp, UTF_16LE);
    assertThat(Files.toString(temp, UTF_16LE)).isEqualTo(I18N);
    Files.append(I18N, temp, UTF_16LE);
    assertThat(Files.toString(temp, UTF_16LE)).isEqualTo(I18N + I18N);
    Files.append(I18N, temp, UTF_16LE);
    assertThat(Files.toString(temp, UTF_16LE)).isEqualTo(I18N + I18N + I18N);
  }

  @SuppressWarnings({"PreferCharsetOverload", "JdkObsolete"}) // requires Java 10 / API Level 33
  public void testCopyToOutputStream() throws IOException {
    File i18nFile = getTestFile("i18n.txt");
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Files.copy(i18nFile, out);
    assertThat(out.toString("UTF-8")).isEqualTo(I18N);
  }

  public void testCopyToAppendable() throws IOException {
    File i18nFile = getTestFile("i18n.txt");
    StringBuilder sb = new StringBuilder();
    Files.copy(i18nFile, UTF_8, sb);
    assertThat(sb.toString()).isEqualTo(I18N);
  }

  public void testCopyFile() throws IOException {
    File i18nFile = getTestFile("i18n.txt");
    File temp = createTempFile();
    Files.copy(i18nFile, temp);
    assertThat(Files.toString(temp, UTF_8)).isEqualTo(I18N);
  }

  public void testCopyEqualFiles() throws IOException {
    File temp1 = createTempFile();
    File temp2 = file(temp1.getPath());
    assertEquals(temp1, temp2);
    Files.write(ASCII, temp1, UTF_8);
    assertThrows(IllegalArgumentException.class, () -> Files.copy(temp1, temp2));
    assertThat(Files.toString(temp1, UTF_8)).isEqualTo(ASCII);
  }

  public void testCopySameFile() throws IOException {
    File temp = createTempFile();
    Files.write(ASCII, temp, UTF_8);
    assertThrows(IllegalArgumentException.class, () -> Files.copy(temp, temp));
    assertThat(Files.toString(temp, UTF_8)).isEqualTo(ASCII);
  }

  public void testCopyIdenticalFiles() throws IOException {
    File temp1 = createTempFile();
    Files.write(ASCII, temp1, UTF_8);
    File temp2 = createTempFile();
    Files.write(ASCII, temp2, UTF_8);
    Files.copy(temp1, temp2);
    assertThat(Files.toString(temp2, UTF_8)).isEqualTo(ASCII);
  }

  public void testEqual() throws IOException {
    File asciiFile = getTestFile("ascii.txt");
    File i18nFile = getTestFile("i18n.txt");
    assertFalse(Files.equal(asciiFile, i18nFile));
    assertTrue(Files.equal(asciiFile, asciiFile));

    File temp = createTempFile();
    Files.copy(asciiFile, temp);
    assertTrue(Files.equal(asciiFile, temp));

    Files.copy(i18nFile, temp);
    assertTrue(Files.equal(i18nFile, temp));

    Files.copy(asciiFile, temp);
    RandomAccessFile rf = new RandomAccessFile(temp, "rw");
    rf.writeByte(0);
    rf.close();
    assertEquals(asciiFile.length(), temp.length());
    assertFalse(Files.equal(asciiFile, temp));

    assertTrue(Files.asByteSource(asciiFile).contentEquals(Files.asByteSource(asciiFile)));

    // 0-length files have special treatment (/proc, etc.)
    assertTrue(Files.equal(asciiFile, new BadLengthFile(asciiFile, 0)));
  }

  public void testNewReader() throws IOException {
    File asciiFile = getTestFile("ascii.txt");
    assertThrows(NullPointerException.class, () -> Files.newReader(asciiFile, null));

    assertThrows(NullPointerException.class, () -> Files.newReader(null, UTF_8));

    BufferedReader r = Files.newReader(asciiFile, US_ASCII);
    try {
      assertThat(r.readLine()).isEqualTo(ASCII);
    } finally {
      r.close();
    }
  }

  public void testNewWriter() throws IOException {
    File temp = createTempFile();
    assertThrows(NullPointerException.class, () -> Files.newWriter(temp, null));

    assertThrows(NullPointerException.class, () -> Files.newWriter(null, UTF_8));

    BufferedWriter w = Files.newWriter(temp, UTF_8);
    try {
      w.write(I18N);
    } finally {
      w.close();
    }

    File i18nFile = getTestFile("i18n.txt");
    assertTrue(Files.equal(i18nFile, temp));
  }

  public void testTouch() throws IOException {
    File temp = createTempFile();
    assertTrue(temp.exists());
    assertTrue(temp.delete());
    assertFalse(temp.exists());
    Files.touch(temp);
    assertTrue(temp.exists());
    Files.touch(temp);
    assertTrue(temp.exists());

    assertThrows(
        IOException.class,
        () ->
            Files.touch(
                new File(temp.getPath()) {
                  @Override
                  public boolean setLastModified(long t) {
                    return false;
                  }

                  private static final long serialVersionUID = 0;
                }));
  }

  public void testTouchTime() throws IOException {
    File temp = createTempFile();
    assertTrue(temp.exists());
    temp.setLastModified(0);
    assertEquals(0, temp.lastModified());
    Files.touch(temp);
    assertThat(temp.lastModified()).isNotEqualTo(0);
  }

  public void testCreateParentDirs_root() throws IOException {
    File file = root();
    assertThat(file.getParentFile()).isNull();
    assertThat(file.getCanonicalFile().getParentFile()).isNull();
    Files.createParentDirs(file);
  }

  public void testCreateParentDirs_relativePath() throws IOException {
    File file = file("nonexistent.file");
    assertThat(file.getParentFile()).isNull();
    assertThat(file.getCanonicalFile().getParentFile()).isNotNull();
    Files.createParentDirs(file);
  }

  public void testCreateParentDirs_noParentsNeeded() throws IOException {
    File file = file(getTempDir(), "nonexistent.file");
    assertTrue(file.getParentFile().exists());
    Files.createParentDirs(file);
  }

  public void testCreateParentDirs_oneParentNeeded() throws IOException {
    File file = file(getTempDir(), "parent", "nonexistent.file");
    File parent = file.getParentFile();
    assertFalse(parent.exists());
    try {
      Files.createParentDirs(file);
      assertTrue(parent.exists());
    } finally {
      assertTrue(parent.delete());
    }
  }

  public void testCreateParentDirs_multipleParentsNeeded() throws IOException {
    File file = file(getTempDir(), "grandparent", "parent", "nonexistent.file");
    File parent = file.getParentFile();
    File grandparent = parent.getParentFile();
    assertFalse(grandparent.exists());
    Files.createParentDirs(file);
    assertTrue(parent.exists());
  }

  public void testCreateParentDirs_nonDirectoryParentExists() throws IOException {
    File parent = getTestFile("ascii.txt");
    assertTrue(parent.isFile());
    File file = file(parent, "foo");
    assertThrows(IOException.class, () -> Files.createParentDirs(file));
  }

  public void testMove() throws IOException {
    File i18nFile = getTestFile("i18n.txt");
    File temp1 = createTempFile();
    File temp2 = createTempFile();

    Files.copy(i18nFile, temp1);
    moveHelper(true, temp1, temp2);
    assertTrue(Files.equal(temp2, i18nFile));
  }

  public void testMoveViaCopy() throws IOException {
    File i18nFile = getTestFile("i18n.txt");
    File temp1 = createTempFile();
    File temp2 = createTempFile();

    Files.copy(i18nFile, temp1);
    moveHelper(true, new UnmovableFile(temp1, false, true), temp2);
    assertTrue(Files.equal(temp2, i18nFile));
  }

  public void testMoveFailures() throws IOException {
    File temp1 = createTempFile();
    File temp2 = createTempFile();

    moveHelper(false, new UnmovableFile(temp1, false, false), temp2);
    moveHelper(
        false, new UnmovableFile(temp1, false, false), new UnmovableFile(temp2, true, false));

    File asciiFile = getTestFile("ascii.txt");
    assertThrows(IllegalArgumentException.class, () -> moveHelper(false, asciiFile, asciiFile));
  }

  private void moveHelper(boolean success, File from, File to) throws IOException {
    try {
      Files.move(from, to);
      if (success) {
        assertFalse(from.exists());
        assertTrue(to.exists());
      } else {
        fail("expected exception");
      }
    } catch (IOException possiblyExpected) {
      if (success) {
        throw possiblyExpected;
      }
    }
  }

  private static class UnmovableFile extends File {

    private final boolean canRename;
    private final boolean canDelete;

    UnmovableFile(File file, boolean canRename, boolean canDelete) {
      super(file.getPath());
      this.canRename = canRename;
      this.canDelete = canDelete;
    }

    @Override
    public boolean renameTo(File to) {
      return canRename && super.renameTo(to);
    }

    @Override
    public boolean delete() {
      return canDelete && super.delete();
    }

    private static final long serialVersionUID = 0;
  }

  public void testLineReading() throws IOException {
    File temp = createTempFile();
    assertThat(Files.readFirstLine(temp, UTF_8)).isNull();
    assertTrue(Files.readLines(temp, UTF_8).isEmpty());

    PrintWriter w = new PrintWriter(Files.newWriter(temp, UTF_8));
    w.println("hello");
    w.println("");
    w.println(" world  ");
    w.println("");
    w.close();

    assertThat(Files.readFirstLine(temp, UTF_8)).isEqualTo("hello");
    assertEquals(ImmutableList.of("hello", "", " world  ", ""), Files.readLines(temp, UTF_8));

    assertTrue(temp.delete());
  }

  public void testReadLines_withLineProcessor() throws IOException {
    File temp = createTempFile();
    LineProcessor<List<String>> collect =
        new LineProcessor<List<String>>() {
          final List<String> collector = new ArrayList<>();

          @Override
          public boolean processLine(String line) {
            collector.add(line);
            return true;
          }

          @Override
          public List<String> getResult() {
            return collector;
          }
        };
    assertThat(Files.readLines(temp, UTF_8, collect)).isEmpty();

    PrintWriter w = new PrintWriter(Files.newWriter(temp, UTF_8));
    w.println("hello");
    w.println("");
    w.println(" world  ");
    w.println("");
    w.close();
    Files.readLines(temp, UTF_8, collect);
    assertThat(collect.getResult()).containsExactly("hello", "", " world  ", "").inOrder();

    LineProcessor<List<String>> collectNonEmptyLines =
        new LineProcessor<List<String>>() {
          final List<String> collector = new ArrayList<>();

          @Override
          public boolean processLine(String line) {
            if (line.length() > 0) {
              collector.add(line);
            }
            return true;
          }

          @Override
          public List<String> getResult() {
            return collector;
          }
        };
    Files.readLines(temp, UTF_8, collectNonEmptyLines);
    assertThat(collectNonEmptyLines.getResult()).containsExactly("hello", " world  ").inOrder();

    assertTrue(temp.delete());
  }

  public void testHash() throws IOException {
    File asciiFile = getTestFile("ascii.txt");
    File i18nFile = getTestFile("i18n.txt");

    String init =
        "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e";
    assertThat(sha512().newHasher().hash().toString()).isEqualTo(init);

    String asciiHash =
        "65d4ba90de6e2733e2364c8baf514b557d18fdda9c8227c68c2e7a7190d8888b4919f84ce412be8fa548298d36fe1f762b38c562bb147e04835e3a52f251ae34";
    assertThat(Files.hash(asciiFile, sha512()).toString()).isEqualTo(asciiHash);

    String i18nHash =
        "4d64ba743dfc35ad6896fcb8674f7ccc040320a8413e086ddd65197e8a8a323559a9cb2d47137b83dd46deb44bbb14c42f9fde3b411411080ed7625828f8474e";
    assertThat(Files.hash(i18nFile, sha512()).toString()).isEqualTo(i18nHash);
  }

  public void testMap() throws IOException {
    // Test data
    int size = 1024;
    byte[] bytes = newPreFilledByteArray(size);

    // Setup
    File file = createTempFile();
    Files.write(bytes, file);

    // Test
    MappedByteBuffer actual = Files.map(file);

    // Verify
    ByteBuffer expected = ByteBuffer.wrap(bytes);
    assertTrue("ByteBuffers should be equal.", expected.equals(actual));
  }

  public void testMap_noSuchFile() throws IOException {
    // Setup
    File file = createTempFile();
    boolean deleted = file.delete();
    assertTrue(deleted);

    // Test
    assertThrows(FileNotFoundException.class, () -> Files.map(file));
  }

  public void testMap_readWrite() throws IOException {
    // Test data
    int size = 1024;
    byte[] expectedBytes = new byte[size];
    byte[] bytes = newPreFilledByteArray(1024);

    // Setup
    File file = createTempFile();
    Files.write(bytes, file);

    Random random = new Random();
    random.nextBytes(expectedBytes);

    // Test
    MappedByteBuffer map = Files.map(file, MapMode.READ_WRITE);
    map.put(expectedBytes);

    // Verify
    byte[] actualBytes = Files.toByteArray(file);
    assertThat(actualBytes).isEqualTo(expectedBytes);
  }

  public void testMap_readWrite_creates() throws IOException {
    // Test data
    int size = 1024;
    byte[] expectedBytes = newPreFilledByteArray(1024);

    // Setup
    File file = createTempFile();
    boolean deleted = file.delete();
    assertTrue(deleted);
    assertFalse(file.exists());

    // Test
    MappedByteBuffer map = Files.map(file, MapMode.READ_WRITE, size);
    map.put(expectedBytes);

    // Verify
    assertTrue(file.exists());
    assertTrue(file.isFile());
    assertEquals(size, file.length());
    byte[] actualBytes = Files.toByteArray(file);
    assertThat(actualBytes).isEqualTo(expectedBytes);
  }

  public void testMap_readWrite_max_value_plus_1() throws IOException {
    // Setup
    File file = createTempFile();
    // Test
    assertThrows(
        IllegalArgumentException.class,
        () -> Files.map(file, MapMode.READ_WRITE, (long) Integer.MAX_VALUE + 1));
  }

  public void testGetFileExtension() {
    assertThat(Files.getFileExtension(".txt")).isEqualTo("txt");
    assertThat(Files.getFileExtension("blah.txt")).isEqualTo("txt");
    assertThat(Files.getFileExtension("blah..txt")).isEqualTo("txt");
    assertThat(Files.getFileExtension(".blah.txt")).isEqualTo("txt");
    assertThat(Files.getFileExtension("/tmp/blah.txt")).isEqualTo("txt");
    assertThat(Files.getFileExtension("blah.tar.gz")).isEqualTo("gz");
    assertThat(Files.getFileExtension("/")).isEqualTo("");
    assertThat(Files.getFileExtension(".")).isEqualTo("");
    assertThat(Files.getFileExtension("..")).isEqualTo("");
    assertThat(Files.getFileExtension("...")).isEqualTo("");
    assertThat(Files.getFileExtension("blah")).isEqualTo("");
    assertThat(Files.getFileExtension("blah.")).isEqualTo("");
    assertThat(Files.getFileExtension(".blah.")).isEqualTo("");
    assertThat(Files.getFileExtension("/foo.bar/blah")).isEqualTo("");
    assertThat(Files.getFileExtension("/foo/.bar/blah")).isEqualTo("");
  }

  public void testGetNameWithoutExtension() {
    assertThat(Files.getNameWithoutExtension(".txt")).isEqualTo("");
    assertThat(Files.getNameWithoutExtension("blah.txt")).isEqualTo("blah");
    assertThat(Files.getNameWithoutExtension("blah..txt")).isEqualTo("blah.");
    assertThat(Files.getNameWithoutExtension(".blah.txt")).isEqualTo(".blah");
    assertThat(Files.getNameWithoutExtension("/tmp/blah.txt")).isEqualTo("blah");
    assertThat(Files.getNameWithoutExtension("blah.tar.gz")).isEqualTo("blah.tar");
    assertThat(Files.getNameWithoutExtension("/")).isEqualTo("");
    assertThat(Files.getNameWithoutExtension(".")).isEqualTo("");
    assertThat(Files.getNameWithoutExtension("..")).isEqualTo(".");
    assertThat(Files.getNameWithoutExtension("...")).isEqualTo("..");
    assertThat(Files.getNameWithoutExtension("blah")).isEqualTo("blah");
    assertThat(Files.getNameWithoutExtension("blah.")).isEqualTo("blah");
    assertThat(Files.getNameWithoutExtension(".blah.")).isEqualTo(".blah");
    assertThat(Files.getNameWithoutExtension("/foo.bar/blah")).isEqualTo("blah");
    assertThat(Files.getNameWithoutExtension("/foo/.bar/blah")).isEqualTo("blah");
  }

  public void testReadBytes() throws IOException {
    ByteProcessor<byte[]> processor =
        new ByteProcessor<byte[]>() {
          private final ByteArrayOutputStream out = new ByteArrayOutputStream();

          @Override
          public boolean processBytes(byte[] buffer, int offset, int length) {
            if (length >= 0) {
              out.write(buffer, offset, length);
            }
            return true;
          }

          @Override
          public byte[] getResult() {
            return out.toByteArray();
          }
        };

    File asciiFile = getTestFile("ascii.txt");
    byte[] result = Files.readBytes(asciiFile, processor);
    assertEquals(asList(Files.toByteArray(asciiFile)), asList(result));
  }

  public void testReadBytes_returnFalse() throws IOException {
    ByteProcessor<byte[]> processor =
        new ByteProcessor<byte[]>() {
          private final ByteArrayOutputStream out = new ByteArrayOutputStream();

          @Override
          public boolean processBytes(byte[] buffer, int offset, int length) {
            if (length > 0) {
              out.write(buffer, offset, 1);
              return false;
            } else {
              return true;
            }
          }

          @Override
          public byte[] getResult() {
            return out.toByteArray();
          }
        };

    File asciiFile = getTestFile("ascii.txt");
    byte[] result = Files.readBytes(asciiFile, processor);
    assertThat(result).hasLength(1);
  }

  public void testPredicates() throws IOException {
    File asciiFile = getTestFile("ascii.txt");
    File dir = asciiFile.getParentFile();
    assertTrue(Files.isDirectory().apply(dir));
    assertFalse(Files.isFile().apply(dir));

    assertFalse(Files.isDirectory().apply(asciiFile));
    assertTrue(Files.isFile().apply(asciiFile));
  }

  /** Returns a root path for the file system. */
  private static File root() {
    return File.listRoots()[0];
  }

  /** Returns a {@code File} object for the given path parts. */
  private static File file(String first, String... more) {
    return file(new File(first), more);
  }

  /** Returns a {@code File} object for the given path parts. */
  private static File file(File first, String... more) {
    // not very efficient, but should definitely be correct
    File file = first;
    for (String name : more) {
      file = new File(file, name);
    }
    return file;
  }
}
