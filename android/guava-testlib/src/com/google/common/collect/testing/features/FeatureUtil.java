/*
 * Copyright (C) 2008 The Guava Authors
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

package com.google.common.collect.testing.features;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.Helpers;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Utilities for collecting and validating tester requirements from annotations.
 *
 * @author George van den Driessche
 */
@GwtIncompatible
public class FeatureUtil {
  /** A cache of annotated objects (typically a Class or Method) to its set of annotations. */
  private static Map<AnnotatedElement, List<Annotation>> annotationCache = new HashMap<>();

  private static final Map<Class<?>, TesterRequirements> classTesterRequirementsCache =
      new HashMap<>();

  private static final Map<Method, TesterRequirements> methodTesterRequirementsCache =
      new HashMap<>();

  /**
   * Given a set of features, add to it all the features directly or indirectly implied by any of
   * them, and return it.
   *
   * @param features the set of features to expand
   * @return the same set of features, expanded with all implied features
   */
  public static Set<Feature<?>> addImpliedFeatures(Set<Feature<?>> features) {
    Queue<Feature<?>> queue = new ArrayDeque<>(features);
    while (!queue.isEmpty()) {
      Feature<?> feature = queue.remove();
      for (Feature<?> implied : feature.getImpliedFeatures()) {
        if (features.add(implied)) {
          queue.add(implied);
        }
      }
    }
    return features;
  }

  /**
   * Given a set of features, return a new set of all features directly or indirectly implied by any
   * of them.
   *
   * @param features the set of features whose implications to find
   * @return the implied set of features
   */
  public static Set<Feature<?>> impliedFeatures(Set<Feature<?>> features) {
    Set<Feature<?>> impliedSet = new LinkedHashSet<>();
    Queue<Feature<?>> queue = new ArrayDeque<>(features);
    while (!queue.isEmpty()) {
      Feature<?> feature = queue.remove();
      for (Feature<?> implied : feature.getImpliedFeatures()) {
        if (!features.contains(implied) && impliedSet.add(implied)) {
          queue.add(implied);
        }
      }
    }
    return impliedSet;
  }

  /**
   * Get the full set of requirements for a tester class.
   *
   * @param testerClass a tester class
   * @return all the constraints implicitly or explicitly required by the class or any of its
   *     superclasses.
   * @throws ConflictingRequirementsException if the requirements are mutually inconsistent.
   */
  public static TesterRequirements getTesterRequirements(Class<?> testerClass)
      throws ConflictingRequirementsException {
    synchronized (classTesterRequirementsCache) {
      TesterRequirements requirements = classTesterRequirementsCache.get(testerClass);
      if (requirements == null) {
        requirements = buildTesterRequirements(testerClass);
        classTesterRequirementsCache.put(testerClass, requirements);
      }
      return requirements;
    }
  }

  /**
   * Get the full set of requirements for a tester class.
   *
   * @param testerMethod a test method of a tester class
   * @return all the constraints implicitly or explicitly required by the method, its declaring
   *     class, or any of its superclasses.
   * @throws ConflictingRequirementsException if the requirements are mutually inconsistent.
   */
  public static TesterRequirements getTesterRequirements(Method testerMethod)
      throws ConflictingRequirementsException {
    synchronized (methodTesterRequirementsCache) {
      TesterRequirements requirements = methodTesterRequirementsCache.get(testerMethod);
      if (requirements == null) {
        requirements = buildTesterRequirements(testerMethod);
        methodTesterRequirementsCache.put(testerMethod, requirements);
      }
      return requirements;
    }
  }

  /**
   * Construct the full set of requirements for a tester class.
   *
   * @param testerClass a tester class
   * @return all the constraints implicitly or explicitly required by the class or any of its
   *     superclasses.
   * @throws ConflictingRequirementsException if the requirements are mutually inconsistent.
   */
  static TesterRequirements buildTesterRequirements(Class<?> testerClass)
      throws ConflictingRequirementsException {
    final TesterRequirements declaredRequirements = buildDeclaredTesterRequirements(testerClass);
    Class<?> baseClass = testerClass.getSuperclass();
    if (baseClass == null) {
      return declaredRequirements;
    } else {
      final TesterRequirements clonedBaseRequirements =
          new TesterRequirements(getTesterRequirements(baseClass));
      return incorporateRequirements(clonedBaseRequirements, declaredRequirements, testerClass);
    }
  }

  /**
   * Construct the full set of requirements for a tester method.
   *
   * @param testerMethod a test method of a tester class
   * @return all the constraints implicitly or explicitly required by the method, its declaring
   *     class, or any of its superclasses.
   * @throws ConflictingRequirementsException if the requirements are mutually inconsistent.
   */
  static TesterRequirements buildTesterRequirements(Method testerMethod)
      throws ConflictingRequirementsException {
    TesterRequirements clonedClassRequirements =
        new TesterRequirements(getTesterRequirements(testerMethod.getDeclaringClass()));
    TesterRequirements declaredRequirements = buildDeclaredTesterRequirements(testerMethod);
    return incorporateRequirements(clonedClassRequirements, declaredRequirements, testerMethod);
  }

  /**
   * Find all the constraints explicitly or implicitly specified by a single tester annotation.
   *
   * @param testerAnnotation a tester annotation
   * @return the requirements specified by the annotation
   * @throws ConflictingRequirementsException if the requirements are mutually inconsistent.
   */
  private static TesterRequirements buildTesterRequirements(Annotation testerAnnotation)
      throws ConflictingRequirementsException {
    Class<? extends Annotation> annotationClass = testerAnnotation.annotationType();
    final Feature<?>[] presentFeatures;
    final Feature<?>[] absentFeatures;
    try {
      presentFeatures = (Feature[]) annotationClass.getMethod("value").invoke(testerAnnotation);
      absentFeatures = (Feature[]) annotationClass.getMethod("absent").invoke(testerAnnotation);
    } catch (Exception e) {
      throw new IllegalArgumentException("Error extracting features from tester annotation.", e);
    }
    Set<Feature<?>> allPresentFeatures =
        addImpliedFeatures(Helpers.<Feature<?>>copyToSet(presentFeatures));
    Set<Feature<?>> allAbsentFeatures =
        addImpliedFeatures(Helpers.<Feature<?>>copyToSet(absentFeatures));
    if (!Collections.disjoint(allPresentFeatures, allAbsentFeatures)) {
      throw new ConflictingRequirementsException(
          "Annotation explicitly or "
              + "implicitly requires one or more features to be both present "
              + "and absent.",
          intersection(allPresentFeatures, allAbsentFeatures),
          testerAnnotation);
    }
    return new TesterRequirements(allPresentFeatures, allAbsentFeatures);
  }

  /**
   * Construct the set of requirements specified by annotations directly on a tester class or
   * method.
   *
   * @param classOrMethod a tester class or a test method thereof
   * @return all the constraints implicitly or explicitly required by annotations on the class or
   *     method.
   * @throws ConflictingRequirementsException if the requirements are mutually inconsistent.
   */
  public static TesterRequirements buildDeclaredTesterRequirements(AnnotatedElement classOrMethod)
      throws ConflictingRequirementsException {
    TesterRequirements requirements = new TesterRequirements();

    Iterable<Annotation> testerAnnotations = getTesterAnnotations(classOrMethod);
    for (Annotation testerAnnotation : testerAnnotations) {
      TesterRequirements moreRequirements = buildTesterRequirements(testerAnnotation);
      incorporateRequirements(requirements, moreRequirements, testerAnnotation);
    }

    return requirements;
  }

  /**
   * Find all the tester annotations declared on a tester class or method.
   *
   * @param classOrMethod a class or method whose tester annotations to find
   * @return an iterable sequence of tester annotations on the class
   */
  public static Iterable<Annotation> getTesterAnnotations(AnnotatedElement classOrMethod) {
    synchronized (annotationCache) {
      List<Annotation> annotations = annotationCache.get(classOrMethod);
      if (annotations == null) {
        annotations = new ArrayList<>();
        for (Annotation a : classOrMethod.getDeclaredAnnotations()) {
          if (a.annotationType().isAnnotationPresent(TesterAnnotation.class)) {
            annotations.add(a);
          }
        }
        annotations = Collections.unmodifiableList(annotations);
        annotationCache.put(classOrMethod, annotations);
      }
      return annotations;
    }
  }

  /**
   * Incorporate additional requirements into an existing requirements object.
   *
   * @param requirements the existing requirements object
   * @param moreRequirements more requirements to incorporate
   * @param source the source of the additional requirements (used only for error reporting)
   * @return the existing requirements object, modified to include the additional requirements
   * @throws ConflictingRequirementsException if the additional requirements are inconsistent with
   *     the existing requirements
   */
  private static TesterRequirements incorporateRequirements(
      TesterRequirements requirements, TesterRequirements moreRequirements, Object source)
      throws ConflictingRequirementsException {
    Set<Feature<?>> presentFeatures = requirements.getPresentFeatures();
    Set<Feature<?>> absentFeatures = requirements.getAbsentFeatures();
    Set<Feature<?>> morePresentFeatures = moreRequirements.getPresentFeatures();
    Set<Feature<?>> moreAbsentFeatures = moreRequirements.getAbsentFeatures();
    checkConflict("absent", absentFeatures, "present", morePresentFeatures, source);
    checkConflict("present", presentFeatures, "absent", moreAbsentFeatures, source);
    presentFeatures.addAll(morePresentFeatures);
    absentFeatures.addAll(moreAbsentFeatures);
    return requirements;
  }

  // Used by incorporateRequirements() only
  private static void checkConflict(
      String earlierRequirement,
      Set<Feature<?>> earlierFeatures,
      String newRequirement,
      Set<Feature<?>> newFeatures,
      Object source)
      throws ConflictingRequirementsException {
    if (!Collections.disjoint(newFeatures, earlierFeatures)) {
      throw new ConflictingRequirementsException(
          String.format(
              Locale.ROOT,
              "Annotation requires to be %s features that earlier "
                  + "annotations required to be %s.",
              newRequirement,
              earlierRequirement),
          intersection(newFeatures, earlierFeatures),
          source);
    }
  }

  /** Construct a new {@link java.util.Set} that is the intersection of the given sets. */
  public static <T> Set<T> intersection(Set<? extends T> set1, Set<? extends T> set2) {
    Set<T> result = Helpers.<T>copyToSet(set1);
    result.retainAll(set2);
    return result;
  }
}
