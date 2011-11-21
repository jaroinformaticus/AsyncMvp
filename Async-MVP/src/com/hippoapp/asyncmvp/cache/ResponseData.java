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
package com.hippoapp.asyncmvp.cache;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Entity to help to cache http responses. Contains status code of http response
 * and response body in binary data
 *
 * @author Bnet.Android.Developer.Team
 *
 */
public class ResponseData implements Parcelable {
	public ResponseData(int statusCode, byte[] responseBody) {
		this.statusCode = statusCode;
		this.responseBody = responseBody;
	}

	private int statusCode;
	private byte[] responseBody;

	public int getStatusCode() {
		return statusCode;
	}

	public byte[] getResponseBody() {
		return responseBody;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeInt(statusCode);
		parcel.writeString(new String(responseBody));
	}

	public static final Parcelable.Creator<ResponseData> CREATOR = new Parcelable.Creator<ResponseData>() {
		@Override
		public ResponseData createFromParcel(Parcel source) {
			return new ResponseData(source.readInt(), source.readString().getBytes());
		}

		@Override
		public ResponseData[] newArray(int size) {
			return new ResponseData[size];
		}
	};
}
