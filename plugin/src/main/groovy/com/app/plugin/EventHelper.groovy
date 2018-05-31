package com.app.plugin
/**
 * Created by BM on 2018/5/23.
 * <p>
 * desc:
 */
class EventHelper {

    public static final String ANNO_RECEIVER = "com.lm.aoptest.event.EventReceiver"
    public static final String ANNO_REGISTER = "com.lm.aoptest.event.EventRegister"
    public static final String ANNO_UN_REGISTER = "com.lm.aoptest.event.EventUnRegister"

    public static final def ON_CREATE = ["onCreate", "onActivityCreated"] as String[]
    public static final def ON_DESTROY = "onDestroy"

    public static final def ACTIVITY_ONCREATE = "\n" +
            "    protected void onCreate(Bundle savedInstanceState) {\n" +
            "        super.onCreate(savedInstanceState);\n" +
            "        %s\n" +
            "    }\n"

    public static final def FRAGMENT_ONCREATE = "\n" +
            "    public void onActivityCreated(Bundle savedInstanceState) {\n" +
            "        super.onActivityCreated(savedInstanceState);\n" +
            "        %s\n" +
            "    }\n"

    public static final def ACTIVITY_ONDESTROY = "\n" +
            "    protected void onDestroy() {\n" +
            "        super.onDestroy();\n" +
            "        %s\n" +
            "}\n"

    public static final def FRAGMENT_ONDESTROY = "\n" +
            "    public void onDestroy() {\n" +
            "        super.onDestroy();\n" +
            "        %s\n" +
            "}\n"

    public static final def PRE_SWITCH_STR = "public void call(Message msg) {\n" +
            "switch (msg.what){\n"


    public static final def IS_ACTIVITY_METHOD = "onRestart"

    static String getInsetRegistEventStr(int tag, int thread) {
        return String.format("EventCenterV2.getInstance().register(%d, (IEventCaller) this, %d);",
                tag, thread)
    }

    static String getInsetActivityRegistMethodStr(int tag, int thread) {
        return String.format(ACTIVITY_ONCREATE, getInsetRegistEventStr(tag, thread))
    }

    static String getInsetFragmentRegistMethodStr(int tag, int thread) {
        return String.format(FRAGMENT_ONCREATE, getInsetRegistEventStr(tag, thread))
    }


    static String getInsetUnRegistEventStr() {
        return "EventCenterV2.getInstance().unRegister((IEventCaller) this);"
    }

    static String getInsetActivityUnRegistMethodStr() {
        return String.format(ACTIVITY_ONDESTROY, getInsetUnRegistEventStr())
    }

    static String getInsetFragmentUnRegistMethodStr() {
        return String.format(FRAGMENT_ONDESTROY, getInsetUnRegistEventStr())
    }

}