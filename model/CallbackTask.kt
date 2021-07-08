package com.athanasioua.battleship.model.newp.model


class CallbackTask : Runnable {
    var task: Runnable? = null
    var callback: Runnable? = null

    /*public interface Callback {
        void complete();
    }*/
    var isSuccess = false

    constructor() {}
    constructor(task: Runnable?, callback: Runnable?) {
        this.task = task
        this.callback = callback
    }

    override fun run() {
        task!!.run()
    }

}
