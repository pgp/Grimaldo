package it.pgp.grimaldo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import java.io.File;

import it.pgp.grimaldo.adapters.IdentitiesVaultAdapter;

public class MainActivity extends Activity {

    public static final String pLabel = "PASSPHRASE";
    public static final String ipLabel = "IP";
    public static final int PORT = 11112;
    EditText ipAddress;
    Spinner keySpinner;

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
        ipAddress = findViewById(R.id.ipAddress);
        keySpinner = findViewById(R.id.keySpinner);
        keySpinner.setAdapter(getAdapterWithFiles());
    }

    public void showVaultActivity(View unused) {
        startActivity(new Intent(this, VaultActivity.class));
    }

    public void unlock(View unused) {
        String ip = ipAddress.getText().toString();

//        new Thread(()->{
//            try {
//                int retcode = new ProcessBuilder(
//                        getApplicationInfo().nativeLibraryDir + "/libgrimald.so",
//                        pass,
//                        ip,
//                        PORT+"").start().waitFor();
//                runOnUiThread(()-> Toast.makeText(MainActivity.this,retcode==0?"Auth OK":"Auth failed, retcode "+retcode,Toast.LENGTH_SHORT).show());
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//                runOnUiThread(()-> Toast.makeText(MainActivity.this,"Error calling native process",Toast.LENGTH_SHORT).show());
//            }
//        }).start();
    }
}
