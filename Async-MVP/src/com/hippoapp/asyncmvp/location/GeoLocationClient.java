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
package com.hippoapp.asyncmvp.location;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;

import com.hippoapp.asyncmvp.core.Presenter;
import com.hippoapp.asyncmvp.core.Presenter.LayerStatus;
import com.hippoapp.asyncmvp.core.Presenter.ModelLayer;
import com.hippoapp.asyncmvp.core.Presenter.ModelLayerInterface;
import com.hippoapp.asyncmvp.location.base.LastLocationFinder;
import com.hippoapp.asyncmvp.location.base.LocationUpdateRequester;
import com.hippoapp.asyncmvp.location.changedreceiver.ActiveLocationChangedReceiver;
import com.hippoapp.asyncmvp.location.changedreceiver.PassiveLocationChangedReceiver;
import com.hippoapp.asyncmvp.utils.AsyncMvpConstants;
import com.hippoapp.asyncmvp.utils.AsyncMvpPresenterProtocol;

/**
 * Simple layer example which determing user location as fast as possible.
 * Module is also compatible with Android OS 1.6+
 * <p>
 * The layer has 3 protocols: {@link PresenterProtocol#V_ENABLE_UPDATE_LOCATION}, {@link PresenterProtocol#V_DISABLE_UPDATE_LOCATION},
 * {@link PresenterProtocol#P_UPDATE_LOCATION}.
 * <p>
 * Send an empty message {@link PresenterProtocol#V_ENABLE_UPDATE_LOCATION} from
 * View to activate user geo-locating. Usually it gets executed during
 * application startup process or an {@link Activity} responding for displaying
 * user location.
 * <p>
 * User location is coming via protocol
 * {@link PresenterProtocol#P_UPDATE_LOCATION}. The {@link Message} contains an
 * object of type {@link Location} as a {@link Message#obj} parameter.
 * <p>
 * Send an empty message {@link PresenterProtocol#V_DISABLE_UPDATE_LOCATION}
 * from View to turn off user geo-locating. Usually it gets executed during
 * application shut down process or exiting {@link Activity} responding for
 * displaying user location.
 * <p>
 * Protocol description is duplicated in {@link PresenterProtocol}.
 *
 * There is no request state identification because the process of locating user
 * doesn’t take much time and it is discrete.
 *
 */
@ModelLayer(nameInt = AsyncMvpConstants.LAYER_MODEL_GEO_LOCATION_NAME)
public class GeoLocationClient implements AsyncMvpConstants, ModelLayerInterface, AsyncMvpPresenterProtocol {

	protected static final String TAG = GeoLocationClient.class.getSimpleName();

	private LocationManager locationManager;
	private Criteria criteria;

	private PendingIntent locationListenerPendingIntent;
	private PendingIntent locationListenerPassivePendingIntent;

	private LastLocationFinder lastLocationFinder;
	private LocationUpdateRequester locationUpdateRequester;

	private Context mContext;

	@Override
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
		case V_ENABLE_UPDATE_LOCATION:
			enableLocationUpdates();
			break;
		case V_DISABLE_UPDATE_LOCATION:
			disableLocationUpdates();
			break;
		case V_CONNECTION_STATE:
			boolean isConnected = (Boolean) msg.obj;
			PackageManager pm = mContext.getPackageManager();

			ComponentName activeLocationReceiver = new ComponentName(mContext, ActiveLocationChangedReceiver.class);
			ComponentName passiveLocationReceiver = new ComponentName(mContext, PassiveLocationChangedReceiver.class);
			if (isConnected) {
				// The default state for the Location Receiver is enabled.
				pm.setComponentEnabledSetting(activeLocationReceiver, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
						PackageManager.DONT_KILL_APP);

				// The default state for the Location Receiver is enabled.
				pm.setComponentEnabledSetting(passiveLocationReceiver, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
						PackageManager.DONT_KILL_APP);
			} else {
				pm.setComponentEnabledSetting(activeLocationReceiver, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
						PackageManager.DONT_KILL_APP);

				pm.setComponentEnabledSetting(passiveLocationReceiver, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
						PackageManager.DONT_KILL_APP);
			}
		default:
			break;
		}
		return false;
	}

	/**
	 * Start listening for location updates. It is recommend to call this from
	 * onResume
	 */
	private void enableLocationUpdates() {
		AsyncTask<Void, Void, Void> findLastLocationTask = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				// Find the last known location, specifying a required accuracy
				// of within the min distance between updates
				// and a required latency of the minimum time required between
				// updates.
				Location lastKnownLocation = lastLocationFinder.getLastBestLocation(MAX_DISTANCE, System.currentTimeMillis()
						- MAX_TIME);
				// send location to view and model
				Presenter.getInst().sendViewMessage(P_UPDATE_LOCATION, lastKnownLocation);
				Presenter.getInst().sendModelMessage(P_UPDATE_LOCATION, lastKnownLocation);
				return null;
			}
		};
		findLastLocationTask.execute();

		// Normal updates while activity is visible.
		locationUpdateRequester.requestLocationUpdates(MAX_TIME, MAX_DISTANCE, criteria, locationListenerPendingIntent);

		// Passive location updates from 3rd party apps when the Activity isn't
		// visible.
		locationUpdateRequester.requestPassiveLocationUpdates(PASSIVE_MAX_TIME, PASSIVE_MAX_DISTANCE,
				locationListenerPassivePendingIntent);
	}

	/**
	 * Stop listening for location updates. It is recommend to call this from
	 * onPause
	 */
	private void disableLocationUpdates() {
		locationManager.removeUpdates(locationListenerPendingIntent);
		lastLocationFinder.cancel();
	}

	/**
	 * One-off location listener that receives updates from the
	 * {@link LastLocationFinder} . This is triggered where the last known
	 * location is outside the bounds of our maximum distance and latency.
	 *
	 */
	protected LocationListener oneShotLastLocationUpdateListener = new LocationListener() {
		@Override
		public void onLocationChanged(Location l) {
			// send location to view and model
			Presenter.getInst().sendViewMessage(P_UPDATE_LOCATION, l);
			Presenter.getInst().sendModelMessage(P_UPDATE_LOCATION, l);
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}
	};

	@Override
	public void init(Context context) {
		mContext = context;
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

		// Specify the Criteria to use when requesting location updates while
		// the application is Active
		criteria = new Criteria();

		criteria.setAccuracy(Criteria.ACCURACY_FINE);

		// Setup the location update Pending Intents
		Intent activeIntent = new Intent(context, ActiveLocationChangedReceiver.class);
		locationListenerPendingIntent = PendingIntent.getBroadcast(context, 0, activeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		Intent passiveIntent = new Intent(context, PassiveLocationChangedReceiver.class);
		locationListenerPassivePendingIntent = PendingIntent.getBroadcast(context, 0, passiveIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		// Instantiate a LastLocationFinder class.
		// This will be used to find the last known location when the
		// application starts.
		lastLocationFinder = PlatformSpecificImplementationFactory.getLastLocationFinder(context);
		lastLocationFinder.setChangedLocationListener(oneShotLastLocationUpdateListener);

		// Instantiate a Location Update Requester class based on the available
		// platform version.
		// This will be used to request location updates.
		locationUpdateRequester = PlatformSpecificImplementationFactory.getLocationUpdateRequester(context);

	}

	@Override
	public LayerStatus getStatus() {
		return null;
	}

}
