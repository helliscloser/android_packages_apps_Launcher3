/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.launcher3.icons;

import android.content.Context;
import android.content.res.Resources;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.XmlResourceParser;
import android.os.Build;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.android.launcher3.R;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.util.Themes;

import org.xmlpull.v1.XmlPullParser;

import java.util.Collections;
import java.util.Map;

/**
 * Extension of {@link IconProvider} with support for overriding theme icons
 */
public class LauncherIconProvider extends IconProvider {

    private static final String TAG_ICON = "icon";
    private static final String ATTR_PACKAGE = "package";
    private static final String ATTR_DRAWABLE = "drawable";

    private static final String TAG = "LIconProvider";
    private static final Map<String, ThemeData> DISABLED_MAP = Collections.emptyMap();

    private Map<String, ThemeData> mThemedIconMap;
    private boolean mSupportsIconTheme;
    
    protected final Context mContext;

    public LauncherIconProvider(Context context) {
        super(context);
        mContext = context;
        setIconThemeSupported(Themes.isThemedIconEnabled(mContext));
    }

    /**
     * Enables or disables icon theme support
     */
    public void setIconThemeSupported(boolean isSupported) {
        mSupportsIconTheme = isSupported;
        buildThemedIconMap();
    }

    @Override
    protected ThemeData getThemeDataForPackage(String packageName, String themedIconPack) {
        return getThemedIconMap(themedIconPack).get(packageName);
    }

    @Override
    public String getSystemIconState() {
        return super.getSystemIconState() + (mSupportsIconTheme ? ",with-theme" : ",no-theme")
                + "," + Build.VERSION.INCREMENTAL;
    }

    private Map<String, ThemeData> getThemedIconMap(String themedIconPack) {
        if (mThemedIconMap != null) {
            return mThemedIconMap;
        }
        ArrayMap<String, ThemeData> map = new ArrayMap<>();
        boolean themedIconPackAvailable = false;
        try {
            Resources res = mContext.getResources();
            if (themedIconPack != null) {
                try {
                    res = mContext.getPackageManager().getResourcesForApplication(themedIconPack);
                    themedIconPackAvailable = true;
                } catch(NameNotFoundException e) {
                    Log.e(TAG, "Themed icon pack " + themedIconPack + " does not exist!");
                }
            }
            int resID = res.getIdentifier("grayscale_icon_map", "xml",
                themedIconPackAvailable ? themedIconPack : mContext.getPackageName());
            if (resID != 0) {
                XmlResourceParser parser = res.getXml(resID);
            final int depth = parser.getDepth();
            int type;
            while ((type = parser.next()) != XmlPullParser.START_TAG
                    && type != XmlPullParser.END_DOCUMENT);

            while (((type = parser.next()) != XmlPullParser.END_TAG
                    || parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
                if (type != XmlPullParser.START_TAG) {
                    continue;
                }
                if (TAG_ICON.equals(parser.getName())) {
                    String pkg = parser.getAttributeValue(null, ATTR_PACKAGE);
                    int iconId = parser.getAttributeResourceValue(null, ATTR_DRAWABLE, 0);
                    if (iconId != 0 && !TextUtils.isEmpty(pkg)) {
                        map.put(pkg, new ThemeData(res, iconId));
                    }
                 }
              }
            } else if (themedIconPackAvailable) {
                Log.e(TAG, "Icon map does not exist in " + themedIconPack);
            }
            mThemedIconMap = map;
        } catch (Exception e) {
            mThemedIconMap = DISABLED_MAP;
            Log.e(TAG, "Unable to parse icon map", e);
        }
    }

    private Map<String, ThemeData> getThemedIconMap() {
        if (mThemedIconMap == null) {
            buildThemedIconMap();
        }
        return mThemedIconMap;
    }
}
