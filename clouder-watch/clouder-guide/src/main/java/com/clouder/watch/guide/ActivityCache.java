package com.clouder.watch.guide;

import android.app.Activity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yang_shoulai on 8/25/2015.
 */
public class ActivityCache {

    static final List<Activity> activities = new ArrayList<>();

    public static void add(Activity activity) {
        if (!activities.contains(activity)) {
            activities.add(activity);
        }
    }

    public void remove(Activity activity) {
        if (activities.contains(activity))
            activities.remove(activity);
    }


    public static void finishAll() {
        for (Activity activity : activities) {
            activity.finish();
        }

    }
}
