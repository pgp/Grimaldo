package it.pgp.grimaldo;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;

import it.pgp.grimaldo.adapters.IdentitiesVaultAdapter;
import it.pgp.grimaldo.dialogs.SphincsPlusKeygenDialog;
import it.pgp.grimaldo.enums.Permissions;
import it.pgp.grimaldo.utils.FileSelectFragment;
import it.pgp.grimaldo.utils.RootHandler;

/**
 * adapted from https://github.com/pgp/XFiles
 */

public class VaultActivity extends Activity implements FileSelectFragment.Callbacks {

    public static final String KEYS_DIR = "keys";
    public static final String DEFAULT_PRV_EXT = ".prvk";
    public static final String DEFAULT_PUB_EXT = ".pubk";

    public static final int ReqCode = 101103107;
    public static final int UpdatedKeysResCode = 103107109;

    public ListView idVaultListView;

    public IdentitiesVaultAdapter idVaultAdapter;

    public boolean checkDangerousPermissions() {
        EnumSet<Permissions> nonGrantedPerms = EnumSet.noneOf(Permissions.class);
        for (Permissions p : Permissions.values())
            if (checkSelfPermission(p.value()) != PackageManager.PERMISSION_GRANTED)
                nonGrantedPerms.add(p);

        return nonGrantedPerms.isEmpty();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length == 0) { // request cancelled
            Toast.makeText(this, "Storage permissions denied", Toast.LENGTH_SHORT).show();
            return;
        }

        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permissions denied", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Toast.makeText(this, "Storage permissions granted", Toast.LENGTH_SHORT).show();
        openFileSelector(null);
    }

    public void openFileSelector(View unused) {
        if(!checkDangerousPermissions()) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
            return;
        }

        String fragTag = getResources().getString(R.string.tag_fragment_FileSelect);

        // Set up a selector for file selection rather than directory selection.
        FileSelectFragment fsf = FileSelectFragment.newInstance(FileSelectFragment.Mode.FileSelector,
                R.string.alert_OK,
                R.string.alert_cancel,
                R.string.alert_file_select,
                R.drawable.lock,
                R.drawable.xf_dir_blu,
                R.drawable.xfiles_file_icon
        );
        fsf.setFilter(IdentitiesVaultAdapter.privateOnlyIdFilter);
        fsf.show(getFragmentManager(), fragTag);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.vault_list_layout);
        idVaultListView= findViewById(R.id.ids_List);
        findViewById(R.id.genNewIdentityBtn).setOnClickListener(v -> new SphincsPlusKeygenDialog(VaultActivity.this).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        idVaultAdapter = new IdentitiesVaultAdapter(this);
        idVaultListView.setAdapter(idVaultAdapter);
    }

    @Override
    public void onConfirmSelect(String absolutePath, String fileName) {
        if (absolutePath != null && fileName != null) {
            String[] prv_pub = new String[2]; // private key filename, public key filename
            if(fileName.endsWith(DEFAULT_PRV_EXT)) {
                prv_pub[0] = ""+fileName;
                prv_pub[1] = fileName.substring(0,fileName.length()-DEFAULT_PRV_EXT.length())+DEFAULT_PUB_EXT;
            }
            else if (fileName.endsWith(DEFAULT_PUB_EXT)) {
                prv_pub[0] = fileName.substring(0,fileName.length()-DEFAULT_PUB_EXT.length())+DEFAULT_PRV_EXT;
                prv_pub[1] = ""+fileName;
            }
            else throw new RuntimeException("Invalid key file extension");

            for(int i=0; i<prv_pub.length; i++) {
                File srcPath = new File(absolutePath,prv_pub[i]);
                if(i==0 && !srcPath.exists()) {
                    Toast.makeText(this,"At least the private key is needed for import (extension "+DEFAULT_PRV_EXT+")",Toast.LENGTH_SHORT).show();
                    return;
                }
                File destPath = new File(getFilesDir(), KEYS_DIR);
                destPath.mkdirs();
                destPath = new File(destPath,prv_pub[i]);

                if (destPath.exists()) {
                    Toast.makeText(this,"A key file with the same name already exists, remove it before adding this one",Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    RootHandler.copyFile(srcPath,destPath);
                }
                catch (IOException e) {
                    if(i==0) { // allow public key import to fail, just ignore it
                        Toast.makeText(this,"Keypair import error",Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }

            idVaultAdapter.notifyDataSetChanged();
            setResult(UpdatedKeysResCode);
            Toast.makeText(this,"Keypair imported successfully",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean isValid(String absolutePath, String fileName) {
        return true;
    }
}

