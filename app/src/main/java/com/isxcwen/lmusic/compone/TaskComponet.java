package com.isxcwen.lmusic.compone;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class TaskComponet {
    private static Map<String, ReentrantLock> locks = new HashMap<>();
    private static ScheduledExecutorService service;

    public static void lock(String key){
        ReentrantLock reentrantLock = locks.get(key);
        if(reentrantLock == null){
            reentrantLock = new ReentrantLock();
            locks.put(key, reentrantLock);
        }
        reentrantLock.lock();
    }
    public static void unLock(String key){
        ReentrantLock reentrantLock = locks.get(key);
        if(reentrantLock !=null && reentrantLock.isLocked()){
            reentrantLock.unlock();
        }
    }
    public static void init() {
        if(service == null){
            service = new ScheduledThreadPoolExecutor(5, TaskComponet::createThread);
        }
    }

    private static Thread createThread(Runnable runnable){
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        return thread;
    }

    public static void registerScheduler(Runnable runnable, long delay, long period){
        init();
        service.scheduleWithFixedDelay(runnable, delay, period, TimeUnit.MILLISECONDS);
    }

    public static void registerDelay(Runnable runnable, long delay){
        init();
        service.schedule(runnable, delay, TimeUnit.MILLISECONDS);
    }

    public static void register(Runnable runnable){
        init();
        service.execute(runnable);
    }

    public static void destory(){
        if(service != null){
            service.shutdownNow();
            service = null;
            locks.clear();
        }
    }
}
