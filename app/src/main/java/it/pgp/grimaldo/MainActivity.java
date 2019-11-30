package it.pgp.grimaldo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;

import it.pgp.grimaldo.adapters.IdentitiesVaultAdapter;
import it.pgp.grimaldo.utils.RootHandler;

public class MainActivity extends Activity {

    static {
        // avoid messing up with content URIs
        StrictMode.setVmPolicy(StrictMode.VmPolicy.LAX);
    }

    public static final String ipLabel = "IP";
    public static final int PORT = 11112;
    EditText ipAddress;
    public Spinner keySpinner;
    SharedPreferences sp;

    public void loadValues() {
        ipAddress.setText(sp.getString(ipLabel,""));
    }

    public void saveValues(View unused) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(ipLabel, ipAddress.getText().toString());
        editor.apply();
    }

    protected ArrayAdapter<String> getAdapterWithFiles() {
        final File workingDir = new File(getFilesDir(), VaultActivity.KEYS_DIR);
        workingDir.mkdirs();
        File[] files = workingDir.listFiles(IdentitiesVaultAdapter.privateOnlyIdFilter);
        String[] filePaths = new String[files.length];
        for(int i=0;i<filePaths.length;i++)
            filePaths[i] = files[i].getName();
        return new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,filePaths);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sp = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        ipAddress = findViewById(R.id.ipAddress);
        keySpinner = findViewById(R.id.keySpinner);
        keySpinner.setAdapter(getAdapterWithFiles());
        loadValues();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void showVaultActivity(View unused) {
        startActivity(new Intent(this, VaultActivity.class));
    }

    public void unlock(View unused) {
        String ip = ipAddress.getText().toString();
        if(ip.isEmpty()) {
            Toast.makeText(this, "IP string is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(()->{
            int[] returnCode = new int[1];
            try {
                Process p = RootHandler.executeCommandSimple(
                        getApplicationInfo().nativeLibraryDir + "/libgrimald.so",
                        new File(getFilesDir(), VaultActivity.KEYS_DIR),
                        false,
                        "client -a "+ip+" -p 11112 -k "+keySpinner.getSelectedItem().toString());
                returnCode[0] = p.waitFor();
            } catch (Exception e) {
                e.printStackTrace();
                returnCode[0] = -1222222222;
            }
            runOnUiThread(()->Toast.makeText(this, "Auth "+(returnCode[0]==0?"command sent":"failed, return code "+returnCode[0]), Toast.LENGTH_LONG).show());
        }).start();
    }
}
