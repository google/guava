/*
 * Copyright (C) 2009 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.net;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.text.ParseException;
import org.jspecify.annotations.Nullable;

/**
 * A syntactically valid host specifier, suitable for use in a URI. This may be either a numeric IP
 * address in IPv4 or IPv6 notation, or a domain name.
 *
 * <p>Because this class is intended to represent host specifiers which can reasonably be used in a
 * URI, the domain name case is further restricted to include only those domain names which end in a
 * recognized public suffix; see {@link InternetDomainName#isPublicSuffix()} for details.
 *
 * <p>In this context, "URI" is used in the general sense, and {@code HostSpecifier} does not
 * guarantee that every URI or URL parser will accept the original input without additional
 * normalization. {@code HostSpecifier} follows the syntactic rules of {@link InetAddresses} and
 * {@link InternetDomainName}, including their documented acceptance of some inputs that particular
 * URI implementations may reject, such as non-ASCII digits, non-ASCII domain names, or domain
 * labels containing underscores.
 *
 * <p>Note that no network lookups are performed by any {@code HostSpecifier} methods. No attempt is
 * made to verify that a provided specifier corresponds to a real or accessible host. Only syntactic
 * and pattern-based checks are performed.
 *
 * <p>If you know that a given string represents a numeric IP address, use {@link InetAddresses} to
 * obtain and manipulate a {@link java.net.InetAddress} instance from it rather than using this
 * class. Similarly, if you know that a given string represents a domain name, use {@link
 * InternetDomainName} rather than this class.
 *
 * @author Craig Berry
 * @since 5.0
 */
@J2ktIncompatible
@GwtIncompatible
public final class HostSpecifier {

  /**
   * The characters allowed in an IPv6 scope ID (zone ID), such as the {@code eth0} in {@code
   * fe80::1%eth0}: the "unreserved" characters of RFC 3986, as required by RFC 6874.
   */
  private static final CharMatcher SCOPE_ID_MATCHER =
      CharMatcher.inRange('a', 'z')
          .or(CharMatcher.inRange('A', 'Z'))
          .or(CharMatcher.inRange('0', '9'))
          .or(CharMatcher.anyOf("-._~"));

  private final String canonicalForm;

  private HostSpecifier(String canonicalForm) {
    this.canonicalForm = canonicalForm;
  }

  /**
   * Returns a {@code HostSpecifier} built from the provided {@code specifier}, which is already
   * known to be valid. If the {@code specifier} might be invalid, use {@link #from(String)}
   * instead.
   *
   * <p>The specifier must be in one of these formats:
   *
   * <ul>
   *   <li>A domain name, like {@code google.com}
   *   <li>A IPv4 address string, like {@code 127.0.0.1}
   *   <li>An IPv6 address string with or without brackets, like {@code [2001:db8::1]} or {@code
   *       2001:db8::1}, optionally with a scope ID, like {@code [fe80::1%eth0]}. The scope ID is
   *       checked only syntactically; it is not validated against the interfaces on the machine.
   * </ul>
   *
   * @throws IllegalArgumentException if the specifier is not valid.
   */
  public static HostSpecifier fromValid(String specifier) {
    // Verify that no port was specified, and strip optional brackets from
    // IPv6 literals.
    HostAndPort parsedHost = HostAndPort.fromString(specifier);
    Preconditions.checkArgument(!parsedHost.hasPort());
    String host = parsedHost.getHost();

    // Split off any IPv6 scope ID before parsing. InetAddresses.forString would validate it
    // against the network interfaces on the machine, which is an OS lookup whose result depends on
    // where the code runs, while this class promises purely syntactic checks.
    int percentIndex = host.indexOf('%');
    String ipString = (percentIndex == -1) ? host : host.substring(0, percentIndex);
    String scopeId = (percentIndex == -1) ? null : host.substring(percentIndex + 1);

    // Try to interpret the specifier as an IP address. Note we build
    // the address rather than using the .is* methods because we want to
    // use InetAddresses.toUriString to convert the result to a string in
    // canonical form.
    InetAddress addr = null;
    try {
      addr = InetAddresses.forString(ipString);
    } catch (IllegalArgumentException e) {
      // It is not an IPv4 or IPv6 literal
    }

    if (addr != null) {
      if (scopeId == null) {
        return new HostSpecifier(InetAddresses.toUriString(addr));
      }
      if (addr instanceof Inet6Address
          && !scopeId.isEmpty()
          && SCOPE_ID_MATCHER.matchesAllOf(scopeId)) {
        return new HostSpecifier("[" + InetAddresses.toAddrString(addr) + "%" + scopeId + "]");
      }
      // An IPv4 address with a scope ID, or an invalid scope ID. Fall through to the domain name
      // check, which rejects the '%'.
    }

    // It is not any kind of IP address; must be a domain name or invalid.

    // TODO(user): different versions of this for different factories?
    InternetDomainName domain = InternetDomainName.from(host);

    if (domain.hasPublicSuffix()) {
      return new HostSpecifier(domain.toString());
    }

    throw new IllegalArgumentException(
        "Domain name does not have a recognized public suffix: " + host);
  }

  /**
   * Attempts to return a {@code HostSpecifier} for the given string, throwing an exception if
   * parsing fails. Always use this method in preference to {@link #fromValid(String)} for a
   * specifier that is not already known to be valid.
   *
   * @throws ParseException if the specifier is not valid.
   */
  @CanIgnoreReturnValue // TODO(b/219820829): consider removing
  public static HostSpecifier from(String specifier) throws ParseException {
    try {
      return fromValid(specifier);
    } catch (IllegalArgumentException e) {
      // Since the IAE can originate at several different points inside
      // fromValid(), we implement this method in terms of that one rather
      // than the reverse.

      ParseException parseException = new ParseException("Invalid host specifier: " + specifier, 0);
      parseException.initCause(e);
      throw parseException;
    }
  }

  /**
   * Determines whether {@code specifier} represents a valid {@link HostSpecifier} as described in
   * the documentation for {@link #fromValid(String)}.
   */
  public static boolean isValid(String specifier) {
    try {
      HostSpecifier unused = fromValid(specifier);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }

    if (other instanceof HostSpecifier) {
      HostSpecifier that = (HostSpecifier) other;
      return this.canonicalForm.equals(that.canonicalForm);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return canonicalForm.hashCode();
  }

  /**
   * Returns a string representation of the host specifier suitable for inclusion in a URI. If the
   * host specifier is a domain name, the string will be normalized to all lower case. If the
   * specifier was an IPv6 address without brackets, brackets are added so that the result will be
   * usable in the host part of a URI.
   */
  @Override
  public String toString() {
    return canonicalForm;
  }
}
