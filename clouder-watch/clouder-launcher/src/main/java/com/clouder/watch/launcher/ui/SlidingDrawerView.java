package com.clouder.watch.launcher.ui;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

/**
 * Created by yang_shoulai on 7/6/2015.
 */
public class SlidingDrawerView extends FrameLayout {

    private static final String TAG = "SlidingDrawerView";

    public static final int VIEW_CONTENT = 0;
    public static final int VIEW_SLIDING = 1;
    public static final int VIEW_DRAWER = 2;

    private int view = VIEW_CONTENT;

    private int mTouchSlop;

    private int downX;
    private int downY;

    private int tempX;
    private int tempY;

    private boolean animation = false;

    private boolean sliding;
    private boolean unsliding;
    private boolean drawering;
    private boolean undrawering;
    private boolean once;

    private boolean swipeRight;

    private boolean swipeTop;

    private View contentView;
    private View slidingView;
    private View drawerView;

    private ISwipeListener swipeListener;

    private OnLongClickListener contentViewLongClickListener;

    private OnClickListener contentViewClickListener;

    public SlidingDrawerView(Context context) {
        this(context, null);
    }

    public SlidingDrawerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingDrawerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        contentView.layout(0, 0, contentView.getMeasuredWidth(), contentView.getMeasuredHeight());
        slidingView.layout(0, -slidingView.getMeasuredHeight(), slidingView.getMeasuredWidth(), 0);
        drawerView.layout(contentView.getMeasuredWidth(), 0, contentView.getMeasuredWidth() + drawerView.getMeasuredWidth(), drawerView.getMeasuredHeight());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        contentView = getChildAt(0);
        slidingView = getChildAt(1);
        drawerView = getChildAt(2);
        contentView.setClickable(true);
        contentView.setLongClickable(true);
        if (contentViewLongClickListener != null) {
            contentView.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (view == VIEW_CONTENT) {
                        return contentViewLongClickListener.onLongClick(v);
                    }
                    return false;
                }
            });
        }
        if (contentViewClickListener != null) {
            contentView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (view == VIEW_CONTENT) {
                        contentViewClickListener.onClick(v);
                    }
                }
            });
        }
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Width must have an exact value or MATCH_PARENT");
        } else if (heightMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Height must have an exact value or MATCH_PARENT");
        } else {
            int childCount = getChildCount();
            if (childCount != 3) {
                throw new IllegalStateException("Vertical drawer layout must have exactly 3 children!");
            }
        }
        this.setMeasuredDimension(widthSize, heightSize);
        measureChildren(widthMeasureSpec, heightMeasureSpec);
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (animation) {
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                once = true;
                sliding = unsliding = drawering = undrawering = swipeRight = swipeTop = false;
                downX = tempX = (int) event.getRawX();
                downY = tempY = (int) event.getRawY();
                Log.d(TAG, String.format("onInterceptTouchEvent : ACTION_DOWN, [x = %s, y = %s].", downX, downY));
                break;
            case MotionEvent.ACTION_MOVE:
                int moveX = (int) event.getRawX();
                int moveY = (int) event.getRawY();
                Log.d(TAG, String.format("onInterceptTouchEvent : ACTION_MOVE, [x = %s, y = %s].", moveX, moveY));
                int deltaX = tempX - moveX;
                int deltaY = tempY - moveY;
                Log.d(TAG, "开始识别此次手势事件.....");
                Log.d(TAG, "需要识别的手势包括，打开/关闭应用列表、打开/关闭快捷设置、右滑呼出语音输入.");
                if (once && (Math.abs(deltaX) > mTouchSlop || Math.abs(deltaY) > mTouchSlop)) {
                    if (view == VIEW_CONTENT) {
                        if (Math.abs(moveX - downX) < Math.abs(moveY - downY) && moveY - downY > 0) {
                            sliding = true;
                            unsliding = drawering = undrawering = swipeRight = swipeTop = false;
                        } else if (Math.abs(moveX - downX) >= Math.abs(moveY - downY) && moveX - downX < 0) {
                            drawering = true;
                            sliding = unsliding = undrawering = swipeRight = swipeTop = false;
                        } else if (Math.abs(moveX - downX) >= Math.abs(moveY - downY) && moveX > downX) {
                            swipeRight = true;
                            sliding = undrawering = drawering = undrawering = swipeTop = false;
                        } else if (Math.abs(moveY - downY) > Math.abs(moveX - downX) && moveY < downY) {
                            swipeTop = true;
                            sliding = undrawering = drawering = undrawering = swipeRight = false;
                        }
                    } else if (view == VIEW_SLIDING) {
                        if (Math.abs(moveX - downX) < Math.abs(moveY - downY) && moveY - downY < 0) {
                            sliding = drawering = undrawering = swipeRight = swipeTop = false;
                            unsliding = true;
                        }

                    } else if (view == VIEW_DRAWER) {
                        if (Math.abs(moveX - downX) > Math.abs(moveY - downY) && moveX - downX > 0) {
                            undrawering = true;
                            sliding = drawering = unsliding = swipeRight = swipeTop = false;
                        }

                    }
                    once = false;
                }
                break;
            default:
                break;
        }
        boolean result = sliding || unsliding || drawering || undrawering || swipeRight || swipeTop;
        if (result) {
            Log.d(TAG, "成功识别到手势，手势事件将被拦截");
            if (sliding) {
                Log.d(TAG, "手势为【打开快捷方式】");
            } else if (unsliding) {
                Log.d(TAG, "手势为【关闭快捷方式】");
            } else if (drawering) {
                Log.d(TAG, "手势为【打开应用列表】");
            } else if (undrawering) {
                Log.d(TAG, "手势为【关闭应用列表】");
            } else if (swipeRight) {
                Log.d(TAG, "手势为【打开语音输入】");
            } else if (swipeTop) {
                Log.d(TAG, "手势为【打开通知列表】");
            }
        } else {
            Log.d(TAG, "未能识别手势，事件将下放");
        }
        return result;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (animation) {
            return true;
        }
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                int moveX = (int) event.getRawX();
                int moveY = (int) event.getRawY();
                tempX = moveX;
                tempY = moveY;
                if (sliding) {
                    if (moveY >= downY && moveY - downY <= slidingView.getMeasuredHeight()) {
                        slidingView.setTranslationY(moveY - downY);
                        contentView.setAlpha(0.95f * (moveY - downY) / slidingView.getMeasuredHeight());
                    }
                } else if (unsliding) {
                    if (moveY <= downY && downY - moveY <= slidingView.getMeasuredHeight()) {
                        slidingView.setTranslationY(slidingView.getMeasuredHeight() + moveY - downY);
                        contentView.setAlpha(0.95f - (downY - moveY) * 0.95f / slidingView.getMeasuredHeight());
                    }
                } else if (drawering) {
                    if (moveX <= downX && downX - moveX <= drawerView.getMeasuredWidth()) {
                        drawerView.setTranslationX(moveX - downX);
                        contentView.setAlpha((downX - moveX) * 1.0f / drawerView.getMeasuredWidth());
                    }
                } else if (undrawering && moveX - downX <= drawerView.getMeasuredWidth()) {
                    if (moveX >= downX) {
                        int left = moveX - downX;
                        drawerView.setTranslationX(-drawerView.getMeasuredWidth() + left);
                        contentView.setAlpha(1.0f - 1.0f * left / drawerView.getMeasuredWidth());
                    }
                } else if (swipeRight && moveX - downX <= contentView.getMeasuredWidth()) {
//                    contentView.setAlpha(0.95f * (moveX - downX) / drawerView.getMeasuredWidth());
                } else if (swipeTop && downY - moveY <= contentView.getMeasuredWidth()) {
//                    contentView.setAlpha(0.95f * (downY - moveY) / drawerView.getMeasuredWidth());
                }
                break;

            case MotionEvent.ACTION_UP:
                int upX = (int) event.getX();
                int upY = (int) event.getY();
                Log.d(TAG, String.format("onTouchEvent : ACTION_UP, [x = %s, y = %s].", upX, upY));
                if (sliding) {
                    if (upY - downY >= slidingView.getMeasuredHeight() / 4) {
                        translateY(slidingView, 0, slidingView.getMeasuredHeight() - (upY - downY), true);
                    } else if (upY - downY > 0) {
                        translateY(slidingView, 0, downY - upY, false);
                    }
                } else if (unsliding) {
                    if (upY - downY <= -slidingView.getMeasuredHeight() / 4) {
                        translateY(slidingView, 0, -slidingView.getMeasuredHeight() + downY - upY, false);
                    } else if (upY - downY < 0) {
                        translateY(slidingView, 0, Math.abs(upY - downY), true);
                    }
                } else if (drawering) {
                    if (upX - downX <= -drawerView.getMeasuredWidth() / 4) {
                        translateX(drawerView, 0, downX - upX - drawerView.getMeasuredWidth(), true);
                    } else if (upX - downX < 0) {
                        translateX(drawerView, 0, downX - upX, false);
                    }
                } else if (undrawering) {
                    if (upX - downX >= drawerView.getMeasuredWidth() / 4) {
                        translateX(drawerView, 0, drawerView.getMeasuredWidth() + downX - upX, false);
                    } else if (upX - downX > 0) {
                        translateX(drawerView, 0, downX - upX, true);
                    }
                } else if (swipeRight) {
                    contentView.setAlpha(0.0f);
                    if (upX - downX >= drawerView.getMeasuredWidth() / 4) {
                        if (swipeListener != null) {
                            swipeListener.onSwipeRight();
                        }
                    }
                } else if (swipeTop) {
                    contentView.setAlpha(0.0f);
                    if (downY - upY >= drawerView.getMeasuredHeight() / 4) {
                        if (swipeListener != null) {
                            swipeListener.onSwipeTop();
                        }
                    }
                }
                break;
            default:
                break;
        }
        return sliding || unsliding || drawering || undrawering || swipeRight || swipeTop;
    }


    private void translateY(final View v, int fromDeltaY, final int toDeltaY, final boolean show) {
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(v, "translationY", v.getTranslationY(), show ? v.getMeasuredHeight() : 0);
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(contentView, "alpha", contentView.getAlpha(), show ? 0.95f : 0.0f);
        animatorSet.playTogether(translateAnimator, alphaAnimator);
        animatorSet.setDuration(Math.abs(toDeltaY) / 2);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                animation = true;
                Log.d(TAG, "Y轴平移动画开始，动画结束时该将" + (show ? "显示" : "隐藏"));
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                animation = false;
                Log.d(TAG, "Y轴平移动画结束");
                if (show) {
                    view = VIEW_SLIDING;
                } else {
                    view = VIEW_CONTENT;
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animatorSet.start();
    }


    private void translateX(final View v, int fromDeltaX, int toDeltaX, final boolean show) {
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(v, "translationX", v.getTranslationX(), show ? -v.getMeasuredWidth() : 0);
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(contentView, "alpha", contentView.getAlpha(), show ? 1.0f : 0.0f);
        animatorSet.playTogether(translateAnimator, alphaAnimator);
        animatorSet.setDuration(Math.abs(toDeltaX) / 2);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                animation = true;
                Log.d(TAG, "X轴平移动画开始，动画结束时该将" + (show ? "显示" : "隐藏"));
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                animation = false;
                Log.d(TAG, "X轴平移动画结束");
                if (show) {
                    view = VIEW_DRAWER;
                } else {
                    view = VIEW_CONTENT;
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animatorSet.start();
    }

    public int getCurrentView() {
        return view;
    }

    public interface ISwipeListener {
        void onSwipeRight();

        void onSwipeTop();
    }

    public void showDrawer() {
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(drawerView, "translationX", drawerView.getTranslationX(), -drawerView.getMeasuredWidth());
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(contentView, "alpha", contentView.getAlpha(), 1.0f);
        animatorSet.setDuration(drawerView.getMeasuredWidth() / 2);
        animatorSet.playTogether(translateAnimator, alphaAnimator);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                animation = true;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                animation = false;
                view = VIEW_DRAWER;
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animatorSet.start();
        /*
        drawerView.setTranslationX(-drawerView.getMeasuredWidth());
        contentView.setAlpha(0.8f);*/
    }


    public void showContentView() {
        if (view == VIEW_SLIDING) {
            slidingView.setTranslationY(-slidingView.getMeasuredHeight());
            view = VIEW_CONTENT;
            contentView.setAlpha(0.0f);
        } else if (view == VIEW_DRAWER) {
            drawerView.setTranslationX(drawerView.getMeasuredWidth());
            view = VIEW_CONTENT;
            contentView.setAlpha(0.0f);
        }
    }

    public void setOnSwipeListener(ISwipeListener swipeRightListener) {
        this.swipeListener = swipeRightListener;
    }


    public void setOnContentViewClickListener(OnClickListener listener) {
        this.contentViewClickListener = listener;
    }


    public void setContentViewOnLongClickListener(OnLongClickListener longClickListener) {
        this.contentViewLongClickListener = longClickListener;
    }

}
