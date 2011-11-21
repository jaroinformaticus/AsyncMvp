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

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

//TODO rewrite
/**
 * A collection of string request parameters to send with HTTP requests.
 * <p>
 * For example:
 * <p>
 *
 * <pre>
 * AsyncHttpRequestParams params = new AsyncHttpRequestParams(&quot;http://ololo.net&quot;);
 * params.put(&quot;action&quot;, &quot;service_view&quot;);
 * params.put(&quot;service&quot;, &quot;123456&quot;);
 *
 * AsyncHttpClient client = new AsyncHttpClient();
 * client.post(params, responseHandler, ...);
 * </pre>
 */
public class AsyncHttpRequestParams {
	private static final String ENCODING = "utf-8";

	protected String mBaseUrl;

	protected ConcurrentHashMap<String, String> mHttpParams = new ConcurrentHashMap<String, String>();

	/**
	 * Constructs a new empty {@link AsyncHttpRequestParams} instance.
	 *
	 * @param baseUrl
	 *            - base URL
	 */
	public AsyncHttpRequestParams(String baseUrl) {
		mBaseUrl = baseUrl;
	}

	/**
	 * Constructs a new {@link AsyncHttpRequestParams} instance containing the
	 * key/value string params from the specified map.
	 *
	 * @param baseUrl
	 *            - base URL
	 *
	 * @param paramMap
	 *            - the source key/value string map to add.
	 */
	public AsyncHttpRequestParams(String baseUrl, Map<String, String> paramMap) {
		mBaseUrl = baseUrl;
		for (Map.Entry<String, String> entry : paramMap.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Adds a key-value string pair to the request.
	 *
	 * @param key
	 *            - the key name for the new param.
	 * @param value
	 *            - the value string for the new param.
	 */
	public void put(String key, String value) {
		if (key != null && value != null) {
			mHttpParams.put(key, value);
		}
	}

	/**
	 * Removes parameter from request.
	 *
	 * @param key
	 *            - the key name for the parameter to remove.
	 */
	public void remove(String key) {
		mHttpParams.remove(key);
	}

	protected String getBaseUrl() {
		return mBaseUrl;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder(mBaseUrl);
		result.append("?");
		for (ConcurrentHashMap.Entry<String, String> entry : mHttpParams.entrySet()) {
			result.append(entry.getKey());
			result.append("=");
			result.append(entry.getValue());
			result.append("&");
		}

		return result.substring(0, result.length() - 1);
	}

	/** package */
	HttpEntity getEntity() {
		HttpEntity entity = null;

		try {
			entity = new UrlEncodedFormEntity(getParamsList(), ENCODING);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		return entity;
	}

	protected List<BasicNameValuePair> getParamsList() {
		List<BasicNameValuePair> lparams = new LinkedList<BasicNameValuePair>();

		for (ConcurrentHashMap.Entry<String, String> entry : mHttpParams.entrySet()) {
			lparams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
		}

		return lparams;
	}

	protected String getParamString() {
		return URLEncodedUtils.format(getParamsList(), ENCODING);
	}

}