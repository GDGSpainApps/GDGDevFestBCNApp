/*
 * Copyright 2012 Google Inc.
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

package com.gdgdevfest.android.apps.devfestbcn.io;

import static com.gdgdevfest.android.apps.devfestbcn.util.LogUtils.makeLogTag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import android.content.ContentProviderOperation;
import android.content.Context;

import com.gdgdevfest.android.apps.devfestbcn.io.map.model.MapConfig;
import com.gdgdevfest.android.apps.devfestbcn.io.map.model.MapResponse;
import com.gdgdevfest.android.apps.devfestbcn.io.map.model.Marker;
import com.gdgdevfest.android.apps.devfestbcn.io.map.model.Tile;
import com.gdgdevfest.android.apps.devfestbcn.provider.ScheduleContract;
import com.gdgdevfest.android.apps.devfestbcn.util.Lists;
import com.gdgdevfest.android.apps.devfestbcn.util.MapUtils;
import com.google.gson.Gson;

public class MapPropertyHandler extends JSONHandler {
    private static final String TAG = makeLogTag(MapPropertyHandler.class);

    private Collection<Tile> mTiles;

    public MapPropertyHandler(Context context) {
        super(context);
    }

    public ArrayList<ContentProviderOperation> parse(String json)
            throws IOException {
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
        MapResponse mapJson = new Gson().fromJson(json, MapResponse.class);
        parseTileOverlays(mapJson.tiles, batch, mContext);
        parseMarkers(mapJson.markers, batch);
        parseConfig(mapJson.config, mContext);
     //   mTiles = mapJson.tiles.values();
        return batch;
    }

    private void parseConfig(MapConfig config, Context mContext) {
        boolean enableMyLocation = config.enableMyLocation;
        MapUtils.setMyLocationEnabled(mContext,enableMyLocation);
    }

    private void parseMarkers(Map<String, Marker[]> markers,
            ArrayList<ContentProviderOperation> batch) {

        for (Entry<String, Marker[]> entry : markers.entrySet()) {

            String floor = entry.getKey();

            // add each Marker
            for (Marker marker : entry.getValue()) {
                ContentProviderOperation.Builder builder = ContentProviderOperation
                        .newInsert(ScheduleContract
                                .addCallerIsSyncAdapterParameter(ScheduleContract.MapMarkers.CONTENT_URI));

                builder.withValue(ScheduleContract.MapMarkers.MARKER_ID, marker.id);
                builder.withValue(ScheduleContract.MapMarkers.MARKER_FLOOR, floor);
                builder.withValue(ScheduleContract.MapMarkers.MARKER_LABEL,
                        marker.title);
                builder.withValue(ScheduleContract.MapMarkers.MARKER_LATITUDE,
                        marker.lat);
                builder.withValue(ScheduleContract.MapMarkers.MARKER_LONGITUDE,
                        marker.lng);
                builder.withValue(ScheduleContract.MapMarkers.MARKER_TYPE,
                        marker.type);
                builder.withValue(ScheduleContract.MapMarkers.MARKER_TRACK, marker.track);

                batch.add(builder.build());
            }
        }

    }

    private void parseTileOverlays(Map<String, Tile> tiles,
            ArrayList<ContentProviderOperation> batch, Context context) {

    }

    public Collection<Tile> getTiles(){
        return mTiles;
    }
}
