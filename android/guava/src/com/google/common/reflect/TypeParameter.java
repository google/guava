/*
 * Copyright (C) 2011 The Guava Authors
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

package com.google.common.reflect;

import static com.google.common.base.Preconditions.checkArgument;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import javax.annotation.CheckForNull;

/**
 * Captures a free type variable that can be used in {@link TypeToken#where}. For example:
 *
 * <pre>{@code
 * static <T> TypeToken<List<T>> listOf(Class<T> elementType) {
 *   return new TypeToken<List<T>>() {}
 *       .where(new TypeParameter<T>() {}, elementType);
 * }
 * }</pre>
 *
 * @author Ben Yu
 * @since 12.0
 */
@ElementTypesAreNonnullByDefault
/*
 * A nullable bound would let users create a TypeParameter instance for a parameter with a nullable
 * bound. However, it would also let them create `new TypeParameter<@Nullable T>() {}`, which
 * wouldn't behave as users might expect. Additionally, it's not clear how the TypeToken API could
 * support even a "normal" `TypeParameter<T>` when `<T>` has a nullable bound. (See the discussion
 * on TypeToken.where.) So, in the interest of failing fast and encouraging the user to switch to a
 * non-null bound if possible, let's require a non-null bound here.
 *
 * TODO(cpovirk): Elaborate on "wouldn't behave as users might expect."
 */
public abstract class TypeParameter<T> extends TypeCapture<T> {

  final TypeVariable<?> typeVariable;

  protected TypeParameter() {
    Type type = capture();
    checkArgument(type instanceof TypeVariable, "%s should be a type variable.", type);
    this.typeVariable = (TypeVariable<?>) type;
  }

  @Override
  public final int hashCode() {
    return typeVariable.hashCode();
  }

  @Override
  public final boolean equals(@CheckForNull Object o) {
    if (o instanceof TypeParameter) {
      TypeParameter<?> that = (TypeParameter<?>) o;
      return typeVariable.equals(that.typeVariable);
    }
    return false;
  }

  @Override
  public String toString() {
    return typeVariable.toString();
  }
}
