/*
 * Copyright (C) 2011 The Guava Authors
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
import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;
import junit.framework.TestCase;

/**
 * Tests for {@link HostAndPort}
 *
 * @author Paul Marks
 */
@GwtCompatible
public class HostAndPortTest extends TestCase {

  public void testFromStringWellFormed() {
    // Well-formed inputs.
    checkFromStringCase("google.com", 80, "google.com", 80, false);
    checkFromStringCase("google.com", 80, "google.com", 80, false);
    checkFromStringCase("192.0.2.1", 82, "192.0.2.1", 82, false);
    checkFromStringCase("[2001::1]", 84, "2001::1", 84, false);
    checkFromStringCase("2001::3", 86, "2001::3", 86, false);
    checkFromStringCase("host:", 80, "host", 80, false);
  }

  public void testFromStringBadDefaultPort() {
    // Well-formed strings with bad default ports.
    checkFromStringCase("gmail.com:81", -1, "gmail.com", 81, true);
    checkFromStringCase("192.0.2.2:83", -1, "192.0.2.2", 83, true);
    checkFromStringCase("[2001::2]:85", -1, "2001::2", 85, true);
    checkFromStringCase("goo.gl:65535", 65536, "goo.gl", 65535, true);
    // No port, bad default.
    checkFromStringCase("google.com", -1, "google.com", -1, false);
    checkFromStringCase("192.0.2.1", 65536, "192.0.2.1", -1, false);
    checkFromStringCase("[2001::1]", -1, "2001::1", -1, false);
    checkFromStringCase("2001::3", 65536, "2001::3", -1, false);
  }

  public void testFromStringUnusedDefaultPort() {
    // Default port, but unused.
    checkFromStringCase("gmail.com:81", 77, "gmail.com", 81, true);
    checkFromStringCase("192.0.2.2:83", 77, "192.0.2.2", 83, true);
    checkFromStringCase("[2001::2]:85", 77, "2001::2", 85, true);
  }

  public void testFromStringBadPort() {
    // Out-of-range ports.
    checkFromStringCase("google.com:65536", 1, null, 99, false);
    checkFromStringCase("google.com:9999999999", 1, null, 99, false);
    // Invalid port parts.
    checkFromStringCase("google.com:port", 1, null, 99, false);
    checkFromStringCase("google.com:-25", 1, null, 99, false);
    checkFromStringCase("google.com:+25", 1, null, 99, false);
    checkFromStringCase("google.com:25  ", 1, null, 99, false);
    checkFromStringCase("google.com:25\t", 1, null, 99, false);
    checkFromStringCase("google.com:0x25 ", 1, null, 99, false);
  }

  public void testFromStringUnparseableNonsense() {
    // Some nonsense that causes parse failures.
    checkFromStringCase("[goo.gl]", 1, null, 99, false);
    checkFromStringCase("[goo.gl]:80", 1, null, 99, false);
    checkFromStringCase("[", 1, null, 99, false);
    checkFromStringCase("[]:", 1, null, 99, false);
    checkFromStringCase("[]:80", 1, null, 99, false);
    checkFromStringCase("[]bad", 1, null, 99, false);
  }

  public void testFromStringParseableNonsense() {
    // Examples of nonsense that gets through.
    checkFromStringCase("[[:]]", 86, "[:]", 86, false);
    checkFromStringCase("x:y:z", 87, "x:y:z", 87, false);
    checkFromStringCase("", 88, "", 88, false);
    checkFromStringCase(":", 99, "", 99, false);
    checkFromStringCase(":123", -1, "", 123, true);
    checkFromStringCase("\nOMG\t", 89, "\nOMG\t", 89, false);
  }

  private static void checkFromStringCase(
      String hpString,
      int defaultPort,
      String expectHost,
      int expectPort,
      boolean expectHasExplicitPort) {
    HostAndPort hp;
    try {
      hp = HostAndPort.fromString(hpString);
    } catch (IllegalArgumentException e) {
      // Make sure we expected this.
      assertNull(expectHost);
      return;
    }
    assertNotNull(expectHost);

    // Apply withDefaultPort(), yielding hp2.
    final boolean badDefaultPort = (defaultPort < 0 || defaultPort > 65535);
    HostAndPort hp2 = null;
    try {
      hp2 = hp.withDefaultPort(defaultPort);
      assertFalse(badDefaultPort);
    } catch (IllegalArgumentException e) {
      assertTrue(badDefaultPort);
    }

    // Check the pre-withDefaultPort() instance.
    if (expectHasExplicitPort) {
      assertTrue(hp.hasPort());
      assertEquals(expectPort, hp.getPort());
    } else {
      assertFalse(hp.hasPort());
      try {
        hp.getPort();
        fail("Expected IllegalStateException");
      } catch (IllegalStateException expected) {
      }
    }
    assertEquals(expectHost, hp.getHost());

    // Check the post-withDefaultPort() instance (if any).
    if (!badDefaultPort) {
      try {
        int port = hp2.getPort();
        assertTrue(expectPort != -1);
        assertEquals(expectPort, port);
      } catch (IllegalStateException e) {
        // Make sure we expected this to fail.
        assertEquals(-1, expectPort);
      }
      assertEquals(expectHost, hp2.getHost());
    }
  }

  public void testFromParts() {
    HostAndPort hp = HostAndPort.fromParts("gmail.com", 81);
    assertEquals("gmail.com", hp.getHost());
    assertTrue(hp.hasPort());
    assertEquals(81, hp.getPort());

    try {
      HostAndPort.fromParts("gmail.com:80", 81);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }

    try {
      HostAndPort.fromParts("gmail.com", -1);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testFromHost() {
    HostAndPort hp = HostAndPort.fromHost("gmail.com");
    assertEquals("gmail.com", hp.getHost());
    assertFalse(hp.hasPort());

    hp = HostAndPort.fromHost("[::1]");
    assertEquals("::1", hp.getHost());
    assertFalse(hp.hasPort());

    try {
      HostAndPort.fromHost("gmail.com:80");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }

    try {
      HostAndPort.fromHost("[gmail.com]");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testGetPortOrDefault() {
    assertEquals(80, HostAndPort.fromString("host:80").getPortOrDefault(123));
    assertEquals(123, HostAndPort.fromString("host").getPortOrDefault(123));
  }

  public void testHashCodeAndEquals() {
    HostAndPort hpNoPort1 = HostAndPort.fromString("foo::123");
    HostAndPort hpNoPort2 = HostAndPort.fromString("foo::123");
    HostAndPort hpNoPort3 = HostAndPort.fromString("[foo::123]");
    HostAndPort hpNoPort4 = HostAndPort.fromHost("[foo::123]");
    HostAndPort hpNoPort5 = HostAndPort.fromHost("foo::123");

    HostAndPort hpWithPort1 = HostAndPort.fromParts("[foo::123]", 80);
    HostAndPort hpWithPort2 = HostAndPort.fromParts("foo::123", 80);
    HostAndPort hpWithPort3 = HostAndPort.fromString("[foo::123]:80");

    new EqualsTester()
        .addEqualityGroup(hpNoPort1, hpNoPort2, hpNoPort3, hpNoPort4, hpNoPort5)
        .addEqualityGroup(hpWithPort1, hpWithPort2, hpWithPort3)
        .testEquals();
  }

  public void testRequireBracketsForIPv6() {
    // Bracketed IPv6 works fine.
    assertEquals("::1", HostAndPort.fromString("[::1]").requireBracketsForIPv6().getHost());
    assertEquals("::1", HostAndPort.fromString("[::1]:80").requireBracketsForIPv6().getHost());
    // Non-bracketed non-IPv6 works fine.
    assertEquals("x", HostAndPort.fromString("x").requireBracketsForIPv6().getHost());
    assertEquals("x", HostAndPort.fromString("x:80").requireBracketsForIPv6().getHost());

    // Non-bracketed IPv6 fails.
    try {
      HostAndPort.fromString("::1").requireBracketsForIPv6();
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testToString() {
    // With ports.
    assertEquals("foo:101", "" + HostAndPort.fromString("foo:101"));
    assertEquals(":102", HostAndPort.fromString(":102").toString());
    assertEquals("[1::2]:103", HostAndPort.fromParts("1::2", 103).toString());
    assertEquals("[::1]:104", HostAndPort.fromString("[::1]:104").toString());

    // Without ports.
    assertEquals("foo", "" + HostAndPort.fromString("foo"));
    assertEquals("", HostAndPort.fromString("").toString());
    assertEquals("[1::2]", HostAndPort.fromString("1::2").toString());
    assertEquals("[::1]", HostAndPort.fromString("[::1]").toString());

    // Garbage in, garbage out.
    assertEquals("[::]]:107", HostAndPort.fromParts("::]", 107).toString());
    assertEquals("[[:]]:108", HostAndPort.fromString("[[:]]:108").toString());
  }

  public void testSerialization() {
    SerializableTester.reserializeAndAssert(HostAndPort.fromParts("host", 80));
    SerializableTester.reserializeAndAssert(HostAndPort.fromString("host"));
    SerializableTester.reserializeAndAssert(HostAndPort.fromString("host:80"));
    SerializableTester.reserializeAndAssert(HostAndPort.fromString("[::1]:104"));
    SerializableTester.reserializeAndAssert(HostAndPort.fromParts("1::2", 103));
  }
}
