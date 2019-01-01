/*
 * Radio - Internet radio for android
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

package org.oucho.radio2.update;

import android.content.Context;
import android.util.Log;

import org.oucho.radio2.radio.RadioKeys;

public class UpdateChecker implements RadioKeys {


    public static void checkForDialog(Context context) {
        if (context != null) {
            new CheckUpdateTask(context, TYPE_DIALOG, true).execute();
        } else {
            Log.e(UPDATE_TAG, "The arg context is null");
        }
    }

    public static void checkForSnack(Context context) {
        if (context != null) {
            new CheckUpdateTask(context, TYPE_SNACK, false).execute();
        } else {
            Log.e(UPDATE_TAG, "The arg context is null");
        }

    }


}
