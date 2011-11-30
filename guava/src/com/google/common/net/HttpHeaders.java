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
 * @author Kurt Alfred Kluever
 * @since 11.0
 */
@Beta
@GwtCompatible
public final class HttpHeaders {
  private HttpHeaders() {}

  // HTTP Request and Response header fields

  /** The HTTP Cache-Control header field name. */
  public static final String CACHE_CONTROL = "Cache-Control";
  /** The HTTP Content-Length header field name. */
  public static final String CONTENT_LENGTH = "Content-Length";
  /** The HTTP Content-Type header field name. */
  public static final String CONTENT_TYPE = "Content-Type";
  /** The HTTP Date header field name. */
  public static final String DATE = "Date";
  /** The HTTP Pragma header field name. */
  public static final String PRAGMA = "Pragma";
  /** The HTTP Via header field name. */
  public static final String VIA = "Via";
  /** The HTTP Warning header field name. */
  public static final String WARNING = "Warning";

  // HTTP Request header fields

  /** The HTTP Accept header field name. */
  public static final String ACCEPT = "Accept";
  /** The HTTP Accept-Charset header field name. */
  public static final String ACCEPT_CHARSET = "Accept-Charset";
  /** The HTTP Accept-Encoding header field name. */
  public static final String ACCEPT_ENCODING = "Accept-Encoding";
  /** The HTTP Accept-Language header field name. */
  public static final String ACCEPT_LANGUAGE = "Accept-Language";
  /** The HTTP Access-Control-Request-Headers header field name. */
  public static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";
  /** The HTTP Access-Control-Request-Method header field name. */
  public static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
  /** The HTTP Authorization header field name. */
  public static final String AUTHORIZATION = "Authorization";
  /** The HTTP Connection header field name. */
  public static final String CONNECTION = "Connection";
  /** The HTTP Cookie header field name. */
  public static final String COOKIE = "Cookie";
  /** The HTTP Expect header field name. */
  public static final String EXPECT = "Expect";
  /** The HTTP From header field name. */
  public static final String FROM = "From";
  /** The HTTP Host header field name. */
  public static final String HOST = "Host";
  /** The HTTP If-Match header field name. */
  public static final String IF_MATCH = "If-Match";
  /** The HTTP If-Modified-Since header field name. */
  public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
  /** The HTTP If-None-Match header field name. */
  public static final String IF_NONE_MATCH = "If-None-Match";
  /** The HTTP If-Range header field name. */
  public static final String IF_RANGE = "If-Range";
  /** The HTTP If-Unmodified-Since header field name. */
  public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
  /** The HTTP Last-Event-ID header field name. */
  public static final String LAST_EVENT_ID = "Last-Event-ID";
  /** The HTTP Max-Forwards header field name. */
  public static final String MAX_FORWARDS = "Max-Forwards";
  /** The HTTP Origin header field name. */
  public static final String ORIGIN = "Origin";
  /** The HTTP Proxy-Authorization header field name. */
  public static final String PROXY_AUTHORIZATION = "Proxy-Authorization";
  /** The HTTP Range header field name. */
  public static final String RANGE = "Range";
  /** The HTTP Referer header field name. */
  public static final String REFERER = "Referer";
  /** The HTTP TE header field name. */
  public static final String TE = "TE";
  /** The HTTP Upgrade header field name. */
  public static final String UPGRADE = "Upgrade";
  /** The HTTP User-Agent header field name. */
  public static final String USER_AGENT = "User-Agent";

  // HTTP Response header fields

  /** The HTTP Accept-Ranges header field name. */
  public static final String ACCEPT_RANGES = "Accept-Ranges";
  /** The HTTP Access-Control-Allow-Headers header field name. */
  public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
  /** The HTTP Access-Control-Allow-Methods header field name. */
  public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
  /** The HTTP Access-Control-Allow-Origin header field name. */
  public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
  /** The HTTP Access-Control-Allow-Credentials header field name. */
  public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
  /** The HTTP Access-Control-Expose-Headers header field name. */
  public static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
  /** The HTTP Access-Control-Max-Age header field name. */
  public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
  /** The HTTP Age header field name. */
  public static final String AGE = "Age";
  /** The HTTP Allow header field name. */
  public static final String ALLOW = "Allow";
  /** The HTTP Content-Disposition header field name. */
  public static final String CONTENT_DISPOSITION = "Content-Disposition";
  /** The HTTP Content-Encoding header field name. */
  public static final String CONTENT_ENCODING = "Content-Encoding";
  /** The HTTP Content-Language header field name. */
  public static final String CONTENT_LANGUAGE = "Content-Language";
  /** The HTTP Content-Location header field name. */
  public static final String CONTENT_LOCATION = "Content-Location";
  /** The HTTP Content-MD5 header field name. */
  public static final String CONTENT_MD5 = "Content-MD5";
  /** The HTTP Content-Range header field name. */
  public static final String CONTENT_RANGE = "Content-Range";
  /** The HTTP ETag header field name. */
  public static final String ETAG = "ETag";
  /** The HTTP Expires header field name. */
  public static final String EXPIRES = "Expires";
  /** The HTTP Last-Modified header field name. */
  public static final String LAST_MODIFIED = "Last-Modified";
  /** The HTTP Link header field name. */
  public static final String LINK = "Link";
  /** The HTTP Location header field name. */
  public static final String LOCATION = "Location";
  /** The HTTP P3P header field name. Limited browser support. */
  public static final String P3P = "P3P";
  /** The HTTP Proxy-Authenticate header field name. */
  public static final String PROXY_AUTHENTICATE = "Proxy-Authenticate";
  /** The HTTP Refresh header field name. Non-standard header supported by most browsers. */
  public static final String REFRESH = "Refresh";
  /** The HTTP Retry-After header field name. */
  public static final String RETRY_AFTER = "Retry-After";
  /** The HTTP Server header field name. */
  public static final String SERVER = "Server";
  /** The HTTP Set-Cookie header field name. */
  public static final String SET_COOKIE = "Set-Cookie";
  /** The HTTP Set-Cookie2 header field name. */
  public static final String SET_COOKIE2 = "Set-Cookie2";
  /** The HTTP Trailer header field name. */
  public static final String TRAILER = "Trailer";
  /** The HTTP Transfer-Encoding header field name. */
  public static final String TRANSFER_ENCODING = "Transfer-Encoding";
  /** The HTTP Vary header field name. */
  public static final String VARY = "Vary";
  /** The HTTP WWW-Authenticate header field name. */
  public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

  // Common, non-standard HTTP header fields

  /** The HTTP DNT header field name. */
  public static final String DNT = "DNT";
  /** The HTTP X-Content-Type-Options header field name. */
  public static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
  /** The HTTP X-Do-Not-Track header field name. */
  public static final String X_DO_NOT_TRACK = "X-Do-Not-Track";
  /** The HTTP X-Forwarded-For header field name. */
  public static final String X_FORWARDED_FOR = "X-Forwarded-For";
  /** The HTTP X-Forwarded-Proto header field name. */
  public static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";
  /** The HTTP X-Frame-Options header field name. */
  public static final String X_FRAME_OPTIONS = "X-Frame-Options";
  /** The HTTP X-Powered-By header field name. */
  public static final String X_POWERED_BY = "X-Powered-By";
  /** The HTTP X-Requested-With header field name. */
  public static final String X_REQUESTED_WITH = "X-Requested-With";
  /** The HTTP X-User-IP header field name. */
  public static final String X_USER_IP = "X-User-IP";
  /** The HTTP X-XSS-Protection header field name. */
  public static final String X_XSS_PROTECTION = "X-XSS-Protection";

}
