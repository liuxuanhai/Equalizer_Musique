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