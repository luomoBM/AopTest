package com.lm.aoptest.event;

import android.support.annotation.IntDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by BM on 2018/5/23.
 * <p>
 * desc:
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface EventReceiver {
    int DEFAULT = 0;
    int MAIN = 1;
    int WORKER = 2;

    @IntDef({DEFAULT, MAIN, WORKER})
    @interface ThreadType {
    }

   /* @ThreadType */int thread() default DEFAULT;

    int value() default -10000;
}
