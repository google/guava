/*
 * Copyright (C) 2012 The Guava Authors
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

/**
 * Interfaces, utilities, and simple implementations of escapers and encoders. The primary type is
 * {@link com.google.common.escape.Escaper}.
 *
 * <p>Additional escapers implementations are found in the applicable packages: {@link
 * com.google.common.html.HtmlEscapers} in {@code com.google.common.html}, {@link
 * com.google.common.xml.XmlEscapers} in {@code com.google.common.xml}, and {@link
 * com.google.common.net.UrlEscapers} in {@code com.google.common.net}.
 *
 * <p>This package is a part of the open-source <a href="http://github.com/google/guava">Guava</a>
 * library.
 */
@CheckReturnValue
@ParametersAreNonnullByDefault
package com.google.common.escape;

import com.google.errorprone.annotations.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;
