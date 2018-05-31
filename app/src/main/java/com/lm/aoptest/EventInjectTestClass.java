package com.lm.aoptest;

import android.content.Context;
import android.widget.Toast;

import com.lm.aoptest.event.EventReceiver;
import com.lm.aoptest.event.EventRegister;
import com.lm.aoptest.event.EventTag;
import com.lm.aoptest.event.EventUnRegister;

/**
 * Created by crizquan on 2018/5/31.
 * <p>
 * desc:
 */
public class EventInjectTestClass {


    private Context mContext;

    @EventRegister
    public void create(Context context) {

        mContext = context;
    }

    @EventUnRegister
    public void destroy() {

    }


    @EventReceiver(EventTag.ON_TEST_EVENT_ACTIVITY)
    public void methodEventReceiveClass(String str) {
        Toast.makeText(mContext, str, Toast.LENGTH_SHORT).show();
    }

}
