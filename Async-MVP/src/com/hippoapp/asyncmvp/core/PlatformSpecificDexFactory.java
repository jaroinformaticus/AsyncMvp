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

import android.content.Context;

import com.hippoapp.asyncmvp.utils.AsyncMvpConstants;

import dalvik.system.DexFile;

/**
 * Platform independent getting dexfile of application.
 *
 */
class PlatformSpecificDexFactory implements AsyncMvpConstants {

	public static DexFile getDexFile(Context context) throws Exception {
		String dexPath;
		if (SUPPORTS_FROYO) {
			dexPath = context.getPackageCodePath();
		} else {
			dexPath = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0).sourceDir;
		}
		return new DexFile(dexPath);
	}
}
