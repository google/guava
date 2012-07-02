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

import static com.google.common.base.CharMatcher.ASCII;
import static com.google.common.base.CharMatcher.JAVA_ISO_CONTROL;
import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Ascii;
import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Represents an <a href="http://en.wikipedia.org/wiki/Internet_media_type">Internet Media Type</a>
 * (also known as a MIME Type or Content Type). This class also supports the concept of media ranges
 * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1">defined by HTTP/1.1</a>.
 * As such, the {@code *} character is treated as a wildcard and is used to represent any acceptable
 * type or subtype value. A media type may not have wildcard type with a declared subtype. The
 * {@code *} character has no special meaning as part of a parameter. All values for type, subtype,
 * parameter attributes or parameter values must be valid according to RFCs
 * <a href="http://www.ietf.org/rfc/rfc2045.txt">2045</a> and
 * <a href="http://www.ietf.org/rfc/rfc2046.txt">2046</a>.
 *
 * <p>All portions of the media type that are case-insensitive (type, subtype, parameter attributes)
 * are normalized to lowercase. The value of the {@code charset} parameter is normalized to
 * lowercase, but all others are left as-is.
 *
 * <p>Note that this specifically does <strong>not</strong> represent the value of the MIME
 * {@code Content-Type} header and as such has no support for header-specific considerations such as
 * line folding and comments.
 *
 * <p>For media types that take a charset the predefined constants default to UTF-8 and have a
 * "_UTF_8" suffix. To get a version without a character set, use {@link #withoutParameters}.
 *
 * @since 12.0
 *
 * @author Gregory Kick
 */
@Beta
@GwtCompatible
@Immutable
public final class MediaType {
  private static final String CHARSET_ATTRIBUTE = "charset";
  private static final ImmutableListMultimap<String, String> UTF_8_CONSTANT_PARAMETERS =
      ImmutableListMultimap.of(CHARSET_ATTRIBUTE, Ascii.toLowerCase(UTF_8.name()));

  /** Matcher for type, subtype and attributes. */
  private static final CharMatcher TOKEN_MATCHER = ASCII.and(JAVA_ISO_CONTROL.negate())
      .and(CharMatcher.isNot(' '))
      .and(CharMatcher.noneOf("()<>@,;:\\\"/[]?="));
  private static final CharMatcher QUOTED_TEXT_MATCHER = ASCII
      .and(CharMatcher.noneOf("\"\\\r"));
  /*
   * This matches the same characters as linear-white-space from RFC 822, but we make no effort to
   * enforce any particular rules with regards to line folding as stated in the class docs.
   */
  private static final CharMatcher LINEAR_WHITE_SPACE = CharMatcher.anyOf(" \t\r\n");

  // TODO(gak): make these public?
  private static final String APPLICATION_TYPE = "application";
  private static final String AUDIO_TYPE = "audio";
  private static final String IMAGE_TYPE = "image";
  private static final String TEXT_TYPE = "text";
  private static final String VIDEO_TYPE = "video";

  private static final String WILDCARD = "*";

  /*
   * The following constants are grouped by their type and ordered alphabetically by the constant
   * name within that type. The constant name should be a sensible identifier that is closest to the
   * "common name" of the media.  This is often, but not necessarily the same as the subtype.
   *
   * Be sure to declare all constants with the type and subtype in all lowercase.
   *
   * When adding constants, be sure to add an entry into the KNOWN_TYPES map. For types that
   * take a charset (e.g. all text/* types), default to UTF-8 and suffix with "_UTF_8".
   */

  public static final MediaType ANY_TYPE = createConstant(WILDCARD, WILDCARD);
  public static final MediaType ANY_TEXT_TYPE = createConstant(TEXT_TYPE, WILDCARD);
  public static final MediaType ANY_IMAGE_TYPE = createConstant(IMAGE_TYPE, WILDCARD);
  public static final MediaType ANY_AUDIO_TYPE = createConstant(AUDIO_TYPE, WILDCARD);
  public static final MediaType ANY_VIDEO_TYPE = createConstant(VIDEO_TYPE, WILDCARD);
  public static final MediaType ANY_APPLICATION_TYPE = createConstant(APPLICATION_TYPE, WILDCARD);

  /* text types */
  public static final MediaType CACHE_MANIFEST_UTF_8 =
      createConstantUtf8(TEXT_TYPE, "cache-manifest");
  public static final MediaType CSS_UTF_8 = createConstantUtf8(TEXT_TYPE, "css");
  public static final MediaType CSV_UTF_8 = createConstantUtf8(TEXT_TYPE, "csv");
  public static final MediaType HTML_UTF_8 = createConstantUtf8(TEXT_TYPE, "html");
  public static final MediaType I_CALENDAR_UTF_8 = createConstantUtf8(TEXT_TYPE, "calendar");
  public static final MediaType PLAIN_TEXT_UTF_8 = createConstantUtf8(TEXT_TYPE, "plain");
  /**
   * <a href="http://www.rfc-editor.org/rfc/rfc4329.txt">RFC 4329</a> declares
   * {@link #JAVASCRIPT_UTF_8 application/javascript} to be the correct media type for JavaScript,
   * but this may be necessary in certain situations for compatibility.
   */
  public static final MediaType TEXT_JAVASCRIPT_UTF_8 = createConstantUtf8(TEXT_TYPE, "javascript");
  public static final MediaType VCARD_UTF_8 = createConstantUtf8(TEXT_TYPE, "vcard");
  public static final MediaType WML_UTF_8 = createConstantUtf8(TEXT_TYPE, "vnd.wap.wml");
  public static final MediaType XML_UTF_8 = createConstantUtf8(TEXT_TYPE, "xml");

  /* image types */
  public static final MediaType BMP = createConstant(IMAGE_TYPE, "bmp");
  public static final MediaType GIF = createConstant(IMAGE_TYPE, "gif");
  public static final MediaType ICO = createConstant(IMAGE_TYPE, "vnd.microsoft.icon");
  public static final MediaType JPEG = createConstant(IMAGE_TYPE, "jpeg");
  public static final MediaType PNG = createConstant(IMAGE_TYPE, "png");
  public static final MediaType SVG_UTF_8 = createConstantUtf8(IMAGE_TYPE, "svg+xml");
  public static final MediaType TIFF = createConstant(IMAGE_TYPE, "tiff");
  public static final MediaType WEBP = createConstant(IMAGE_TYPE, "webp");

  /* audio types */
  public static final MediaType MP4_AUDIO = createConstant(AUDIO_TYPE, "mp4");
  public static final MediaType MPEG_AUDIO = createConstant(AUDIO_TYPE, "mpeg");
  public static final MediaType OGG_AUDIO = createConstant(AUDIO_TYPE, "ogg");
  public static final MediaType WEBM_AUDIO = createConstant(AUDIO_TYPE, "webm");

  /* video types */
  public static final MediaType MP4_VIDEO = createConstant(VIDEO_TYPE, "mp4");
  public static final MediaType MPEG_VIDEO = createConstant(VIDEO_TYPE, "mpeg");
  public static final MediaType OGG_VIDEO = createConstant(VIDEO_TYPE, "ogg");
  public static final MediaType QUICKTIME = createConstant(VIDEO_TYPE, "quicktime");
  public static final MediaType WEBM_VIDEO = createConstant(VIDEO_TYPE, "webm");
  public static final MediaType WMV = createConstant(VIDEO_TYPE, "x-ms-wmv");

  /* application types */
  public static final MediaType ATOM_UTF_8 = createConstantUtf8(APPLICATION_TYPE, "atom+xml");
  public static final MediaType BZIP2 = createConstant(APPLICATION_TYPE, "x-bzip2");
  public static final MediaType FORM_DATA = createConstant(APPLICATION_TYPE,
      "x-www-form-urlencoded");
  public static final MediaType GZIP = createConstant(APPLICATION_TYPE, "x-gzip");
   /**
    * <a href="http://www.rfc-editor.org/rfc/rfc4329.txt">RFC 4329</a> declares this to be the
    * correct media type for JavaScript, but {@link #TEXT_JAVASCRIPT_UTF_8 text/javascript} may be
    * necessary in certain situations for compatibility.
    */
  public static final MediaType JAVASCRIPT_UTF_8 =
      createConstantUtf8(APPLICATION_TYPE, "javascript");
  public static final MediaType JSON_UTF_8 = createConstantUtf8(APPLICATION_TYPE, "json");
  public static final MediaType KML = createConstant(APPLICATION_TYPE, "vnd.google-earth.kml+xml");
  public static final MediaType KMZ = createConstant(APPLICATION_TYPE, "vnd.google-earth.kmz");
  public static final MediaType MBOX = createConstant(APPLICATION_TYPE, "mbox");
  public static final MediaType MICROSOFT_EXCEL = createConstant(APPLICATION_TYPE, "vnd.ms-excel");
  public static final MediaType MICROSOFT_POWERPOINT =
      createConstant(APPLICATION_TYPE, "vnd.ms-powerpoint");
  public static final MediaType MICROSOFT_WORD = createConstant(APPLICATION_TYPE, "msword");
  public static final MediaType OCTET_STREAM = createConstant(APPLICATION_TYPE, "octet-stream");
  public static final MediaType OGG_CONTAINER = createConstant(APPLICATION_TYPE, "ogg");
  public static final MediaType OOXML_DOCUMENT = createConstant(APPLICATION_TYPE,
      "vnd.openxmlformats-officedocument.wordprocessingml.document");
  public static final MediaType OOXML_PRESENTATION = createConstant(APPLICATION_TYPE,
      "vnd.openxmlformats-officedocument.presentationml.presentation");
  public static final MediaType OOXML_SHEET =
      createConstant(APPLICATION_TYPE, "vnd.openxmlformats-officedocument.spreadsheetml.sheet");
  public static final MediaType OPENDOCUMENT_GRAPHICS =
      createConstant(APPLICATION_TYPE, "vnd.oasis.opendocument.graphics");
  public static final MediaType OPENDOCUMENT_PRESENTATION =
      createConstant(APPLICATION_TYPE, "vnd.oasis.opendocument.presentation");
  public static final MediaType OPENDOCUMENT_SPREADSHEET =
      createConstant(APPLICATION_TYPE, "vnd.oasis.opendocument.spreadsheet");
  public static final MediaType OPENDOCUMENT_TEXT =
      createConstant(APPLICATION_TYPE, "vnd.oasis.opendocument.text");
  public static final MediaType PDF = createConstant(APPLICATION_TYPE, "pdf");
  public static final MediaType POSTSCRIPT = createConstant(APPLICATION_TYPE, "postscript");
  public static final MediaType RTF_UTF_8 = createConstantUtf8(APPLICATION_TYPE, "rtf");
  public static final MediaType SHOCKWAVE_FLASH = createConstant(APPLICATION_TYPE,
      "x-shockwave-flash");
  public static final MediaType SKETCHUP = createConstant(APPLICATION_TYPE, "vnd.sketchup.skp");
  public static final MediaType TAR = createConstant(APPLICATION_TYPE, "x-tar");
  public static final MediaType XHTML_UTF_8 = createConstantUtf8(APPLICATION_TYPE, "xhtml+xml");
  public static final MediaType ZIP = createConstant(APPLICATION_TYPE, "zip");

  private static final ImmutableMap<MediaType, MediaType> KNOWN_TYPES =
      new ImmutableMap.Builder<MediaType, MediaType>()
          .put(ANY_TYPE, ANY_TYPE)
          .put(ANY_TEXT_TYPE, ANY_TEXT_TYPE)
          .put(ANY_IMAGE_TYPE, ANY_IMAGE_TYPE)
          .put(ANY_AUDIO_TYPE, ANY_AUDIO_TYPE)
          .put(ANY_VIDEO_TYPE, ANY_VIDEO_TYPE)
          .put(ANY_APPLICATION_TYPE, ANY_APPLICATION_TYPE)
          /* text types */
          .put(CACHE_MANIFEST_UTF_8, CACHE_MANIFEST_UTF_8)
          .put(CSS_UTF_8, CSS_UTF_8)
          .put(CSV_UTF_8, CSV_UTF_8)
          .put(HTML_UTF_8, HTML_UTF_8)
          .put(I_CALENDAR_UTF_8, I_CALENDAR_UTF_8)
          .put(PLAIN_TEXT_UTF_8, PLAIN_TEXT_UTF_8)
          .put(TEXT_JAVASCRIPT_UTF_8, TEXT_JAVASCRIPT_UTF_8)
          .put(VCARD_UTF_8, VCARD_UTF_8)
          .put(WML_UTF_8, WML_UTF_8)
          .put(XML_UTF_8, XML_UTF_8)
          /* image types */
          .put(BMP, BMP)
          .put(GIF, GIF)
          .put(ICO, ICO)
          .put(JPEG, JPEG)
          .put(PNG, PNG)
          .put(SVG_UTF_8, SVG_UTF_8)
          .put(TIFF, TIFF)
          .put(WEBP, WEBP)
          /* audio types */
          .put(MP4_AUDIO, MP4_AUDIO)
          .put(MPEG_AUDIO, MPEG_AUDIO)
          .put(OGG_AUDIO, OGG_AUDIO)
          .put(WEBM_AUDIO, WEBM_AUDIO)
          /* video types */
          .put(MP4_VIDEO, MP4_VIDEO)
          .put(MPEG_VIDEO, MPEG_VIDEO)
          .put(OGG_VIDEO, OGG_VIDEO)
          .put(QUICKTIME, QUICKTIME)
          .put(WEBM_VIDEO, WEBM_VIDEO)
          .put(WMV, WMV)
          /* application types */
          .put(ATOM_UTF_8, ATOM_UTF_8)
          .put(BZIP2, BZIP2)
          .put(FORM_DATA, FORM_DATA)
          .put(GZIP, GZIP)
          .put(JAVASCRIPT_UTF_8, JAVASCRIPT_UTF_8)
          .put(JSON_UTF_8, JSON_UTF_8)
          .put(KML, KML)
          .put(KMZ, KMZ)
          .put(MBOX, MBOX)
          .put(MICROSOFT_EXCEL, MICROSOFT_EXCEL)
          .put(MICROSOFT_POWERPOINT, MICROSOFT_POWERPOINT)
          .put(MICROSOFT_WORD, MICROSOFT_WORD)
          .put(OCTET_STREAM, OCTET_STREAM)
          .put(OGG_CONTAINER, OGG_CONTAINER)
          .put(OOXML_DOCUMENT, OOXML_DOCUMENT)
          .put(OOXML_PRESENTATION, OOXML_PRESENTATION)
          .put(OOXML_SHEET, OOXML_SHEET)
          .put(OPENDOCUMENT_GRAPHICS, OPENDOCUMENT_GRAPHICS)
          .put(OPENDOCUMENT_PRESENTATION, OPENDOCUMENT_PRESENTATION)
          .put(OPENDOCUMENT_SPREADSHEET, OPENDOCUMENT_SPREADSHEET)
          .put(OPENDOCUMENT_TEXT, OPENDOCUMENT_TEXT)
          .put(PDF, PDF)
          .put(POSTSCRIPT, POSTSCRIPT)
          .put(RTF_UTF_8, RTF_UTF_8)
          .put(SHOCKWAVE_FLASH, SHOCKWAVE_FLASH)
          .put(SKETCHUP, SKETCHUP)
          .put(TAR, TAR)
          .put(XHTML_UTF_8, XHTML_UTF_8)
          .put(ZIP, ZIP)
          .build();

  private final String type;
  private final String subtype;
  private final ImmutableListMultimap<String, String> parameters;

  private MediaType(String type, String subtype,
      ImmutableListMultimap<String, String> parameters) {
    this.type = type;
    this.subtype = subtype;
    this.parameters = parameters;
  }

  private static MediaType createConstant(String type, String subtype) {
    return new MediaType(type, subtype, ImmutableListMultimap.<String, String>of());
  }

  private static MediaType createConstantUtf8(String type, String subtype) {
    return new MediaType(type, subtype, UTF_8_CONSTANT_PARAMETERS);
  }

  /** Returns the top-level media type.  For example, {@code "text"} in {@code "text/plain"}. */
  public String type() {
    return type;
  }

  /** Returns the media subtype.  For example, {@code "plain"} in {@code "text/plain"}. */
  public String subtype() {
    return subtype;
  }

  /** Returns a multimap containing the parameters of this media type. */
  public ImmutableListMultimap<String, String> parameters() {
    return parameters;
  }

  private Map<String, ImmutableMultiset<String>> parametersAsMap() {
    return Maps.transformValues(parameters.asMap(),
        new Function<Collection<String>, ImmutableMultiset<String>>() {
          @Override public ImmutableMultiset<String> apply(Collection<String> input) {
            return ImmutableMultiset.copyOf(input);
          }
        });
  }

  /**
   * Returns an optional charset for the value of the charset parameter if it is specified.
   *
   * @throws IllegalStateException if multiple charset values have been set for this media type
   * @throws IllegalCharsetNameException if a charset value is present, but illegal
   * @throws UnsupportedCharsetException if a charset value is present, but no support is available
   *     in this instance of the Java virtual machine
   */
  public Optional<Charset> charset() {
    ImmutableSet<String> charsetValues = ImmutableSet.copyOf(parameters.get(CHARSET_ATTRIBUTE));
    switch (charsetValues.size()) {
      case 0:
        return Optional.absent();
      case 1:
        return Optional.of(Charset.forName(Iterables.getOnlyElement(charsetValues)));
      default:
        throw new IllegalStateException("Multiple charset values defined: " + charsetValues);
    }
  }

  /**
   * Returns a new instance with the same type and subtype as this instance, but without any
   * parameters.
   */
  public MediaType withoutParameters() {
    return parameters.isEmpty() ? this : create(type, subtype);
  }

  /**
   * <em>Replaces</em> all parameters with the given parameters.
   *
   * @throws IllegalArgumentException if any parameter or value is invalid
   */
  public MediaType withParameters(Multimap<String, String> parameters) {
    return create(type, subtype, parameters);
  }

  /**
   * <em>Replaces</em> all parameters with the given attribute with a single parameter with the
   * given value. If multiple parameters with the same attributes are necessary use
   * {@link #withParameters}. Prefer {@link #withCharset} for setting the {@code charset} parameter
   * when using a {@link Charset} object.
   *
   * @throws IllegalArgumentException if either {@code attribute} or {@code value} is invalid
   */
  public MediaType withParameter(String attribute, String value) {
    checkNotNull(attribute);
    checkNotNull(value);
    String normalizedAttribute = normalizeToken(attribute);
    ImmutableListMultimap.Builder<String, String> builder = ImmutableListMultimap.builder();
    for (Entry<String, String> entry : parameters.entries()) {
      String key = entry.getKey();
      if (!normalizedAttribute.equals(key)) {
        builder.put(key, entry.getValue());
      }
    }
    builder.put(normalizedAttribute, normalizeParameterValue(normalizedAttribute, value));
    MediaType mediaType = new MediaType(type, subtype, builder.build());
    // Return one of the constants if the media type is a known type.
    return Objects.firstNonNull(KNOWN_TYPES.get(mediaType), mediaType);
  }

  /**
   * Returns a new instance with the same type and subtype as this instance, with the
   * {@code charset} parameter set to the {@link Charset#name name} of the given charset. Only one
   * {@code charset} parameter will be present on the new instance regardless of the number set on
   * this one.
   *
   * <p>If a charset must be specified that is not supported on this JVM (and thus is not
   * representable as a {@link Charset} instance, use {@link #withParameter}.
   */
  public MediaType withCharset(Charset charset) {
    checkNotNull(charset);
    return withParameter(CHARSET_ATTRIBUTE, charset.name());
  }

  /** Returns true if either the type or subtype is the wildcard. */
  public boolean hasWildcard() {
    return WILDCARD.equals(type) || WILDCARD.equals(subtype);
  }

  /**
   * Returns {@code true} if this instance falls within the range (as defined by
   * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html">the HTTP Accept header</a>)
   * given by the argument according to three criteria:
   *
   * <ol>
   * <li>The type of the argument is the wildcard or equal to the type of this instance.
   * <li>The subtype of the argument is the wildcard or equal to the subtype of this instance.
   * <li>All of the parameters present in the argument are present in this instance.
   * </ol>
   *
   * For example: <pre>   {@code
   *   PLAIN_TEXT_UTF_8.is(PLAIN_TEXT_UTF_8) // true
   *   PLAIN_TEXT_UTF_8.is(HTML_UTF_8) // false
   *   PLAIN_TEXT_UTF_8.is(ANY_TYPE) // true
   *   PLAIN_TEXT_UTF_8.is(ANY_TEXT_TYPE) // true
   *   PLAIN_TEXT_UTF_8.is(ANY_IMAGE_TYPE) // false
   *   PLAIN_TEXT_UTF_8.is(ANY_TEXT_TYPE.withCharset(UTF_8)) // true
   *   PLAIN_TEXT_UTF_8.withoutParameters().is(ANY_TEXT_TYPE.withCharset(UTF_8)) // false
   *   PLAIN_TEXT_UTF_8.is(ANY_TEXT_TYPE.withCharset(UTF_16)) // false}</pre>
   *
   * <p>Note that while it is possible to have the same parameter declared multiple times within a
   * media type this method does not consider the number of occurrences of a parameter.  For
   * example, {@code "text/plain; charset=UTF-8"} satisfies
   * {@code "text/plain; charset=UTF-8; charset=UTF-8"}.
   */
  public boolean is(MediaType mediaTypeRange) {
    return (mediaTypeRange.type.equals(WILDCARD) || mediaTypeRange.type.equals(this.type))
        && (mediaTypeRange.subtype.equals(WILDCARD) || mediaTypeRange.subtype.equals(this.subtype))
        && this.parameters.entries().containsAll(mediaTypeRange.parameters.entries());
  }

  /**
   * Creates a new media type with the given type and subtype.
   *
   * @throws IllegalArgumentException if type or subtype is invalid or if a wildcard is used for the
   * type, but not the subtype.
   */
  public static MediaType create(String type, String subtype) {
    return create(type, subtype, ImmutableListMultimap.<String, String>of());
  }

  /**
   * Creates a media type with the "application" type and the given subtype.
   *
   * @throws IllegalArgumentException if subtype is invalid
   */
  static MediaType createApplicationType(String subtype) {
    return create(APPLICATION_TYPE, subtype);
  }

  /**
   * Creates a media type with the "audio" type and the given subtype.
   *
   * @throws IllegalArgumentException if subtype is invalid
   */
  static MediaType createAudioType(String subtype) {
    return create(AUDIO_TYPE, subtype);
  }

  /**
   * Creates a media type with the "image" type and the given subtype.
   *
   * @throws IllegalArgumentException if subtype is invalid
   */
  static MediaType createImageType(String subtype) {
    return create(IMAGE_TYPE, subtype);
  }

  /**
   * Creates a media type with the "text" type and the given subtype.
   *
   * @throws IllegalArgumentException if subtype is invalid
   */
  static MediaType createTextType(String subtype) {
    return create(TEXT_TYPE, subtype);
  }

  /**
   * Creates a media type with the "video" type and the given subtype.
   *
   * @throws IllegalArgumentException if subtype is invalid
   */
  static MediaType createVideoType(String subtype) {
    return create(VIDEO_TYPE, subtype);
  }

  private static MediaType create(String type, String subtype,
      Multimap<String, String> parameters) {
    checkNotNull(type);
    checkNotNull(subtype);
    checkNotNull(parameters);
    String normalizedType = normalizeToken(type);
    String normalizedSubtype = normalizeToken(subtype);
    checkArgument(!WILDCARD.equals(normalizedType) || WILDCARD.equals(normalizedSubtype),
        "A wildcard type cannot be used with a non-wildcard subtype");
    ImmutableListMultimap.Builder<String, String> builder = ImmutableListMultimap.builder();
    for (Entry<String, String> entry : parameters.entries()) {
      String attribute = normalizeToken(entry.getKey());
      builder.put(attribute, normalizeParameterValue(attribute, entry.getValue()));
    }
    MediaType mediaType = new MediaType(normalizedType, normalizedSubtype, builder.build());
    // Return one of the constants if the media type is a known type.
    return Objects.firstNonNull(KNOWN_TYPES.get(mediaType), mediaType);
  }

  private static String normalizeToken(String token) {
    checkArgument(TOKEN_MATCHER.matchesAllOf(token));
    return Ascii.toLowerCase(token);
  }

  private static String normalizeParameterValue(String attribute, String value) {
    return CHARSET_ATTRIBUTE.equals(attribute) ? Ascii.toLowerCase(value) : value;
  }

  /**
   * Parses a media type from its string representation.
   *
   * @throws IllegalArgumentException if the input is not parsable
   */
  public static MediaType parse(String input) {
    checkNotNull(input);
    Tokenizer tokenizer = new Tokenizer(input);
    try {
      String type = tokenizer.consumeToken(TOKEN_MATCHER);
      tokenizer.consumeCharacter('/');
      String subtype = tokenizer.consumeToken(TOKEN_MATCHER);
      ImmutableListMultimap.Builder<String, String> parameters = ImmutableListMultimap.builder();
      while (tokenizer.hasMore()) {
        tokenizer.consumeCharacter(';');
        tokenizer.consumeTokenIfPresent(LINEAR_WHITE_SPACE);
        String attribute = tokenizer.consumeToken(TOKEN_MATCHER);
        tokenizer.consumeCharacter('=');
        final String value;
        if ('"' == tokenizer.previewChar()) {
          tokenizer.consumeCharacter('"');
          StringBuilder valueBuilder = new StringBuilder();
          while ('"' != tokenizer.previewChar()) {
            if ('\\' == tokenizer.previewChar()) {
              tokenizer.consumeCharacter('\\');
              valueBuilder.append(tokenizer.consumeCharacter(ASCII));
            } else {
              valueBuilder.append(tokenizer.consumeToken(QUOTED_TEXT_MATCHER));
            }
          }
          value = valueBuilder.toString();
          tokenizer.consumeCharacter('"');
        } else {
          value = tokenizer.consumeToken(TOKEN_MATCHER);
        }
        parameters.put(attribute, value);
      }
      return create(type, subtype, parameters.build());
    } catch (IllegalStateException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private static final class Tokenizer {
    final String input;
    int position = 0;

    Tokenizer(String input) {
      this.input = input;
    }

    String consumeTokenIfPresent(CharMatcher matcher) {
      checkState(hasMore());
      int startPosition = position;
      position = matcher.negate().indexIn(input, startPosition);
      return hasMore() ? input.substring(startPosition, position) : input.substring(startPosition);
    }

    String consumeToken(CharMatcher matcher) {
      int startPosition = position;
      String token = consumeTokenIfPresent(matcher);
      checkState(position != startPosition);
      return token;
    }

    char consumeCharacter(CharMatcher matcher) {
      checkState(hasMore());
      char c = previewChar();
      checkState(matcher.matches(c));
      position++;
      return c;
    }

    char consumeCharacter(char c) {
      checkState(hasMore());
      checkState(previewChar() == c);
      position++;
      return c;
    }

    char previewChar() {
      checkState(hasMore());
      return input.charAt(position);
    }

    boolean hasMore() {
      return (position >= 0) && (position < input.length());
    }
  }

  @Override public boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof MediaType) {
      MediaType that = (MediaType) obj;
      return this.type.equals(that.type)
          && this.subtype.equals(that.subtype)
          // compare parameters regardless of order
          && this.parametersAsMap().equals(that.parametersAsMap());
    } else {
      return false;
    }
  }

  @Override public int hashCode() {
    return Objects.hashCode(type, subtype, parametersAsMap());
  }

  private static final MapJoiner PARAMETER_JOINER = Joiner.on("; ").withKeyValueSeparator("=");

  /**
   * Returns the string representation of this media type in the format described in <a
   * href="http://www.ietf.org/rfc/rfc2045.txt">RFC 2045</a>.
   */
  @Override public String toString() {
    StringBuilder builder = new StringBuilder().append(type).append('/').append(subtype);
    if (!parameters.isEmpty()) {
      builder.append("; ");
      Multimap<String, String> quotedParameters = Multimaps.transformValues(parameters,
          new Function<String, String>() {
            @Override public String apply(String value) {
              return TOKEN_MATCHER.matchesAllOf(value) ? value : escapeAndQuote(value);
            }
          });
      PARAMETER_JOINER.appendTo(builder, quotedParameters.entries());
    }
    return builder.toString();
  }

  private static String escapeAndQuote(String value) {
    StringBuilder escaped = new StringBuilder(value.length() + 16).append('"');
    for (char ch : value.toCharArray()) {
      if (ch == '\r' || ch == '\\' || ch == '"') {
        escaped.append('\\');
      }
      escaped.append(ch);
    }
    return escaped.append('"').toString();
  }

}
