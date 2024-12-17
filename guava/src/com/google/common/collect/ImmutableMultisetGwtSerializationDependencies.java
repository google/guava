/*
 * Copyright (C) 2016 The Guava Authors
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

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;

/**
 * A dummy superclass to support GWT serialization of the element type of an {@link
 * ImmutableMultiset}. The GWT supersource for this class contains a field of type {@code E}.
 *
 * <p>For details about this hack, see {@code GwtSerializationDependencies}, which takes the same
 * approach but with a subclass rather than a superclass.
 *
 * <p>TODO(cpovirk): Consider applying this subclass approach to our other types.
 *
 * <p>For {@code ImmutableMultiset} in particular, I ran into a problem with the {@code
 * GwtSerializationDependencies} approach: When autogenerating a serializer for the new class, GWT
 * tries to refer to our dummy serializer for the superclass,
 * ImmutableMultiset_CustomFieldSerializer. But that type has no methods (since it's never actually
 * used). We could probably fix the problem by adding dummy methods to that class, but that is
 * starting to sound harder than taking the superclass approach, which I've been coming to like,
 * anyway, since it doesn't require us to declare dummy methods (though occasionally constructors)
 * and make types non-final.
 */
@GwtCompatible(emulated = true)
abstract class ImmutableMultisetGwtSerializationDependencies<E> extends ImmutableCollection<E> {}
