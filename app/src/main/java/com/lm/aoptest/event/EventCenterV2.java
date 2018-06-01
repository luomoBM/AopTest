package com.lm.aoptest.event;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;

import com.lm.aoptest.util.ExecutorCenter;
import com.lm.aoptest.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by crizquan on 2018/5/23.
 * <p>
 * desc:
 */

public class EventCenterV2 {
    private final String TAG = "EventCenterV2";

    private ConcurrentHashMap<Integer, CopyOnWriteArrayList<SparseArray<IEventCaller>>> eventMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<IEventCaller, Integer> searchTagMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<IEventCaller, SparseArray<IEventCaller>> searchSparseMap = new ConcurrentHashMap<>();

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private static EventCenterV2 sInstance = new EventCenterV2();

    private EventCenterV2() {

    }

    public static EventCenterV2 getInstance() {
        return sInstance;
    }

    public void register(int eventTag, IEventCaller caller) {
        register(eventTag, caller, EventReceiver.DEFAULT);
    }

    public void register(int eventTag, IEventCaller caller, @EventReceiver.ThreadType int thread) {
        if (caller == null) {
            Log.w(TAG, StringUtils.format("regist null caller, tag = %d", eventTag));
            return;
        }
        SparseArray<IEventCaller> callerList = new SparseArray<>();
        callerList.put(thread, caller);
        CopyOnWriteArrayList<SparseArray<IEventCaller>> exist = eventMap.get(eventTag);
        if (exist != null) {
            exist.add(callerList);
        } else {
            CopyOnWriteArrayList<SparseArray<IEventCaller>> handlerList = new CopyOnWriteArrayList<>();
            handlerList.add(callerList);
            eventMap.put(eventTag, handlerList);
        }
        searchTagMap.put(caller, eventTag);
        searchSparseMap.put(caller, callerList);
    }

    public void unRegister(int eventTag) {
        CopyOnWriteArrayList<SparseArray<IEventCaller>> handlerList = eventMap.get(eventTag);
        if (handlerList != null) {
            for (SparseArray<IEventCaller> handler : handlerList) {
                IEventCaller caller = handler.valueAt(0);
                if (caller != null) {
                    searchTagMap.remove(caller);
                    searchSparseMap.remove(caller);
                }
            }
            eventMap.remove(eventTag);
            return;
        }
        Log.w(TAG, StringUtils.format("warning, unRegister null caller, eventTag = %d", eventTag));
    }

    public void unRegister(IEventCaller caller) {
        if (caller == null) {
            Log.w(TAG, StringUtils.format("warning,  unRegister null caller"));
            return;
        }
        Integer eventTag = searchTagMap.get(caller);
        SparseArray<IEventCaller> handler = searchSparseMap.get(caller);
        if (eventTag == null || handler == null) {
            Log.w(TAG, StringUtils.format("fail unregist eventTag null or handler null, handler = %s, caller = %s",
                    handler == null ? "null" : handler.toString(), eventTag == null ? "null" : eventTag.toString()));
            return;
        }
        CopyOnWriteArrayList<SparseArray<IEventCaller>> exist = eventMap.get(eventTag);
        if (exist != null) {
            if (exist.remove(handler)) {
                searchTagMap.remove(caller);
                searchSparseMap.remove(caller);
                return;
            }
            Log.w(TAG, StringUtils.format("fail unregist non exist handler = %s, caller = %s", handler.toString(), caller.toString()));
            return;
        }
        Log.w(TAG, StringUtils.format("fail unregist non exist eventTag = %d", eventTag));
    }

    public void onEvent(int tag) {
        onEvent(tag, null);
    }
    public void onEvent(int tag, Object data) {
        CopyOnWriteArrayList<SparseArray<IEventCaller>> handlerList = eventMap.get(tag);
        if (handlerList == null) {
            return;
        }
        Message m = mHandler.obtainMessage();
        m.obj = data;
        m.what = tag;
        Log.d(TAG, StringUtils.format("on event tag = &d, size = %d", tag, handlerList.size()));
        for (SparseArray<IEventCaller> caller : handlerList) {
            handlerCall(m, caller.keyAt(0), caller.valueAt(0));
        }
    }

    private void handlerCall(final Message message, int thread, final IEventCaller caller) {
        switch (thread) {
            case EventReceiver.DEFAULT:
                caller.call(message);
                break;
            case EventReceiver.MAIN:
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        caller.call(message);
                    }
                });
                break;
            case EventReceiver.WORKER:
                ExecutorCenter.Schedulers.worker().execute(new Runnable() {
                    @Override
                    public void run() {
                        caller.call(message);
                    }
                });
                break;
        }
    }
}
