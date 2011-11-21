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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.hippoapp.asyncmvp.core.PreferencesManager.BackupPreference;
import com.hippoapp.asyncmvp.core.PreferencesManager.KeysPreference;
import com.hippoapp.asyncmvp.core.Presenter.ModelLayer;
import com.hippoapp.asyncmvp.core.Presenter.ModelLayerInterface;

import dalvik.system.DexFile;

/**
 * <code>Application</code>, that not only get global application state but
 * also:
 *
 * <ul>
 * <li>initialize view and model <i>layers</i>
 * <li>initialize container, which responsible for user preferences
 * <li>initialize classes, which need to get global application state when
 * application is starting
 * <li>register application component, which need to get low memory notification
 * </ul>
 *
 * <p>
 * The <code>AsyncApplication</code> is an important part of AsyncMVP framework.
 *
 * <p>
 * Topics covered here:
 * <ol>
 * <li><a href="#Layers">Layers</a>
 * <li><a href="#UserPreference">User preferences container</a>
 * <li><a href="#PrerunnedClass">Pre-runned class initialization</a>
 * <li><a href="#LowMemory">Low memory warning</a>
 * </ol>
 *
 * <a name="Layers"></a>
 * <h3>Layers</h3>
 *
 * <p>
 * Two of four main framework features are MVP-based architecture and module
 * support. Other two key features (Message-base architecture and Asynchrounous
 * execution) covered in {@link Presenter}. Layers and modules were added to
 * support those key features. The module is a framework component that is
 * responsible for a narrow part of sending, receiving, storing or processing
 * data. The layer is an interface for linking up with a module. A good example
 * of modules are HTTP networking component, data cacher, geo-location service
 * component. Layers are used for interactions with those modules. Using layers
 * you can initiate module tasks execution (like turning on the geo-location
 * service), request data processed by the module (like getting some cached
 * data), etc. Layer should realize the interface called
 * {@link ModelLayerInterface} and should be annotated with {@link ModelLayer}.
 * Numeric nonzero annotation parameter nameInt is used to identify a layer or a
 * module within the system. Each module is executed in its own thread to
 * achieve asynchronous data processing. Current module execution status can be
 * accessed via {@link ModelLayerInterface#getStatus()} method. Check out
 * {@link Presenter} for details. High level of development flexibility is
 * achieved by using this module system. Developers can distribute their modules
 * as an open-source product or as a pre-compied libaries. Current framework
 * version contains built-in geo-location module which allows to get user’s
 * location as fast as it allowed by user’s device.
 *
 * <a name="UserPreference"></a>
 * <h3>User preferences container and how to backup its</h3>
 *
 * <p>
 * 99% applications are user-oriented. So, using and storing different user
 * settings is a mandatory part of an application. AsyncMVP allows you to
 * control user settings fast and simply. In AsyncMVP ideology all user settings
 * are stored in a single class - container. To indicate and initialize
 * container class should be annotated with {@link KeysPreference}. Starting
 * from Android 2.3 we can store a backup copy of user settings in cloud.
 * AsyncMVP support this feature and all you need to do is annotate needed
 * fields with {@link BackupPreference}. AsyncMVP framework is compatible with
 * late Android OS versions, so if you launch an application annotated with
 * {@link BackupPreference} there should be no errors in run-time.
 *
 * <a name="PrerunnedClass"></a>
 * <h3>Pre-runned class initialization</h3>
 *
 * <p>
 * Usually an application has some classes that should be initialized during
 * application startup. The class should realize interface
 * {@link OnInitInstance} to get initialized during the startup process.
 *
 * <a name="LowMemory"></a>
 * <h3>Low memory warning</h3>
 *
 * <p>
 * Another useful feature is onLowMemory method in Application class. An object
 * should realize interface {@link OnLowMemoryListener} interface to be notified
 * of the low memory situation. AsyncApplication has
 * {@link AsyncApplication#addOnLowMemoryListener(OnLowMemoryListener)} /
 * {@link AsyncApplication#removeOnLowMemoryListener(OnLowMemoryListener)}
 * methods to add or remove objects that should be notified of getting low on
 * memory.
 *
 * <p>
 * The framework also provides a notifications of establishing and losing
 * internet connection. Check out {@link ConnectivityChangedReceiver} class for
 * details.
 *
 * To make your app supporting all those features you should write following in
 * <code>AndroidManifest.xml</code> application class name
 *
 * <pre>
 * <application android:name="com.hippoapp.asyncmvp.core.AsyncAppliction” …
 * </application> user and registration information backup class name you got
 * from linkToRegistration
 *
 *
 * <application
 * android:backupAgent="com.hippoapp.asyncmvp.core.froyo.BackupAgent" …
 * <meta-data android:name="com.google.android.backup.api_key"
 * android:value="the api_key you’ve got /> </application> register an internet
 * connection state receiver
 *
 *
 * <receiver
 * android:name="com.hippoapp.async.mvp.core.ConnectivityChangedReceiver">
 * <intent-filter> <action android:name="android.net.conn.CONNECTIVITY_CHANGE"
 * /> </intent-filter> </receiver>
 * </pre>
 *
 * <p>
 * To make available get connection status, you must declare the
 * {@link android.Manifest.permission#ACCESS_NETWORK_STATE} permission in your
 * Android Manifest.
 *
 *
 *
 * @author Bnet.Android.Developer.Team
 */
public class AsyncApplication extends Application {

	/**
	 * For debug identification
	 */
	public static final String TAG = AsyncApplication.class.getSimpleName();

	/**
	 * Set of objects which want to get notification about {
	 * {@link #onLowMemory()} method execution.
	 */
	private HashSet<OnLowMemoryListener> mLowMemoryListeners = new HashSet<OnLowMemoryListener>();

	@Override
	public void onCreate() {
		super.onCreate();
		init();
	}

	/**
	 * All initialization going here.
	 *
	 */
	private void init() {
		List<ModelLayerInterface> listInboxLayers = new ArrayList<ModelLayerInterface>();

		Set<String> backupPreference = new HashSet<String>();

		try {
			// get full classes list of application
			ClassLoader loader = getClassLoader();
			String packageName = getPackageName();
			DexFile dexFile = PlatformSpecificDexFactory.getDexFile(this);
			Enumeration<String> enumeration = dexFile.entries();

			while (enumeration.hasMoreElements()) {
				String className = enumeration.nextElement();
				if (className.length() >= packageName.length()
						&& packageName.equals(className.substring(0, packageName.length()))) {
					Class<?> appClass = loader.loadClass(className);
					Object classInstance = null;
					// if class implements InboxLayerInterface and is annotated
					// by @InboxLayer, so this class is a layer
					if (Utils.classContainsInterfaceByName(appClass, ModelLayerInterface.class)) {
						ModelLayerInterface inboxLayerI = (ModelLayerInterface) appClass.newInstance();
						inboxLayerI.init(this);
						listInboxLayers.add(inboxLayerI);

						classInstance = inboxLayerI;
					}

					// if class is annotated by @KeyPreference, so this class is
					// user preference container. If some fields need to backup
					// its must annotated by @BackupPreferece
					if (Utils.classContainsAnnotationByName(appClass, KeysPreference.class)) {
						Field[] fields = appClass.getDeclaredFields();
						for (Field field : fields) {
							if (Utils.fieldContainsAnnotationByName(field, BackupPreference.class)) {
								backupPreference.add((String) field.get(""));
							}
						}
					}

					// if class implements OnInitInstance interface, so this
					// class will initialize when application starts
					if (Utils.classContainsInterfaceByName(appClass, OnInitInstance.class)) {
						OnInitInstance initInstance;
						if (classInstance == null) {
							initInstance = (OnInitInstance) appClass.newInstance();
						} else {
							initInstance = (OnInitInstance) classInstance;
						}
						initInstance.initInstance(this);

						classInstance = initInstance;
					}

					// if class implements OnLowMemoryListener, so this class
					// will be notified when onLowMemory method executes
					if (Utils.classContainsInterfaceByName(appClass, OnLowMemoryListener.class)) {
						OnLowMemoryListener onLowMemoryInstance;
						if (classInstance == null) {
							onLowMemoryInstance = (OnLowMemoryListener) appClass.newInstance();
						} else {
							onLowMemoryInstance = (OnLowMemoryListener) classInstance;
						}
						mLowMemoryListeners.add(onLowMemoryInstance);
					}

				}
			}
		} catch (Throwable e) {
			Log.e(TAG, "error init application", e);
		}
		Presenter.initInstance(this, listInboxLayers);
		PreferencesManager.initInstance(this, backupPreference);
	}

	/**
	 * add object which want to be notified when onLowMemory method executes
	 *
	 * @param onLowMemoryListener
	 *            - object want to be notified
	 */
	public void addOnLowMemoryListener(OnLowMemoryListener onLowMemoryListener) {
		if (onLowMemoryListener != null) {
			mLowMemoryListeners.add(onLowMemoryListener);
		}
	}

	/**
	 * remove object from list of notified when onLowMemory method executes
	 *
	 * @param onLowMemoryListener
	 *            - remove object
	 */
	public void removeOnLowMemoryListener(OnLowMemoryListener onLowMemoryListener) {
		mLowMemoryListeners.remove(onLowMemoryListener);
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		for (OnLowMemoryListener onLowMemoryListener : mLowMemoryListeners) {
			onLowMemoryListener.onLowMemory();
		}
	}

	@Deprecated
	@Target({ ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface InitInstance {
	}

	/**
	 * Used for init objects when application starts
	 *
	 */
	public interface OnInitInstance {
		void initInstance(Context context);
	}

	/**
	 * Used for receiving low memory notification
	 *
	 */
	public interface OnLowMemoryListener {
		void onLowMemory();
	}
}
