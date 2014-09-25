package com.anypresence.library;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/**
 * A view with a pop-up with input for a date. Reduces the hassle of messing
 * with date pickers.
 * */
public class AdaptableLinearLayout extends LinearLayout {
    private BaseAdapter mAdapter;
    private AdapterView.OnItemClickListener mItemClickListener;
    private View mHeader;
    private View mFooter;
    private DataSetObserver mObserver;
    private boolean mSwipeToDismissEnabled = false;
    private OnDismissedListener mOnDismissedListener;

    public AdaptableLinearLayout(Context context) {
        super(context);
        setUp();
    }

    public AdaptableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setUp();
    }

    private void setUp() {
        setOrientation(VERTICAL);
    }

    public void setAdapter(BaseAdapter adapter) {
        if(mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mObserver);
        }
        mAdapter = adapter;
        refresh();
        if(adapter != null) {
            mObserver = new DataSetObserver() {
                @Override
                public void onChanged() {
                    super.onChanged();
                    refresh();
                }
            };
            mAdapter.registerDataSetObserver(mObserver);
        }
    }

    private void refresh() {
        removeAllViews();
        if(mAdapter == null) return;

        if(mHeader != null) {
            if(mHeader.getParent() != null) ((ViewGroup) mHeader.getParent()).removeView(mHeader);
            final FrameLayout fl = new FrameLayout(getContext());
            fl.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            fl.setBackgroundResource(R.drawable.ap_selector_background);
            fl.addView(mHeader);
            addView(fl, 0);
        }

        int offset = (mHeader != null ? 1 : 0);
        for (int i = offset; i < mAdapter.getCount() + offset; i++) {
            final int index = i;
            final FrameLayout fl = new FrameLayout(getContext());
            fl.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            fl.setBackgroundResource(R.drawable.ap_selector_background);
            fl.addView(mAdapter.getView(i, null, fl));
            addView(fl);

            if (mItemClickListener != null) {
                fl.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mItemClickListener.onItemClick(null, fl.getRootView(), index, fl.getRootView().getId());
                    }
                });
            }

            if(mSwipeToDismissEnabled) {
                fl.setOnTouchListener(new SwipeDismissTouchListener (
                        fl,
                        mAdapter.getItem(i-offset),
                        new SwipeDismissTouchListener.DismissCallbacks() {
                            @Override
                            public boolean canDismiss(Object token) {
                                return true;
                            }

                            @Override
                            public void onDismiss(View view, Object token) {
                                removeView(fl);
                                if(mOnDismissedListener != null) {
                                    mOnDismissedListener.onDismiss(token);
                                }
                            }
                        }));
            }
        }

        if(mFooter != null) {
            if(mFooter.getParent() != null) ((ViewGroup) mFooter.getParent()).removeView(mFooter);
            final FrameLayout fl = new FrameLayout(getContext());
            fl.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            fl.setBackgroundResource(R.drawable.ap_selector_background);
            fl.addView(mFooter);
            addView(fl);
        }
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener l) {
        mItemClickListener = l;
        if(mAdapter != null) {
            int offset = (mHeader != null ? 1 : 0);
            for (int i = offset; i < mAdapter.getCount() + offset; i++) {
                final int index = i;
                final FrameLayout fl = (FrameLayout) getChildAt(index);
                if (mItemClickListener != null) {
                    fl.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mItemClickListener.onItemClick(null, fl.getRootView(), index, fl.getRootView().getId());
                        }
                    });
                } else {
                    fl.setOnClickListener(null);
                }
            }
        }
    }

    public void setHeader(View header) {
        mHeader = header;
        refresh();
    }

    public void setFooter(View footer) {
        mFooter = footer;
        refresh();
    }

    public void setSwipeToDismissEnabled(boolean enabled) {
        setSwipeToDismissEnabled(enabled, null);
    }

    public void setSwipeToDismissEnabled(boolean enabled, OnDismissedListener l) {
        mOnDismissedListener = l;
        mSwipeToDismissEnabled = enabled && android.os.Build.VERSION.SDK_INT >= 13;
        if(mAdapter != null) {
            int offset = (mHeader != null ? 1 : 0);
            for (int i = offset; i < mAdapter.getCount() + offset; i++) {
                final FrameLayout fl = (FrameLayout) getChildAt(i);
                if(mSwipeToDismissEnabled) {
                    fl.setOnTouchListener(new SwipeDismissTouchListener (
                            fl,
                            mAdapter.getItem(i-offset),
                            new SwipeDismissTouchListener.DismissCallbacks() {
                                @Override
                                public boolean canDismiss(Object token) {
                                    return true;
                                }

                                @Override
                                public void onDismiss(View view, Object token) {
                                    removeView(fl);
                                    if(mOnDismissedListener != null) {
                                        mOnDismissedListener.onDismiss(token);
                                    }
                                }
                            }));
                } else {
                    fl.setOnTouchListener(null);
                }
            }
        }
    }

    public interface OnDismissedListener {
        public void onDismiss(Object item);
    }
}
