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

package com.gdgdevfest.android.apps.devfestbcn.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.gdgdevfest.android.apps.devfestbcn.R;
import com.gdgdevfest.android.apps.devfestbcn.provider.ScheduleContract;
import com.gdgdevfest.android.apps.devfestbcn.util.BeamUtils;

public class SessionFeedbackActivity extends SimpleSinglePaneActivity implements
        TrackInfoHelperFragment.Callbacks {

    private String mSessionId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Uri sessionUri = getIntent().getData();
            BeamUtils.setBeamSessionUri(this, sessionUri);
            getSupportFragmentManager().beginTransaction()
                    .add(TrackInfoHelperFragment.newFromSessionUri(sessionUri),
                            "track_info")
                    .commit();
        }

        mSessionId = ScheduleContract.Sessions.getSessionId(getIntent().getData());
    }

    @Override
    protected int getContentViewResId() {
        return R.layout.activity_letterboxed_when_large;
    }

    @Override
    protected Fragment onCreatePane() {
        return new SessionFeedbackFragment();
    }

    @Override
    public Intent getParentActivityIntent() {
        // Up to this session's track details, or Home if no track is available
        if (mSessionId != null) {
            return new Intent(Intent.ACTION_VIEW,
                    ScheduleContract.Sessions.buildSessionUri(mSessionId));
        } else {
            return new Intent(this, HomeActivity.class);
        }
    }

    @Override
    public void onTrackInfoAvailable(String trackId, TrackInfo track) {
        setTitle(track.name);
        setActionBarTrackIcon(track.name, track.color);
    }
}
