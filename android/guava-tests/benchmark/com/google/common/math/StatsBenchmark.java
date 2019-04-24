/*
 * Copyright (C) 2013 The Guava Authors
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

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.caliper.api.SkipThisScenarioException;
import com.google.common.primitives.Doubles;
import java.util.Random;

/**
 * Benchmarks for various algorithms for computing the mean and/or variance.
 *
 * @author Louis Wasserman
 */
public class StatsBenchmark {

  enum MeanAlgorithm {
    SIMPLE {
      @Override
      double mean(double[] values) {
        double sum = 0.0;
        for (double value : values) {
          sum += value;
        }
        return sum / values.length;
      }
    },
    KAHAN {
      @Override
      double mean(double[] values) {
        double sum = 0.0;
        double c = 0.0;
        for (double value : values) {
          double y = value - c;
          double t = sum + y;
          c = (t - sum) - y;
          sum = t;
        }
        return sum / values.length;
      }
    },
    KNUTH {
      @Override
      double mean(double[] values) {
        double mean = values[0];
        for (int i = 1; i < values.length; i++) {
          mean = mean + (values[i] - mean) / (i + 1);
        }
        return mean;
      }
    };

    abstract double mean(double[] values);
  }

  static class MeanAndVariance {
    private final double mean;
    private final double variance;

    MeanAndVariance(double mean, double variance) {
      this.mean = mean;
      this.variance = variance;
    }

    @Override
    public int hashCode() {
      return Doubles.hashCode(mean) * 31 + Doubles.hashCode(variance);
    }
  }

  enum VarianceAlgorithm {
    DO_NOT_COMPUTE {
      @Override
      MeanAndVariance variance(double[] values, MeanAlgorithm meanAlgorithm) {
        return new MeanAndVariance(meanAlgorithm.mean(values), 0.0);
      }
    },
    SIMPLE {
      @Override
      MeanAndVariance variance(double[] values, MeanAlgorithm meanAlgorithm) {
        double mean = meanAlgorithm.mean(values);
        double sumOfSquaresOfDeltas = 0.0;
        for (double value : values) {
          double delta = value - mean;
          sumOfSquaresOfDeltas += delta * delta;
        }
        return new MeanAndVariance(mean, sumOfSquaresOfDeltas / values.length);
      }
    },
    KAHAN {
      @Override
      MeanAndVariance variance(double[] values, MeanAlgorithm meanAlgorithm) {
        double mean = meanAlgorithm.mean(values);
        double sumOfSquaresOfDeltas = 0.0;
        double c = 0.0;
        for (double value : values) {
          double delta = value - mean;
          double deltaSquared = delta * delta;
          double y = deltaSquared - c;
          double t = sumOfSquaresOfDeltas + deltaSquared;
          c = (t - sumOfSquaresOfDeltas) - y;
          sumOfSquaresOfDeltas = t;
        }
        return new MeanAndVariance(mean, sumOfSquaresOfDeltas / values.length);
      }
    },
    KNUTH {
      @Override
      MeanAndVariance variance(double[] values, MeanAlgorithm meanAlgorithm) {
        if (meanAlgorithm != MeanAlgorithm.KNUTH) {
          throw new SkipThisScenarioException();
        }
        double mean = values[0];
        double s = 0.0;
        for (int i = 1; i < values.length; i++) {
          double nextMean = mean + (values[i] - mean) / (i + 1);
          s += (values[i] - mean) * (values[i] - nextMean);
          mean = nextMean;
        }
        return new MeanAndVariance(mean, s / values.length);
      }
    };

    abstract MeanAndVariance variance(double[] values, MeanAlgorithm meanAlgorithm);
  }

  @Param({"100", "10000"})
  int n;

  @Param MeanAlgorithm meanAlgorithm;
  @Param VarianceAlgorithm varianceAlgorithm;

  private double[][] values = new double[0x100][];

  @BeforeExperiment
  void setUp() {
    Random rng = new Random();
    for (int i = 0; i < 0x100; i++) {
      values[i] = new double[n];
      for (int j = 0; j < n; j++) {
        values[i][j] = rng.nextDouble();
      }
    }
  }

  @Benchmark
  int meanAndVariance(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      tmp += varianceAlgorithm.variance(values[i & 0xFF], meanAlgorithm).hashCode();
    }
    return tmp;
  }
}
