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

package com.google.common.io;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.io.SourceSinkFactory.ByteSinkFactory;
import static com.google.common.io.SourceSinkFactory.ByteSourceFactory;
import static com.google.common.io.SourceSinkFactory.CharSinkFactory;
import static com.google.common.io.SourceSinkFactory.CharSourceFactory;

import com.google.common.base.Charsets;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.CharBuffer;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * {@link SourceSinkFactory} implementations.
 *
 * @author Colin Decker
 */
public class SourceSinkFactories {

  private SourceSinkFactories() {}

  public static CharSourceFactory stringCharSourceFactory() {
    return new StringSourceFactory();
  }

  public static ByteSourceFactory byteArraySourceFactory() {
    return new ByteArraySourceFactory();
  }

  public static ByteSourceFactory emptyByteSourceFactory() {
    return new EmptyByteSourceFactory();
  }

  public static CharSourceFactory emptyCharSourceFactory() {
    return new EmptyCharSourceFactory();
  }

  public static ByteSourceFactory fileByteSourceFactory() {
    return new FileByteSourceFactory();
  }

  public static ByteSinkFactory fileByteSinkFactory() {
    return new FileByteSinkFactory(null);
  }

  public static ByteSinkFactory appendingFileByteSinkFactory() {
    String initialString = IoTestCase.ASCII + IoTestCase.I18N;
    return new FileByteSinkFactory(initialString.getBytes(Charsets.UTF_8));
  }

  public static CharSourceFactory fileCharSourceFactory() {
    return new FileCharSourceFactory();
  }

  public static CharSinkFactory fileCharSinkFactory() {
    return new FileCharSinkFactory(null);
  }

  public static CharSinkFactory appendingFileCharSinkFactory() {
    String initialString = IoTestCase.ASCII + IoTestCase.I18N;
    return new FileCharSinkFactory(initialString);
  }

  public static ByteSourceFactory urlByteSourceFactory() {
    return new UrlByteSourceFactory();
  }

  public static CharSourceFactory urlCharSourceFactory() {
    return new UrlCharSourceFactory();
  }

  @AndroidIncompatible
  public static ByteSourceFactory pathByteSourceFactory() {
    return new PathByteSourceFactory();
  }

  @AndroidIncompatible
  public static ByteSinkFactory pathByteSinkFactory() {
    return new PathByteSinkFactory(null);
  }

  @AndroidIncompatible
  public static ByteSinkFactory appendingPathByteSinkFactory() {
    String initialString = IoTestCase.ASCII + IoTestCase.I18N;
    return new PathByteSinkFactory(initialString.getBytes(Charsets.UTF_8));
  }

  @AndroidIncompatible
  public static CharSourceFactory pathCharSourceFactory() {
    return new PathCharSourceFactory();
  }

  @AndroidIncompatible
  public static CharSinkFactory pathCharSinkFactory() {
    return new PathCharSinkFactory(null);
  }

  @AndroidIncompatible
  public static CharSinkFactory appendingPathCharSinkFactory() {
    String initialString = IoTestCase.ASCII + IoTestCase.I18N;
    return new PathCharSinkFactory(initialString);
  }

  public static ByteSourceFactory asByteSourceFactory(final CharSourceFactory factory) {
    checkNotNull(factory);
    return new ByteSourceFactory() {
      @Override
      public ByteSource createSource(byte[] data) throws IOException {
        return factory.createSource(new String(data, Charsets.UTF_8)).asByteSource(Charsets.UTF_8);
      }

      @Override
      public byte[] getExpected(byte[] data) {
        return factory.getExpected(new String(data, Charsets.UTF_8)).getBytes(Charsets.UTF_8);
      }

      @Override
      public void tearDown() throws IOException {
        factory.tearDown();
      }
    };
  }

  public static CharSourceFactory asCharSourceFactory(final ByteSourceFactory factory) {
    checkNotNull(factory);
    return new CharSourceFactory() {
      @Override
      public CharSource createSource(String string) throws IOException {
        return factory.createSource(string.getBytes(Charsets.UTF_8)).asCharSource(Charsets.UTF_8);
      }

      @Override
      public String getExpected(String data) {
        return new String(factory.getExpected(data.getBytes(Charsets.UTF_8)), Charsets.UTF_8);
      }

      @Override
      public void tearDown() throws IOException {
        factory.tearDown();
      }
    };
  }

  public static CharSinkFactory asCharSinkFactory(final ByteSinkFactory factory) {
    checkNotNull(factory);
    return new CharSinkFactory() {
      @Override
      public CharSink createSink() throws IOException {
        return factory.createSink().asCharSink(Charsets.UTF_8);
      }

      @Override
      public String getSinkContents() throws IOException {
        return new String(factory.getSinkContents(), Charsets.UTF_8);
      }

      @Override
      public String getExpected(String data) {
        /*
         * Get what the byte sink factory would expect for no written bytes, then append expected
         * string to that.
         */
        byte[] factoryExpectedForNothing = factory.getExpected(new byte[0]);
        return new String(factoryExpectedForNothing, Charsets.UTF_8) + checkNotNull(data);
      }

      @Override
      public void tearDown() throws IOException {
        factory.tearDown();
      }
    };
  }

  public static ByteSourceFactory asSlicedByteSourceFactory(
      final ByteSourceFactory factory, final long off, final long len) {
    checkNotNull(factory);
    return new ByteSourceFactory() {
      @Override
      public ByteSource createSource(byte[] bytes) throws IOException {
        return factory.createSource(bytes).slice(off, len);
      }

      @Override
      public byte[] getExpected(byte[] bytes) {
        byte[] baseExpected = factory.getExpected(bytes);
        int startOffset = (int) Math.min(off, baseExpected.length);
        int actualLen = (int) Math.min(len, baseExpected.length - startOffset);
        return Arrays.copyOfRange(baseExpected, startOffset, startOffset + actualLen);
      }

      @Override
      public void tearDown() throws IOException {
        factory.tearDown();
      }
    };
  }

  private static class StringSourceFactory implements CharSourceFactory {

    @Override
    public CharSource createSource(String data) throws IOException {
      return CharSource.wrap(data);
    }

    @Override
    public String getExpected(String data) {
      return data;
    }

    @Override
    public void tearDown() throws IOException {}
  }

  private static class ByteArraySourceFactory implements ByteSourceFactory {

    @Override
    public ByteSource createSource(byte[] bytes) throws IOException {
      return ByteSource.wrap(bytes);
    }

    @Override
    public byte[] getExpected(byte[] bytes) {
      return bytes;
    }

    @Override
    public void tearDown() throws IOException {}
  }

  private static class EmptyCharSourceFactory implements CharSourceFactory {

    @Override
    public CharSource createSource(String data) throws IOException {
      return CharSource.empty();
    }

    @Override
    public String getExpected(String data) {
      return "";
    }

    @Override
    public void tearDown() throws IOException {}
  }

  private static class EmptyByteSourceFactory implements ByteSourceFactory {

    @Override
    public ByteSource createSource(byte[] bytes) throws IOException {
      return ByteSource.empty();
    }

    @Override
    public byte[] getExpected(byte[] bytes) {
      return new byte[0];
    }

    @Override
    public void tearDown() throws IOException {}
  }

  private abstract static class FileFactory {

    private static final Logger logger = Logger.getLogger(FileFactory.class.getName());

    private final ThreadLocal<File> fileThreadLocal = new ThreadLocal<>();

    protected File createFile() throws IOException {
      File file = File.createTempFile("SinkSourceFile", "txt");
      fileThreadLocal.set(file);
      return file;
    }

    protected File getFile() {
      return fileThreadLocal.get();
    }

    public final void tearDown() throws IOException {
      if (!fileThreadLocal.get().delete()) {
        logger.warning("Unable to delete file: " + fileThreadLocal.get());
      }
      fileThreadLocal.remove();
    }
  }

  private static class FileByteSourceFactory extends FileFactory implements ByteSourceFactory {

    @Override
    public ByteSource createSource(byte[] bytes) throws IOException {
      checkNotNull(bytes);
      File file = createFile();
      OutputStream out = new FileOutputStream(file);
      try {
        out.write(bytes);
      } finally {
        out.close();
      }
      return Files.asByteSource(file);
    }

    @Override
    public byte[] getExpected(byte[] bytes) {
      return checkNotNull(bytes);
    }
  }

  private static class FileByteSinkFactory extends FileFactory implements ByteSinkFactory {

    private final byte[] initialBytes;

    private FileByteSinkFactory(byte @Nullable [] initialBytes) {
      this.initialBytes = initialBytes;
    }

    @Override
    public ByteSink createSink() throws IOException {
      File file = createFile();
      if (initialBytes != null) {
        FileOutputStream out = new FileOutputStream(file);
        try {
          out.write(initialBytes);
        } finally {
          out.close();
        }
        return Files.asByteSink(file, FileWriteMode.APPEND);
      }
      return Files.asByteSink(file);
    }

    @Override
    public byte[] getExpected(byte[] bytes) {
      if (initialBytes == null) {
        return checkNotNull(bytes);
      } else {
        byte[] result = new byte[initialBytes.length + bytes.length];
        System.arraycopy(initialBytes, 0, result, 0, initialBytes.length);
        System.arraycopy(bytes, 0, result, initialBytes.length, bytes.length);
        return result;
      }
    }

    @Override
    public byte[] getSinkContents() throws IOException {
      File file = getFile();
      InputStream in = new FileInputStream(file);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buffer = new byte[100];
      int read;
      while ((read = in.read(buffer)) != -1) {
        out.write(buffer, 0, read);
      }
      return out.toByteArray();
    }
  }

  private static class FileCharSourceFactory extends FileFactory implements CharSourceFactory {

    @Override
    public CharSource createSource(String string) throws IOException {
      checkNotNull(string);
      File file = createFile();
      Writer writer = new OutputStreamWriter(new FileOutputStream(file), Charsets.UTF_8);
      try {
        writer.write(string);
      } finally {
        writer.close();
      }
      return Files.asCharSource(file, Charsets.UTF_8);
    }

    @Override
    public String getExpected(String string) {
      return checkNotNull(string);
    }
  }

  private static class FileCharSinkFactory extends FileFactory implements CharSinkFactory {

    private final String initialString;

    private FileCharSinkFactory(@Nullable String initialString) {
      this.initialString = initialString;
    }

    @Override
    public CharSink createSink() throws IOException {
      File file = createFile();
      if (initialString != null) {
        Writer writer = new OutputStreamWriter(new FileOutputStream(file), Charsets.UTF_8);
        try {
          writer.write(initialString);
        } finally {
          writer.close();
        }
        return Files.asCharSink(file, Charsets.UTF_8, FileWriteMode.APPEND);
      }
      return Files.asCharSink(file, Charsets.UTF_8);
    }

    @Override
    public String getExpected(String string) {
      checkNotNull(string);
      return initialString == null ? string : initialString + string;
    }

    @Override
    public String getSinkContents() throws IOException {
      File file = getFile();
      Reader reader = new InputStreamReader(new FileInputStream(file), Charsets.UTF_8);
      StringBuilder builder = new StringBuilder();
      CharBuffer buffer = CharBuffer.allocate(100);
      while (reader.read(buffer) != -1) {
        buffer.flip();
        builder.append(buffer);
        buffer.clear();
      }
      return builder.toString();
    }
  }

  private static class UrlByteSourceFactory extends FileByteSourceFactory {

    @SuppressWarnings("CheckReturnValue") // only using super.createSource to create a file
    @Override
    public ByteSource createSource(byte[] bytes) throws IOException {
      super.createSource(bytes);
      return Resources.asByteSource(getFile().toURI().toURL());
    }
  }

  private static class UrlCharSourceFactory extends FileCharSourceFactory {

    @SuppressWarnings("CheckReturnValue") // only using super.createSource to create a file
    @Override
    public CharSource createSource(String string) throws IOException {
      super.createSource(string); // just ignore returned CharSource
      return Resources.asCharSource(getFile().toURI().toURL(), Charsets.UTF_8);
    }
  }

  @AndroidIncompatible
  private abstract static class Jdk7FileFactory {

    private static final Logger logger = Logger.getLogger(Jdk7FileFactory.class.getName());

    private final ThreadLocal<Path> fileThreadLocal = new ThreadLocal<>();

    protected Path createFile() throws IOException {
      Path file = java.nio.file.Files.createTempFile("SinkSourceFile", "txt");
      fileThreadLocal.set(file);
      return file;
    }

    protected Path getPath() {
      return fileThreadLocal.get();
    }

    public final void tearDown() throws IOException {
      try {
        java.nio.file.Files.delete(fileThreadLocal.get());
      } catch (IOException e) {
        logger.log(Level.WARNING, "Unable to delete file: " + fileThreadLocal.get(), e);
      }
      fileThreadLocal.remove();
    }
  }

  @AndroidIncompatible
  private static class PathByteSourceFactory extends Jdk7FileFactory implements ByteSourceFactory {

    @Override
    public ByteSource createSource(byte[] bytes) throws IOException {
      checkNotNull(bytes);
      Path file = createFile();

      java.nio.file.Files.write(file, bytes);
      return MoreFiles.asByteSource(file);
    }

    @Override
    public byte[] getExpected(byte[] bytes) {
      return checkNotNull(bytes);
    }
  }

  @AndroidIncompatible
  private static class PathByteSinkFactory extends Jdk7FileFactory implements ByteSinkFactory {

    private final byte[] initialBytes;

    private PathByteSinkFactory(byte @Nullable [] initialBytes) {
      this.initialBytes = initialBytes;
    }

    @Override
    public ByteSink createSink() throws IOException {
      Path file = createFile();
      if (initialBytes != null) {
        java.nio.file.Files.write(file, initialBytes);
        return MoreFiles.asByteSink(file, StandardOpenOption.APPEND);
      }
      return MoreFiles.asByteSink(file);
    }

    @Override
    public byte[] getExpected(byte[] bytes) {
      if (initialBytes == null) {
        return checkNotNull(bytes);
      } else {
        byte[] result = new byte[initialBytes.length + bytes.length];
        System.arraycopy(initialBytes, 0, result, 0, initialBytes.length);
        System.arraycopy(bytes, 0, result, initialBytes.length, bytes.length);
        return result;
      }
    }

    @Override
    public byte[] getSinkContents() throws IOException {
      Path file = getPath();
      return java.nio.file.Files.readAllBytes(file);
    }
  }

  @AndroidIncompatible
  private static class PathCharSourceFactory extends Jdk7FileFactory implements CharSourceFactory {

    @Override
    public CharSource createSource(String string) throws IOException {
      checkNotNull(string);
      Path file = createFile();
      try (Writer writer = java.nio.file.Files.newBufferedWriter(file, Charsets.UTF_8)) {
        writer.write(string);
      }
      return MoreFiles.asCharSource(file, Charsets.UTF_8);
    }

    @Override
    public String getExpected(String string) {
      return checkNotNull(string);
    }
  }

  @AndroidIncompatible
  private static class PathCharSinkFactory extends Jdk7FileFactory implements CharSinkFactory {

    private final String initialString;

    private PathCharSinkFactory(@Nullable String initialString) {
      this.initialString = initialString;
    }

    @Override
    public CharSink createSink() throws IOException {
      Path file = createFile();
      if (initialString != null) {
        try (Writer writer = java.nio.file.Files.newBufferedWriter(file, Charsets.UTF_8)) {
          writer.write(initialString);
        }
        return MoreFiles.asCharSink(file, Charsets.UTF_8, StandardOpenOption.APPEND);
      }
      return MoreFiles.asCharSink(file, Charsets.UTF_8);
    }

    @Override
    public String getExpected(String string) {
      checkNotNull(string);
      return initialString == null ? string : initialString + string;
    }

    @Override
    public String getSinkContents() throws IOException {
      Path file = getPath();
      try (Reader reader = java.nio.file.Files.newBufferedReader(file, Charsets.UTF_8)) {
        StringBuilder builder = new StringBuilder();
        for (int c = reader.read(); c != -1; c = reader.read()) {
          builder.append((char) c);
        }
        return builder.toString();
      }
    }
  }
}
