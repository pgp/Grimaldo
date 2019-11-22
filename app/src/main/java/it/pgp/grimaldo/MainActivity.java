package it.pgp.grimaldo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {

    public static final String pLabel = "PASSPHRASE";
    public static final String ipLabel = "IP";
    public static final int PORT = 11112;
    EditText ipAddress;
    EditText passphrase;
    SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sp = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        ipAddress = findViewById(R.id.ipAddress);
        passphrase = findViewById(R.id.passphrase);
        loadValues();
    }

    public void loadValues() {
        ipAddress.setText(sp.getString(ipLabel,""));
        passphrase.setText(sp.getString(pLabel,""));
    }

    public void saveValues(View unused) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(ipLabel, ipAddress.getText().toString());
        editor.putString(pLabel, passphrase.getText().toString());
        editor.apply();
    }

    public void unlock(View unused) {
        String ip = ipAddress.getText().toString();
        String pass = passphrase.getText().toString();

        new Thread(()->{
            try {
                int retcode = new ProcessBuilder(
                        getApplicationInfo().nativeLibraryDir + "/libgrimald.so",
                        pass,
                        ip,
                        PORT+"").start().waitFor();
                runOnUiThread(()-> Toast.makeText(MainActivity.this,retcode==0?"Auth OK":"Auth failed, retcode "+retcode,Toast.LENGTH_SHORT).show());
            }
            catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(()-> Toast.makeText(MainActivity.this,"Error calling native process",Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
