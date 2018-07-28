/*
 * Copyright (c) 2015-2016 by Miguel Catalan Bañuls
 * Copyright (c) 2018 by k3b.
 *
 * This file is part of AndroFotoFinder / #APhotoManager.
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
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import java.lang.reflect.Field;

import de.k3b.android.androFotoFinder.R;
import de.k3b.io.StringUtils;

/**
 * This is the implementation of a Searchview based on
 * code from com.miguelcatalan.materialsearchview.MaterialSearchView
 * from https://github.com/MiguelCatalan/MaterialSearchView/
 *
 * originally created (c) 2015-2016 by Miguel Catalan Bañuls via "Apache License"
 *
 * ------
 *
 * Changes: (c) 2018 by k3b
 * * removed voice, styling, ListView with corresponding Adapter;
 * * No more dependency to AppCmpat lib
 * * Removed animation (required AppCompat)
 * * Can be used with MenuItem (even without AppCompatActivity)
 *
 *
 *     <item
 *          android:id="@+id/action_search"
 *          android:icon="@android:drawable/ic_menu_search"
 *          android:orderInCategory="100"
 *          android:title="@android:string/search_go"
 *          android:showAsAction="always|collapseActionView"
 *          android:actionViewClass="com.miguelcatalan.materialsearchview.MaterialSearchView"
 *          />
 * without embedded toolbar in client qui.
 *
 */
public class SearchViewWithHistory extends FrameLayout implements Filter.FilterListener, MenuItem.OnActionExpandListener {
    private MenuItem mMenuItem;
    private boolean mIsSearchOpen = false;
    private boolean mClearingFocus;

    //Views
    private View mSearchLayout;
    private View mTintView;
    private EditText mSearchSrcTextView;
    private ImageButton mBackBtn;
    private ImageButton mEmptyBtn;
    private View mSearchTopBar;
    private HistoryEditText mHistory;

    private CharSequence mOldQueryText;
    private CharSequence mUserQuery;

    private OnQueryTextListener mOnQueryChangeListener;
    private SearchViewListener mSearchViewListener;

    private SavedState mSavedState;
    private boolean submit = false;

    private boolean ellipsize = false;

    private Context mContext;

    public SearchViewWithHistory(Context context) {
        this(context, null);
    }

    public SearchViewWithHistory(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchViewWithHistory(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs);

        mContext = context;

        initiateView();

        initStyle(attrs, defStyleAttr);
    }

    // for debugging
    @Override
    public String toString() {
        return StringUtils.appendMessage(null,
                this.getClass().getSimpleName(),": open",
                mIsSearchOpen, mUserQuery, mSavedState).toString();
    }

    private void initStyle(AttributeSet attrs, int defStyleAttr) {
        TypedArray a = mContext.obtainStyledAttributes(attrs, R.styleable.SearchViewWithHistory, defStyleAttr, 0);

        if (a != null) {
            if (a.hasValue(R.styleable.SearchViewWithHistory_searchBackground)) {
                setBackground(a.getDrawable(R.styleable.SearchViewWithHistory_searchBackground));
            }

            if (a.hasValue(R.styleable.SearchViewWithHistory_searchCloseIcon)) {
                setCloseIcon(a.getDrawable(R.styleable.SearchViewWithHistory_searchCloseIcon));
            }

            if (a.hasValue(R.styleable.SearchViewWithHistory_searchBackIcon)) {
                setBackIcon(a.getDrawable(R.styleable.SearchViewWithHistory_searchBackIcon));
            }

            a.recycle();
        }
    }

    private void initiateView() {
        LayoutInflater.from(mContext).inflate(R.layout.search_view, this, true);
        mSearchLayout = findViewById(R.id.search_layout);

        mSearchTopBar = mSearchLayout.findViewById(R.id.search_top_bar);
        mSearchSrcTextView = (EditText) mSearchLayout.findViewById(R.id.searchTextView);
        mBackBtn = (ImageButton) mSearchLayout.findViewById(R.id.action_up_btn);
        mEmptyBtn = (ImageButton) mSearchLayout.findViewById(R.id.action_empty_btn);
        mTintView = mSearchLayout.findViewById(R.id.transparent_view);


        mSearchSrcTextView.setOnClickListener(mOnClickListener);
        mBackBtn.setOnClickListener(mOnClickListener);
        mEmptyBtn.setOnClickListener(mOnClickListener);
        mTintView.setOnClickListener(mOnClickListener);

        initSearchView();

        mHistory = new HistoryEditText(mContext, new int[] {
                R.id.action_history_btn},
                mSearchSrcTextView          );
    }

    private void initSearchView() {
        mSearchSrcTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                onSubmitQuery();
                return true;
            }
        });

        mSearchSrcTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mUserQuery = s;
                startFilter(s);
                SearchViewWithHistory.this.onTextChanged(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mSearchSrcTextView.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    showKeyboard(mSearchSrcTextView);
                }
            }
        });
    }

    /** called every time the text changes */
    private void startFilter(CharSequence s) {
        /*
        if (mAdapter != null && mAdapter instanceof Filterable) {
            ((Filterable) mAdapter).getFilter().filter(s, SearchViewWithHistory.this);
        }
        */
    }

    private final OnClickListener mOnClickListener = new OnClickListener() {

        public void onClick(View v) {
            if (v == mBackBtn) {
                closeSearch();
            } else if (v == mEmptyBtn) {
                mSearchSrcTextView.setText(null);
            } else if (v == mTintView) {
                closeSearch();
            }
        }
    };

    private void onTextChanged(CharSequence newText) {
        CharSequence text = mSearchSrcTextView.getText();
        mUserQuery = text;
        boolean hasText = !TextUtils.isEmpty(text);
        if (hasText) {
            mEmptyBtn.setVisibility(VISIBLE);
        } else {
            mEmptyBtn.setVisibility(GONE);
        }

        if (mOnQueryChangeListener != null && !TextUtils.equals(newText, mOldQueryText)) {
            mOnQueryChangeListener.onQueryTextChange(newText.toString());
        }
        mOldQueryText = newText.toString();
    }

    private void onSubmitQuery() {
        CharSequence query = mSearchSrcTextView.getText();
        if (query != null && TextUtils.getTrimmedLength(query) > 0) {
            if (mOnQueryChangeListener == null || !mOnQueryChangeListener.onQueryTextSubmit(query.toString())) {
                closeSearch();
                mSearchSrcTextView.setText(null);
            }
        }
    }

    public void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void showKeyboard(View view) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1 && view.hasFocus()) {
            view.clearFocus();
        }
        view.requestFocus();
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, 0);
    }

    //Public Attributes

    @Override
    public void setBackground(Drawable background) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mSearchTopBar.setBackground(background);
        } else {
            mSearchTopBar.setBackgroundDrawable(background);
        }
    }

    @Override
    public void setBackgroundColor(int color) {
        mSearchTopBar.setBackgroundColor(color);
    }

    public void setTextColor(int color) {
        mSearchSrcTextView.setTextColor(color);
    }

    public void setHintTextColor(int color) {
        mSearchSrcTextView.setHintTextColor(color);
    }

    public void setHint(CharSequence hint) {
        mSearchSrcTextView.setHint(hint);
    }

    public void setCloseIcon(Drawable drawable) {
        mEmptyBtn.setImageDrawable(drawable);
    }

    public void setBackIcon(Drawable drawable) {
        mBackBtn.setImageDrawable(drawable);
    }

    public void setCursorDrawable(int drawable) {
        try {
            // https://github.com/android/platform_frameworks_base/blob/kitkat-release/core/java/android/widget/TextView.java#L562-564
            Field f = TextView.class.getDeclaredField("mCursorDrawableRes");
            f.setAccessible(true);
            f.set(mSearchSrcTextView, drawable);
        } catch (Exception ignored) {
            Log.e("MaterialSearchView", ignored.toString());
        }
    }

    //Public Methods

    /**
     * Submit the query as soon as the user clicks the item.
     *
     * @param submit submit state
     */
    public void setSubmitOnClick(boolean submit) {
        this.submit = submit;
    }

    /**
     * Calling this will set the query to search text box. if submit is true, it'll submit the query.
     *
     * @param query
     * @param submit
     */
    public void setQuery(CharSequence query, boolean submit) {
        mSearchSrcTextView.setText(query);
        if (query != null) {
            mSearchSrcTextView.setSelection(mSearchSrcTextView.length());
            mUserQuery = query;
        }
        if (submit && !TextUtils.isEmpty(query)) {
            onSubmitQuery();
        }
    }

    /**
     * Call this method and pass the menu item so this class can handle click events for the Menu Item.
     *
     * @param menuItem
     */
    public void setMenuItem(final MenuItem menuItem) {
        this.mMenuItem = menuItem;
        mMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (menuItem.getActionView() != null) {
                    menuItem.expandActionView();
                }
                showSearch();
                return true;
            }
        });
    }

    /**
     * Return true if search is open
     *
     * @return
     */
    public boolean isSearchOpen() {
        return mIsSearchOpen;
    }

    /**
     * Open Search View. This will animate the showing of the view.
     */
    public void showSearch() {
        showSearch(false);
    }

    /**
     * Open Search View. If animate is true, Animate the showing of the view.
     *
     * @param animate true for animate
     */
    public void showSearch(boolean animate) {
        if (isSearchOpen()) {
            return;
        }

        //Request Focus
        mSearchSrcTextView.setText(null);
        mSearchSrcTextView.requestFocus();

        if (animate) {
            setVisibleWithAnimation();

        } else {
            mSearchLayout.setVisibility(VISIBLE);
            if (mSearchViewListener != null) {
                mSearchViewListener.onSearchViewShown();
            }
        }
        mIsSearchOpen = true;
    }

    private void setVisibleWithAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mSearchLayout.setVisibility(View.VISIBLE);
            // AnimationUtil.reveal(mSearchTopBar, animationListener);

        } else {
            // AnimationUtil.fadeInView(mSearchLayout, mAnimationDuration, animationListener);
        }
    }

    /**
     * Close search view.
     */
    public void closeSearch() {
        if (mHistory != null) mHistory.saveHistory();

        clearFocus();
        if (!isSearchOpen()) {
            return;
        }

        mIsSearchOpen = false;
        if (mSearchViewListener != null) {
            mSearchViewListener.onSearchViewClosed();
        }
        // mSearchSrcTextView.setText(null);

        if ((mMenuItem != null) && (mMenuItem.getActionView() != null)) {
            mMenuItem.collapseActionView();
        }

        mSearchLayout.setVisibility(GONE);

    }

    /**
     * Set this listener to listen to Query Change events.
     *
     * @param listener
     */
    public void setOnQueryTextListener(OnQueryTextListener listener) {
        mOnQueryChangeListener = listener;
    }

    /**
     * Set this listener to listen to Search View open and close events
     *
     * @param listener
     */
    public void setOnSearchViewListener(SearchViewListener listener) {
        mSearchViewListener = listener;
    }

    /**
     * Ellipsize suggestions longer than one line.
     *
     * @param ellipsize
     */
    public void setEllipsize(boolean ellipsize) {
        this.ellipsize = ellipsize;
    }

    @Override
    public void onFilterComplete(int count) {
        if (count > 0) {
            //showSuggestions();
        } else {
            //dismissSuggestions();
        }
    }

    @Override
    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        // Don't accept focus if in the middle of clearing focus
        if (mClearingFocus) return false;
        // Check if SearchView is focusable.
        if (!isFocusable()) return false;
        return mSearchSrcTextView.requestFocus(direction, previouslyFocusedRect);
    }

    @Override
    public void clearFocus() {
        mClearingFocus = true;
        hideKeyboard(this);
        super.clearFocus();
        mSearchSrcTextView.clearFocus();
        mClearingFocus = false;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        mSavedState = new SavedState(superState);
        mSavedState.query = mUserQuery != null ? mUserQuery.toString() : null;
        mSavedState.isSearchOpen = this.mIsSearchOpen;

        return mSavedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        mSavedState = (SavedState) state;

        if (mSavedState.isSearchOpen) {
            showSearch(false);
            setQuery(mSavedState.query, false);
        }

        super.onRestoreInstanceState(mSavedState.getSuperState());
    }

    /**
     * Called when a menu item with {@link MenuItem#SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW}
     * is expanded.
     *
     * @param item Item that was expanded
     * @return true if the item should expand, false if expansion should be suppressed.
     */
    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        return true;
    }

    /**
     * Called when a menu item with {@link MenuItem#SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW}
     * is collapsed.
     *
     * @param item Item that was collapsed
     * @return true if the item should collapse, false if collapsing should be suppressed.
     */
    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        return true;
    }

    public String getFilterValue() {
        return (mSearchSrcTextView == null) ? "" : mSearchSrcTextView.getText().toString();
    }

    static class SavedState extends BaseSavedState {
        String query;
        boolean isSearchOpen;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.query = in.readString();
            this.isSearchOpen = in.readInt() == 1;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeString(query);
            out.writeInt(isSearchOpen ? 1 : 0);
        }

        //required field that makes Parcelables from a Parcel
        public static final Creator<SavedState> CREATOR =
                new Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };

        // for debugging
        @Override
        public String toString() {
            return StringUtils.appendMessage(null,
                    this.getClass().getSimpleName(),": open",isSearchOpen, query).toString();
        }

    }

    public interface OnQueryTextListener {

        /**
         * Called when the user submits the query. This could be due to a key press on the
         * keyboard or due to pressing a submit button.
         * The listener can override the standard behavior by returning true
         * to indicate that it has handled the submit request. Otherwise return false to
         * let the SearchView handle the submission by launching any associated intent.
         *
         * @param query the query text that is to be submitted
         * @return true if the query has been handled by the listener, false to let the
         * SearchView perform the default action.
         */
        boolean onQueryTextSubmit(String query);

        /**
         * Called when the query text is changed by the user.
         *
         * @param newText the new content of the query text field.
         * @return false if the SearchView should perform the default action of showing any
         * suggestions if available, true if the action was handled by the listener.
         */
        boolean onQueryTextChange(String newText);
    }

    public interface SearchViewListener {
        void onSearchViewShown();

        void onSearchViewClosed();
    }


}