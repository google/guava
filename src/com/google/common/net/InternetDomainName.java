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

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

import java.util.List;

import javax.annotation.Nullable;

/**
 * An immutable well-formed internet domain name, as defined by
 * <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>, with the
 * exception that names ending in "." are not supported (as they are not
 * generally used in browsers, email, and other end-user applications.  Examples
 * include {@code com} and {@code foo.co.uk}.  Only syntactic analysis is
 * performed; no DNS lookups or other network interactions take place.  Thus
 * there is no guarantee that the domain actually exists on the internet.
 * Invalid domain names throw {@link IllegalArgumentException} on construction.
 *
 * <p>It is often the case that domains of interest are those under
 * {@linkplain #isRecognizedTld() TLD}s but not themselves
 * {@linkplain #isRecognizedTld() TLD}s; {@link #hasRecognizedTld()} and
 * {@link #isImmediatelyUnderTld()} test for this. Similarly, one
 * often needs to obtain the domain consisting of the
 * {@linkplain #isRecognizedTld() TLD} plus one subdomain level, typically
 * to obtain the highest-level domain for which cookies may be set.
 * Use {@link #topCookieDomain()} for this purpose.
 *
 * <p>{@linkplain #equals(Object) Equality} of domain names is case-insensitive,
 * so for convenience, the {@link #name()} and {@link #parts()} methods
 * return the lower-case form of the name.
 *
 * <p>{@linkplain #isRecognizedTld() TLD} identification is done by reference
 * to the generated Java class {@link TldPatterns}, which is in turn derived
 * from a Mozilla-supplied text file listing known
 * {@linkplain #isRecognizedTld() TLD} patterns.
 *
 * <p>Note that
 * <a href="http://en.wikipedia.org/wiki/Internationalized_domain_name">
 * internationalized domain names (IDN)</a> are not supported.  If IDN is
 * required, the {@code ToASCII} transformation (described in the referenced
 * page) should be applied to the domain string before it is provided to this
 * class.
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
   * Value of {@link #tldIndex} which indicates that no TLD was found.
   */
  private static final int NO_TLD_FOUND = -1;

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
   * The index in the {@link #parts} list at which the TLD begins.  For
   * example, for the domain name {@code www.google.co.uk}, the value would
   * be 2 (the index of the {@code co} part).  The value is negative
   * (specifically, {@link #NO_TLD_FOUND}) if no TLD was found.
   */
  private final int tldIndex;

  /**
   * Private constructor used to implement {@link #from(String)}.
   */
  private InternetDomainName(String name) {
    this.name = name;
    this.parts = ImmutableList.copyOf(DOT_SPLITTER.split(name));

    Preconditions.checkArgument(
        validateSyntax(parts),
        "Not a valid domain name: '%s'",
        name);

    this.tldIndex = findTld();
  }

  /**
   * Private constructor used to implement {@link #ancestor(int)}.
   * Argument parts are assumed to be valid, as they always come
   * from an existing domain.
   */
  private InternetDomainName(List<String> parts) {
    Preconditions.checkArgument(!parts.isEmpty());

    this.parts = ImmutableList.copyOf(parts);
    this.name = DOT_JOINER.join(parts);
    this.tldIndex = findTld();
  }

  /**
   * Returns the index of the leftmost part of the TLD, or -1 if not found.
   */
  private int findTld() {
    final int partsSize = parts.size();

    for (int i = 0; i < partsSize; i++) {
      String ancestorName = DOT_JOINER.join(parts.subList(i, partsSize));

      if (isTldInternal(ancestorName)) {
        return i;
      }
    }

    return NO_TLD_FOUND;
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

  // Patterns used for validation of domain name components.
  // We use strings instead of compiled patterns to maintain GWT compatibility.
  // Only the intersection of Java regex and Javascript regex is supported.
  private static final String NORMAL_PART =
      "[A-Za-z0-9]([A-Za-z0-9_-]*[A-Za-z0-9])?";
  private static final String FINAL_PART =
      "[A-Za-z]([A-Za-z0-9_-]*[A-Za-z0-9])?";

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
   * Helper method for {@link #validateSyntax(List)}.  Validates that one
   * part of a domain name is valid.
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
   * Returns the parts of the domain name, normalized to all lower case.
   */
  public ImmutableList<String> parts() {
    return parts;
  }

  /**
   * Returns {@code true} if the domain name is an <b>effective</b> top-level
   * domain.  An effective TLD is a domain which is controlled by a national or
   * other registrar, and which is not itself in use as a real, separately
   * addressable domain name.  Addressable domains occur as subdomains of
   * effective TLDs.  Examples of TLDs include {@code com}, {@code co.uk}, and
   * {@code ca.us}.  Examples of non-TLDs include {@code google},
   * {@code google.com}, and {@code foo.co.uk}.
   *
   * <p>Identification of effective TLDs is done by reference to a list of
   * patterns maintained by the Mozilla project; see
   * <a href="https://wiki.mozilla.org/Gecko:Effective_TLD_List">the
   * Gecko:Effective TLD List project page</a> for details.
   *
   * <p>TODO: Note that the Mozilla TLD list does not guarantee that
   * the one-part TLDs like {@code com} and {@code us} will necessarily be
   * listed explicitly in the patterns file.  All of the ones required for
   * proper operation of this class appear to be there in the current version of
   * the file, but this might not always be the case.  We may wish to tighten
   * this up by providing an auxilliary source for the canonical one-part TLDs,
   * using the existing "amendment file" process or a similar mechanism.
   */
  public boolean isRecognizedTld() {
    return tldIndex == 0;
  }

  /**
   * Returns {@code true} if the domain name ends in a
   * {@linkplain #isRecognizedTld() TLD}, but is not a complete
   * {@linkplain #isRecognizedTld() TLD} itself.  For example, returns
   * {@code true} for {@code www.google.com}, {@code foo.co.uk}, and
   * {@code bar.ca.us}; returns {@code false} for {@code google}, {@code com},
   * and {@code google.foo}.
   */
  public boolean isUnderRecognizedTld() {
    return tldIndex > 0;
  }

  /**
   * Returns {@code true} if the domain name ends in a
   * {@linkplain #isRecognizedTld() TLD}, or is a complete
   * {@linkplain #isRecognizedTld() TLD} itself.  For example, returns
   * {@code true} for {@code www.google.com}, {@code foo.co.uk}, and
   * {@code com}; returns {@code false} for {@code google} and
   * {@code google.foo}.
   */
  public boolean hasRecognizedTld() {
    return tldIndex != NO_TLD_FOUND;
  }

  /**
   * Returns the {@linkplain #isRecognizedTld() TLD} portion of the
   * domain name, or null if no TLD is present according to
   * {@link #hasRecognizedTld()}.
   */
  public InternetDomainName recognizedTld() {
    return hasRecognizedTld() ? ancestor(tldIndex) : null;
  }

  /**
   * Returns {@code true} if the domain name is an immediate subdomain of a
   * {@linkplain #isRecognizedTld() TLD}, but is not a
   * {@linkplain #isRecognizedTld() TLD} itself. For example, returns
   * {@code true} for {@code google.com} and {@code foo.co.uk}; returns
   * {@code false} for {@code www.google.com} and {@code co.uk}.
   */
  public boolean isImmediatelyUnderTld() {
    return tldIndex == 1;
  }

  /**
   * Returns the rightmost non-{@linkplain #isRecognizedTld() TLD} domain name
   * part.  For example
   * {@code new InternetDomainName("www.google.com").rightmostNonTldPart()}
   * returns {@code "google"}.  Returns null if either no
   * {@linkplain #isRecognizedTld() TLD} is found, or the whole domain name is
   * itself a {@linkplain #isRecognizedTld() TLD}.
   */
  public String rightmostNonTldPart() {
    return tldIndex >= 1
        ? parts.get(tldIndex - 1)
        : null;
  }

  /**
   * Returns the "top cookie domain" for the {@code InternetDomainName}.
   * This is defined as the domain consisting of the
   * {@linkplain #isRecognizedTld() TLD} plus one subdomain level. This
   * is the highest-level parent of this domain for which cookies may be set,
   * as cookies cannot be set on {@code TLD}s themselves. Note that this
   * information has non-cookie-related uses as well, but determining the
   * cookie domain is the most common.
   *
   * <p>If called on a domain for which {@link #isImmediatelyUnderTld()} is
   * {@code true}, this is an identity operation which returns the existing
   * object.
   *
   * @throws IllegalStateException if the domain is not under a recognized TLD.
   */
  public InternetDomainName topCookieDomain() {
    if (isImmediatelyUnderTld()) {
      return this;
    }

    if (!isUnderRecognizedTld()) {
      throw new IllegalStateException("Not under TLD: " + name);
    }

    return ancestor(tldIndex - 1);
  }

  /**
   * Does this domain have a parent domain?  That is, does it have two or more
   * parts?
   */
  public boolean hasParent() {
    return parts.size() > 1;
  }

  /**
   * Create a new {@code InternetDomainName} which is the parent of this one;
   * that is, the parent domain is the current domain with the leftmost part
   * removed.  For example,
   * {@code new InternetDomainName("www.google.com").parent()} returns
   * a new {@code InternetDomainName} corresponding to the value
   * {@code "google.com"}.
   *
   * @throws IllegalStateException if the domain has no parent
   */
  public InternetDomainName parent() {
    Preconditions.checkState(hasParent(), "Domain '%s' has no parent", name);
    return ancestor(1);
  }

  /**
   * Returns the ancestor of the current domain at the given number of levels
   * "higher" (rightward) in the subdomain list. The number of levels must
   * be non-negative, and less than {@code N-1}, where {@code N} is the number
   * of parts in the domain.
   *
   * <p>TODO: Reasonable candidate for addition to public API.
   */
  private InternetDomainName ancestor(int levels) {
    return new InternetDomainName(parts.subList(levels, parts.size()));
  }

  /**
   * Creates and returns a new {@code InternetDomainName} by prepending the
   * argument and a dot to the current name.  For example,
   * {@code InternetDomainName.from("foo.com").child("www.bar")} returns a
   * new {@code InternetDomainName} with the value {@code www.bar.foo.com}.
   *
   * @throws NullPointerException if leftParts is null
   * @throws IllegalArgumentException if the resulting name is not valid
   */
  public InternetDomainName child(String leftParts) {
    Preconditions.checkNotNull(leftParts);
    return InternetDomainName.from(leftParts + "." + name);
  }

  /**
   * Determines whether the argument is a syntactically valid domain name.
   * This method is intended for the case where a {@link String} must be
   * validated as a valid domain name, but no further work with that
   * {@link String} as an {@link InternetDomainName} will be required.  Code
   * like the following will unnecessarily repeat the work of validation:
   * <pre>
   *   if (InternetDomainName.isValid(name)) {
   *     domainName = InternetDomainName.from(name);
   *   } else {
   *     domainName = DEFAULT_DOMAIN;
   *   }
   * </pre>
   * Such code should instead be written as follows:
   * <pre>
   *   try {
   *     domainName = InternetDomainName.from(name);
   *   } catch (IllegalArgumentException e) {
   *     domainName = DEFAULT_DOMAIN;
   *   }
   * </pre>
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
   * Does the domain name satisfy the Mozilla criteria for an effective
   * {@linkplain #isRecognizedTld() TLD}?
   */
  private static boolean isTldInternal(String domain) {
    return TldPatterns.EXACT.contains(domain)
        || (!TldPatterns.EXCLUDED.contains(domain)
            && isSubTld(domain));
  }

  /**
   * Does the domain name match one of the "under" patterns (e.g. "*.ar")?
   */
  private static boolean isSubTld(String domain) {
    final String[] pieces = domain.split(DOT_REGEX, 2);
    return pieces.length == 2 && TldPatterns.UNDER.contains(pieces[1]);
  }

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
