/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippoapp.asyncmvp.location.finder;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import com.hippoapp.asyncmvp.location.base.LastLocationFinder;

/**
 * Legacy implementation of Last Location Finder for all Android platforms down
 * to Android 1.6.
 *
 * This class let's you find the "best" (most accurate and timely) previously
 * detected location using whatever providers are available.
 *
 * Where a timely / accurate previous location is not detected it will return
 * the newest location (where one exists) and setup a one-off location update to
 * find the current location.
 */
public class LegacyLastLocationFinder extends LastLocationFinder {

	protected static String TAG = "PreGingerbreadLastLocationFinder";

	protected LocationListener locationListener;
	protected Criteria criteria;

	/**
	 * Construct a new Legacy Last Location Finder.
	 *
	 * @param context
	 *            Context
	 */
	public LegacyLastLocationFinder(Context context) {
		super(context);
		criteria = new Criteria();
		// Coarse accuracy is specified here to get the fastest possible result.
		// The calling Activity will likely (or have already) request ongoing
		// updates using the Fine location provider.
		criteria.setAccuracy(Criteria.ACCURACY_COARSE);
	}

	/**
	 * Returns the most accurate and timely previously detected location. Where
	 * the last result is beyond the specified maximum distance or latency a
	 * one-off location update is returned via the {@link LocationListener}
	 * specified in {@link setChangedLocationListener}.
	 *
	 * @param minDistance
	 *            Minimum distance before we require a location update.
	 * @param minTime
	 *            Minimum time required between location updates.
	 * @return The most accurate and / or timely previously detected location.
	 */
	@Override
	public Location getLastBestLocation(int minDistance, long minTime) {
		Location bestResult = null;
		float bestAccuracy = Float.MAX_VALUE;
		long bestTime = Long.MAX_VALUE;

		bestResult = getBestLocationResult(bestAccuracy, bestTime, minTime);

		// If the best result is beyond the allowed time limit, or the accuracy
		// of the
		// best result is wider than the acceptable maximum distance, request a
		// single update.
		// This check simply implements the same conditions we set when
		// requesting regular
		// location updates every [minTime] and [minDistance].
		// Prior to Gingerbread "one-shot" updates weren't available, so we need
		// to implement
		// this manually.
		if (locationListener != null && (bestTime > minTime || bestAccuracy > minDistance)) {
			String provider = locationManager.getBestProvider(criteria, true);
			if (provider != null) {
				locationManager.requestLocationUpdates(provider, 0, 0, singeUpdateListener, context.getMainLooper());
			}
		}

		return bestResult;
	}

	/**
	 * This one-off {@link LocationListener} simply listens for a single
	 * location update before unregistering itself. The one-off location update
	 * is returned via the {@link LocationListener} specified in
	 * {@link setChangedLocationListener} .
	 */
	protected LocationListener singeUpdateListener = new LocationListener() {
		@Override
		public void onLocationChanged(Location location) {
			if (locationListener != null && location != null) {
				locationListener.onLocationChanged(location);
			}
			locationManager.removeUpdates(singeUpdateListener);
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onProviderDisabled(String provider) {
		}
	};

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setChangedLocationListener(LocationListener l) {
		locationListener = l;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void cancel() {
		locationManager.removeUpdates(singeUpdateListener);
	}
}
