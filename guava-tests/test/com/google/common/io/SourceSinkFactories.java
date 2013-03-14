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
import com.google.common.jdk5backport.Arrays;

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
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.logging.Logger;

import javax.annotation.Nullable;

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
    try {
      return new FileByteSinkFactory(initialString.getBytes(Charsets.UTF_8.name()));
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
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

  public static CharSourceFactory asCharSourceFactory(final ByteSourceFactory factory) {
    checkNotNull(factory);
    return new CharSourceFactory() {
      @Override
      public CharSource createSource(String string) throws IOException {
        return factory.createSource(string.getBytes(Charsets.UTF_8.name()))
            .asCharSource(Charsets.UTF_8);
      }

      @Override
      public String getExpected(String data) {
        try {
          return new String(factory.getExpected(data.getBytes(Charsets.UTF_8.name())),
              Charsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
          throw new AssertionError();
        }
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
        return new String(factory.getSinkContents(), Charsets.UTF_8.name());
      }

      @Override
      public String getExpected(String data) {
        /*
         * Get what the byte sink factory would expect for no written bytes, then append expected
         * string to that.
         */
        byte[] factoryExpectedForNothing = factory.getExpected(new byte[0]);
        try {
          return new String(factoryExpectedForNothing, Charsets.UTF_8.name()) + checkNotNull(data);
        } catch (UnsupportedEncodingException e) {
          throw new AssertionError();
        }
      }

      @Override
      public void tearDown() throws IOException {
        factory.tearDown();
      }
    };
  }

  public static ByteSourceFactory asSlicedByteSourceFactory(final ByteSourceFactory factory,
      final int off, final int len) {
    checkNotNull(factory);
    return new ByteSourceFactory() {
      @Override
      public ByteSource createSource(byte[] bytes) throws IOException {
        return factory.createSource(bytes).slice(off, len);
      }

      @Override
      public byte[] getExpected(byte[] bytes) {
        byte[] baseExpected = factory.getExpected(bytes);
        return Arrays.copyOfRange(baseExpected, off, Math.min(baseExpected.length, off + len));
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
    public void tearDown() throws IOException {
    }
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
    public void tearDown() throws IOException {
    }
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
    public void tearDown() throws IOException {
    }
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
    public void tearDown() throws IOException {
    }
  }

  private abstract static class FileFactory {

    private static final Logger logger = Logger.getLogger(FileFactory.class.getName());

    private final ThreadLocal<File> fileThreadLocal = new ThreadLocal<File>();

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

    private FileByteSinkFactory(@Nullable byte[] initialBytes) {
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
      return initialString == null
          ? string
          : initialString + string;
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

    @Override
    public ByteSource createSource(byte[] bytes) throws IOException {
      super.createSource(bytes);
      return Resources.asByteSource(getFile().toURI().toURL());
    }
  }

  private static class UrlCharSourceFactory extends FileCharSourceFactory {

    @Override
    public CharSource createSource(String string) throws IOException {
      super.createSource(string); // just ignore returned CharSource
      return Resources.asCharSource(getFile().toURI().toURL(), Charsets.UTF_8);
    }
  }
}
