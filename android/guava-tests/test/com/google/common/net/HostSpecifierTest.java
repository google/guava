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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import java.text.ParseException;
import junit.framework.TestCase;

/**
 * {@link TestCase} for {@link HostSpecifier}. This is a relatively cursory test, as HostSpecifier
 * is a thin wrapper around {@link InetAddresses} and {@link InternetDomainName}; the unit tests for
 * those classes explore numerous corner cases. The intent here is to confirm that everything is
 * wired up properly.
 *
 * @author Craig Berry
 */
public final class HostSpecifierTest extends TestCase {

  private static final ImmutableList<String> GOOD_IPS =
      ImmutableList.of("1.2.3.4", "2001:db8::1", "[2001:db8::1]");

  private static final ImmutableList<String> BAD_IPS =
      ImmutableList.of("1.2.3", "2001:db8::1::::::0", "[2001:db8::1", "[::]:80");

  private static final ImmutableList<String> GOOD_DOMAINS =
      ImmutableList.of("com", "google.com", "foo.co.uk");

  private static final ImmutableList<String> BAD_DOMAINS =
      ImmutableList.of("foo.blah", "", "[google.com]");

  public void testGoodIpAddresses() throws ParseException {
    for (String spec : GOOD_IPS) {
      assertGood(spec);
    }
  }

  public void testBadIpAddresses() {
    for (String spec : BAD_IPS) {
      assertBad(spec);
    }
  }

  public void testGoodDomains() throws ParseException {
    for (String spec : GOOD_DOMAINS) {
      assertGood(spec);
    }
  }

  public void testBadDomains() {
    for (String spec : BAD_DOMAINS) {
      assertBad(spec);
    }
  }

  public void testEquality() {
    new EqualsTester()
        .addEqualityGroup(spec("1.2.3.4"), spec("1.2.3.4"))
        .addEqualityGroup(spec("2001:db8::1"), spec("2001:db8::1"), spec("[2001:db8::1]"))
        .addEqualityGroup(spec("2001:db8::2"))
        .addEqualityGroup(spec("google.com"), spec("google.com"))
        .addEqualityGroup(spec("www.google.com"))
        .testEquals();
  }

  private static HostSpecifier spec(String specifier) {
    return HostSpecifier.fromValid(specifier);
  }

  public void testNulls() {
    final NullPointerTester tester = new NullPointerTester();

    tester.testAllPublicStaticMethods(HostSpecifier.class);
    tester.testAllPublicInstanceMethods(HostSpecifier.fromValid("google.com"));
  }

  private void assertGood(String spec) throws ParseException {
    HostSpecifier.fromValid(spec); // Throws exception if not working correctly
    HostSpecifier.from(spec);
    assertTrue(HostSpecifier.isValid(spec));
  }

  private void assertBad(String spec) {
    try {
      HostSpecifier.fromValid(spec);
      fail("Should have thrown IllegalArgumentException: " + spec);
    } catch (IllegalArgumentException expected) {
    }

    try {
      HostSpecifier.from(spec);
      fail("Should have thrown ParseException: " + spec);
    } catch (ParseException expected) {
      assertThat(expected).hasCauseThat().isInstanceOf(IllegalArgumentException.class);
    }

    assertFalse(HostSpecifier.isValid(spec));
  }
}
