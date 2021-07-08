package com.athanasioua.battleship.model.newp.model

import android.view.View

class BreadcrumbItem(var text: String, var listener: View.OnClickListener) : Comparable<Any?> {

    override operator fun compareTo(o: Any?): Int {
        return 0
    }

}