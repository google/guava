/*
 * Copyright (C) 2011 The Guava Authors
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
  public static final String CACHE_CONTROL = "cache-control";
  /** The HTTP {@code Content-Length} header field name. */
  public static final String CONTENT_LENGTH = "content-length";
  /** The HTTP {@code Content-Type} header field name. */
  public static final String CONTENT_TYPE = "content-type";
  /** The HTTP {@code Date} header field name. */
  public static final String DATE = "date";
  /** The HTTP {@code Pragma} header field name. */
  public static final String PRAGMA = "pragma";
  /** The HTTP {@code Via} header field name. */
  public static final String VIA = "via";
  /** The HTTP {@code Warning} header field name. */
  public static final String WARNING = "warning";

  // HTTP Request header fields

  /** The HTTP {@code Accept} header field name. */
  public static final String ACCEPT = "accept";
  /** The HTTP {@code Accept-Charset} header field name. */
  public static final String ACCEPT_CHARSET = "accept-charset";
  /** The HTTP {@code Accept-Encoding} header field name. */
  public static final String ACCEPT_ENCODING = "accept-encoding";
  /** The HTTP {@code Accept-Language} header field name. */
  public static final String ACCEPT_LANGUAGE = "accept-language";
  /** The HTTP {@code Access-Control-Request-Headers} header field name. */
  public static final String ACCESS_CONTROL_REQUEST_HEADERS = "access-control-request-headers";
  /** The HTTP {@code Access-Control-Request-Method} header field name. */
  public static final String ACCESS_CONTROL_REQUEST_METHOD = "access-control-request-method";
  /** The HTTP {@code Authorization} header field name. */
  public static final String AUTHORIZATION = "authorization";
  /** The HTTP {@code Connection} header field name. */
  public static final String CONNECTION = "connection";
  /** The HTTP {@code Cookie} header field name. */
  public static final String COOKIE = "cookie";
  /** The HTTP {@code Expect} header field name. */
  public static final String EXPECT = "expect";
  /** The HTTP {@code From} header field name. */
  public static final String FROM = "from";

  /**
   * The HTTP {@code Follow-Only-When-Prerender-Shown}</a> header field name.
   *
   * @since 17.0
   */
  @Beta
  public static final String FOLLOW_ONLY_WHEN_PRERENDER_SHOWN = "follow-only-when-prerender-shown";
  /** The HTTP {@code Host} header field name. */
  public static final String HOST = "host";
  /** The HTTP {@code If-Match} header field name. */
  public static final String IF_MATCH = "if-match";
  /** The HTTP {@code If-Modified-Since} header field name. */
  public static final String IF_MODIFIED_SINCE = "if-modified-since";
  /** The HTTP {@code If-None-Match} header field name. */
  public static final String IF_NONE_MATCH = "if-none-match";
  /** The HTTP {@code If-Range} header field name. */
  public static final String IF_RANGE = "if-range";
  /** The HTTP {@code If-Unmodified-Since} header field name. */
  public static final String IF_UNMODIFIED_SINCE = "if-unmodified-since";
  /** The HTTP {@code Last-Event-ID} header field name. */
  public static final String LAST_EVENT_ID = "last-event-id";
  /** The HTTP {@code Max-Forwards} header field name. */
  public static final String MAX_FORWARDS = "max-forwards";
  /** The HTTP {@code Origin} header field name. */
  public static final String ORIGIN = "origin";
  /** The HTTP {@code Proxy-Authorization} header field name. */
  public static final String PROXY_AUTHORIZATION = "proxy-authorization";
  /** The HTTP {@code Range} header field name. */
  public static final String RANGE = "range";
  /** The HTTP {@code Referer} header field name. */
  public static final String REFERER = "referer";
  /**
   * The HTTP <a href="https://www.w3.org/TR/service-workers/#update-algorithm">
   * {@code Service-Worker}</a> header field name.
   */
  public static final String SERVICE_WORKER = "service-worker";
  /** The HTTP {@code TE} header field name. */
  public static final String TE = "te";
  /** The HTTP {@code Upgrade} header field name. */
  public static final String UPGRADE = "upgrade";
  /** The HTTP {@code User-Agent} header field name. */
  public static final String USER_AGENT = "user-agent";

  // HTTP Response header fields

  /** The HTTP {@code Accept-Ranges} header field name. */
  public static final String ACCEPT_RANGES = "accept-ranges";
  /** The HTTP {@code Access-Control-Allow-Headers} header field name. */
  public static final String ACCESS_CONTROL_ALLOW_HEADERS = "access-control-allow-headers";
  /** The HTTP {@code Access-Control-Allow-Methods} header field name. */
  public static final String ACCESS_CONTROL_ALLOW_METHODS = "access-control-allow-methods";
  /** The HTTP {@code Access-Control-Allow-Origin} header field name. */
  public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "access-control-allow-origin";
  /** The HTTP {@code Access-Control-Allow-Credentials} header field name. */
  public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "access-control-allow-credentials";
  /** The HTTP {@code Access-Control-Expose-Headers} header field name. */
  public static final String ACCESS_CONTROL_EXPOSE_HEADERS = "access-control-expose-headers";
  /** The HTTP {@code Access-Control-Max-Age} header field name. */
  public static final String ACCESS_CONTROL_MAX_AGE = "access-control-max-age";
  /** The HTTP {@code Age} header field name. */
  public static final String AGE = "age";
  /** The HTTP {@code Allow} header field name. */
  public static final String ALLOW = "allow";
  /** The HTTP {@code Content-Disposition} header field name. */
  public static final String CONTENT_DISPOSITION = "content-disposition";
  /** The HTTP {@code Content-Encoding} header field name. */
  public static final String CONTENT_ENCODING = "content-encoding";
  /** The HTTP {@code Content-Language} header field name. */
  public static final String CONTENT_LANGUAGE = "content-language";
  /** The HTTP {@code Content-Location} header field name. */
  public static final String CONTENT_LOCATION = "content-location";
  /** The HTTP {@code Content-MD5} header field name. */
  public static final String CONTENT_MD5 = "content-md5";
  /** The HTTP {@code Content-Range} header field name. */
  public static final String CONTENT_RANGE = "content-range";
  /**
   * The HTTP <a href="http://w3.org/TR/CSP/#content-security-policy-header-field">
   * {@code Content-Security-Policy}</a> header field name.
   *
   * @since 15.0
   */
  public static final String CONTENT_SECURITY_POLICY = "content-security-policy";
  /**
   * The HTTP <a href="http://w3.org/TR/CSP/#content-security-policy-report-only-header-field">
   * {@code Content-Security-Policy-Report-Only}</a> header field name.
   *
   * @since 15.0
   */
  public static final String CONTENT_SECURITY_POLICY_REPORT_ONLY =
      "content-security-policy-report-only";
  /**
   * The HTTP nonstandard {@code X-Content-Security-Policy} header field name. It was introduced in
   * <a href="https://www.w3.org/TR/2011/WD-CSP-20111129/">CSP v.1</a> and used by the Firefox
   * until version 23 and the Internet Explorer version 10.
   * Please, use {@link #CONTENT_SECURITY_POLICY} to pass the CSP.
   *
   * @since 20.0
   */
  public static final String X_CONTENT_SECURITY_POLICY = "x-content-security-policy";
  /**
   * The HTTP nonstandard {@code X-Content-Security-Policy-Report-Only} header field name.
   * It was introduced in <a href="https://www.w3.org/TR/2011/WD-CSP-20111129/">CSP v.1</a> and
   * used by the Firefox until version 23 and the Internet Explorer version 10.
   * Please, use {@link #CONTENT_SECURITY_POLICY_REPORT_ONLY} to pass the CSP.
   *
   * @since 20.0
   */
  public static final String X_CONTENT_SECURITY_POLICY_REPORT_ONLY =
      "x-content-security-policy-report-only";
  /**
   * The HTTP nonstandard {@code X-WebKit-CSP} header field name. It was introduced in
   * <a href="https://www.w3.org/TR/2011/WD-CSP-20111129/">CSP v.1</a> and used by the Chrome until
   * version 25. Please, use {@link #CONTENT_SECURITY_POLICY} to pass the CSP.
   *
   * @since 20.0
   */
  public static final String X_WEBKIT_CSP = "x-webkit-csp";
  /**
   * The HTTP nonstandard {@code X-WebKit-CSP-Report-Only} header field name. It was introduced in
   * <a href="https://www.w3.org/TR/2011/WD-CSP-20111129/">CSP v.1</a> and used by the Chrome until
   * version 25. Please, use {@link #CONTENT_SECURITY_POLICY_REPORT_ONLY} to pass the CSP.
   *
   * @since 20.0
   */
  public static final String X_WEBKIT_CSP_REPORT_ONLY = "x-webkit-csp-report-only";
  /** The HTTP {@code ETag} header field name. */
  public static final String ETAG = "etag";
  /** The HTTP {@code Expires} header field name. */
  public static final String EXPIRES = "expires";
  /** The HTTP {@code Last-Modified} header field name. */
  public static final String LAST_MODIFIED = "last-modified";
  /** The HTTP {@code Link} header field name. */
  public static final String LINK = "link";
  /** The HTTP {@code Location} header field name. */
  public static final String LOCATION = "location";
  /** The HTTP {@code P3P} header field name. Limited browser support. */
  public static final String P3P = "p3p";
  /** The HTTP {@code Proxy-Authenticate} header field name. */
  public static final String PROXY_AUTHENTICATE = "proxy-authenticate";
  /** The HTTP {@code Refresh} header field name. Non-standard header supported by most browsers. */
  public static final String REFRESH = "refresh";
  /** The HTTP {@code Retry-After} header field name. */
  public static final String RETRY_AFTER = "retry-after";
  /** The HTTP {@code Server} header field name. */
  public static final String SERVER = "server";
  /**
   * The HTTP <a href="https://www.w3.org/TR/service-workers/#update-algorithm">
   * {@code Service-Worker-Allowed}</a> header field name.
   *
   * @since 20.0
   */
  public static final String SERVICE_WORKER_ALLOWED = "service-worker-allowed";
  /** The HTTP {@code Set-Cookie} header field name. */
  public static final String SET_COOKIE = "set-cookie";
  /** The HTTP {@code Set-Cookie2} header field name. */
  public static final String SET_COOKIE2 = "set-cookie2";
  /**
   * The HTTP
   * <a href="http://tools.ietf.org/html/rfc6797#section-6.1">{@code Strict-Transport-Security}</a>
   * header field name.
   *
   * @since 15.0
   */
  public static final String STRICT_TRANSPORT_SECURITY = "strict-transport-security";
  /**
   * The HTTP <a href="http://www.w3.org/TR/resource-timing/#cross-origin-resources">
   * {@code Timing-Allow-Origin}</a> header field name.
   *
   * @since 15.0
   */
  public static final String TIMING_ALLOW_ORIGIN = "timing-allow-origin";
  /** The HTTP {@code Trailer} header field name. */
  public static final String TRAILER = "trailer";
  /** The HTTP {@code Transfer-Encoding} header field name. */
  public static final String TRANSFER_ENCODING = "transfer-encoding";
  /** The HTTP {@code Vary} header field name. */
  public static final String VARY = "vary";
  /** The HTTP {@code WWW-Authenticate} header field name. */
  public static final String WWW_AUTHENTICATE = "www-authenticate";

  // Common, non-standard HTTP header fields

  /** The HTTP {@code DNT} header field name. */
  public static final String DNT = "dnt";
  /** The HTTP {@code X-Content-Type-Options} header field name. */
  public static final String X_CONTENT_TYPE_OPTIONS = "x-content-type-options";
  /** The HTTP {@code X-Do-Not-Track} header field name. */
  public static final String X_DO_NOT_TRACK = "x-do-not-track";
  /** The HTTP {@code X-Forwarded-For} header field name. */
  public static final String X_FORWARDED_FOR = "x-forwarded-for";
  /** The HTTP {@code X-Forwarded-Proto} header field name. */
  public static final String X_FORWARDED_PROTO = "x-forwarded-proto";
  /**
   * The HTTP <a href="http://goo.gl/lQirAH">{@code X-Forwarded-Host}</a> header field name.
   *
   * @since 20.0
   */
  public static final String X_FORWARDED_HOST = "x-forwarded-host";
  /**
   * The HTTP <a href="http://goo.gl/YtV2at">{@code X-Forwarded-Port}</a> header field name.
   *
   * @since 20.0
   */
  public static final String X_FORWARDED_PORT = "x-forwarded-port";
  /** The HTTP {@code X-Frame-Options} header field name. */
  public static final String X_FRAME_OPTIONS = "x-frame-options";
  /** The HTTP {@code X-Powered-By} header field name. */
  public static final String X_POWERED_BY = "x-powered-by";
  /**
   * The HTTP
   * <a href="http://tools.ietf.org/html/draft-evans-palmer-key-pinning">{@code Public-Key-Pins}</a>
   * header field name.
   *
   * @since 15.0
   */
  @Beta public static final String PUBLIC_KEY_PINS = "public-key-pins";
  /**
   * The HTTP <a href="http://tools.ietf.org/html/draft-evans-palmer-key-pinning">
   * {@code Public-Key-Pins-Report-Only}</a> header field name.
   *
   * @since 15.0
   */
  @Beta public static final String PUBLIC_KEY_PINS_REPORT_ONLY = "public-key-pins-report-only";
  /** The HTTP {@code X-Requested-With} header field name. */
  public static final String X_REQUESTED_WITH = "x-requested-with";
  /** The HTTP {@code X-User-IP} header field name. */
  public static final String X_USER_IP = "x-user-ip";
  /** The HTTP {@code X-XSS-Protection} header field name. */
  public static final String X_XSS_PROTECTION = "x-xss-protection";
  /**
   * The HTTP <a href="http://html.spec.whatwg.org/multipage/semantics.html#hyperlink-auditing">
   * {@code Ping-From}</a> header field name.
   *
   * @since 19.0
   */
  public static final String PING_FROM = "ping-from";
  /**
   * The HTTP <a href="http://html.spec.whatwg.org/multipage/semantics.html#hyperlink-auditing">
   * {@code Ping-To}</a> header field name.
   *
   * @since 19.0
   */
  public static final String PING_TO = "ping-to";
}
