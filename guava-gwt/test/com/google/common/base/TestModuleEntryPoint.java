/*
 * Copyright (C) 2010 The Guava Authors
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

package com.google.common.base;

import com.google.gwt.core.client.EntryPoint;

import java.util.Arrays;
import java.util.List;

/**
 * A dummy entry point that accesses all GWT classes in
 * {@code com.google.common.base}.
 *
 * @author Hayward Chan
 */
@SuppressWarnings("unchecked")
public class TestModuleEntryPoint implements EntryPoint {

  @Override
  public void onModuleLoad() {
    // TODO: Auto generate this list.
    List<Class<?>> allClasses = Arrays.<Class<?>>asList(
        CharMatcher.class,
        Equivalence.class,
        Equivalences.class,
        Function.class,
        Functions.class,
        Joiner.class,
        Objects.class,
        Preconditions.class,
        Predicate.class,
        Predicates.class,
        Strings.class,
        Splitter.class,
        Supplier.class);
  }
}
