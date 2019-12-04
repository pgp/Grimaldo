package it.pgp.grimaldo.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import it.pgp.grimaldo.R;
import it.pgp.grimaldo.VaultActivity;
import it.pgp.grimaldo.dialogs.KeyInfoDialog;

public class IdentitiesVaultAdapter extends BaseAdapter implements ListAdapter {
    private final VaultActivity vaultActivity;
    private final List<String> idsFilenames = new ArrayList<>();
    private final List<String> containsPubkeys = new ArrayList<>();
    private final File idsDir;

    public static final FilenameFilter idFilter = (dir, name) -> name.endsWith(VaultActivity.DEFAULT_PRV_EXT) ||
            name.endsWith(VaultActivity.DEFAULT_PUB_EXT);

    public static final FilenameFilter privateOnlyIdFilter = (dir, name) -> name.endsWith(VaultActivity.DEFAULT_PRV_EXT);

    public IdentitiesVaultAdapter(final VaultActivity vaultActivity) {
        this.vaultActivity = vaultActivity;
        idsDir = new File(vaultActivity.getFilesDir(), VaultActivity.KEYS_DIR);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return idsFilenames.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, final View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) vaultActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.id_list_item, null);
        }
        String idFilename = idsFilenames.get(position);
        String contains_pubkey = containsPubkeys.get(position);

        //Handle TextView and display string from your list
        TextView filename = view.findViewById(R.id.id_listitem_filename);
        TextView p = view.findViewById(R.id.id_listitem_contains_pubkey);

        filename.setText(idFilename);
        p.setText(contains_pubkey);

        //Handle buttons and add onClickListeners
        ImageButton showBtn = view.findViewById(R.id.id_listitem_show);
        ImageButton deleteBtn = view.findViewById(R.id.id_listitem_delete);
        ImageButton shareBtn = view.findViewById(R.id.id_listitem_share);

        showBtn.setOnClickListener(v -> new KeyInfoDialog(vaultActivity, idsFilenames.get(position)).show());
        deleteBtn.setOnClickListener(v -> {
            String prvkname = idsFilenames.get(position);
            String pubkname = prvkname.substring(0,prvkname.length()-VaultActivity.DEFAULT_PRV_EXT.length())+VaultActivity.DEFAULT_PUB_EXT;
            File f = new File(idsDir,prvkname);
            File g = new File(idsDir,pubkname);
            g.delete(); // public key may not be present, don't indicate error
            boolean deleted = f.delete();
            String message=deleted?"Deleted!":"Delete error";
            Toast.makeText(vaultActivity,message,Toast.LENGTH_SHORT).show();
            notifyDataSetChanged();
            vaultActivity.setResult(VaultActivity.UpdatedKeysResCode);
        });
        shareBtn.setOnClickListener(v -> {
            String prvkname = idsFilenames.get(position);
            String pubkname = prvkname.substring(0,prvkname.length()-VaultActivity.DEFAULT_PRV_EXT.length())+VaultActivity.DEFAULT_PUB_EXT;
            File g = new File(idsDir,pubkname);
            if(!g.exists()) {
                Toast.makeText(vaultActivity, "No public key available for this item", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent sharingIntent = new Intent();
            sharingIntent.setAction(Intent.ACTION_SEND);
            sharingIntent.setType("*/*");
            sharingIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(g));
            sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            vaultActivity.startActivity(Intent.createChooser(sharingIntent, "Share file using"));
        });

        return view;
    }

    @Override
    public void notifyDataSetChanged() {
        idsFilenames.clear();
        containsPubkeys.clear();
        File[] files = idsDir.listFiles(privateOnlyIdFilter);
        // DEBUG
        if (files != null) {
            for (File x : files) {
                String y = x.getAbsolutePath();
                idsFilenames.add(x.getName());
                if(new File(y.substring(0,y.length()-VaultActivity.DEFAULT_PRV_EXT.length())+VaultActivity.DEFAULT_PUB_EXT).exists())
                    containsPubkeys.add("P"); //
                else containsPubkeys.add("X");
            }
        }

        super.notifyDataSetChanged();
    }
}
