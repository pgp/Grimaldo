package it.pgp.grimaldo.adapters;

import android.content.Context;
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

/**
 * Created by pgp on 12/02/17
 */

public class IdentitiesVaultAdapter extends BaseAdapter implements ListAdapter {
    private final VaultActivity vaultActivity;
    private final List<String> idsFilenames = new ArrayList<>();
    private final List<String> containsPubkeys = new ArrayList<>();
    private final File idsDir;

    // TODO on choosing private key, if public one is present, copy it as well
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

        showBtn.setOnClickListener(v -> {/* TODO show basic text dialog with file content*/});
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
