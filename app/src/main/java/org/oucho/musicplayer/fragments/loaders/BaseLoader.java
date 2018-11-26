/*
 * Musique - Music player/converter for android
 * Copyright (C) 2017  Old-Geek
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.oucho.musicplayer.fragments.loaders;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.content.AsyncTaskLoader;

import org.oucho.musicplayer.utils.Permissions;


abstract public class BaseLoader<D> extends AsyncTaskLoader<D> {

    private D mData;

    private String mFilter;

    private String mSelectionString;
    private String[] mSelectionArgs;
    private String mSortOrder = null;

    BaseLoader(Context context) {
        super(context);
    }

    @Override
    protected void onStartLoading() {
        if (mData != null) {
            deliverResult(mData);
        }
        if (takeContentChanged() || mData == null) {
            forceLoad();
        }
    }

    String getFilter() {
        return mFilter;
    }

    public void setFilter(String filter) {
        mFilter = filter;
    }

    @Override
    protected void onReset() {
        super.onReset();
        mData = null;
        onStopLoading();
    }

    @Override
    protected void onStopLoading() {
        super.onStopLoading();
        cancelLoad();
    }

    @Override
    public void deliverResult(D data) {
        if (!isReset()) {
            super.deliverResult(data);
        }
    }



    public void setSelection(String selectionString, String[] selectionArgs) {

        mSelectionString = selectionString;
        setSelectionArgs(selectionArgs);
    }

    private void setSelectionArgs (String[] value) {
        mSelectionArgs = value;
    }

    String getSelectionString() {
        return mSelectionString;

    }

    String[] getSelectionArgs() {
        return mSelectionArgs;
    }

    @Nullable
    private Cursor getCursor(Uri musicUri, String[] projection, String selection, String[] selectionArgs, String filteredFieldName, String filter, String orderBy) {
        if (!Permissions.checkPermission(getContext())) {
            return null;
        }


        if (filter != null) {
            if ("".equals(filter)) {
                return null; // empty filter means that we don't want any result
            }
            selection = DatabaseUtils.concatenateWhere(selection, filteredFieldName + " LIKE ?");
            selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{"%" + filter + "%"});

        }


        return getContext().getContentResolver().query(musicUri, projection, selection, selectionArgs, orderBy);
    }

    @Nullable
    Cursor getCursor(Uri musicUri, String[] projection, String selection, String[] selectionArgs, String filteredFieldName, String filter) {
        return getCursor(musicUri, projection, selection, selectionArgs, filteredFieldName, filter, mSortOrder);
    }


    public void setSortOrder(String orderBy) {
        mSortOrder = orderBy;
    }


}