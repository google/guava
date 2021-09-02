/*
 * Copyright (C) 2008 The Guava Authors
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

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.NullPointerTester;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import junit.framework.TestCase;

/**
 * Tests for {@link InetAddresses}.
 *
 * @author Erik Kline
 */
public class InetAddressesTest extends TestCase {

  public void testNulls() {
    NullPointerTester tester = new NullPointerTester();

    tester.testAllPublicStaticMethods(InetAddresses.class);
  }

  public void testForStringBogusInput() {
    ImmutableSet<String> bogusInputs =
        ImmutableSet.of(
            "",
            "016.016.016.016",
            "016.016.016",
            "016.016",
            "016",
            "000.000.000.000",
            "000",
            "0x0a.0x0a.0x0a.0x0a",
            "0x0a.0x0a.0x0a",
            "0x0a.0x0a",
            "0x0a",
            "42.42.42.42.42",
            "42.42.42",
            "42.42",
            "42",
            "42..42.42",
            "42..42.42.42",
            "42.42.42.42.",
            "42.42.42.42...",
            ".42.42.42.42",
            ".42.42.42",
            "...42.42.42.42",
            "42.42.42.-0",
            "42.42.42.+0",
            ".",
            "...",
            "bogus",
            "bogus.com",
            "192.168.0.1.com",
            "12345.67899.-54321.-98765",
            "257.0.0.0",
            "42.42.42.-42",
            "42.42.42.ab",
            "3ffe::1.net",
            "3ffe::1::1",
            "1::2::3::4:5",
            "::7:6:5:4:3:2:", // should end with ":0"
            ":6:5:4:3:2:1::", // should begin with "0:"
            "2001::db:::1",
            "FEDC:9878",
            "+1.+2.+3.4",
            "1.2.3.4e0",
            "6:5:4:3:2:1:0", // too few parts
            "::7:6:5:4:3:2:1:0", // too many parts
            "7:6:5:4:3:2:1:0::", // too many parts
            "9:8:7:6:5:4:3::2:1", // too many parts
            "0:1:2:3::4:5:6:7", // :: must remove at least one 0.
            "3ffe:0:0:0:0:0:0:0:1", // too many parts (9 instead of 8)
            "3ffe::10000", // hextet exceeds 16 bits
            "3ffe::goog",
            "3ffe::-0",
            "3ffe::+0",
            "3ffe::-1",
            ":",
            ":::",
            "::1.2.3",
            "::1.2.3.4.5",
            "::1.2.3.4:",
            "1.2.3.4::",
            "2001:db8::1:",
            ":2001:db8::1",
            ":1:2:3:4:5:6:7",
            "1:2:3:4:5:6:7:",
            ":1:2:3:4:5:6:");

    for (String bogusInput : bogusInputs) {
      try {
        InetAddresses.forString(bogusInput);
        fail("IllegalArgumentException expected for '" + bogusInput + "'");
      } catch (IllegalArgumentException expected) {
      }
      assertFalse(InetAddresses.isInetAddress(bogusInput));
    }
  }

  public void test3ff31() {
    try {
      InetAddresses.forString("3ffe:::1");
      fail("IllegalArgumentException expected");
    } catch (IllegalArgumentException expected) {
    }
    assertFalse(InetAddresses.isInetAddress("016.016.016.016"));
  }

  public void testForStringIPv4Input() throws UnknownHostException {
    String ipStr = "192.168.0.1";
    // Shouldn't hit DNS, because it's an IP string literal.
    InetAddress ipv4Addr = InetAddress.getByName(ipStr);
    assertEquals(ipv4Addr, InetAddresses.forString(ipStr));
    assertTrue(InetAddresses.isInetAddress(ipStr));
  }

  public void testForStringIPv4NonAsciiInput() throws UnknownHostException {
    String ipStr = "૧૯૨.૧૬૮.૦.૧"; // 192.168.0.1 in Gujarati digits
    // Shouldn't hit DNS, because it's an IP string literal.
    InetAddress ipv4Addr;
    try {
      ipv4Addr = InetAddress.getByName(ipStr);
    } catch (UnknownHostException e) {
      // OK: this is probably Android, which is stricter.
      return;
    }
    assertEquals(ipv4Addr, InetAddresses.forString(ipStr));
    assertTrue(InetAddresses.isInetAddress(ipStr));
  }

  public void testForStringIPv6Input() throws UnknownHostException {
    String ipStr = "3ffe::1";
    // Shouldn't hit DNS, because it's an IP string literal.
    InetAddress ipv6Addr = InetAddress.getByName(ipStr);
    assertEquals(ipv6Addr, InetAddresses.forString(ipStr));
    assertTrue(InetAddresses.isInetAddress(ipStr));
  }

  public void testForStringIPv6NonAsciiInput() throws UnknownHostException {
    String ipStr = "૩ffe::૧"; // 3ffe::1 with Gujarati digits for 3 and 1
    // Shouldn't hit DNS, because it's an IP string literal.
    InetAddress ipv6Addr;
    try {
      ipv6Addr = InetAddress.getByName(ipStr);
    } catch (UnknownHostException e) {
      // OK: this is probably Android, which is stricter.
      return;
    }
    assertEquals(ipv6Addr, InetAddresses.forString(ipStr));
    assertTrue(InetAddresses.isInetAddress(ipStr));
  }

  public void testForStringIPv6EightColons() throws UnknownHostException {
    ImmutableSet<String> eightColons =
        ImmutableSet.of("::7:6:5:4:3:2:1", "::7:6:5:4:3:2:0", "7:6:5:4:3:2:1::", "0:6:5:4:3:2:1::");

    for (String ipString : eightColons) {
      // Shouldn't hit DNS, because it's an IP string literal.
      InetAddress ipv6Addr = InetAddress.getByName(ipString);
      assertEquals(ipv6Addr, InetAddresses.forString(ipString));
      assertTrue(InetAddresses.isInetAddress(ipString));
    }
  }

  public void testConvertDottedQuadToHex() throws UnknownHostException {
    ImmutableSet<String> ipStrings =
        ImmutableSet.of("7::0.128.0.127", "7::0.128.0.128", "7::128.128.0.127", "7::0.128.128.127");

    for (String ipString : ipStrings) {
      // Shouldn't hit DNS, because it's an IP string literal.
      InetAddress ipv6Addr = InetAddress.getByName(ipString);
      assertEquals(ipv6Addr, InetAddresses.forString(ipString));
      assertTrue(InetAddresses.isInetAddress(ipString));
    }
  }

  // see https://github.com/google/guava/issues/2587
  private static final ImmutableSet<String> SCOPE_IDS =
      ImmutableSet.of("eno1", "en1", "eth0", "X", "1", "2", "14", "20");

  public void testIPv4AddressWithScopeId() {
    ImmutableSet<String> ipStrings = ImmutableSet.of("1.2.3.4", "192.168.0.1");
    for (String ipString : ipStrings) {
      for (String scopeId : SCOPE_IDS) {
        String withScopeId = ipString + "%" + scopeId;
        assertFalse(
            "InetAddresses.isInetAddress(" + withScopeId + ") should be false but was true",
            InetAddresses.isInetAddress(withScopeId));
      }
    }
  }

  public void testDottedQuadAddressWithScopeId() {
    ImmutableSet<String> ipStrings =
        ImmutableSet.of("7::0.128.0.127", "7::0.128.0.128", "7::128.128.0.127", "7::0.128.128.127");
    for (String ipString : ipStrings) {
      for (String scopeId : SCOPE_IDS) {
        String withScopeId = ipString + "%" + scopeId;
        assertFalse(
            "InetAddresses.isInetAddress(" + withScopeId + ") should be false but was true",
            InetAddresses.isInetAddress(withScopeId));
      }
    }
  }

  public void testIPv6AddressWithScopeId() {
    ImmutableSet<String> ipStrings =
        ImmutableSet.of(
            "0:0:0:0:0:0:0:1",
            "fe80::a",
            "fe80::1",
            "fe80::2",
            "fe80::42",
            "fe80::3dd0:7f8e:57b7:34d5",
            "fe80::71a3:2b00:ddd3:753f",
            "fe80::8b2:d61e:e5c:b333",
            "fe80::b059:65f4:e877:c40");
    for (String ipString : ipStrings) {
      for (String scopeId : SCOPE_IDS) {
        String withScopeId = ipString + "%" + scopeId;
        assertTrue(
            "InetAddresses.isInetAddress(" + withScopeId + ") should be true but was false",
            InetAddresses.isInetAddress(withScopeId));
        assertEquals(InetAddresses.forString(withScopeId), InetAddresses.forString(ipString));
      }
    }
  }

  public void testToAddrStringIPv4() {
    // Don't need to test IPv4 much; it just calls getHostAddress().
    assertEquals("1.2.3.4", InetAddresses.toAddrString(InetAddresses.forString("1.2.3.4")));
  }

  public void testToAddrStringIPv6() {
    assertEquals(
        "1:2:3:4:5:6:7:8", InetAddresses.toAddrString(InetAddresses.forString("1:2:3:4:5:6:7:8")));
    assertEquals(
        "2001:0:0:4::8", InetAddresses.toAddrString(InetAddresses.forString("2001:0:0:4:0:0:0:8")));
    assertEquals(
        "2001::4:5:6:7:8",
        InetAddresses.toAddrString(InetAddresses.forString("2001:0:0:4:5:6:7:8")));
    assertEquals(
        "2001:0:3:4:5:6:7:8",
        InetAddresses.toAddrString(InetAddresses.forString("2001:0:3:4:5:6:7:8")));
    assertEquals(
        "0:0:3::ffff", InetAddresses.toAddrString(InetAddresses.forString("0:0:3:0:0:0:0:ffff")));
    assertEquals(
        "::4:0:0:0:ffff",
        InetAddresses.toAddrString(InetAddresses.forString("0:0:0:4:0:0:0:ffff")));
    assertEquals(
        "::5:0:0:ffff", InetAddresses.toAddrString(InetAddresses.forString("0:0:0:0:5:0:0:ffff")));
    assertEquals(
        "1::4:0:0:7:8", InetAddresses.toAddrString(InetAddresses.forString("1:0:0:4:0:0:7:8")));
    assertEquals("::", InetAddresses.toAddrString(InetAddresses.forString("0:0:0:0:0:0:0:0")));
    assertEquals("::1", InetAddresses.toAddrString(InetAddresses.forString("0:0:0:0:0:0:0:1")));
    assertEquals(
        "2001:658:22a:cafe::",
        InetAddresses.toAddrString(InetAddresses.forString("2001:0658:022a:cafe::")));
    assertEquals("::102:304", InetAddresses.toAddrString(InetAddresses.forString("::1.2.3.4")));
  }

  public void testToUriStringIPv4() {
    String ipStr = "1.2.3.4";
    InetAddress ip = InetAddresses.forString(ipStr);
    assertEquals("1.2.3.4", InetAddresses.toUriString(ip));
  }

  public void testToUriStringIPv6() {
    // Unfortunately the InetAddress.toString() method for IPv6 addresses
    // does not collapse contiguous shorts of zeroes with the :: abbreviation.
    String ipStr = "3ffe::1";
    InetAddress ip = InetAddresses.forString(ipStr);
    assertEquals("[3ffe::1]", InetAddresses.toUriString(ip));
  }

  public void testForUriStringIPv4() {
    Inet4Address expected = (Inet4Address) InetAddresses.forString("192.168.1.1");
    assertEquals(expected, InetAddresses.forUriString("192.168.1.1"));
  }

  public void testForUriStringIPv6() {
    Inet6Address expected = (Inet6Address) InetAddresses.forString("3ffe:0:0:0:0:0:0:1");
    assertEquals(expected, InetAddresses.forUriString("[3ffe:0:0:0:0:0:0:1]"));
  }

  public void testForUriStringIPv4Mapped() {
    Inet4Address expected = (Inet4Address) InetAddresses.forString("192.0.2.1");
    assertEquals(expected, InetAddresses.forUriString("[::ffff:192.0.2.1]"));
  }

  public void testIsUriInetAddress() {
    assertTrue(InetAddresses.isUriInetAddress("192.168.1.1"));
    assertTrue(InetAddresses.isUriInetAddress("[3ffe:0:0:0:0:0:0:1]"));
    assertTrue(InetAddresses.isUriInetAddress("[::ffff:192.0.2.1]"));

    assertFalse(InetAddresses.isUriInetAddress("[192.168.1.1"));
    assertFalse(InetAddresses.isUriInetAddress("192.168.1.1]"));
    assertFalse(InetAddresses.isUriInetAddress(""));
    assertFalse(InetAddresses.isUriInetAddress("192.168.999.888"));
    assertFalse(InetAddresses.isUriInetAddress("www.google.com"));
    assertFalse(InetAddresses.isUriInetAddress("1:2e"));
    assertFalse(InetAddresses.isUriInetAddress("[3ffe:0:0:0:0:0:0:1"));
    assertFalse(InetAddresses.isUriInetAddress("3ffe:0:0:0:0:0:0:1]"));
    assertFalse(InetAddresses.isUriInetAddress("3ffe:0:0:0:0:0:0:1"));
    assertFalse(InetAddresses.isUriInetAddress("::ffff:192.0.2.1"));
  }

  public void testForUriStringBad() {
    try {
      InetAddresses.forUriString("");
      fail("expected IllegalArgumentException"); // COV_NF_LINE
    } catch (IllegalArgumentException expected) {
    }

    try {
      InetAddresses.forUriString("192.168.999.888");
      fail("expected IllegalArgumentException"); // COV_NF_LINE
    } catch (IllegalArgumentException expected) {
    }

    try {
      InetAddresses.forUriString("www.google.com");
      fail("expected IllegalArgumentException"); // COV_NF_LINE
    } catch (IllegalArgumentException expected) {
    }

    try {
      InetAddresses.forUriString("[1:2e]");
      fail("expected IllegalArgumentException"); // COV_NF_LINE
    } catch (IllegalArgumentException expected) {
    }

    try {
      InetAddresses.forUriString("[192.168.1.1]");
      fail("expected IllegalArgumentException"); // COV_NF_LINE
    } catch (IllegalArgumentException expected) {
    }

    try {
      InetAddresses.forUriString("192.168.1.1]");
      fail("expected IllegalArgumentException"); // COV_NF_LINE
    } catch (IllegalArgumentException expected) {
    }

    try {
      InetAddresses.forUriString("[192.168.1.1");
      fail("expected IllegalArgumentException"); // COV_NF_LINE
    } catch (IllegalArgumentException expected) {
    }

    try {
      InetAddresses.forUriString("[3ffe:0:0:0:0:0:0:1");
      fail("expected IllegalArgumentException"); // COV_NF_LINE
    } catch (IllegalArgumentException expected) {
    }

    try {
      InetAddresses.forUriString("3ffe:0:0:0:0:0:0:1]");
      fail("expected IllegalArgumentException"); // COV_NF_LINE
    } catch (IllegalArgumentException expected) {
    }

    try {
      InetAddresses.forUriString("3ffe:0:0:0:0:0:0:1");
      fail("expected IllegalArgumentException"); // COV_NF_LINE
    } catch (IllegalArgumentException expected) {
    }

    try {
      InetAddresses.forUriString("::ffff:192.0.2.1");
      fail("expected IllegalArgumentException"); // COV_NF_LINE
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCompatIPv4Addresses() {
    ImmutableSet<String> nonCompatAddresses = ImmutableSet.of("3ffe::1", "::", "::1");

    for (String nonCompatAddress : nonCompatAddresses) {
      InetAddress ip = InetAddresses.forString(nonCompatAddress);
      assertFalse(InetAddresses.isCompatIPv4Address((Inet6Address) ip));
      try {
        InetAddresses.getCompatIPv4Address((Inet6Address) ip);
        fail("IllegalArgumentException expected for '" + nonCompatAddress + "'");
      } catch (IllegalArgumentException expected) {
      }
    }

    ImmutableSet<String> validCompatAddresses = ImmutableSet.of("::1.2.3.4", "::102:304");
    String compatStr = "1.2.3.4";
    InetAddress compat = InetAddresses.forString(compatStr);

    for (String validCompatAddress : validCompatAddresses) {
      InetAddress ip = InetAddresses.forString(validCompatAddress);
      assertTrue("checking '" + validCompatAddress + "'", ip instanceof Inet6Address);
      assertTrue(
          "checking '" + validCompatAddress + "'",
          InetAddresses.isCompatIPv4Address((Inet6Address) ip));
      assertEquals(
          "checking '" + validCompatAddress + "'",
          compat,
          InetAddresses.getCompatIPv4Address((Inet6Address) ip));
    }
  }

  public void testMappedIPv4Addresses() throws UnknownHostException {
    /*
     * Verify that it is not possible to instantiate an Inet6Address
     * from an "IPv4 mapped" IPv6 address.  Our String-based method can
     * at least identify them, however.
     */
    String mappedStr = "::ffff:192.168.0.1";
    assertTrue(InetAddresses.isMappedIPv4Address(mappedStr));
    InetAddress mapped = InetAddresses.forString(mappedStr);
    assertThat(mapped).isNotInstanceOf(Inet6Address.class);
    assertEquals(InetAddress.getByName("192.168.0.1"), mapped);

    // check upper case
    mappedStr = "::FFFF:192.168.0.1";
    assertTrue(InetAddresses.isMappedIPv4Address(mappedStr));
    mapped = InetAddresses.forString(mappedStr);
    assertThat(mapped).isNotInstanceOf(Inet6Address.class);
    assertEquals(InetAddress.getByName("192.168.0.1"), mapped);

    mappedStr = "0:00:000:0000:0:ffff:1.2.3.4";
    assertTrue(InetAddresses.isMappedIPv4Address(mappedStr));
    mapped = InetAddresses.forString(mappedStr);
    assertThat(mapped).isNotInstanceOf(Inet6Address.class);
    assertEquals(InetAddress.getByName("1.2.3.4"), mapped);

    mappedStr = "::ffff:0102:0304";
    assertTrue(InetAddresses.isMappedIPv4Address(mappedStr));
    mapped = InetAddresses.forString(mappedStr);
    assertThat(mapped).isNotInstanceOf(Inet6Address.class);
    assertEquals(InetAddress.getByName("1.2.3.4"), mapped);

    assertFalse(InetAddresses.isMappedIPv4Address("::"));
    assertFalse(InetAddresses.isMappedIPv4Address("::ffff"));
    assertFalse(InetAddresses.isMappedIPv4Address("::ffff:0"));
    assertFalse(InetAddresses.isMappedIPv4Address("::fffe:0:0"));
    assertFalse(InetAddresses.isMappedIPv4Address("::1:ffff:0:0"));
    assertFalse(InetAddresses.isMappedIPv4Address("foo"));
    assertFalse(InetAddresses.isMappedIPv4Address("192.0.2.1"));
  }

  public void test6to4Addresses() {
    ImmutableSet<String> non6to4Addresses = ImmutableSet.of("::1.2.3.4", "3ffe::1", "::", "::1");

    for (String non6to4Address : non6to4Addresses) {
      InetAddress ip = InetAddresses.forString(non6to4Address);
      assertFalse(InetAddresses.is6to4Address((Inet6Address) ip));
      try {
        InetAddresses.get6to4IPv4Address((Inet6Address) ip);
        fail("IllegalArgumentException expected for '" + non6to4Address + "'");
      } catch (IllegalArgumentException expected) {
      }
    }

    String valid6to4Address = "2002:0102:0304::1";
    String ipv4Str = "1.2.3.4";

    InetAddress ipv4 = InetAddresses.forString(ipv4Str);
    InetAddress ip = InetAddresses.forString(valid6to4Address);
    assertTrue(InetAddresses.is6to4Address((Inet6Address) ip));
    assertEquals(ipv4, InetAddresses.get6to4IPv4Address((Inet6Address) ip));
  }

  public void testTeredoAddresses() {
    ImmutableSet<String> nonTeredoAddresses = ImmutableSet.of("::1.2.3.4", "3ffe::1", "::", "::1");

    for (String nonTeredoAddress : nonTeredoAddresses) {
      InetAddress ip = InetAddresses.forString(nonTeredoAddress);
      assertFalse(InetAddresses.isTeredoAddress((Inet6Address) ip));
      try {
        InetAddresses.getTeredoInfo((Inet6Address) ip);
        fail("IllegalArgumentException expected for '" + nonTeredoAddress + "'");
      } catch (IllegalArgumentException expected) {
      }
    }

    String validTeredoAddress = "2001:0000:4136:e378:8000:63bf:3fff:fdd2";
    String serverStr = "65.54.227.120";
    String clientStr = "192.0.2.45";
    int port = 40000;
    int flags = 0x8000;

    InetAddress ip = InetAddresses.forString(validTeredoAddress);
    assertTrue(InetAddresses.isTeredoAddress((Inet6Address) ip));
    InetAddresses.TeredoInfo teredo = InetAddresses.getTeredoInfo((Inet6Address) ip);

    InetAddress server = InetAddresses.forString(serverStr);
    assertEquals(server, teredo.getServer());

    InetAddress client = InetAddresses.forString(clientStr);
    assertEquals(client, teredo.getClient());

    assertEquals(port, teredo.getPort());
    assertEquals(flags, teredo.getFlags());
  }

  public void testTeredoAddress_nullServer() {
    InetAddresses.TeredoInfo info = new InetAddresses.TeredoInfo(null, null, 80, 1000);
    assertEquals(InetAddresses.forString("0.0.0.0"), info.getServer());
    assertEquals(InetAddresses.forString("0.0.0.0"), info.getClient());
    assertEquals(80, info.getPort());
    assertEquals(1000, info.getFlags());
  }

  public void testIsatapAddresses() {
    InetAddress ipv4 = InetAddresses.forString("1.2.3.4");
    ImmutableSet<String> validIsatapAddresses =
        ImmutableSet.of(
            "2001:db8::5efe:102:304",
            "2001:db8::100:5efe:102:304", // Private Multicast? Not likely.
            "2001:db8::200:5efe:102:304",
            "2001:db8::300:5efe:102:304" // Public Multicast? Also unlikely.
            );
    ImmutableSet<String> nonIsatapAddresses =
        ImmutableSet.of(
            "::1.2.3.4",
            "3ffe::1",
            "::",
            "::1",
            "2001:db8::0040:5efe:102:304",
            "2001:db8::5ffe:102:304",
            "2001:db8::5eff:102:304",
            "2001:0:102:203:200:5efe:506:708" // Teredo address; not ISATAP
            );

    for (String validIsatapAddress : validIsatapAddresses) {
      InetAddress ip = InetAddresses.forString(validIsatapAddress);
      assertTrue(InetAddresses.isIsatapAddress((Inet6Address) ip));
      assertEquals(
          "checking '" + validIsatapAddress + "'",
          ipv4,
          InetAddresses.getIsatapIPv4Address((Inet6Address) ip));
    }
    for (String nonIsatapAddress : nonIsatapAddresses) {
      InetAddress ip = InetAddresses.forString(nonIsatapAddress);
      assertFalse(InetAddresses.isIsatapAddress((Inet6Address) ip));
      try {
        InetAddresses.getIsatapIPv4Address((Inet6Address) ip);
        fail("IllegalArgumentException expected for '" + nonIsatapAddress + "'");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  public void testGetEmbeddedIPv4ClientAddress() {
    Inet6Address testIp;

    // Test regular global unicast address.
    testIp = (Inet6Address) InetAddresses.forString("2001:db8::1");
    assertFalse(InetAddresses.hasEmbeddedIPv4ClientAddress(testIp));

    // Test ISATAP address.
    testIp = (Inet6Address) InetAddresses.forString("2001:db8::5efe:102:304");
    assertFalse(InetAddresses.hasEmbeddedIPv4ClientAddress(testIp));

    // Test compat address.
    testIp = (Inet6Address) InetAddresses.forString("::1.2.3.4");
    assertTrue(InetAddresses.hasEmbeddedIPv4ClientAddress(testIp));
    InetAddress ipv4 = InetAddresses.forString("1.2.3.4");
    assertEquals(ipv4, InetAddresses.getEmbeddedIPv4ClientAddress(testIp));

    // Test 6to4 address.
    testIp = (Inet6Address) InetAddresses.forString("2002:0102:0304::1");
    assertTrue(InetAddresses.hasEmbeddedIPv4ClientAddress(testIp));
    ipv4 = InetAddresses.forString("1.2.3.4");
    assertEquals(ipv4, InetAddresses.getEmbeddedIPv4ClientAddress(testIp));

    // Test Teredo address.
    testIp = (Inet6Address) InetAddresses.forString("2001:0000:4136:e378:8000:63bf:3fff:fdd2");
    assertTrue(InetAddresses.hasEmbeddedIPv4ClientAddress(testIp));
    ipv4 = InetAddresses.forString("192.0.2.45");
    assertEquals(ipv4, InetAddresses.getEmbeddedIPv4ClientAddress(testIp));
  }

  public void testGetCoercedIPv4Address() {
    // Check that a coerced IPv4 address is unaltered.
    assertThat(InetAddresses.getCoercedIPv4Address(InetAddresses.forString("127.0.0.1")))
        .isEqualTo(InetAddresses.forString("127.0.0.1"));

    // ::1 special case
    assertThat(InetAddresses.getCoercedIPv4Address(InetAddresses.forString("::1")))
        .isEqualTo(InetAddresses.forString("127.0.0.1"));

    // :: special case
    assertThat(InetAddresses.getCoercedIPv4Address(InetAddresses.forString("::")))
        .isEqualTo(InetAddresses.forString("0.0.0.0"));

    // test compat address (should be hashed)
    assertThat(InetAddresses.getCoercedIPv4Address(InetAddresses.forString("::1.2.3.4")))
        .isNotEqualTo(InetAddresses.forString("1.2.3.4"));

    // test 6to4 address (should be hashed)
    assertThat(InetAddresses.getCoercedIPv4Address(InetAddresses.forString("2002:0102:0304::1")))
        .isNotEqualTo(InetAddresses.forString("1.2.3.4"));

    // 2 6to4 addresses differing in the embedded IPv4 address should
    // hash to the different values.
    assertThat(InetAddresses.getCoercedIPv4Address(InetAddresses.forString("2002:0102:0304::1")))
        .isNotEqualTo(
            InetAddresses.getCoercedIPv4Address(InetAddresses.forString("2002:0506:0708::1")));

    // 2 6to4 addresses NOT differing in the embedded IPv4 address should
    // hash to the same value.
    assertThat(InetAddresses.getCoercedIPv4Address(InetAddresses.forString("2002:0102:0304::1")))
        .isEqualTo(
            InetAddresses.getCoercedIPv4Address(InetAddresses.forString("2002:0102:0304::2")));

    // test Teredo address (should be hashed)
    assertThat(
            InetAddresses.getCoercedIPv4Address(
                InetAddresses.forString("2001:0000:4136:e378:8000:63bf:3fff:fdd2")))
        .isNotEqualTo(InetAddresses.forString("192.0.2.45"));

    // 2 Teredo addresses differing in their embedded IPv4 addresses should hash to different
    // values.
    assertThat(
            InetAddresses.getCoercedIPv4Address(
                InetAddresses.forString("2001:0000:4136:e378:8000:63bf:3fff:fdd2")))
        .isNotEqualTo(
            InetAddresses.getCoercedIPv4Address(
                InetAddresses.forString("2001:0000:4136:e378:8000:63bf:3fff:fdd3")));

    // 2 Teredo addresses NOT differing in the their embedded IPv4 addresses should hash to the same
    // value.
    assertThat(
            InetAddresses.getCoercedIPv4Address(
                InetAddresses.forString("2001:0000:4136:e378:8000:63bf:3fff:fdd2")))
        .isEqualTo(
            InetAddresses.getCoercedIPv4Address(
                InetAddresses.forString("2001:0000:5136:f378:9000:73bf:3fff:fdd2")));

    // Test that an address hashes in to the 224.0.0.0/3 number-space.
    int coercedInt =
        InetAddresses.coerceToInteger(
            InetAddresses.getCoercedIPv4Address(InetAddresses.forString("2001:4860::1")));
    assertThat(coercedInt).isAtLeast(0xe0000000);
    assertThat(coercedInt).isAtMost(0xfffffffe);
  }

  public void testCoerceToInteger() {
    assertThat(InetAddresses.coerceToInteger(InetAddresses.forString("127.0.0.1")))
        .isEqualTo(0x7f000001);
  }

  public void testFromInteger() {
    assertThat(InetAddresses.fromInteger(0x7f000001))
        .isEqualTo(InetAddresses.forString("127.0.0.1"));
  }

  public void testFromLittleEndianByteArray() throws UnknownHostException {
    assertEquals(
        InetAddresses.fromLittleEndianByteArray(new byte[] {1, 2, 3, 4}),
        InetAddress.getByAddress(new byte[] {4, 3, 2, 1}));

    assertEquals(
        InetAddresses.fromLittleEndianByteArray(
            new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}),
        InetAddress.getByAddress(
            new byte[] {16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1}));

    try {
      InetAddresses.fromLittleEndianByteArray(new byte[3]);
      fail("expected exception");
    } catch (UnknownHostException expected) {
      // success
    }
  }

  public void testIsMaximum() throws UnknownHostException {
    InetAddress address = InetAddress.getByName("255.255.255.254");
    assertFalse(InetAddresses.isMaximum(address));

    address = InetAddress.getByName("255.255.255.255");
    assertTrue(InetAddresses.isMaximum(address));

    address = InetAddress.getByName("ffff:ffff:ffff:ffff:ffff:ffff:ffff:fffe");
    assertFalse(InetAddresses.isMaximum(address));

    address = InetAddress.getByName("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff");
    assertTrue(InetAddresses.isMaximum(address));
  }

  public void testIncrementIPv4() throws UnknownHostException {
    InetAddress address_66_0 = InetAddress.getByName("172.24.66.0");
    InetAddress address_66_255 = InetAddress.getByName("172.24.66.255");
    InetAddress address_67_0 = InetAddress.getByName("172.24.67.0");

    InetAddress address = address_66_0;
    for (int i = 0; i < 255; i++) {
      address = InetAddresses.increment(address);
    }
    assertEquals(address_66_255, address);

    address = InetAddresses.increment(address);
    assertEquals(address_67_0, address);

    InetAddress address_ffffff = InetAddress.getByName("255.255.255.255");
    address = address_ffffff;
    try {
      address = InetAddresses.increment(address);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testIncrementIPv6() throws UnknownHostException {
    InetAddress addressV6_66_0 = InetAddress.getByName("2001:db8::6600");
    InetAddress addressV6_66_ff = InetAddress.getByName("2001:db8::66ff");
    InetAddress addressV6_67_0 = InetAddress.getByName("2001:db8::6700");

    InetAddress address = addressV6_66_0;
    for (int i = 0; i < 255; i++) {
      address = InetAddresses.increment(address);
    }
    assertEquals(addressV6_66_ff, address);

    address = InetAddresses.increment(address);
    assertEquals(addressV6_67_0, address);

    InetAddress addressV6_ffffff = InetAddress.getByName("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff");
    address = addressV6_ffffff;
    try {
      address = InetAddresses.increment(address);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testDecrementIPv4() throws UnknownHostException {
    InetAddress address660 = InetAddress.getByName("172.24.66.0");
    InetAddress address66255 = InetAddress.getByName("172.24.66.255");
    InetAddress address670 = InetAddress.getByName("172.24.67.0");

    InetAddress address = address670;
    address = InetAddresses.decrement(address);

    assertEquals(address66255, address);

    for (int i = 0; i < 255; i++) {
      address = InetAddresses.decrement(address);
    }
    assertEquals(address660, address);

    InetAddress address0000 = InetAddress.getByName("0.0.0.0");
    address = address0000;
    try {
      address = InetAddresses.decrement(address);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testDecrementIPv6() throws UnknownHostException {
    InetAddress addressV6660 = InetAddress.getByName("2001:db8::6600");
    InetAddress addressV666ff = InetAddress.getByName("2001:db8::66ff");
    InetAddress addressV6670 = InetAddress.getByName("2001:db8::6700");

    InetAddress address = addressV6670;
    address = InetAddresses.decrement(address);

    assertEquals(addressV666ff, address);

    for (int i = 0; i < 255; i++) {
      address = InetAddresses.decrement(address);
    }
    assertEquals(addressV6660, address);

    InetAddress addressV6000000 = InetAddress.getByName("0:0:0:0:0:0:0:0");
    address = addressV6000000;
    try {
      address = InetAddresses.decrement(address);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testFromIpv4BigIntegerThrowsLessThanZero() {
    try {
      InetAddresses.fromIPv4BigInteger(BigInteger.valueOf(-1L));
      fail();
    } catch (IllegalArgumentException expected) {
      assertEquals("BigInteger must be greater than or equal to 0", expected.getMessage());
    }
  }

  public void testFromIpv6BigIntegerThrowsLessThanZero() {
    try {
      InetAddresses.fromIPv6BigInteger(BigInteger.valueOf(-1L));
      fail();
    } catch (IllegalArgumentException expected) {
      assertEquals("BigInteger must be greater than or equal to 0", expected.getMessage());
    }
  }

  public void testFromIpv4BigIntegerValid() {
    checkBigIntegerConversion("0.0.0.0", BigInteger.ZERO);
    checkBigIntegerConversion("0.0.0.1", BigInteger.ONE);
    checkBigIntegerConversion("127.255.255.255", BigInteger.valueOf(Integer.MAX_VALUE));
    checkBigIntegerConversion(
        "255.255.255.254", BigInteger.valueOf(Integer.MAX_VALUE).multiply(BigInteger.valueOf(2)));
    checkBigIntegerConversion(
        "255.255.255.255", BigInteger.ONE.shiftLeft(32).subtract(BigInteger.ONE));
  }

  public void testFromIpv6BigIntegerValid() {
    checkBigIntegerConversion("::", BigInteger.ZERO);
    checkBigIntegerConversion("::1", BigInteger.ONE);
    checkBigIntegerConversion("::7fff:ffff", BigInteger.valueOf(Integer.MAX_VALUE));
    checkBigIntegerConversion("::7fff:ffff:ffff:ffff", BigInteger.valueOf(Long.MAX_VALUE));
    checkBigIntegerConversion(
        "::ffff:ffff:ffff:ffff", BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE));
    checkBigIntegerConversion(
        "ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff",
        BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE));
  }

  public void testFromIpv4BigIntegerInputTooLarge() {
    try {
      InetAddresses.fromIPv4BigInteger(BigInteger.ONE.shiftLeft(32).add(BigInteger.ONE));
      fail();
    } catch (IllegalArgumentException expected) {
      assertEquals(
          "BigInteger cannot be converted to InetAddress because it has more than 4 bytes:"
              + " 4294967297",
          expected.getMessage());
    }
  }

  public void testFromIpv6BigIntegerInputTooLarge() {
    try {
      InetAddresses.fromIPv6BigInteger(BigInteger.ONE.shiftLeft(128).add(BigInteger.ONE));
      fail();
    } catch (IllegalArgumentException expected) {
      assertEquals(
          "BigInteger cannot be converted to InetAddress because it has more than 16 bytes:"
              + " 340282366920938463463374607431768211457",
          expected.getMessage());
    }
  }

  /** Checks that the IP converts to the big integer and the big integer converts to the IP. */
  private static void checkBigIntegerConversion(String ip, BigInteger bigIntegerIp) {
    InetAddress address = InetAddresses.forString(ip);
    boolean isIpv6 = address instanceof Inet6Address;
    assertEquals(bigIntegerIp, InetAddresses.toBigInteger(address));
    assertEquals(
        address,
        isIpv6
            ? InetAddresses.fromIPv6BigInteger(bigIntegerIp)
            : InetAddresses.fromIPv4BigInteger(bigIntegerIp));
  }
}
