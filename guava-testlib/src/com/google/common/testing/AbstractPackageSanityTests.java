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

package com.google.common.testing;

import static com.google.common.testing.AbstractPackageSanityTests.Chopper.suffix;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.MutableClassToInstanceMap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.Parameter;
import com.google.common.reflect.TypeToken;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

/**
 * Automatically runs sanity checks for the entire package of the subclass. Currently sanity checks
 * include {@link NullPointerTester} and {@link SerializableTester}. For example:
 * <pre>
 * {@literal @MediumTest}(MediumTestAttributes.FILE)
 * public class PackageSanityTests extends AbstractPackageSanityTests {}
 * </pre>
 *
 * <p>If a certain type Foo's null check testing requires default value to be manually set, or that
 * it needs custom code to instantiate an instance for testing instance methods, add a {@code
 * public void testNulls()} method to FooTest and Foo will be ignored by the automated {@link
 * #testNulls} test.
 *
 * <p>Since this class scans the classpath and reads classpath resources, the test is essentially
 * a {@code MediumTest}.
 *
 * @author Ben Yu
 * @since 14.0
 */
@Beta
// TODO: Switch to JUnit 4 and use @Parameterized and @BeforeClass
public abstract class AbstractPackageSanityTests extends TestCase {

  /* The names of the expected method that tests null checks. */
  private static final ImmutableList<String> NULL_TEST_METHOD_NAMES = ImmutableList.of(
      "testNulls", "testNull", "testNullPointer", "testNullPointerException");

  /* The names of the expected method that tests serializable. */
  private static final ImmutableList<String> SERIALIZABLE_TEST_METHOD_NAMES =
      ImmutableList.of("testSerializable", "testSerialization");

  private static final Chopper TEST_SUFFIX =
      suffix("Test")
          .or(suffix("Tests"))
          .or(suffix("TestCase"))
          .or(suffix("TestSuite"));

  /**
   * Sorts methods/constructors with least number of parameters first since it's likely easier to
   * fill dummy parameter values for them. Ties are broken by name then by the string form of the
   * parameter list.
   */
  private static final Ordering<Invokable<?, ?>> LEAST_PARAMETERS_FIRST =
      new Ordering<Invokable<?, ?>>() {
        @Override public int compare(Invokable<?, ?> left, Invokable<?, ?> right) {
          List<Parameter> params1 = left.getParameters();
          List<Parameter> params2 = right.getParameters();
          return ComparisonChain.start()
              .compare(params1.size(), params2.size())
              .compare(left.getName(), right.getName())
              .compare(params1, params2, Ordering.usingToString())
              .result();
        }
      };

  private final Logger logger = Logger.getLogger(getClass().getName());
  private final MutableClassToInstanceMap<Object> defaultValues =
      MutableClassToInstanceMap.create();
  private final NullPointerTester nullPointerTester = new NullPointerTester();

  public AbstractPackageSanityTests() {
    // TODO(benyu): bake these into ArbitraryInstances.
    setDefault(byte.class, (byte) 1);
    setDefault(Byte.class, (byte) 1);
    setDefault(short.class, (short) 1);
    setDefault(Short.class, (short) 1);
    setDefault(int.class, 1);
    setDefault(Integer.class, 1);
    setDefault(long.class, 1L);
    setDefault(Long.class, 1L);
    setDefault(float.class, 1F);
    setDefault(Float.class, 1F);
    setDefault(double.class, 1D);
    setDefault(Double.class, 1D);
  }

  /** Tests all {@link Serializable} classes in the package. */
  @Test
  public void testSerializable() throws Exception {
    // TODO: when we use @BeforeClass, we can pay the cost of class path scanning only once.
    for (Class<?> classToTest
        : findClassesToTest(loadPublicClassesInPackage(), SERIALIZABLE_TEST_METHOD_NAMES)) {
      if (Serializable.class.isAssignableFrom(classToTest)) {
        testSerializable(classToTest);
      }
    }
  }

  /** Tests null checks through the entire package. */
  @Test
  public void testNulls() throws Exception {
    for (Class<?> classToTest
        : findClassesToTest(loadPublicClassesInPackage(), NULL_TEST_METHOD_NAMES)) {
      testNulls(classToTest);
    }
  }

  /**
   * Sets the default value for {@code type}, when dummy value for a parameter of the same type
   * needs to be created in order to invoke a method or constructor.
   */
  protected final <T> void setDefault(Class<T> type, T value) {
    nullPointerTester.setDefault(type, value);
    defaultValues.putInstance(type, value);
  }

  private void testNulls(Class<?> cls) throws Exception {
    nullPointerTester.testAllPublicConstructors(cls);
    nullPointerTester.testAllPublicStaticMethods(cls);
    Object instance = instantiate(cls, TestErrorReporter.FOR_NULLS_TEST);
    if (instance != null) {
      nullPointerTester.testAllPublicInstanceMethods(instance);
    }
  }

  private void testSerializable(Class<?> cls) throws Exception {
    Object instance = instantiate(cls, TestErrorReporter.FOR_SERIALIZABLE_TEST);
    if (instance != null) {
      if (isEqualsDefined(cls)) {
        SerializableTester.reserializeAndAssert(instance);
      } else {
        SerializableTester.reserialize(instance);
      }
    }
  }

  /**
   * Finds the classes not ending with a test suffix and not covered by an explicit test
   * whose name is {@code explicitTestName}.
   */
  @VisibleForTesting static List<Class<?>> findClassesToTest(
      Iterable<? extends Class<?>> classes, Iterable<String> explicitTestNames) {
    // "a.b.Foo" -> a.b.Foo.class
    TreeMap<String, Class<?>> classMap = Maps.newTreeMap();
    for (Class<?> cls : classes) {
      classMap.put(cls.getName(), cls);
    }
    // Foo.class -> [FooTest.class, FooTests.class, FooTestSuite.class, ...]
    Multimap<Class<?>, Class<?>> testClasses = HashMultimap.create();
    LinkedHashSet<Class<?>> nonTestClasses = Sets.newLinkedHashSet();
    for (Class<?> cls : classes) {
      Optional<String> testedClassName = TEST_SUFFIX.chop(cls.getName());
      if (testedClassName.isPresent()) {
        Class<?> testedClass = classMap.get(testedClassName.get());
        if (testedClass != null) {
          testClasses.put(testedClass, cls);
        }
      } else {
        nonTestClasses.add(cls);
      }
    }
    List<Class<?>> classesToTest = Lists.newArrayListWithExpectedSize(nonTestClasses.size());
    NEXT_CANDIDATE: for (Class<?> cls : nonTestClasses) {
      for (Class<?> testClass : testClasses.get(cls)) {
        if (hasTest(testClass, explicitTestNames)) {
          // covered by explicit test
          continue NEXT_CANDIDATE;
        }
      }
      classesToTest.add(cls);
    }
    return classesToTest;
  }

  /** Returns null if no instance can be created. */
  @Nullable private Object instantiate(Class<?> cls, TestErrorReporter errorReporter)
      throws Exception {
    if (cls.isEnum()) {
      Object[] constants = cls.getEnumConstants();
      if (constants.length > 0) {
        return constants[0];
      } else {
        return null;
      }
    }
    TypeToken<?> type = TypeToken.of(cls);
    List<AssertionFailedError> errors = Lists.newArrayList();
    List<InvocationTargetException> instantiationExceptions = Lists.newArrayList();
    for (Invokable<?, ?> factory : getFactories(type)) {
      List<Object> args;
      try {
        args = getDummyArguments(factory, errorReporter);
      } catch (AssertionFailedError e) {
        errors.add(e);
        continue;
      }
      Object instance;
      try {
        instance = factory.invoke(null, args.toArray());
      } catch (InvocationTargetException e) {
        instantiationExceptions.add(e);
        continue;
      }
      try {
        assertNotNull(factory + " returns null and cannot be used to test instance methods.",
            instance);
        return instance;
      } catch (AssertionFailedError e) {
        errors.add(e);
      }
    }
    if (!errors.isEmpty()) {
      throw errors.get(0);
    }
    if (!instantiationExceptions.isEmpty()) {
      throw instantiationExceptions.get(0);
    }
    return null;
  }

  private static List<Invokable<?, ?>> getFactories(TypeToken<?> type) {
    List<Invokable<?, ?>> invokables = Lists.newArrayList();
    for (Method method : type.getRawType().getMethods()) {
      Invokable<?, ?> invokable = type.method(method);
      if (invokable.isStatic() && type.isAssignableFrom(invokable.getReturnType())) {
        invokables.add(invokable);
      }
    }
    if (!Modifier.isAbstract(type.getRawType().getModifiers())) {
      for (Constructor<?> constructor : type.getRawType().getConstructors()) {
        invokables.add(type.constructor(constructor));
      }
    }
    Collections.sort(invokables, LEAST_PARAMETERS_FIRST);
    return invokables;
  }

  private List<Object> getDummyArguments(
      Invokable<?, ?> invokable, TestErrorReporter errorReporter) {
    List<Object> args = Lists.newArrayList();
    for (Parameter param : invokable.getParameters()) {
      Object defaultValue = getDummyValue(param.getType());
      assertTrue(errorReporter.cannotDetermineParameterValue(invokable, param),
          defaultValue != null || param.isAnnotationPresent(Nullable.class));
      args.add(defaultValue);
    }
    return args;
  }

  private <T> T getDummyValue(TypeToken<T> type) {
    Class<? super T> rawType = type.getRawType();
    @SuppressWarnings("unchecked") // Assume all default values are generics safe.
    T defaultValue = (T) defaultValues.getInstance(rawType);
    if (defaultValue != null) {
      return defaultValue;
    }
    @SuppressWarnings("unchecked") // ArbitraryInstances always returns generics-safe dummies.
    T value = (T) ArbitraryInstances.get(rawType);
    if (value != null) {
      return value;
    }
    if (rawType.isInterface()) {
      return new DummyProxy() {
        @Override <R> R dummyReturnValue(TypeToken<R> returnType) {
          return getDummyValue(returnType);
        }
      }.newProxy(type);
    }
    return null;
  }

  private List<Class<?>> loadPublicClassesInPackage() throws IOException {
    List<Class<?>> classes = Lists.newArrayList();
    String packageName = getClass().getPackage().getName();
    for (ClassPath.ClassInfo classInfo 
        : ClassPath.from(getClass().getClassLoader()).getTopLevelClasses(packageName)) {
      Class<?> cls;
      try {
        cls = classInfo.load();
      } catch (NoClassDefFoundError e) {
        // In case there were linking problems, this is probably not a class we care to test anyway.
        logger.log(Level.SEVERE, "Cannot load class " + classInfo + ", skipping...", e);
        continue;
      }
      if (!cls.isInterface() && Modifier.isPublic(cls.getModifiers())) {
        classes.add(cls);
      }
    }
    return classes;
  }

  private static boolean hasTest(Class<?> testClass, Iterable<String> testNames) {
    for (String testName : testNames) {
      try {
        testClass.getMethod(testName);
        return true;
      } catch (NoSuchMethodException e) {
        continue;
      }
    }
    return false;
  }

  private static boolean isEqualsDefined(Class<?> cls) {
    try {
      cls.getDeclaredMethod("equals", Object.class);
      return true;
    } catch (NoSuchMethodException e) {
      return false;
    }
  }

  private enum TestErrorReporter {

    FOR_NULLS_TEST {
      @Override String suggestExplicitTest(Class<?> classToTest) {
        return "Please explicitly test null checks in "
            + classToTest.getName() + "Test." + NULL_TEST_METHOD_NAMES.get(0) + "()";
      }
    },
    FOR_SERIALIZABLE_TEST {
      @Override String suggestExplicitTest(Class<?> classToTest) {
        return "Please explicitly test serialization in "
            + classToTest.getName() + "Test." + SERIALIZABLE_TEST_METHOD_NAMES.get(0) + "()";
      }
    },
    ;

    final String cannotDetermineParameterValue(Invokable<?, ?> factory, Parameter param) {
      return "Cannot use " + factory + " to instantiate " + factory.getDeclaringClass()
          + " because default value of " + param + " cannot be determined.\n"
          + suggestExplicitTest(factory.getDeclaringClass());
    }

    abstract String suggestExplicitTest(Class<?> classToTest);
  }

  static abstract class Chopper {

    final Chopper or(final Chopper you) {
      final Chopper i = this;
      return new Chopper() {
        @Override Optional<String> chop(String str) {
          return i.chop(str).or(you.chop(str));
        }
      };
    }

    abstract Optional<String> chop(String str);

    static Chopper suffix(final String suffix) {
      return new Chopper() {
        @Override Optional<String> chop(String str) {
          if (str.endsWith(suffix)) {
            return Optional.of(str.substring(0, str.length() - suffix.length()));
          } else {
            return Optional.absent();
          }
        }
      };
    }
  }
}
