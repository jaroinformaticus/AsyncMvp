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
package com.hippoapp.asyncmvp.utils;

import android.app.AlarmManager;

public interface AsyncMvpConstants {

	String SHARED_PREFERENCE_FILE = "SHARED_PREFERENCE_FILE";
	String SP_KEY_IN_BACKGROUND = "SP_KEY_IN_BACKGROUND";
	String SP_KEY_RUN_ONCE = "SP_KEY_RUN_ONCE";
	String SP_KEY_LAST_LIST_UPDATE_TIME = "SP_KEY_LAST_LIST_UPDATE_TIME";
	String SP_KEY_LAST_LIST_UPDATE_LAT = "SP_KEY_LAST_LIST_UPDATE_LAT";
	String SP_KEY_LAST_LIST_UPDATE_LNG = "SP_KEY_LAST_LIST_UPDATE_LNG";

	String CONSTRUCTED_LOCATION_PROVIDER = "CONSTRUCTED_LOCATION_PROVIDER";

	String PREF_CONNECTION_STATE = "CONNECTION_STATE";

	boolean SUPPORTS_GINGERBREAD = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD;
	boolean SUPPORTS_FROYO = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO;
	boolean SUPPORTS_ECLAIR = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ECLAIR;

	// The default search radius when searching for places nearby.
	int DEFAULT_RADIUS = 150;
	// The maximum distance the user should travel between location updates.
	int MAX_DISTANCE = DEFAULT_RADIUS / 2;
	// The maximum time that should pass before the user gets a location update.
	long MAX_TIME = AlarmManager.INTERVAL_FIFTEEN_MINUTES;

	// You will generally want passive location updates to occur less frequently
	// than active updates. You need to balance location freshness with battery
	// life.
	// The location update distance for passive updates.
	int PASSIVE_MAX_DISTANCE = MAX_DISTANCE;
	// The location update time for passive updates
	long PASSIVE_MAX_TIME = MAX_TIME;

	int LAYER_MODEL_GEO_LOCATION_NAME = 200;
}
