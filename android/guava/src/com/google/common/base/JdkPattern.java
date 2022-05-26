/*
 * Copyright (C) 2016 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.base;

import com.google.common.annotations.GwtIncompatible;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A regex pattern implementation which is backed by the {@link Pattern}. */
@ElementTypesAreNonnullByDefault
@GwtIncompatible
final class JdkPattern extends CommonPattern implements Serializable {
  private final Pattern pattern;

  JdkPattern(Pattern pattern) {
    this.pattern = Preconditions.checkNotNull(pattern);
  }

  @Override
  public CommonMatcher matcher(CharSequence t) {
    return new JdkMatcher(pattern.matcher(t));
  }

  @Override
  public String pattern() {
    return pattern.pattern();
  }

  @Override
  public int flags() {
    return pattern.flags();
  }

  @Override
  public String toString() {
    return pattern.toString();
  }

  private static final class JdkMatcher extends CommonMatcher {
    final Matcher matcher;

    JdkMatcher(Matcher matcher) {
      this.matcher = Preconditions.checkNotNull(matcher);
    }

    @Override
    public boolean matches() {
      return matcher.matches();
    }

    @Override
    public boolean find() {
      return matcher.find();
    }

    @Override
    public boolean find(int index) {
      return matcher.find(index);
    }

    @Override
    public String replaceAll(String replacement) {
      return matcher.replaceAll(replacement);
    }

    @Override
    public int end() {
      return matcher.end();
    }

    @Override
    public int start() {
      return matcher.start();
    }
  }

  private static final long serialVersionUID = 0;
}
