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
    try {
      LinearTransformation.mapping(Double.POSITIVE_INFINITY, 3.4);
      fail("Expected IllegalArgumentException from mapping(x, y) with infinite x");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMapping_infiniteY1() {
    try {
      LinearTransformation.mapping(1.2, Double.NEGATIVE_INFINITY);
      fail("Expected IllegalArgumentException from mapping(x, y) with infinite y");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMappingAnd_infiniteX2() {
    try {
      LinearTransformation.mapping(1.2, 3.4).and(Double.NEGATIVE_INFINITY, 7.8);
      fail("Expected IllegalArgumentException from and(x, y) with infinite x");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMappingAnd_infiniteY2() {
    try {
      LinearTransformation.mapping(1.2, 3.4).and(5.6, Double.POSITIVE_INFINITY);
      fail("Expected IllegalArgumentException from and(x, y) with infinite y");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMapping_nanX1() {
    try {
      LinearTransformation.mapping(Double.NaN, 3.4);
      fail("Expected IllegalArgumentException from mapping(x, y) with NaN x");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMapping_nanY1() {
    try {
      LinearTransformation.mapping(1.2, Double.NaN);
      fail("Expected IllegalArgumentException from mapping(x, y) with NaN y");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMappingAnd_nanX2() {
    try {
      LinearTransformation.mapping(1.2, 3.4).and(Double.NaN, 7.8);
      fail("Expected IllegalArgumentException from and(x, y) with NaN x");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMappingAnd_nanY2() {
    try {
      LinearTransformation.mapping(1.2, 3.4).and(5.6, Double.NaN);
      fail("Expected IllegalArgumentException from and(x, y) with NaN y");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMappingAnd_samePointTwice() {
    try {
      double x = 1.2;
      double y = 3.4;
      LinearTransformation.mapping(x, y).and(x, y);
      fail(
          "Expected IllegalArgumentException from mapping(x1, y1).and(x2, y2) with"
              + " (x1 == x2) && (y1 == y2)");
    } catch (IllegalArgumentException expected) {
    }
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
    try {
      LinearTransformation.mapping(1.2, 3.4).withSlope(Double.NaN);
      fail("Expected IllegalArgumentException from withSlope(slope) with NaN slope");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testVertical_regular() {
    double x = 1.2;
    LinearTransformation transformation = LinearTransformation.vertical(x);
    assertVerticalLinearTransformation(transformation, x);
  }

  public void testVertical_infiniteX() {
    try {
      LinearTransformation.vertical(Double.NEGATIVE_INFINITY);
      fail("Expected IllegalArgumentException from vertical(x) with infinite x");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testVertical_nanX() {
    try {
      LinearTransformation.vertical(Double.NaN);
      fail("Expected IllegalArgumentException from vertical(x) with NaN x");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testHorizontal_regular() {
    double y = 1.2;
    LinearTransformation transformation = LinearTransformation.horizontal(y);
    assertHorizontalLinearTransformation(transformation, y);
  }

  public void testHorizontal_infiniteY() {
    try {
      LinearTransformation.horizontal(Double.POSITIVE_INFINITY);
      fail("Expected IllegalArgumentException from horizontal(y) with infinite y");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testHorizontal_nanY() {
    try {
      LinearTransformation.horizontal(Double.NaN);
      fail("Expected IllegalArgumentException from horizontal(y) with NaN y");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testForNaN() {
    LinearTransformation transformation = LinearTransformation.forNaN();
    assertLinearTransformationNaN(transformation);
  }
}
