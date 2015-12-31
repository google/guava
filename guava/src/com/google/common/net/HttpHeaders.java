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

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

/**
 * Contains constant definitions for the HTTP header field names. See:
 * <ul>
 * <li><a href="http://www.ietf.org/rfc/rfc2109.txt">RFC 2109</a>
 * <li><a href="http://www.ietf.org/rfc/rfc2183.txt">RFC 2183</a>
 * <li><a href="http://www.ietf.org/rfc/rfc2616.txt">RFC 2616</a>
 * <li><a href="http://www.ietf.org/rfc/rfc2965.txt">RFC 2965</a>
 * <li><a href="http://www.ietf.org/rfc/rfc5988.txt">RFC 5988</a>
 * </ul>
 *
 *
 * @author Kurt Alfred Kluever
 * @since 11.0
 */
@GwtCompatible
public final class HttpHeaders {
  private HttpHeaders() {}

  // HTTP Request and Response header fields

  /** The HTTP {@code Cache-Control} header field name. */
  public static final String CACHE_CONTROL = "Cache-Control";
  /** The HTTP {@code Content-Length} header field name. */
  public static final String CONTENT_LENGTH = "Content-Length";
  /** The HTTP {@code Content-Type} header field name. */
  public static final String CONTENT_TYPE = "Content-Type";
  /** The HTTP {@code Date} header field name. */
  public static final String DATE = "Date";
  /** The HTTP {@code Pragma} header field name. */
  public static final String PRAGMA = "Pragma";
  /** The HTTP {@code Via} header field name. */
  public static final String VIA = "Via";
  /** The HTTP {@code Warning} header field name. */
  public static final String WARNING = "Warning";

  // HTTP Request header fields

  /** The HTTP {@code Accept} header field name. */
  public static final String ACCEPT = "Accept";
  /** The HTTP {@code Accept-Charset} header field name. */
  public static final String ACCEPT_CHARSET = "Accept-Charset";
  /** The HTTP {@code Accept-Encoding} header field name. */
  public static final String ACCEPT_ENCODING = "Accept-Encoding";
  /** The HTTP {@code Accept-Language} header field name. */
  public static final String ACCEPT_LANGUAGE = "Accept-Language";
  /** The HTTP {@code Access-Control-Request-Headers} header field name. */
  public static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";
  /** The HTTP {@code Access-Control-Request-Method} header field name. */
  public static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
  /** The HTTP {@code Authorization} header field name. */
  public static final String AUTHORIZATION = "Authorization";
  /** The HTTP {@code Connection} header field name. */
  public static final String CONNECTION = "Connection";
  /** The HTTP {@code Cookie} header field name. */
  public static final String COOKIE = "Cookie";
  /** The HTTP {@code Expect} header field name. */
  public static final String EXPECT = "Expect";
  /** The HTTP {@code From} header field name. */
  public static final String FROM = "From";
  /**
   * The HTTP {@code Follow-Only-When-Prerender-Shown}</a> header field name.
   *
   * @since 17.0
   */
  @Beta
  public static final String FOLLOW_ONLY_WHEN_PRERENDER_SHOWN = "Follow-Only-When-Prerender-Shown";
  /** The HTTP {@code Host} header field name. */
  public static final String HOST = "Host";
  /** The HTTP {@code If-Match} header field name. */
  public static final String IF_MATCH = "If-Match";
  /** The HTTP {@code If-Modified-Since} header field name. */
  public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
  /** The HTTP {@code If-None-Match} header field name. */
  public static final String IF_NONE_MATCH = "If-None-Match";
  /** The HTTP {@code If-Range} header field name. */
  public static final String IF_RANGE = "If-Range";
  /** The HTTP {@code If-Unmodified-Since} header field name. */
  public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
  /** The HTTP {@code Last-Event-ID} header field name. */
  public static final String LAST_EVENT_ID = "Last-Event-ID";
  /** The HTTP {@code Max-Forwards} header field name. */
  public static final String MAX_FORWARDS = "Max-Forwards";
  /** The HTTP {@code Origin} header field name. */
  public static final String ORIGIN = "Origin";
  /** The HTTP {@code Proxy-Authorization} header field name. */
  public static final String PROXY_AUTHORIZATION = "Proxy-Authorization";
  /** The HTTP {@code Range} header field name. */
  public static final String RANGE = "Range";
  /** The HTTP {@code Referer} header field name. */
  public static final String REFERER = "Referer";
  /** The HTTP {@code TE} header field name. */
  public static final String TE = "TE";
  /** The HTTP {@code Upgrade} header field name. */
  public static final String UPGRADE = "Upgrade";
  /** The HTTP {@code User-Agent} header field name. */
  public static final String USER_AGENT = "User-Agent";

  // HTTP Response header fields

  /** The HTTP {@code Accept-Ranges} header field name. */
  public static final String ACCEPT_RANGES = "Accept-Ranges";
  /** The HTTP {@code Access-Control-Allow-Headers} header field name. */
  public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
  /** The HTTP {@code Access-Control-Allow-Methods} header field name. */
  public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
  /** The HTTP {@code Access-Control-Allow-Origin} header field name. */
  public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
  /** The HTTP {@code Access-Control-Allow-Credentials} header field name. */
  public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
  /** The HTTP {@code Access-Control-Expose-Headers} header field name. */
  public static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
  /** The HTTP {@code Access-Control-Max-Age} header field name. */
  public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
  /** The HTTP {@code Age} header field name. */
  public static final String AGE = "Age";
  /** The HTTP {@code Allow} header field name. */
  public static final String ALLOW = "Allow";
  /** The HTTP {@code Content-Disposition} header field name. */
  public static final String CONTENT_DISPOSITION = "Content-Disposition";
  /** The HTTP {@code Content-Encoding} header field name. */
  public static final String CONTENT_ENCODING = "Content-Encoding";
  /** The HTTP {@code Content-Language} header field name. */
  public static final String CONTENT_LANGUAGE = "Content-Language";
  /** The HTTP {@code Content-Location} header field name. */
  public static final String CONTENT_LOCATION = "Content-Location";
  /** The HTTP {@code Content-MD5} header field name. */
  public static final String CONTENT_MD5 = "Content-MD5";
  /** The HTTP {@code Content-Range} header field name. */
  public static final String CONTENT_RANGE = "Content-Range";
  /**
   * The HTTP <a href="http://w3.org/TR/CSP/#content-security-policy-header-field">
   * {@code Content-Security-Policy}</a> header field name.
   *
   * @since 15.0
   */
  public static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";
  /**
   * The HTTP <a href="http://w3.org/TR/CSP/#content-security-policy-report-only-header-field">
   * {@code Content-Security-Policy-Report-Only}</a> header field name.
   *
   * @since 15.0
   */
  public static final String CONTENT_SECURITY_POLICY_REPORT_ONLY =
      "Content-Security-Policy-Report-Only";
  /** The HTTP {@code ETag} header field name. */
  public static final String ETAG = "ETag";
  /** The HTTP {@code Expires} header field name. */
  public static final String EXPIRES = "Expires";
  /** The HTTP {@code Last-Modified} header field name. */
  public static final String LAST_MODIFIED = "Last-Modified";
  /** The HTTP {@code Link} header field name. */
  public static final String LINK = "Link";
  /** The HTTP {@code Location} header field name. */
  public static final String LOCATION = "Location";
  /** The HTTP {@code P3P} header field name. Limited browser support. */
  public static final String P3P = "P3P";
  /** The HTTP {@code Proxy-Authenticate} header field name. */
  public static final String PROXY_AUTHENTICATE = "Proxy-Authenticate";
  /** The HTTP {@code Refresh} header field name. Non-standard header supported by most browsers. */
  public static final String REFRESH = "Refresh";
  /** The HTTP {@code Retry-After} header field name. */
  public static final String RETRY_AFTER = "Retry-After";
  /** The HTTP {@code Server} header field name. */
  public static final String SERVER = "Server";
  /** The HTTP {@code Set-Cookie} header field name. */
  public static final String SET_COOKIE = "Set-Cookie";
  /** The HTTP {@code Set-Cookie2} header field name. */
  public static final String SET_COOKIE2 = "Set-Cookie2";
  /**
   * The HTTP <a href="http://tools.ietf.org/html/rfc6797#section-6.1">
   * {@code Strict-Transport-Security}</a> header field name.
   *
   * @since 15.0
   */
  public static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";
  /**
   * The HTTP <a href="http://www.w3.org/TR/resource-timing/#cross-origin-resources">
   * {@code Timing-Allow-Origin}</a> header field name.
   *
   * @since 15.0
   */
  public static final String TIMING_ALLOW_ORIGIN = "Timing-Allow-Origin";
  /** The HTTP {@code Trailer} header field name. */
  public static final String TRAILER = "Trailer";
  /** The HTTP {@code Transfer-Encoding} header field name. */
  public static final String TRANSFER_ENCODING = "Transfer-Encoding";
  /** The HTTP {@code Vary} header field name. */
  public static final String VARY = "Vary";
  /** The HTTP {@code WWW-Authenticate} header field name. */
  public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

  // Common, non-standard HTTP header fields

  /** The HTTP {@code DNT} header field name. */
  public static final String DNT = "DNT";
  /** The HTTP {@code X-Content-Type-Options} header field name. */
  public static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
  /** The HTTP {@code X-Do-Not-Track} header field name. */
  public static final String X_DO_NOT_TRACK = "X-Do-Not-Track";
  /** The HTTP {@code X-Forwarded-For} header field name. */
  public static final String X_FORWARDED_FOR = "X-Forwarded-For";
  /** The HTTP {@code X-Forwarded-Proto} header field name. */
  public static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";
  /** The HTTP {@code X-Frame-Options} header field name. */
  public static final String X_FRAME_OPTIONS = "X-Frame-Options";
  /** The HTTP {@code X-Powered-By} header field name. */
  public static final String X_POWERED_BY = "X-Powered-By";
  /**
   * The HTTP <a href="http://tools.ietf.org/html/draft-evans-palmer-key-pinning">
   * {@code Public-Key-Pins}</a> header field name.
   *
   * @since 15.0
   */
  @Beta
  public static final String PUBLIC_KEY_PINS = "Public-Key-Pins";
  /**
   * The HTTP <a href="http://tools.ietf.org/html/draft-evans-palmer-key-pinning">
   * {@code Public-Key-Pins-Report-Only}</a> header field name.
   *
   * @since 15.0
   */
  @Beta
  public static final String PUBLIC_KEY_PINS_REPORT_ONLY = "Public-Key-Pins-Report-Only";
  /** The HTTP {@code X-Requested-With} header field name. */
  public static final String X_REQUESTED_WITH = "X-Requested-With";
  /** The HTTP {@code X-User-IP} header field name. */
  public static final String X_USER_IP = "X-User-IP";
  /** The HTTP {@code X-XSS-Protection} header field name. */
  public static final String X_XSS_PROTECTION = "X-XSS-Protection";
  /**
   * The HTTP <a href="http://html.spec.whatwg.org/multipage/semantics.html#hyperlink-auditing">
   * {@code Ping-From}</a> header field name.
   *
   * @since 19.0
   */
  public static final String PING_FROM = "Ping-From";
  /**
   * The HTTP <a href="http://html.spec.whatwg.org/multipage/semantics.html#hyperlink-auditing">
   * {@code Ping-To}</a> header field name.
   *
   * @since 19.0
   */
  public static final String PING_TO = "Ping-To";
}
