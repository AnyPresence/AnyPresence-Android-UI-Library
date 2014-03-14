package com.anypresence.library;

import java.util.Calendar;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.widget.DatePicker;

/**
 * A preference with a pop-up that stores a date. Saves the date as a long in
 * seconds from epoch.
 * */
public class DatePreference extends Preference {
    private Date mDate;
    private Date mMaxDate;
    private Date mMinDate;

    public DatePreference(Context context) {
        super(context);
        setUp();
    }

    public DatePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setUp();
    }

    public DatePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setUp();
    }

    @SuppressLint("NewApi")
    private void setUp() {
        setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference p) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                final DatePicker datePicker = new DatePicker(getContext());

                if(android.os.Build.VERSION.SDK_INT >= 11) {
                    datePicker.setCalendarViewShown(false);
                }
                if(mMaxDate != null) datePicker.setMaxDate(mMaxDate.getTime());
                if(mMinDate != null) datePicker.setMinDate(mMinDate.getTime());

                Calendar cal = Calendar.getInstance();
                cal.setTime(mDate == null ? new Date() : mDate);
                datePicker.updateDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));

                builder.setView(datePicker);
                builder.setPositiveButton(R.string.ap_option_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Calendar cal = Calendar.getInstance();
                        cal.set(Calendar.YEAR, datePicker.getYear());
                        cal.set(Calendar.MONTH, datePicker.getMonth());
                        cal.set(Calendar.DATE, datePicker.getDayOfMonth());
                        setDate(cal.getTime());
                    }
                });
                builder.setNegativeButton(R.string.ap_option_clear, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setDate(null);
                    }
                });
                builder.create().show();
                return true;
            }
        });
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.timeInMillis = mDate == null ? -1 : mDate.getTime();
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if(!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setDate(ss.timeInMillis);
    }

    public void setDate(long timeInMillis) {
        setDate(timeInMillis == -1 ? null : new Date(timeInMillis));
    }

    public void setDate(Date date) {
        this.mDate = date;
        if(getOnPreferenceChangeListener() != null) {
            if(!getOnPreferenceChangeListener().onPreferenceChange(this, date)) return;
        }
        if(isPersistent()) {
            PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putLong(getKey(), date.getTime()).commit();
        }
    }

    public Date getDate() {
        return mDate;
    }

    public void setMaxDate(Date date) {
        this.mMaxDate = date;
    }

    public Date getMaxDate() {
        return mMaxDate;
    }

    public void setMinDate(Date date) {
        this.mMinDate = date;
    }

    public Date getMinDate() {
        return mMinDate;
    }

    static class SavedState extends BaseSavedState {
        long timeInMillis;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.timeInMillis = in.readLong();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeLong(timeInMillis);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
