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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.hippoapp.asyncmvp.utils.AsyncMvpConstants;

/**
 * Осуществляет управление пользовательскими настройками. Инициализится на этапе
 * запуска приложения. You do not instantiate this class directly; instead,
 * retrieve it through {@link PreferencesManager#getInst()}.
 *
 * @author Bnet.Android.Developer.Team
 *
 */
public class PreferencesManager implements AsyncMvpConstants {

	private static volatile PreferencesManager sInstance;

	private Context mContext;

	private SharedPreferences mSettings;

	private Editor mEditor;

	private SharedPreferences mSettingsBackup;

	private Editor mEditorBackup;

	public static final String SUFFIX_BACK = "_pref_back";

	private Set<String> mBackupSet;

	private String mPrefName;

	public static final void initInstance(Context context, Set<String> backupSet) {
		sInstance = new PreferencesManager(context, backupSet);
	}

	public void reboot() {
		mPrefName = mContext.getPackageName().replace('.', '_');
		mSettings = mContext.getSharedPreferences(mPrefName, 0);
		mEditor = mSettings.edit();
		mSettingsBackup = mContext.getSharedPreferences(mPrefName + SUFFIX_BACK, 0);
		mEditorBackup = mSettingsBackup.edit();
	}

	public static final PreferencesManager getInst() {
		return sInstance;
	}

	public void dataChanged() {
		if (SUPPORTS_FROYO) {
			BackupManager.dataChanged(mContext.getPackageName());
		}
	}

	private PreferencesManager(Context context, Set<String> backupSet) {
		mContext = context;
		mBackupSet = backupSet;
		if (!SUPPORTS_FROYO) {
			mBackupSet.clear();
		}
		reboot();
	}

	private SharedPreferences preference(String key) {
		return mBackupSet.contains(key) ? mSettingsBackup : mSettings;
	}

	public String getPrefFileName() {
		return mPrefName;
	}

	public boolean get(String key, boolean defValue) {
		return preference(key).getBoolean(key, defValue);
	}

	public float get(String key, float defValue) {
		return preference(key).getFloat(key, defValue);
	}

	public int get(String key, int defValue) {
		return preference(key).getInt(key, defValue);
	}

	public long get(String key, long defValue) {
		return preference(key).getLong(key, defValue);
	}

	public String get(String key, String defValue) {
		return preference(key).getString(key, defValue);
	}

	public void put(String key, boolean value) {
		if (mBackupSet.contains(key)) {
			mEditorBackup.putBoolean(key, value);
			mEditorBackup.commit();
			dataChanged();
		} else {
			mEditor.putBoolean(key, value);
			mEditor.commit();
		}
	}

	public void put(String key, float value) {
		if (mBackupSet.contains(key)) {
			mEditorBackup.putFloat(key, value);
			mEditorBackup.commit();
			dataChanged();
		} else {
			mEditor.putFloat(key, value);
			mEditor.commit();
		}
	}

	public void put(String key, int value) {
		if (mBackupSet.contains(key)) {
			mEditorBackup.putInt(key, value);
			mEditorBackup.commit();
			dataChanged();
		} else {
			mEditor.putInt(key, value);
			mEditor.commit();
		}
	}

	public void put(String key, long value) {
		if (mBackupSet.contains(key)) {
			mEditorBackup.putLong(key, value);
			mEditorBackup.commit();
			dataChanged();
		} else {
			mEditor.putLong(key, value);
			mEditor.commit();
		}
	}

	public void put(String key, String value) {
		if (mBackupSet.contains(key)) {
			mEditorBackup.putString(key, value);
			mEditorBackup.commit();
			dataChanged();
		} else {
			mEditor.putString(key, value);
			mEditor.commit();
		}
	}

	@Target({ ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface KeysPreference {
	}

	@Target({ ElementType.FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface BackupPreference {
	}
}
