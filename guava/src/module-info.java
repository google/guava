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

/** Google Guava */
module com.google.common {
  requires java.logging;
  requires transitive com.google.common.util.concurrent.internal;
  requires static jdk.unsupported;
  requires static com.google.errorprone.annotations;
  requires static com.google.j2objc.annotations;
  requires static org.jspecify;

  exports com.google.common.annotations;
  exports com.google.common.base;
  exports com.google.common.cache;
  exports com.google.common.collect;
  exports com.google.common.escape;
  exports com.google.common.eventbus;
  exports com.google.common.graph;
  exports com.google.common.hash;
  exports com.google.common.html;
  exports com.google.common.io;
  exports com.google.common.math;
  exports com.google.common.net;
  exports com.google.common.primitives;
  exports com.google.common.reflect;
  exports com.google.common.util.concurrent;
  exports com.google.common.xml;
}
