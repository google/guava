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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.throwIfUnchecked;
import static com.google.common.testing.NullPointerTester.isNullable;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.MutableClassToInstanceMap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.Parameter;
import com.google.common.reflect.Reflection;
import com.google.common.reflect.TypeToken;
import com.google.common.testing.NullPointerTester.Visibility;
import com.google.common.testing.RelationshipTester.Item;
import com.google.common.testing.RelationshipTester.ItemReporter;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Tester that runs automated sanity tests for any given class. A typical use case is to test static
 * factory classes like:
 *
 * <pre>
 * interface Book {...}
 * public class Books {
 *   public static Book hardcover(String title) {...}
 *   public static Book paperback(String title) {...}
 * }
 * </pre>
 *
 * <p>And all the created {@code Book} instances can be tested with:
 *
 * <pre>
 * new ClassSanityTester()
 *     .forAllPublicStaticMethods(Books.class)
 *     .thatReturn(Book.class)
 *     .testEquals(); // or testNulls(), testSerializable() etc.
 * </pre>
 *
 * @author Ben Yu
 * @since 14.0
 */
@GwtIncompatible
@J2ktIncompatible
public final class ClassSanityTester {

  private static final Ordering<Invokable<?, ?>> BY_METHOD_NAME =
      new Ordering<Invokable<?, ?>>() {
        @Override
        public int compare(Invokable<?, ?> left, Invokable<?, ?> right) {
          return left.getName().compareTo(right.getName());
        }
      };

  private static final Ordering<Invokable<?, ?>> BY_PARAMETERS =
      new Ordering<Invokable<?, ?>>() {
        @Override
        public int compare(Invokable<?, ?> left, Invokable<?, ?> right) {
          return Ordering.usingToString().compare(left.getParameters(), right.getParameters());
        }
      };

  private static final Ordering<Invokable<?, ?>> BY_NUMBER_OF_PARAMETERS =
      new Ordering<Invokable<?, ?>>() {
        @Override
        public int compare(Invokable<?, ?> left, Invokable<?, ?> right) {
          return Ints.compare(left.getParameters().size(), right.getParameters().size());
        }
      };

  private final MutableClassToInstanceMap<Object> defaultValues =
      MutableClassToInstanceMap.create();
  private final ListMultimap<Class<?>, Object> distinctValues = ArrayListMultimap.create();
  private final NullPointerTester nullPointerTester = new NullPointerTester();

  public ClassSanityTester() {
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
    setDefault(Class.class, Class.class);
  }

  /**
   * Sets the default value for {@code type}. The default value isn't used in testing {@link
   * Object#equals} because more than one sample instances are needed for testing inequality. To set
   * distinct values for equality testing, use {@link #setDistinctValues} instead.
   */
  @CanIgnoreReturnValue
  public <T> ClassSanityTester setDefault(Class<T> type, T value) {
    nullPointerTester.setDefault(type, value);
    defaultValues.putInstance(type, value);
    return this;
  }

  /**
   * Sets distinct values for {@code type}, so that when a class {@code Foo} is tested for {@link
   * Object#equals} and {@link Object#hashCode}, and its construction requires a parameter of {@code
   * type}, the distinct values of {@code type} can be passed as parameters to create {@code Foo}
   * instances that are unequal.
   *
   * <p>Calling {@code setDistinctValues(type, v1, v2)} also sets the default value for {@code type}
   * that's used for {@link #testNulls}.
   *
   * <p>Only necessary for types where {@link ClassSanityTester} doesn't already know how to create
   * distinct values.
   *
   * @return this tester instance
   * @since 17.0
   */
  @CanIgnoreReturnValue
  public <T> ClassSanityTester setDistinctValues(Class<T> type, T value1, T value2) {
    checkNotNull(type);
    checkNotNull(value1);
    checkNotNull(value2);
    checkArgument(!Objects.equal(value1, value2), "Duplicate value provided.");
    distinctValues.replaceValues(type, ImmutableList.of(value1, value2));
    setDefault(type, value1);
    return this;
  }

  /**
   * Tests that {@code cls} properly checks null on all constructor and method parameters that
   * aren't annotated nullable (according to the rules of {@link NullPointerTester}). In details:
   *
   * <ul>
   *   <li>All non-private static methods are checked such that passing null for any parameter
   *       that's not annotated nullable should throw {@link NullPointerException}.
   *   <li>If there is any non-private constructor or non-private static factory method declared by
   *       {@code cls}, all non-private instance methods will be checked too using the instance
   *       created by invoking the constructor or static factory method.
   *   <li>If there is any non-private constructor or non-private static factory method declared by
   *       {@code cls}:
   *       <ul>
   *         <li>Test will fail if default value for a parameter cannot be determined.
   *         <li>Test will fail if the factory method returns null so testing instance methods is
   *             impossible.
   *         <li>Test will fail if the constructor or factory method throws exception.
   *       </ul>
   *   <li>If there is no non-private constructor or non-private static factory method declared by
   *       {@code cls}, instance methods are skipped for nulls test.
   *   <li>Nulls test is not performed on method return values unless the method is a non-private
   *       static factory method whose return type is {@code cls} or {@code cls}'s subtype.
   * </ul>
   */
  public void testNulls(Class<?> cls) {
    try {
      doTestNulls(cls, Visibility.PACKAGE);
    } catch (Exception e) {
      throwIfUnchecked(e);
      throw new RuntimeException(e);
    }
  }

  void doTestNulls(Class<?> cls, Visibility visibility)
      throws ParameterNotInstantiableException, IllegalAccessException, InvocationTargetException,
          FactoryMethodReturnsNullException {
    if (!Modifier.isAbstract(cls.getModifiers())) {
      nullPointerTester.testConstructors(cls, visibility);
    }
    nullPointerTester.testStaticMethods(cls, visibility);
    if (hasInstanceMethodToTestNulls(cls, visibility)) {
      Object instance = instantiate(cls);
      if (instance != null) {
        nullPointerTester.testInstanceMethods(instance, visibility);
      }
    }
  }

  private boolean hasInstanceMethodToTestNulls(Class<?> c, Visibility visibility) {
    for (Method method : nullPointerTester.getInstanceMethodsToTest(c, visibility)) {
      for (Parameter param : Invokable.from(method).getParameters()) {
        if (!NullPointerTester.isPrimitiveOrNullable(param)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Tests the {@link Object#equals} and {@link Object#hashCode} of {@code cls}. In details:
   *
   * <ul>
   *   <li>The non-private constructor or non-private static factory method with the most parameters
   *       is used to construct the sample instances. In case of tie, the candidate constructors or
   *       factories are tried one after another until one can be used to construct sample
   *       instances.
   *   <li>For the constructor or static factory method used to construct instances, it's checked
   *       that when equal parameters are passed, the result instance should also be equal; and vice
   *       versa.
   *   <li>If a non-private constructor or non-private static factory method exists:
   *       <ul>
   *         <li>Test will fail if default value for a parameter cannot be determined.
   *         <li>Test will fail if the factory method returns null so testing instance methods is
   *             impossible.
   *         <li>Test will fail if the constructor or factory method throws exception.
   *       </ul>
   *   <li>If there is no non-private constructor or non-private static factory method declared by
   *       {@code cls}, no test is performed.
   *   <li>Equality test is not performed on method return values unless the method is a non-private
   *       static factory method whose return type is {@code cls} or {@code cls}'s subtype.
   *   <li>Inequality check is not performed against state mutation methods such as {@link
   *       List#add}, or functional update methods such as {@link
   *       com.google.common.base.Joiner#skipNulls}.
   * </ul>
   *
   * <p>Note that constructors taking a builder object cannot be tested effectively because
   * semantics of builder can be arbitrarily complex. Still, a factory class can be created in the
   * test to facilitate equality testing. For example:
   *
   * <pre>
   * public class FooTest {
   *
   *   private static class FooFactoryForTest {
   *     public static Foo create(String a, String b, int c, boolean d) {
   *       return Foo.builder()
   *           .setA(a)
   *           .setB(b)
   *           .setC(c)
   *           .setD(d)
   *           .build();
   *     }
   *   }
   *
   *   public void testEquals() {
   *     new ClassSanityTester()
   *       .forAllPublicStaticMethods(FooFactoryForTest.class)
   *       .thatReturn(Foo.class)
   *       .testEquals();
   *   }
   * }
   * </pre>
   *
   * <p>It will test that Foo objects created by the {@code create(a, b, c, d)} factory method with
   * equal parameters are equal and vice versa, thus indirectly tests the builder equality.
   */
  public void testEquals(Class<?> cls) {
    try {
      doTestEquals(cls);
    } catch (Exception e) {
      throwIfUnchecked(e);
      throw new RuntimeException(e);
    }
  }

  void doTestEquals(Class<?> cls)
      throws ParameterNotInstantiableException, ParameterHasNoDistinctValueException,
          IllegalAccessException, InvocationTargetException, FactoryMethodReturnsNullException {
    if (cls.isEnum()) {
      return;
    }
    List<? extends Invokable<?, ?>> factories = Lists.reverse(getFactories(TypeToken.of(cls)));
    if (factories.isEmpty()) {
      return;
    }
    int numberOfParameters = factories.get(0).getParameters().size();
    List<ParameterNotInstantiableException> paramErrors = Lists.newArrayList();
    List<ParameterHasNoDistinctValueException> distinctValueErrors = Lists.newArrayList();
    List<InvocationTargetException> instantiationExceptions = Lists.newArrayList();
    List<FactoryMethodReturnsNullException> nullErrors = Lists.newArrayList();
    // Try factories with the greatest number of parameters.
    for (Invokable<?, ?> factory : factories) {
      if (factory.getParameters().size() == numberOfParameters) {
        try {
          testEqualsUsing(factory);
          return;
        } catch (ParameterNotInstantiableException e) {
          paramErrors.add(e);
        } catch (ParameterHasNoDistinctValueException e) {
          distinctValueErrors.add(e);
        } catch (InvocationTargetException e) {
          instantiationExceptions.add(e);
        } catch (FactoryMethodReturnsNullException e) {
          nullErrors.add(e);
        }
      }
    }
    throwFirst(paramErrors);
    throwFirst(distinctValueErrors);
    throwFirst(instantiationExceptions);
    throwFirst(nullErrors);
  }

  /**
   * Instantiates {@code cls} by invoking one of its non-private constructors or non-private static
   * factory methods with the parameters automatically provided using dummy values.
   *
   * @return The instantiated instance, or {@code null} if the class has no non-private constructor
   *     or factory method to be constructed.
   */
  <T> @Nullable T instantiate(Class<T> cls)
      throws ParameterNotInstantiableException,
          IllegalAccessException,
          InvocationTargetException,
          FactoryMethodReturnsNullException {
    if (cls.isEnum()) {
      T[] constants = cls.getEnumConstants();
      if (constants != null && constants.length > 0) {
        return constants[0];
      } else {
        return null;
      }
    }
    TypeToken<T> type = TypeToken.of(cls);
    List<ParameterNotInstantiableException> paramErrors = Lists.newArrayList();
    List<InvocationTargetException> instantiationExceptions = Lists.newArrayList();
    List<FactoryMethodReturnsNullException> nullErrors = Lists.newArrayList();
    for (Invokable<?, ? extends T> factory : getFactories(type)) {
      T instance;
      try {
        instance = instantiate(factory);
      } catch (ParameterNotInstantiableException e) {
        paramErrors.add(e);
        continue;
      } catch (InvocationTargetException e) {
        instantiationExceptions.add(e);
        continue;
      }
      if (instance == null) {
        nullErrors.add(new FactoryMethodReturnsNullException(factory));
      } else {
        return instance;
      }
    }
    throwFirst(paramErrors);
    throwFirst(instantiationExceptions);
    throwFirst(nullErrors);
    return null;
  }

  /**
   * Instantiates using {@code factory}. If {@code factory} is annotated nullable and returns null,
   * null will be returned.
   *
   * @throws ParameterNotInstantiableException if the static methods cannot be invoked because the
   *     default value of a parameter cannot be determined.
   * @throws IllegalAccessException if the class isn't public or is nested inside a non-public
   *     class, preventing its methods from being accessible.
   * @throws InvocationTargetException if a static method threw exception.
   */
  private <T> @Nullable T instantiate(Invokable<?, ? extends T> factory)
      throws ParameterNotInstantiableException, InvocationTargetException, IllegalAccessException {
    return invoke(factory, getDummyArguments(factory));
  }

  /**
   * Returns an object responsible for performing sanity tests against the return values of all
   * public static methods declared by {@code cls}, excluding superclasses.
   */
  public FactoryMethodReturnValueTester forAllPublicStaticMethods(Class<?> cls) {
    ImmutableList.Builder<Invokable<?, ?>> builder = ImmutableList.builder();
    for (Method method : cls.getDeclaredMethods()) {
      Invokable<?, ?> invokable = Invokable.from(method);
      invokable.setAccessible(true);
      if (invokable.isPublic() && invokable.isStatic() && !invokable.isSynthetic()) {
        builder.add(invokable);
      }
    }
    return new FactoryMethodReturnValueTester(cls, builder.build(), "public static methods");
  }

  /** Runs sanity tests against return values of static factory methods declared by a class. */
  public final class FactoryMethodReturnValueTester {
    private final Set<String> packagesToTest = Sets.newHashSet();
    private final Class<?> declaringClass;
    private final ImmutableList<Invokable<?, ?>> factories;
    private final String factoryMethodsDescription;
    private Class<?> returnTypeToTest = Object.class;

    private FactoryMethodReturnValueTester(
        Class<?> declaringClass,
        ImmutableList<Invokable<?, ?>> factories,
        String factoryMethodsDescription) {
      this.declaringClass = declaringClass;
      this.factories = factories;
      this.factoryMethodsDescription = factoryMethodsDescription;
      packagesToTest.add(Reflection.getPackageName(declaringClass));
    }

    /**
     * Specifies that only the methods that are declared to return {@code returnType} or its subtype
     * are tested.
     *
     * @return this tester object
     */
    @CanIgnoreReturnValue
    public FactoryMethodReturnValueTester thatReturn(Class<?> returnType) {
      this.returnTypeToTest = returnType;
      return this;
    }

    /**
     * Tests null checks against the instance methods of the return values, if any.
     *
     * <p>Test fails if default value cannot be determined for a constructor or factory method
     * parameter, or if the constructor or factory method throws exception.
     *
     * @return this tester
     */
    @CanIgnoreReturnValue
    public FactoryMethodReturnValueTester testNulls() throws Exception {
      for (Invokable<?, ?> factory : getFactoriesToTest()) {
        Object instance = instantiate(factory);
        if (instance != null
            && packagesToTest.contains(Reflection.getPackageName(instance.getClass()))) {
          try {
            nullPointerTester.testAllPublicInstanceMethods(instance);
          } catch (AssertionError e) {
            AssertionError error =
                new AssertionFailedError("Null check failed on return value of " + factory);
            error.initCause(e);
            throw error;
          }
        }
      }
      return this;
    }

    /**
     * Tests {@link Object#equals} and {@link Object#hashCode} against the return values of the
     * static methods, by asserting that when equal parameters are passed to the same static method,
     * the return value should also be equal; and vice versa.
     *
     * <p>Test fails if default value cannot be determined for a constructor or factory method
     * parameter, or if the constructor or factory method throws exception.
     *
     * @return this tester
     */
    @CanIgnoreReturnValue
    public FactoryMethodReturnValueTester testEquals() throws Exception {
      for (Invokable<?, ?> factory : getFactoriesToTest()) {
        try {
          testEqualsUsing(factory);
        } catch (FactoryMethodReturnsNullException e) {
          // If the factory returns null, we just skip it.
        }
      }
      return this;
    }

    /**
     * Runs serialization test on the return values of the static methods.
     *
     * <p>Test fails if default value cannot be determined for a constructor or factory method
     * parameter, or if the constructor or factory method throws exception.
     *
     * @return this tester
     */
    @CanIgnoreReturnValue
    @SuppressWarnings("CatchingUnchecked") // sneaky checked exception
    public FactoryMethodReturnValueTester testSerializable() throws Exception {
      for (Invokable<?, ?> factory : getFactoriesToTest()) {
        Object instance = instantiate(factory);
        if (instance != null) {
          try {
            SerializableTester.reserialize(instance);
          } catch (Exception e) { // sneaky checked exception
            AssertionError error =
                new AssertionFailedError("Serialization failed on return value of " + factory);
            error.initCause(e.getCause());
            throw error;
          }
        }
      }
      return this;
    }

    /**
     * Runs equals and serialization test on the return values.
     *
     * <p>Test fails if default value cannot be determined for a constructor or factory method
     * parameter, or if the constructor or factory method throws exception.
     *
     * @return this tester
     */
    @CanIgnoreReturnValue
    @SuppressWarnings("CatchingUnchecked") // sneaky checked exception
    public FactoryMethodReturnValueTester testEqualsAndSerializable() throws Exception {
      for (Invokable<?, ?> factory : getFactoriesToTest()) {
        try {
          testEqualsUsing(factory);
        } catch (FactoryMethodReturnsNullException e) {
          // If the factory returns null, we just skip it.
        }
        Object instance = instantiate(factory);
        if (instance != null) {
          try {
            SerializableTester.reserializeAndAssert(instance);
          } catch (Exception e) { // sneaky checked exception
            AssertionError error =
                new AssertionFailedError("Serialization failed on return value of " + factory);
            error.initCause(e.getCause());
            throw error;
          } catch (AssertionFailedError e) {
            AssertionError error =
                new AssertionFailedError(
                    "Return value of " + factory + " reserialized to an unequal value");
            error.initCause(e);
            throw error;
          }
        }
      }
      return this;
    }

    private ImmutableList<Invokable<?, ?>> getFactoriesToTest() {
      ImmutableList.Builder<Invokable<?, ?>> builder = ImmutableList.builder();
      for (Invokable<?, ?> factory : factories) {
        if (returnTypeToTest.isAssignableFrom(factory.getReturnType().getRawType())) {
          builder.add(factory);
        }
      }
      ImmutableList<Invokable<?, ?>> factoriesToTest = builder.build();
      Assert.assertFalse(
          "No "
              + factoryMethodsDescription
              + " that return "
              + returnTypeToTest.getName()
              + " or subtype are found in "
              + declaringClass
              + ".",
          factoriesToTest.isEmpty());
      return factoriesToTest;
    }
  }

  private void testEqualsUsing(final Invokable<?, ?> factory)
      throws ParameterNotInstantiableException, ParameterHasNoDistinctValueException,
          IllegalAccessException, InvocationTargetException, FactoryMethodReturnsNullException {
    List<Parameter> params = factory.getParameters();
    List<FreshValueGenerator> argGenerators = Lists.newArrayListWithCapacity(params.size());
    List<@Nullable Object> args = Lists.newArrayListWithCapacity(params.size());
    for (Parameter param : params) {
      FreshValueGenerator generator = newFreshValueGenerator();
      argGenerators.add(generator);
      args.add(generateDummyArg(param, generator));
    }
    Object instance = createInstance(factory, args);
    List<Object> equalArgs = generateEqualFactoryArguments(factory, params, args);
    // Each group is a List of items, each item has a list of factory args.
    final List<List<List<Object>>> argGroups = Lists.newArrayList();
    argGroups.add(ImmutableList.of(args, equalArgs));
    EqualsTester tester =
        new EqualsTester(
            new ItemReporter() {
              @Override
              String reportItem(Item<?> item) {
                List<Object> factoryArgs = argGroups.get(item.groupNumber).get(item.itemNumber);
                return factory.getName()
                    + "("
                    + Joiner.on(", ").useForNull("null").join(factoryArgs)
                    + ")";
              }
            });
    tester.addEqualityGroup(instance, createInstance(factory, equalArgs));
    for (int i = 0; i < params.size(); i++) {
      List<Object> newArgs = Lists.newArrayList(args);
      Object newArg = argGenerators.get(i).generateFresh(params.get(i).getType());

      if (newArg == null || Objects.equal(args.get(i), newArg)) {
        if (params.get(i).getType().getRawType().isEnum()) {
          continue; // Nothing better we can do if it's single-value enum
        }
        throw new ParameterHasNoDistinctValueException(params.get(i));
      }
      newArgs.set(i, newArg);
      tester.addEqualityGroup(createInstance(factory, newArgs));
      argGroups.add(ImmutableList.of(newArgs));
    }
    tester.testEquals();
  }

  /**
   * Returns dummy factory arguments that are equal to {@code args} but may be different instances,
   * to be used to construct a second instance of the same equality group.
   */
  private List<Object> generateEqualFactoryArguments(
      Invokable<?, ?> factory, List<Parameter> params, List<Object> args)
      throws ParameterNotInstantiableException, FactoryMethodReturnsNullException,
          InvocationTargetException, IllegalAccessException {
    List<Object> equalArgs = Lists.newArrayList(args);
    for (int i = 0; i < args.size(); i++) {
      Parameter param = params.get(i);
      Object arg = args.get(i);
      // Use new fresh value generator because 'args' were populated with new fresh generator each.
      // Two newFreshValueGenerator() instances should normally generate equal value sequence.
      Object shouldBeEqualArg = generateDummyArg(param, newFreshValueGenerator());
      if (arg != shouldBeEqualArg
          && Objects.equal(arg, shouldBeEqualArg)
          && hashCodeInsensitiveToArgReference(factory, args, i, shouldBeEqualArg)
          && hashCodeInsensitiveToArgReference(
              factory, args, i, generateDummyArg(param, newFreshValueGenerator()))) {
        // If the implementation uses identityHashCode(), referential equality is
        // probably intended. So no point in using an equal-but-different factory argument.
        // We check twice to avoid confusion caused by accidental hash collision.
        equalArgs.set(i, shouldBeEqualArg);
      }
    }
    return equalArgs;
  }

  private static boolean hashCodeInsensitiveToArgReference(
      Invokable<?, ?> factory, List<Object> args, int i, Object alternateArg)
      throws FactoryMethodReturnsNullException, InvocationTargetException, IllegalAccessException {
    List<Object> tentativeArgs = Lists.newArrayList(args);
    tentativeArgs.set(i, alternateArg);
    return createInstance(factory, tentativeArgs).hashCode()
        == createInstance(factory, args).hashCode();
  }

  // distinctValues is a type-safe class-values mapping, but we don't have a type-safe data
  // structure to hold the mappings.
  @SuppressWarnings({"unchecked", "rawtypes"})
  private FreshValueGenerator newFreshValueGenerator() {
    FreshValueGenerator generator =
        new FreshValueGenerator() {
          @Override
          Object interfaceMethodCalled(Class<?> interfaceType, Method method) {
            return getDummyValue(TypeToken.of(interfaceType).method(method).getReturnType());
          }
        };
    for (Entry<Class<?>, Collection<Object>> entry : distinctValues.asMap().entrySet()) {
      generator.addSampleInstances((Class) entry.getKey(), entry.getValue());
    }
    return generator;
  }

  private static @Nullable Object generateDummyArg(Parameter param, FreshValueGenerator generator)
      throws ParameterNotInstantiableException {
    if (isNullable(param)) {
      return null;
    }
    Object arg = generator.generateFresh(param.getType());
    if (arg == null) {
      throw new ParameterNotInstantiableException(param);
    }
    return arg;
  }

  private static <X extends Throwable> void throwFirst(List<X> exceptions) throws X {
    if (!exceptions.isEmpty()) {
      throw exceptions.get(0);
    }
  }

  /** Factories with the least number of parameters are listed first. */
  private static <T> ImmutableList<Invokable<?, ? extends T>> getFactories(TypeToken<T> type) {
    List<Invokable<?, ? extends T>> factories = Lists.newArrayList();
    for (Method method : type.getRawType().getDeclaredMethods()) {
      Invokable<?, ?> invokable = type.method(method);
      if (!invokable.isPrivate()
          && !invokable.isSynthetic()
          && invokable.isStatic()
          && type.isSupertypeOf(invokable.getReturnType())) {
        @SuppressWarnings("unchecked") // guarded by isAssignableFrom()
        Invokable<?, ? extends T> factory = (Invokable<?, ? extends T>) invokable;
        factories.add(factory);
      }
    }
    if (!Modifier.isAbstract(type.getRawType().getModifiers())) {
      for (Constructor<?> constructor : type.getRawType().getDeclaredConstructors()) {
        Invokable<T, T> invokable = type.constructor(constructor);
        if (!invokable.isPrivate() && !invokable.isSynthetic()) {
          factories.add(invokable);
        }
      }
    }
    for (Invokable<?, ?> factory : factories) {
      factory.setAccessible(true);
    }
    // Sorts methods/constructors with the least number of parameters first since it's likely easier
    // to fill dummy parameter values for them. Ties are broken by name then by the string form of
    // the parameter list.
    return BY_NUMBER_OF_PARAMETERS
        .compound(BY_METHOD_NAME)
        .compound(BY_PARAMETERS)
        .immutableSortedCopy(factories);
  }

  private List<Object> getDummyArguments(Invokable<?, ?> invokable)
      throws ParameterNotInstantiableException {
    List<Object> args = Lists.newArrayList();
    for (Parameter param : invokable.getParameters()) {
      if (isNullable(param)) {
        args.add(null);
        continue;
      }
      Object defaultValue = getDummyValue(param.getType());
      if (defaultValue == null) {
        throw new ParameterNotInstantiableException(param);
      }
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
      return new SerializableDummyProxy(this).newProxy(type);
    }
    return null;
  }

  private static <T> T createInstance(Invokable<?, ? extends T> factory, List<?> args)
      throws FactoryMethodReturnsNullException, InvocationTargetException, IllegalAccessException {
    T instance = invoke(factory, args);
    if (instance == null) {
      throw new FactoryMethodReturnsNullException(factory);
    }
    return instance;
  }

  private static <T> @Nullable T invoke(Invokable<?, ? extends T> factory, List<?> args)
      throws InvocationTargetException, IllegalAccessException {
    T returnValue = factory.invoke(null, args.toArray());
    if (returnValue == null) {
      Assert.assertTrue(
          factory + " returns null but it's not annotated with @Nullable", isNullable(factory));
    }
    return returnValue;
  }

  /**
   * Thrown if the test tries to invoke a constructor or static factory method but failed because
   * the dummy value of a constructor or method parameter is unknown.
   */
  @VisibleForTesting
  static class ParameterNotInstantiableException extends Exception {
    public ParameterNotInstantiableException(Parameter parameter) {
      super(
          "Cannot determine value for parameter "
              + parameter
              + " of "
              + parameter.getDeclaringInvokable());
    }
  }

  /**
   * Thrown if the test fails to generate two distinct non-null values of a constructor or factory
   * parameter in order to test {@link Object#equals} and {@link Object#hashCode} of the declaring
   * class.
   */
  @VisibleForTesting
  static class ParameterHasNoDistinctValueException extends Exception {
    ParameterHasNoDistinctValueException(Parameter parameter) {
      super(
          "Cannot generate distinct value for parameter "
              + parameter
              + " of "
              + parameter.getDeclaringInvokable());
    }
  }

  /**
   * Thrown if the test tries to invoke a static factory method to test instance methods but the
   * factory returned null.
   */
  @VisibleForTesting
  static class FactoryMethodReturnsNullException extends Exception {
    public FactoryMethodReturnsNullException(Invokable<?, ?> factory) {
      super(factory + " returns null and cannot be used to test instance methods.");
    }
  }

  private static final class SerializableDummyProxy extends DummyProxy implements Serializable {

    private final transient ClassSanityTester tester;

    SerializableDummyProxy(ClassSanityTester tester) {
      this.tester = tester;
    }

    @Override
    <R> R dummyReturnValue(TypeToken<R> returnType) {
      return tester.getDummyValue(returnType);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      return obj instanceof SerializableDummyProxy;
    }

    @Override
    public int hashCode() {
      return 0;
    }
  }
}
