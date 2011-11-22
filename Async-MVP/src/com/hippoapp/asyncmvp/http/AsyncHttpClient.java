/*
 * Copyright (C) 2010-2011 Bnet.inc (http://bnet.su)
 *
 * This file is part of AsyncMvp.
 *
 * AsyncMvp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AsyncMvp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AsyncMvp.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.hippoapp.asyncmvp.http;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.SyncBasicHttpContext;

import android.content.Context;

import com.hippoapp.asyncmvp.cache.AsyncCacheClient;

/**
 * Custom realization of HTTP client. Client is adopted for concurrent requests
 * from View component. AsyncHttpClient is used to make GET and POST HTTP
 * requests.
 *
 * Each {@link AsyncCachedHttpRequest} could contain
 * {@link AsyncHttpRequestParams} for additional parameters and should pass
 * result to {@link IAsyncHttpResponseHandler}. Each calling get or post methods
 * should pass protocol for correct identification in
 * {@link IAsyncHttpResponseHandler} .
 *
 * Optional {@link AsyncHttpRequest}s could be cached with one or two level
 * cache. Cache is enabled by calling method
 * {@link AsyncHttpClient#enableResponseCache(int, int, int)},
 * {@link AsyncHttpClient#enableResponseCache(int, int, int, int)},
 * {@link AsyncHttpClient#enableResponseCache(int, int, int, int, int)},
 * {@link AsyncHttpClient#enableResponseCache(int, int, int, int, int, int)}.
 * Responses can cache all together or each response(with specified parameters)
 * can cache separately according to AsyncMvp protocol.
 *
 * This realization of asynchronous HTTP client also has an opportunity to make
 * synchronous requests by setting fixed thread executor with one thread as
 * <code>ThreadPoolExecutor</code>.
 *
 * <p>
 * Simple use of this package:
 * <p>
 *
 * <pre>
 * AsyncHttpClient client = new AsyncHttpClient();
 * client.get(&quot;http://www.google.com&quot;, new IAsyncHttpResponseHandler() {
 * 	&#064;Override
 * 	public void onSuccess(int protocol, byte[] response) {
 * 		// some actions
 * 	}
 * });
 * </pre>
 *
 * <p>
 * This is how to use AsycnHttpClient in RemotePresenter class. RemoteModel
 * class is subclass of RemoteAsyncHttpResponseHandler and consider to parse XML
 * or JSON response. In RemoteModel response come to onSuccess according to
 * Presenter protocol.
 *
 * <pre>
 * class RemotePresenter implements Handler.Callback {
 * 	static AsyncHttpClient asyncHttpClient;
 * 	static {
 * 		asycnHttpClient = new AsyncHttpClient();
 * 	}
 *
 * 	public RemotePresenter() {
 * 		remoteModel = new RemoteAsyncHttpResponseHandler();
 * 	}
 *
 * 	&#064;Override
 * 	public boolean handleMessage(Message msg) {
 * 		switch (msg.what) {
 * 		case V_GET: {
 * 			AsyncHttpRequestParams requestParams = new AsyncHttpRequestParams(GET_URL_ADDRESS);
 * 			requestParams.put(&quot;GET_PARAMS_1&quot;, &quot;GET_VALUE_1&quot;);
 * 			requestParams.put(&quot;GET_PARAMS_2&quot;, &quot;GET_VALUE_2&quot;);
 *
 * 			asyncHttpClient.get(requestParams, remoteModel, msg.what);
 * 			return true;
 * 		}
 * 		case V_POST: {
 * 			RequestParams requestParams = new RequestParams(POST_URL_ADDRESS);
 * 			requestParams.put(&quot;POST_PARAMS_1&quot;, &quot;POST_VALUE_1&quot;);
 * 			requestParams.put(&quot;POST_PARAMS_2&quot;, &quot;POST_VALUE_2&quot;);
 *
 * 			asyncHttpClient.post(requestParams, remoteModel, msg.what);
 * 			return true;
 * 		}
 * 		default:
 * 			break;
 * 		}
 * 		return false;
 * 	}
 * }
 *
 * class RemoteHandler extends RemoteAsyncHttpResponseHandler {
 * 	&#064;Override
 * 	public void onSuccess(int protocol, String content) {
 * 		switch (protocol) {
 * 		case V_GET_STORES:
 * 			// parse content
 * 			break;
 * 		case V_POST:
 * 			// parse content
 * 			break;
 * 		}
 * 	}
 * }
 * </pre>
 */
public class AsyncHttpClient {
	private static final String VERSION = "1.0.3";

	private static final int DEFAULT_MAX_CONNECTIONS = 10;
	private static final int DEFAULT_SOCKET_TIMEOUT = 10 * 1000;
	private static final int DEFAULT_MAX_RETRIES = 5;

	public static final int HTTP_RESPONSE_CACHE_PROTOCOL = 1;

	private static int maxConnections = DEFAULT_MAX_CONNECTIONS;
	private static int socketTimeout = DEFAULT_SOCKET_TIMEOUT;

	private DefaultHttpClient httpClient;
	private HttpContext httpContext;
	private ThreadPoolExecutor threadPool;
	private Map<Context, List<WeakReference<Future>>> requestMap;

	/**
	 * Creates a new AsyncHttpClient and configure it with default parameters.
	 */
	public AsyncHttpClient() {
		BasicHttpParams httpParams = new BasicHttpParams();

		ConnManagerParams.setTimeout(httpParams, socketTimeout);
		ConnManagerParams.setMaxConnectionsPerRoute(httpParams, new ConnPerRouteBean(maxConnections));
		ConnManagerParams.setMaxTotalConnections(httpParams, DEFAULT_MAX_CONNECTIONS);

		HttpConnectionParams.setSoTimeout(httpParams, socketTimeout);
		HttpConnectionParams.setTcpNoDelay(httpParams, true);

		HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setUserAgent(httpParams, String.format("su.bnet.applications", VERSION));

		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
		ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(httpParams, schemeRegistry);

		httpContext = new SyncBasicHttpContext(new BasicHttpContext());
		httpClient = new DefaultHttpClient(cm, httpParams);

		httpClient.setHttpRequestRetryHandler(new RetryHandler());

		threadPool = (ThreadPoolExecutor) Executors.newCachedThreadPool();

		requestMap = new WeakHashMap<Context, List<WeakReference<Future>>>();
	}

	/**
	 * Enables one level cache of HTTP responses. All responses cache in default
	 * cache storage. This will only enable the in-memory cache. To enable the
	 * two level cache(in-memory and disk), use
	 * {@link #enableResponseCache(int, int, int, int)}.
	 *
	 * @param inMemoryCacheInitialCapacity
	 *            - the initial element size of the cache
	 * @param inMemoryCacheExpirationInMinutes
	 *            - time in minutes after which elements will be purged from the
	 *            cache
	 * @param inMemoryCacheMaxConcurrentThreads
	 *            - how many threads you think may at once access the cache;
	 *            this need not be an exact number, but it helps in fragmenting
	 *            the cache properly
	 */
	public static void enableResponseCache(int inMemoryCacheInitialCapacity, int inMemoryCacheExpirationInMinutes,
			int inMemoryCacheMaxConcurrentThreads) {
		AsyncCacheClient.getInstance().initCacheInstance(HTTP_RESPONSE_CACHE_PROTOCOL, inMemoryCacheInitialCapacity,
				inMemoryCacheExpirationInMinutes, inMemoryCacheMaxConcurrentThreads);
	}

	/**
	 * Enables two level cache of HTTP responses. All responses cache in default
	 * cache storage.
	 *
	 * @param inMemoryCacheInitialCapacity
	 *            - the initial element size of the cache
	 * @param inMemoryCacheExpirationInMinutes
	 *            - time in minutes after which elements will be purged from the
	 *            in-memory cache
	 * @param inMemoryCacheMaxConcurrentThreads
	 *            - how many threads you think may at once access the cache;
	 *            this need not be an exact number, but it helps in fragmenting
	 *            the cache properly
	 * @param diskCacheExpirationInMinutes
	 *            - time in minutes after which elements will be purged from the
	 *            disk cache
	 * @param diskCacheType
	 *            - type of cache: {@link AsyncCacheClient#INTERNAL_CACHE},
	 *            {@link AsyncCacheClient#EXTERNAL_CACHE}
	 * @throws IOException
	 *             if sd card not mounted
	 */
	public static void enableResponseCache(int inMemoryCacheInitialCapacity, int inMemoryCacheExpirationInMinutes,
			int inMemoryCacheMaxConcurrentThreads, int diskCacheExpirationInMinutes, int diskCacheType) throws IOException {
		AsyncCacheClient.getInstance().initCacheInstance(HTTP_RESPONSE_CACHE_PROTOCOL, inMemoryCacheInitialCapacity,
				inMemoryCacheExpirationInMinutes, inMemoryCacheMaxConcurrentThreads, diskCacheExpirationInMinutes, diskCacheType);
	}

	/**
	 * Enables one level cache of HTTP responses. All responses cache in custom
	 * cache storage according protocol. This will only enable the in-memory
	 * cache. To enable the two level cache(in-memory and disk), use
	 * {@link #enableResponseCache(int, int, int, int, int, int)}.
	 *
	 * @param protocol
	 *            - to identify cache storage
	 * @param inMemoryCacheInitialCapacity
	 *            - the initial element size of the cache
	 * @param inMemoryCacheExpirationInMinutes
	 *            - time in minutes after which elements will be purged from the
	 *            cache
	 * @param inMemoryCacheMaxConcurrentThreads
	 *            - how many threads you think may at once access the cache;
	 *            this need not be an exact number, but it helps in fragmenting
	 *            the cache properly
	 */
	public static void enableResponseCache(int protocol, int inMemoryCacheInitialCapacity, int inMemoryCacheExpirationInMinutes,
			int inMemoryCacheMaxConcurrentThreads) {
		AsyncCacheClient.getInstance().initCacheInstance(protocol, inMemoryCacheInitialCapacity,
				inMemoryCacheExpirationInMinutes, inMemoryCacheMaxConcurrentThreads);
	}

	/**
	 * Enables two level cache of HTTP responses. All responses cache in custom
	 * cache storage according protocol.
	 *
	 * @param protocol
	 *            - to identify cache storage
	 * @param inMemoryCacheInitialCapacity
	 *            - the initial element size of the cache
	 * @param inMemoryCacheExpirationInMinutes
	 *            - time in minutes after which elements will be purged from the
	 *            in-memory cache
	 * @param inMemoryCacheMaxConcurrentThreads
	 *            - how many threads you think may at once access the cache;
	 *            this need not be an exact number, but it helps in fragmenting
	 *            the cache properly
	 * @param diskCacheExpirationInMinutes
	 *            - time in minutes after which elements will be purged from the
	 *            disk cache
	 * @param diskCacheType
	 *            - type of cache: {@link AsyncCacheClient#INTERNAL_CACHE},
	 *            {@link AsyncCacheClient#EXTERNAL_CACHE}
	 * @throws IOException
	 *             if sd card not mounted
	 */
	public static void enableResponseCache(int protocol, int inMemoryCacheInitialCapacity, int inMemoryCacheExpirationInMinutes,
			int inMemoryCacheMaxConcurrentThreads, int diskCacheExpirationInMinutes, int diskCacheType) throws IOException {
		AsyncCacheClient.getInstance().initCacheInstance(protocol, inMemoryCacheInitialCapacity,
				inMemoryCacheExpirationInMinutes, inMemoryCacheMaxConcurrentThreads, diskCacheExpirationInMinutes, diskCacheType);
	}

	public static void clearCache(int protocol) {
		AsyncCacheClient.getInstance().removeAll(protocol);
	}

	public static void clearCache() {
		AsyncCacheClient.getInstance().removeAll(HTTP_RESPONSE_CACHE_PROTOCOL);
	}

	/**
	 * Get the underlying HttpClient instance. This is useful for setting
	 * additional settings by accessing the client's ConnectionManager,
	 * HttpParams and SchemeRegistry.
	 */
	public HttpClient getHttpClient() {
		return this.httpClient;
	}

	/**
	 * Overrides the threadpool implementation used when queuing/pooling
	 * requests. By default, <code>Executors.newCachedThreadPool()</code> is
	 * used. If need to make synchronous requests use
	 * <code>Executors.newFixedThreadPool(1)</code>
	 *
	 * @param threadPool
	 *            an instance of {@link ThreadPoolExecutor} to use for
	 *            queuing/pooling requests.
	 */
	public void setThreadPool(ThreadPoolExecutor threadPool) {
		this.threadPool = threadPool;
	}

	/**
	 * Sets the User-Agent header to be sent with each request.
	 *
	 * @param userAgent
	 *            the string to use in the User-Agent header.
	 */
	public void setUserAgent(String userAgent) {
		HttpProtocolParams.setUserAgent(this.httpClient.getParams(), userAgent);
	}

	/**
	 * Sets the SSLSocketFactory to user when making requests. By default, a
	 * new, default SSLSocketFactory is used.
	 *
	 * @param sslSocketFactory
	 *            the socket factory to use for https requests.
	 */
	public void setSSLSocketFactory(SSLSocketFactory sslSocketFactory) {
		this.httpClient.getConnectionManager().getSchemeRegistry().register(new Scheme("https", sslSocketFactory, 443));
	}

	/**
	 * Cancels any pending (or potentially active) requests associated with the
	 * passed Context.
	 * <p>
	 * TODO delete under comment <b>Note:</b> This will only affect requests
	 * which were created with a non-null android Context. This method is
	 * intended to be used in the onDestroy method of your android activities to
	 * destroy all requests which are no longer required.
	 *
	 * @param context
	 *            the android Context instance associated to the request.
	 * @param mayInterruptIfRunning
	 *            specifies if active requests should be cancelled along with
	 *            pending requests.
	 */
	public void cancelRequests(Context context, boolean mayInterruptIfRunning) {
		List<WeakReference<Future>> requestList = requestMap.get(context);
		if (requestList != null) {
			for (WeakReference<Future> requestRef : requestList) {
				Future request = requestRef.get();
				if (request != null) {
					request.cancel(mayInterruptIfRunning);
				}
			}
		}
		requestMap.remove(context);
	}

	/**
	 * Perform a HTTP GET request, without any parameters.
	 *
	 * @param url
	 *            the URL to send the request to.
	 * @param responseHandler
	 *            the response handler instance that should handle the response.
	 * @param protocol
	 *            for identify in response handler which response is come. It is
	 *            preferable to use protocol of AsyncMVP.
	 */
	public void get(String url, IAsyncHttpResponseHandler responseHandler, int protocol, int cacheProtocol) {
		get(null, new AsyncHttpRequestParams(url), responseHandler, protocol, cacheProtocol);
	}

	/**
	 * Perform a HTTP GET request with parameters.
	 *
	 * @param params
	 *            the URL to send the request to and additional GET parameters
	 *            to send with the request.
	 * @param responseHandler
	 *            the response handler instance that should handle the response.
	 * @param protocol
	 *            for identify in response handler which response is come. It is
	 *            preferable to use protocol of AsyncMVP.
	 */
	public void get(AsyncHttpRequestParams params, IAsyncHttpResponseHandler responseHandler, int protocol, int cacheProtocol) {
		get(null, params, responseHandler, protocol, cacheProtocol);
	}

	/**
	 * Perform a HTTP GET request and track the Android Context which initiated
	 * the request.
	 *
	 * @param context
	 *            the Android Context which initiated the request.
	 * @param params
	 *            the URL to send the request to and additional GET parameters
	 *            to send with the request.
	 * @param responseHandler
	 *            the response handler instance that should handle the response.
	 * @param protocol
	 */
	public void get(Context context, AsyncHttpRequestParams params, IAsyncHttpResponseHandler responseHandler, int protocol,
			int cacheProtocol) {
		sendRequest(httpClient, httpContext, new HttpGet(params.toString()), null, responseHandler, context, protocol,
				cacheProtocol, Integer.toString(params.toString().hashCode()));
	}

	/**
	 * Perform a HTTP POST request, without any parameters.
	 *
	 * @param url
	 *            the URL to send the request to.
	 * @param responseHandler
	 *            the response handler instance that should handle the response.
	 * @param protocol
	 *            for identify in response handler which response is come. It is
	 *            preferable to use protocol of AsyncMVP.
	 */
	public void post(String url, IAsyncHttpResponseHandler responseHandler, int protocol, int cacheProtocol) {
		post(null, new AsyncHttpRequestParams(url), responseHandler, protocol, cacheProtocol);
	}

	/**
	 * Perform a HTTP POST request with parameters.
	 *
	 * @param params
	 *            the URL to send the request to and additional POST parameters
	 *            or files to send with the request.
	 * @param responseHandler
	 *            the response handler instance that should handle the response.
	 * @param protocol
	 *            for identify in response handler which response is come. It is
	 *            preferable to use protocol of AsyncMVP.
	 */
	public void post(AsyncHttpRequestParams params, IAsyncHttpResponseHandler responseHandler, int protocol, int cacheProtocol) {
		post(null, params, responseHandler, protocol, cacheProtocol);
	}

	/**
	 * Perform a HTTP POST request and track the Android Context which initiated
	 * the request.
	 *
	 * @param context
	 *            the Android Context which initiated the request.
	 * @param params
	 *            the URL to send the request to and additional POST parameters
	 *            or files to send with the request.
	 * @param responseHandler
	 *            the response handler instance that should handle the response.
	 */
	public void post(Context context, AsyncHttpRequestParams params, IAsyncHttpResponseHandler responseHandler, int protocol,
			int cacheProtocol) {
		sendRequest(httpClient, httpContext, addEntityToRequestBase(params), null, responseHandler, context, protocol,
				cacheProtocol, Integer.toString(params.toString().hashCode()));
	}

	private void sendRequest(DefaultHttpClient client, HttpContext httpContext, HttpUriRequest uriRequest, String contentType,
			IAsyncHttpResponseHandler responseHandler, Context context, int protocol, int cacheProtocol, String cacheId) {
		if (contentType != null) {
			uriRequest.addHeader("Content-Type", contentType);
		}
		Future request = threadPool.submit(new AsyncCachedHttpRequest(client, httpContext, uriRequest, responseHandler, protocol,
				cacheProtocol, cacheId));
		// TODO if use application context there is no need to check
		if (context != null) {
			// Add request to request map
			List<WeakReference<Future>> requestList = requestMap.get(context);
			if (requestList == null) {
				requestList = new LinkedList<WeakReference<Future>>();
				requestMap.put(context, requestList);
			}

			requestList.add(new WeakReference<Future>(request));

			// TODO: Remove dead weakrefs from requestLists?
		}
	}

	private HttpEntityEnclosingRequestBase addEntityToRequestBase(AsyncHttpRequestParams params) {
		HttpEntityEnclosingRequestBase requestBase = new HttpPost(params.getBaseUrl());
		if (params.mHttpParams.size() > 0) {
			requestBase.setEntity(params.getEntity());
		}
		return requestBase;
	}
}