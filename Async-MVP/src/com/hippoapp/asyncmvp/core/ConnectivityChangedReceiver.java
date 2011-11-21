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
package com.hippoapp.asyncmvp.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.hippoapp.asyncmvp.utils.AsyncMvpConstants;
import com.hippoapp.asyncmvp.utils.AsyncMvpPresenterProtocol;

/**
 * Broadcast receiver which gets actual connection state, saves it in preference
 * by {@link AsyncMvpConstants#PREF_CONNECTION_STATE} key and send changed state
 * to model and view by {@link AsyncMvpPresenterProtocol#P_CONNECTION_STATE} and
 * {@link AsyncMvpPresenterProtocol#V_CONNECTION_STATE} protocols
 *
 */
public class ConnectivityChangedReceiver extends BroadcastReceiver implements AsyncMvpConstants, AsyncMvpPresenterProtocol {

	private static final String TAG = ConnectivityChangedReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

		// Check if we are connected to an active data network.
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

		// reorganize async-mvp constants
		PreferencesManager.getInst().put(PREF_CONNECTION_STATE, isConnected);
		// notify by sending message by protocol
		Presenter.getInst().sendModelMessage(V_CONNECTION_STATE, isConnected ? Boolean.TRUE : Boolean.FALSE);
		Presenter.getInst().sendModelMessage(P_CONNECTION_STATE, isConnected ? Boolean.TRUE : Boolean.FALSE);
	}
}
