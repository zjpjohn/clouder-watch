/*****************************************************************************
 * HOPERUN PROPRIETARY INFORMATION
 * <p/>
 * The information contained herein is proprietary to HopeRun
 * and shall not be reproduced or disclosed in whole or in part
 * or used for any design or manufacture
 * without direct written authorization from HopeRun.
 * <p/>
 * Copyright (c) 2014 by HopeRun.  All rights reserved.
 *****************************************************************************/
package com.clouder.watch.call.ui;

import android.support.v4.view.PagerAdapter;
import android.view.View;

import java.util.List;

/**
 * ClassName: MyAdapter
 *
 * @description
 * @author xing_peng
 * @Date 2015-9-6
 *
 */
public class MyAdapter extends PagerAdapter {

    List<View> viewLists;

    public MyAdapter(List<View> lists) {
        viewLists = lists;
    }

    @Override
    public int getCount() { // get size
        return viewLists.size();
    }

    @Override
    public boolean isViewFromObject(View arg0, Object arg1) {
        return arg0 == arg1;
    }

    @Override
    public void destroyItem(View view, int position, Object object) // destroy Item
    {
        ((MyViewPager) view).removeView(viewLists.get(position));
    }

    @Override
    public Object instantiateItem(View view, int position) // instantiate Item
    {
        ((MyViewPager) view).addView(viewLists.get(position), 0);
        return viewLists.get(position);
    }
}
