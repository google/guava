package com.google.thirdparty.publicsuffix;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class PublicSuffixTrieTest {

  @Test
  public void testFindSuffixIndex() {
    // Tests based on PublicSuffixPatterns.TRIE which is generated from PSL.
    // registry: com, co.uk
    // private: blogspot.com
    // wildcard: *.ck
    // exclusion: !www.ck

    assertThat(PublicSuffixPatterns.TRIE.findSuffixIndex(ImmutableList.of("com"))).isEqualTo(0);
    assertThat(PublicSuffixPatterns.TRIE.findSuffixIndex(ImmutableList.of("google", "com")))
        .isEqualTo(1);
    assertThat(PublicSuffixPatterns.TRIE.findSuffixIndex(ImmutableList.of("abc", "google", "com")))
        .isEqualTo(2);

    assertThat(PublicSuffixPatterns.TRIE.findSuffixIndex(ImmutableList.of("co", "uk")))
        .isEqualTo(0);
    assertThat(PublicSuffixPatterns.TRIE.findSuffixIndex(ImmutableList.of("google", "co", "uk")))
        .isEqualTo(1);

    assertThat(PublicSuffixPatterns.TRIE.findSuffixIndex(ImmutableList.of("blogspot", "com")))
        .isEqualTo(0);
    assertThat(
            PublicSuffixPatterns.TRIE.findSuffixIndex(ImmutableList.of("foo", "blogspot", "com")))
        .isEqualTo(1);
  }

  @Test
  public void testDesiredType() {
    assertThat(
            PublicSuffixPatterns.TRIE.findSuffixIndex(
                ImmutableList.of("blogspot", "com"), PublicSuffixType.PRIVATE))
        .isEqualTo(0);
    assertThat(
            PublicSuffixPatterns.TRIE.findSuffixIndex(
                ImmutableList.of("blogspot", "com"), PublicSuffixType.REGISTRY))
        .isEqualTo(1);

    assertThat(
            PublicSuffixPatterns.TRIE.findSuffixIndex(
                ImmutableList.of("com"), PublicSuffixType.REGISTRY))
        .isEqualTo(0);
    assertThat(
            PublicSuffixPatterns.TRIE.findSuffixIndex(
                ImmutableList.of("com"), PublicSuffixType.PRIVATE))
        .isEqualTo(-1);
  }

  @Test
  public void testWildcardAndExclusion() {
    // *.ck is a public suffix
    assertThat(PublicSuffixPatterns.TRIE.findSuffixIndex(ImmutableList.of("foo", "ck")))
        .isEqualTo(0);
    assertThat(PublicSuffixPatterns.TRIE.findSuffixIndex(ImmutableList.of("bar", "foo", "ck")))
        .isEqualTo(1);

    // !www.ck is an exception, so ck is the suffix
    assertThat(PublicSuffixPatterns.TRIE.findSuffixIndex(ImmutableList.of("www", "ck")))
        .isEqualTo(1);
    assertThat(PublicSuffixPatterns.TRIE.findSuffixIndex(ImmutableList.of("foo", "www", "ck")))
        .isEqualTo(2);
  }

  @Test
  public void testNoMatch() {
    assertThat(PublicSuffixPatterns.TRIE.findSuffixIndex(ImmutableList.of("notadomain")))
        .isEqualTo(-1);
  }
}
