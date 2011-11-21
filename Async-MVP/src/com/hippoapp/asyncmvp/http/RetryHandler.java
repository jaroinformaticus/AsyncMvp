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
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

import android.os.SystemClock;

/** package */
class RetryHandler implements HttpRequestRetryHandler {
	private static final int RETRY_SLEEP_TIME_IN_MILLS = 1500;

	private static Set<Class<? extends Exception>> sRetriedExceptionSet = new HashSet<Class<? extends Exception>>();
	private static Set<Class<? extends Exception>> sUnretriedExceptionSet = new HashSet<Class<? extends Exception>>();

	static {
		// retry-this, since it may happens as part of a Wi-Fi to 3G failover
		sRetriedExceptionSet.add(UnknownHostException.class);

		// never retry timeouts
		sUnretriedExceptionSet.add(InterruptedIOException.class);
	}
	private static final int DEFAULT_MAX_RETRIES = 5;

	@Override
	public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
		boolean retry;

		Boolean b = (Boolean) context.getAttribute(ExecutionContext.HTTP_REQ_SENT);
		boolean sent = (b != null && b.booleanValue());

		if (executionCount > DEFAULT_MAX_RETRIES) {
			retry = false;
		} else if (sUnretriedExceptionSet.contains(exception.getClass())) {
			retry = false;
		} else if (sRetriedExceptionSet.contains(exception.getClass())) {
			retry = true;
		} else if (!sent) {
			// for most other errors, retry only if request hasn't been fully
			// sent yet
			retry = true;
		} else {
			// resend all idempotent requests
			HttpUriRequest currentReq = (HttpUriRequest) context.getAttribute(ExecutionContext.HTTP_REQUEST);
			String requestType = currentReq.getMethod();
			if (!requestType.equals("POST")) {
				retry = true;
			} else {
				// otherwise do not retry
				retry = false;
			}
		}

		if (retry) {
			SystemClock.sleep(RETRY_SLEEP_TIME_IN_MILLS);
		} else {
			exception.printStackTrace();
		}

		return retry;
	}
}