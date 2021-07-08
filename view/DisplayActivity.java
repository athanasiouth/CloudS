package com.athanasioua.battleship.model.newp.view;

import android.Manifest;
import android.app.Activity;

import androidx.core.app.NotificationCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.athanasioua.battleship.model.newp.model.Repository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.athanasioua.battleship.R;
import com.athanasioua.battleship.model.newp.Utils;
import com.athanasioua.battleship.model.newp.adapter.DisplayItemsAdapter;
import com.athanasioua.battleship.model.newp.model.ActiveRepository;
import com.athanasioua.battleship.model.newp.model.BreadcrumbItem;
import com.athanasioua.battleship.model.newp.model.CallbackTask;
import com.athanasioua.battleship.model.newp.model.DisplayItem;
import com.athanasioua.battleship.model.newp.model.EncFile;
import com.athanasioua.battleship.model.newp.model.Indexing;
import com.athanasioua.battleship.model.newp.viewmodel.EncDec;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.SecretKey;

import butterknife.Bind;
import butterknife.ButterKnife;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;

public class DisplayActivity extends AppCompatActivity {

    private static final String ADD_REPO_DIALOG_TAG = "add_dialog_tag";
    private static final String PASSPHRASE_DIALOG_TAG = "passphrase_tag";
    private static final String GENERIC_DIALOG_TAG = "generic_dialog_tag";

    @Bind(R.id.main_rel)
    RelativeLayout mainRel;
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.search_btn)
    FloatingActionButton searchBtn;
    @Bind(R.id.search_top_rel)
    RelativeLayout searchTopRel;
    @Bind(R.id.search_img_view)
    ImageView searchImgView;
    @Bind(R.id.search_cancel_btn)
    TextView searchCancelBtn;
    @Bind(R.id.search_query_edit_txt)
    EditText searchQueryEditTxt;
    @Bind(R.id.breadcrumb_lin)
    LinearLayout breadcrumbLin;
    @Bind(R.id.no_matches_txt)
    TextView noMatchesTxt;
    @Bind(R.id.display_items_recycler)
    RecyclerView displayItemsRecycler;
    @Bind(R.id.add_btn)
    FloatingActionButton addBtn;
    @Bind(R.id.add_repo_btn)
    FloatingActionButton addRepoBtn;
    @Bind(R.id.add_file_btn)
    FloatingActionButton addFileBtn;

    private static final int READ_REQUEST_CODE = 42;


    private EncDec encDec;
    private boolean isFABOpen = false;
    private boolean isSearchOpen = false;
    private AlertDialog progressDialog;

    private DisplayItemsAdapter displayItemsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_activity);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setNavigationOnClickListener(view -> onBackPressed());

        encDec = new EncDec();
        encDec.setActivity(this);
        //encDec.getPublicIndex().observe(this, this::onIndexingChanged);
        encDec.getDisplayItemsList().observe(this, this::onDisplayItemsListChanged);
        encDec.getActiveRepository().observe(this, this::onActiveRepositoryChanged);
        encDec.getBreadcrumb().observe(this, this::onBreadcrumbChanged);
        encDec.getJustEncryptedFile().observe(this, this::onEncryptedFileCreatedChanged);

        Utils.setContext(this);


        StaggeredGridLayoutManager gridLayoutManager =
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        displayItemsAdapter = new DisplayItemsAdapter(this, new ArrayList<>(),encDec);
        displayItemsRecycler.setLayoutManager(gridLayoutManager);
        displayItemsRecycler.setAdapter(displayItemsAdapter);


        addBtn.setOnClickListener(v -> {
            if(!isFABOpen){
                showFABMenu();
            }else{
                closeFABMenu();
            }
        });

        mainRel.setOnClickListener(v -> {
            if(isFABOpen){
               closeFABMenu();
            }
        });

        addRepoBtn.setOnClickListener(v->{
            Log.e("Thanos","----- addRepoBtn -----");
            closeFABMenu();
            AddRepoDialog dialog = AddRepoDialog.newInstance(this);
            dialog.show(getSupportFragmentManager(), ADD_REPO_DIALOG_TAG);
        });

        addFileBtn.setOnClickListener(v->{
            Log.e("Thanos","----- addFileBtn -----");
            if(encDec.getActiveRepository().getValue() != null) {
                performFileSearch();
            } else {
                GenericMessageDialog dialog = GenericMessageDialog.newInstance(this, getResources().getString(R.string.select_repository));
                dialog.show(getSupportFragmentManager(), GENERIC_DIALOG_TAG);
            }
            closeFABMenu();
        });


        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_search_normal);
        drawable.setColorFilter(Color.parseColor(getResources().getString(R.color.white)), PorterDuff.Mode.SRC_IN);

        searchBtn.setOnClickListener(v -> {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);

            searchTopRel.animate().alpha(1f);
            searchBtn.animate().alpha(0f).withEndAction(()->{
                searchBtn.setVisibility(View.GONE);
            });
            isSearchOpen = true;
        });

        searchImgView.setImageDrawable(drawable);
        searchImgView.setOnClickListener(v->{
            hideSoftKeyboard(this);
            if(!searchQueryEditTxt.getText().toString().equals("")) {
                //showProgressDialog(true);
                encDec.searchFor(searchQueryEditTxt.getText().toString());
                searchImgView.setVisibility(View.GONE);
                searchCancelBtn.setVisibility(View.VISIBLE);
                encDec.setSearchActive(true);
            } else {
                searchCanceled();
            }
        });

        searchCancelBtn.setOnClickListener(v-> {
            searchCanceled();
            encDec.goBack();
        });


        handlePermisionsCode(45);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(Utils.getIsLoggedIn() && !Utils.getIsFileSelection() ) {
            handlePermisionsCode(45);
        }
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    @Override
    public void onBackPressed() {
        if(encDec.getActiveRepository().getValue() == null && !isSearchOpen) {
            super.onBackPressed();
        }else{
            if(isSearchOpen) searchCanceled();
            /**
             *
             *  Go up a level
             *
             */
             encDec.goBack();
        }
    }

    private void handlePermisionsCode(int permisionsCode) {
        String[] PERMISSIONS = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };
        if (hasPermissions(this, PERMISSIONS)) {
            switch (permisionsCode) {
                case 45:
                    /** create encryption key to store the credentials **/
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            SecretKey sKey = Utils.getPassphraseSecretKey("passPhraseKey");
                            if(sKey == null) {
                                Utils.createPassphraseKey("passPhraseKey");
                                PassphraseDialog dialog = PassphraseDialog.newInstance(this);
                                dialog.show(getSupportFragmentManager(), PASSPHRASE_DIALOG_TAG);
                            } else {
                                encDec.appInitialisationFlow();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    break;
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(PERMISSIONS, permisionsCode);
            }
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 45:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    handlePermisionsCode(requestCode);
                }
                break;

        }

    }

    public void addRepository(String repositoryName){
        encDec.addRepository(repositoryName);
    }

    public void addPassphrase(String passphrase){
        encDec.addPassphrase(DisplayActivity.this,passphrase);
    }

    private void onDisplayItemsListChanged(ArrayList<DisplayItem> itemsList) {
        if(itemsList != null) {

            //TODO CHANGE THE UI -> notify adapters etc
            Log.e("Thanos", "----- indexing updated -----");
            displayItemsAdapter.setItems(itemsList);
            /*if(itemsList.size() == 0) {
                noMatchesTxt.setVisibility(View.VISIBLE);
                if(isSearchOpen)
                    noMatchesTxt.setText(getResources().getString(R.string.no_matches_found));
            } else {
                noMatchesTxt.setVisibility(View.GONE);
            }*/
        }
    }

    private void onBreadcrumbChanged(ArrayList<BreadcrumbItem> breadcrumb) {
        breadcrumbLin.removeAllViews();
        breadcrumbLin.addView(
                createClickableTextView("Home",
                        v-> encDec.goToRepoInBreadcrumb(null)
                )
        );
        if(breadcrumb != null && breadcrumb.size() > 0) {
            for(BreadcrumbItem breadcrumbItem : breadcrumb) {
                breadcrumbLin.addView(
                        createClickableTextView(" / ",
                                v->{}
                        )
                );
                breadcrumbLin.addView(
                        createClickableTextView(breadcrumbItem.getText(),
                                breadcrumbItem.getListener()
                        )
                );
            }
        }
    }

    private void onIndexingChanged(Indexing indexing) {
        if(indexing != null) {
            Log.e("Thanos", "----- indexing updated -----");
        }
    }

    private void onActiveRepositoryChanged(Repository repository) {
        if(repository != null ) {
            Log.e("Thanos", "----- active repo updated -----");
            noMatchesTxt.setVisibility(View.GONE);
            searchCanceled();
            encDec.createDisplayItemsList(encDec.getActiveRepository().getValue().getRepositories(), encDec.getActiveRepository().getValue().getFiles());
        } else {
            encDec.createDisplayItemsList(encDec.getPublicIndex().getValue().getRepositories(), null);
        }
    }


    private void onEncryptedFileCreatedChanged(EncFile encFile) {
        if(encFile != null){
            encDec.saveFileToIndex(encFile);
        }
    }

    private void showFABMenu(){
        isFABOpen=true;
        addRepoBtn.animate().translationY(-getResources().getDimension(R.dimen.standard_55));
        addFileBtn.animate().translationY(-getResources().getDimension(R.dimen.standard_105));
    }

    private void closeFABMenu(){
        isFABOpen=false;
        addRepoBtn.animate().translationY(0);
        addFileBtn.animate().translationY(0);
    }


    private void searchCanceled(){
        if(isSearchOpen) {
            hideSoftKeyboard(this);
            isSearchOpen = false;
            encDec.setSearchActive(false);
            searchImgView.setVisibility(View.VISIBLE);
            searchCancelBtn.setVisibility(View.GONE);
            searchQueryEditTxt.setText("");
            noMatchesTxt.setVisibility(View.GONE);
            searchBtn.setVisibility(View.VISIBLE);
            searchTopRel.animate().alpha(0f).withEndAction(() -> {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            });
            searchBtn.animate().alpha(1f);
        }
    }

    public void hideSoftKeyboard(Activity activity) {
        if(activity != null && activity.getCurrentFocus() != null) {
            InputMethodManager inputMethodManager =
                    (InputMethodManager) activity.getSystemService(
                            Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(
                    activity.getCurrentFocus().getWindowToken(), 0);
        }
    }

    private TextView createClickableTextView(String text, View.OnClickListener listener){
        final TextView view = new TextView(this);
        view.setClickable(true);
        view.setText(text);
        view.setOnClickListener(listener);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP,12);
        view.setAllCaps(true);
        view.setGravity(Gravity.CENTER);
        return view;
    }

    public void performFileSearch() {

        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Filter to show only images, using the image MIME data type.
        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        intent.setType("*/*");
        Utils.setIsFileSelection(true);
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                Context context = this;
                Activity activity = this;
                showProgressDialog(true);

                /**
                 *
                 *  Encrypt the file in separate thread, so the UI thread is not blocked
                 *
                 */
                final ExecutorService executorService = Executors.newCachedThreadPool();
                CallbackTask callbackTask = new CallbackTask();
                Uri finalUri = uri;
                callbackTask.setTask(
                        () -> {
                            try {
                                encDec.addFile( activity,Utils.getFilePath(context, finalUri));
                                Utils.setIsFileSelection(false);
                            } catch (URISyntaxException e) {
                                e.printStackTrace();
                            }
                            callbackTask.getCallback().run();
                        });
                callbackTask.setCallback(
                        () -> {
                            showProgressDialog(false);

                            try {

                                createNotificationChannel();


                                String fPath = Utils.getFilePath(context, finalUri);
                                String[] srcFileNameArr = fPath.substring(fPath.lastIndexOf("/") + 1, fPath.length()).split("\\.");

                                Intent intent = new Intent(this, DisplayActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);



                                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "1")
                                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                                        .setContentTitle(getString(R.string.notification_title))
                                        .setContentText(String.format( getString(R.string.notification_desc), srcFileNameArr[0] ))
                                        .setContentIntent(pendingIntent)
                                        .setAutoCancel(true); // clear notification when clicked
                                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

                                notificationManager.notify((int) Math.random(), mBuilder.build());

                            } catch (Exception e) {
                                e.printStackTrace();
                            }


                        });
                executorService.submit(callbackTask);
            }
        }
    }

    public void showProgressDialog(boolean show) {
        if(progressDialog == null) {
            LayoutInflater li = LayoutInflater.from(this);
            View promptsView = li.inflate(R.layout.custom_dialog_progress, null);


            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogThemeProgress);
            builder.setView(promptsView);
            promptsView.setFocusable(false);
            promptsView.setClickable(false);

            progressDialog = builder.create();
            progressDialog.setCanceledOnTouchOutside(false);
        }
        if(progressDialog != null) {
            if (show)
                progressDialog.show();
            else
                progressDialog.dismiss();
        }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "File Encryption";
            String description = "";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("1", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


}
