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
package com.hippoapp.asyncmvp.cache;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.app.Application;
import android.content.Context;
import android.os.Parcelable;

/**
 * Cache client which delegate actions like put, get and remove to defined
 * {@link AsyncCacheStorage} according asyncMVP protocol.
 *
 * Before cache actions it's necessary to initialize {@link AsyncCacheStorage}
 * by {@link #initCacheInstance(Context, int, int, int, int, int)}.
 *
 * There are two types of cache:
 * <ul>
 * <li>in-memory cache
 * <li>disk cache
 * </ul>
 * When initialize {@link AsyncCacheStorage} you can define which type(s) of
 * cache you need to use.
 * <ul>
 * <li>if you need to use only in-memory cache use method
 * {@link #initCacheInstance(int, int, int, int)}
 * <li>if you need to use in-memory and internal disk cache use
 * {@link #INTERNAL_CACHE} as <code>typeOfDiskCache</code> parameter in
 * {@link #initCacheInstance(Context, int, int, int, int, int)}
 * <li>if you need to use in-memory and sdcard disk cache use
 * {@link #EXTERNAL_CACHE} as <code>typeOfDiskCache</code> parameter in
 * {@link #initCacheInstance(Context, int, int, int, int, int)}.
 * <p>
 * <b>Note:</b> if sdcard is not mounted disk cache will not be activated and
 * {@link #initCacheInstance(int, int, int, int, int, int)} will throw an
 * {@link IOException}
 * </ul>
 *
 * Special directory for each protocol is defined. The path of
 * {@link #INTERNAL_CACHE} is look like {@link Context#getCacheDir()}
 * /{protocol}/. The path of {@link #EXTERNAL_CACHE} is look like
 * <code>external_storage_directory</code>
 * /{application_package_name}/cache/{protocol}. In low-level directory for each
 * key create separate file.
 * <p>
 * To make available {@link #EXTERNAL_CACHE}, you must declare the
 * {@link android.Manifest.permission#WRITE_EXTERNAL_STORAGE} permission in your
 * Android Manifest.
 *
 * @author Bnet.Android.Developer.Team
 *
 */
public class AsyncCacheClient {

	public static final int INTERNAL_CACHE = 1;
	public static final int EXTERNAL_CACHE = 2;

	private static AsyncCacheClient sAsyncCacheClient;

	private final HashMap<Integer, AsyncCacheStorage> protocolCacheInstanceMap = new HashMap<Integer, AsyncCacheStorage>();
	private Context mContext;

	private AsyncCacheClient(Context context) {
		mContext = context;
	}

	/**
	 * initiate new {@link AsyncCacheClient}. It is preferable to create new
	 * instance in {@link Application#onCreate()}
	 *
	 * @param context
	 *            - context
	 * @return instance of {@link AsyncCacheClient}
	 */
	public static AsyncCacheClient newInstance(Context context) {
		if (sAsyncCacheClient == null) {
			sAsyncCacheClient = new AsyncCacheClient(context);
		}
		return sAsyncCacheClient;
	}

	/**
	 * get instance of current {@link AsyncCacheClient} object. If object is
	 * null {@link NullPointerException} will be thrown
	 *
	 * @return - current instance
	 */
	public static AsyncCacheClient getInstance() {
		if (sAsyncCacheClient == null) {
			throw new NullPointerException("run first newInstance(Context) please");
		}
		return sAsyncCacheClient;
	}

	/**
	 * Initialize concrete {@link AsyncCacheStorage} with in-memory and disk
	 * caches.
	 *
	 * @param context
	 *            - context
	 * @param protocol
	 *            - for creating disk cache directory. It is preferable to use
	 *            protocol of AsyncMVP.
	 * @param initialCapacity
	 *            - initial size of in-memory cache
	 * @param inMemoryCacheExpirationInMinutes
	 *            - cache values live time (explanation: values will be
	 *            truncated after this amount of time)
	 * @param inMemoryCacheMaxConcurrentThreads
	 *            - maximum amount of concurrent threads (explanation: when we
	 *            accessing cache storage asynchronously from different threads)
	 * @param diskCacheExpirationInMinutes
	 *            - external cache values live time (explanation: values will be
	 *            truncated after this amount of time)
	 * @param typeOfDiskCache
	 *            - type of cache: {@link #INTERNAL_CACHE},
	 *            {@link #EXTERNAL_CACHE}
	 *
	 * @throws IOException
	 *             if sd card not mounted
	 */
	public void initCacheInstance(int protocol, int inMemoryCacheInitialCapacity, int inMemoryCacheExpirationInMinutes,
			int inMemoryCacheMaxConcurrentThreads, int diskCacheExpirationInMinutes, int diskCacheType) throws IOException {
		if (protocolCacheInstanceMap.get(protocol) == null) {
			AsyncCacheStorage asyncCacheStorage = new AsyncCacheStorage(mContext, protocol, inMemoryCacheInitialCapacity,
					inMemoryCacheExpirationInMinutes, inMemoryCacheMaxConcurrentThreads, diskCacheExpirationInMinutes,
					diskCacheType);

			protocolCacheInstanceMap.put(protocol, asyncCacheStorage);
		}
	}

	/**
	 * initialize concrete {@link AsyncCacheStorage} with in-memory cache
	 *
	 * @param protocol
	 *            - for creating disk cache directory. It is preferable to use
	 *            protocol of AsyncMVP.
	 * @param inMemoryCacheInitialCapacity
	 *            - initial size of in-memory cache
	 * @param inMemoryCacheExpirationInMinutes
	 *            - in-memory cache values live time (explanation: values will
	 *            be truncated after this amount of time)
	 * @param inMemoryCacheMaxConcurrentThreads
	 *            - maximum amount of concurrent threads (explanation: when we
	 *            accessing cache storage asynchronously from different threads)
	 */
	public void initCacheInstance(int protocol, int inMemoryCacheInitialCapacity, int inMemoryCacheExpirationInMinutes,
			int inMemoryCacheMaxConcurrentThreads) {
		if (protocolCacheInstanceMap.get(protocol) == null) {
			AsyncCacheStorage asyncCacheStorage = new AsyncCacheStorage(inMemoryCacheInitialCapacity,
					inMemoryCacheExpirationInMinutes, inMemoryCacheMaxConcurrentThreads);

			protocolCacheInstanceMap.put(protocol, asyncCacheStorage);
		}
	}

	/**
	 * Associates the specified value with the specified key in cache defined by
	 * protocol. If the cache previously contained a caching for the key, the
	 * old value is replaced by the specified value.
	 *
	 * @param protocol
	 *            define which {@link AsyncCacheStorage} to use
	 * @param key
	 *            key with which the specified value is to be associated
	 * @param value
	 *            value to be associated with the specified key
	 */
	public void put(int protocol, String key, Parcelable value) {
		AsyncCacheStorage asyncCacheStorage = getCacheInstance(protocol);
		asyncCacheStorage.put(key, value);
	}

	/**
	 * Associates the specified value with the specified key in cache defined by
	 * protocol. If the cache previously contained a caching for the key, the
	 * old value is replaced by the specified value.
	 *
	 * @param protocol
	 *            define which {@link AsyncCacheStorage} to use
	 * @param key
	 *            key with which the specified value is to be associated
	 * @param value
	 *            value to be associated with the specified key
	 */
	public void put(int protocol, String key, Parcelable[] values) {
		AsyncCacheStorage asyncCacheStorage = getCacheInstance(protocol);
		asyncCacheStorage.put(key, values);
	}

	/**
	 * Copies all of the mappings from the specified map to cache defined by
	 * protocol. The effect of this call is equivalent to that of calling
	 * {@link #put(int, String, Parcelable)} on this cache for each mapping
	 * key-value.
	 *
	 * @param protocol
	 *            define which {@link AsyncCacheStorage} to use
	 * @param keyValueMap
	 *            mappings to be stored in this map
	 */
	public void putAll(int protocol, Map<String, Parcelable> keyValueMap) {
		AsyncCacheStorage asyncCacheStorage = getCacheInstance(protocol);
		asyncCacheStorage.putAll(keyValueMap);
	}

	/**
	 * Returns the value to which the specified key is mapped, or {@code null}
	 * if cache defined by protocol contains no mapping for the key.
	 *
	 * @param protocol
	 *            define which {@link AsyncCacheStorage} to use
	 * @param key
	 *            the key whose associated value is to be returned
	 * @return the value to which the specified key is mapped, or {@code null}
	 *         if cache contains no caching for the key
	 */
	public Parcelable get(int protocol, String key) {
		AsyncCacheStorage asyncCacheStorage = getCacheInstance(protocol);
		return asyncCacheStorage.get(key);
	}

	/**
	 * Returns the value to which the specified key is mapped, or {@code null}
	 * if cache defined by protocol contains no mapping for the key.
	 *
	 * @param protocol
	 *            define which {@link AsyncCacheStorage} to use
	 * @param key
	 *            the key whose associated value is to be returned
	 * @return the value to which the specified key is mapped, or {@code null}
	 *         if cache contains no caching for the key
	 */
	public Parcelable[] getArray(int protocol, String key) {
		AsyncCacheStorage asyncCacheStorage = getCacheInstance(protocol);
		return asyncCacheStorage.getArray(key);
	}

	/**
	 * Removes the mapping for a key from cache defined by protocol if it is
	 * present
	 *
	 * @param protocol
	 *            define which {@link AsyncCacheStorage} to use
	 * @param key
	 *            the key whose associated value is to be removed
	 */
	public void remove(int protocol, String key) {
		AsyncCacheStorage asyncCacheStorage = getCacheInstance(protocol);
		asyncCacheStorage.remove(key);
	}

	/**
	 * Removes all of the cachings from cache defined by protocol.
	 *
	 * @param protocol
	 *            define which {@link AsyncCacheStorage} to use
	 */
	public void removeAll(int protocol) {
		AsyncCacheStorage asyncCacheStorage = getCacheInstance(protocol);
		asyncCacheStorage.removeAll();
	}

	private AsyncCacheStorage getCacheInstance(int protocol) {
		AsyncCacheStorage asyncCacheStorage = null;
		if ((asyncCacheStorage = protocolCacheInstanceMap.get(protocol)) == null) {
			throw new NullPointerException(
					"Call initCacheInstance(Context context, int protocol, int initialCapacity, int expirationInMinutes, int maxConcurrentThreads, int typeOfDiskCache) firstly.");
		}
		return asyncCacheStorage;
	}

}
