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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.common.collect.MapMaker;
import com.hippoapp.asyncmvp.core.AsyncApplication.OnLowMemoryListener;

/**
 * An object that caches keys to values. A cache cannot contain duplicate keys;
 * each key can assosiated to at most one value in cache.
 *
 * There are 2 types of cache
 * <ul>
 * <li>In-memory cache is small and fast
 * <li>Disk cache is big and slow
 * </ul>
 *
 * <p>
 * Disk caching is presented in 2 types:
 * <ul>
 * <li>Internal memory
 * <li>External memory on SD-card
 * </ul>
 * To define which one of disk caching to use, choose
 * {@link AsyncCacheClient#INTERNAL_CACHE} or
 * {@link AsyncCacheClient#EXTERNAL_CACHE}.
 *
 * <p>
 * When push to the cache the data will be pushed both in memory and on disk, if
 * disk caching is enabled.
 * </p>
 *
 * @author Bnet.Android.Developer.Team
 *
 */
public class AsyncCacheStorage implements OnLowMemoryListener {

	private static final String TAG = AsyncCacheStorage.class.getSimpleName();

	private int mInMemoryInitialCacheCapacity;

	private int mDiskCacheExpirationInMinutes;
	private int mTypeOfDiskCache;

	private String mRootDir = null;

	private boolean isDiskCacheEnabled = false;

	private ConcurrentMap<String, Object> mCache;

	/**
	 * Create new cache storage which support in-memory and disk cache storage
	 * by protocol
	 *
	 * @param context
	 *            - context
	 * @param protocol
	 *            - for creating disk cache directory. It is preferable to use
	 *            protocol of AsyncMVP.
	 * @param inMemoryInitialCacheCapacity
	 *            - initial size of in-memory cache
	 * @param inMemoryCacheExpirationInMinutes
	 *            - cache values live time (explanation: values will be
	 *            truncated after this amount of time)
	 * @param inMemoryMaxConcurrentThreads
	 *            - maximum amount of concurrent threads (explanation: when we
	 *            accessing cache storage asynchronously from different threads)
	 * @param diskCacheExpirationInMinutes
	 *            - external cache values live time (explanation: values will be
	 *            truncated after this amount of time)
	 * @param typeOfDiskCache
	 *            - - type of cache: {@link AsyncCacheClient#INTERNAL_CACHE},
	 *            {@link AsyncCacheClient#EXTERNAL_CACHE}
	 * @throws IOException
	 *             - if sd card not mounted
	 */
	public AsyncCacheStorage(Context context, int protocol, int inMemoryInitialCacheCapacity,
			int inMemoryCacheExpirationInMinutes, int inMemoryMaxConcurrentThreads, int diskCacheExpirationInMinutes,
			int typeOfDiskCache) throws IOException {
		this.mDiskCacheExpirationInMinutes = diskCacheExpirationInMinutes;
		this.mInMemoryInitialCacheCapacity = inMemoryInitialCacheCapacity;

		initInMemoryCache(inMemoryInitialCacheCapacity, inMemoryCacheExpirationInMinutes, inMemoryMaxConcurrentThreads);

		initDiskCache(context, protocol, typeOfDiskCache);
	}

	/**
	 * Create new cache storage which support in-memory cache storage by
	 * protocol
	 *
	 * @param inMemoryInitialCacheCapacity
	 *            - initial size of in-memory cache
	 * @param inMemoryCacheExpirationInMinutes
	 *            - in-memory cache values live time (explanation: values will
	 *            be truncated after this amount of time)
	 * @param inMemoryCacheMaxConcurrentThreads
	 *            - maximum amount of concurrent threads (explanation: when we
	 *            accessing cache storage asynchronously from different threads)
	 */
	public AsyncCacheStorage(int inMemoryInitialCacheCapacity, int inMemoryCacheExpirationInMinutes,
			int inMemoryMaxConcurrentThreads) {
		mInMemoryInitialCacheCapacity = inMemoryInitialCacheCapacity;

		initInMemoryCache(inMemoryInitialCacheCapacity, inMemoryCacheExpirationInMinutes, inMemoryMaxConcurrentThreads);
	}

	/**
	 * Associates the specified value with the specified key in cache. If disk
	 * cache is enabled create separate file for each key.
	 *
	 * @param key
	 *            - key with which the specified value is to be associated
	 * @param value
	 *            - value to be associated with the specified key
	 */
	public void put(String key, Parcelable value) {
		if (isDiskCacheEnabled) {
			cacheToDisk(key, value);
		}
		if (mCache.size() > mInMemoryInitialCacheCapacity) {
			mCache.clear();
		}
		if (value != null) {
			mCache.put(key, value);
		} else {
			Log.d(TAG, "WTF?? value is null by key: " + key);
		}

	}

	/**
	 * Associates the specified value with the specified key in cache. If disk
	 * cache is enabled create separate file for each key.
	 *
	 * @param key
	 *            - key with which the specified value is to be associated
	 * @param values
	 *            - values to be associated with the specified key
	 */
	public void put(String key, Parcelable[] values) {
		if (isDiskCacheEnabled) {
			cacheToDisk(key, values);
		}
		if (mCache.size() > mInMemoryInitialCacheCapacity) {
			mCache.clear();
		}
		if (values != null) {
			mCache.put(key, values);
		} else {
			Log.d(TAG, "WTF?? value is null by key: " + key);
		}
	}

	/**
	 * Copies all of the mappings from the specified map to cache. The effect of
	 * this call is equivalent to that of calling
	 * {@link #put(String, Parcelable)} on this cache for each mapping
	 * key-value. If disk cache is enabled create separate file for each key.
	 *
	 * @param keyValueMap
	 *            mappings to be stored in this map
	 */
	public void putAll(Map<String, Parcelable> keyValueMap) {
		for (String key : keyValueMap.keySet()) {
			put(key, keyValueMap.get(key));
		}
	}

	/**
	 * Returns the value to which the specified key is mapped, or {@code null}
	 * if cache contains no mapping for the key. If expiration time is over,
	 * value in cache will be removed and {@code null} is returned.
	 *
	 * @param key
	 *            - the key whose associated value is to be returned
	 * @return the value to which the specified key is mapped, or {@code null}
	 *         if cache contains no caching for the key
	 */
	public Parcelable get(String key) {
		Parcelable value = (Parcelable) mCache.get(key);
		if (value != null) {
			return value;
		}
		File file = new File(mRootDir + "/" + key);
		if (file.exists()) {
			long ageInMinutes = countFileAgeInMinutes(file);
			if (ageInMinutes >= mDiskCacheExpirationInMinutes && mDiskCacheExpirationInMinutes != 0) {
				file.delete();
				return null;
			}
			try {
				value = readValueFromDisk(file);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}

			if (value == null) {
				return null;
			}
			mCache.put(key, value);

			return value;
		}
		// cache miss
		return null;
	}

	/**
	 * Returns the value to which the specified key is mapped, or {@code null}
	 * if cache contains no mapping for the key. If expiration time is over,
	 * value in cache will be removed and {@code null} is returned.
	 *
	 * @param key
	 *            - the key whose associated value is to be returned
	 * @return the value to which the specified key is mapped, or {@code null}
	 *         if cache contains no caching for the key
	 */
	public Parcelable[] getArray(String key) {
		Parcelable[] value = (Parcelable[]) mCache.get(key);
		if (value != null) {
			return value;
		}

		File file = new File(mRootDir + "/" + key);
		if (file.exists()) {
			long ageInMinutes = countFileAgeInMinutes(file);

			if (ageInMinutes >= mDiskCacheExpirationInMinutes && mDiskCacheExpirationInMinutes != 0) {
				file.delete();
				return null;
			}

			try {
				value = readValuesFromDisk(file);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}

			if (value == null) {
				return null;
			}

			mCache.put(key, value);

			return value;
		}

		// cache miss
		return null;
	}

	/**
	 * Removes the value for a key from cache.
	 *
	 * @param key
	 *            - the key whose associated value is to be removed
	 */
	public void remove(String key) {
		mCache.remove(key);

		if (isDiskCacheEnabled) {
			File cachedValue = new File(mRootDir + "/" + key);
			if (cachedValue.exists()) {
				cachedValue.delete();
			}
		}
	}

	/**
	 * Removes all values from cache. If disk cache enabled all files from
	 * specified directory removed.
	 */
	public void removeAll() {
		mCache.clear();
		if (isDiskCacheEnabled) {
			File cachedValue = new File(mRootDir + "/");
			Log.e(TAG, "exist: " + cachedValue.exists());
			if (cachedValue.exists()) {
				File[] listFiles = cachedValue.listFiles();
				for (int i = 0; i < listFiles.length; ++i) {
					listFiles[i].delete();
				}
			}
		}
	}

	@Override
	public void onLowMemory() {
		if (mTypeOfDiskCache == AsyncCacheClient.INTERNAL_CACHE) {
			removeAll();
		} else {
			mCache.clear();
		}
	}

	private void initInMemoryCache(int initialCapacity, int expirationInMinutes, int maxConcurrentThreads) {
		MapMaker mapMaker = new MapMaker();
		mapMaker.initialCapacity(initialCapacity);
		mapMaker.expiration(expirationInMinutes * 60, TimeUnit.SECONDS);
		mapMaker.concurrencyLevel(maxConcurrentThreads);
		mapMaker.softValues();
		this.mCache = mapMaker.makeMap();
	}

	private void initDiskCache(Context context, int protocol, int typeOfDiskCache) throws IOException {
		mTypeOfDiskCache = typeOfDiskCache;
		switch (typeOfDiskCache) {
		case AsyncCacheClient.INTERNAL_CACHE: {
			File internalCacheDir = context.getCacheDir();
			// apparently on some configurations this can come back as null
			if (internalCacheDir == null) {
				isDiskCacheEnabled = false;
			}
			mRootDir = internalCacheDir.getAbsolutePath() + "/" + Integer.toString(protocol);
			break;
		}
		case AsyncCacheClient.EXTERNAL_CACHE: {
			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				StringBuilder stringBuilder = new StringBuilder();
				stringBuilder.append(Environment.getExternalStorageDirectory().getAbsolutePath() + "/");
				stringBuilder.append(context.getPackageName());
				stringBuilder.append("/cache/");
				stringBuilder.append(Integer.toString(protocol));

				mRootDir = stringBuilder.toString();
			} else {
				isDiskCacheEnabled = false;
				throw new IOException("SD card is not available on this device");
			}
			break;
		}
		default:
			isDiskCacheEnabled = false;
			break;
		}
		// сreate all directories which will contain cache data
		File outFile = new File(mRootDir);
		outFile.mkdirs();

		isDiskCacheEnabled = outFile.exists();

		if (isDiskCacheEnabled) {
			expiryDiskCache();
		}
	}

	private void expiryDiskCache() {
		File[] cachedFiles = new File(mRootDir).listFiles();
		if (cachedFiles == null) {
			return;
		}
		for (File f : cachedFiles) {
			long ageInMinutes = countFileAgeInMinutes(f);

			if (ageInMinutes >= mDiskCacheExpirationInMinutes && mDiskCacheExpirationInMinutes != 0) {
				f.delete();
			}
		}
	}

	private long countFileAgeInMinutes(File file) {
		long lastModified = file.lastModified();
		Calendar currentTimeCalendar = Calendar.getInstance();
		return ((currentTimeCalendar.getTimeInMillis() - lastModified) / (1000 * 60));
	}

	private void cacheToDisk(String key, Parcelable value) {
		key = key.replaceAll("\\W", "_");
		File file = new File(mRootDir + "/" + key);
		try {
			file.createNewFile();
			file.deleteOnExit();

			writeValueToDisk(file, value);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void cacheToDisk(String key, Parcelable[] values) {
		key = key.replaceAll("\\W", "_");
		File file = new File(mRootDir + "/" + key);
		try {
			file.createNewFile();
			file.deleteOnExit();

			writeValueToDisk(file, values);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected Parcelable readValueFromDisk(File file) throws IOException {
		FileInputStream istream = new FileInputStream(file);

		// Read file into byte array
		byte[] dataWritten = new byte[(int) file.length()];
		BufferedInputStream bistream = new BufferedInputStream(istream);
		bistream.read(dataWritten);
		bistream.close();

		// Create parcel with cached data
		Parcel parcelIn = Parcel.obtain();
		parcelIn.unmarshall(dataWritten, 0, dataWritten.length);
		parcelIn.setDataPosition(0);
		// Read class name from parcel and use the class loader to read parcel
		String className = parcelIn.readString();
		// In case this sometimes hits a null value
		if (className == null) {
			return null;
		}
		Class<?> clazz;
		try {
			clazz = Class.forName(className);
			Parcelable parcelable = parcelIn.readParcelable(clazz.getClassLoader());
			return parcelable;
		} catch (ClassNotFoundException e) {
			throw new IOException(e.getMessage());
		}
	}

	protected Parcelable[] readValuesFromDisk(File file) throws IOException {
		FileInputStream istream = new FileInputStream(file);

		// Read file into byte array
		byte[] dataWritten = new byte[(int) file.length()];
		BufferedInputStream bistream = new BufferedInputStream(istream);
		bistream.read(dataWritten);
		bistream.close();

		// Create parcel with cached data
		Parcel parcelIn = Parcel.obtain();
		parcelIn.unmarshall(dataWritten, 0, dataWritten.length);
		parcelIn.setDataPosition(0);

		// Read class name from parcel and use the class loader to read parcel
		String className = parcelIn.readString();
		// In case this sometimes hits a null value
		if (className == null) {
			return null;
		}
		Class<?> clazz;
		try {
			clazz = Class.forName(className);
			return parcelIn.readParcelableArray(clazz.getClassLoader());
		} catch (ClassNotFoundException e) {
			throw new IOException(e.getMessage());
		}
	}

	protected void writeValueToDisk(File file, Parcelable data) throws IOException {
		// Write object into parcel
		Parcel parcelOut = Parcel.obtain();
		parcelOut.writeString(data.getClass().getCanonicalName());
		parcelOut.writeParcelable(data, 0);

		// Write byte data to file
		FileOutputStream ostream = new FileOutputStream(file);
		BufferedOutputStream bistream = new BufferedOutputStream(ostream);
		bistream.write(parcelOut.marshall());
		bistream.close();
	}

	protected void writeValueToDisk(File file, Parcelable[] data) throws IOException {
		// Write object into parcel
		Parcel parcelOut = Parcel.obtain();
		parcelOut.writeString(data.getClass().getCanonicalName());
		parcelOut.writeParcelableArray(data, 0);

		// Write byte data to file
		FileOutputStream ostream = new FileOutputStream(file);
		BufferedOutputStream bistream = new BufferedOutputStream(ostream);
		bistream.write(parcelOut.marshall());
		bistream.close();
	}

}
