package com.lm.aoptest.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * ***注意: 实时媒体，协议，游戏扫描，DB IO 等长期独占任务之调度不在此列
 * ExecutorCenter 提供以下线程供不同任务场景调度
 * 1. 最多 2x 耗时任务线程，按需生成／销毁
 * 2. 最多 4x 短时任务线程，按需生成/销毁
 * 3. 最多 2x IO线程，按需生成/销毁
 * 4. 1x 顺序任务线程
 * 5. 1x 定时任务线程
 */

public class ExecutorCenter {

    public static class Schedulers {

        private static ExecutorService ioScheduler = new ThreadPoolExecutor(1, 2,
                180L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
        private static ExecutorService computeScheduler = new ThreadPoolExecutor(1, 2,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
        private static ExecutorService workerScheduler = new ThreadPoolExecutor(1, 4,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
        private static ExecutorService sequentialScheduler = null;
        private static ScheduledExecutorService timebaseScheduler = null;

        //IO调度
        public static ExecutorService io() {
            return ioScheduler;
        }

        //耗时任务调度
        public static ExecutorService compute() {
            return computeScheduler;
        }

        //短时任务调度
        public static ExecutorService worker() {
            return workerScheduler;
        }

        //顺序任务调度
        public static ExecutorService sequencing() {
            synchronized (Schedulers.class) {
                if (sequentialScheduler == null) {
                    sequentialScheduler = Executors.newSingleThreadExecutor();
                }
            }
            return sequentialScheduler;
        }

        //定时任务调度
        public static ScheduledExecutorService timebase() {
            synchronized (Schedulers.class) {
                if (timebaseScheduler == null) {
                    timebaseScheduler = Executors.newSingleThreadScheduledExecutor();
                }
                return timebaseScheduler;
            }
        }

    }

    public static class Util {

        public static long currentThreadId() {
            return Thread.currentThread().getId();
        }

    }


}
