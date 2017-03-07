/*
 * Copyright (C) 2009 The Guava Authors
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

package com.google.common.collect.testing.google;

import static java.util.Arrays.asList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.testing.TestCharacterListGenerator;
import com.google.common.collect.testing.TestListGenerator;
import com.google.common.collect.testing.TestStringListGenerator;
import com.google.common.collect.testing.TestUnhashableCollectionGenerator;
import com.google.common.collect.testing.UnhashableObject;
import com.google.common.primitives.Chars;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Common generators of different types of lists.
 *
 * @author Hayward Chan
 */
@GwtCompatible
public final class ListGenerators {

  private ListGenerators() {}

  public static class ImmutableListOfGenerator extends TestStringListGenerator {
    @Override
    protected List<String> create(String[] elements) {
      return ImmutableList.copyOf(elements);
    }
  }

  public static class BuilderAddListGenerator extends TestStringListGenerator {
    @Override
    protected List<String> create(String[] elements) {
      ImmutableList.Builder<String> builder = ImmutableList.<String>builder();
      for (String element : elements) {
        builder.add(element);
      }
      return builder.build();
    }
  }

  public static class BuilderAddAllListGenerator extends TestStringListGenerator {
    @Override
    protected List<String> create(String[] elements) {
      return ImmutableList.<String>builder().addAll(asList(elements)).build();
    }
  }

  public static class BuilderReversedListGenerator extends TestStringListGenerator {
    @Override
    protected List<String> create(String[] elements) {
      List<String> list = asList(elements);
      Collections.reverse(list);
      return ImmutableList.copyOf(list).reverse();
    }
  }

  public static class ImmutableListHeadSubListGenerator extends TestStringListGenerator {
    @Override
    protected List<String> create(String[] elements) {
      String[] suffix = {"f", "g"};
      String[] all = new String[elements.length + suffix.length];
      System.arraycopy(elements, 0, all, 0, elements.length);
      System.arraycopy(suffix, 0, all, elements.length, suffix.length);
      return ImmutableList.copyOf(all).subList(0, elements.length);
    }
  }

  public static class ImmutableListTailSubListGenerator extends TestStringListGenerator {
    @Override
    protected List<String> create(String[] elements) {
      String[] prefix = {"f", "g"};
      String[] all = new String[elements.length + prefix.length];
      System.arraycopy(prefix, 0, all, 0, 2);
      System.arraycopy(elements, 0, all, 2, elements.length);
      return ImmutableList.copyOf(all).subList(2, elements.length + 2);
    }
  }

  public static class ImmutableListMiddleSubListGenerator extends TestStringListGenerator {
    @Override
    protected List<String> create(String[] elements) {
      String[] prefix = {"f", "g"};
      String[] suffix = {"h", "i"};

      String[] all = new String[2 + elements.length + 2];
      System.arraycopy(prefix, 0, all, 0, 2);
      System.arraycopy(elements, 0, all, 2, elements.length);
      System.arraycopy(suffix, 0, all, 2 + elements.length, 2);

      return ImmutableList.copyOf(all).subList(2, elements.length + 2);
    }
  }

  public static class CharactersOfStringGenerator extends TestCharacterListGenerator {
    @Override
    public List<Character> create(Character[] elements) {
      char[] chars = Chars.toArray(Arrays.asList(elements));
      return Lists.charactersOf(String.copyValueOf(chars));
    }
  }

  public static class CharactersOfCharSequenceGenerator extends TestCharacterListGenerator {
    @Override
    public List<Character> create(Character[] elements) {
      char[] chars = Chars.toArray(Arrays.asList(elements));
      StringBuilder str = new StringBuilder();
      str.append(chars);
      return Lists.charactersOf(str);
    }
  }

  private abstract static class TestUnhashableListGenerator
      extends TestUnhashableCollectionGenerator<List<UnhashableObject>>
      implements TestListGenerator<UnhashableObject> {}

  public static class UnhashableElementsImmutableListGenerator extends TestUnhashableListGenerator {
    @Override
    public List<UnhashableObject> create(UnhashableObject[] elements) {
      return ImmutableList.copyOf(elements);
    }
  }
}
