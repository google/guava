/*
 * Copyright (C) 2024 The Guava Authors
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

/** Guava Testlib */
module com.google.common.testlib {
  requires java.logging;
  requires transitive com.google.common;
  requires transitive junit;
  requires static com.google.errorprone.annotations;
  requires static com.google.j2objc.annotations;
  requires static org.jspecify;

  exports com.google.common.collect.testing;
  exports com.google.common.collect.testing.features;
  exports com.google.common.collect.testing.google;
  exports com.google.common.collect.testing.testers;
  exports com.google.common.escape.testing;
  exports com.google.common.testing;
  exports com.google.common.util.concurrent.testing;
}
