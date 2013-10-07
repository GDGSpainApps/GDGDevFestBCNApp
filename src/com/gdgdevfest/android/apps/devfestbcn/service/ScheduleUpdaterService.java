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

package com.gdgdevfest.android.apps.devfestbcn.service;

import static com.gdgdevfest.android.apps.devfestbcn.util.LogUtils.LOGE;
import static com.gdgdevfest.android.apps.devfestbcn.util.LogUtils.LOGI;
import static com.gdgdevfest.android.apps.devfestbcn.util.LogUtils.makeLogTag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import com.gdgdevfest.android.apps.devfestbcn.sync.SyncHelper;

/**
 * Background {@link android.app.Service} that adds or removes sessions from your calendar via the
 * Conference API.
 *
 * @see com.gdgdevfest.android.apps.devfestbcn.sync.SyncHelper
 */
public class ScheduleUpdaterService extends Service {
    private static final String TAG = makeLogTag(ScheduleUpdaterService.class);

    public static final String EXTRA_SESSION_ID
            = "com.gdgdevfest.android.apps.devfestbcn.extra.SESSION_ID";
    public static final String EXTRA_IN_SCHEDULE
            = "com.gdgdevfest.android.apps.devfestbcn.extra.IN_SCHEDULE";

    private static final int SCHEDULE_UPDATE_DELAY_MILLIS = 5000;

    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;
    private final LinkedList<Intent> mScheduleUpdates = new LinkedList<Intent>();

    // Handler pattern copied from IntentService
    private final class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            processPendingScheduleUpdates();

            int numRemainingUpdates;
            synchronized (mScheduleUpdates) {
                numRemainingUpdates = mScheduleUpdates.size();
            }

            if (numRemainingUpdates == 0) {
                stopSelf();
            } else {
                // More updates were added since the current pending set was processed. Reschedule
                // another pass.
                removeMessages(0);
                sendEmptyMessageDelayed(0, SCHEDULE_UPDATE_DELAY_MILLIS);
            }
        }
    }

//    private static class NotifyGcmDevicesTask extends AsyncTask<String, Void, Void> {
//
//        @Override
//        protected Void doInBackground(String... params) {
//            BasicHttp
//            String gPlusID = params[0];
//            Uri uri = new Uri(Config.GCM_SERVER_URL + "/send/" + gPlusID + "/sync_user");
//            connection = (HttpURLConnection) url.openConnection();
//            connection.setDoOutput(true);
//            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//            connection.setRequestMethod("POST");
//
//            request = new OutputStreamWriter(connection.getOutputStream());
//            request.write(parameters);
//            request.flush();
//            request.close();
//
//        }
//    }

   
    public ScheduleUpdaterService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread(ScheduleUpdaterService.class.getSimpleName());
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // When receiving a new intent, delay the schedule until 5 seconds from now.
        mServiceHandler.removeMessages(0);
        mServiceHandler.sendEmptyMessageDelayed(0, SCHEDULE_UPDATE_DELAY_MILLIS);

        // Remove pending updates involving this session ID.
        String sessionId = intent.getStringExtra(EXTRA_SESSION_ID);
        Iterator<Intent> updatesIterator = mScheduleUpdates.iterator();
        while (updatesIterator.hasNext()) {
            Intent existingIntent = updatesIterator.next();
            if (sessionId.equals(existingIntent.getStringExtra(EXTRA_SESSION_ID))) {
                updatesIterator.remove();
            }
        }

        // Queue this schedule update.
        synchronized (mScheduleUpdates) {
            mScheduleUpdates.add(intent);
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        mServiceLooper.quit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    void processPendingScheduleUpdates() {
        try {
            // Operate on a local copy of the schedule update list so as not to block
            // the main thread adding to this list
            List<Intent> scheduleUpdates = new ArrayList<Intent>();
            synchronized (mScheduleUpdates) {
                scheduleUpdates.addAll(mScheduleUpdates);
                mScheduleUpdates.clear();
            }

            SyncHelper syncHelper = new SyncHelper(this);
            for (Intent updateIntent : scheduleUpdates) {
                String sessionId = updateIntent.getStringExtra(EXTRA_SESSION_ID);
                boolean inSchedule = updateIntent.getBooleanExtra(EXTRA_IN_SCHEDULE, false);
                LOGI(TAG, "addOrRemoveSessionFromSchedule:"
                        + " sessionId=" + sessionId
                        + " inSchedule=" + inSchedule);
                syncHelper.addOrRemoveSessionFromSchedule(this, sessionId, inSchedule);
            }
        } catch (IOException e) {
            // TODO: do something useful here, like revert the changes locally in the
            // content provider to maintain client/server sync
            LOGE(TAG, "Error processing schedule update", e);
        }
    }
}
