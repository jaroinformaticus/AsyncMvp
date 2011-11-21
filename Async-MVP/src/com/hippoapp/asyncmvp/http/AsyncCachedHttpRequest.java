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
import java.net.ConnectException;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.os.Parcelable;

import com.hippoapp.asyncmvp.cache.AsyncCacheClient;
import com.hippoapp.asyncmvp.cache.ResponseData;

/** package */
class AsyncCachedHttpRequest implements Runnable {
	private static final String TAG = AsyncCachedHttpRequest.class.getSimpleName();

	private AbstractHttpClient client;

	private HttpContext context;

	private HttpUriRequest request;

	private IAsyncHttpResponseHandler responseHandler;

	private int executionCount;

	private int protocol;

	private String cacheId;
	private int cacheProtocol;

	public AsyncCachedHttpRequest(AbstractHttpClient client, HttpContext context, HttpUriRequest request,
			IAsyncHttpResponseHandler responseHandler, int protocol, int cacheProtocol, String cacheId) {
		this.client = client;
		this.context = context;
		this.request = request;

		this.responseHandler = responseHandler;
		this.protocol = protocol;

		this.cacheProtocol = cacheProtocol;
		this.cacheId = cacheId;

	}

	@Override
	public void run() {
		if (responseHandler != null) {
			try {
				responseHandler.onStart(protocol);
				Parcelable parcelable = null;
				try {
					parcelable = AsyncCacheClient.getInstance().get(cacheProtocol, cacheId);
				} catch (NullPointerException e) {
					// no cache
				}
				if (parcelable != null) {
					responseHandler.onSuccess(protocol, ((ResponseData) parcelable).getResponseBody());
				} else {
					makeRequestWithRetries();
				}

			} catch (IOException e) {
				responseHandler.onFailure(protocol, e);
			}
			responseHandler.onFinish(protocol);
		}
	}

	private void makeRequestWithRetries() throws ConnectException {
		boolean retry = true;
		IOException cause = null;
		HttpRequestRetryHandler retryHandler = client.getHttpRequestRetryHandler();
		while (retry) {
			try {
				makeRequest();
				return;
			} catch (IOException e) {
				cause = e;
				retry = retryHandler.retryRequest(cause, ++executionCount, context);
			} catch (NullPointerException e) {
				// http://code.google.com/p/android/issues/detail?id=5255
				cause = new IOException("NPE in HttpClient" + e.getMessage());
				retry = retryHandler.retryRequest(cause, ++executionCount, context);
			}
		}

		ConnectException ex = new ConnectException();
		ex.initCause(cause);
		throw ex;
	}

	private void makeRequest() throws IOException {
		HttpResponse response = client.execute(request, context);

		StatusLine status = response.getStatusLine();
		if (status.getStatusCode() >= 300) {
			responseHandler.onFailure(protocol, new HttpResponseException(status.getStatusCode(), status.getReasonPhrase()));
		} else {
			byte[] httpResponseByte = EntityUtils.toByteArray(response.getEntity());
			// add to cache
			ResponseData responseData = new ResponseData(status.getStatusCode(), httpResponseByte);
			try {
				AsyncCacheClient.getInstance().put(cacheProtocol, cacheId, responseData);
			} catch (NullPointerException e) {
				// no cache
			}
			responseHandler.onSuccess(protocol, httpResponseByte);
		}
	}
}
