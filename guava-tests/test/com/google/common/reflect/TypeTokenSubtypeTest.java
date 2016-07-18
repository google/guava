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

package com.google.common.reflect;

import java.util.Comparator;
import java.util.List;
import junit.framework.TestCase;

@AndroidIncompatible // lots of failures, possibly some related to bad equals() implementations?
public class TypeTokenSubtypeTest extends TestCase {

  public void testOwnerTypeSubtypes() throws Exception {
    new OwnerTypeSubtypingTests().testAllDeclarations();
  }

  public void testWildcardSubtypes() throws Exception {
    new WildcardSubtypingTests().testAllDeclarations();
  }

  public void testSubtypeOfInnerClass_nonStaticAnonymousClass() {
    TypeToken<?> supertype = new TypeToken<Mall<Outdoor>.Shop<Electronics>>() {};
    Class<?> subclass = new Mall<Outdoor>().new Shop<Electronics>() {}.getClass();
    assertTrue(TypeToken.of(subclass).isSubtypeOf(supertype));
  }

  public void testSubtypeOfInnerClass_nonStaticAnonymousClass_typeParameterOfOwnerTypeNotMatch() {
    TypeToken<?> supertype = new TypeToken<Mall<Outdoor>.Shop<Electronics>>() {};
    Class<?> subclass = new Mall<Indoor>().new Shop<Electronics>() {}.getClass();
    assertFalse(TypeToken.of(subclass).isSubtypeOf(supertype));
  }

  public void testSubtypeOfInnerClass_nonStaticAnonymousClass_typeParameterOfInnerTypeNotMatch() {
    TypeToken<?> supertype = new TypeToken<Mall<Outdoor>.Shop<Electronics>>() {};
    Class<?> subclass = new Mall<Outdoor>().new Shop<Grocery>() {}.getClass();
    assertFalse(TypeToken.of(subclass).isSubtypeOf(supertype));
  }

  public static void testSubtypeOfInnerClass_staticAnonymousClass() {
    TypeToken<?> supertype = new TypeToken<Mall<Outdoor>.Shop<Electronics>>() {};
    Class<?> subclass = new Mall<Outdoor>().new Shop<Electronics>() {}.getClass();
    assertTrue(TypeToken.of(subclass).isSubtypeOf(supertype));
  }

  public static void testSubtypeOfStaticAnonymousClass() {
    Class<?> superclass = new Mall<Outdoor>().new Shop<Electronics>() {}.getClass();
    assertTrue(TypeToken.of(superclass).isSubtypeOf(superclass));
    assertFalse(TypeToken.of(new Mall<Outdoor>().new Shop<Electronics>() {}.getClass())
        .isSubtypeOf(superclass));
  }

  public void testSubtypeOfNonStaticAnonymousClass() {
    Class<?> superclass = new Mall<Outdoor>().new Shop<Electronics>() {}.getClass();
    assertTrue(TypeToken.of(superclass).isSubtypeOf(superclass));
    assertFalse(TypeToken.of(new Mall<Outdoor>().new Shop<Electronics>() {}.getClass())
        .isSubtypeOf(superclass));
  }

  private static class OwnerTypeSubtypingTests extends SubtypeTester {
    @TestSubtype
    public Mall<Outdoor>.Shop<Electronics> innerTypeIsSubtype(
        Mall<Outdoor>.Retailer<Electronics> retailer) {
      return isSubtype(retailer);
    }

    @TestSubtype(suppressGetSupertype = true, suppressGetSubtype = true)
    public Mall<Outdoor>.Shop<? extends Electronics> innerTypeIsSubtype_supertypeWithWildcard(
        Mall<Outdoor>.Retailer<Electronics> retailer) {
      return isSubtype(retailer);
    }

    @TestSubtype(suppressGetSupertype = true, suppressGetSubtype = true)
    public Mall<?>.Shop<?> innerTypeIsSubtype_withWildcards(
        Mall<? extends Indoor>.Retailer<? extends Electronics> retailer) {
      return isSubtype(retailer);
    }

    @TestSubtype
    public Mall<Outdoor>.Shop<Electronics> ownerTypeIsSubtype(
        Outlet<Outdoor>.Shop<Electronics> shop) {
      return isSubtype(shop);
    }

    @TestSubtype(suppressGetSupertype = true, suppressGetSubtype = true)
    public Mall<? extends Outdoor>.Shop<Electronics> ownerTypeIsSubtype_supertypeWithWildcard(
        Outlet<Outdoor>.Shop<Electronics> shop) {
      return isSubtype(shop);
    }

    @TestSubtype(suppressGetSupertype = true, suppressGetSubtype = true)
    public Mall<?>.Shop<Electronics> ownerTypeIsSubtype_withWildcards(
        Outlet<? extends Outdoor>.Shop<Electronics> shop) {
      return isSubtype(shop);
    }

    @TestSubtype
    public Mall<Outdoor>.Shop<Electronics> bothOwnerTypeAndInnerTypeAreSubtypes(
        Outlet<Outdoor>.Retailer<Electronics> retailer) {
      return isSubtype(retailer);
    }

    @TestSubtype(suppressGetSupertype = true, suppressGetSubtype = true)
    public Mall<? super Outdoor>.Shop<? extends Electronics>
    bothOwnerTypeAndInnerTypeAreSubtypes_supertypeWithWildcard(
        Outlet<Outdoor>.Retailer<Electronics> retailer) {
      return isSubtype(retailer);
    }

    @TestSubtype(suppressGetSupertype = true, suppressGetSubtype = true)
    public Mall<? super Outdoor>.Shop<? extends Electronics>
    bothOwnerTypeAndInnerTypeAreSubtypes_withWildcards(
        Outlet<Outdoor>.Retailer<Electronics> retailer) {
      return isSubtype(retailer);
    }

    @TestSubtype
    public Mall<Outdoor>.Shop<Electronics> ownerTypeDoesNotMatch(
        Mall<Indoor>.Shop<Electronics> shop) {
      return notSubtype(shop);
    }

    @TestSubtype
    public Mall<Outdoor>.Shop<Electronics> ownerTypeDoesNotMatch_subtypeWithWildcard(
        Mall<? extends Outdoor>.Shop<Electronics> shop) {
      return notSubtype(shop);
    }

    @TestSubtype
    public Mall<? extends Outdoor>.Shop<Electronics> ownerTypeDoesNotMatch_supertypeWithWildcard(
        Mall<?>.Shop<Electronics> shop) {
      return notSubtype(shop);
    }

    @TestSubtype
    public Mall<Outdoor>.Retailer<Electronics> innerTypeDoesNotMatch(
        Mall<Outdoor>.Shop<Grocery> shop) {
      return notSubtype(shop);
    }

    @TestSubtype
    public Mall<Outdoor>.Shop<Electronics> innerTypeDoesNotMatch_subtypeWithWildcard(
        Mall<Outdoor>.Shop<? extends Electronics> shop) {
      return notSubtype(shop);
    }

    @TestSubtype
    public Mall<Outdoor>.Shop<? extends Electronics> innerTypeDoesNotMatch_supertypeWithWildcard(
       Mall<Outdoor>.Shop<Grocery> shop) {
      return notSubtype(shop);
    }

    @TestSubtype(suppressGetSubtype = true)
    public ConsumerFacing<Electronics> supertypeIsNestedClass(
        Mall<Indoor>.Retailer<Electronics> shop) {
      return isSubtype(shop);
    }

    @TestSubtype
    public ConsumerFacing<Grocery> nestedClassIsNotSupertypeDueToTypeParameter(
        Mall<Indoor>.Shop<Electronics> shop) {
      return notSubtype(shop);
    }

    @TestSubtype
    public ConsumerFacing<Grocery> nestedClassIsNotSupertype(
        Mall<Indoor>.Shop<Grocery> shop) {
      return notSubtype(shop);
    }

    @TestSubtype(suppressGetSubtype = true)
    public Comparator<Electronics> supertypeIsTopLevelClass(
        Mall<Indoor>.Retailer<Electronics> shop) {
      return isSubtype(shop);
    }

    @TestSubtype
    public Comparator<Electronics> topLevelClassIsNotSupertypeDueToTypeParameter(
        Mall<Indoor>.Retailer<Grocery> shop) {
      return notSubtype(shop);
    }

    @TestSubtype
    public Comparator<Electronics> topLevelClassIsNotSupertype(
        Mall<Indoor>.Shop<Electronics> shop) {
      return notSubtype(shop);
    }
  }

  private static class WildcardSubtypingTests extends SubtypeTester {
    @TestSubtype(suppressGetSupertype = true, suppressGetSubtype = true)
    public <T> Iterable<? extends T> supertypeWithWildcardUpperBound(List<T> list) {
      return isSubtype(list);
    }

    @TestSubtype(suppressGetSupertype = true, suppressGetSubtype = true)
    public <T> Iterable<? extends T> supertypeWithWildcardUpperBound_notMatch(List<String> list) {
      return notSubtype(list);
    }

    @TestSubtype(suppressGetSupertype = true, suppressGetSubtype = true)
    public <T> Iterable<? super T> supertypeWithWildcardULowerBound(List<T> list) {
      return isSubtype(list);
    }

    @TestSubtype(suppressGetSupertype = true, suppressGetSubtype = true)
    public <T> Iterable<? extends T> supertypeWithWildcardULowerBound_notMatch(List<String> list) {
      return notSubtype(list);
    }

    @TestSubtype(suppressGetSupertype = true, suppressGetSubtype = true)
    public <T> Iterable<?> wildcardsMatchByUpperBound(List<? extends T> list) {
      return isSubtype(list);
    }

    @TestSubtype(suppressGetSupertype = true, suppressGetSubtype = true)
    public <T> Iterable<? extends T> wildCardsDoNotMatchByUpperBound(List<?> list) {
      return notSubtype(list);
    }

    @TestSubtype(suppressGetSupertype = true, suppressGetSubtype = true)
    public <T> Iterable<? super String> wildcardsMatchByLowerBound(
        List<? super CharSequence> list) {
      return isSubtype(list);
    }

    @TestSubtype(suppressGetSupertype = true, suppressGetSubtype = true)
    public <T> Iterable<? super CharSequence> wildCardsDoNotMatchByLowerBound(
        List<? super String> list) {
      return notSubtype(list);
    }
  }

  // TODO(benyu): migrate all subtyping tests from TypeTokenTest to this class using SubtypeTester.

  private interface Outdoor {}
  private interface Indoor {}
  private interface Grocery {}
  private interface Electronics {}
  private interface ConsumerFacing<T> {}

  private static class Mall<T> {
    class Shop<ProductT> {}
    abstract class Retailer<ProductT> extends Shop<ProductT>
        implements Comparator<ProductT>, ConsumerFacing<ProductT> {}
  }

  private static class Outlet<T> extends Mall<T> {}
}

