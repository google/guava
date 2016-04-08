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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Ascii;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.thirdparty.publicsuffix.PublicSuffixPatterns;

import java.util.List;

import javax.annotation.Nullable;

/**
 * An immutable well-formed internet domain name, such as {@code com} or {@code
 * foo.co.uk}. Only syntactic analysis is performed; no DNS lookups or other network interactions
 * take place. Thus there is no guarantee that the domain actually exists on the internet.
 *
 * <p>One common use of this class is to determine whether a given string is likely to represent an
 * addressable domain on the web -- that is, for a candidate string {@code "xxx"}, might browsing to
 * {@code "http://xxx/"} result in a webpage being displayed? In the past, this test was frequently
 * done by determining whether the domain ended with a {@linkplain #isPublicSuffix() public suffix}
 * but was not itself a public suffix. However, this test is no longer accurate. There are many
 * domains which are both public suffixes and addressable as hosts; {@code "uk.com"} is one example.
 * As a result, the only useful test to determine if a domain is a plausible web host is
 * {@link #hasPublicSuffix()}. This will return {@code true} for many domains which (currently) are
 * not hosts, such as {@code "com"}, but given that any public suffix may become a host without
 * warning, it is better to err on the side of permissiveness and thus avoid spurious rejection of
 * valid sites.
 *
 * <p>During construction, names are normalized in two ways:
 *
 * <ol>
 * <li>ASCII uppercase characters are converted to lowercase.
 * <li>Unicode dot separators other than the ASCII period ({@code '.'}) are converted to the ASCII
 *     period.
 * </ol>
 *
 * <p>The normalized values will be returned from {@link #toString()} and {@link #parts()}, and will
 * be reflected in the result of {@link #equals(Object)}.
 *
 * <p><a href="http://en.wikipedia.org/wiki/Internationalized_domain_name">Internationalized domain
 * names</a> such as {@code 网络.cn} are supported, as are the equivalent
 * <a href="http://en.wikipedia.org/wiki/Internationalized_domain_name">IDNA Punycode-encoded</a>
 * versions.
 *
 * @author Craig Berry
 * @since 5.0
 */
@Beta
@GwtCompatible
public final class InternetDomainName {

  private static final CharMatcher DOTS_MATCHER = CharMatcher.anyOf(".\u3002\uFF0E\uFF61");
  private static final Splitter DOT_SPLITTER = Splitter.on('.');
  private static final Joiner DOT_JOINER = Joiner.on('.');

  /**
   * Value of {@link #publicSuffixIndex} which indicates that no public suffix was found.
   */
  private static final int NO_PUBLIC_SUFFIX_FOUND = -1;

  private static final String DOT_REGEX = "\\.";

  /**
   * Maximum parts (labels) in a domain name. This value arises from the 255-octet limit described
   * in <a href="http://www.ietf.org/rfc/rfc2181.txt">RFC 2181</a> part 11 with the fact that the
   * encoding of each part occupies at least two bytes (dot plus label externally, length byte plus
   * label internally). Thus, if all labels have the minimum size of one byte, 127 of them will fit.
   */
  private static final int MAX_PARTS = 127;

  /**
   * Maximum length of a full domain name, including separators, and leaving room for the root
   * label. See <a href="http://www.ietf.org/rfc/rfc2181.txt">RFC 2181</a> part 11.
   */
  private static final int MAX_LENGTH = 253;

  /**
   * Maximum size of a single part of a domain name. See
   * <a href="http://www.ietf.org/rfc/rfc2181.txt">RFC 2181</a> part 11.
   */
  private static final int MAX_DOMAIN_PART_LENGTH = 63;

  /**
   * The full domain name, converted to lower case.
   */
  private final String name;

  /**
   * The parts of the domain name, converted to lower case.
   */
  private final ImmutableList<String> parts;

  /**
   * The index in the {@link #parts()} list at which the public suffix begins. For example, for the
   * domain name {@code www.google.co.uk}, the value would be 2 (the index of the {@code co} part).
   * The value is negative (specifically, {@link #NO_PUBLIC_SUFFIX_FOUND}) if no public suffix was
   * found.
   */
  private final int publicSuffixIndex;

  /**
   * Constructor used to implement {@link #from(String)}, and from subclasses.
   */
  InternetDomainName(String name) {
    // Normalize:
    // * ASCII characters to lowercase
    // * All dot-like characters to '.'
    // * Strip trailing '.'

    name = Ascii.toLowerCase(DOTS_MATCHER.replaceFrom(name, '.'));

    if (name.endsWith(".")) {
      name = name.substring(0, name.length() - 1);
    }

    checkArgument(name.length() <= MAX_LENGTH, "Domain name too long: '%s':", name);
    this.name = name;

    this.parts = ImmutableList.copyOf(DOT_SPLITTER.split(name));
    checkArgument(parts.size() <= MAX_PARTS, "Domain has too many parts: '%s'", name);
    checkArgument(validateSyntax(parts), "Not a valid domain name: '%s'", name);

    this.publicSuffixIndex = findPublicSuffix();
  }

  /**
   * Returns the index of the leftmost part of the public suffix, or -1 if not found. Note that the
   * value defined as the "public suffix" may not be a public suffix according to
   * {@link #isPublicSuffix()} if the domain ends with an excluded domain pattern such as
   * {@code "nhs.uk"}.
   */
  private int findPublicSuffix() {
    final int partsSize = parts.size();

    for (int i = 0; i < partsSize; i++) {
      String ancestorName = DOT_JOINER.join(parts.subList(i, partsSize));

      if (PublicSuffixPatterns.EXACT.containsKey(ancestorName)) {
        return i;
      }

      // Excluded domains (e.g. !nhs.uk) use the next highest
      // domain as the effective public suffix (e.g. uk).

      if (PublicSuffixPatterns.EXCLUDED.containsKey(ancestorName)) {
        return i + 1;
      }

      if (matchesWildcardPublicSuffix(ancestorName)) {
        return i;
      }
    }

    return NO_PUBLIC_SUFFIX_FOUND;
  }

  /**
   * Returns an instance of {@link InternetDomainName} after lenient validation. Specifically,
   * validation against <a href="http://www.ietf.org/rfc/rfc3490.txt">RFC 3490</a>
   * ("Internationalizing Domain Names in Applications") is skipped, while validation against
   * <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a> is relaxed in the following ways:
   * <ul>
   * <li>Any part containing non-ASCII characters is considered valid.
   * <li>Underscores ('_') are permitted wherever dashes ('-') are permitted.
   * <li>Parts other than the final part may start with a digit, as mandated by
   * <a href="https://tools.ietf.org/html/rfc1123#section-2">RFC 1123</a>.
   * </ul>
   *
   *
   * @param domain A domain name (not IP address)
   * @throws IllegalArgumentException if {@code name} is not syntactically valid according to
   *     {@link #isValid}
   * @since 10.0 (previously named {@code fromLenient})
   */
  public static InternetDomainName from(String domain) {
    return new InternetDomainName(checkNotNull(domain));
  }

  /**
   * Validation method used by {@from} to ensure that the domain name is syntactically valid
   * according to RFC 1035.
   *
   * @return Is the domain name syntactically valid?
   */
  private static boolean validateSyntax(List<String> parts) {
    final int lastIndex = parts.size() - 1;

    // Validate the last part specially, as it has different syntax rules.

    if (!validatePart(parts.get(lastIndex), true)) {
      return false;
    }

    for (int i = 0; i < lastIndex; i++) {
      String part = parts.get(i);
      if (!validatePart(part, false)) {
        return false;
      }
    }

    return true;
  }

  private static final CharMatcher DASH_MATCHER = CharMatcher.anyOf("-_");

  private static final CharMatcher PART_CHAR_MATCHER =
      CharMatcher.javaLetterOrDigit().or(DASH_MATCHER);

  /**
   * Helper method for {@link #validateSyntax(List)}. Validates that one part of a domain name is
   * valid.
   *
   * @param part The domain name part to be validated
   * @param isFinalPart Is this the final (rightmost) domain part?
   * @return Whether the part is valid
   */
  private static boolean validatePart(String part, boolean isFinalPart) {

    // These tests could be collapsed into one big boolean expression, but
    // they have been left as independent tests for clarity.

    if (part.length() < 1 || part.length() > MAX_DOMAIN_PART_LENGTH) {
      return false;
    }

    /*
     * GWT claims to support java.lang.Character's char-classification methods, but it actually only
     * works for ASCII. So for now, assume any non-ASCII characters are valid. The only place this
     * seems to be documented is here:
     * http://osdir.com/ml/GoogleWebToolkitContributors/2010-03/msg00178.html
     *
     * <p>ASCII characters in the part are expected to be valid per RFC 1035, with underscore also
     * being allowed due to widespread practice.
     */

    String asciiChars = CharMatcher.ascii().retainFrom(part);

    if (!PART_CHAR_MATCHER.matchesAllOf(asciiChars)) {
      return false;
    }

    // No initial or final dashes or underscores.

    if (DASH_MATCHER.matches(part.charAt(0))
        || DASH_MATCHER.matches(part.charAt(part.length() - 1))) {
      return false;
    }

    /*
     * Note that we allow (in contravention of a strict interpretation of the relevant RFCs) domain
     * parts other than the last may begin with a digit (for example, "3com.com"). It's important to
     * disallow an initial digit in the last part; it's the only thing that stops an IPv4 numeric
     * address like 127.0.0.1 from looking like a valid domain name.
     */

    if (isFinalPart && CharMatcher.digit().matches(part.charAt(0))) {
      return false;
    }

    return true;
  }

  /**
   * Returns the individual components of this domain name, normalized to all lower case. For
   * example, for the domain name {@code mail.google.com}, this method returns the list
   * {@code ["mail", "google", "com"]}.
   */
  public ImmutableList<String> parts() {
    return parts;
  }

  /**
   * Indicates whether this domain name represents a <i>public suffix</i>, as defined by the Mozilla
   * Foundation's <a href="http://publicsuffix.org/">Public Suffix List</a> (PSL). A public suffix
   * is one under which Internet users can directly register names, such as {@code com},
   * {@code co.uk} or {@code pvt.k12.wy.us}. Examples of domain names that are <i>not</i> public
   * suffixes include {@code google}, {@code google.com} and {@code foo.co.uk}.
   *
   * @return {@code true} if this domain name appears exactly on the public suffix list
   * @since 6.0
   */
  public boolean isPublicSuffix() {
    return publicSuffixIndex == 0;
  }

  /**
   * Indicates whether this domain name ends in a {@linkplain #isPublicSuffix() public suffix},
   * including if it is a public suffix itself. For example, returns {@code true} for
   * {@code www.google.com}, {@code foo.co.uk} and {@code com}, but not for {@code google} or
   * {@code google.foo}. This is the recommended method for determining whether a domain is
   * potentially an addressable host.
   *
   * @since 6.0
   */
  public boolean hasPublicSuffix() {
    return publicSuffixIndex != NO_PUBLIC_SUFFIX_FOUND;
  }

  /**
   * Returns the {@linkplain #isPublicSuffix() public suffix} portion of the domain name, or
   * {@code null} if no public suffix is present.
   *
   * @since 6.0
   */
  public InternetDomainName publicSuffix() {
    return hasPublicSuffix() ? ancestor(publicSuffixIndex) : null;
  }

  /**
   * Indicates whether this domain name ends in a {@linkplain #isPublicSuffix() public suffix},
   * while not being a public suffix itself. For example, returns {@code true} for
   * {@code www.google.com}, {@code foo.co.uk} and {@code bar.ca.us}, but not for {@code google},
   * {@code com}, or {@code
   * google.foo}.
   *
   * <p><b>Warning:</b> a {@code false} result from this method does not imply that the domain does
   * not represent an addressable host, as many public suffixes are also addressable hosts. Use
   * {@link #hasPublicSuffix()} for that test.
   *
   * <p>This method can be used to determine whether it will probably be possible to set cookies on
   * the domain, though even that depends on individual browsers' implementations of cookie
   * controls. See <a href="http://www.ietf.org/rfc/rfc2109.txt">RFC 2109</a> for details.
   *
   * @since 6.0
   */
  public boolean isUnderPublicSuffix() {
    return publicSuffixIndex > 0;
  }

  /**
   * Indicates whether this domain name is composed of exactly one subdomain component followed by a
   * {@linkplain #isPublicSuffix() public suffix}. For example, returns {@code true} for
   * {@code google.com} and {@code foo.co.uk}, but not for {@code www.google.com} or {@code co.uk}.
   *
   * <p><b>Warning:</b> A {@code true} result from this method does not imply that the domain is at
   * the highest level which is addressable as a host, as many public suffixes are also addressable
   * hosts. For example, the domain {@code bar.uk.com} has a public suffix of {@code uk.com}, so it
   * would return {@code true} from this method. But {@code uk.com} is itself an addressable host.
   *
   * <p>This method can be used to determine whether a domain is probably the highest level for
   * which cookies may be set, though even that depends on individual browsers' implementations of
   * cookie controls. See <a href="http://www.ietf.org/rfc/rfc2109.txt">RFC 2109</a> for details.
   *
   * @since 6.0
   */
  public boolean isTopPrivateDomain() {
    return publicSuffixIndex == 1;
  }

  /**
   * Returns the portion of this domain name that is one level beneath the public suffix. For
   * example, for {@code x.adwords.google.co.uk} it returns {@code google.co.uk}, since
   * {@code co.uk} is a public suffix.
   *
   * <p>If {@link #isTopPrivateDomain()} is true, the current domain name instance is returned.
   *
   * <p>This method should not be used to determine the topmost parent domain which is addressable
   * as a host, as many public suffixes are also addressable hosts. For example, the domain
   * {@code foo.bar.uk.com} has a public suffix of {@code uk.com}, so it would return
   * {@code bar.uk.com} from this method. But {@code uk.com} is itself an addressable host.
   *
   * <p>This method can be used to determine the probable highest level parent domain for which
   * cookies may be set, though even that depends on individual browsers' implementations of cookie
   * controls.
   *
   * @throws IllegalStateException if this domain does not end with a public suffix
   * @since 6.0
   */
  public InternetDomainName topPrivateDomain() {
    if (isTopPrivateDomain()) {
      return this;
    }
    checkState(isUnderPublicSuffix(), "Not under a public suffix: %s", name);
    return ancestor(publicSuffixIndex - 1);
  }

  /**
   * Indicates whether this domain is composed of two or more parts.
   */
  public boolean hasParent() {
    return parts.size() > 1;
  }

  /**
   * Returns an {@code InternetDomainName} that is the immediate ancestor of this one; that is, the
   * current domain with the leftmost part removed. For example, the parent of
   * {@code www.google.com} is {@code google.com}.
   *
   * @throws IllegalStateException if the domain has no parent, as determined by {@link #hasParent}
   */
  public InternetDomainName parent() {
    checkState(hasParent(), "Domain '%s' has no parent", name);
    return ancestor(1);
  }

  /**
   * Returns the ancestor of the current domain at the given number of levels "higher" (rightward)
   * in the subdomain list. The number of levels must be non-negative, and less than {@code N-1},
   * where {@code N} is the number of parts in the domain.
   *
   * <p>TODO: Reasonable candidate for addition to public API.
   */
  private InternetDomainName ancestor(int levels) {
    return from(DOT_JOINER.join(parts.subList(levels, parts.size())));
  }

  /**
   * Creates and returns a new {@code InternetDomainName} by prepending the argument and a dot to
   * the current name. For example, {@code
   * InternetDomainName.from("foo.com").child("www.bar")} returns a new {@code InternetDomainName}
   * with the value {@code www.bar.foo.com}. Only lenient validation is performed, as described
   * {@link #from(String) here}.
   *
   * @throws NullPointerException if leftParts is null
   * @throws IllegalArgumentException if the resulting name is not valid
   */
  public InternetDomainName child(String leftParts) {
    return from(checkNotNull(leftParts) + "." + name);
  }

  /**
   * Indicates whether the argument is a syntactically valid domain name using lenient validation.
   * Specifically, validation against <a href="http://www.ietf.org/rfc/rfc3490.txt">RFC 3490</a>
   * ("Internationalizing Domain Names in Applications") is skipped.
   *
   * <p>The following two code snippets are equivalent:
   *
   * <pre>   {@code
   *   domainName = InternetDomainName.isValid(name)
   *       ? InternetDomainName.from(name)
   *       : DEFAULT_DOMAIN;}</pre>
   *
   * <pre>   {@code
   *   try {
   *     domainName = InternetDomainName.from(name);
   *   } catch (IllegalArgumentException e) {
   *     domainName = DEFAULT_DOMAIN;
   *   }}</pre>
   *
   * @since 8.0 (previously named {@code isValidLenient})
   */
  public static boolean isValid(String name) {
    try {
      from(name);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * Does the domain name match one of the "wildcard" patterns (e.g. {@code "*.ar"})?
   */
  private static boolean matchesWildcardPublicSuffix(String domain) {
    final String[] pieces = domain.split(DOT_REGEX, 2);
    return pieces.length == 2 && PublicSuffixPatterns.UNDER.containsKey(pieces[1]);
  }

  /**
   * Returns the domain name, normalized to all lower case.
   */
  @Override
  public String toString() {
    return name;
  }

  /**
   * Equality testing is based on the text supplied by the caller, after normalization as described
   * in the class documentation. For example, a non-ASCII Unicode domain name and the Punycode
   * version of the same domain name would not be considered equal.
   *
   */
  @Override
  public boolean equals(@Nullable Object object) {
    if (object == this) {
      return true;
    }

    if (object instanceof InternetDomainName) {
      InternetDomainName that = (InternetDomainName) object;
      return this.name.equals(that.name);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }
}
