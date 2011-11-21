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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.hippoapp.asyncmvp.utils.AsyncMvpPresenterProtocol;

/**
 * Presenter is the main component of MVP template and Message-based
 * architecture. The logic of the Presenter is based on
 * <ol>
 * <li>HandlerThread
 * <li>Handler
 * <li>Message
 * </ol>
 * Using those classes Android OS gives us a flexible interactions between
 * <code>View</code> and <code>Model</code> in MVP template.
 *
 * Presenter contains <code>mViewHandler</code> and <code>mModelHandler</code>
 * lists which allow <code>View</code> and <code>Model</code> in MVP template to
 * send messages to each other. <code>mModelHandler</code> elements are
 * registered during application startup process. You should implement
 * {@link Callback} interface and register it using
 * {@link Presenter#subscribe(Callback)} method to register elements of
 * ViewHandler. To unregister them use {@link Presenter#unsubscribe(Callback)}
 * method. Information is transfered between code>View</code> and
 * <code>Model</code> using messages. Those fields are used to interact:
 * <ul>
 * <li>{@link Message#what} - message protocol
 * <li>{@link Message#arg1} - service data #1
 * <li>{@link Message#arg2} - service data #2
 * <li>{@link Message#obj} - message object
 * <li>{@link Bundle} - primitives container for an additional information. To
 * append it to the message use {@link Message#setData(Bundle)} method
 * </ul>
 * Messages are processed within handleMessage methods with corresponding
 * protocol values. It is important to execute MVP <code>Views</code> and
 * <code>Models</code> in different threads because sometimes data processing is
 * taking lots of time. That is why all modelHandler’s are registered on a new
 * separate {@link HandlerThread}. You can get each request’s state via
 * {@link AsyncMvpPresenterProtocol#GET_STATUS} protocol, sending needed layer
 * identifier as {@link Message#arg1}. Identifier is a number annotated as
 * {@link ModelLayer} A message with this protocol should be sent to a needed
 * layer. Answer is received via {@link AsyncMvpPresenterProtocol#PUT_STATUS}
 * protocol with {@link LayerStatus} object, which contains a list of currently
 * executing layer protocols.
 *
 * @author Bnet.Android.Developer.Team
 *
 */
public final class Presenter implements Callback {

	public static final String TAG = Presenter.class.getSimpleName();

	private ModelHandler[] mModelHandlers;

	private int mSizeInboxHandlers;

	private List<Handler> mViewHandlers;

	private HashMap<Callback, Handler> mHandlerMap;

	private static volatile Presenter sInstance;

	private Context mContext;

	private Handler mThisHandler;
	private HandlerThread mThisHandlerThread;

	public static final void initInstance(Context context, List<ModelLayerInterface> listInbox) {
		sInstance = new Presenter(context, listInbox);
	}

	public static final Presenter getInst() {
		return sInstance;
	}

	public Context getApplicationContext() {
		return mContext;
	}

	private Presenter(Context context, List<ModelLayerInterface> listInbox) {
		mContext = context;

		mHandlerMap = new HashMap<Callback, Handler>();
		mViewHandlers = new ArrayList<Handler>();

		mSizeInboxHandlers = listInbox.size();
		mModelHandlers = new ModelHandler[mSizeInboxHandlers];
		for (int i = 0; i < mSizeInboxHandlers; ++i) {
			HandlerThread handlerThread = new HandlerThread(listInbox.get(i).getClass().getName());
			handlerThread.start();
			mModelHandlers[i] = new ModelHandler(handlerThread.getLooper(), listInbox.get(i));
		}

		mThisHandlerThread = new HandlerThread(getClass().getCanonicalName());
		mThisHandlerThread.start();
		mThisHandler = new Handler(mThisHandlerThread.getLooper(), this);
	}

	/**
	 * Add View-component in <code>mViewHandler</code> for receiving messages
	 *
	 * @param viewComponentCallback
	 *            - View-component, which realized {@link Callback}.
	 */
	public final void subscribe(Callback viewComponentCallback) {
		Handler handler = new Handler(viewComponentCallback);
		mHandlerMap.put(viewComponentCallback, handler);
		mViewHandlers.add(handler);
	}

	/**
	 * Remove View-component from <code>mViewHandler</code>.
	 *
	 * @param viewComponentallback
	 *            - View-component, which realized {@link Callback}.
	 */
	public final void unsubscribe(Callback viewComponentallback) {
		List<Handler> newList = new ArrayList<Handler>(mViewHandlers);
		newList.remove(mHandlerMap.get(viewComponentallback));
		mHandlerMap.remove(viewComponentallback);
		mViewHandlers = newList;
	}

	/**
	 * Send empty message with {@link Message#arg1}=0, {@link Message#arg2}=0 to
	 * View-components. View-components receive message consequentially.
	 *
	 * @param what
	 *            - protocol, from {@link AsyncMvpPresenterProtocol} or class
	 *            extended it.
	 */
	public final void sendViewMessage(int what) {
		sendViewMessage(what, 0, 0, null, null);
	}

	/**
	 * Send message with {@link Message#arg1}=0, {@link Message#arg2}=0 to
	 * View-components. View-components receive message consequentially.
	 *
	 * @param what
	 *            - protocol, from {@link AsyncMvpPresenterProtocol} or class
	 *            extended it.
	 * @param obj
	 *            - {@link Message#obj}
	 */
	public final void sendViewMessage(int what, Object obj) {
		sendViewMessage(what, 0, 0, obj, null);
	}

	/**
	 * Send message to View-components. View-components receive message
	 * consequentially.
	 *
	 * @param what
	 *            - protocol, from {@link AsyncMvpPresenterProtocol} or class
	 *            extended it.
	 * @param arg1
	 *            - {@link Message#arg1}
	 * @param arg2
	 *            - {@link Message#arg2}
	 * @param obj
	 *            - {@link Message#obj}
	 */
	public final void sendViewMessage(int what, int arg1, int arg2, Object obj) {
		sendViewMessage(what, arg1, arg2, obj, null);
	}

	/**
	 * Send message to View-components. View-components receive message
	 * consequentially.
	 *
	 * @param what
	 *            - protocol, from {@link AsyncMvpPresenterProtocol} or class
	 *            extended it.
	 * @param arg1
	 *            - {@link Message#arg1}
	 * @param arg2
	 *            - {@link Message#arg2}
	 * @param obj
	 *            - {@link Message#obj}
	 * @param bundle
	 *            - container with primitive types for additional info
	 */
	public final void sendViewMessage(int what, int arg1, int arg2, Object obj, Bundle bundle) {
		List<Handler> outBoxList = mViewHandlers;
		for (Handler handler : outBoxList) {
			sendMessageToTarget(handler, what, arg1, arg2, obj, bundle);
		}
	}

	/**
	 * Send empty message with {@link Message#arg1}=0, {@link Message#arg2}=0 to
	 * Model-components. Model-components receive message consequentially or
	 * parallel.
	 *
	 * @param what
	 *            - protocol, from {@link AsyncMvpPresenterProtocol} or class
	 *            extended it.
	 */
	public final void sendModelMessage(int what) {
		sendModelMessage(what, 0, 0, null, null);
	}

	/**
	 * Send message with {@link Message#arg1}=0, {@link Message#arg2}=0 to
	 * Model-components. Model-components receive message consequentially or
	 * parallel.
	 *
	 * @param what
	 *            - protocol, from {@link AsyncMvpPresenterProtocol} or class
	 *            extended it.
	 * @param obj
	 *            - {@link Message#obj}
	 */
	public final void sendModelMessage(int what, Object obj) {
		sendModelMessage(what, 0, 0, obj, null);
	}

	/**
	 * Send message with {@link Message#arg1}=0, {@link Message#arg2}=0 to
	 * Model-components. Model-components receive message consequentially or
	 * parallel.
	 *
	 * @param what
	 *            - protocol, from {@link AsyncMvpPresenterProtocol} or class
	 *            extended it.
	 * @param arg1
	 *            - {@link Message#arg1}
	 * @param arg2
	 *            - {@link Message#arg2}
	 * @param obj
	 *            - {@link Message#obj}
	 */
	public final void sendModelMessage(int what, int arg1, int arg2, Object obj) {
		sendModelMessage(what, arg1, arg2, obj, null);
	}

	/**
	 * Send message with {@link Message#arg1}=0, {@link Message#arg2}=0 to
	 * Model-components. Model-components receive message consequentially or
	 * parallel.
	 *
	 * @param what
	 *            - protocol, from {@link AsyncMvpPresenterProtocol} or class
	 *            extended it.
	 * @param arg1
	 *            - {@link Message#arg1}
	 * @param arg2
	 *            - {@link Message#arg2}
	 * @param obj
	 *            - {@link Message#obj}
	 * @param bundle
	 *            - container with primitive types for additional info
	 */
	public final void sendModelMessage(int what, int arg1, int arg2, Object obj, Bundle bundle) {
		for (Handler handler : mModelHandlers) {
			sendMessageToTarget(handler, what, arg1, arg2, obj, bundle);
		}
		Message.obtain(mThisHandler, what, arg1, arg2, obj).sendToTarget();
	}

	private void sendMessageToTarget(Handler handler, int what, int arg1, int arg2, Object obj, Bundle bundle) {
		Message message = Message.obtain(handler, what, arg1, arg2, obj);
		message.setData(bundle);
		message.sendToTarget();
	}

	public final void dispose() {
		for (Handler handler : mModelHandlers) {
			handler.getLooper().quit();
		}
	}

	@Override
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
		case AsyncMvpPresenterProtocol.GET_STATUS: {
			int name = msg.arg1;
			for (ModelHandler modelHandler : mModelHandlers) {
				if (modelHandler.name != 0 && modelHandler.name == name) {
					sendViewMessage(AsyncMvpPresenterProtocol.PUT_STATUS, name, 0, modelHandler.inboxLayerInterface.getStatus());
					sendModelMessage(AsyncMvpPresenterProtocol.PUT_STATUS, name, 0, modelHandler.inboxLayerInterface.getStatus());
				}
			}
			return true;
		}
		}
		return false;
	}

	public interface ModelLayerInterface extends Handler.Callback {
		public void init(Context context);

		public LayerStatus getStatus();
	}

	@Target({ ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface ModelLayer {
		int nameInt() default 0;
	}

	public class LayerStatus {
		HashSet<Integer> activeProtocolSet = new HashSet<Integer>();
	}

	private class ModelHandler extends Handler {
		int name;

		ModelLayerInterface inboxLayerInterface;

		public ModelHandler(Looper looper, ModelLayerInterface modelLayerInterface) {
			super(looper, modelLayerInterface);
			name = modelLayerInterface.getClass().getAnnotation(ModelLayer.class).nameInt();
			this.inboxLayerInterface = modelLayerInterface;
		}
	}
}
