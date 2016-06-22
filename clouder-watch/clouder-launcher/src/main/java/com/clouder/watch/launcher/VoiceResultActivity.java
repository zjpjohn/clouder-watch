package com.clouder.watch.launcher;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.view.WatchViewStub;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.clouder.watch.common.ui.SwipeRightActivity;
import com.clouder.watch.common.widget.WatchToast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yang_shoulai on 8/24/2015.
 */
public class VoiceResultActivity extends SwipeRightActivity {

    private WearableListView listView;

    private RecyclerView.Adapter adapter;

    private List<Plan> plans;

    private TextView mHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_result);
        listView = (WearableListView) findViewById(R.id.listView);
        mHeader = (TextView) findViewById(R.id.header);
        plans = getPlansFromVoice();
        adapter = new PlanAdapter(VoiceResultActivity.this, plans);
        listView.setAdapter(adapter);
        listView.addOnScrollListener(new WearableListView.OnScrollListener() {

            @Override
            public void onScroll(int i) {

            }

            @Override
            public void onAbsoluteScrollChange(int i) {
                if (i >= 0) {
                    mHeader.setY(-i);
                }
            }

            @Override
            public void onScrollStateChanged(int i) {

            }

            @Override
            public void onCentralPositionChanged(int i) {

            }
        });
    }

    public static class PlanAdapter extends WearableListView.Adapter {
        private LayoutInflater mInflater;

        private List<Plan> items;

        public PlanAdapter(Context context, List<Plan> plans) {
            this.mInflater = LayoutInflater.from(context);
            this.items = plans;

        }

        @Override
        public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new WearableListView.ViewHolder(mInflater.inflate(R.layout.item_voice_plan, null));
        }


        @Override
        public void onBindViewHolder(WearableListView.ViewHolder holder, final int position) {
            TextView time = (TextView) holder.itemView.findViewById(R.id.time);
            TextView subject = (TextView) holder.itemView.findViewById(R.id.subject);
            time.setText(items.get(position).time);
            subject.setText(items.get(position).subject);
            holder.itemView.setTag(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return this.items.size();
        }

    }

    private List<Plan> getPlansFromVoice() {
        List<Plan> plans = new ArrayList<>();
        plans.add(new Plan("10:30 am", getString(R.string.morning_meeting)));
        plans.add(new Plan("11:00 am", getString(R.string.reception_customer)));
        plans.add(new Plan("11:30 am", getString(R.string.dinner_with_customers)));
        plans.add(new Plan("12:00 am", getString(R.string.noon_break)));
        plans.add(new Plan("14:20 pm", getString(R.string.afternoon_tea)));
        plans.add(new Plan("17:50 pm", getString(R.string.knock_off)));
        return plans;
    }


    public static class Plan {

        public String time;
        public String subject;

        public Plan(String time, String subject) {
            this.time = time;
            this.subject = subject;
        }
    }

}
