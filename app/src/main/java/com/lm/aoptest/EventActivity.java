package com.lm.aoptest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.lm.aoptest.event.EventCenterV2;
import com.lm.aoptest.event.EventReceiver;
import com.lm.aoptest.event.EventTag;

/**
 * Created by crizquan on 2018/5/31.
 * <p>
 * desc:
 */
public class EventActivity extends AppCompatActivity {

    private EventInjectTestClass mEventInjectTestClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);

        mEventInjectTestClass = new EventInjectTestClass();
        mEventInjectTestClass.create(this);
    }

    @EventReceiver(EventTag.ON_TEST_EVENT_ACTIVITY)
    public void methodEventActivityReceive(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    @EventReceiver(value = EventTag.ON_TEST_EVENT_NORMAL_CLASS, thread = EventReceiver.MAIN)
    public void methodEventNormalClassReceive(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    public void onClickActivityEvent(View view) {
        EventCenterV2.getInstance().onEvent(EventTag.ON_TEST_EVENT_ACTIVITY, "来自activity事件对象");
    }

    public void onClickNormalClassEvent(View view) {
        EventCenterV2.getInstance().onEvent(EventTag.ON_TEST_EVENT_NORMAL_CLASS, "来自normalclass事件对象");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mEventInjectTestClass.destroy();
    }
}
