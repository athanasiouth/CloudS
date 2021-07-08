package com.athanasioua.battleship.model.newp.model;

public class CallbackTask_old implements Runnable {

    private Runnable task = null;

    private Runnable callback = null;
    private boolean success = false;

    public CallbackTask_old(){ }

    public CallbackTask_old(Runnable task, Runnable callback) {
        this.task = task;
        this.callback = callback;
    }

    public void run() {
        task.run();
    }

    public Runnable getTask() {
        return task;
    }

    public void setTask(Runnable task) {
        this.task = task;
    }

    public Runnable getCallback() {
        return callback;
    }

    public void setCallback(Runnable callback) {
        this.callback = callback;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    /*public interface Callback {
        void complete();
    }*/
}
