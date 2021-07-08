package com.athanasioua.battleship.model.newp;


import android.content.Context;

import com.athanasioua.battleship.model.newp.model.Indexing;
import com.athanasioua.battleship.model.newp.model.Repository;

public final class Prefs {

    private static Prefs mInstacce = null;


    public Prefs() {

    }

    public static Prefs getInstance(Context applicationContext) {
        if (mInstacce == null) {
            mInstacce = new Prefs();
        }
        return mInstacce;
    }


}
