package com.clouder.watch.mobile.notification;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.wearable.view.CardFrame;
import android.support.wearable.view.CardScrollView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.clouder.watch.mobile.R;


/**
 * Created by SYSTEM on 8/17/2015.
 */
public class MyCard extends Fragment {
    private static final String CONTENT_SAVED_STATE = "CardScrollView_content";
    public static final int EXPAND_UP = -1;
    public static final int EXPAND_DOWN = 1;
    public static final String KEY_TITLE = "CardFragment_title";
    public static final String KEY_TEXT = "CardFragment_text";
    public static final String KEY_ICON_RESOURCE = "CardFragment_icon";
    private CardFrame mCard;
    private CardScrollView mCardScroll;
    private int mCardGravity = 80;
    private boolean mExpansionEnabled = true;
    private float mExpansionFactor = 0F;
    private int mExpansionDirection = 1;
    private boolean mScrollToTop;
    private boolean mScrollToBottom;
    private final Rect mCardMargins = new Rect(0, 0, 0, 0);
    private Rect mCardPadding;
    private boolean mActivityCreated;
    private RelativeLayout wholeView, translate;
    private LinearLayout bg;
    private TextView text;
    private TextView time;
    private TextView date;
    private TextView timeTop;
    private ImageView msg;
    private LinearLayout times;
    private ImageView imageMore, imagePhone;
    //    private ScrollView scrollView;
    public static boolean card = false;

    public static MyCard create(CharSequence title, CharSequence text, CharSequence date, CharSequence time, byte[] iconRes) {
        MyCard fragment = new MyCard();
        Bundle args = new Bundle();
        if (title != null) {
            args.putCharSequence("CardFragment_title", title);
        }

        if (text != null) {
            args.putCharSequence("CardFragment_text", text);
        }
        if (time != null) {
            args.putCharSequence("CardFragment_date", date);
        }
        if (time != null) {
            args.putCharSequence("CardFragment_time", time);
        }
        if (iconRes != null) {
//            args.putInt("CardFragment_icon", iconRes);
            args.putParcelable("CardFragment_icon", Bytes2Bimap(iconRes));
        }
        fragment.setArguments(args);
        return fragment;
    }

    public static MyCard creates(CharSequence title, CharSequence text, CharSequence date, CharSequence time) {
        MyCard fragment = new MyCard();
        Bundle args = new Bundle();
        if (title != null) {
            args.putCharSequence("CardFragment_title", title);
        }

        if (text != null) {
            args.putCharSequence("CardFragment_text", text);
        }
        if (time != null) {
            args.putCharSequence("CardFragment_date", date);
        }
        if (time != null) {
            args.putCharSequence("CardFragment_time", time);
        }
//        if (iconRes != null) {
////            args.putInt("CardFragment_icon", iconRes);
//            args.putParcelable("CardFragment_icon", Bytes2Bimap(iconRes));
//        }
        fragment.setArguments(args);
        return fragment;
    }

    public void setExpansionEnabled(boolean enabled) {
        this.mExpansionEnabled = enabled;
        if (this.mCard != null) {
            this.mCard.setExpansionEnabled(this.mExpansionEnabled);
        }

    }

    public void setExpansionDirection(int direction) {
        this.mExpansionDirection = direction;
        if (this.mCard != null) {
            this.mCard.setExpansionDirection(this.mExpansionDirection);
        }

    }

    public void setCardGravity(int gravity) {
        this.mCardGravity = gravity & 112;
        if (this.mActivityCreated) {
            this.applyCardGravity();
        }

    }

    private void applyCardGravity() {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) this.mCard.getLayoutParams();
        lp.gravity = this.mCardGravity;
        this.mCard.setLayoutParams(lp);
    }

    public void setCardMargins(int left, int top, int right, int bottom) {
        this.mCardMargins.set(left, top, right, bottom);
        if (this.mActivityCreated) {
            this.applyCardMargins();
        }

    }

    public void setCardMarginTop(int top) {
        this.mCardMargins.top = top;
        if (this.mActivityCreated) {
            this.applyCardMargins();
        }

    }

    public void setCardMarginLeft(int left) {
        this.mCardMargins.left = left;
        if (this.mActivityCreated) {
            this.applyCardMargins();
        }

    }

    public void setCardMarginRight(int right) {
        this.mCardMargins.right = right;
        if (this.mActivityCreated) {
            this.applyCardMargins();
        }

    }

    public void setCardMarginBottom(int bottom) {
        this.mCardMargins.bottom = bottom;
        if (this.mActivityCreated) {
            this.applyCardMargins();
        }

    }

    private void applyCardMargins() {
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) this.mCard.getLayoutParams();
        if (this.mCardMargins.left != -1) {
            lp.leftMargin = this.mCardMargins.left;
        }

        if (this.mCardMargins.top != -1) {
            lp.topMargin = this.mCardMargins.top;
        }

        if (this.mCardMargins.right != -1) {
            lp.rightMargin = this.mCardMargins.right;
        }

        if (this.mCardMargins.bottom != -1) {
            lp.bottomMargin = this.mCardMargins.bottom;
        }

        this.mCard.setLayoutParams(lp);
    }

    private void applyPadding() {
        if (this.mCard != null) {
            this.mCard.setContentPadding(0, 0, 0, 0);
        }

    }

    public void setContentPadding(int left, int top, int right, int bottom) {
        this.mCardPadding = new Rect(left, top, right, bottom);
        this.applyPadding();
    }

    public Rect getContentPadding() {
        return new Rect(this.mCardPadding);
    }

    public void setContentPaddingLeft(int leftPadding) {
        this.mCardPadding.left = leftPadding;
        this.applyPadding();
    }

    public int getContentPaddingLeft() {
        return this.mCardPadding.left;
    }

    public void setContentPaddingTop(int topPadding) {
        this.mCardPadding.top = topPadding;
        this.applyPadding();
    }

    public int getContentPaddingTop() {
        return this.mCardPadding.top;
    }

    public void setContentPaddingRight(int rightPadding) {
        this.mCardPadding.right = rightPadding;
        this.applyPadding();
    }

    public int getContentPaddingRight() {
        return this.mCardPadding.right;
    }

    public void setContentPaddingBottom(int bottomPadding) {
        this.mCardPadding.bottom = bottomPadding;
        this.applyPadding();
    }

    public int getContentPaddingBottom() {
        return this.mCardPadding.bottom;
    }

    public void setExpansionFactor(float factor) {
        this.mExpansionFactor = factor;
        if (this.mCard != null) {
            this.mCard.setExpansionFactor(factor);
        }

    }

    public void scrollToTop() {
        if (this.mCardScroll != null) {
            this.mCardScroll.scrollBy(0, this.mCardScroll.getAvailableScrollDelta(-1));
        } else {
            this.mScrollToTop = true;
            this.mScrollToBottom = false;
        }

    }

    public void scrollToBottom() {
        if (this.mCardScroll != null) {
            this.mCardScroll.scrollBy(0, this.mCardScroll.getAvailableScrollDelta(1));
        } else {
            this.mScrollToTop = true;
            this.mScrollToBottom = false;
        }

    }

    public final View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mCardScroll = new CardScrollView(inflater.getContext());
        this.mCardScroll.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
        this.mCard = new CardFrame(inflater.getContext());
//        Drawable drawable = getResources().getDrawable(R.drawable.bg);
//        this.mCard.setBackground(drawable);
        this.mCard.getBackground().setAlpha(0);
        this.mCard.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        this.mCard.setExpansionEnabled(false);
        this.mCard.setExpansionFactor(this.mExpansionFactor);
        this.mCard.setExpansionDirection(this.mExpansionDirection);
        this.mCard.setContentPadding(0, 0, 0, 0);
        this.mCard.setPadding(0, 0, 0, 0);
        this.mCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("MyCard", "Card Click!");
                translate.setVisibility(View.GONE);
                RelativeLayout.LayoutParams layoutParam = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                layoutParam.setMargins(0, 0, 0, 0);
                bg.setLayoutParams(layoutParam);
                WindowManager wm = (WindowManager) getActivity()
                        .getSystemService(Context.WINDOW_SERVICE);
                int height = wm.getDefaultDisplay().getHeight();
                Animation animation = new TranslateAnimation(0, 0, height / 2, 0);
                animation.setDuration(2000);

                wholeView.startAnimation(animation);
                text.setMaxLines(20);
//                ViewGroup.LayoutParams lp;
//                lp = scrollView.findViewById(R.id.scrollView).getLayoutParams();
//                lp.width = ActionBar.LayoutParams.MATCH_PARENT;
//                lp.height = ActionBar.LayoutParams.MATCH_PARENT;
//                scrollView.setLayoutParams(lp);
                imageMore.setVisibility(View.GONE);
                msg.setVisibility(View.GONE);
//                imagePhone.setVisibility(View.VISIBLE);
                times.setVisibility(View.VISIBLE);
                timeTop.setVisibility(View.INVISIBLE);
                NotificationActivity.enableImage();
                mCard.setClickable(false);
                NotificationActivity.viewpagerEnable = false;
            }
        });

        if (this.mCardPadding != null) {
            this.applyPadding();
        }

        this.mCardScroll.addView(this.mCard);
        if (this.mScrollToTop || this.mScrollToBottom) {
            this.mCardScroll.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    MyCard.this.mCardScroll.removeOnLayoutChangeListener(this);
                    if (MyCard.this.mScrollToTop) {
                        MyCard.this.mScrollToTop = false;
                        MyCard.this.scrollToTop();
                    } else if (MyCard.this.mScrollToBottom) {
                        MyCard.this.mScrollToBottom = false;
                        MyCard.this.scrollToBottom();
                    }

                }
            });
        }

        Bundle contentSavedState = null;
//        if (savedInstanceState != null) {
//            contentSavedState = savedInstanceState.getBundle("CardScrollView_content");
//        }

        View content = this.onCreateContentView(inflater, this.mCard, contentSavedState);
        if (content != null) {
            this.mCard.addView(content);
        }

        return this.mCardScroll;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.mActivityCreated = true;
        this.applyCardMargins();
        this.applyCardGravity();
    }

    public void onDestroy() {
        this.mActivityCreated = false;
        super.onDestroy();
    }

    public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.activity_notification, container, false);
        Bundle args = this.getArguments();
        if (args != null) {
            wholeView = (RelativeLayout) view.findViewById(R.id.view);
            translate = (RelativeLayout) view.findViewById(R.id.translate);
            bg = (LinearLayout) view.findViewById(R.id.bg);
            text = (TextView) view.findViewById(R.id.text);
            time = (TextView) view.findViewById(R.id.timeDate);
            date = (TextView) view.findViewById(R.id.timeTop);
            TextView title = (TextView) view.findViewById(R.id.title);
//            scrollView = (ScrollView) view.findViewById(R.id.scrollView);
            imageMore = (ImageView) view.findViewById(R.id.imageMore);
            msg = (ImageView) view.findViewById(R.id.largeIcon);
            imagePhone = (ImageView) view.findViewById(R.id.imagePhone);
            times = (LinearLayout) view.findViewById(R.id.time);
            timeTop = (TextView) view.findViewById(R.id.timeTop);
            if (args.containsKey("CardFragment_title") && title != null) {
                title.setText(args.getCharSequence("CardFragment_title"));
            }

            if (args.containsKey("CardFragment_text")) {
                if (text != null) {
                    text.setText(args.getCharSequence("CardFragment_text"));
                }
            }
            if (args.containsKey("CardFragment_date")) {
                if (text != null) {
                    date.setText(args.getCharSequence("CardFragment_date"));
                }
            }
            if (args.containsKey("CardFragment_time")) {
                if (text != null) {
                    time.setText(args.getCharSequence("CardFragment_time"));
                }
            }

            if (args.containsKey("CardFragment_icon") && title != null) {
//                title.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, args.getInt("CardFragment_icon"), 0);
                Bitmap bitmap = args.getParcelable("CardFragment_icon");
                Drawable drawable = new BitmapDrawable(bitmap);
                view.findViewById(R.id.largeIcon).setBackground(drawable);
            }
        }

        return view;
    }

    public static Bitmap Bytes2Bimap(byte[] b) {
        if (b.length != 0) {
            return BitmapFactory.decodeByteArray(b, 0, b.length);
        } else {
            return null;
        }
    }

}
