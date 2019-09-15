/*
 * Copyright (C) 2019 The Guava Authors
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

package com.google.common.primitives;

final class Platform {
  /*
   * We will eventually disable GWT-RPC on the server side, but we'll leave it nominally enabled on
   * the client side. There's little practical difference: If it's disabled on the server, it won't
   * work. It's just a matter of how quickly it fails. I'm not sure if failing on the client would
   * be better or not, but it's harder: GWT's System.getProperty reads from a different property
   * list than Java's, so anyone who needs to reenable GWT-RPC in an emergency would have to figure
   * out how to set both properties. It's easier to have to set only one, and it might as well be
   * the Java property, since Guava already reads another Java property.
   */
  static void checkGwtRpcEnabled() {}

  private Platform() {}
}
