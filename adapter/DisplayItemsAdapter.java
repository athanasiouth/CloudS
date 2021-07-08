package com.athanasioua.battleship.model.newp.adapter;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.athanasioua.battleship.R;
import com.athanasioua.battleship.model.newp.Utils;
import com.athanasioua.battleship.model.newp.model.DisplayItem;
import com.athanasioua.battleship.model.newp.model.EncFile;
import com.athanasioua.battleship.model.newp.viewmodel.EncDec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class DisplayItemsAdapter extends RecyclerView.Adapter<DisplayItemsAdapter.ViewHolder> {

    private Context context;
    private ArrayList<DisplayItem> items;

    public EncDec viewModel;

    public DisplayItemsAdapter(Context context, ArrayList<DisplayItem> items, EncDec viewModel){
        this.context = context;
        this.items = items;
        this.viewModel = viewModel;
    }

    @NonNull
    @Override
    public DisplayItemsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.display_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull DisplayItemsAdapter.ViewHolder holder, int position) {
        final DisplayItem item = items.get(position);
        if(item != null) {
            holder.name.setText(item.getText());
            Drawable drawable;
            if (item.getType().equals("repo")) {
                drawable = ContextCompat.getDrawable(context, R.mipmap.ic_folder_white).mutate();
            } else {
                drawable = ContextCompat.getDrawable(context, R.mipmap.ic_file_white).mutate();
            }
            drawable.setColorFilter(Color.parseColor("#cccccc"), PorterDuff.Mode.SRC_IN);
            holder.image.setImageDrawable(drawable);
            holder.itemLin.setOnClickListener(v -> {
                if (item.getType().equals("repo")) {
                    viewModel.chooseRepository(item);
                } else {
                    //TODO what happens when a file is chosen
                    PopupMenu.OnMenuItemClickListener listener = new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            switch (menuItem.getItemId()) {
                                case R.id.view_item:
                                    viewFile(item);
                                    return true;
                                case R.id.save_item:
                                    // do your code
                                    return true;
                                case R.id.upload_item:
                                    // do your code
                                    return true;
                                case R.id.delete_item:
                                    deleteFile(item);
                                    return true;
                                default:
                                    return false;
                            }
                        }
                    };

                      viewModel.chooseFile(context, holder.itemLin, listener);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        protected LinearLayout itemLin;
        protected ImageView image;
        protected TextView name;

        public ViewHolder(View view) {
            super(view);
            itemLin = (LinearLayout) view.findViewById(R.id.item_lin);
            image = (ImageView) view.findViewById(R.id.item_image_view);
            name = (TextView) view.findViewById(R.id.name_text_view);
        }
    }

    public void setItems(ArrayList<DisplayItem> itemsList){
        items = itemsList;

        notifyDataSetChanged();
    }

    private void sortList(){
        Collections.sort(items, (a, b) -> {
            if (a.getTimestamp() < b.getTimestamp()) {
                return -1;
            }else if (a.getTimestamp() > b.getTimestamp()) {
                return 1;
            }
            return 0;
        });
    }

    private void viewFile(DisplayItem item){
        EncFile file = item.getParentRepository().getFiles().get(
                item.getParentRepository().findFileById(item.getParentRepository().getFiles(),
                        item.getId()));
        Utils.decryptFile(new EncFile(item.getId(),
                file.getName(),
                file.getExtension()),
               true);

        MimeTypeMap myMime = MimeTypeMap.getSingleton();
        Intent newIntent = new Intent(Intent.ACTION_VIEW);


        String mimeType = myMime.getMimeTypeFromExtension(file.getExtension());
        try {
            File filelocation = new File( context.getCacheDir(), new String(Utils.decryptString(Base64.decode(file.getName(), Base64.URL_SAFE)))+"."+file.getExtension());
            Uri uri = FileProvider.getUriForFile(
                    context,
                    context.getApplicationContext()
                            .getPackageName() + ".provider", filelocation);
            newIntent.setDataAndType(uri,mimeType);
        } catch (Exception e) {
            e.printStackTrace();
        }
        newIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            context.startActivity(newIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "No handler for this type of file.", Toast.LENGTH_LONG).show();
        }
    }

    private void deleteFile(DisplayItem item){

        EncFile file = item.getParentRepository().getFiles().get(
                item.getParentRepository().findFileById(item.getParentRepository().getFiles(),
                        item.getId()));

        File fileToDelete = null;
        boolean fileDeleted = false;
        try {
            fileToDelete = new File(Utils.encryptedFilePath, new String(Base64.decode(file.getName(),Base64.URL_SAFE))+".enc");
            fileToDelete.delete();
            if(fileToDelete.exists()){
                try {
                    fileToDelete.getCanonicalFile().delete();
                    if(fileToDelete.exists()){
                        context.deleteFile( fileToDelete.getName() );
                        fileDeleted = true;
                    } else {
                        fileDeleted = true;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                fileDeleted = true;
            }

            if(fileDeleted) {
                viewModel.deleteFile(
                        item.getParentRepository().findFileById(item.getParentRepository().getFiles(),
                        item.getId()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
