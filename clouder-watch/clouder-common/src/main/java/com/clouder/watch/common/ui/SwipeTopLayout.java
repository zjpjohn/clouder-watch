package com.clouder.watch.common.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Scroller;


import com.clouder.watch.common.R;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by yang_shoulai on 6/26/2015.
 */
public class SwipeTopLayout extends FrameLayout {
    private static final String TAG = SwipeTopLayout.class.getSimpleName();
    private View mContentView;
    private int mTouchSlop;
    private int downX;
    private int downY;
    private int tempX;
    private int tempY;
    private Scroller mScroller;
    // private int viewWidth;
    // private int viewHeight;
    private boolean isSilding;
    private boolean isFinish;
    private Drawable mShadowDrawable;
    private Activity mActivity;

    private int screenHeight;

    private List<ViewPager> mViewPagers = new LinkedList<ViewPager>();

    public SwipeTopLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwipeTopLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        screenHeight = displayMetrics.heightPixels;
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mScroller = new Scroller(context);

        mShadowDrawable = getResources().getDrawable(R.drawable.shadow_left);
    }


    public void attachToActivity(Activity activity) {
        mActivity = activity;
        TypedArray a = activity.getTheme().obtainStyledAttributes(
                new int[]{android.R.attr.windowBackground});
        int background = a.getResourceId(0, 0);
        a.recycle();

        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
        ViewGroup decorChild = (ViewGroup) decor.getChildAt(0);
        decorChild.setBackgroundResource(background);
        decor.removeView(decorChild);
        addView(decorChild);
        setContentView(decorChild);
        decor.addView(this);
    }

    private void setContentView(View decorChild) {
        mContentView = (View) decorChild.getParent();
    }

    /**
     * 事件拦截操作
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = tempX = (int) ev.getRawX();
                downY = tempY = (int) ev.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                int moveX = (int) ev.getRawX();
                int moveY = (int) ev.getRawY();
                // 满足此条件屏蔽SildingFinishLayout里面子类的touch事件
                if (moveY - downY < -mTouchSlop && Math.abs(moveX - downX) < mTouchSlop) {
                    return true;
                }
                break;
        }

        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                int moveX = (int) event.getRawX();
                int moveY = (int) event.getRawY();
                int deltaY = tempY - moveY;
                tempX = moveX;
                tempY = moveY;

                if (moveY - downY < -mTouchSlop
                        && Math.abs((int) event.getRawX() - downX) < mTouchSlop) {
                    isSilding = true;
                }

                if (moveY - downY <= 0 && isSilding) {
                    mContentView.scrollBy(0, deltaY);
                }
                break;
            case MotionEvent.ACTION_UP:
                isSilding = false;
                if (mContentView.getScrollY() >= screenHeight / 2) {
                    isFinish = true;
                    scrollTop();
                } else {
                    scrollOrigin();
                    isFinish = false;
                }
                break;
            default:break;
        }

        return true;
    }

    /**
     * 获取SwipeBackLayout里面的ViewPager的集合
     *
     * @param mViewPagers
     * @param parent
     */
    private void getAlLViewPager(List<ViewPager> mViewPagers, ViewGroup parent) {
        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            if (child instanceof ViewPager) {
                mViewPagers.add((ViewPager) child);
            } else if (child instanceof ViewGroup) {
                getAlLViewPager(mViewPagers, (ViewGroup) child);
            }
        }
    }


    /**
     * 返回我们touch的ViewPager
     *
     * @param mViewPagers
     * @param ev
     * @return
     */
    private ViewPager getTouchViewPager(List<ViewPager> mViewPagers, MotionEvent ev) {
        if (mViewPagers == null || mViewPagers.size() == 0) {
            return null;
        }
        Rect mRect = new Rect();
        for (ViewPager v : mViewPagers) {
            v.getHitRect(mRect);

            if (mRect.contains((int) ev.getX(), (int) ev.getY())) {
                return v;
            }
        }
        return null;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed) {
            /*screenHeight = this.getWidth();
            screenHeight = this.getHeight();*/
            getAlLViewPager(mViewPagers, this);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mShadowDrawable != null && mContentView != null) {

            int left = mContentView.getLeft()
                    - mShadowDrawable.getIntrinsicWidth();
            int right = left + mShadowDrawable.getIntrinsicWidth();
            int top = mContentView.getTop();
            int bottom = mContentView.getBottom();

            mShadowDrawable.setBounds(left, top, right, bottom);
            mShadowDrawable.draw(canvas);
        }

    }


    /**
     * 滚动出界面
     */
    private void scrollTop() {
        final int delta = (screenHeight - mContentView.getScrollY());
        // 调用startScroll方法来设置一些滚动的参数，我们在computeScroll()方法中调用scrollTo来滚动item
        mScroller.startScroll(0, mContentView.getScrollY(), 0, delta,
                Math.abs(delta));
        invalidate();
    }

    /**
     * 滚动到起始位置
     */
    private void scrollOrigin() {
        int delta = mContentView.getScrollY();
        mScroller.startScroll(0, mContentView.getScrollY(), 0, -delta,
                Math.abs(delta));
        invalidate();
    }

    @Override
    public void computeScroll() {
        // 调用startScroll的时候scroller.computeScrollOffset()返回true
        if (mScroller.computeScrollOffset()) {
            mContentView.scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            invalidate();

            if (mScroller.isFinished() && isFinish) {
                mActivity.finish();
            }
        }
    }

}
