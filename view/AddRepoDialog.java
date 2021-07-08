package com.athanasioua.battleship.model.newp.view;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.athanasioua.battleship.R;


public class AddRepoDialog extends DialogFragment {

    private TextInputLayout repoNameLayout;
    private TextInputEditText repoNameEditText;

    private AlertDialog alertDialog;

    private String repoName;


    private View rootView;
    private DisplayActivity activity;

    public static AddRepoDialog newInstance(DisplayActivity activity) {
        AddRepoDialog dialog = new AddRepoDialog();
        dialog.activity = activity;
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        initViews();
        alertDialog = new AlertDialog.Builder(getContext())
                .setView(rootView)
                .setTitle(R.string.repo_dialog_title)
                .setCancelable(false)
                .setPositiveButton(R.string.done, null)
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
                .inflate(R.layout.add_repo_dialog, null, false);

        repoNameLayout = rootView.findViewById(R.id.layout_repo_name);
        repoNameEditText = rootView.findViewById(R.id.et_repo_name);
        addTextWatchers();
    }

    private void onDialogShow(AlertDialog dialog) {
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(v -> { onDoneClicked(); });

        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        repoNameEditText.requestFocus();

    }

    private void onDoneClicked() {
        if (isAValidName(repoNameLayout, repoName)) {
            //add repo
            activity.addRepository(repoName);
            dismiss();
        }
    }

    private boolean isAValidName(TextInputLayout layout, String name) {
        if (TextUtils.isEmpty(name)) {
            layout.setErrorEnabled(true);
            layout.setError(getString(R.string.game_dialog_empty_name));
            return false;
        }

        layout.setErrorEnabled(false);
        layout.setError("");
        return true;
    }

    private void addTextWatchers() {
        repoNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                repoName = s.toString();
            }
        });
    }
}
