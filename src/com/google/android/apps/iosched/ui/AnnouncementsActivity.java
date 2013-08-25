/*
 * Copyright 2013 Google Inc.
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

package com.google.android.apps.iosched.ui;

import com.gdgdevfest.android.apps.devfestbcn.R;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

public class AnnouncementsActivity extends SimpleSinglePaneActivity {
    @Override
    protected Fragment onCreatePane() {
        setIntent(getIntent().putExtra(AnnouncementsFragment.EXTRA_ADD_VERTICAL_MARGINS, true));
        return new AnnouncementsFragment();
    }

    @Override
    protected int getContentViewResId() {
        return R.layout.activity_plus_stream;
    }
}
