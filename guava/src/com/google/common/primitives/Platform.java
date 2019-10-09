/*
 * Copyright (C) 2019 The Guava Authors
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

package com.google.common.primitives;

import static com.google.common.base.Strings.lenientFormat;
import static java.lang.Boolean.parseBoolean;

import com.google.common.annotations.GwtCompatible;

/** Methods factored out so that they can be emulated differently in GWT. */
@GwtCompatible(emulated = true)
final class Platform {
  private static final String GWT_RPC_PROPERTY_NAME = "guava.gwt.emergency_reenable_rpc";

  static void checkGwtRpcEnabled() {
    if (!parseBoolean(System.getProperty(GWT_RPC_PROPERTY_NAME, "true"))) {
      throw new UnsupportedOperationException(
          lenientFormat(
              "We are removing GWT-RPC support for Guava types. You can temporarily reenable"
                  + " support by setting the system property %s to true. For more about system"
                  + " properties, see %s. For more about Guava's GWT-RPC support, see %s.",
              GWT_RPC_PROPERTY_NAME,
              "https://stackoverflow.com/q/5189914/28465",
              "https://groups.google.com/d/msg/guava-announce/zHZTFg7YF3o/rQNnwdHeEwAJ"));
    }
  }

  private Platform() {}
}
