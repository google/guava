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

package com.google.common.net;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Ascii;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import junit.framework.TestCase;

/**
 * {@link TestCase} for {@link InternetDomainName}.
 *
 * @author Craig Berry
 */
@GwtCompatible(emulated = true)
public final class InternetDomainNameTest extends TestCase {
  private static final InternetDomainName UNICODE_EXAMPLE =
      InternetDomainName.from("j\u00f8rpeland.no");
  private static final InternetDomainName PUNYCODE_EXAMPLE =
      InternetDomainName.from("xn--jrpeland-54a.no");

  /** The Greek letter delta, used in unicode testing. */
  private static final String DELTA = "\u0394";

  /** A domain part which is valid under lenient validation, but invalid under strict validation. */
  static final String LOTS_OF_DELTAS = Strings.repeat(DELTA, 62);

  private static final String ALMOST_TOO_MANY_LEVELS = Strings.repeat("a.", 127);

  private static final String ALMOST_TOO_LONG = Strings.repeat("aaaaa.", 40) + "1234567890.c";

  private static final ImmutableSet<String> VALID_NAME =
      ImmutableSet.of(
          "foo.com",
          "f-_-o.cOM",
          "f--1.com",
          "f11-1.com",
          "www",
          "abc.a23",
          "biz.com.ua",
          "x",
          "fOo",
          "f--o",
          "f_a",
          "foo.net.us\uFF61ocm",
          "woo.com.",
          "8server.shop",
          "123.cn",
          "a" + DELTA + "b.com",
          ALMOST_TOO_MANY_LEVELS,
          ALMOST_TOO_LONG);

  private static final ImmutableSet<String> INVALID_NAME =
      ImmutableSet.of(
          "",
          " ",
          "127.0.0.1",
          "::1",
          "13",
          "abc.12c",
          "foo-.com",
          "_bar.quux",
          "foo+bar.com",
          "foo!bar.com",
          ".foo.com",
          "..bar.com",
          "baz..com",
          "..quiffle.com",
          "fleeb.com..",
          ".",
          "..",
          "...",
          "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.com",
          "a" + DELTA + " .com",
          ALMOST_TOO_MANY_LEVELS + "com",
          ALMOST_TOO_LONG + ".c");

  private static final ImmutableSet<String> RS =
      ImmutableSet.of(
          "com",
          "co.uk",
          "foo.bd",
          "xxxxxx.bd",
          "org.mK",
          "us",
          "co.uk.", // Trailing dot
          "co\uFF61uk", // Alternate dot character
          "\u7f51\u7edc.Cn", // "网络.Cn"
          "j\u00f8rpeland.no", // "jorpeland.no" (first o slashed)
          "xn--jrpeland-54a.no"); // IDNA (punycode) encoding of above

  private static final ImmutableSet<String> PS_NOT_RS =
      ImmutableSet.of("blogspot.com", "blogspot.co.uk", "uk.com");

  private static final ImmutableSet<String> PS =
      ImmutableSet.<String>builder().addAll(RS).addAll(PS_NOT_RS).build();

  private static final ImmutableSet<String> NO_PS =
      ImmutableSet.of("www", "foo.ihopethiswillneverbeapublicsuffix", "x.y.z");

  /**
   * Having a public suffix is equivalent to having a registry suffix, because all registry suffixes
   * are public suffixes, and all public suffixes have registry suffixes.
   */
  private static final ImmutableSet<String> NO_RS = NO_PS;

  private static final ImmutableSet<String> NON_PS =
      ImmutableSet.of(
          "foo.bar.com",
          "foo.ca",
          "foo.bar.ca",
          "foo.blogspot.com",
          "foo.blogspot.co.uk",
          "foo.uk.com",
          "foo.bar.co.il",
          "state.CA.us",
          "www.state.pa.us",
          "pvt.k12.ca.us",
          "www.google.com",
          "www4.yahoo.co.uk",
          "home.netscape.com",
          "web.MIT.edu",
          "foo.eDu.au",
          "utenti.blah.IT",
          "dominio.com.co");

  private static final ImmutableSet<String> NON_RS =
      ImmutableSet.<String>builder().addAll(NON_PS).addAll(PS_NOT_RS).build();

  private static final ImmutableSet<String> TOP_UNDER_REGISTRY_SUFFIX =
      ImmutableSet.of("google.com", "foo.Co.uk", "foo.ca.us.");

  private static final ImmutableSet<String> TOP_PRIVATE_DOMAIN =
      ImmutableSet.of("google.com", "foo.Co.uk", "foo.ca.us.", "foo.blogspot.com");

  private static final ImmutableSet<String> UNDER_TOP_UNDER_REGISTRY_SUFFIX =
      ImmutableSet.of("foo.bar.google.com", "a.b.co.uk", "x.y.ca.us");

  private static final ImmutableSet<String> UNDER_PRIVATE_DOMAIN =
      ImmutableSet.of("foo.bar.google.com", "a.b.co.uk", "x.y.ca.us", "a.b.blogspot.com");

  private static final ImmutableSet<String> VALID_IP_ADDRS =
      ImmutableSet.of("1.2.3.4", "127.0.0.1", "::1", "2001:db8::1");

  private static final ImmutableSet<String> INVALID_IP_ADDRS =
      ImmutableSet.of(
          "", "1", "1.2.3", "...", "1.2.3.4.5", "400.500.600.700", ":", ":::1", "2001:db8:");

  private static final ImmutableSet<String> SOMEWHERE_UNDER_PS =
      ImmutableSet.of(
          "foo.bar.google.com",
          "a.b.c.1.2.3.ca.us",
          "site.jp",
          "uomi-online.kir.jp",
          "jprs.co.jp",
          "site.quick.jp",
          "site.tenki.jp",
          "site.or.jp",
          "site.gr.jp",
          "site.ne.jp",
          "site.ac.jp",
          "site.ad.jp",
          "site.ed.jp",
          "site.geo.jp",
          "site.go.jp",
          "site.lg.jp",
          "1.fm",
          "site.cc",
          "site.ee",
          "site.fi",
          "site.fm",
          "site.gr",
          "www.leguide.ma",
          "site.ma",
          "some.org.mk",
          "site.mk",
          "site.tv",
          "site.us",
          "www.odev.us",
          "www.GOOGLE.com",
          "www.com",
          "google.com",
          "www7.google.co.uk",
          "google.Co.uK",
          "jobs.kt.com.",
          "home.netscape.com",
          "web.stanford.edu",
          "stanford.edu",
          "state.ca.us",
          "www.state.ca.us",
          "state.ca.us",
          "pvt.k12.ca.us",
          "www.rave.ca.",
          "cnn.ca",
          "ledger-enquirer.com",
          "it-trace.ch",
          "cool.dk",
          "cool.co.uk",
          "cool.de",
          "cool.es",
          "cool\uFF61fr", // Alternate dot character
          "cool.nl",
          "members.blah.nl.",
          "cool.se",
          "utenti.blah.it",
          "kt.co",
          "a\u7f51\u7edcA.\u7f51\u7edc.Cn" // "a网络A.网络.Cn"
          );

  private static final ImmutableSet<String> SOMEWHERE_UNDER_RS =
      ImmutableSet.<String>builder().addAll(SOMEWHERE_UNDER_PS).addAll(PS_NOT_RS).build();

  public void testValid() {
    for (String name : VALID_NAME) {
      InternetDomainName.from(name);
    }
  }

  public void testInvalid() {
    for (String name : INVALID_NAME) {
      try {
        InternetDomainName.from(name);
        fail("Should have been invalid: '" + name + "'");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  public void testPublicSuffix() {
    for (String name : PS) {
      final InternetDomainName domain = InternetDomainName.from(name);
      assertTrue(name, domain.isPublicSuffix());
      assertTrue(name, domain.hasPublicSuffix());
      assertFalse(name, domain.isUnderPublicSuffix());
      assertFalse(name, domain.isTopPrivateDomain());
      assertEquals(domain, domain.publicSuffix());
    }

    for (String name : NO_PS) {
      final InternetDomainName domain = InternetDomainName.from(name);
      assertFalse(name, domain.isPublicSuffix());
      assertFalse(name, domain.hasPublicSuffix());
      assertFalse(name, domain.isUnderPublicSuffix());
      assertFalse(name, domain.isTopPrivateDomain());
      assertNull(domain.publicSuffix());
    }

    for (String name : NON_PS) {
      final InternetDomainName domain = InternetDomainName.from(name);
      assertFalse(name, domain.isPublicSuffix());
      assertTrue(name, domain.hasPublicSuffix());
      assertTrue(name, domain.isUnderPublicSuffix());
    }
  }

  public void testUnderPublicSuffix() {
    for (String name : SOMEWHERE_UNDER_PS) {
      final InternetDomainName domain = InternetDomainName.from(name);
      assertFalse(name, domain.isPublicSuffix());
      assertTrue(name, domain.hasPublicSuffix());
      assertTrue(name, domain.isUnderPublicSuffix());
    }
  }

  public void testTopPrivateDomain() {
    for (String name : TOP_PRIVATE_DOMAIN) {
      final InternetDomainName domain = InternetDomainName.from(name);
      assertFalse(name, domain.isPublicSuffix());
      assertTrue(name, domain.hasPublicSuffix());
      assertTrue(name, domain.isUnderPublicSuffix());
      assertTrue(name, domain.isTopPrivateDomain());
      assertEquals(domain.parent(), domain.publicSuffix());
    }
  }

  public void testUnderPrivateDomain() {
    for (String name : UNDER_PRIVATE_DOMAIN) {
      final InternetDomainName domain = InternetDomainName.from(name);
      assertFalse(name, domain.isPublicSuffix());
      assertTrue(name, domain.hasPublicSuffix());
      assertTrue(name, domain.isUnderPublicSuffix());
      assertFalse(name, domain.isTopPrivateDomain());
    }
  }

  public void testRegistrySuffix() {
    for (String name : RS) {
      final InternetDomainName domain = InternetDomainName.from(name);
      assertTrue(name, domain.isRegistrySuffix());
      assertTrue(name, domain.hasRegistrySuffix());
      assertFalse(name, domain.isUnderRegistrySuffix());
      assertFalse(name, domain.isTopDomainUnderRegistrySuffix());
      assertEquals(domain, domain.registrySuffix());
    }

    for (String name : NO_RS) {
      final InternetDomainName domain = InternetDomainName.from(name);
      assertFalse(name, domain.isRegistrySuffix());
      assertFalse(name, domain.hasRegistrySuffix());
      assertFalse(name, domain.isUnderRegistrySuffix());
      assertFalse(name, domain.isTopDomainUnderRegistrySuffix());
      assertNull(domain.registrySuffix());
    }

    for (String name : NON_RS) {
      final InternetDomainName domain = InternetDomainName.from(name);
      assertFalse(name, domain.isRegistrySuffix());
      assertTrue(name, domain.hasRegistrySuffix());
      assertTrue(name, domain.isUnderRegistrySuffix());
    }
  }

  public void testUnderRegistrySuffix() {
    for (String name : SOMEWHERE_UNDER_RS) {
      final InternetDomainName domain = InternetDomainName.from(name);
      assertFalse(name, domain.isRegistrySuffix());
      assertTrue(name, domain.hasRegistrySuffix());
      assertTrue(name, domain.isUnderRegistrySuffix());
    }
  }

  public void testTopDomainUnderRegistrySuffix() {
    for (String name : TOP_UNDER_REGISTRY_SUFFIX) {
      final InternetDomainName domain = InternetDomainName.from(name);
      assertFalse(name, domain.isRegistrySuffix());
      assertTrue(name, domain.hasRegistrySuffix());
      assertTrue(name, domain.isUnderRegistrySuffix());
      assertTrue(name, domain.isTopDomainUnderRegistrySuffix());
      assertEquals(domain.parent(), domain.registrySuffix());
    }
  }

  public void testUnderTopDomainUnderRegistrySuffix() {
    for (String name : UNDER_TOP_UNDER_REGISTRY_SUFFIX) {
      final InternetDomainName domain = InternetDomainName.from(name);
      assertFalse(name, domain.isRegistrySuffix());
      assertTrue(name, domain.hasRegistrySuffix());
      assertTrue(name, domain.isUnderRegistrySuffix());
      assertFalse(name, domain.isTopDomainUnderRegistrySuffix());
    }
  }

  public void testParent() {
    assertEquals("com", InternetDomainName.from("google.com").parent().toString());
    assertEquals("uk", InternetDomainName.from("co.uk").parent().toString());
    assertEquals("google.com", InternetDomainName.from("www.google.com").parent().toString());

    try {
      InternetDomainName.from("com").parent();
      fail("'com' should throw ISE on .parent() call");
    } catch (IllegalStateException expected) {
    }
  }

  public void testChild() {
    InternetDomainName domain = InternetDomainName.from("foo.com");

    assertEquals("www.foo.com", domain.child("www").toString());

    try {
      domain.child("www.");
      fail("www..google.com should have been invalid");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testParentChild() {
    InternetDomainName origin = InternetDomainName.from("foo.com");
    InternetDomainName parent = origin.parent();
    assertEquals("com", parent.toString());

    // These would throw an exception if leniency were not preserved during parent() and child()
    // calls.
    InternetDomainName child = parent.child(LOTS_OF_DELTAS);
    child.child(LOTS_OF_DELTAS);
  }

  public void testValidTopPrivateDomain() {
    InternetDomainName googleDomain = InternetDomainName.from("google.com");

    assertEquals(googleDomain, googleDomain.topPrivateDomain());
    assertEquals(googleDomain, googleDomain.child("mail").topPrivateDomain());
    assertEquals(googleDomain, googleDomain.child("foo.bar").topPrivateDomain());
  }

  public void testInvalidTopPrivateDomain() {
    ImmutableSet<String> badCookieDomains = ImmutableSet.of("co.uk", "foo", "com");

    for (String domain : badCookieDomains) {
      try {
        InternetDomainName.from(domain).topPrivateDomain();
        fail(domain);
      } catch (IllegalStateException expected) {
      }
    }
  }

  public void testIsValid() {
    final Iterable<String> validCases = Iterables.concat(VALID_NAME, PS, NO_PS, NON_PS);
    final Iterable<String> invalidCases =
        Iterables.concat(INVALID_NAME, VALID_IP_ADDRS, INVALID_IP_ADDRS);

    for (String valid : validCases) {
      assertTrue(valid, InternetDomainName.isValid(valid));
    }

    for (String invalid : invalidCases) {
      assertFalse(invalid, InternetDomainName.isValid(invalid));
    }
  }

  public void testToString() {
    for (String inputName : SOMEWHERE_UNDER_PS) {
      InternetDomainName domain = InternetDomainName.from(inputName);

      /*
       * We would ordinarily use constants for the expected results, but
       * doing it by derivation allows us to reuse the test case definitions
       * used in other tests.
       */

      String expectedName = Ascii.toLowerCase(inputName);
      expectedName = expectedName.replaceAll("[\u3002\uFF0E\uFF61]", ".");

      if (expectedName.endsWith(".")) {
        expectedName = expectedName.substring(0, expectedName.length() - 1);
      }

      assertEquals(expectedName, domain.toString());
    }
  }

  public void testPublicSuffixExclusion() {
    InternetDomainName domain = InternetDomainName.from("foo.city.yokohama.jp");
    assertTrue(domain.hasPublicSuffix());
    assertEquals("yokohama.jp", domain.publicSuffix().toString());

    // Behold the weirdness!
    assertFalse(domain.publicSuffix().isPublicSuffix());
  }

  public void testPublicSuffixMultipleUnders() {
    // PSL has both *.uk and *.sch.uk; the latter should win.
    // See http://code.google.com/p/guava-libraries/issues/detail?id=1176

    InternetDomainName domain = InternetDomainName.from("www.essex.sch.uk");
    assertTrue(domain.hasPublicSuffix());
    assertEquals("essex.sch.uk", domain.publicSuffix().toString());
    assertEquals("www.essex.sch.uk", domain.topPrivateDomain().toString());
  }

  public void testRegistrySuffixExclusion() {
    InternetDomainName domain = InternetDomainName.from("foo.city.yokohama.jp");
    assertTrue(domain.hasRegistrySuffix());
    assertEquals("yokohama.jp", domain.registrySuffix().toString());

    // Behold the weirdness!
    assertFalse(domain.registrySuffix().isRegistrySuffix());
  }

  public void testRegistrySuffixMultipleUnders() {
    // PSL has both *.uk and *.sch.uk; the latter should win.
    // See http://code.google.com/p/guava-libraries/issues/detail?id=1176

    InternetDomainName domain = InternetDomainName.from("www.essex.sch.uk");
    assertTrue(domain.hasRegistrySuffix());
    assertEquals("essex.sch.uk", domain.registrySuffix().toString());
    assertEquals("www.essex.sch.uk", domain.topDomainUnderRegistrySuffix().toString());
  }

  public void testEquality() {
    new EqualsTester()
        .addEqualityGroup(idn("google.com"), idn("google.com"), idn("GOOGLE.COM"))
        .addEqualityGroup(idn("www.google.com"))
        .addEqualityGroup(UNICODE_EXAMPLE)
        .addEqualityGroup(PUNYCODE_EXAMPLE)
        .testEquals();
  }

  private static InternetDomainName idn(String domain) {
    return InternetDomainName.from(domain);
  }

  @GwtIncompatible // NullPointerTester
  public void testNulls() {
    final NullPointerTester tester = new NullPointerTester();

    tester.testAllPublicStaticMethods(InternetDomainName.class);
    tester.testAllPublicInstanceMethods(InternetDomainName.from("google.com"));
  }
}
