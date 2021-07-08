package com.athanasioua.battleship.model.newp.model;

import androidx.annotation.NonNull;
import android.view.View;

public class BreadcrumbItem_old implements Comparable {
    private String text;
    private View.OnClickListener listener;

    public BreadcrumbItem_old(String text, View.OnClickListener listener) {
        this.text = text;
        this.listener = listener;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public View.OnClickListener getListener() {
        return listener;
    }

    public void setListener(View.OnClickListener listener) {
        this.listener = listener;
    }

    @Override
    public int compareTo(@NonNull Object o) {
        return 0;
    }
}
