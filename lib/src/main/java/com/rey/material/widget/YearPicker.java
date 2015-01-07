package com.rey.material.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.BaseAdapter;

import com.rey.material.R;
import com.rey.material.drawable.BlankDrawable;
import com.rey.material.drawable.CircleDrawable;
import com.rey.material.util.ThemeUtil;
import com.rey.material.util.TypefaceUtil;

import java.util.Calendar;

/**
 * Created by Rey on 12/26/2014.
 */
public class YearPicker extends ListView{

    private YearAdapter mAdapter;

    private int mTextSize;
    private int mItemHeight;
    private int mSelectionColor;
    private int mAnimDuration;
    private Interpolator mInInterpolator;
    private Interpolator mOutInterpolator;
    private Typeface mTypeface;

    private int mItemRealHeight = -1;
    private int mPadding;
    private int mPositionShift;
    private int mDistanceShift;

    private Paint mPaint;

    public interface OnYearChangedListener{

        public void onYearChanged(int oldValue, int newValue);

    }

    private OnYearChangedListener mOnYearChangedListener;

    private static final int[][] STATES = new int[][]{
            new int[]{-android.R.attr.state_checked},
            new int[]{android.R.attr.state_checked},
    };

    private int[] mTextColors = new int[2];

    private float mAlpha;

    public YearPicker(Context context) {
        super(context);

        init(context, null, 0, 0);
    }

    public YearPicker(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context, attrs, 0, 0);
    }

    public YearPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs, defStyleAttr, 0);
    }

    public YearPicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes){
        setWillNotDraw(false);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);

        mAdapter = new YearAdapter();
        setAdapter(mAdapter);
        setScrollBarStyle(SCROLLBARS_OUTSIDE_OVERLAY);
        setSelector(BlankDrawable.getInstance());
        setDividerHeight(0);
        setCacheColorHint(Color.TRANSPARENT);
        setClipToPadding(false);

        mPadding = ThemeUtil.dpToPx(context, 4);

        applyStyle(context, attrs, defStyleAttr, defStyleRes);
    }

    public void applyStyle(int resId){
        applyStyle(getContext(), null, 0, resId);
    }

    private void applyStyle(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes){
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.YearPicker, defStyleAttr, defStyleRes);
        mTextSize = a.getDimensionPixelSize(R.styleable.YearPicker_dp_yearTextSize, context.getResources().getDimensionPixelOffset(R.dimen.abc_text_size_title_material));
        int year = a.getInteger(R.styleable.YearPicker_dp_year, mAdapter.getYear());
        int yearMin = a.getInteger(R.styleable.YearPicker_dp_yearMin, mAdapter.getMinYear());
        int yearMax = a.getInteger(R.styleable.YearPicker_dp_yearMax, mAdapter.getMaxYear());
        mItemHeight = a.getDimensionPixelSize(R.styleable.YearPicker_dp_yearItemHeight, ThemeUtil.dpToPx(context, 48));
        mTextColors[0] = a.getColor(R.styleable.YearPicker_dp_textColor, 0xFF000000);
        mTextColors[1] = a.getColor(R.styleable.YearPicker_dp_textHighlightColor, 0xFFFFFFFF);
        mSelectionColor = a.getColor(R.styleable.YearPicker_dp_selectionColor, ThemeUtil.colorPrimary(context, 0xFF000000));
        mAnimDuration = a.getInteger(R.styleable.YearPicker_dp_animDuration, context.getResources().getInteger(android.R.integer.config_mediumAnimTime));
        int resId = a.getResourceId(R.styleable.YearPicker_dp_inInterpolator, 0);
        if(resId != 0)
            mInInterpolator = AnimationUtils.loadInterpolator(context, resId);
        else
            mInInterpolator = new DecelerateInterpolator();
        resId = a.getResourceId(R.styleable.YearPicker_dp_outInterpolator, 0);
        if(resId != 0)
            mOutInterpolator = AnimationUtils.loadInterpolator(context, resId);
        else
            mOutInterpolator = new DecelerateInterpolator();
        String familyName = a.getString(R.styleable.YearPicker_dp_fontFamily);
        int style = a.getInteger(R.styleable.YearPicker_dp_textStyle, Typeface.NORMAL);

        mTypeface = TypefaceUtil.load(context, familyName, style);

        a.recycle();

        if(yearMax < yearMin)
            yearMax = Integer.MAX_VALUE;

        if(year < 0){
            Calendar cal = Calendar.getInstance();
            year = cal.get(Calendar.YEAR);
        }

        year = Math.max(yearMin, Math.min(yearMax, year));

        setYearRange(yearMin, yearMax);
        setYear(year);
        mAdapter.notifyDataSetChanged();
    }

    public void setYearRange(int min, int max){
        mAdapter.setYearRange(min, max);
    }

    public void setYear(int year){
        mAdapter.setYear(year);
        setSelectionFromTop(mAdapter.positionOfYear(mAdapter.getYear()) - mPositionShift, mDistanceShift);
    }

    public int getYear(){
        return mAdapter.getYear();
    }

    public void setOnYearChangedListener(OnYearChangedListener listener){
        mOnYearChangedListener = listener;
    }

    private void measureItemHeight(){
        if(mItemRealHeight > 0)
            return;

        mPaint.setTextSize(mTextSize);
        mItemRealHeight = Math.max(Math.round(mPaint.measureText("9999", 0, 4)) + mPadding * 2, mItemHeight);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        measureItemHeight();

        if(heightMode != MeasureSpec.EXACTLY){
            heightSize = heightMode == MeasureSpec.AT_MOST ? Math.min(heightSize, mItemRealHeight * 3) : mItemRealHeight * 3;
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize + getPaddingTop() + getPaddingBottom(), MeasureSpec.EXACTLY);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float shift = (h / (float)mItemRealHeight - 1) / 2;
        mPositionShift = (int)Math.floor(shift);
        mPositionShift = shift > mPositionShift ? mPositionShift + 1 : mPositionShift;
        mDistanceShift = (int)((shift - mPositionShift) * mItemRealHeight) - getPaddingTop();
        setSelectionFromTop(mAdapter.positionOfYear(mAdapter.getYear()) - mPositionShift, mDistanceShift);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void setAlpha(float alpha) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            super.setAlpha(alpha);
            invalidate();
        }
        else{
            if(mAlpha != alpha){
                mAlpha = alpha;
                ViewCompat.setAlpha(this, alpha);
                invalidate();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public float getAlpha() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) ? super.getAlpha() : mAlpha;
    }

    @Override
    public void draw(Canvas canvas) {
        if(getAlpha() != 0f)
            super.draw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if(getAlpha() == 0f)
            return false;

        return super.onTouchEvent(ev);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if(getAlpha() == 0f)
            return false;

        return super.dispatchTouchEvent(ev);
    }

    private class YearAdapter extends BaseAdapter implements View.OnClickListener{

        private int mMinYear = 1990;
        private int mMaxYear = Integer.MAX_VALUE - 1;
        private int mCurYear = -1;

        public YearAdapter(){}

        public int getMinYear(){
            return mMinYear;
        }

        public int getMaxYear(){
            return mMaxYear;
        }

        public void setYearRange(int min, int max){
            if(mMinYear != min || mMaxYear != max){
                mMinYear = min;
                mMaxYear = max;
                notifyDataSetChanged();
            }
        }

        public int positionOfYear(int year){
            return year - mMinYear;
        }

        @Override
        public int getCount(){
            return mMaxYear - mMinYear + 1;
        }

        @Override
        public Object getItem(int position){
            return mMinYear + position;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        public void setYear(int year){
            if(mCurYear != year){
                int old = mCurYear;
                mCurYear = year;

                CircleCheckedTextView child = (CircleCheckedTextView)YearPicker.this.getChildAt(positionOfYear(old) - YearPicker.this.getFirstVisiblePosition());
                if(child != null)
                    child.setChecked(false);

                child = (CircleCheckedTextView)YearPicker.this.getChildAt(positionOfYear(mCurYear) - YearPicker.this.getFirstVisiblePosition());
                if(child != null)
                    child.setChecked(true);

                if(mOnYearChangedListener != null)
                    mOnYearChangedListener.onYearChanged(old, mCurYear);
            }
        }

        public int getYear(){
            return mCurYear;
        }

        @Override
        public void onClick(View v) {
            setYear((Integer)v.getTag());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            CircleCheckedTextView v = (CircleCheckedTextView)convertView;
            if(v == null){
                v = new CircleCheckedTextView(getContext());
                v.setGravity(Gravity.CENTER);
                v.setMinHeight(mItemRealHeight);
                v.setMaxHeight(mItemRealHeight);
                v.setAnimDuration(mAnimDuration);
                v.setInterpolator(mInInterpolator, mOutInterpolator);
                v.setBackgroundColor(mSelectionColor);
                v.setTypeface(mTypeface);
                v.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize);
                v.setTextColor(new ColorStateList(STATES, mTextColors));
                v.setOnClickListener(this);
            }

            int year = (Integer)getItem(position);
            v.setTag(year);
            v.setText(String.valueOf(year));
            v.setCheckedImmediately(year == mCurYear);
            return v;
        }
    }

    private class CircleCheckedTextView extends android.widget.CheckedTextView {

        private CircleDrawable mBackground;

        public CircleCheckedTextView(Context context) {
            super(context);

            setGravity(Gravity.CENTER);
            setPadding(0, 0, 0, 0);

            mBackground = new CircleDrawable();
            mBackground.setInEditMode(isInEditMode());
            mBackground.setAnimEnable(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                setBackground(mBackground);
            else
                setBackgroundDrawable(mBackground);
            mBackground.setAnimEnable(true);
        }

        @Override
        public void setBackgroundColor(int color) {
            mBackground.setColor(color);
        }

        public void setAnimDuration(int duration) {
            mBackground.setAnimDuration(duration);
        }

        public void setInterpolator(Interpolator in, Interpolator out) {
            mBackground.setInterpolator(in, out);
        }

        public void setCheckedImmediately(boolean checked){
            mBackground.setAnimEnable(false);
            setChecked(checked);
            mBackground.setAnimEnable(true);
        }

    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);

        ss.yearMin = mAdapter.getMinYear();
        ss.yearMax = mAdapter.getMaxYear();
        ss.year = mAdapter.getYear();

        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setYearRange(ss.yearMin, ss.yearMax);
        setYear(ss.year);
    }

    static class SavedState extends BaseSavedState {
        int yearMin;
        int yearMax;
        int year;

        /**
         * Constructor called from {@link Switch#onSaveInstanceState()}
         */
        SavedState(Parcelable superState) {
            super(superState);
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            yearMin = in.readInt();
            yearMax = in.readInt();
            year = in.readInt();
        }

        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeValue(yearMin);
            out.writeValue(yearMax);
            out.writeValue(year);
        }

        @Override
        public String toString() {
            return "YearPicker.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " yearMin=" + yearMin
                    + " yearMax=" + yearMax
                    + " year=" + year + "}";
        }

        public static final Creator<SavedState> CREATOR
                = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}