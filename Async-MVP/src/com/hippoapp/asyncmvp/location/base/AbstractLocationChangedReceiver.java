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
package com.hippoapp.asyncmvp.location.base;

import android.content.BroadcastReceiver;
import android.location.Location;

import com.hippoapp.asyncmvp.core.Presenter;
import com.hippoapp.asyncmvp.utils.AsyncMvpConstants;
import com.hippoapp.asyncmvp.utils.AsyncMvpPresenterProtocol;

public abstract class AbstractLocationChangedReceiver extends BroadcastReceiver implements AsyncMvpConstants, AsyncMvpPresenterProtocol {

	private static final String TAG = AbstractLocationChangedReceiver.class.getSimpleName();

	protected void sendLocation(Location location) {
		if (location != null) {
			// send location to view and model
			Presenter.getInst().sendViewMessage(P_UPDATE_LOCATION, location);
			Presenter.getInst().sendModelMessage(P_UPDATE_LOCATION, location);
		}
	}
}
