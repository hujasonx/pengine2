package com.phonygames.pengine.util;

import com.badlogic.gdx.Gdx;

public class PPostableTaskQueue {
  private final Object lock = new Object();
  private PPostableTask currentTask = null;
  private PList<PPostableTask> tasks = new PList<>();
  private Thread thread;

  public void enqueue(PPostableTask task) {
    synchronized (lock) {
      tasks.add(task);
      startNextTaskIfNeeded();
    }
  }

  private void startNextTaskIfNeeded() {
    if (currentTask == null && !tasks.isEmpty()) {
      currentTask = tasks.removeIndex(0);
      Gdx.app.postRunnable(new Runnable() {
        @Override public void run() {
          currentTask.intro();
          new Thread(new Runnable() {
            @Override public void run() {
              currentTask.middle();
              Gdx.app.postRunnable(new Runnable() {
                @Override public void run() {
                  currentTask.end();
                  synchronized (lock) {
                    currentTask = null;
                    startNextTaskIfNeeded();
                  }
                }
              });
            }
          }).start();
        }
      });
    }
  }
}
