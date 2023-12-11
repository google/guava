/*
 * Copyright (C) 2006 The Guava Authors
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

package com.google.common.io;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.base.Preconditions;
import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * File name filter that only accepts files matching a regular expression. This class is thread-safe
 * and immutable.
 *
 * @author Apple Chow
 * @since 1.0
 */
@J2ktIncompatible
@GwtIncompatible
@ElementTypesAreNonnullByDefault
public final class PatternFilenameFilter implements FilenameFilter {

  private final Pattern pattern;

  /**
   * Constructs a pattern file name filter object.
   *
   * @param patternStr the pattern string on which to filter file names
   * @throws PatternSyntaxException if pattern compilation fails (runtime)
   */
  public PatternFilenameFilter(String patternStr) {
    this(Pattern.compile(patternStr));
  }

  /**
   * Constructs a pattern file name filter object.
   *
   * @param pattern the pattern on which to filter file names
   */
  public PatternFilenameFilter(Pattern pattern) {
    this.pattern = Preconditions.checkNotNull(pattern);
  }

  /*
   * Our implementation works fine with a null `dir`. However, there's nothing in the documentation
   * of the supertype that suggests that implementations are expected to tolerate null. That said, I
   * see calls in Google code that pass a null `dir` to a FilenameFilter.... So let's declare the
   * parameter as non-nullable (since passing null to a FilenameFilter is unsafe in general), but if
   * someone still manages to pass null, let's continue to have the method work.
   *
   * (PatternFilenameFilter is of course one of those classes that shouldn't be a publicly visible
   * class to begin with but rather something returned from a static factory method whose declared
   * return type is plain FilenameFilter. If we made such a change, then the annotation we choose
   * here would have no significance to end users, who would be forced to conform to the signature
   * used in FilenameFilter.)
   */
  @Override
  public boolean accept(File dir, String fileName) {
    return pattern.matcher(fileName).matches();
  }
}
