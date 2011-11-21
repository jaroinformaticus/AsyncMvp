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

import android.location.Location;
import android.os.Message;

import com.hippoapp.asyncmvp.core.Presenter.LayerStatus;

/**
 * List of framework protocols. Your own list must extends this interface for
 * non-conflicting of identificators
 *
 */
public interface AsyncMvpPresenterProtocol {

	// в arg1 sends nameInt of model module
	public int GET_STATUS = -1;
	/**
	 * return {@link LayerStatus} of model module: in {@link Message#obj}; in
	 * {@link Message#arg1} - nameInt of module
	 */
	public int PUT_STATUS = -2;

	/**
	 * Empty message of activating GeoLocationModule
	 */
	int V_ENABLE_UPDATE_LOCATION = 100;
	/**
	 * answer of {@link AsyncMvpPresenterProtocol#V_ENABLE_UPDATE_LOCATION}.
	 * Return {@link Location} in {@link Message#obj}.
	 *
	 */
	int P_UPDATE_LOCATION = 101;
	/**
	 * Empty message of deactivating GeoLocationModule.
	 */
	int V_DISABLE_UPDATE_LOCATION = 102;

	int V_CONNECTION_STATE = 103;
	int P_CONNECTION_STATE = 104;
}
