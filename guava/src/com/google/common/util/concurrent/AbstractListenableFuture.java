/*
 * Copyright (C) 2007 The Guava Authors
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

package com.google.common.util.concurrent;

import com.google.common.annotations.Beta;

/**
 * Legacy location of {@link AbstractFuture}.
 *
 * @author Sven Mawson
 * @since 1.0
 * @deprecated Use {@link AbstractFuture}. <b>This class is scheduled for deletion from Guava in
 *     Guava release 11.0.</b>
 */
@Beta
@Deprecated
public
abstract class AbstractListenableFuture<V> extends AbstractFuture<V> {
}
