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

package com.hippoapp.asyncmvp.location.changedreceiver;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;

import com.hippoapp.asyncmvp.location.base.AbstractLocationChangedReceiver;
import com.hippoapp.asyncmvp.location.finder.LegacyLastLocationFinder;

/**
 * This Receiver class is used to listen for Broadcast Intents that announce
 * that a location change has occurred while this application isn't visible.
 *
 * Where possible, this is triggered by a Passive Location listener.
 */
public class PassiveLocationChangedReceiver extends
		AbstractLocationChangedReceiver {

	protected static String TAG = PassiveLocationChangedReceiver.class
			.getSimpleName();

	/**
	 * When a new location is received, extract it from the Intent and use it to
	 * start the Service used to update the list of nearby places.
	 *
	 * This is the Passive receiver, used to receive Location updates from third
	 * party apps when the Activity is not visible.
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		String key = LocationManager.KEY_LOCATION_CHANGED;
		Location location = null;
		if (intent.hasExtra(key)) {
			// This update came from Passive provider, so we can extract the
			// location
			// directly.
			location = (Location) intent.getExtras().get(key);
		} else {
			// This update came from a recurring alarm. We need to determine if
			// there
			// has been a more recent Location received than the last location
			// we used.

			// Get the best last location detected from the providers.
			LegacyLastLocationFinder lastLocationFinder = new LegacyLastLocationFinder(
					context);
			location = lastLocationFinder.getLastBestLocation(MAX_DISTANCE,
					System.currentTimeMillis() - MAX_TIME);
			SharedPreferences prefs = context.getSharedPreferences(
					SHARED_PREFERENCE_FILE, Context.MODE_PRIVATE);

			// Get the last location we used to get a listing.
			long lastTime = prefs.getLong(SP_KEY_LAST_LIST_UPDATE_TIME,
					Long.MIN_VALUE);
			long lastLat = prefs.getLong(SP_KEY_LAST_LIST_UPDATE_LAT,
					Long.MIN_VALUE);
			long lastLng = prefs.getLong(SP_KEY_LAST_LIST_UPDATE_LNG,
					Long.MIN_VALUE);
			Location lastLocation = new Location(CONSTRUCTED_LOCATION_PROVIDER);
			lastLocation.setLatitude(lastLat);
			lastLocation.setLongitude(lastLng);

			// Check if the last location detected from the providers is either
			// too soon, or too close to the last
			// value we used. If it is within those thresholds we set the
			// location to null to prevent the update
			// Service being run unnecessarily (and spending battery on data
			// transfers).
			if ((lastTime > System.currentTimeMillis() - MAX_TIME)
					|| (lastLocation.distanceTo(location) < MAX_DISTANCE)) {
				location = null;
			}
		}

		sendLocation(location);
	}
}