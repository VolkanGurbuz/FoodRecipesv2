package com.codingwithmitch.foodrecipes;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.LogRecord;

public class AppExecutors {

  private static AppExecutors instance;

  public static AppExecutors getInstance() {
    if (instance == null) {
      instance = new AppExecutors();
    }
    return instance;
  }

  public final Executor mDiskIO = Executors.newSingleThreadExecutor();
  public final Executor mMainThreadExecutor = new MainThreadExecutor();

  public Executor getmDiskIO() {
    return mDiskIO;
  }

  public Executor getmMainThreadExecutor() {
    return mMainThreadExecutor;
  }

  private static class MainThreadExecutor implements Executor {

    private Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    @Override
    public void execute(Runnable command) {
      mainThreadHandler.post(command);
    }
  }
}
