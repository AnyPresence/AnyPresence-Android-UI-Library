package com.anypresence.library;

import java.util.List;

import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;

import com.anypresence.rails_droid.RemoteObject;

/**
 * A class that abstracts away loading data into a fragment.
 * */
public abstract class AnyPresenceCarouselFragment<T extends RemoteObject> extends AnyPresenceViewFragment<T> {
	private ViewPager mPager;
	private AnyPresenceAdapter<T> mAdapter;
	private PagerAdapter mPagerAdapter;

	@Override
	protected View createAuthorizedView() {
		View root = new ViewPager(getContext());
		root.setId(R.id.ap_pager);
		return root;
	}

    private void ensureView() {
        if (mPager != null) {
            return;
        }
        View root = getView();
        if (root == null) {
            throw new IllegalStateException("Content view not yet created");
        }

        View rawView = root.findViewById(R.id.ap_pager);
        if (rawView == null) {
            throw new RuntimeException(
                    "Your content must have a View whose id attribute is " +
                            "'R.id.ap_pager'");
        }
        try {
        	mPager = (ViewPager) rawView;
        }
        catch(ClassCastException e) {
        	throw new RuntimeException("The View with an id 'R.id.ap_pager' must be an instance of android.support.v4.view.ViewPager");
        }
    }

    /**
     * Create an adapter for the UI of the ListView.
     * */
    protected abstract AnyPresenceAdapter<T> createAdapter(List<T> items);

    /**
     * The item has been selected.
     * */
    protected void onItemSelected(T item) {}

	/**
	 * Override this to change what happens when data is loaded.
	 * */
	@Override
	protected void update(final List<T> items) {
		ensureView();
		if(mAdapter == null) {
			mAdapter = createAdapter(items);
		}
		else {
			mAdapter.updateAdapter(items);
		}
		if(mPagerAdapter == null) {
			mPagerAdapter = new PagerAdapter() {
				private View[] mViews = new View[3];

				@Override
				public void startUpdate(View container) {}

				@Override
				public void finishUpdate(View container) {}

				@Override
				public Object instantiateItem(View container, final int position) {
					View convertView = null;
					if(mPager.getCurrentItem() == position - 1) {
						convertView = mViews[0];
						convertView = mViews[0] = mAdapter.getView(position, convertView, null);
					}
					else if(mPager.getCurrentItem() == position) {
						convertView = mViews[1];
						convertView = mViews[1] = mAdapter.getView(position, convertView, null);
					}
					else if(mPager.getCurrentItem() == position + 1) {
						convertView = mViews[2];
						convertView = mViews[2] = mAdapter.getView(position, convertView, null);
					}
					
					if(convertView.getParent() == null) {
						((ViewGroup) container).addView(convertView);
					}

					convertView.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							onItemSelected(items.get(position));
						}
					});

					return convertView;
				}

				@Override
				public void destroyItem(View container, int position, Object object) {
					((ViewGroup) container).removeView((View) object);
				}

				@Override
				public boolean isViewFromObject(View v, Object o) {
					return v == o;
				}

				@Override
				public int getCount() {
					return mAdapter.getCount();
				}

				@Override
				public Parcelable saveState() {
					return null;
				}

				@Override
				public void restoreState(Parcelable state, ClassLoader loader) {}
			};
		}
		mPager.setAdapter(mPagerAdapter);
	}
}
