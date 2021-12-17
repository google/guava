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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import junit.framework.TestCase;

/**
 * @param <S> the source or sink type
 * @param <T> the data type (byte[] or String)
 * @param <F> the factory type
 * @author Colin Decker
 */
@AndroidIncompatible // Android doesn't understand tests that lack default constructors.
public class SourceSinkTester<S, T, F extends SourceSinkFactory<S, T>> extends TestCase {

  static final String LOREM_IPSUM =
      "Lorem ipsum dolor sit amet, consectetur adipiscing "
          + "elit. Cras fringilla elit ac ipsum adipiscing vulputate. Maecenas in lorem nulla, ac "
          + "sollicitudin quam. Praesent neque elit, sodales quis vestibulum vel, pellentesque nec "
          + "erat. Proin cursus commodo lacus eget congue. Aliquam erat volutpat. Fusce ut leo sed "
          + "risus tempor vehicula et a odio. Nam aliquet dolor viverra libero rutrum accumsan "
          + "quis in augue. Suspendisse id dui in lorem tristique placerat eget vel risus. Sed "
          + "metus neque, scelerisque in molestie ac, mattis quis lectus. Pellentesque viverra "
          + "justo commodo quam bibendum ut gravida leo accumsan. Nullam malesuada sagittis diam, "
          + "quis suscipit mauris euismod vulputate. Pellentesque ultrices tellus sed lorem "
          + "aliquet pulvinar. Nam lorem nunc, ultrices at auctor non, scelerisque eget turpis. "
          + "Nullam eget varius erat. Sed a lorem id arcu dictum euismod. Fusce lectus odio, "
          + "elementum ullamcorper mattis viverra, dictum sit amet lacus.\n"
          + "\n"
          + "Nunc quis lacus est. Sed aliquam pretium cursus. Sed eu libero eros. In hac habitasse "
          + "platea dictumst. Pellentesque molestie, nibh nec iaculis luctus, justo sem lobortis "
          + "enim, at feugiat leo magna nec libero. Mauris quis odio eget nisl rutrum cursus nec "
          + "eget augue. Sed nec arcu sem. In hac habitasse platea dictumst.";

  static final ImmutableMap<String, String> TEST_STRINGS =
      ImmutableMap.<String, String>builder()
          .put("empty", "")
          .put("1 char", "0")
          .put("1 word", "hello")
          .put("2 words", "hello world")
          .put("\\n line break", "hello\nworld")
          .put("\\r line break", "hello\rworld")
          .put("\\r\\n line break", "hello\r\nworld")
          .put("\\n at EOF", "hello\nworld\n")
          .put("\\r at EOF", "hello\nworld\r")
          .put("lorem ipsum", LOREM_IPSUM)
          .build();

  protected final F factory;
  protected final T data;
  protected final T expected;

  private final String suiteName;
  private final String caseDesc;

  SourceSinkTester(F factory, T data, String suiteName, String caseDesc, Method method) {
    super(method.getName());
    this.factory = checkNotNull(factory);
    this.data = checkNotNull(data);
    this.expected = checkNotNull(factory.getExpected(data));
    this.suiteName = checkNotNull(suiteName);
    this.caseDesc = checkNotNull(caseDesc);
  }

  @Override
  public String getName() {
    return super.getName() + " [" + suiteName + " [" + caseDesc + "]]";
  }

  protected static ImmutableList<String> getLines(final String string) {
    try {
      return new CharSource() {
        @Override
        public Reader openStream() throws IOException {
          return new StringReader(string);
        }
      }.readLines();
    } catch (IOException e) {
      throw new AssertionError();
    }
  }

  @Override
  public void tearDown() throws IOException {
    factory.tearDown();
  }

  static ImmutableList<Method> getTestMethods(Class<?> testClass) {
    List<Method> result = Lists.newArrayList();
    for (Method method : testClass.getDeclaredMethods()) {
      if (Modifier.isPublic(method.getModifiers())
          && method.getReturnType() == void.class
          && method.getParameterTypes().length == 0
          && method.getName().startsWith("test")) {
        result.add(method);
      }
    }
    return ImmutableList.copyOf(result);
  }
}
