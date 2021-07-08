package com.athanasioua.battleship.model.newp.view;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.athanasioua.battleship.R;


public class GenericMessageDialog extends DialogFragment {

    private TextView genericMessageText;

    private AlertDialog alertDialog;


    private View rootView;
    private DisplayActivity activity;
    private String message;

    public static GenericMessageDialog newInstance(DisplayActivity activity, String message) {
        GenericMessageDialog dialog = new GenericMessageDialog();
        dialog.activity = activity;
        dialog.message = message;
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        initViews();
        alertDialog = new AlertDialog.Builder(getContext())
                .setView(rootView)
                .setTitle("")
                .setCancelable(false)
                .setPositiveButton(R.string.ok, null)
                .create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setCancelable(false);
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                                          @Override
                                          public void onShow(DialogInterface dialog) {
                                              onDialogShow(alertDialog);
                                          }
                                      });
        return alertDialog;
    }

    private void initViews() {
        rootView = LayoutInflater.from(getContext())
                .inflate(R.layout.generic_message_dialog, null, false);

        genericMessageText = rootView.findViewById(R.id.generic_message_text);
        genericMessageText.setText(message);

    }

    private void onDialogShow(AlertDialog dialog) {
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(v -> { onDoneClicked(); });
    }

    private void onDoneClicked() {
        dismiss();
    }




}
