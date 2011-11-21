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
package com.hippoapp.asyncmvp.location.updaterequester;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.location.Criteria;
import android.location.LocationManager;

import com.hippoapp.asyncmvp.location.base.LocationUpdateRequester;
import com.hippoapp.asyncmvp.utils.AsyncMvpConstants;

public class EclairLocationUpdateRequester extends LocationUpdateRequester implements AsyncMvpConstants {

	private static final String TAG = EclairLocationUpdateRequester.class.getSimpleName();

	private AlarmManager alarmManager;

	public EclairLocationUpdateRequester(LocationManager locationManager, AlarmManager alarmManager) {
		super(locationManager);
		this.alarmManager = alarmManager;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void requestLocationUpdates(long minTime, long minDistance, Criteria criteria, PendingIntent pendingIntent) {
		// Prior to Gingerbread we needed to find the best provider manually.
		// Note that we aren't monitoring this provider to check if it becomes
		// disabled - this is handled by the calling Activity.
		String provider = mLocationManager.getBestProvider(criteria, true);
		if (provider != null) {
			mLocationManager.requestLocationUpdates(provider, minTime, minDistance, pendingIntent);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void requestPassiveLocationUpdates(long minTime, long minDistance, PendingIntent pendingIntent) {
		// Pre-Froyo there was no Passive Location Provider, so instead we will
		// set an inexact repeating, non-waking alarm
		// that will trigger once the minimum time between passive updates has
		// expired. This is potentially more expensive
		// than simple passive alarms, however the Receiver will ensure we've
		// transitioned beyond the minimum time and
		// distance before initiating a background nearby loction update.
		alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, System.currentTimeMillis() + MAX_TIME, MAX_TIME,
				pendingIntent);
	}

}
