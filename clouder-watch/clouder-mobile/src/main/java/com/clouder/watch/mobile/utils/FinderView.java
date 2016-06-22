package com.clouder.watch.mobile.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.View;


/**
 * Created by yang_shoulai on 11/18/2015.
 */
public class FinderView extends View {
    private static final long ANIMATION_DELAY = 10L;
    private static final int OPAQUE = 255;
    private static final int MAX_RESULT_POINTS = 20;
    private int MIDDLE_LINE_WIDTH;
    private int MIDDLE_LINE_PADDING;
    private static final int SPEED_DISTANCE = 8;
    private Paint paint;
    private int CORNER_PADDING;
    private int slideTop;
    private int slideBottom;
    private boolean isFirst = true;
    private final int maskColor;
    private CameraManager cameraManager;
    private Bitmap mCornerTopLeft;
    private Bitmap mCornerTopRight;
    private Bitmap mCornerBottomLeft;
    private Bitmap mCornerBottomRight;
    private Bitmap mScanLexer;

    public FinderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.CORNER_PADDING = this.dip2px(context, 0.0F);
        MIDDLE_LINE_PADDING = this.dip2px(context, 20.0F);
        MIDDLE_LINE_WIDTH = this.dip2px(context, 3.0F);
        this.paint = new Paint(1);
        Resources resources = this.getResources();
        this.maskColor = -1440406235;
        this.mCornerTopLeft = BitmapFactory.decodeResource(resources, com.github.yoojia.qrcode.R.mipmap.scan_corner_top_left);
        this.mCornerTopRight = BitmapFactory.decodeResource(resources, com.github.yoojia.qrcode.R.mipmap.scan_corner_top_right);
        this.mCornerBottomLeft = BitmapFactory.decodeResource(resources, com.github.yoojia.qrcode.R.mipmap.scan_corner_bottom_left);
        this.mCornerBottomRight = BitmapFactory.decodeResource(resources, com.github.yoojia.qrcode.R.mipmap.scan_corner_bottom_right);
        this.mScanLexer = ((BitmapDrawable) this.getResources().getDrawable(com.github.yoojia.qrcode.R.mipmap.scan_laser)).getBitmap();
    }

    public void setCameraManager(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
    }

    public void onDraw(Canvas canvas) {
        if (this.cameraManager != null) {
            Rect frame = this.cameraManager.getFramingRect();
            if (frame != null) {
                this.drawCover(canvas, frame);
                this.drawEdges(canvas, frame);
                this.drawScanLaxer(canvas, frame);
                this.postInvalidateDelayed(10L, frame.left, frame.top, frame.right, frame.bottom);
            }
        }
    }

    private void drawScanLaxer(Canvas canvas, Rect frame) {
        if (this.isFirst) {
            this.isFirst = false;
            this.slideTop = frame.top;
            this.slideBottom = frame.bottom;
        }

        this.slideTop += 8;
        if (this.slideTop >= this.slideBottom) {
            this.slideTop = frame.top;
        }

        Rect lineRect = new Rect();
        lineRect.left = frame.left + MIDDLE_LINE_PADDING;
        lineRect.right = frame.right - MIDDLE_LINE_PADDING;
        lineRect.top = this.slideTop;
        lineRect.bottom = this.slideTop + MIDDLE_LINE_WIDTH;
        canvas.drawBitmap(this.mScanLexer, (Rect) null, lineRect, this.paint);
    }

    private void drawCover(Canvas canvas, Rect frame) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        this.paint.setColor(this.maskColor);
        canvas.drawRect(0.0F, 0.0F, (float) width, (float) frame.top, this.paint);
        canvas.drawRect(0.0F, (float) frame.top, (float) frame.left, (float) (frame.bottom + 1), this.paint);
        canvas.drawRect((float) (frame.right + 1), (float) frame.top, (float) width, (float) (frame.bottom + 1), this.paint);
        canvas.drawRect(0.0F, (float) (frame.bottom + 1), (float) width, (float) height, this.paint);
    }

    private void drawEdges(Canvas canvas, Rect frame) {
        this.paint.setColor(-1);
        this.paint.setAlpha(255);
        canvas.drawBitmap(this.mCornerTopLeft, (float) (frame.left + this.CORNER_PADDING), (float) (frame.top + this.CORNER_PADDING), this.paint);
        canvas.drawBitmap(this.mCornerTopRight, (float) (frame.right - this.CORNER_PADDING - this.mCornerTopRight.getWidth()), (float) (frame.top + this.CORNER_PADDING), this.paint);
        canvas.drawBitmap(this.mCornerBottomLeft, (float) (frame.left + this.CORNER_PADDING), (float) (2 + (frame.bottom - this.CORNER_PADDING - this.mCornerBottomLeft.getHeight())), this.paint);
        canvas.drawBitmap(this.mCornerBottomRight, (float) (frame.right - this.CORNER_PADDING - this.mCornerBottomRight.getWidth()), (float) (2 + (frame.bottom - this.CORNER_PADDING - this.mCornerBottomRight.getHeight())), this.paint);
    }

    public int dip2px(Context context, float dipValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5F);
    }
}
