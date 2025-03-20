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

import com.google.common.annotations.GwtCompatible;

/**
 * Contains constant definitions for the HTTP header field names. See:
 *
 * <ul>
 *   <li><a href="http://www.ietf.org/rfc/rfc2109.txt">RFC 2109</a>
 *   <li><a href="http://www.ietf.org/rfc/rfc2183.txt">RFC 2183</a>
 *   <li><a href="http://www.ietf.org/rfc/rfc2616.txt">RFC 2616</a>
 *   <li><a href="http://www.ietf.org/rfc/rfc2965.txt">RFC 2965</a>
 *   <li><a href="http://www.ietf.org/rfc/rfc5988.txt">RFC 5988</a>
 * </ul>
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

  /**
   * The HTTP {@code Accept-Signature} header field name.
   *
   * @since NEXT
   */
  public static final String ACCEPT_SIGNATURE = "Accept-Signature";

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

  /**
   * The HTTP <a href="https://fetch.spec.whatwg.org/#cross-origin-resource-policy-header">{@code
   * Cross-Origin-Resource-Policy}</a> header field name.
   *
   * @since 28.0
   */
  public static final String CROSS_ORIGIN_RESOURCE_POLICY = "Cross-Origin-Resource-Policy";

  /**
   * The HTTP <a href="https://datatracker.ietf.org/doc/html/rfc8470">{@code Early-Data}</a> header
   * field name.
   *
   * @since 27.0
   */
  public static final String EARLY_DATA = "Early-Data";

  /** The HTTP {@code Expect} header field name. */
  public static final String EXPECT = "Expect";

  /** The HTTP {@code From} header field name. */
  public static final String FROM = "From";

  /**
   * The HTTP <a href="https://datatracker.ietf.org/doc/html/rfc7239">{@code Forwarded}</a> header
   * field name.
   *
   * @since 20.0
   */
  public static final String FORWARDED = "Forwarded";

  /**
   * The HTTP {@code Follow-Only-When-Prerender-Shown} header field name.
   *
   * @since 17.0
   */
  public static final String FOLLOW_ONLY_WHEN_PRERENDER_SHOWN = "Follow-Only-When-Prerender-Shown";

  /** The HTTP {@code Host} header field name. */
  public static final String HOST = "Host";

  /**
   * The HTTP <a href="https://datatracker.ietf.org/doc/html/rfc7540#section-3.2.1">{@code
   * HTTP2-Settings} </a> header field name.
   *
   * @since 24.0
   */
  public static final String HTTP2_SETTINGS = "HTTP2-Settings";

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

  /**
   * The HTTP <a href="https://github.com/WICG/origin-isolation">{@code Origin-Isolation}</a> header
   * field name.
   *
   * @since 30.1
   */
  public static final String ORIGIN_ISOLATION = "Origin-Isolation";

  /** The HTTP {@code Proxy-Authorization} header field name. */
  public static final String PROXY_AUTHORIZATION = "Proxy-Authorization";

  /** The HTTP {@code Range} header field name. */
  public static final String RANGE = "Range";

  /** The HTTP {@code Referer} header field name. */
  public static final String REFERER = "Referer";

  /**
   * The HTTP <a href="https://www.w3.org/TR/referrer-policy/">{@code Referrer-Policy}</a> header
   * field name.
   *
   * @since 23.4
   */
  public static final String REFERRER_POLICY = "Referrer-Policy";

  /**
   * Values for the <a href="https://www.w3.org/TR/referrer-policy/">{@code Referrer-Policy}</a>
   * header.
   *
   * @since 23.4
   */
  public static final class ReferrerPolicyValues {
    private ReferrerPolicyValues() {}

    public static final String NO_REFERRER = "no-referrer";
    public static final String NO_REFFERER_WHEN_DOWNGRADE = "no-referrer-when-downgrade";
    public static final String SAME_ORIGIN = "same-origin";
    public static final String ORIGIN = "origin";
    public static final String STRICT_ORIGIN = "strict-origin";
    public static final String ORIGIN_WHEN_CROSS_ORIGIN = "origin-when-cross-origin";
    public static final String STRICT_ORIGIN_WHEN_CROSS_ORIGIN = "strict-origin-when-cross-origin";
    public static final String UNSAFE_URL = "unsafe-url";
  }

  /**
   * The HTTP <a href="https://www.w3.org/TR/service-workers/#update-algorithm">{@code
   * Service-Worker}</a> header field name.
   *
   * @since 20.0
   */
  public static final String SERVICE_WORKER = "Service-Worker";

  /** The HTTP {@code TE} header field name. */
  public static final String TE = "TE";

  /** The HTTP {@code Upgrade} header field name. */
  public static final String UPGRADE = "Upgrade";

  /**
   * The HTTP <a href="https://w3c.github.io/webappsec-upgrade-insecure-requests/#preference">{@code
   * Upgrade-Insecure-Requests}</a> header field name.
   *
   * @since 28.1
   */
  public static final String UPGRADE_INSECURE_REQUESTS = "Upgrade-Insecure-Requests";

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

  /**
   * The HTTP <a href="https://wicg.github.io/private-network-access/#headers">{@code
   * Access-Control-Allow-Private-Network}</a> header field name.
   *
   * @since 31.1
   */
  public static final String ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK =
      "Access-Control-Allow-Private-Network";

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
   * The HTTP <a href="http://w3.org/TR/CSP/#content-security-policy-header-field">{@code
   * Content-Security-Policy}</a> header field name.
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

  /**
   * The HTTP nonstandard {@code X-Content-Security-Policy} header field name. It was introduced in
   * <a href="https://www.w3.org/TR/2011/WD-CSP-20111129/">CSP v.1</a> and used by the Firefox until
   * version 23 and the Internet Explorer version 10. Please, use {@link #CONTENT_SECURITY_POLICY}
   * to pass the CSP.
   *
   * @since 20.0
   */
  public static final String X_CONTENT_SECURITY_POLICY = "X-Content-Security-Policy";

  /**
   * The HTTP nonstandard {@code X-Content-Security-Policy-Report-Only} header field name. It was
   * introduced in <a href="https://www.w3.org/TR/2011/WD-CSP-20111129/">CSP v.1</a> and used by the
   * Firefox until version 23 and the Internet Explorer version 10. Please, use {@link
   * #CONTENT_SECURITY_POLICY_REPORT_ONLY} to pass the CSP.
   *
   * @since 20.0
   */
  public static final String X_CONTENT_SECURITY_POLICY_REPORT_ONLY =
      "X-Content-Security-Policy-Report-Only";

  /**
   * The HTTP nonstandard {@code X-WebKit-CSP} header field name. It was introduced in <a
   * href="https://www.w3.org/TR/2011/WD-CSP-20111129/">CSP v.1</a> and used by the Chrome until
   * version 25. Please, use {@link #CONTENT_SECURITY_POLICY} to pass the CSP.
   *
   * @since 20.0
   */
  public static final String X_WEBKIT_CSP = "X-WebKit-CSP";

  /**
   * The HTTP nonstandard {@code X-WebKit-CSP-Report-Only} header field name. It was introduced in
   * <a href="https://www.w3.org/TR/2011/WD-CSP-20111129/">CSP v.1</a> and used by the Chrome until
   * version 25. Please, use {@link #CONTENT_SECURITY_POLICY_REPORT_ONLY} to pass the CSP.
   *
   * @since 20.0
   */
  public static final String X_WEBKIT_CSP_REPORT_ONLY = "X-WebKit-CSP-Report-Only";

  /**
   * The HTTP <a href="https://wicg.github.io/cross-origin-embedder-policy/#COEP">{@code
   * Cross-Origin-Embedder-Policy}</a> header field name.
   *
   * @since 30.0
   */
  public static final String CROSS_ORIGIN_EMBEDDER_POLICY = "Cross-Origin-Embedder-Policy";

  /**
   * The HTTP <a href="https://wicg.github.io/cross-origin-embedder-policy/#COEP-RO">{@code
   * Cross-Origin-Embedder-Policy-Report-Only}</a> header field name.
   *
   * @since 30.0
   */
  public static final String CROSS_ORIGIN_EMBEDDER_POLICY_REPORT_ONLY =
      "Cross-Origin-Embedder-Policy-Report-Only";

  /**
   * The HTTP Cross-Origin-Opener-Policy header field name.
   *
   * @since 28.2
   */
  public static final String CROSS_ORIGIN_OPENER_POLICY = "Cross-Origin-Opener-Policy";

  /** The HTTP {@code ETag} header field name. */
  public static final String ETAG = "ETag";

  /** The HTTP {@code Expires} header field name. */
  public static final String EXPIRES = "Expires";

  /**
   * The HTTP {@code Identity-Digest} header field name.
   *
   * @since NEXT
   */
  public static final String IDENTITY_DIGEST = "Identity-Digest";

  /** The HTTP {@code Last-Modified} header field name. */
  public static final String LAST_MODIFIED = "Last-Modified";

  /** The HTTP {@code Link} header field name. */
  public static final String LINK = "Link";

  /** The HTTP {@code Location} header field name. */
  public static final String LOCATION = "Location";

  /**
   * The HTTP {@code Keep-Alive} header field name.
   *
   * @since 31.0
   */
  public static final String KEEP_ALIVE = "Keep-Alive";

  /**
   * The HTTP <a href="https://github.com/WICG/nav-speculation/blob/main/no-vary-search.md">{@code
   * No-Vary-Seearch}</a> header field name.
   *
   * @since 32.0.0
   */
  public static final String NO_VARY_SEARCH = "No-Vary-Search";

  /**
   * The HTTP <a href="https://googlechrome.github.io/OriginTrials/#header">{@code Origin-Trial}</a>
   * header field name.
   *
   * @since 27.1
   */
  public static final String ORIGIN_TRIAL = "Origin-Trial";

  /** The HTTP {@code P3P} header field name. Limited browser support. */
  public static final String P3P = "P3P";

  /** The HTTP {@code Proxy-Authenticate} header field name. */
  public static final String PROXY_AUTHENTICATE = "Proxy-Authenticate";

  /** The HTTP {@code Refresh} header field name. Non-standard header supported by most browsers. */
  public static final String REFRESH = "Refresh";

  /**
   * The HTTP <a href="https://www.w3.org/TR/reporting/">{@code Report-To}</a> header field name.
   *
   * @since 27.1
   */
  public static final String REPORT_TO = "Report-To";

  /** The HTTP {@code Retry-After} header field name. */
  public static final String RETRY_AFTER = "Retry-After";

  /** The HTTP {@code Server} header field name. */
  public static final String SERVER = "Server";

  /**
   * The HTTP <a href="https://www.w3.org/TR/server-timing/">{@code Server-Timing}</a> header field
   * name.
   *
   * @since 23.6
   */
  public static final String SERVER_TIMING = "Server-Timing";

  /**
   * The HTTP <a href="https://www.w3.org/TR/service-workers/#update-algorithm">{@code
   * Service-Worker-Allowed}</a> header field name.
   *
   * @since 20.0
   */
  public static final String SERVICE_WORKER_ALLOWED = "Service-Worker-Allowed";

  /** The HTTP {@code Set-Cookie} header field name. */
  public static final String SET_COOKIE = "Set-Cookie";

  /** The HTTP {@code Set-Cookie2} header field name. */
  public static final String SET_COOKIE2 = "Set-Cookie2";

  /**
   * The HTTP {@code Signature} header field name.
   *
   * @since NEXT
   */
  public static final String SIGNATURE = "Signature";

  /**
   * The HTTP {@code Signature-Input} header field name.
   *
   * @since NEXT
   */
  public static final String SIGNATURE_INPUT = "Signature-Input";

  /**
   * The HTTP <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/SourceMap">{@code
   * SourceMap}</a> header field name.
   *
   * @since 27.1
   */
  public static final String SOURCE_MAP = "SourceMap";

  /**
   * The HTTP <a href="https://github.com/WICG/nav-speculation/blob/main/opt-in.md">{@code
   * Supports-Loading-Mode}</a> header field name. This can be used to specify, for example, <a
   * href="https://developer.chrome.com/docs/privacy-sandbox/fenced-frame/#server-opt-in">fenced
   * frames</a>.
   *
   * @since 32.0.0
   */
  public static final String SUPPORTS_LOADING_MODE = "Supports-Loading-Mode";

  /**
   * The HTTP <a href="http://tools.ietf.org/html/rfc6797#section-6.1">{@code
   * Strict-Transport-Security}</a> header field name.
   *
   * @since 15.0
   */
  public static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";

  /**
   * The HTTP <a href="http://www.w3.org/TR/resource-timing/#cross-origin-resources">{@code
   * Timing-Allow-Origin}</a> header field name.
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

  /**
   * The HTTP <a
   * href="https://iabtechlab.com/wp-content/uploads/2019/06/VAST_4.2_final_june26.pdf">{@code
   * X-Device-IP}</a> header field name. Header used for VAST requests to provide the IP address of
   * the device on whose behalf the request is being made.
   *
   * @since 31.0
   */
  public static final String X_DEVICE_IP = "X-Device-IP";

  /**
   * The HTTP <a
   * href="https://iabtechlab.com/wp-content/uploads/2019/06/VAST_4.2_final_june26.pdf">{@code
   * X-Device-Referer}</a> header field name. Header used for VAST requests to provide the {@link
   * #REFERER} header value that the on-behalf-of client would have used when making a request
   * itself.
   *
   * @since 31.0
   */
  public static final String X_DEVICE_REFERER = "X-Device-Referer";

  /**
   * The HTTP <a
   * href="https://iabtechlab.com/wp-content/uploads/2019/06/VAST_4.2_final_june26.pdf">{@code
   * X-Device-Accept-Language}</a> header field name. Header used for VAST requests to provide the
   * {@link #ACCEPT_LANGUAGE} header value that the on-behalf-of client would have used when making
   * a request itself.
   *
   * @since 31.0
   */
  public static final String X_DEVICE_ACCEPT_LANGUAGE = "X-Device-Accept-Language";

  /**
   * The HTTP <a
   * href="https://iabtechlab.com/wp-content/uploads/2019/06/VAST_4.2_final_june26.pdf">{@code
   * X-Device-Requested-With}</a> header field name. Header used for VAST requests to provide the
   * {@link #X_REQUESTED_WITH} header value that the on-behalf-of client would have used when making
   * a request itself.
   *
   * @since 31.0
   */
  public static final String X_DEVICE_REQUESTED_WITH = "X-Device-Requested-With";

  /** The HTTP {@code X-Do-Not-Track} header field name. */
  public static final String X_DO_NOT_TRACK = "X-Do-Not-Track";

  /** The HTTP {@code X-Forwarded-For} header field name (superseded by {@code Forwarded}). */
  public static final String X_FORWARDED_FOR = "X-Forwarded-For";

  /** The HTTP {@code X-Forwarded-Proto} header field name. */
  public static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";

  /**
   * The HTTP <a
   * href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-Host">{@code
   * X-Forwarded-Host}</a> header field name.
   *
   * @since 20.0
   */
  public static final String X_FORWARDED_HOST = "X-Forwarded-Host";

  /**
   * The HTTP <a
   * href="https://docs.aws.amazon.com/elasticloadbalancing/latest/classic/x-forwarded-headers.html#x-forwarded-port">{@code
   * X-Forwarded-Port}</a> header field name.
   *
   * @since 20.0
   */
  public static final String X_FORWARDED_PORT = "X-Forwarded-Port";

  /** The HTTP {@code X-Frame-Options} header field name. */
  public static final String X_FRAME_OPTIONS = "X-Frame-Options";

  /** The HTTP {@code X-Powered-By} header field name. */
  public static final String X_POWERED_BY = "X-Powered-By";

  /**
   * The HTTP <a href="http://tools.ietf.org/html/draft-evans-palmer-key-pinning">{@code
   * Public-Key-Pins}</a> header field name.
   *
   * @since 15.0
   */
  public static final String PUBLIC_KEY_PINS = "Public-Key-Pins";

  /**
   * The HTTP <a href="http://tools.ietf.org/html/draft-evans-palmer-key-pinning">{@code
   * Public-Key-Pins-Report-Only}</a> header field name.
   *
   * @since 15.0
   */
  public static final String PUBLIC_KEY_PINS_REPORT_ONLY = "Public-Key-Pins-Report-Only";

  /**
   * The HTTP {@code X-Request-ID} header field name.
   *
   * @since 30.1
   */
  public static final String X_REQUEST_ID = "X-Request-ID";

  /** The HTTP {@code X-Requested-With} header field name. */
  public static final String X_REQUESTED_WITH = "X-Requested-With";

  /** The HTTP {@code X-User-IP} header field name. */
  public static final String X_USER_IP = "X-User-IP";

  /**
   * The HTTP <a
   * href="https://learn.microsoft.com/en-us/archive/blogs/ieinternals/internet-explorer-and-custom-http-headers#:~:text=X%2DDownload%2DOptions">{@code
   * X-Download-Options}</a> header field name.
   *
   * <p>When the new X-Download-Options header is present with the value {@code noopen}, the user is
   * prevented from opening a file download directly; instead, they must first save the file
   * locally.
   *
   * @since 24.1
   */
  public static final String X_DOWNLOAD_OPTIONS = "X-Download-Options";

  /** The HTTP {@code X-XSS-Protection} header field name. */
  public static final String X_XSS_PROTECTION = "X-XSS-Protection";

  /**
   * The HTTP <a
   * href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-DNS-Prefetch-Control">{@code
   * X-DNS-Prefetch-Control}</a> header controls DNS prefetch behavior. Value can be "on" or "off".
   * By default, DNS prefetching is "on" for HTTP pages and "off" for HTTPS pages.
   */
  public static final String X_DNS_PREFETCH_CONTROL = "X-DNS-Prefetch-Control";

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

  /**
   * The HTTP <a
   * href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Link_prefetching_FAQ#As_a_server_admin.2C_can_I_distinguish_prefetch_requests_from_normal_requests.3F">{@code
   * Purpose}</a> header field name.
   *
   * @since 28.0
   */
  public static final String PURPOSE = "Purpose";

  /**
   * The HTTP <a
   * href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Link_prefetching_FAQ#As_a_server_admin.2C_can_I_distinguish_prefetch_requests_from_normal_requests.3F">{@code
   * X-Purpose}</a> header field name.
   *
   * @since 28.0
   */
  public static final String X_PURPOSE = "X-Purpose";

  /**
   * The HTTP <a
   * href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Link_prefetching_FAQ#As_a_server_admin.2C_can_I_distinguish_prefetch_requests_from_normal_requests.3F">{@code
   * X-Moz}</a> header field name.
   *
   * @since 28.0
   */
  public static final String X_MOZ = "X-Moz";

  /**
   * The HTTP <a
   * href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Device-Memory">{@code
   * Device-Memory}</a> header field name.
   *
   * @since 31.0
   */
  public static final String DEVICE_MEMORY = "Device-Memory";

  /**
   * The HTTP <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Downlink">{@code
   * Downlink}</a> header field name.
   *
   * @since 31.0
   */
  public static final String DOWNLINK = "Downlink";

  /**
   * The HTTP <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/ECT">{@code
   * ECT}</a> header field name.
   *
   * @since 31.0
   */
  public static final String ECT = "ECT";

  /**
   * The HTTP <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/RTT">{@code
   * RTT}</a> header field name.
   *
   * @since 31.0
   */
  public static final String RTT = "RTT";

  /**
   * The HTTP <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Save-Data">{@code
   * Save-Data}</a> header field name.
   *
   * @since 31.0
   */
  public static final String SAVE_DATA = "Save-Data";

  /**
   * The HTTP <a
   * href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Viewport-Width">{@code
   * Viewport-Width}</a> header field name.
   *
   * @since 31.0
   */
  public static final String VIEWPORT_WIDTH = "Viewport-Width";

  /**
   * The HTTP <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Width">{@code
   * Width}</a> header field name.
   *
   * @since 31.0
   */
  public static final String WIDTH = "Width";

  /**
   * The HTTP <a href="https://www.w3.org/TR/permissions-policy-1/">{@code Permissions-Policy}</a>
   * header field name.
   *
   * @since 31.0
   */
  public static final String PERMISSIONS_POLICY = "Permissions-Policy";

  /**
   * The HTTP <a
   * href="https://w3c.github.io/webappsec-permissions-policy/#permissions-policy-report-only-http-header-field">{@code
   * Permissions-Policy-Report-Only}</a> header field name.
   *
   * @since 33.2.0
   */
  public static final String PERMISSIONS_POLICY_REPORT_ONLY = "Permissions-Policy-Report-Only";

  /**
   * The HTTP <a
   * href="https://wicg.github.io/user-preference-media-features-headers/#sec-ch-prefers-color-scheme">{@code
   * Sec-CH-Prefers-Color-Scheme}</a> header field name.
   *
   * <p>This header is experimental.
   *
   * @since 31.0
   */
  public static final String SEC_CH_PREFERS_COLOR_SCHEME = "Sec-CH-Prefers-Color-Scheme";

  /**
   * The HTTP <a
   * href="https://www.rfc-editor.org/rfc/rfc8942#name-the-accept-ch-response-head">{@code
   * Accept-CH}</a> header field name.
   *
   * @since 31.0
   */
  public static final String ACCEPT_CH = "Accept-CH";

  /**
   * The HTTP <a
   * href="https://datatracker.ietf.org/doc/html/draft-davidben-http-client-hint-reliability-03.txt#section-3">{@code
   * Critical-CH}</a> header field name.
   *
   * @since 31.0
   */
  public static final String CRITICAL_CH = "Critical-CH";

  /**
   * The HTTP <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua">{@code Sec-CH-UA}</a>
   * header field name.
   *
   * @since 30.0
   */
  public static final String SEC_CH_UA = "Sec-CH-UA";

  /**
   * The HTTP <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua-arch">{@code
   * Sec-CH-UA-Arch}</a> header field name.
   *
   * @since 30.0
   */
  public static final String SEC_CH_UA_ARCH = "Sec-CH-UA-Arch";

  /**
   * The HTTP <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua-model">{@code
   * Sec-CH-UA-Model}</a> header field name.
   *
   * @since 30.0
   */
  public static final String SEC_CH_UA_MODEL = "Sec-CH-UA-Model";

  /**
   * The HTTP <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua-platform">{@code
   * Sec-CH-UA-Platform}</a> header field name.
   *
   * @since 30.0
   */
  public static final String SEC_CH_UA_PLATFORM = "Sec-CH-UA-Platform";

  /**
   * The HTTP <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua-platform-version">{@code
   * Sec-CH-UA-Platform-Version}</a> header field name.
   *
   * @since 30.0
   */
  public static final String SEC_CH_UA_PLATFORM_VERSION = "Sec-CH-UA-Platform-Version";

  /**
   * The HTTP <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua-full-version">{@code
   * Sec-CH-UA-Full-Version}</a> header field name.
   *
   * @deprecated Prefer {@link SEC_CH_UA_FULL_VERSION_LIST}.
   * @since 30.0
   */
  @Deprecated public static final String SEC_CH_UA_FULL_VERSION = "Sec-CH-UA-Full-Version";

  /**
   * The HTTP <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua-full-version-list">{@code
   * Sec-CH-UA-Full-Version}</a> header field name.
   *
   * @since 31.1
   */
  public static final String SEC_CH_UA_FULL_VERSION_LIST = "Sec-CH-UA-Full-Version-List";

  /**
   * The HTTP <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua-mobile">{@code
   * Sec-CH-UA-Mobile}</a> header field name.
   *
   * @since 30.0
   */
  public static final String SEC_CH_UA_MOBILE = "Sec-CH-UA-Mobile";

  /**
   * The HTTP <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua-wow64">{@code
   * Sec-CH-UA-WoW64}</a> header field name.
   *
   * @since 32.0.0
   */
  public static final String SEC_CH_UA_WOW64 = "Sec-CH-UA-WoW64";

  /**
   * The HTTP <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua-bitness">{@code
   * Sec-CH-UA-Bitness}</a> header field name.
   *
   * @since 31.0
   */
  public static final String SEC_CH_UA_BITNESS = "Sec-CH-UA-Bitness";

  /**
   * The HTTP <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua-form-factor">{@code
   * Sec-CH-UA-Form-Factor}</a> header field name.
   *
   * @deprecated Prefer {@link SEC_CH_UA_FORM_FACTORS}.
   * @since 32.0.0
   */
  @Deprecated public static final String SEC_CH_UA_FORM_FACTOR = "Sec-CH-UA-Form-Factor";

  /**
   * The HTTP <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua-form-factors">{@code
   * Sec-CH-UA-Form-Factors}</a> header field name.
   *
   * @since 33.3.0
   */
  public static final String SEC_CH_UA_FORM_FACTORS = "Sec-CH-UA-Form-Factors";

  /**
   * The HTTP <a
   * href="https://wicg.github.io/responsive-image-client-hints/#sec-ch-viewport-width">{@code
   * Sec-CH-Viewport-Width}</a> header field name.
   *
   * @since 32.0.0
   */
  public static final String SEC_CH_VIEWPORT_WIDTH = "Sec-CH-Viewport-Width";

  /**
   * The HTTP <a
   * href="https://wicg.github.io/responsive-image-client-hints/#sec-ch-viewport-height">{@code
   * Sec-CH-Viewport-Height}</a> header field name.
   *
   * @since 32.0.0
   */
  public static final String SEC_CH_VIEWPORT_HEIGHT = "Sec-CH-Viewport-Height";

  /**
   * The HTTP <a href="https://wicg.github.io/responsive-image-client-hints/#sec-ch-dpr">{@code
   * Sec-CH-DPR}</a> header field name.
   *
   * @since 32.0.0
   */
  public static final String SEC_CH_DPR = "Sec-CH-DPR";

  /**
   * The HTTP <a href="https://w3c.github.io/webappsec-fetch-metadata/">{@code Sec-Fetch-Dest}</a>
   * header field name.
   *
   * @since 27.1
   */
  public static final String SEC_FETCH_DEST = "Sec-Fetch-Dest";

  /**
   * The HTTP <a href="https://w3c.github.io/webappsec-fetch-metadata/">{@code Sec-Fetch-Mode}</a>
   * header field name.
   *
   * @since 27.1
   */
  public static final String SEC_FETCH_MODE = "Sec-Fetch-Mode";

  /**
   * The HTTP <a href="https://w3c.github.io/webappsec-fetch-metadata/">{@code Sec-Fetch-Site}</a>
   * header field name.
   *
   * @since 27.1
   */
  public static final String SEC_FETCH_SITE = "Sec-Fetch-Site";

  /**
   * The HTTP <a href="https://w3c.github.io/webappsec-fetch-metadata/">{@code Sec-Fetch-User}</a>
   * header field name.
   *
   * @since 27.1
   */
  public static final String SEC_FETCH_USER = "Sec-Fetch-User";

  /**
   * The HTTP <a href="https://w3c.github.io/webappsec-fetch-metadata/">{@code Sec-Metadata}</a>
   * header field name.
   *
   * @since 26.0
   */
  public static final String SEC_METADATA = "Sec-Metadata";

  /**
   * The HTTP <a href="https://datatracker.ietf.org/doc/html/draft-ietf-tokbind-https">{@code
   * Sec-Token-Binding}</a> header field name.
   *
   * @since 25.1
   */
  public static final String SEC_TOKEN_BINDING = "Sec-Token-Binding";

  /**
   * The HTTP <a href="https://datatracker.ietf.org/doc/html/draft-ietf-tokbind-ttrp">{@code
   * Sec-Provided-Token-Binding-ID}</a> header field name.
   *
   * @since 25.1
   */
  public static final String SEC_PROVIDED_TOKEN_BINDING_ID = "Sec-Provided-Token-Binding-ID";

  /**
   * The HTTP <a href="https://datatracker.ietf.org/doc/html/draft-ietf-tokbind-ttrp">{@code
   * Sec-Referred-Token-Binding-ID}</a> header field name.
   *
   * @since 25.1
   */
  public static final String SEC_REFERRED_TOKEN_BINDING_ID = "Sec-Referred-Token-Binding-ID";

  /**
   * The HTTP <a href="https://datatracker.ietf.org/doc/html/rfc6455">{@code
   * Sec-WebSocket-Accept}</a> header field name.
   *
   * @since 28.0
   */
  public static final String SEC_WEBSOCKET_ACCEPT = "Sec-WebSocket-Accept";

  /**
   * The HTTP <a href="https://datatracker.ietf.org/doc/html/rfc6455">{@code
   * Sec-WebSocket-Extensions}</a> header field name.
   *
   * @since 28.0
   */
  public static final String SEC_WEBSOCKET_EXTENSIONS = "Sec-WebSocket-Extensions";

  /**
   * The HTTP <a href="https://datatracker.ietf.org/doc/html/rfc6455">{@code Sec-WebSocket-Key}</a>
   * header field name.
   *
   * @since 28.0
   */
  public static final String SEC_WEBSOCKET_KEY = "Sec-WebSocket-Key";

  /**
   * The HTTP <a href="https://datatracker.ietf.org/doc/html/rfc6455">{@code
   * Sec-WebSocket-Protocol}</a> header field name.
   *
   * @since 28.0
   */
  public static final String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";

  /**
   * The HTTP <a href="https://datatracker.ietf.org/doc/html/rfc6455">{@code
   * Sec-WebSocket-Version}</a> header field name.
   *
   * @since 28.0
   */
  public static final String SEC_WEBSOCKET_VERSION = "Sec-WebSocket-Version";

  /**
   * The HTTP <a href="https://patcg-individual-drafts.github.io/topics/">{@code
   * Sec-Browsing-Topics}</a> header field name.
   *
   * @since 32.0.0
   */
  public static final String SEC_BROWSING_TOPICS = "Sec-Browsing-Topics";

  /**
   * The HTTP <a href="https://patcg-individual-drafts.github.io/topics/">{@code
   * Observe-Browsing-Topics}</a> header field name.
   *
   * @since 32.0.0
   */
  public static final String OBSERVE_BROWSING_TOPICS = "Observe-Browsing-Topics";

  /**
   * The HTTP <a
   * href="https://wicg.github.io/turtledove/#handling-direct-from-seller-signals">{@code
   * Sec-Ad-Auction-Fetch}</a> header field name.
   *
   * @since 33.0.0
   */
  public static final String SEC_AD_AUCTION_FETCH = "Sec-Ad-Auction-Fetch";

  /**
   * The HTTP <a
   * href="https://privacycg.github.io/gpc-spec/#the-sec-gpc-header-field-for-http-requests">{@code
   * Sec-GPC}</a> header field name.
   *
   * @since 33.2.0
   */
  public static final String SEC_GPC = "Sec-GPC";

  /**
   * The HTTP <a
   * href="https://wicg.github.io/turtledove/#handling-direct-from-seller-signals">{@code
   * Ad-Auction-Signals}</a> header field name.
   *
   * @since 33.0.0
   */
  public static final String AD_AUCTION_SIGNALS = "Ad-Auction-Signals";

  /**
   * The HTTP <a href="https://wicg.github.io/turtledove/#http-headerdef-ad-auction-allowed">{@code
   * Ad-Auction-Allowed}</a> header field name.
   *
   * @since 33.2.0
   */
  public static final String AD_AUCTION_ALLOWED = "Ad-Auction-Allowed";

  /**
   * The HTTP <a href="https://datatracker.ietf.org/doc/html/rfc8586">{@code CDN-Loop}</a> header
   * field name.
   *
   * @since 28.0
   */
  public static final String CDN_LOOP = "CDN-Loop";

  /**
   * The HTTP <a href="https://datatracker.ietf.org/doc/html/rfc7838#page-8">{@code Alt-Svc}</a>
   * header field name.
   *
   * @since 33.4.0
   */
  public static final String ALT_SVC = "Alt-Svc";
}
