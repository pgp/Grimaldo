package it.pgp.grimaldo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

public class DoUnlockActivity extends Activity {

    /**
     * This seems to be needed as workaround for a bug appearing when energy saving mode is
     * active on some devices: the widget broadcast is received, but the native process libgrimald.so
     * keeps hanging until one starts MainActivity manually
     * (debug msg: Waiting for a blocking GC ProfileSaver)
     */

    public static final String LOGTAG = "DOUNLOCK";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        MainActivity.mainActivityContext = this;
        MainActivity.refreshToastHandler(this);

        Log.d(LOGTAG,"start challenge response");
        SharedPreferences sp = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        String ip = sp.getString(MainActivity.ipLabel,"");
        if(ip==null || ip.isEmpty()) {
            Toast.makeText(this, "No default IP address set", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String[] pks = MainActivity.getPrivateKeysNames(this);
        if(pks == null || pks.length == 0) {
            Toast.makeText(this, "No private keys found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String firstPrivateKey = pks[0]; // TODO to be replaced with something like getDefaultPrivateKey
        MainActivity.doUnlock(this,ip,firstPrivateKey);
        new Thread(()->{
            try {Thread.sleep(1000);} catch (InterruptedException ignored) {}
            runOnUiThread(this::finish);
        }).start();
    }
}
