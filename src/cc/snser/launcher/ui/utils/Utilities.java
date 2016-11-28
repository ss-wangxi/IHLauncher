/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.snser.launcher.ui.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.Display;
import cc.snser.launcher.App;
import cc.snser.launcher.Utils;


/**
 * Various utilities shared amongst the Launcher's classes.
 */
public final class Utilities {
    private static final String TAG = "Launcher.Utilities";
    private static DisplayMetrics mDisplayMetrics = new DisplayMetrics();

    public static Drawable getDrawableDefault(Context context, String resName, boolean autofit) {
        int resId = context.getResources().getIdentifier(resName, "drawable",
                context.getPackageName());

        if (resId != 0) {
            return Utils.getDrawableFromResources(context, context.getResources(), resId, autofit);
        }

        return null;
    }

    public static Drawable getDrawableDefault(Context context, int resId, boolean autofit) {
        return Utils.getDrawableFromResources(context, context.getResources(), resId, autofit);
    }

    /**
    * 根据手机的分辨率从 dip 的单位 转成为 px(像素)
    */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static DisplayMetrics getDisplayMetrics(){
    	Display display = App.getApp().getLauncher().getWindowManager().getDefaultDisplay();
    	display.getMetrics(mDisplayMetrics);
    	return mDisplayMetrics;
    }
}
