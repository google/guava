// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.common.collect;

import com.google.gwt.core.client.EntryPoint;

import java.util.Arrays;
import java.util.List;

/**
 * A dummy entry point that accesses all GWT classes in
 * {@code com.google.common.collect}.
 *
 * @author hhchan@google.com (Hayward Chan)
 */
@SuppressWarnings("unchecked")
public class TestModuleEntryPoint implements EntryPoint {

  // It matches all classes that are both @GwtCompatible and Google internal.

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
