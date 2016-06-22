package com.clouder.contacts.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.RelativeLayout;
import android.widget.Scroller;

public class SildingFinishLayout extends RelativeLayout implements
		OnTouchListener {

	private ViewGroup mParentView;

	private View touchView;

	private View listTouchView;

	private int mTouchSlop;

	private int downX;

	private int downY;

	private int tempX;

	private Scroller mScroller;

	private int viewWidth;

	private boolean isSilding;

	private boolean isLeft;
	private int deltaLeft;

	private OnSildingFinishListener onSildingFinishListener;
	private boolean isFinish;


	public SildingFinishLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public SildingFinishLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		mScroller = new Scroller(context);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		if (changed) {
			mParentView = (ViewGroup) this.getParent();
			viewWidth = this.getWidth();
		}
	}

	public void setOnSildingFinishListener(
			OnSildingFinishListener onSildingFinishListener) {
		this.onSildingFinishListener = onSildingFinishListener;
	}

	public void setTouchView(View touchView) {
		this.touchView = touchView;
		touchView.setOnTouchListener(this);
	}

	public View getTouchView() {
		return touchView;
	}

	public void setListTouchView(View touchView) {
		this.listTouchView = touchView;
		touchView.setOnTouchListener(this);
	}

	public View getListTouchView(View touchView) {
		return listTouchView;
	}

	private void scrollRight() {
		final int delta = (viewWidth + mParentView.getScrollX());
		mScroller.startScroll(mParentView.getScrollX(), 0, -delta + 1, 0,
				Math.abs(delta));
		postInvalidate();
	}

	private void scrollOrigin() {
		int delta = mParentView.getScrollX();
		mScroller.startScroll(mParentView.getScrollX(), 0, -delta, 0,
				Math.abs(delta));
		postInvalidate();
	}

	@SuppressLint({ "ClickableViewAccessibility", "Recycle" })
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				downX = tempX = (int) event.getRawX();
				downY = (int) event.getRawY();
				break;
			case MotionEvent.ACTION_MOVE:
				int moveX = (int) event.getRawX();
				int deltaX = tempX - moveX;
				tempX = moveX;
				if (Math.abs(moveX - downX) > mTouchSlop
						&& Math.abs((int) event.getRawY() - downY) < mTouchSlop) {
					isSilding = true;

					if (v instanceof AbsListView) {
						MotionEvent cancelEvent = MotionEvent.obtain(event);
						cancelEvent
								.setAction(MotionEvent.ACTION_CANCEL
										| (event.getActionIndex() << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
						v.onTouchEvent(cancelEvent);
					}
				}

				if (moveX - downX >= 0 && isSilding) {
					mParentView.scrollBy(deltaX, 0);
					if (v instanceof AbsListView) {
						return true;
					}
				}

				if (moveX - downX <= 0 && isSilding) {
					isLeft = true;
					deltaLeft = downX - moveX;
				}
				break;
			case MotionEvent.ACTION_UP:
				isSilding = false;
				if (mParentView.getScrollX() <= -viewWidth / 2) {
					isFinish = true;
					scrollRight();
				} else {
					scrollOrigin();
					isFinish = false;
				}

				if (isLeft && deltaLeft >= viewWidth / 2) {

				} else {
					isLeft = false;
					deltaLeft = 0;
				}
				break;
		}

		if (v instanceof AbsListView) {
			return v.onTouchEvent(event);
		}

		return true;
	}

	@Override
	public void computeScroll() {
		if (mScroller.computeScrollOffset()) {
			mParentView.scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
			postInvalidate();

			if (mScroller.isFinished()) {

				if (onSildingFinishListener != null && isFinish) {
					onSildingFinishListener.onSildingRightFinish();
				} else if (onSildingFinishListener != null && isLeft) {
					onSildingFinishListener.onSildingLeftFinish();
				}
			}
		}
	}


	public interface OnSildingFinishListener {
		public void onSildingRightFinish();

		public void onSildingLeftFinish();
	}

}
