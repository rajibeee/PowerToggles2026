package com.painless.pc.nav;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import com.painless.pc.R;

/**
 * Fragment displaying the 2026 changelog — what works, what opens settings,
 * and what is disabled on modern Android.
 */
public class ChangelogFrag extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.changelog);
    }
}

