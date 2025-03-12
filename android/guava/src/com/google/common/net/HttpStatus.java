/*
 * Copyright (C) 2020 The Guava Authors
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
 * Contains constant definitions for the HTTP status codes.
 * Status code references:
 *
 * <ul>
 *   <li><a href="https://tools.ietf.org/html/rfc1945">RFC 1945 (HTTP/1.0)</a>
 *   <li><a href="https://tools.ietf.org/html/rfc2295">RFC 2295</a>
 *   <li><a href="https://tools.ietf.org/html/rfc2518">RFC 2518 (WebDAV)</a>
 *   <li><a href="https://tools.ietf.org/html/rfc2616">RFC 2616 (HTTP/1.1)</a>
 *   <li><a href="https://tools.ietf.org/html/rfc2774">RFC 2774</a>
 *   <li><a href="https://tools.ietf.org/html/rfc3229">RFC 3229</a>
 *   <li><a href="https://tools.ietf.org/html/rfc4918">RFC 4918 (WebDAV)</a>
 *   <li><a href="https://tools.ietf.org/html/rfc5842">RFC 5842 (WebDAV)</a>
 *   <li><a href="https://tools.ietf.org/html/rfc6585">RFC 6585</a>
 *   <li><a href="https://tools.ietf.org/html/rfc7168">RFC 7168</a>
 *   <li><a href="https://tools.ietf.org/html/rfc7231">RFC 7231</a>
 *   <li><a href="https://tools.ietf.org/html/rfc7538">RFC 7538</a>
 *   <li><a href="https://tools.ietf.org/html/rfc7540">RFC 7540 (HTTP/2)</a>
 *   <li><a href="https://tools.ietf.org/html/rfc7725">RFC 7725</a>
 *   <li><a href="https://tools.ietf.org/html/rfc8297">RFC 8297</a>
 *   <li><a href="https://tools.ietf.org/html/rfc8470">RFC 8470</a>
 * </ul>
 *
 *
 * @author William Collishaw
 * @since NEXT
 */
@Beta
@GwtCompatible
public final class HttpStatus {
	private HttpStatus() {}

	/*
	 * 1xx: Informational
	 *  https://tools.ietf.org/html/rfc2616#section-10.1
	 *
	 *  This class of status code indicates a provisional response,
	 *  consisting only of the Status-Line and optional headers, and is
	 *  terminated by an empty line. There are no required headers for this
	 *  class of status code. Since HTTP/1.0 did not define any 1xx status
	 *  codes, servers MUST NOT send a 1xx response to an HTTP/1.0 client
	 *  except under experimental conditions.
	 *
	 *  A client MUST be prepared to accept one or more 1xx status responses
	 *  prior to a regular response, even if the client does not expect a 100
	 *  (Continue) status message. Unexpected 1xx status responses MAY be
	 *  ignored by a user agent.
	 *
	 *  Proxies MUST forward 1xx responses, unless the connection between the
	 *  proxy and its client has been closed, or unless the proxy itself
	 *  requested the generation of the 1xx response. (For example, if a
	 *  proxy adds a "Expect: 100-continue" field when it forwards a request,
	 *  then it need not forward the corresponding 100 (Continue)
	 *  response(s).)
	 */

	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.1.1">{@code Continue}</a> HTTP response status code. */
	public static final int CONTINUE = 100;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.1.2">{@code Switching Protocols}</a> HTTP response status code. */
	public static final int SWITCHING_PROTOCOLS = 101;
	/** The <a href="https://tools.ietf.org/html/rfc2518#section-10.1">{@code Processing}</a> HTTP response status code. */
	public static final int PROCESSING = 102;
	/** The <a href="https://tools.ietf.org/html/rfc8297#section-2">{@code Early Hints}</a> HTTP response status code. */
	public static final int EARLY_HINTS = 103;

	/*
	 * 2xx: Success
	 *  https://tools.ietf.org/html/rfc2616#section-10.2
	 *
	 *  This class of status code indicates that the client's request was
	 *  successfully received, understood, and accepted.
	 */

	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.2.1">{@code OK}</a> HTTP response status code. */
	public static final int OK = 200;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.2.2">{@code Created}</a> HTTP response status code. */
	public static final int CREATED = 201;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.2.3">{@code Accepted}</a> HTTP response status code. */
	public static final int ACCEPTED = 202;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.2.4">{@code Non-Authoritative Information}</a> HTTP response status code. */
	public static final int NON_AUTHORITATIVE_INFORMATION = 203;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.2.5">{@code No Content}</a> HTTP response status code. */
	public static final int NO_CONTENT = 204;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.2.6">{@code Reset Content}</a> HTTP response status code. */
	public static final int RESET_CONTENT = 205;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.2.7">{@code Partial Content}</a> HTTP response status code. */
	public static final int PARTIAL_CONTENT = 206;
	/** The <a href="https://tools.ietf.org/html/rfc4918#section-11.1">{@code Multi-Status}</a> HTTP response status code. */
	public static final int MULTI_STATUS = 207;
	/** The <a href="https://tools.ietf.org/html/rfc5842#section-7.1">{@code Already Reported}</a> HTTP response status code. */
	public static final int ALREADY_REPORTED = 208;
	/** The <a href="https://tools.ietf.org/html/rfc3229#section-10.4.1">{@code IM Used}</a> HTTP response status code. */
	public static final int IM_USED = 226;

	/*
	 * 3xx: Redirection
	 *  https://tools.ietf.org/html/rfc2616#section-10.3
	 *
	 *  This class of status code indicates that further action needs to be
	 *  taken by the user agent in order to fulfill the request.  The action
	 *  required MAY be carried out by the user agent without interaction
	 *  with the user if and only if the method used in the second request is
	 *  GET or HEAD. A client SHOULD detect infinite redirection loops, since
	 *  such loops generate network traffic for each redirection.
	 */

	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.3.1">{@code Multiple Choices}</a> HTTP response status code. */
	public static final int MULTIPLE_CHOICES = 300;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.3.2">{@code Moved Permanently}</a> HTTP response status code. */
	public static final int MOVED_PERMANENTLY = 301;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.3.3">{@code Found}</a> HTTP response status code. */
	public static final int FOUND = 302;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.3.4">{@code See Other}</a> HTTP response status code. */
	public static final int SEE_OTHER = 303;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.3.5">{@code Not Modified}</a> HTTP response status code. */
	public static final int NOT_MODIFIED = 304;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.3.6">{@code Use Proxy}</a> HTTP response status code. */
	public static final int USE_PROXY = 305;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.3.8">{@code Temporary Redirect}</a> HTTP response status code. */
	public static final int TEMPORARY_REDIRECT = 307;
	/** The <a href="https://tools.ietf.org/html/rfc7538#section-3">{@code Permanent Redirect}</a> HTTP response status code. */
	public static final int PERMANENT_REDIRECT = 308;

	/*
	 * 4xx: Client Error
	 *  https://tools.ietf.org/html/rfc2616#section-10.4
	 *
	 *  The 4xx class of status code is intended for cases in which the
	 *  client seems to have erred. Except when responding to a HEAD request,
	 *  the server SHOULD include an entity containing an explanation of the
	 *  error situation, and whether it is a temporary or permanent
	 *  condition. These status codes are applicable to any request method.
	 *  User agents SHOULD display any included entity to the user.
	 *
	 *  If the client is sending data, a server implementation using TCP
	 *  SHOULD be careful to ensure that the client acknowledges receipt of
	 *  the packet(s) containing the response, before the server closes the
	 *  input connection. If the client continues sending data to the server
	 *  after the close, the server's TCP stack will send a reset packet to
	 *  the client, which may erase the client's unacknowledged input buffers
	 *  before they can be read and interpreted by the HTTP application.
	 */

	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.4.1">{@code Bad Request}</a> HTTP response status code. */
	public static final int BAD_REQUEST = 400;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.4.2">{@code Unauthorized}</a> HTTP response status code. */
	public static final int UNAUTHORIZED = 401;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.4.3">{@code Payment Required}</a> HTTP response status code. */
	public static final int PAYMENT_REQUIRED = 402;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.4.4">{@code Forbidden}</a> HTTP response status code. */
	public static final int FORBIDDEN = 403;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.4.5">{@code Not Found}</a> HTTP response status code. */
	public static final int NOT_FOUND = 404;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.4.6">{@code Method Not Allowed}</a> HTTP response status code. */
	public static final int METHOD_NOT_ALLOWED = 405;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.4.7">{@code Not Acceptable}</a> HTTP response status code. */
	public static final int NOT_ACCEPTABLE = 406;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.4.8">{@code Proxy Authentication Required}</a> HTTP response status code. */
	public static final int PROXY_AUTHENTICATION_REQUIRED = 407;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.4.9">{@code Request Timeout}</a> HTTP response status code. */
	public static final int REQUEST_TIMEOUT = 408;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.4.10">{@code Conflict}</a> HTTP response status code. */
	public static final int CONFLICT = 409;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.4.11">{@code Gone}</a> HTTP response status code. */
	public static final int GONE = 410;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.4.12">{@code Length Required}</a> HTTP response status code. */
	public static final int LENGTH_REQUIRED = 411;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.4.13">{@code Precondition Failed}</a> HTTP response status code. */
	public static final int PRECONDITION_FAILED = 412;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.4.14">{@code Request Entity Too Large}</a> HTTP response status code. */
	public static final int REQUEST_ENTITY_TOO_LARGE = 413;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.4.15">{@code Request-URI Too Long}</a> HTTP response status code. */
	public static final int REQUEST_URI_TOO_LONG = 414;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.4.16">{@code Unsupported Media Type}</a> HTTP response status code. */
	public static final int UNSUPPORTED_MEDIA_TYPE = 415;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.4.17">{@code Requested Range Not Satisfiable}</a> HTTP response status code. */
	public static final int REQUESTED_RANGE_NOT_SATISFIABLE = 416;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.4.18">{@code Expectation Failed}</a> HTTP response status code. */
	public static final int EXPECTATION_FAILED = 417;
	/** The <a href="https://tools.ietf.org/html/rfc7168#section-2.3.3">{@code I'm a Teapot}</a> HTTP response status code. */
	public static final int IM_A_TEAPOT = 418;
	/** The <a href="https://tools.ietf.org/html/rfc7540#section-9.1.2">{@code Misdirected Request}</a> HTTP response status code. */
	public static final int MISDIRECTED_REQUEST = 421;
	/** The <a href="https://tools.ietf.org/html/rfc4918#section-11.2">{@code Unprocessable Entity}</a> HTTP response status code. */
	public static final int UNPROCESSABLE_ENTITY = 422;
	/** The <a href="https://tools.ietf.org/html/rfc4918#section-11.3">{@code Locked}</a> HTTP response status code. */
	public static final int LOCKED = 423;
	/** The <a href="https://tools.ietf.org/html/rfc4918#section-11.4">{@code Failed Dependency}</a> HTTP response status code. */
	public static final int FAILED_DEPENDENCY = 424;
	/** The <a href="https://tools.ietf.org/html/rfc8470#section-5.2">{@code Too Early}</a> HTTP response status code. */
	public static final int TOO_EARLY = 425;
	/** The <a href="https://tools.ietf.org/html/rfc7231#section-6.5.15">{@code Upgrade Required}</a> HTTP response status code. */
	public static final int UPGRADE_REQUIRED = 426;
	/** The <a href="https://tools.ietf.org/html/rfc6585#section-3">{@code Precondition Required}</a> HTTP response status code. */
	public static final int PRECONDITION_REQUIRED = 428;
	/** The <a href="https://tools.ietf.org/html/rfc6585#section-4">{@code Too Many Requests}</a> HTTP response status code. */
	public static final int TOO_MANY_REQUESTS = 429;
	/** The <a href="https://tools.ietf.org/html/rfc6585#section-5">{@code Request Header Fields Too Large}</a> HTTP response status code. */
	public static final int REQUEST_HEADER_FIELDS_TOO_LARGE = 431;
	/** The <a href="https://tools.ietf.org/html/rfc7725#section-3">{@code Unavailable For Legal Reasons}</a> HTTP response status code. */
	public static final int UNAVAILABLE_FOR_LEGAL_REASONS = 451;

	/*
	 * 5xx: Server Error
	 *  https://tools.ietf.org/html/rfc2616#section-10.5
	 *
	 *  Response status codes beginning with the digit "5" indicate cases in
	 *  which the server is aware that it has erred or is incapable of
	 *  performing the request. Except when responding to a HEAD request, the
	 *  server SHOULD include an entity containing an explanation of the
	 *  error situation, and whether it is a temporary or permanent
	 *  condition. User agents SHOULD display any included entity to the
	 *  user. These response codes are applicable to any request method.
	 */

	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.5.1">{@code Internal Server Error}</a> HTTP response status code. */
	public static final int INTERNAL_SERVER_ERROR = 500;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.5.2">{@code Not Implemented}</a> HTTP response status code. */
	public static final int NOT_IMPLEMENTED = 501;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.5.3">{@code Bad Gateway}</a> HTTP response status code. */
	public static final int BAD_GATEWAY = 502;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.5.4">{@code Service Unavailable}</a> HTTP response status code. */
	public static final int SERVICE_UNAVAILABLE = 503;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.5.5">{@code Gateway Timeout}</a> HTTP response status code. */
	public static final int GATEWAY_TIMEOUT = 504;
	/** The <a href="https://tools.ietf.org/html/rfc2616#section-10.5.6">{@code HTTP Version Not Supported}</a> HTTP response status code. */
	public static final int HTTP_VERSION_NOT_SUPPORTED = 505;
	/** The <a href="https://tools.ietf.org/html/rfc2295#section-8.1">{@code Variant Also Negotiates}</a> HTTP response status code. */
	public static final int VARIANT_ALSO_NEGOTIATES = 506;
	/** The <a href="https://tools.ietf.org/html/rfc4918#section-11.5">{@code Insufficient Storage}</a> HTTP response status code. */
	public static final int INSUFFICIENT_STORAGE = 507;
	/** The <a href="https://tools.ietf.org/html/rfc5842#section-7.2">{@code Loop Detected}</a> HTTP response status code. */
	public static final int LOOP_DETECTED = 508;
	/** The <a href="https://tools.ietf.org/html/rfc2774#section-7">{@code Not Extended}</a> HTTP response status code. */
	public static final int NOT_EXTENDED = 510;
	/** The <a href="https://tools.ietf.org/html/rfc6585#section-6">{@code Network Authentication Required}</a> HTTP response status code. */
	public static final int NETWORK_AUTHENTICATION_REQUIRED = 511;
}
