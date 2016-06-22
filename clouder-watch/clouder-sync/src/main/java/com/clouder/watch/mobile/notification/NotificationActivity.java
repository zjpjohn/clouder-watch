package com.clouder.watch.mobile.notification;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.clouder.watch.mobile.R;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "NotificationActivity";
    private GridViewPager GridPager;
    private View view1, view2, view3;
    private MyViewPager mViewPager;
    private static List<View> mLists;
    private TextView noNotificationPost;
    private static ImageView mFirstImage;
    private static ImageView mSecondImage;
    private int downX, downY, viewPagerX;
    private int screenHeight, textY, textMoveY;
    public static boolean viewpagerStatus = true;
    private int positions = 0;
    private List<Page> pages = new ArrayList<>();
    private int moveX, gridMoveX;
    private MyViewPageAdapter myPageAdapter;
    private int width;
    private boolean mCardScroll = false;
    public static boolean viewpagerEnable = true;
    private String uuids = null;
    private boolean remove = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.viewpager);
        init();
        disableImage();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        if (intent != null) {
            Serializable object = intent.getSerializableExtra("NOTIFICATION_DATA");
            if (object != null) {
                List<Page> list = (List<Page>) object;
                pages = list;
                uuids = pages.get(positions).getUuid();
                Log.d(TAG, "onResume uuid=" + uuids);
                Log.d(TAG, ", positions = " + positions);
                mCardAdapter.notifyDataSetChanged();
                noNotificationPost.setVisibility(View.GONE);
                findViewById(R.id.notify_none).setVisibility(View.GONE);
                mViewPager.setVisibility(View.VISIBLE);
            } else {
                noNotificationPost.setVisibility(View.VISIBLE);
//                noNotificationPost.setBackgroundResource(R.drawable.bg);
                mFirstImage.setVisibility(View.GONE);
                mSecondImage.setVisibility(View.GONE);
                mViewPager.setVisibility(View.INVISIBLE);
                findViewById(R.id.notify_none).setVisibility(View.VISIBLE);
                findViewById(R.id.notify_none).setClickable(true);
                findViewById(R.id.notify_none).setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        Log.d(TAG, "noNotificationPost--------onTouch");
                        WindowManager windowManager = getWindowManager();
                        Display display = windowManager.getDefaultDisplay();
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                Log.d(TAG, "noNotificationPost--------ACTION_DOWN" + textY);
                                screenHeight = display.getHeight();
                                textY = (int) event.getRawY();
                                break;

                            case MotionEvent.ACTION_UP:
                                Log.d(TAG, "noNotificationPost--------ACTION_UP");
                                if (textMoveY - textY > display.getHeight() / 4) {
                                    Log.d(TAG, "下滑删除：" + (textMoveY - textY));
                                    finish();
                                }
                                break;

                            case MotionEvent.ACTION_MOVE:
                                Log.d(TAG, "noNotificationPost--------ACTION_MOVE");
                                textMoveY = (int) event.getRawY();
                                break;
                        }
                        return false;
                    }
                });
            }
        }
    }

    private void init() {
        WindowManager wm = (WindowManager) getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE);
        width = wm.getDefaultDisplay().getWidth();
        mFirstImage = (ImageView) findViewById(R.id.first);
        mSecondImage = (ImageView) findViewById(R.id.second);
        mViewPager = (MyViewPager) findViewById(R.id.adv_pager);
        noNotificationPost = (TextView) findViewById(R.id.background_notification);
        mLists = new ArrayList<>();
        LayoutInflater inflate = getLayoutInflater();
        view1 = inflate.inflate(R.layout.viewpager_none, null);
        view2 = inflate.inflate(R.layout.activity_my, null);
        view3 = inflate.inflate(R.layout.item_see_on_phone, null);
        view3.findViewById(R.id.btn_see_on_phone).setOnClickListener(this);
        GridPager = (GridViewPager) view2.findViewById(R.id.pager);
        GridPager.setOnTouchListener(mGridTouchListener);
        GridPager.setAdapter(mCardAdapter);
        mLists.add(view1);
        mLists.add(view2);
        mLists.add(view3);
        myPageAdapter = new MyViewPageAdapter(mLists);
        mViewPager.setAdapter(myPageAdapter);
        mViewPager.setOnPageChangeListener(mViewPagerChangeListener);
        GridPager.setOnPageChangeListener(mGridPagerChangeListener);
        mViewPager.setCurrentItem(1);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Log.d(TAG, "onNewIntent");
        if (intent != null) {
            Serializable object = intent.getSerializableExtra("NOTIFICATION_DATA");
            if (object != null) {
                List<Page> list = (List<Page>) object;
                pages = list;

                mCardAdapter.notifyDataSetChanged();
                Log.d(TAG, ", positions = " + positions);
                uuids = pages.get(positions).getUuid();
                Log.d(TAG, "onNewIntent uuid=" + uuids);
                disableImage();
            }
        }
    }

    /*
       * 下滑删除监听事件
      */
    private View.OnTouchListener mGridTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    WindowManager windowManager = getWindowManager();
                    Display display = windowManager.getDefaultDisplay();
                    screenHeight = display.getHeight();
                    downX = (int) event.getRawX();
                    downY = (int) event.getRawY();
                    if (downY < screenHeight / 8) {
                        mCardScroll = true;
                    } else {
                        mCardScroll = false;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (downY < screenHeight / 8 && (gridMoveX - downY) > 100) {
                        Log.d(TAG, "下滑删除：" + (moveX - downX));
                        finish();
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    gridMoveX = (int) event.getRawY();
                    viewPagerX = (int) event.getRawX();
                    if (downY < screenHeight / 8) {
                        Log.d(TAG, "删除判断：" + (gridMoveX - downY));
                    }
                    break;
            }
            return mCardScroll;
        }
    };


    /*
     * ViewPager 左右滑动事件监听
    */
    private MyViewPager.OnPageChangeListener mViewPagerChangeListener = new MyViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {


        }

        @Override
        public void onPageSelected(int position) {
            Log.d(TAG, "*************," + position + ",pages.size+" + pages.size());
            switch (position) {
                case 0:
                    remove = true;
                    if (pages.size() > 0 && pages.size() > positions) {
                        Intent i = new Intent(NotificationActivity.this, SyncNotificationService.class);
                        i.putExtra("REMOVE_PAGE_UUID", uuids);
                        Log.d(TAG, "POSITION=" + positions + ",uuid:" + uuids);
                        NotificationActivity.this.startService(i);
                        pages.remove(positions);
//                        if (pages.size() > 0) {
//                            uuids = pages.get(GridPager.getCurrentItem().x).getUuid();
//                            Log.d(TAG, "REMOVE_PAGE_UUID_NEXT--------> " + uuids);
//                        }
                        disableImage();
                        TranslateAnimation translateAnimation = new TranslateAnimation(mViewPager.getTranslationX(), width, mViewPager.getTranslationY(), mViewPager.getTranslationY());
                        translateAnimation.setDuration(500);
                        translateAnimation.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
//                                mViewPager.setOverScrollMode(2);
//                                mViewPager.setCurrentItem(1);
                                mViewPager.setCurrentItem(1, false);
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }
                        });
                        mViewPager.startAnimation(translateAnimation);
                        mCardAdapter.notifyDataSetChanged();
                        if (pages.size() > 0) {
                            Log.d(TAG, "ppppoooooosssstion:" + positions);
                            if (positions == 0) {
                                Log.d(TAG, "0000000000000 : " + pages.get(0).getUuid());
                                uuids = pages.get(0).getUuid();
                                positions = 0;
                            } else {
                                uuids = pages.get(positions - 1).getUuid();
                                positions = positions - 1;
                                Log.d(TAG, ",CurrentPage =" + (positions - 1) + ",uuid =" + uuids);
                                Log.d(TAG, "111111111  22222222222 : " + pages.get(0).getUuid());
                            }

//                            else if (positions == (pages.size() - 1)) {
//                                uuids = pages.get(positions - 1).getUuid();
//                                Log.d(TAG, ",CurrentPage =" + (positions - 1) + ",uuid =" + uuids);
//                                Log.d(TAG, "111111111  22222222222 : " + pages.get(0).getUuid());
//                                positions = positions - 1;
                        }

                        if (pages.size() < 1) {
                            Log.d(TAG, "通知全部删除，退出通知页面");
                            finish();
                        }
                    }
                    break;
                case 1:
                    remove = false;
                    viewpagerStatus = false;
                    mFirstImage.setImageResource(R.drawable.circle_2);
                    mSecondImage.setImageResource(R.drawable.circle_1);
                    break;
                case 2:
                    viewpagerStatus = true;
                    mFirstImage.setImageResource(R.drawable.circle_1);
                    mSecondImage.setImageResource(R.drawable.circle_2);
                    break;
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

            Log.d(TAG, "ViewPager状态监听:" + state);
        }
    };

    /*
     * GridViewPager 卡片上下滑动事件监听
    */
    private GridViewPager.OnPageChangeListener mGridPagerChangeListener = new GridViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int i, int i1, float v, float v1, int i2, int i3) {
        }

        @Override
        public void onPageSelected(int i, int i1) {
            if (remove == false) {
                positions = i;
                uuids = pages.get(positions).getUuid();
                Log.d(TAG, ",onPageSelected =" + positions + ",uuid =" + uuids);
                disableImage();
                Log.d(TAG, "GridViewPager.onPageSelected" + i);
            } else {
                Log.d(TAG, "滑动删除时调用了onPageSelected");
            }
        }

        @Override
        public void onPageScrollStateChanged(int i) {

        }
    };

    private FragmentGridPagerAdapter mCardAdapter = new FragmentGridPagerAdapter(getFragmentManager()) {
        @Override
        public Fragment getFragment(int row, int col) {
            Page page = pages.get(row);
            if (page.getIconRes() != null) {
                MyCard fragment = MyCard.create(page.getTitleRes(), page.getTextRes(), page.getDate(), page.getTime(), page.getIconRes());
                return fragment;
            } else {
                MyCard fragment = MyCard.creates(page.getTitleRes(), page.getTextRes(), page.getDate(), page.getTime());
                return fragment;
            }

        }

        @Override
        public int getRowCount() {
            return pages == null ? 0 : pages.size();
        }

        @Override
        public int getColumnCount(int i) {
            return 1;
        }

//        @Override
//        public Drawable getBackgroundForPage(int row, int column) {
//            return getDrawable(backgrounds[row % backgrounds.length]);
//        }
    };

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_see_on_phone:
                Intent i = new Intent(NotificationActivity.this, SyncNotificationService.class);
                i.putExtra("OPEN_ON_PHONE", uuids);
                Log.d(TAG, "POSITION=" + positions + ",uuid=" + uuids);
                NotificationActivity.this.startService(i);
                break;
            default:
                break;
        }
    }

    private class MyViewPageAdapter extends PagerAdapter {

        List<View> mViews;

        public MyViewPageAdapter(List<View> views) {
            this.mViews = views;
        }

        @Override
        public int getCount() {
            return mViews.size();
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

        @Override
        public Object instantiateItem(View arg0, int arg1) {
            ((MyViewPager) arg0).addView(mViews.get(arg1), 0);
            return mViews.get(arg1);
        }

        @Override
        public void destroyItem(View arg0, int arg1, Object arg2) {
            ((MyViewPager) arg0).removeView(mViews.get(arg1));
        }
    }

    public static void enableImage() {
        mFirstImage.setVisibility(View.VISIBLE);
        mSecondImage.setVisibility(View.VISIBLE);
    }

    public static void disableImage() {
        mFirstImage.setVisibility(View.INVISIBLE);
        mSecondImage.setVisibility(View.INVISIBLE);
    }
}
