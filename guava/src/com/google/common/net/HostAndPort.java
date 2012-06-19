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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.concurrent.Immutable;

/**
 * An immutable representation of a host and port.
 *
 * <p>Example usage:
 * <pre>
 * HostAndPort hp = HostAndPort.fromString("[2001:db8::1]")
 *     .withDefaultPort(80)
 *     .requireBracketsForIPv6();
 * hp.getHostText();  // returns "2001:db8::1"
 * hp.getPort();      // returns 80
 * hp.toString();     // returns "[2001:db8::1]:80"
 * </pre>
 *
 * <p>Here are some examples of recognized formats:
 * <ul>
 *   <li>example.com
 *   <li>example.com:80
 *   <li>192.0.2.1
 *   <li>192.0.2.1:80
 *   <li>[2001:db8::1]     - {@link #getHostText()} omits brackets
 *   <li>[2001:db8::1]:80  - {@link #getHostText()} omits brackets
 *   <li>2001:db8::1       - Use {@link #requireBracketsForIPv6()} to prohibit this
 * </ul>
 *
 * <p>Note that this is not an exhaustive list, because these methods are only
 * concerned with brackets, colons, and port numbers.  Full validation of the
 * host field (if desired) is the caller's responsibility.
 *
 * @author Paul Marks
 * @since 10.0
 */
@Beta @Immutable
public final class HostAndPort implements Serializable {
  /** Magic value indicating the absence of a port number. */
  private static final int NO_PORT = -1;

  /** Hostname, IPv4/IPv6 literal, or unvalidated nonsense. */
  private final String host;

  /** Validated port number in the range [0..65535], or NO_PORT */
  private final int port;

  /** True if the parsed host has colons, but no surrounding brackets. */
  private final boolean hasBracketlessColons;

  private HostAndPort(String host, int port, boolean hasBracketlessColons) {
    this.host = host;
    this.port = port;
    this.hasBracketlessColons = hasBracketlessColons;
  }

  /**
   * Returns the portion of this {@code HostAndPort} instance that should
   * represent the hostname or IPv4/IPv6 literal.
   *
   * A successful parse does not imply any degree of sanity in this field.
   * For additional validation, see the {@link HostSpecifier} class.
   */
  public String getHostText() {
    return host;
  }

  /** Return true if this instance has a defined port. */
  public boolean hasPort() {
    return port >= 0;
  }

  /**
   * Get the current port number, failing if no port is defined.
   *
   * @return a validated port number, in the range [0..65535]
   * @throws IllegalStateException if no port is defined.  You can use
   *         {@link #withDefaultPort(int)} to prevent this from occurring.
   */
  public int getPort() {
    checkState(hasPort());
    return port;
  }

  /**
   * Returns the current port number, with a default if no port is defined.
   */
  public int getPortOrDefault(int defaultPort) {
    return hasPort() ? port : defaultPort;
  }

  /**
   * Build a HostAndPort instance from separate host and port values.
   *
   * <p>Note: Non-bracketed IPv6 literals are allowed.
   * Use {@link #requireBracketsForIPv6()} to prohibit these.
   *
   * @param host the host string to parse.  Must not contain a port number.
   * @param port a port number from [0..65535]
   * @return if parsing was successful, a populated HostAndPort object.
   * @throws IllegalArgumentException if {@code host} contains a port number,
   *     or {@code port} is out of range.
   */
  public static HostAndPort fromParts(String host, int port) {
    checkArgument(isValidPort(port));
    HostAndPort parsedHost = fromString(host);
    checkArgument(!parsedHost.hasPort());
    return new HostAndPort(parsedHost.host, port, parsedHost.hasBracketlessColons);
  }

  private static final Pattern BRACKET_PATTERN = Pattern.compile("^\\[(.*:.*)\\](?::(\\d*))?$");

  /**
   * Split a freeform string into a host and port, without strict validation.
   *
   * Note that the host-only formats will leave the port field undefined.  You
   * can use {@link #withDefaultPort(int)} to patch in a default value.
   *
   * @param hostPortString the input string to parse.
   * @return if parsing was successful, a populated HostAndPort object.
   * @throws IllegalArgumentException if nothing meaningful could be parsed.
   */
  public static HostAndPort fromString(String hostPortString) {
    checkNotNull(hostPortString);
    String host;
    String portString = null;
    boolean hasBracketlessColons = false;

    if (hostPortString.startsWith("[")) {
      // Parse a bracketed host, typically an IPv6 literal.
      Matcher matcher = BRACKET_PATTERN.matcher(hostPortString);
      checkArgument(matcher.matches(), "Invalid bracketed host/port: %s", hostPortString);
      host = matcher.group(1);
      portString = matcher.group(2);  // could be null
    } else {
      int colonPos = hostPortString.indexOf(':');
      if (colonPos >= 0 && hostPortString.indexOf(':', colonPos + 1) == -1) {
        // Exactly 1 colon.  Split into host:port.
        host = hostPortString.substring(0, colonPos);
        portString = hostPortString.substring(colonPos + 1);
      } else {
        // 0 or 2+ colons.  Bare hostname or IPv6 literal.
        host = hostPortString;
        hasBracketlessColons = (colonPos >= 0);
      }
    }

    int port = NO_PORT;
    if (portString != null) {
      // Try to parse the whole port string as a number.
      // JDK7 accepts leading plus signs. We don't want to.
      checkArgument(!portString.startsWith("+"), "Unparseable port number: %s", hostPortString);
      try {
        port = Integer.parseInt(portString);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Unparseable port number: " + hostPortString);
      }
      checkArgument(isValidPort(port), "Port number out of range: %s", hostPortString);
    }

    return new HostAndPort(host, port, hasBracketlessColons);
  }

  /**
   * Provide a default port if the parsed string contained only a host.
   *
   * You can chain this after {@link #fromString(String)} to include a port in
   * case the port was omitted from the input string.  If a port was already
   * provided, then this method is a no-op.
   *
   * @param defaultPort a port number, from [0..65535]
   * @return a HostAndPort instance, guaranteed to have a defined port.
   */
  public HostAndPort withDefaultPort(int defaultPort) {
    checkArgument(isValidPort(defaultPort));
    if (hasPort() || port == defaultPort) {
      return this;
    }
    return new HostAndPort(host, defaultPort, hasBracketlessColons);
  }

  /**
   * Generate an error if the host might be a non-bracketed IPv6 literal.
   *
   * <p>URI formatting requires that IPv6 literals be surrounded by brackets,
   * like "[2001:db8::1]".  Chain this call after {@link #fromString(String)}
   * to increase the strictness of the parser, and disallow IPv6 literals
   * that don't contain these brackets.
   *
   * <p>Note that this parser identifies IPv6 literals solely based on the
   * presence of a colon.  To perform actual validation of IP addresses, see
   * the {@link InetAddresses#forString(String)} method.
   *
   * @return {@code this}, to enable chaining of calls.
   * @throws IllegalArgumentException if bracketless IPv6 is detected.
   */
  public HostAndPort requireBracketsForIPv6() {
    checkArgument(!hasBracketlessColons, "Possible bracketless IPv6 literal: %s", host);
    return this;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other instanceof HostAndPort) {
      HostAndPort that = (HostAndPort) other;
      return Objects.equal(this.host, that.host)
          && this.port == that.port
          && this.hasBracketlessColons == that.hasBracketlessColons;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(host, port, hasBracketlessColons);
  }

  /** Rebuild the host:port string, including brackets if necessary. */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder(host.length() + 7);
    if (host.indexOf(':') >= 0) {
      builder.append('[').append(host).append(']');
    } else {
      builder.append(host);
    }
    if (hasPort()) {
      builder.append(':').append(port);
    }
    return builder.toString();
  }

  /** Return true for valid port numbers. */
  private static boolean isValidPort(int port) {
    return port >= 0 && port <= 65535;
  }

  private static final long serialVersionUID = 0;
}
