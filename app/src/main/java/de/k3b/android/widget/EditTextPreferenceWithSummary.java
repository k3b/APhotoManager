/*
 * Copyright (C) 2014 k3b
 * 
 * This file is part of de.k3b.android.toGoZip (https://github.com/k3b/ToGoZip/) .
 * 
 * This program is free software: you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version. 
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details. 
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program. If not, see <http://www.gnu.org/licenses/>
 */
package de.k3b.android.widget;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

/**
 * EditTextPreference that displays it's string-value in the Preference-summary line.
 */
public class EditTextPreferenceWithSummary extends EditTextPreference {
    private final static String TAG = EditTextPreferenceWithSummary.class
            .getName();

    public EditTextPreferenceWithSummary(android.content.Context context, android.util.AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.init();
    }

    public EditTextPreferenceWithSummary(final Context context,
                                         final AttributeSet attrs) {
        super(context, attrs);
        this.init();
    }

    public EditTextPreferenceWithSummary(final Context context) {
        super(context);
        this.init();
    }

    private void init() {
        Log.i(EditTextPreferenceWithSummary.TAG, "init");
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this.getContext());
        Object value = prefs.getAll().get(this.getKey());

        this.setSummary((value != null) ? value.toString() : "");

        this.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(final Preference preference,
                                              final Object newValue) {
                Log.i(EditTextPreferenceWithSummary.TAG,
                        "display score changed to " + newValue);
                preference.setSummary(newValue.toString()); // getSummary());
                return true;
            }
        });
    }

    @Override
    public CharSequence getSummary() {
        return super.getSummary();
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        this.setSummary(this.getText());
        return super.onCreateView(parent);
    }
}
