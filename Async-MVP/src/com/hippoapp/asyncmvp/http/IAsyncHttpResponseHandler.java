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

/**
 * Used to handle the responses from requests made using {@link AsyncHttpClient}
 * . The {@link #onSuccess(String)}, {@link #onFailure(Throwable)},
 * {@link #onStart()}, and {@link #onFinish()} methods describe the state of
 * request.
 * <p>
 * For example:
 * <p>
 *
 * <pre>
 * AsyncHttpClient client = new AsyncHttpClient();
 * client.get(new AsyncHttpRequestParams(&quot;http://www.google.com&quot;), new AsyncHttpResponseHandler() {
 * 	&#064;Override
 * 	public void onStart(int protocol) {
 * 		// request is started
 * 	}
 *
 * 	&#064;Override
 * 	public void onSuccess(int protocol, byte[] content) {
 * 		// request is successful
 * 	}
 *
 * 	&#064;Override
 * 	public void onFailure(int protocol, Throwable e) {
 * 		// request is failed
 * 	}
 *
 * 	&#064;Override
 * 	public void onFinish(int protocol) {
 * 		// request is finished (despite success or failure)
 * 	}
 * });
 * </pre>
 */
public interface IAsyncHttpResponseHandler {
	/**
	 * Executes when request is started
	 *
	 * @param protocol
	 *            for identify which response is come. It is preferable to use
	 *            protocol of AsyncMVP.
	 */
	public void onStart(int protocol);

	/**
	 * Executes when request is finished (success or
	 * failure)
	 *
	 * @param protocol
	 *            for identify which response is come. It is preferable to use
	 *            protocol of AsyncMVP.
	 */
	public void onFinish(int protocol);

	/**
	 * Executes when request is run successfully
	 *
	 * @param protocol
	 *            for identify which response is come. It is preferable to use
	 *            protocol of AsyncMVP.
	 *
	 * @param content
	 *            the body of the HTTP response from the server
	 */
	public void onSuccess(int protocol, byte[] content);

	/**
	 * Executes when request is failed fails and does not complete
	 *
	 * @param protocol
	 *            for identify which response is come. It is preferable to use
	 *            protocol of AsyncMVP.
	 * @param error
	 *            the underlying cause of the failure
	 */
	public void onFailure(int protocol, Throwable error);

}