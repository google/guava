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

import com.google.common.collect.testing.Helpers;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utilities for collecting and validating tester requirements from annotations.
 *
 * @author George van den Driessche
 */
public class FeatureUtil {
  /**
   * A cache of annotated objects (typically a Class or Method) to its
   * set of annotations.
   */
  private static Map<AnnotatedElement, Annotation[]> annotationCache =
      new HashMap<AnnotatedElement, Annotation[]>();

  private static final Map<Class<?>, TesterRequirements>
      classTesterRequirementsCache =
          new HashMap<Class<?>, TesterRequirements>();

  /**
   * Given a set of features, add to it all the features directly or indirectly
   * implied by any of them, and return it.
   * @param features the set of features to expand
   * @return the same set of features, expanded with all implied features
   */
  public static Set<Feature<?>> addImpliedFeatures(Set<Feature<?>> features) {
    // The base case of the recursion is an empty set of features, which will
    // occur when the previous set contained only simple features.
    if (!features.isEmpty()) {
      features.addAll(impliedFeatures(features));
    }
    return features;
  }

  /**
   * Given a set of features, return a new set of all features directly or
   * indirectly implied by any of them.
   * @param features the set of features whose implications to find
   * @return the implied set of features
   */
  public static Set<Feature<?>> impliedFeatures(Set<Feature<?>> features) {
    Set<Feature<?>> implied = new LinkedHashSet<Feature<?>>();
    for (Feature<?> feature : features) {
      implied.addAll(feature.getImpliedFeatures());
    }
    addImpliedFeatures(implied);
    return implied;
  }

  /**
   * Get the full set of requirements for a tester class.
   * @param testerClass a tester class
   * @return all the constraints implicitly or explicitly required by the class
   * or any of its superclasses.
   * @throws ConflictingRequirementsException if the requirements are mutually
   * inconsistent.
   */
  public static TesterRequirements getTesterRequirements(Class<?> testerClass)
      throws ConflictingRequirementsException {
    synchronized (classTesterRequirementsCache) {
      TesterRequirements requirements =
          classTesterRequirementsCache.get(testerClass);
      if (requirements == null) {
        requirements = buildTesterRequirements(testerClass);
        classTesterRequirementsCache.put(testerClass, requirements);
      }
      return requirements;
    }
  }

  /**
   * Get the full set of requirements for a tester class.
   * @param testerMethod a test method of a tester class
   * @return all the constraints implicitly or explicitly required by the
   * method, its declaring class, or any of its superclasses.
   * @throws ConflictingRequirementsException if the requirements are
   * mutually inconsistent.
   */
  public static TesterRequirements getTesterRequirements(Method testerMethod)
      throws ConflictingRequirementsException {
    return buildTesterRequirements(testerMethod);
  }

  /**
   * Construct the full set of requirements for a tester class.
   * @param testerClass a tester class
   * @return all the constraints implicitly or explicitly required by the class
   * or any of its superclasses.
   * @throws ConflictingRequirementsException if the requirements are mutually
   * inconsistent.
   */
  static TesterRequirements buildTesterRequirements(Class<?> testerClass)
      throws ConflictingRequirementsException {
    final TesterRequirements declaredRequirements =
        buildDeclaredTesterRequirements(testerClass);
    Class<?> baseClass = testerClass.getSuperclass();
    if (baseClass == null) {
      return declaredRequirements;
    } else {
      final TesterRequirements clonedBaseRequirements =
          new TesterRequirements(getTesterRequirements(baseClass));
      return incorporateRequirements(
          clonedBaseRequirements, declaredRequirements, testerClass);
    }
  }

  /**
   * Construct the full set of requirements for a tester method.
   * @param testerMethod a test method of a tester class
   * @return all the constraints implicitly or explicitly required by the
   * method, its declaring class, or any of its superclasses.
   * @throws ConflictingRequirementsException if the requirements are mutually
   * inconsistent.
   */
  static TesterRequirements buildTesterRequirements(Method testerMethod)
      throws ConflictingRequirementsException {
    TesterRequirements clonedClassRequirements = new TesterRequirements(
        getTesterRequirements(testerMethod.getDeclaringClass()));
    TesterRequirements declaredRequirements =
        buildDeclaredTesterRequirements(testerMethod);
    return incorporateRequirements(
        clonedClassRequirements, declaredRequirements, testerMethod);
  }

  /**
   * Construct the set of requirements specified by annotations
   * directly on a tester class or method.
   * @param classOrMethod a tester class or a test method thereof
   * @return all the constraints implicitly or explicitly required by
   *         annotations on the class or method.
   * @throws ConflictingRequirementsException if the requirements are mutually
   *         inconsistent.
   */
  public static TesterRequirements buildDeclaredTesterRequirements(
      AnnotatedElement classOrMethod)
      throws ConflictingRequirementsException {
    TesterRequirements requirements = new TesterRequirements();

    Iterable<Annotation> testerAnnotations =
        getTesterAnnotations(classOrMethod);
    for (Annotation testerAnnotation : testerAnnotations) {
      TesterRequirements moreRequirements =
          buildTesterRequirements(testerAnnotation);
      incorporateRequirements(
          requirements, moreRequirements, testerAnnotation);
    }

    return requirements;
  }

  /**
   * Find all the tester annotations declared on a tester class or method.
   * @param classOrMethod a class or method whose tester annotations to find
   * @return an iterable sequence of tester annotations on the class
   */
  public static Iterable<Annotation> getTesterAnnotations(
      AnnotatedElement classOrMethod) {
    List<Annotation> result = new ArrayList<Annotation>();

    Annotation[] annotations;
    synchronized (annotationCache) {
      annotations = annotationCache.get(classOrMethod);
      if (annotations == null) {
        annotations = classOrMethod.getDeclaredAnnotations();
        annotationCache.put(classOrMethod, annotations);
      }
    }

    for (Annotation a : annotations) {
      Class<? extends Annotation> annotationClass = a.annotationType();
      if (annotationClass.isAnnotationPresent(TesterAnnotation.class)) {
        result.add(a);
      }
    }
    return result;
  }

  /**
   * Find all the constraints explicitly or implicitly specified by a single
   * tester annotation.
   * @param testerAnnotation a tester annotation
   * @return the requirements specified by the annotation
   * @throws ConflictingRequirementsException if the requirements are mutually
   *         inconsistent.
   */
  private static TesterRequirements buildTesterRequirements(
      Annotation testerAnnotation)
      throws ConflictingRequirementsException {
    Class<? extends Annotation> annotationClass = testerAnnotation.getClass();
    final Feature<?>[] presentFeatures;
    final Feature<?>[] absentFeatures;
    try {
      presentFeatures = (Feature[]) annotationClass.getMethod("value")
          .invoke(testerAnnotation);
      absentFeatures = (Feature[]) annotationClass.getMethod("absent")
          .invoke(testerAnnotation);
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Error extracting features from tester annotation.", e);
    }
    Set<Feature<?>> allPresentFeatures =
        addImpliedFeatures(Helpers.<Feature<?>>copyToSet(presentFeatures));
    Set<Feature<?>> allAbsentFeatures =
        addImpliedFeatures(Helpers.<Feature<?>>copyToSet(absentFeatures));
    Set<Feature<?>> conflictingFeatures =
        intersection(allPresentFeatures, allAbsentFeatures);
    if (!conflictingFeatures.isEmpty()) {
      throw new ConflictingRequirementsException("Annotation explicitly or " +
          "implicitly requires one or more features to be both present " +
          "and absent.",
          conflictingFeatures, testerAnnotation);
    }
    return new TesterRequirements(allPresentFeatures, allAbsentFeatures);
  }

  /**
   * Incorporate additional requirements into an existing requirements object.
   * @param requirements the existing requirements object
   * @param moreRequirements more requirements to incorporate
   * @param source the source of the additional requirements
   *        (used only for error reporting)
   * @return the existing requirements object, modified to include the
   *         additional requirements
   * @throws ConflictingRequirementsException if the additional requirements
   *         are inconsistent with the existing requirements
   */
  private static TesterRequirements incorporateRequirements(
      TesterRequirements requirements, TesterRequirements moreRequirements,
      Object source) throws ConflictingRequirementsException {
    Set<Feature<?>> presentFeatures = requirements.getPresentFeatures();
    Set<Feature<?>> absentFeatures = requirements.getAbsentFeatures();
    Set<Feature<?>> morePresentFeatures = moreRequirements.getPresentFeatures();
    Set<Feature<?>> moreAbsentFeatures = moreRequirements.getAbsentFeatures();
    checkConflict(
        "absent", absentFeatures,
        "present", morePresentFeatures, source);
    checkConflict(
        "present", presentFeatures,
        "absent", moreAbsentFeatures, source);
    presentFeatures.addAll(morePresentFeatures);
    absentFeatures.addAll(moreAbsentFeatures);
    return requirements;
  }

  // Used by incorporateRequirements() only
  private static void checkConflict(
      String earlierRequirement, Set<Feature<?>> earlierFeatures,
      String newRequirement, Set<Feature<?>> newFeatures,
      Object source) throws ConflictingRequirementsException {
    Set<Feature<?>> conflictingFeatures;
    conflictingFeatures = intersection(newFeatures, earlierFeatures);
    if (!conflictingFeatures.isEmpty()) {
      throw new ConflictingRequirementsException(String.format(
          "Annotation requires to be %s features that earlier " +
          "annotations required to be %s.",
              newRequirement, earlierRequirement),
          conflictingFeatures, source);
    }
  }

  /**
   * Construct a new {@link java.util.Set} that is the intersection
   * of the given sets.
   */
  // Calls generic varargs method.
  @SuppressWarnings("unchecked")
  public static <T> Set<T> intersection(
      Set<? extends T> set1, Set<? extends T> set2) {
    return intersection(new Set[] {set1, set2});
  }

  /**
   * Construct a new {@link java.util.Set} that is the intersection
   * of all the given sets.
   * @param sets the sets to intersect
   * @return the intersection of the sets
   * @throws java.lang.IllegalArgumentException if there are no sets to
   *         intersect
   */
  public static <T> Set<T> intersection(Set<? extends T> ... sets) {
    if (sets.length == 0) {
      throw new IllegalArgumentException(
          "Can't intersect no sets; would have to return the universe.");
    }
    Set<T> results = Helpers.copyToSet(sets[0]);
    for (int i = 1; i < sets.length; i++) {
      Set<? extends T> set = sets[i];
      results.retainAll(set);
    }
    return results;
  }
}
