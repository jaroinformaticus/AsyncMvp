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

package com.hippoapp.asyncmvp.location;

import android.app.AlarmManager;
import android.content.Context;
import android.location.LocationManager;

import com.hippoapp.asyncmvp.location.base.LastLocationFinder;
import com.hippoapp.asyncmvp.location.base.LocationUpdateRequester;
import com.hippoapp.asyncmvp.location.finder.GingerbreadLastLocationFinder;
import com.hippoapp.asyncmvp.location.finder.LegacyLastLocationFinder;
import com.hippoapp.asyncmvp.location.updaterequester.EclairLocationUpdateRequester;
import com.hippoapp.asyncmvp.location.updaterequester.FroyoLocationUpdateRequester;
import com.hippoapp.asyncmvp.location.updaterequester.GingerbreadLocationUpdateRequester;
import com.hippoapp.asyncmvp.utils.AsyncMvpConstants;

/**
 * Factory class to create the correct instances of a variety of classes with
 * platform specific implementations.
 *
 */
public class PlatformSpecificImplementationFactory implements AsyncMvpConstants {

	/**
	 * Create a new LocationUpdateRequester
	 *
	 * @param locationManager
	 *            Location Manager
	 * @return LocationUpdateRequester
	 */
	public static LocationUpdateRequester getLocationUpdateRequester(
			Context context) {
		LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		LocationUpdateRequester locatonUpdateRequester = null;
		if (SUPPORTS_GINGERBREAD) {
			locatonUpdateRequester = new GingerbreadLocationUpdateRequester(
					locationManager);
		} else if (SUPPORTS_FROYO) {
			locatonUpdateRequester = new FroyoLocationUpdateRequester(
					locationManager);
		} else if (SUPPORTS_ECLAIR) {
			locatonUpdateRequester = new EclairLocationUpdateRequester(
					locationManager, alarmManager);
		}

		return locatonUpdateRequester;
	}

	public static LastLocationFinder getLastLocationFinder(Context context) {
		return SUPPORTS_GINGERBREAD ? new GingerbreadLastLocationFinder(context)
				: new LegacyLastLocationFinder(context);
	}

}
