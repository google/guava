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
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;

import junit.framework.TestCase;

import java.util.List;
import java.util.Locale;

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

  /**
   * The Greek letter delta, used in unicode testing.
   */
  private static final String DELTA = "\u0394";

  /**
   * A domain part which is valid under lenient validation, but invalid under
   * strict validation.
   */
  static final String LOTS_OF_DELTAS = Strings.repeat(DELTA, 62);

  private static final String ALMOST_TOO_MANY_LEVELS =
      Strings.repeat("a.", 127);

  private static final String ALMOST_TOO_LONG =
      Strings.repeat("aaaaa.", 40) + "1234567890.c";

  private static final List<String> VALID_NAME = ImmutableList.of(
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
      "a" + DELTA + "b.com",
      ALMOST_TOO_MANY_LEVELS,
      ALMOST_TOO_LONG);

  private static final List<String> INVALID_NAME = ImmutableList.of(
      "",
      " ",
      "127.0.0.1",
      "::1", "13",
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

  private static final List<String> PS = ImmutableList.of(
      "com",
      "co.uk",
      "foo.ar",
      "xxxxxx.ar",
      "org.mK",
      "us",
      "uk\uFF61com.",  // Alternate dot character
      "\u7f51\u7edc.Cn",  // "网络.Cn"
      "j\u00f8rpeland.no",  // "jorpeland.no" (first o slashed)
      "xn--jrpeland-54a.no"  // IDNA (punycode) encoding of above
  );

  private static final List<String> NO_PS = ImmutableList.of(
      "www", "foo.google", "x.y.z");

  private static final List<String> NON_PS = ImmutableList.of(
      "foo.bar.com", "foo.ca", "foo.bar.ca",
      "foo.bar.co.il", "state.CA.us", "www.state.pa.us", "pvt.k12.ca.us",
      "www.google.com", "www4.yahoo.co.uk", "home.netscape.com",
      "web.MIT.edu", "foo.eDu.au", "utenti.blah.IT", "dominio.com.co");

  private static final List<String> TOP_PRIVATE_DOMAIN = ImmutableList.of(
      "google.com", "foo.Co.uk", "foo.ca.us.");

  private static final List<String> UNDER_PRIVATE_DOMAIN = ImmutableList.of(
      "foo.bar.google.com", "a.b.co.uk", "x.y.ca.us");

  private static final List<String> VALID_IP_ADDRS = ImmutableList.of(
      "1.2.3.4", "127.0.0.1", "::1", "2001:db8::1");

  private static final List<String> INVALID_IP_ADDRS = ImmutableList.of(
      "", "1", "1.2.3", "...", "1.2.3.4.5", "400.500.600.700",
      ":", ":::1", "2001:db8:");

  private static final List<String> SOMEWHERE_UNDER_PS = ImmutableList.of(
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
      "a\u7f51\u7edcA.\u7f51\u7edc.Cn"  // "a网络A.网络.Cn"
  );

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
        // Expected case
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

  public void testParent() {
    assertEquals(
        "com",
        InternetDomainName.from("google.com").parent().name());
    assertEquals(
        "uk",
        InternetDomainName.from("co.uk").parent().name());
    assertEquals(
        "google.com",
        InternetDomainName.from("www.google.com").parent().name());

    try {
      InternetDomainName.from("com").parent();
      fail("'com' should throw ISE on .parent() call");
    } catch (IllegalStateException expected) {
    }
  }

  public void testChild() {
    InternetDomainName domain = InternetDomainName.from("foo.com");

    assertEquals("www.foo.com", domain.child("www").name());

    try {
      domain.child("www.");
      fail("www..google.com should have been invalid");
    } catch (IllegalArgumentException expected) {
      // Expected outcome
    }
  }

  public void testParentChild() {
    InternetDomainName origin = InternetDomainName.from("foo.com");
    InternetDomainName parent = origin.parent();
    assertEquals("com", parent.name());

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
    List<String> badCookieDomains = ImmutableList.of("co.uk", "foo", "com");

    for (String domain : badCookieDomains) {
      try {
        InternetDomainName.from(domain).topPrivateDomain();
        fail(domain);
      } catch (IllegalStateException expected) {
      }
    }
  }

  public void testIsValid() {
    final Iterable<String> validCases = Iterables.concat(
        VALID_NAME, PS, NO_PS, NON_PS);
    final Iterable<String> invalidCases = Iterables.concat(
        INVALID_NAME, VALID_IP_ADDRS, INVALID_IP_ADDRS);

    for (String valid : validCases) {
      assertTrue(valid, InternetDomainName.isValid(valid));
    }

    for (String invalid : invalidCases) {
      assertFalse(invalid, InternetDomainName.isValid(invalid));
    }
  }

  // TODO(hhchan): Resurrect this test after removing the reference to
  // String.toLowerCase(Locale)
  @GwtIncompatible("String.toLowerCase(Locale)")
  public void testName() {
    for (String inputName : SOMEWHERE_UNDER_PS) {
      InternetDomainName domain = InternetDomainName.from(inputName);

      /*
       * We would ordinarily use constants for the expected results, but
       * doing it by derivation allows us to reuse the test case definitions
       * used in other tests.
       */

      String expectedName = inputName.toLowerCase(Locale.ENGLISH);
      expectedName = expectedName.replaceAll("[\u3002\uFF0E\uFF61]", ".");

      if (expectedName.endsWith(".")) {
        expectedName = expectedName.substring(0, expectedName.length() - 1);
      }

      assertEquals(expectedName, domain.name());
    }
  }

  public void testExclusion() {
    InternetDomainName domain = InternetDomainName.from("foo.nhs.uk");
    assertTrue(domain.hasPublicSuffix());
    assertEquals("uk", domain.publicSuffix().name());

    // Behold the weirdness!
    assertFalse(domain.publicSuffix().isPublicSuffix());
  }

  public void testEquality() {
    new EqualsTester()
        .addEqualityGroup(
            idn("google.com"), idn("google.com"), idn("GOOGLE.COM"))
        .addEqualityGroup(idn("www.google.com"))
        .addEqualityGroup(UNICODE_EXAMPLE)
        .addEqualityGroup(PUNYCODE_EXAMPLE)
        .testEquals();
  }

  private static InternetDomainName idn(String domain) {
    return InternetDomainName.from(domain);
  }

  @GwtIncompatible("NullPointerTester")
  public void testNulls() throws Exception {
    final NullPointerTester tester = new NullPointerTester();

    tester.testAllPublicStaticMethods(InternetDomainName.class);
    tester.testAllPublicInstanceMethods(InternetDomainName.from("google.com"));
  }

}
