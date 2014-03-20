package com.droidprojects.personaltranslator.customclasses;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * This custom class removes the swipe functionality from the view pager.
 * @author Fouad
 */
public class NonSwipeViewPager extends ViewPager{

	public NonSwipeViewPager(Context context) {
		super(context);
	}

	public NonSwipeViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent arg0) {
		return false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent arg0) {
		return false;
	}
}