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

package com.google.common.math;

import static com.google.common.math.StatsTesting.assertDiagonalLinearTransformation;
import static com.google.common.math.StatsTesting.assertHorizontalLinearTransformation;
import static com.google.common.math.StatsTesting.assertLinearTransformationNaN;
import static com.google.common.math.StatsTesting.assertVerticalLinearTransformation;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import junit.framework.TestCase;

/**
 * Tests for {@link LinearTransformation}.
 *
 * @author Pete Gillin
 */
public class LinearTransformationTest extends TestCase {

  private static final double ALLOWED_ERROR = 1e-10;

  public void testMappingAnd_regular() {
    double x1 = 1.2;
    double y1 = 3.4;
    double xDelta = 5.6;
    double yDelta = 7.8;
    LinearTransformation transformation =
        LinearTransformation.mapping(x1, y1).and(x1 + xDelta, y1 + yDelta);
    assertDiagonalLinearTransformation(transformation, x1, y1, xDelta, yDelta);
  }

  public void testMappingAnd_horizontal() {
    double x1 = 1.2;
    double xDelta = 3.4;
    double y = 5.6;
    LinearTransformation transformation = LinearTransformation.mapping(x1, y).and(x1 + xDelta, y);
    assertHorizontalLinearTransformation(transformation, y);
  }

  public void testMappingAnd_vertical() {
    double x = 1.2;
    double y1 = 3.4;
    double yDelta = 5.6;
    LinearTransformation transformation = LinearTransformation.mapping(x, y1).and(x, y1 + yDelta);
    assertVerticalLinearTransformation(transformation, x);
  }

  public void testMapping_infiniteX1() {
    assertThrows(
        IllegalArgumentException.class,
        () -> LinearTransformation.mapping(Double.POSITIVE_INFINITY, 3.4));
  }

  public void testMapping_infiniteY1() {
    assertThrows(
        IllegalArgumentException.class,
        () -> LinearTransformation.mapping(1.2, Double.NEGATIVE_INFINITY));
  }

  public void testMappingAnd_infiniteX2() {
    assertThrows(
        IllegalArgumentException.class,
        () -> LinearTransformation.mapping(1.2, 3.4).and(Double.NEGATIVE_INFINITY, 7.8));
  }

  public void testMappingAnd_infiniteY2() {
    assertThrows(
        IllegalArgumentException.class,
        () -> LinearTransformation.mapping(1.2, 3.4).and(5.6, Double.POSITIVE_INFINITY));
  }

  public void testMapping_nanX1() {
    assertThrows(
        IllegalArgumentException.class, () -> LinearTransformation.mapping(Double.NaN, 3.4));
  }

  public void testMapping_nanY1() {
    assertThrows(
        IllegalArgumentException.class, () -> LinearTransformation.mapping(1.2, Double.NaN));
  }

  public void testMappingAnd_nanX2() {
    assertThrows(
        IllegalArgumentException.class,
        () -> LinearTransformation.mapping(1.2, 3.4).and(Double.NaN, 7.8));
  }

  public void testMappingAnd_nanY2() {
    assertThrows(
        IllegalArgumentException.class,
        () -> LinearTransformation.mapping(1.2, 3.4).and(5.6, Double.NaN));
  }

  public void testMappingAnd_samePointTwice() {
    double x = 1.2;
    double y = 3.4;
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          LinearTransformation.mapping(x, y).and(x, y);
        });
  }

  public void testMappingWithSlope_regular() {
    double x1 = 1.2;
    double y1 = 3.4;
    double xDelta = -5.6;
    double slope = -7.8;
    LinearTransformation transformation = LinearTransformation.mapping(x1, y1).withSlope(slope);
    assertDiagonalLinearTransformation(transformation, x1, y1, xDelta, xDelta * slope);
  }

  public void testMappingWithSlope_horizontal() {
    double x1 = 1.2;
    double y = 5.6;
    LinearTransformation transformation = LinearTransformation.mapping(x1, y).withSlope(0.0);
    assertHorizontalLinearTransformation(transformation, y);
  }

  public void testMappingWithSlope_vertical() {
    double x = 1.2;
    double y1 = 3.4;
    LinearTransformation transformation =
        LinearTransformation.mapping(x, y1).withSlope(Double.POSITIVE_INFINITY);
    assertVerticalLinearTransformation(transformation, x);
  }

  public void testMappingWithSlope_minimalSlope() {
    double x1 = 1.2;
    double y1 = 3.4;
    double slope = Double.MIN_VALUE;
    LinearTransformation transformation = LinearTransformation.mapping(x1, y1).withSlope(slope);
    assertThat(transformation.isVertical()).isFalse();
    assertThat(transformation.isHorizontal()).isFalse();
    assertThat(transformation.slope()).isWithin(ALLOWED_ERROR).of(slope);
    // Note that we cannot test the actual mapping of points, as the results will be unreliable due
    // to loss of precision with this value of the slope.
  }

  public void testMappingWithSlope_maximalSlope() {
    double x1 = 1.2;
    double y1 = 3.4;
    double slope = Double.MAX_VALUE;
    LinearTransformation transformation = LinearTransformation.mapping(x1, y1).withSlope(slope);
    assertThat(transformation.isVertical()).isFalse();
    assertThat(transformation.isHorizontal()).isFalse();
    assertThat(transformation.slope()).isWithin(ALLOWED_ERROR).of(slope);
    // Note that we cannot test the actual mapping of points, as the results will be unreliable due
    // to loss of precision with this value of the slope.
  }

  public void testMappingWithSlope_nanSlope() {
    assertThrows(
        IllegalArgumentException.class,
        () -> LinearTransformation.mapping(1.2, 3.4).withSlope(Double.NaN));
  }

  public void testVertical_regular() {
    double x = 1.2;
    LinearTransformation transformation = LinearTransformation.vertical(x);
    assertVerticalLinearTransformation(transformation, x);
  }

  public void testVertical_infiniteX() {
    assertThrows(
        IllegalArgumentException.class,
        () -> LinearTransformation.vertical(Double.NEGATIVE_INFINITY));
  }

  public void testVertical_nanX() {
    assertThrows(IllegalArgumentException.class, () -> LinearTransformation.vertical(Double.NaN));
  }

  public void testHorizontal_regular() {
    double y = 1.2;
    LinearTransformation transformation = LinearTransformation.horizontal(y);
    assertHorizontalLinearTransformation(transformation, y);
  }

  public void testHorizontal_infiniteY() {
    assertThrows(
        IllegalArgumentException.class,
        () -> LinearTransformation.horizontal(Double.POSITIVE_INFINITY));
  }

  public void testHorizontal_nanY() {
    assertThrows(IllegalArgumentException.class, () -> LinearTransformation.horizontal(Double.NaN));
  }

  public void testForNaN() {
    LinearTransformation transformation = LinearTransformation.forNaN();
    assertLinearTransformationNaN(transformation);
  }
}
