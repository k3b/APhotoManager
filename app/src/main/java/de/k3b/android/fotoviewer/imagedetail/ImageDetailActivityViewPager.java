/*******************************************************************************
 * Copyright 2011, 2012 Chris Banes.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package de.k3b.android.fotoviewer.imagedetail;

import android.app.Activity;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.k3b.android.fotoviewer.Global;
import de.k3b.android.fotoviewer.R;
import de.k3b.android.fotoviewer.queries.FotoSql;
import de.k3b.android.fotoviewer.queries.QueryParameterParcelable;

/**
 * Lock/Unlock button is added to the ActionBar.
 * Use it to temporarily disable ViewPager navigation in order to correctly interact with ImageView by gestures.
 * Lock/Unlock state of ViewPager is saved and restored on configuration changes.
 * 
 * Julia Zudikova
 */

public class ImageDetailActivityViewPager extends Activity {
    public static final String EXTRA_QUERY = "de.k3b.extras.sql";
    public static final String EXTRA_POSITION = "de.k3b.extras.position";

    // private static final String ISLOCKED_ARG = "isLocked";
	
	private ViewPager mViewPager;
    private ImagePagerAdapterFromCursor mAdapter;

    private QueryParameterParcelable mGalleryContentQuery = null;

    // for debugging
    private static int id = 1;
    private String debugPrefix;
    private DataSetObserver loadCompleteHandler;

    @Override
	public void onCreate(Bundle savedInstanceState) {
        debugPrefix = "ImageDetailActivityViewPager#" + (id++)  + " ";
        Global.debugMemory(debugPrefix, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view_pager);

        mViewPager = (LockableViewPager) findViewById(R.id.view_pager);
		setContentView(mViewPager);

        // extra parameter
        this.mGalleryContentQuery = getIntent().getParcelableExtra(EXTRA_QUERY);
        if (mGalleryContentQuery == null) {
            Log.e(Global.LOG_CONTEXT, debugPrefix + " onCreate() : intent.extras[" + EXTRA_QUERY +
                        "] not found. Using default.");
            mGalleryContentQuery = FotoSql.getQuery(FotoSql.QUERY_TYPE_DEFAULT);
        } else if (Global.debugEnabled) {
            Log.e(Global.LOG_CONTEXT, debugPrefix + " onCreate() : query = " + mGalleryContentQuery);
        }

        mAdapter = new ImagePagerAdapterFromCursor(this, mGalleryContentQuery, debugPrefix);
        loadCompleteHandler = new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                onLoadCompleted();
            }
        };
        mAdapter.registerDataSetObserver(loadCompleteHandler);
        mViewPager.setAdapter(mAdapter);
		
		if (savedInstanceState != null) {
			// boolean isLocked = savedInstanceState.getBoolean(ISLOCKED_ARG, false);
			// ((HackyViewPager) mViewPager).setLocked(isLocked);
		}
	}

/* these doe not work yet (tested with for android 4.0)
    manifest         <activity ... android:ellipsize="middle" />

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        // http://stackoverflow.com/questions/10779037/set-activity-title-ellipse-to-middle
        final int actionBarTitle = android.R.id.title; //  Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
        final TextView titleView = (TextView)  this.getWindow().findViewById(actionBarTitle);
        if ( titleView != null ) {
            titleView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        }
    }
*/

    @Override
    protected void onPause () {
        Global.debugMemory(debugPrefix, "onPause");
        super.onPause();
    }

    @Override
    protected void onResume () {
        Global.debugMemory(debugPrefix, "onResume");
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        Global.debugMemory(debugPrefix, "onDestroy");
        mAdapter.unregisterDataSetObserver(loadCompleteHandler);
        loadCompleteHandler = null;
        mViewPager.setAdapter(null);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.viewpager_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void onLoadCompleted() {
        int mInitialPosition = getIntent().getIntExtra(EXTRA_POSITION, 0);
        mViewPager.setCurrentItem(mInitialPosition);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }
    
    private void toggleViewPagerScrolling() {
    	if (isViewPagerActive()) {
    		((LockableViewPager) mViewPager).toggleLock();
    	}
    }
    
    private boolean isViewPagerActive() {
    	return (mViewPager != null && mViewPager instanceof LockableViewPager);
    }

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (isViewPagerActive()) {
			// outState.putBoolean(ISLOCKED_ARG, ((HackyViewPager) mViewPager).isLocked());
    	}
		super.onSaveInstanceState(outState);
	}

}
