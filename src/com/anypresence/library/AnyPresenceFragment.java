package com.anypresence.library;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import java.lang.reflect.Field;

public abstract class AnyPresenceFragment extends Fragment {
    static final int INTERNAL_PROGRESS_CONTAINER_ID = 0x00ff0002;
    static final int INTERNAL_VIEW_CONTAINER_ID = 0x00ff0003;

    final private Handler mHandler = new Handler();

    final private Runnable mRequestFocus = new Runnable() {
        public void run() {
            ((ViewGroup) mViewContainer).focusableViewAvailable(mView);
        }
    };

    private View mView;
    private View mProgressContainer;
    private View mViewContainer;
    private boolean mViewShown = true;

    /**
     * Returns a base context. This context will die when the activity dies, so
     * don't hold on to it.
     * */
    protected Context getContext() {
        return getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	super.onCreateView(inflater, container, savedInstanceState);
        final Context context = getActivity();

        FrameLayout root = new FrameLayout(context);

        // ------------------------------------------------------------------

        LinearLayout pframe = new LinearLayout(context);
        pframe.setId(INTERNAL_PROGRESS_CONTAINER_ID);
        pframe.setOrientation(LinearLayout.VERTICAL);
        pframe.setVisibility(View.GONE);
        pframe.setGravity(Gravity.CENTER);

        ProgressBar progress = new ProgressBar(context, null,
                android.R.attr.progressBarStyleLarge);
        pframe.addView(progress, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(pframe, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // ------------------------------------------------------------------

        FrameLayout lframe = new FrameLayout(context);
        lframe.setId(INTERNAL_VIEW_CONTAINER_ID);

        View cv = inflateView(savedInstanceState);
        cv.setId(android.R.id.content);
        lframe.addView(cv, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        root.addView(lframe, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // ------------------------------------------------------------------

        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        return root;
    }

    /**
     * Attach to view once the view hierarchy has been created.
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ensureView();
    }

    /**
     * Detach from list view.
     */
    @Override
    public void onDestroyView() {
        mHandler.removeCallbacks(mRequestFocus);
        mView = null;
        mViewShown = false;
        mProgressContainer = mViewContainer = null;
        super.onDestroyView();
    }

    protected abstract View inflateView(Bundle savedInstanceState);

    /**
     * Control whether the view is being displayed.  You can make it not
     * displayed if you are waiting for the initial data to show in it.  During
     * this time an indeterminant progress indicator will be shown instead.
     *
     * @param shown If true, the view is shown; if false, the progress
     * indicator.  The initial value is true.
     */
    public void setViewShown(boolean shown) {
        setViewShown(shown, true);
    }

    /**
     * Like {@link #setViewShown(boolean)}, but no animation is used when
     * transitioning from the previous state.
     */
    public void setViewShownNoAnimation(boolean shown) {
        setViewShown(shown, false);
    }

    /**
     * Control whether the view is being displayed.  You can make it not
     * displayed if you are waiting for the initial data to show in it.  During
     * this time an indeterminant progress indicator will be shown instead.
     *
     * @param shown If true, the view is shown; if false, the progress
     * indicator.  The initial value is true.
     * @param animate If true, an animation will be used to transition to the
     * new state.
     */
    private void setViewShown(boolean shown, boolean animate) {
        ensureView();
        if (mProgressContainer == null) {
            throw new IllegalStateException("Can't be used with a custom content view");
        }
        if (mViewShown == shown) {
            return;
        }
        mViewShown = shown;
        if (shown) {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
                mViewContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
            } else {
                mProgressContainer.clearAnimation();
                mViewContainer.clearAnimation();
            }
            mProgressContainer.setVisibility(View.GONE);
            mViewContainer.setVisibility(View.VISIBLE);
        } else {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
                mViewContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
            } else {
                mProgressContainer.clearAnimation();
                mViewContainer.clearAnimation();
            }
            mProgressContainer.setVisibility(View.VISIBLE);
            mViewContainer.setVisibility(View.GONE);
        }
    }

    private void ensureView(){
        if (mView != null) {
            return;
        }
        View root = getView();
        if (root == null) {
            throw new IllegalStateException("Content view not yet created");
        }

        mProgressContainer = root.findViewById(INTERNAL_PROGRESS_CONTAINER_ID);
        mViewContainer = root.findViewById(INTERNAL_VIEW_CONTAINER_ID);
        View rawView = root.findViewById(android.R.id.content);
        if (rawView == null) {
            throw new RuntimeException(
                    "Your content must have a View whose id attribute is " +
                            "'android.R.id.content'");
        }
        mView = rawView;

        mViewShown = true;

        mHandler.post(mRequestFocus);
    }

    @Override
    public void setRetainInstance(boolean retain) {
	    try {
	        // Fragments inside fragments can't do this
	        super.setRetainInstance(retain);
	    }
	    catch(IllegalStateException e) {}
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // A bug fix for child fragment manager, since it doesn't get reset properly.
        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
