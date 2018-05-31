package com.app.plugin

import javassist.CtClass
import javassist.CtMethod

import java.lang.annotation.Annotation

/**
 * Created by BM on 2018/5/24.
 * <p>
 * desc:
 */

class EventInfo {
    CtClass ctClz
    CtMethod onCreateMethod
    CtMethod onDestroyMethod
    CtMethod onRegistMethod
    CtMethod onUnRegistMethod

    Map<CtMethod, Annotation> eventMethods = new HashMap<>()

    boolean isActivity
//    int eventTag
//    int thread
}
