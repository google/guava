/*
 * Copyright (C) 2009 Google Inc.
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
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

import java.util.List;

import javax.annotation.Nullable;

/**
 * An immutable well-formed internet domain name, as defined by
 * <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>, with the
 * exception that names ending in {@code "."} are not supported (as they are not
 * generally used in browsers, email, and other end-user applications. Examples
 * include {@code com} and {@code foo.co.uk}. Only syntactic analysis is
 * performed; no DNS lookups or other network interactions take place. Thus
 * there is no guarantee that the domain actually exists on the internet.
 * Invalid domain names throw {@link IllegalArgumentException} on construction.
 *
 * <p>It is often the case that domains of interest are those under a
 * {@linkplain #isPublicSuffix() public suffix} but not themselves a public
 * suffix; {@link #hasPublicSuffix()} and {@link #isTopPrivateDomain()} test for
 * this. Similarly, one often needs to obtain the domain consisting of the
 * public suffix plus one subdomain level, typically to obtain the highest-level
 * domain for which cookies may be set. Use {@link #topPrivateDomain()} for this
 * purpose.
 *
 * <p>{@linkplain #equals(Object) Equality} of domain names is case-insensitive,
 * so for convenience, the {@link #name()} and {@link #parts()} methods return
 * the lowercase form of the name.
 *
 * <p><a href="http://en.wikipedia.org/wiki/Internationalized_domain_name">
 * internationalized domain names (IDN)</a> such as {@code ??.cn} are
 * supported.
 *
 * @author Craig Berry
 * @since 5
 */
@Beta
@GwtCompatible
public final class InternetDomainName {
  private static final Splitter DOT_SPLITTER = Splitter.on('.');
  private static final Joiner DOT_JOINER = Joiner.on('.');

  /**
   * Value of {@link #publicSuffixIndex} which indicates that no public suffix
   * was found.
   */
  private static final int NO_PUBLIC_SUFFIX_FOUND = -1;

  private static final String DOT_REGEX = "\\.";

  /**
   * The full domain name, converted to lower case.
   */
  private final String name;

  /**
   * The parts of the domain name, converted to lower case.
   */
  private final ImmutableList<String> parts;

  /**
   * The index in the {@link #parts()} list at which the public suffix begins.
   * For example, for the domain name {@code www.google.co.uk}, the value would
   * be 2 (the index of the {@code co} part). The value is negative
   * (specifically, {@link #NO_PUBLIC_SUFFIX_FOUND}) if no public suffix was
   * found.
   */
  private final int publicSuffixIndex;

  /**
   * Private constructor used to implement {@link #from(String)}.
   */
  private InternetDomainName(String name) {
    this.name = name;
    this.parts = ImmutableList.copyOf(DOT_SPLITTER.split(name));
    checkArgument(validateSyntax(parts), "Not a valid domain name: '%s'", name);
    this.publicSuffixIndex = findPublicSuffix();
  }

  /**
   * Private constructor used to implement {@link #ancestor(int)}. Argument
   * parts are assumed to be valid, as they always come from an existing domain.
   */
  private InternetDomainName(List<String> parts) {
    checkArgument(!parts.isEmpty());

    this.parts = ImmutableList.copyOf(parts);
    this.name = DOT_JOINER.join(parts);
    this.publicSuffixIndex = findPublicSuffix();
  }

  /**
   * Returns the index of the leftmost part of the public suffix, or -1 if not
   * found.
   */
  private int findPublicSuffix() {
    final int partsSize = parts.size();

    for (int i = 0; i < partsSize; i++) {
      String ancestorName = DOT_JOINER.join(parts.subList(i, partsSize));

      if (isPublicSuffixInternal(ancestorName)) {
        return i;
      }
    }

    return NO_PUBLIC_SUFFIX_FOUND;
  }

  /**
   * A factory method for creating {@code InternetDomainName} objects.
   *
   * @param domain A domain name (not IP address)
   * @throws IllegalArgumentException If name is not syntactically valid
   */
  public static InternetDomainName from(String domain) {
    // RFC 1035 defines domain names to be case-insensitive; normalizing
    // to lower case allows us to simplify matching.
    return new InternetDomainName(domain.toLowerCase());
  }

  /*
   * Patterns used for validation of domain name components. We use strings
   * instead of compiled patterns to maintain GWT compatibility. Only the
   * intersection of Java regex and Javascript regex is supported. Javascript
   * regexes do not support Unicode character category matchers, so instead we
   * transform them into giant ugly character range matchers.
   *
   * TODO: These should be generated as a separate java source file as part of
   * the build process rather than being hard-coded here.
   */

  private static final String LETTER_RANGES = // \\p{L}
      "\\u0041-\\u005a\\u0061-\\u007a\\u00aa\\u00b5\\u00ba\\u00c0-\\u00d6"
      + "\\u00d8-\\u00f6\\u00f8-\\u0236\\u0250-\\u02c1\\u02c6-\\u02d1"
      + "\\u02e0-\\u02e4\\u02ee\\u037a\\u0386\\u0388-\\u038a\\u038c"
      + "\\u038e-\\u03a1\\u03a3-\\u03ce\\u03d0-\\u03f5\\u03f7-\\u03fb"
      + "\\u0400-\\u0481\\u048a-\\u04ce\\u04d0-\\u04f5\\u04f8-\\u04f9"
      + "\\u0500-\\u050f\\u0531-\\u0556\\u0559\\u0561-\\u0587\\u05d0-\\u05ea"
      + "\\u05f0-\\u05f2\\u0621-\\u063a\\u0640-\\u064a\\u066e-\\u066f"
      + "\\u0671-\\u06d3\\u06d5\\u06e5-\\u06e6\\u06ee-\\u06ef\\u06fa-\\u06fc"
      + "\\u06ff\\u0710\\u0712-\\u072f\\u074d-\\u074f\\u0780-\\u07a5\\u07b1"
      + "\\u0904-\\u0939\\u093d\\u0950\\u0958-\\u0961\\u0985-\\u098c"
      + "\\u098f-\\u0990\\u0993-\\u09a8\\u09aa-\\u09b0\\u09b2\\u09b6-\\u09b9"
      + "\\u09bd\\u09dc-\\u09dd\\u09df-\\u09e1\\u09f0-\\u09f1\\u0a05-\\u0a0a"
      + "\\u0a0f-\\u0a10\\u0a13-\\u0a28\\u0a2a-\\u0a30\\u0a32-\\u0a33"
      + "\\u0a35-\\u0a36\\u0a38-\\u0a39\\u0a59-\\u0a5c\\u0a5e\\u0a72-\\u0a74"
      + "\\u0a85-\\u0a8d\\u0a8f-\\u0a91\\u0a93-\\u0aa8\\u0aaa-\\u0ab0"
      + "\\u0ab2-\\u0ab3\\u0ab5-\\u0ab9\\u0abd\\u0ad0\\u0ae0-\\u0ae1"
      + "\\u0b05-\\u0b0c\\u0b0f-\\u0b10\\u0b13-\\u0b28\\u0b2a-\\u0b30"
      + "\\u0b32-\\u0b33\\u0b35-\\u0b39\\u0b3d\\u0b5c-\\u0b5d\\u0b5f-\\u0b61"
      + "\\u0b71\\u0b83\\u0b85-\\u0b8a\\u0b8e-\\u0b90\\u0b92-\\u0b95"
      + "\\u0b99-\\u0b9a\\u0b9c\\u0b9e-\\u0b9f\\u0ba3-\\u0ba4\\u0ba8-\\u0baa"
      + "\\u0bae-\\u0bb5\\u0bb7-\\u0bb9\\u0c05-\\u0c0c\\u0c0e-\\u0c10"
      + "\\u0c12-\\u0c28\\u0c2a-\\u0c33\\u0c35-\\u0c39\\u0c60-\\u0c61"
      + "\\u0c85-\\u0c8c\\u0c8e-\\u0c90\\u0c92-\\u0ca8\\u0caa-\\u0cb3"
      + "\\u0cb5-\\u0cb9\\u0cbd\\u0cde\\u0ce0-\\u0ce1\\u0d05-\\u0d0c"
      + "\\u0d0e-\\u0d10\\u0d12-\\u0d28\\u0d2a-\\u0d39\\u0d60-\\u0d61"
      + "\\u0d85-\\u0d96\\u0d9a-\\u0db1\\u0db3-\\u0dbb\\u0dbd\\u0dc0-\\u0dc6"
      + "\\u0e01-\\u0e30\\u0e32-\\u0e33\\u0e40-\\u0e46\\u0e81-\\u0e82\\u0e84"
      + "\\u0e87-\\u0e88\\u0e8a\\u0e8d\\u0e94-\\u0e97\\u0e99-\\u0e9f"
      + "\\u0ea1-\\u0ea3\\u0ea5\\u0ea7\\u0eaa-\\u0eab\\u0ead-\\u0eb0"
      + "\\u0eb2-\\u0eb3\\u0ebd\\u0ec0-\\u0ec4\\u0ec6\\u0edc-\\u0edd\\u0f00"
      + "\\u0f40-\\u0f47\\u0f49-\\u0f6a\\u0f88-\\u0f8b\\u1000-\\u1021"
      + "\\u1023-\\u1027\\u1029-\\u102a\\u1050-\\u1055\\u10a0-\\u10c5"
      + "\\u10d0-\\u10f8\\u1100-\\u1159\\u115f-\\u11a2\\u11a8-\\u11f9"
      + "\\u1200-\\u1206\\u1208-\\u1246\\u1248\\u124a-\\u124d\\u1250-\\u1256"
      + "\\u1258\\u125a-\\u125d\\u1260-\\u1286\\u1288\\u128a-\\u128d"
      + "\\u1290-\\u12ae\\u12b0\\u12b2-\\u12b5\\u12b8-\\u12be\\u12c0"
      + "\\u12c2-\\u12c5\\u12c8-\\u12ce\\u12d0-\\u12d6\\u12d8-\\u12ee"
      + "\\u12f0-\\u130e\\u1310\\u1312-\\u1315\\u1318-\\u131e\\u1320-\\u1346"
      + "\\u1348-\\u135a\\u13a0-\\u13f4\\u1401-\\u166c\\u166f-\\u1676"
      + "\\u1681-\\u169a\\u16a0-\\u16ea\\u1700-\\u170c\\u170e-\\u1711"
      + "\\u1720-\\u1731\\u1740-\\u1751\\u1760-\\u176c\\u176e-\\u1770"
      + "\\u1780-\\u17b3\\u17d7\\u17dc\\u1820-\\u1877\\u1880-\\u18a8"
      + "\\u1900-\\u191c\\u1950-\\u196d\\u1970-\\u1974\\u1d00-\\u1d6b"
      + "\\u1e00-\\u1e9b\\u1ea0-\\u1ef9\\u1f00-\\u1f15\\u1f18-\\u1f1d"
      + "\\u1f20-\\u1f45\\u1f48-\\u1f4d\\u1f50-\\u1f57\\u1f59\\u1f5b\\u1f5d"
      + "\\u1f5f-\\u1f7d\\u1f80-\\u1fb4\\u1fb6-\\u1fbc\\u1fbe\\u1fc2-\\u1fc4"
      + "\\u1fc6-\\u1fcc\\u1fd0-\\u1fd3\\u1fd6-\\u1fdb\\u1fe0-\\u1fec"
      + "\\u1ff2-\\u1ff4\\u1ff6-\\u1ffc\\u2071\\u207f\\u2102\\u2107"
      + "\\u210a-\\u2113\\u2115\\u2119-\\u211d\\u2124\\u2126\\u2128"
      + "\\u212a-\\u212d\\u212f-\\u2131\\u2133-\\u2139\\u213d-\\u213f"
      + "\\u2145-\\u2149\\u3005-\\u3006\\u3031-\\u3035\\u303b-\\u303c"
      + "\\u3041-\\u3096\\u309d-\\u309f\\u30a1-\\u30fa\\u30fc-\\u30ff"
      + "\\u3105-\\u312c\\u3131-\\u318e\\u31a0-\\u31b7\\u31f0-\\u31ff"
      + "\\u3400-\\u4db5\\u4e00-\\u9fa5\\ua000-\\ua48c\\uac00-\\ud7a3"
      + "\\uf900-\\ufa2d\\ufa30-\\ufa6a\\ufb00-\\ufb06\\ufb13-\\ufb17"
      + "\\ufb1d\\ufb1f-\\ufb28\\ufb2a-\\ufb36\\ufb38-\\ufb3c\\ufb3e"
      + "\\ufb40-\\ufb41\\ufb43-\\ufb44\\ufb46-\\ufbb1\\ufbd3-\\ufd3d"
      + "\\ufd50-\\ufd8f\\ufd92-\\ufdc7\\ufdf0-\\ufdfb\\ufe70-\\ufe74"
      + "\\ufe76-\\ufefc\\uff21-\\uff3a\\uff41-\\uff5a\\uff66-\\uffbe"
      + "\\uffc2-\\uffc7\\uffca-\\uffcf\\uffd2-\\uffd7\\uffda-\\uffdc";

  private static final String NUMBER_RANGES = // \\p{N}
      "\\u0030-\\u0039\\u00b2-\\u00b3\\u00b9\\u00bc-\\u00be\\u0660-\\u0669"
      + "\\u06f0-\\u06f9\\u0966-\\u096f\\u09e6-\\u09ef\\u09f4-\\u09f9"
      + "\\u0a66-\\u0a6f\\u0ae6-\\u0aef\\u0b66-\\u0b6f\\u0be7-\\u0bf2"
      + "\\u0c66-\\u0c6f\\u0ce6-\\u0cef\\u0d66-\\u0d6f\\u0e50-\\u0e59"
      + "\\u0ed0-\\u0ed9\\u0f20-\\u0f33\\u1040-\\u1049\\u1369-\\u137c"
      + "\\u16ee-\\u16f0\\u17e0-\\u17e9\\u17f0-\\u17f9\\u1810-\\u1819"
      + "\\u1946-\\u194f\\u2070\\u2074-\\u2079\\u2080-\\u2089\\u2153-\\u2183"
      + "\\u2460-\\u249b\\u24ea-\\u24ff\\u2776-\\u2793\\u3007\\u3021-\\u3029"
      + "\\u3038-\\u303a\\u3192-\\u3195\\u3220-\\u3229\\u3251-\\u325f"
      + "\\u3280-\\u3289\\u32b1-\\u32bf\\uff10-\\uff19";

  private static final String LETTER_OR_NUMBER_RANGES =
      LETTER_RANGES + NUMBER_RANGES;

  private static final String LETTER_NUMBER_OR_DASH_RANGES =
      LETTER_OR_NUMBER_RANGES + "_-";

  private static final String NORMAL_PART =
      "[" + LETTER_OR_NUMBER_RANGES + "](["
      + LETTER_NUMBER_OR_DASH_RANGES + "]*["
      + LETTER_OR_NUMBER_RANGES + "])?";
  private static final String FINAL_PART =
      "[" + LETTER_RANGES + "](["
      + LETTER_NUMBER_OR_DASH_RANGES + "]*["
      + LETTER_OR_NUMBER_RANGES + "])?";

  /**
   * Validation method used by {@from} to ensure that the domain name is
   * syntactically valid according to RFC 1035.
   *
   * @return Is the domain name syntactically valid?
   */
  private static boolean validateSyntax(List<String> parts) {
    final int lastIndex = parts.size() - 1;

    // Validate the last part specially, as it has different syntax rules.

    if (!validatePart(parts.get(lastIndex), FINAL_PART)) {
      return false;
    }

    for (int i = 0; i < lastIndex; i++) {
      String part = parts.get(i);
      if (!validatePart(part, NORMAL_PART)) {
        return false;
      }
    }

    return true;
  }

  /**
   * The maximum size of a single part of a domain name.
   */
  private static final int MAX_DOMAIN_PART_LENGTH = 63;

  /**
   * Helper method for {@link #validateSyntax(List)}. Validates that one part of
   * a domain name is valid.
   *
   * @param part The domain name part to be validated
   * @param pattern The regex pattern against which to validate
   * @return Whether the part is valid
   */
  private static boolean validatePart(String part, String pattern) {
    return part.length() <= MAX_DOMAIN_PART_LENGTH
        && part.matches(pattern);
  }

  /**
   * Returns the domain name, normalized to all lower case.
   */
  public String name() {
    return name;
  }

  /**
   * Returns the individual components of this domain name, normalized to all
   * lower case. For example, for the domain name {@code mail.google.com}, this
   * method returns the list {@code ["mail", "google", "com"]}.
   */
  public ImmutableList<String> parts() {
    return parts;
  }

  /**
   * Old location of {@link #isPublicSuffix()}.
   *
   * @deprecated use {@link #isPublicSuffix()}
   */
  @Deprecated public boolean isRecognizedTld() {
    return isPublicSuffix();
  }

  /**
   * Old location of {@link #isUnderPublicSuffix()}.
   *
   * @deprecated use {@link #isUnderPublicSuffix()}
   */
  @Deprecated public boolean isUnderRecognizedTld() {
    return isUnderPublicSuffix();
  }

  /**
   * Old location of {@link #hasPublicSuffix()}.
   *
   * @deprecated use {@link #hasPublicSuffix()}
   */
  @Deprecated public boolean hasRecognizedTld() {
    return hasPublicSuffix();
  }

  /**
   * Old location of {@link #publicSuffix()}.
   *
   * @deprecated use {@link #publicSuffix()}
   */
  @Deprecated public InternetDomainName recognizedTld() {
    return publicSuffix();
  }

  /**
   * Old location of {@link #isTopPrivateDomain()}.
   *
   * @deprecated use {@link #isTopPrivateDomain()}
   */
  @Deprecated public boolean isImmediatelyUnderTld() {
    return isTopPrivateDomain();
  }

  /**
   * Old location of {@link #topPrivateDomain()}.
   *
   * @deprecated use {@link #topPrivateDomain()}
   */
  @Deprecated public InternetDomainName topCookieDomain() {
    return topPrivateDomain();
  }

  /**
   * Returns the rightmost non-{@linkplain #isRecognizedTld() TLD} domain name
   * part.  For example
   * {@code new InternetDomainName("www.google.com").rightmostNonTldPart()}
   * returns {@code "google"}.  Returns null if either no
   * {@linkplain #isRecognizedTld() TLD} is found, or the whole domain name is
   * itself a {@linkplain #isRecognizedTld() TLD}.
   *
   * @deprecated use the first {@linkplain #parts part} of the {@link
   *     #topPrivateDomain()}
   */
  @Deprecated public String rightmostNonTldPart() {
    return publicSuffixIndex >= 1
        ? parts.get(publicSuffixIndex - 1)
        : null;
  }

  /**
   * Indicates whether this domain name represents a <i>public suffix</i>, as
   * defined by the Mozilla Foundation's
   * <a href="http://publicsuffix.org/">Public Suffix List</a> (PSL). A public
   * suffix is one under which Internet users can directly register names, such
   * as {@code com}, {@code co.uk} or {@code pvt.k12.wy.us}. Examples of domain
   * names that are <i>not</i> public suffixes include {@code google}, {@code
   * google.com} and {@code foo.co.uk}.
   *
   * @return {@code true} if this domain name appears exactly on the public
   *     suffix list
   * @since 6
   */
  public boolean isPublicSuffix() {
    return publicSuffixIndex == 0;
  }

  /**
   * Indicates whether this domain name ends in a {@linkplain #isPublicSuffix()
   * public suffix}, including if it is a public suffix itself. For example,
   * returns {@code true} for {@code www.google.com}, {@code foo.co.uk} and
   * {@code com}, but not for {@code google} or {@code google.foo}.
   *
   * @since 6
   */
  public boolean hasPublicSuffix() {
    return publicSuffixIndex != NO_PUBLIC_SUFFIX_FOUND;
  }

  /**
   * Returns the {@linkplain #isPublicSuffix() public suffix} portion of the
   * domain name, or {@code null} if no public suffix is present.
   *
   * @since 6
   */
  public InternetDomainName publicSuffix() {
    return hasPublicSuffix() ? ancestor(publicSuffixIndex) : null;
  }

  /**
   * Indicates whether this domain name ends in a {@linkplain #isPublicSuffix()
   * public suffix}, while not being a public suffix itself. For example,
   * returns {@code true} for {@code www.google.com}, {@code foo.co.uk} and
   * {@code bar.ca.us}, but not for {@code google}, {@code com}, or {@code
   * google.foo}.
   *
   * @since 6
   */
  public boolean isUnderPublicSuffix() {
    return publicSuffixIndex > 0;
  }

  /**
   * Indicates whether this domain name is composed of exactly one subdomain
   * component followed by a {@linkplain #isPublicSuffix() public suffix}. For
   * example, returns {@code true} for {@code google.com} and {@code foo.co.uk},
   * but not for {@code www.google.com} or {@code co.uk}.
   *
   * @since 6
   */
  public boolean isTopPrivateDomain() {
    return publicSuffixIndex == 1;
  }

  /**
   * Returns the portion of this domain name that is one level beneath the
   * public suffix. For example, for {@code x.adwords.google.co.uk} it returns
   * {@code google.co.uk}, since {@code co.uk} is a public suffix. This is the
   * highest-level parent of this domain for which cookies may be set, as
   * cookies cannot be set on a public suffix itself.
   *
   * <p>If {@link #isTopPrivateDomain()} is true, the current domain name
   * instance is returned.
   *
   * @throws IllegalStateException if this domain does not end with a
   *     public suffix
   * @since 6
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
   * Returns an {@code InternetDomainName} that is the immediate ancestor of
   * this one; that is, the current domain with the leftmost part removed. For
   * example, the parent of {@code www.google.com} is {@code google.com}.
   *
   * @throws IllegalStateException if the domain has no parent, as determined
   *     by {@link #hasParent}
   */
  public InternetDomainName parent() {
    checkState(hasParent(), "Domain '%s' has no parent", name);
    return ancestor(1);
  }

  /**
   * Returns the ancestor of the current domain at the given number of levels
   * "higher" (rightward) in the subdomain list. The number of levels must be
   * non-negative, and less than {@code N-1}, where {@code N} is the number of
   * parts in the domain.
   *
   * <p>TODO: Reasonable candidate for addition to public API.
   */
  private InternetDomainName ancestor(int levels) {
    return new InternetDomainName(parts.subList(levels, parts.size()));
  }

  /**
   * Creates and returns a new {@code InternetDomainName} by prepending the
   * argument and a dot to the current name. For example, {@code
   * InternetDomainName.from("foo.com").child("www.bar")} returns a new {@code
   * InternetDomainName} with the value {@code www.bar.foo.com}.
   *
   * @throws NullPointerException if leftParts is null
   * @throws IllegalArgumentException if the resulting name is not valid
   */
  public InternetDomainName child(String leftParts) {
    return InternetDomainName.from(checkNotNull(leftParts) + "." + name);
  }

  /**
   * Indicates whether the argument is a syntactically valid domain name.  This
   * method is intended for the case where a {@link String} must be validated as
   * a valid domain name, but no further work with that {@link String} as an
   * {@link InternetDomainName} will be required. Code like the following will
   * unnecessarily repeat the work of validation: <pre>   {@code
   *
   *   if (InternetDomainName.isValid(name)) {
   *     domainName = InternetDomainName.from(name);
   *   } else {
   *     domainName = DEFAULT_DOMAIN;
   *   }}</pre>
   *
   * Such code could instead be written as follows: <pre>   {@code
   *
   *   try {
   *     domainName = InternetDomainName.from(name);
   *   } catch (IllegalArgumentException e) {
   *     domainName = DEFAULT_DOMAIN;
   *   }}</pre>
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
   * Does the domain name satisfy the Mozilla criteria for a {@linkplain
   * #isPublicSuffix() public suffix}?
   */
  private static boolean isPublicSuffixInternal(String domain) {
    return TldPatterns.EXACT.contains(domain)
        || (!TldPatterns.EXCLUDED.contains(domain)
            && matchesWildcardPublicSuffix(domain));
  }

  /**
   * Does the domain name match one of the "wildcard" patterns (e.g. "*.ar")?
   */
  private static boolean matchesWildcardPublicSuffix(String domain) {
    final String[] pieces = domain.split(DOT_REGEX, 2);
    return pieces.length == 2 && TldPatterns.UNDER.contains(pieces[1]);
  }

  // TODO: specify this to return the same as name(); remove name()
  @Override
  public String toString() {
    return Objects.toStringHelper(this).add("name", name).toString();
  }

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
