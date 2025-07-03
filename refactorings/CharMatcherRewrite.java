/*
 * Copyright (C) 2017 The Guava Authors
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

import com.google.common.base.CharMatcher;
import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;

/**
 * Refaster refactorings to rewrite uses of CharMatcher static constants to the static factory
 * methods.
 */
public class CharMatcherRewrite {
  class Whitespace {
    @BeforeTemplate
    CharMatcher before() {
      return CharMatcher.WHITESPACE;
    }

    @AfterTemplate
    CharMatcher after() {
      return CharMatcher.whitespace();
    }
  }

  class BreakingWhitespace {
    @BeforeTemplate
    CharMatcher before() {
      return CharMatcher.BREAKING_WHITESPACE;
    }

    @AfterTemplate
    CharMatcher after() {
      return CharMatcher.breakingWhitespace();
    }
  }

  class Ascii {
    @BeforeTemplate
    CharMatcher before() {
      return CharMatcher.ASCII;
    }

    @AfterTemplate
    CharMatcher after() {
      return CharMatcher.ascii();
    }
  }

  class Digit {
    @BeforeTemplate
    CharMatcher before() {
      return CharMatcher.DIGIT;
    }

    @AfterTemplate
    CharMatcher after() {
      return CharMatcher.digit();
    }
  }

  class JavaDigit {
    @BeforeTemplate
    CharMatcher before() {
      return CharMatcher.JAVA_DIGIT;
    }

    @AfterTemplate
    CharMatcher after() {
      return CharMatcher.javaDigit();
    }
  }

  class JavaLetterOrDigit {
    @BeforeTemplate
    CharMatcher before() {
      return CharMatcher.JAVA_LETTER_OR_DIGIT;
    }

    @AfterTemplate
    CharMatcher after() {
      return CharMatcher.javaLetterOrDigit();
    }
  }

  class JavaUpperCase {
    @BeforeTemplate
    CharMatcher before() {
      return CharMatcher.JAVA_UPPER_CASE;
    }

    @AfterTemplate
    CharMatcher after() {
      return CharMatcher.javaUpperCase();
    }
  }

  class JavaLowerCase {
    @BeforeTemplate
    CharMatcher before() {
      return CharMatcher.JAVA_LOWER_CASE;
    }

    @AfterTemplate
    CharMatcher after() {
      return CharMatcher.javaLowerCase();
    }
  }

  class JavaIsoControl {
    @BeforeTemplate
    CharMatcher before() {
      return CharMatcher.JAVA_ISO_CONTROL;
    }

    @AfterTemplate
    CharMatcher after() {
      return CharMatcher.javaIsoControl();
    }
  }

  class Invisible {
    @BeforeTemplate
    CharMatcher before() {
      return CharMatcher.INVISIBLE;
    }

    @AfterTemplate
    CharMatcher after() {
      return CharMatcher.invisible();
    }
  }

  class SingleWidth {
    @BeforeTemplate
    CharMatcher before() {
      return CharMatcher.SINGLE_WIDTH;
    }

    @AfterTemplate
    CharMatcher after() {
      return CharMatcher.singleWidth();
    }
  }

  class Any {
    @BeforeTemplate
    CharMatcher before() {
      return CharMatcher.ANY;
    }

    @AfterTemplate
    CharMatcher after() {
      return CharMatcher.any();
    }
  }

  class None {
    @BeforeTemplate
    CharMatcher before() {
      return CharMatcher.NONE;
    }

    @AfterTemplate
    CharMatcher after() {
      return CharMatcher.none();
    }
  }
}
