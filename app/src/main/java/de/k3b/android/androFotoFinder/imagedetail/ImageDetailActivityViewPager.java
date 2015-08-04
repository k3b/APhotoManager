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
package de.k3b.android.androFotoFinder.imagedetail;

import android.app.Activity;
import android.content.Intent;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

// import com.squareup.leakcanary.RefWatcher;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.queries.QueryParameterParcelable;

/**
 * Lock/Unlock button is added to the ActionBar.
 * Use it to temporarily disable ViewPager navigation in order to correctly interact with ImageView by gestures.
 * Lock/Unlock state of ViewPager is saved and restored on configuration changes.
 * 
 * Julia Zudikova
 */

public class ImageDetailActivityViewPager extends Activity {
    private static final String INSTANCE_STATE_LAST_SCROLL_POSITION = "lastScrollPosition";
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
    private int mInitialPosition;

    public static void showActivity(Activity context, Uri imageUri, int position, QueryParameterParcelable imageDetailQuery) {
        Intent intent;
        //Create intent
        intent = new Intent(context, ImageDetailActivityViewPager.class);

        intent.putExtra(ImageDetailActivityViewPager.EXTRA_QUERY, imageDetailQuery);
        intent.putExtra(ImageDetailActivityViewPager.EXTRA_POSITION, position);
        intent.setData(imageUri);

        context.startActivity(intent);
    }


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

        this.mInitialPosition = getIntent().getIntExtra(EXTRA_POSITION, this.mInitialPosition);
        if (savedInstanceState != null) {
            mInitialPosition = savedInstanceState.getInt(INSTANCE_STATE_LAST_SCROLL_POSITION, this.mInitialPosition);
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
        // RefWatcher refWatcher = AndroFotoFinderApp.getRefWatcher(this);
        // refWatcher.watch(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_image_detail, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void onLoadCompleted() {
        mViewPager.setCurrentItem(mInitialPosition);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_details:
                showDetails();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void showDetails() {
        int itemPosition = mViewPager.getCurrentItem();
        String fullFilePath = this.mAdapter.getFullFilePath(itemPosition);
        ImageDetailDialogBuilder.createImageDetailDialog(this, fullFilePath).show();
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
            outState.putInt(INSTANCE_STATE_LAST_SCROLL_POSITION, mViewPager.getCurrentItem());
    	}
		super.onSaveInstanceState(outState);
	}

}
