package com.lm.aoptest;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.lm.aoptest.event.EventCenterV2;
import com.lm.aoptest.event.EventTag;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void goEventActivity(View view) {
        Intent intent = new Intent(this, EventActivity.class);
        startActivity(intent);
    }

    public void dispatchEvent(View view) {
        EventCenterV2.getInstance().onEvent(EventTag.ON_TEST_EVENT_ACTIVITY, "activity");
        EventCenterV2.getInstance().onEvent(EventTag.ON_TEST_EVENT_NORMAL_CLASS, "normalclass");
    }

}
