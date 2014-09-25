package com.anypresence.library;

import java.util.List;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A class that abstracts authorization in a fragment.
 * */
public class AnyPresenceRoleFragment extends AnyPresenceFragment {
    boolean mIsActivityCreated = false;
    private View mAuthorizedText;
    private View mUnauthorizedText;
    List<String> mRoles;
    LinearLayout mRoot;

    public AnyPresenceRoleFragment() {
        super();
        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mIsActivityCreated = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        notifyDataChanged();
    }

    /**
     * Displays an unauthorized message when needed
     * */
    public void notifyDataChanged() {
        if(!mIsActivityCreated) return;
        if(Auth.isAuthorized(mRoles)) {
            // User is authorized to see page
        	enableAuthorizedView();
        }
        else {
            // User is not authorized to see page
        	enableUnauthorizedView();
        }
    }
    
    void disableViews() {
        mAuthorizedText.setVisibility(View.GONE);
        mUnauthorizedText.setVisibility(View.GONE);
    }
    
    protected void enableAuthorizedView() {
        try {
            disableViews();
            mAuthorizedText.setVisibility(View.VISIBLE);
        }
        catch(IllegalStateException e) {}
    }

    protected void enableUnauthorizedView() {
        try {
            disableViews();
            mUnauthorizedText.setVisibility(View.VISIBLE);
        }
        catch(IllegalStateException e) {}
    }

    @Override
    public void setViewShown(boolean shown) {
        try {
            super.setViewShown(shown);
        }
        catch(IllegalStateException e) {}
    }

    /**
     * Create a view to be displayed when authorized.
     * 
     * Override this method to provide a custom view
     * */
    protected View createAuthorizedView() {
        TextView emptyView = new TextView(getContext());
        emptyView.setText(R.string.ap_authorized);
        emptyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        emptyView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        emptyView.setGravity(Gravity.CENTER);
        return emptyView;
    }

    /**
     * Create a view to be displayed when not authorized.
     * 
     * Override this method to provide a custom view
     * */
    protected View createUnauthorizedView() {
        TextView emptyView = new TextView(getContext());
        emptyView.setText(R.string.ap_unauthorized);
        emptyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        emptyView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        emptyView.setGravity(Gravity.CENTER);
        return emptyView;
    }
    
    /**
     * Set roles that require the user to be authenticated
     * */
    public void setRoles(List<String> roles) {
        mRoles = roles;
        notifyDataChanged();
    }
    
    /**
     * Get roles that require the user to be authenticated
     * */
    public List<String> getRoles() {
        return mRoles;
    }

    private static final int MATCH_PARENT = LinearLayout.LayoutParams.MATCH_PARENT;

	@Override
	protected View inflateView(Bundle savedInstanceState) {
		mRoot = new LinearLayout(getContext());

		mAuthorizedText = createAuthorizedView();
		mAuthorizedText.setVisibility(View.GONE);
		mRoot.addView(mAuthorizedText, new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));

		mUnauthorizedText = createUnauthorizedView();
		mUnauthorizedText.setVisibility(View.GONE);
		mRoot.addView(mUnauthorizedText, new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));

		return mRoot;
	}
}
