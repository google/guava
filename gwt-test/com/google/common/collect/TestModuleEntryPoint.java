/*
 * Copyright (C) 2010 Google Inc.
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

import com.google.gwt.core.client.EntryPoint;

import java.util.Arrays;
import java.util.List;

/**
 * A dummy entry point that accesses all GWT classes in
 * {@code com.google.common.collect}.
 *
 * @author Hayward Chan
 */
@SuppressWarnings("unchecked")
public class TestModuleEntryPoint implements EntryPoint {

  @Override
  public void onModuleLoad() {
    // TODO: Auto generate this list.
    // Files covered by GWT_SRCS
    List<Class<?>> allClasses = Arrays.<Class<?>>asList(
        AbstractIndexedIterator.class,
        AbstractIterator.class,
        AbstractListMultimap.class,
        AbstractMapEntry.class,
        AbstractMultimap.class,
        AbstractMultiset.class,
        AbstractSetMultimap.class,
        AbstractSortedSetMultimap.class,
        BiMap.class,
        ByFunctionOrdering.class,
        ClassToInstanceMap.class,
        Collections2.class,
        ComparisonChain.class,
        ComparatorOrdering.class,
        CompoundOrdering.class,
        ComputationException.class,
        Constraint.class,
        Constraints.class,
        EmptyImmutableListMultimap.class,
        EmptyImmutableMultiset.class,
        EmptyImmutableSetMultimap.class,
        ExplicitOrdering.class,
        ForwardingCollection.class,
        ForwardingConcurrentMap.class,
        ForwardingIterator.class,
        ForwardingList.class,
        ForwardingListIterator.class,
        ForwardingListMultimap.class,
        ForwardingMap.class,
        ForwardingMapEntry.class,
        ForwardingMultimap.class,
        ForwardingMultiset.class,
        ForwardingObject.class,
        ForwardingQueue.class,
        ForwardingSet.class,
        ForwardingSetMultimap.class,
        ForwardingSortedMap.class,
        ForwardingSortedSet.class,
        ForwardingSortedSetMultimap.class,
        GenericMapMaker.class,
        Hashing.class,
        ImmutableEntry.class,
        ImmutableListMultimap.class,
        LexicographicalOrdering.class,
        ListMultimap.class,
        Lists.class,
        MapConstraint.class,
        MapConstraints.class,
        MapDifference.class,
        Maps.class,
        Multimap.class,
        Multiset.class,
        Multisets.class,
        NaturalOrdering.class,
        NullsFirstOrdering.class,
        NullsLastOrdering.class,
        ObjectArrays.class,
        Ordering.class,
        PeekingIterator.class,
        ReverseNaturalOrdering.class,
        ReverseOrdering.class,
        SetMultimap.class,
        SortedSetMultimap.class,
        UnmodifiableIterator.class,
        UsingToStringOrdering.class,

        // Classes covered by :generated_supersource
        AbstractBiMap.class,
        AbstractMapBasedMultiset.class,
        ArrayListMultimap.class,
        EnumBiMap.class,
        EnumHashBiMap.class,
        EnumMultiset.class,
        HashBiMap.class,
        HashMultimap.class,
        HashMultiset.class,
        ImmutableListMultimap.class,
        ImmutableMultimap.class,
        ImmutableMultiset.class,
        ImmutableSetMultimap.class,
        Iterables.class,
        Iterators.class,
        LinkedHashMultimap.class,
        LinkedHashMultiset.class,
        LinkedListMultimap.class,
        Multimaps.class,
        ObjectArrays.class,
        Sets.class,
        Synchronized.class,
        TreeMultimap.class,
        TreeMultiset.class);
  }

  // Reference that make sure an RPC interface is referenced.
}
