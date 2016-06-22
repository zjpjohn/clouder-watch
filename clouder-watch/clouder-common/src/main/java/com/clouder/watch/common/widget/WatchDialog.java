package com.clouder.watch.common.widget;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.clouder.watch.common.R;

/**
 * Created by yang_shoulai on 7/14/2015.
 */
public class WatchDialog extends Dialog {

    private Context mContext;

    private String title;

    private String message;

    private ICallbackListener callbackListener;

    private boolean hideTitle;

    public WatchDialog(Context context, String title, String message) {
        this(context, title, message, false);
    }

    public WatchDialog(Context context, String title, String message, boolean hidetitle) {
        super(context, R.style.dialog);
        this.mContext = context;
        this.title = title;
        this.message = message;
        this.hideTitle = hidetitle;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.watch_dialog_layout, null);
        TextView tvTitle = (TextView) view.findViewById(R.id.title);
        if (hideTitle) {
            tvTitle.setVisibility(View.GONE);
        } else {
            tvTitle.setVisibility(View.VISIBLE);
        }
        TextView tvMessage = (TextView) view.findViewById(R.id.message);
        TextView negative = (TextView) view.findViewById(R.id.btn_n);
        TextView positive = (TextView) view.findViewById(R.id.btn_p);
        tvTitle.setText(title);
        tvMessage.setText(message);
        negative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callbackListener != null) {
                    callbackListener.onNegativeClick(WatchDialog.this);
                } else {
                    WatchDialog.this.dismiss();
                }
            }
        });
        positive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callbackListener != null) {
                    callbackListener.onPositiveClick(WatchDialog.this);
                } else {
                    WatchDialog.this.dismiss();
                }
            }
        });
        setContentView(view);
    }


    public void setCallbackListener(ICallbackListener callbackListener) {
        this.callbackListener = callbackListener;
    }

    public interface ICallbackListener {

        void onNegativeClick(WatchDialog dialog);

        void onPositiveClick(WatchDialog dialog);
    }
}
