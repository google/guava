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

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Ascii;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.thirdparty.publicsuffix.PublicSuffixPatterns;
import com.google.thirdparty.publicsuffix.PublicSuffixType;
import java.util.List;
import javax.annotation.CheckForNull;

/**
 * An immutable well-formed internet domain name, such as {@code com} or {@code foo.co.uk}. Only
 * syntactic analysis is performed; no DNS lookups or other network interactions take place. Thus
 * there is no guarantee that the domain actually exists on the internet.
 *
 * <p>One common use of this class is to determine whether a given string is likely to represent an
 * addressable domain on the web -- that is, for a candidate string {@code "xxx"}, might browsing to
 * {@code "http://xxx/"} result in a webpage being displayed? In the past, this test was frequently
 * done by determining whether the domain ended with a {@linkplain #isPublicSuffix() public suffix}
 * but was not itself a public suffix. However, this test is no longer accurate. There are many
 * domains which are both public suffixes and addressable as hosts; {@code "uk.com"} is one example.
 * Using the subset of public suffixes that are {@linkplain #isRegistrySuffix() registry suffixes},
 * one can get a better result, as only a few registry suffixes are addressable. However, the most
 * useful test to determine if a domain is a plausible web host is {@link #hasPublicSuffix()}. This
 * will return {@code true} for many domains which (currently) are not hosts, such as {@code "com"},
 * but given that any public suffix may become a host without warning, it is better to err on the
 * side of permissiveness and thus avoid spurious rejection of valid sites. Of course, to actually
 * determine addressability of any host, clients of this class will need to perform their own DNS
 * lookups.
 *
 * <p>During construction, names are normalized in two ways:
 *
 * <ol>
 *   <li>ASCII uppercase characters are converted to lowercase.
 *   <li>Unicode dot separators other than the ASCII period ({@code '.'}) are converted to the ASCII
 *       period.
 * </ol>
 *
 * <p>The normalized values will be returned from {@link #toString()} and {@link #parts()}, and will
 * be reflected in the result of {@link #equals(Object)}.
 *
 * <p><a href="http://en.wikipedia.org/wiki/Internationalized_domain_name">Internationalized domain
 * names</a> such as {@code 网络.cn} are supported, as are the equivalent <a
 * href="http://en.wikipedia.org/wiki/Internationalized_domain_name">IDNA Punycode-encoded</a>
 * versions.
 *
 * @author Catherine Berry
 * @since 5.0
 */
@GwtCompatible(emulated = true)
@Immutable
@ElementTypesAreNonnullByDefault
public final class InternetDomainName {

  private static final CharMatcher DOTS_MATCHER = CharMatcher.anyOf(".\u3002\uFF0E\uFF61");
  private static final Splitter DOT_SPLITTER = Splitter.on('.');
  private static final Joiner DOT_JOINER = Joiner.on('.');

  /**
   * Value of {@link #publicSuffixIndex()} or {@link #registrySuffixIndex()} which indicates that no
   * relevant suffix was found.
   */
  private static final int NO_SUFFIX_FOUND = -1;

  /**
   * Value of {@link #publicSuffixIndexCache} or {@link #registrySuffixIndexCache} which indicates
   * that they were not initialized yet.
   */
  private static final int SUFFIX_NOT_INITIALIZED = -2;

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
   * Maximum size of a single part of a domain name. See <a
   * href="http://www.ietf.org/rfc/rfc2181.txt">RFC 2181</a> part 11.
   */
  private static final int MAX_DOMAIN_PART_LENGTH = 63;

  /** The full domain name, converted to lower case. */
  private final String name;

  /** The parts of the domain name, converted to lower case. */
  private final ImmutableList<String> parts;

  /**
   * Cached value of #publicSuffixIndex(). Do not use directly.
   *
   * <p>Since this field isn't {@code volatile}, if an instance of this class is shared across
   * threads before it is initialized, then each thread is likely to compute their own copy of the
   * value.
   */
  @SuppressWarnings("Immutable")
  @LazyInit
  private int publicSuffixIndexCache = SUFFIX_NOT_INITIALIZED;

  /**
   * Cached value of #registrySuffixIndex(). Do not use directly.
   *
   * <p>Since this field isn't {@code volatile}, if an instance of this class is shared across
   * threads before it is initialized, then each thread is likely to compute their own copy of the
   * value.
   */
  @SuppressWarnings("Immutable")
  @LazyInit
  private int registrySuffixIndexCache = SUFFIX_NOT_INITIALIZED;

  /** Constructor used to implement {@link #from(String)}, and from subclasses. */
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
  }

  /**
   * The index in the {@link #parts()} list at which the public suffix begins. For example, for the
   * domain name {@code myblog.blogspot.co.uk}, the value would be 1 (the index of the {@code
   * blogspot} part). The value is negative (specifically, {@link #NO_SUFFIX_FOUND}) if no public
   * suffix was found.
   */
  private int publicSuffixIndex() {
    int publicSuffixIndexLocal = publicSuffixIndexCache;
    if (publicSuffixIndexLocal == SUFFIX_NOT_INITIALIZED) {
      publicSuffixIndexCache =
          publicSuffixIndexLocal = findSuffixOfType(Optional.<PublicSuffixType>absent());
    }
    return publicSuffixIndexLocal;
  }

  /**
   * The index in the {@link #parts()} list at which the registry suffix begins. For example, for
   * the domain name {@code myblog.blogspot.co.uk}, the value would be 2 (the index of the {@code
   * co} part). The value is negative (specifically, {@link #NO_SUFFIX_FOUND}) if no registry suffix
   * was found.
   */
  private int registrySuffixIndex() {
    int registrySuffixIndexLocal = registrySuffixIndexCache;
    if (registrySuffixIndexLocal == SUFFIX_NOT_INITIALIZED) {
      registrySuffixIndexCache =
          registrySuffixIndexLocal = findSuffixOfType(Optional.of(PublicSuffixType.REGISTRY));
    }
    return registrySuffixIndexLocal;
  }

  /**
   * Returns the index of the leftmost part of the suffix, or -1 if not found. Note that the value
   * defined as a suffix may not produce {@code true} results from {@link #isPublicSuffix()} or
   * {@link #isRegistrySuffix()} if the domain ends with an excluded domain pattern such as {@code
   * "nhs.uk"}.
   *
   * <p>If a {@code desiredType} is specified, this method only finds suffixes of the given type.
   * Otherwise, it finds the first suffix of any type.
   */
  private int findSuffixOfType(Optional<PublicSuffixType> desiredType) {
    int partsSize = parts.size();

    for (int i = 0; i < partsSize; i++) {
      String ancestorName = DOT_JOINER.join(parts.subList(i, partsSize));

      if (i > 0
          && matchesType(
              desiredType, Optional.fromNullable(PublicSuffixPatterns.UNDER.get(ancestorName)))) {
        return i - 1;
      }

      if (matchesType(
          desiredType, Optional.fromNullable(PublicSuffixPatterns.EXACT.get(ancestorName)))) {
        return i;
      }

      // Excluded domains (e.g. !nhs.uk) use the next highest
      // domain as the effective public suffix (e.g. uk).

      if (PublicSuffixPatterns.EXCLUDED.containsKey(ancestorName)) {
        return i + 1;
      }
    }

    return NO_SUFFIX_FOUND;
  }

  /**
   * Returns an instance of {@link InternetDomainName} after lenient validation. Specifically,
   * validation against <a href="http://www.ietf.org/rfc/rfc3490.txt">RFC 3490</a>
   * ("Internationalizing Domain Names in Applications") is skipped, while validation against <a
   * href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a> is relaxed in the following ways:
   *
   * <ul>
   *   <li>Any part containing non-ASCII characters is considered valid.
   *   <li>Underscores ('_') are permitted wherever dashes ('-') are permitted.
   *   <li>Parts other than the final part may start with a digit, as mandated by <a
   *       href="https://tools.ietf.org/html/rfc1123#section-2">RFC 1123</a>.
   * </ul>
   *
   * @param domain A domain name (not IP address)
   * @throws IllegalArgumentException if {@code domain} is not syntactically valid according to
   *     {@link #isValid}
   * @since 10.0 (previously named {@code fromLenient})
   */
  @CanIgnoreReturnValue // TODO(b/219820829): consider removing
  public static InternetDomainName from(String domain) {
    return new InternetDomainName(checkNotNull(domain));
  }

  /**
   * Validation method used by {@code from} to ensure that the domain name is syntactically valid
   * according to RFC 1035.
   *
   * @return Is the domain name syntactically valid?
   */
  private static boolean validateSyntax(List<String> parts) {
    int lastIndex = parts.size() - 1;

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

  private static final CharMatcher DIGIT_MATCHER = CharMatcher.inRange('0', '9');

  private static final CharMatcher LETTER_MATCHER =
      CharMatcher.inRange('a', 'z').or(CharMatcher.inRange('A', 'Z'));

  private static final CharMatcher PART_CHAR_MATCHER =
      DIGIT_MATCHER.or(LETTER_MATCHER).or(DASH_MATCHER);

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
     * https://groups.google.com/d/topic/google-web-toolkit-contributors/1UEzsryq1XI
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

    if (isFinalPart && DIGIT_MATCHER.matches(part.charAt(0))) {
      return false;
    }

    return true;
  }

  /**
   * Returns the individual components of this domain name, normalized to all lower case. For
   * example, for the domain name {@code mail.google.com}, this method returns the list {@code
   * ["mail", "google", "com"]}.
   */
  public ImmutableList<String> parts() {
    return parts;
  }

  /**
   * Indicates whether this domain name represents a <i>public suffix</i>, as defined by the Mozilla
   * Foundation's <a href="http://publicsuffix.org/">Public Suffix List</a> (PSL). A public suffix
   * is one under which Internet users can directly register names, such as {@code com}, {@code
   * co.uk} or {@code pvt.k12.wy.us}. Examples of domain names that are <i>not</i> public suffixes
   * include {@code google.com}, {@code foo.co.uk}, and {@code myblog.blogspot.com}.
   *
   * <p>Public suffixes are a proper superset of {@linkplain #isRegistrySuffix() registry suffixes}.
   * The list of public suffixes additionally contains privately owned domain names under which
   * Internet users can register subdomains. An example of a public suffix that is not a registry
   * suffix is {@code blogspot.com}. Note that it is true that all public suffixes <i>have</i>
   * registry suffixes, since domain name registries collectively control all internet domain names.
   *
   * <p>For considerations on whether the public suffix or registry suffix designation is more
   * suitable for your application, see <a
   * href="https://github.com/google/guava/wiki/InternetDomainNameExplained">this article</a>.
   *
   * @return {@code true} if this domain name appears exactly on the public suffix list
   * @since 6.0
   */
  public boolean isPublicSuffix() {
    return publicSuffixIndex() == 0;
  }

  /**
   * Indicates whether this domain name ends in a {@linkplain #isPublicSuffix() public suffix},
   * including if it is a public suffix itself. For example, returns {@code true} for {@code
   * www.google.com}, {@code foo.co.uk} and {@code com}, but not for {@code invalid} or {@code
   * google.invalid}. This is the recommended method for determining whether a domain is potentially
   * an addressable host.
   *
   * <p>Note that this method is equivalent to {@link #hasRegistrySuffix()} because all registry
   * suffixes are public suffixes <i>and</i> all public suffixes have registry suffixes.
   *
   * @since 6.0
   */
  public boolean hasPublicSuffix() {
    return publicSuffixIndex() != NO_SUFFIX_FOUND;
  }

  /**
   * Returns the {@linkplain #isPublicSuffix() public suffix} portion of the domain name, or {@code
   * null} if no public suffix is present.
   *
   * @since 6.0
   */
  @CheckForNull
  public InternetDomainName publicSuffix() {
    return hasPublicSuffix() ? ancestor(publicSuffixIndex()) : null;
  }

  /**
   * Indicates whether this domain name ends in a {@linkplain #isPublicSuffix() public suffix},
   * while not being a public suffix itself. For example, returns {@code true} for {@code
   * www.google.com}, {@code foo.co.uk} and {@code myblog.blogspot.com}, but not for {@code com},
   * {@code co.uk}, {@code google.invalid}, or {@code blogspot.com}.
   *
   * <p>This method can be used to determine whether it will probably be possible to set cookies on
   * the domain, though even that depends on individual browsers' implementations of cookie
   * controls. See <a href="http://www.ietf.org/rfc/rfc2109.txt">RFC 2109</a> for details.
   *
   * @since 6.0
   */
  public boolean isUnderPublicSuffix() {
    return publicSuffixIndex() > 0;
  }

  /**
   * Indicates whether this domain name is composed of exactly one subdomain component followed by a
   * {@linkplain #isPublicSuffix() public suffix}. For example, returns {@code true} for {@code
   * google.com} {@code foo.co.uk}, and {@code myblog.blogspot.com}, but not for {@code
   * www.google.com}, {@code co.uk}, or {@code blogspot.com}.
   *
   * <p>This method can be used to determine whether a domain is probably the highest level for
   * which cookies may be set, though even that depends on individual browsers' implementations of
   * cookie controls. See <a href="http://www.ietf.org/rfc/rfc2109.txt">RFC 2109</a> for details.
   *
   * @since 6.0
   */
  public boolean isTopPrivateDomain() {
    return publicSuffixIndex() == 1;
  }

  /**
   * Returns the portion of this domain name that is one level beneath the {@linkplain
   * #isPublicSuffix() public suffix}. For example, for {@code x.adwords.google.co.uk} it returns
   * {@code google.co.uk}, since {@code co.uk} is a public suffix. Similarly, for {@code
   * myblog.blogspot.com} it returns the same domain, {@code myblog.blogspot.com}, since {@code
   * blogspot.com} is a public suffix.
   *
   * <p>If {@link #isTopPrivateDomain()} is true, the current domain name instance is returned.
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
    return ancestor(publicSuffixIndex() - 1);
  }

  /**
   * Indicates whether this domain name represents a <i>registry suffix</i>, as defined by a subset
   * of the Mozilla Foundation's <a href="http://publicsuffix.org/">Public Suffix List</a> (PSL). A
   * registry suffix is one under which Internet users can directly register names via a domain name
   * registrar, and have such registrations lawfully protected by internet-governing bodies such as
   * ICANN. Examples of registry suffixes include {@code com}, {@code co.uk}, and {@code
   * pvt.k12.wy.us}. Examples of domain names that are <i>not</i> registry suffixes include {@code
   * google.com} and {@code foo.co.uk}.
   *
   * <p>Registry suffixes are a proper subset of {@linkplain #isPublicSuffix() public suffixes}. The
   * list of public suffixes additionally contains privately owned domain names under which Internet
   * users can register subdomains. An example of a public suffix that is not a registry suffix is
   * {@code blogspot.com}. Note that it is true that all public suffixes <i>have</i> registry
   * suffixes, since domain name registries collectively control all internet domain names.
   *
   * <p>For considerations on whether the public suffix or registry suffix designation is more
   * suitable for your application, see <a
   * href="https://github.com/google/guava/wiki/InternetDomainNameExplained">this article</a>.
   *
   * @return {@code true} if this domain name appears exactly on the public suffix list as part of
   *     the registry suffix section (labelled "ICANN").
   * @since 23.3
   */
  public boolean isRegistrySuffix() {
    return registrySuffixIndex() == 0;
  }

  /**
   * Indicates whether this domain name ends in a {@linkplain #isRegistrySuffix() registry suffix},
   * including if it is a registry suffix itself. For example, returns {@code true} for {@code
   * www.google.com}, {@code foo.co.uk} and {@code com}, but not for {@code invalid} or {@code
   * google.invalid}.
   *
   * <p>Note that this method is equivalent to {@link #hasPublicSuffix()} because all registry
   * suffixes are public suffixes <i>and</i> all public suffixes have registry suffixes.
   *
   * @since 23.3
   */
  public boolean hasRegistrySuffix() {
    return registrySuffixIndex() != NO_SUFFIX_FOUND;
  }

  /**
   * Returns the {@linkplain #isRegistrySuffix() registry suffix} portion of the domain name, or
   * {@code null} if no registry suffix is present.
   *
   * @since 23.3
   */
  @CheckForNull
  public InternetDomainName registrySuffix() {
    return hasRegistrySuffix() ? ancestor(registrySuffixIndex()) : null;
  }

  /**
   * Indicates whether this domain name ends in a {@linkplain #isRegistrySuffix() registry suffix},
   * while not being a registry suffix itself. For example, returns {@code true} for {@code
   * www.google.com}, {@code foo.co.uk} and {@code blogspot.com}, but not for {@code com}, {@code
   * co.uk}, or {@code google.invalid}.
   *
   * @since 23.3
   */
  public boolean isUnderRegistrySuffix() {
    return registrySuffixIndex() > 0;
  }

  /**
   * Indicates whether this domain name is composed of exactly one subdomain component followed by a
   * {@linkplain #isRegistrySuffix() registry suffix}. For example, returns {@code true} for {@code
   * google.com}, {@code foo.co.uk}, and {@code blogspot.com}, but not for {@code www.google.com},
   * {@code co.uk}, or {@code myblog.blogspot.com}.
   *
   * <p><b>Warning:</b> This method should not be used to determine the probable highest level
   * parent domain for which cookies may be set. Use {@link #topPrivateDomain()} for that purpose.
   *
   * @since 23.3
   */
  public boolean isTopDomainUnderRegistrySuffix() {
    return registrySuffixIndex() == 1;
  }

  /**
   * Returns the portion of this domain name that is one level beneath the {@linkplain
   * #isRegistrySuffix() registry suffix}. For example, for {@code x.adwords.google.co.uk} it
   * returns {@code google.co.uk}, since {@code co.uk} is a registry suffix. Similarly, for {@code
   * myblog.blogspot.com} it returns {@code blogspot.com}, since {@code com} is a registry suffix.
   *
   * <p>If {@link #isTopDomainUnderRegistrySuffix()} is true, the current domain name instance is
   * returned.
   *
   * <p><b>Warning:</b> This method should not be used to determine whether a domain is probably the
   * highest level for which cookies may be set. Use {@link #isTopPrivateDomain()} for that purpose.
   *
   * @throws IllegalStateException if this domain does not end with a registry suffix
   * @since 23.3
   */
  public InternetDomainName topDomainUnderRegistrySuffix() {
    if (isTopDomainUnderRegistrySuffix()) {
      return this;
    }
    checkState(isUnderRegistrySuffix(), "Not under a registry suffix: %s", name);
    return ancestor(registrySuffixIndex() - 1);
  }

  /** Indicates whether this domain is composed of two or more parts. */
  public boolean hasParent() {
    return parts.size() > 1;
  }

  /**
   * Returns an {@code InternetDomainName} that is the immediate ancestor of this one; that is, the
   * current domain with the leftmost part removed. For example, the parent of {@code
   * www.google.com} is {@code google.com}.
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
   * the current name. For example, {@code InternetDomainName.from("foo.com").child("www.bar")}
   * returns a new {@code InternetDomainName} with the value {@code www.bar.foo.com}. Only lenient
   * validation is performed, as described {@link #from(String) here}.
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
   * <pre>{@code
   * domainName = InternetDomainName.isValid(name)
   *     ? InternetDomainName.from(name)
   *     : DEFAULT_DOMAIN;
   * }</pre>
   *
   * <pre>{@code
   * try {
   *   domainName = InternetDomainName.from(name);
   * } catch (IllegalArgumentException e) {
   *   domainName = DEFAULT_DOMAIN;
   * }
   * }</pre>
   *
   * @since 8.0 (previously named {@code isValidLenient})
   */
  public static boolean isValid(String name) {
    try {
      InternetDomainName unused = from(name);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * If a {@code desiredType} is specified, returns true only if the {@code actualType} is
   * identical. Otherwise, returns true as long as {@code actualType} is present.
   */
  private static boolean matchesType(
      Optional<PublicSuffixType> desiredType, Optional<PublicSuffixType> actualType) {
    return desiredType.isPresent() ? desiredType.equals(actualType) : actualType.isPresent();
  }

  /** Returns the domain name, normalized to all lower case. */
  @Override
  public String toString() {
    return name;
  }

  /**
   * Equality testing is based on the text supplied by the caller, after normalization as described
   * in the class documentation. For example, a non-ASCII Unicode domain name and the Punycode
   * version of the same domain name would not be considered equal.
   */
  @Override
  public boolean equals(@CheckForNull Object object) {
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
