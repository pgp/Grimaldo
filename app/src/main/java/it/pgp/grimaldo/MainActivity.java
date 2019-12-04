package it.pgp.grimaldo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
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

    public static Context mainActivityContext;
    public static final int toastHandlerTag = 17192329;
    public static Handler toastHandler;
    public static void refreshToastHandler(Context context) {
        if (toastHandler == null) toastHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == toastHandlerTag) {
                    Log.d("handleMessage", "Received toastmessage");
                    Toast.makeText(context,""+msg.obj,Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    public static void showToastOnUIWithHandler(String s) {
        Message m = new Message();
        m.obj = s;
        m.what = toastHandlerTag;
        toastHandler.sendMessage(m);
    }

    public void loadValues() {
        ipAddress.setText(sp.getString(ipLabel,""));
    }

    public void saveValues(View unused) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(ipLabel, ipAddress.getText().toString());
        editor.apply();
    }

    public static String[] getPrivateKeysNames(Context context) {
        final File workingDir = new File(context.getFilesDir(), VaultActivity.KEYS_DIR);
        workingDir.mkdirs();
        File[] files = workingDir.listFiles(IdentitiesVaultAdapter.privateOnlyIdFilter);
        String[] fileNames = new String[files.length];
        for(int i=0;i<fileNames.length;i++)
            fileNames[i] = files[i].getName();
        return fileNames;
    }

    protected ArrayAdapter<String> getAdapterWithFiles() {
        return new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,getPrivateKeysNames(this));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivityContext = this;
        refreshToastHandler(mainActivityContext);
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
        mainActivityContext = null;
    }

    public void showVaultActivity(View unused) {
        startActivityForResult(new Intent(this, VaultActivity.class), VaultActivity.ReqCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == VaultActivity.ReqCode && resultCode == VaultActivity.UpdatedKeysResCode)
            keySpinner.setAdapter(getAdapterWithFiles());
    }

    public static void doUnlock(Context context, String ip, String keyName) {
        new Thread(()->{
            int[] returnCode = new int[1];
            try {
                Process p = RootHandler.executeCommandSimple(
                        context.getApplicationInfo().nativeLibraryDir + "/libgrimald.so",
                        new File(context.getFilesDir(), VaultActivity.KEYS_DIR),
                        false,
                        "client -a "+ip+" -p 11112 -k "+keyName);
                returnCode[0] = p.waitFor();
            } catch (Exception e) {
                e.printStackTrace();
                returnCode[0] = -1222222222;
            }
            showToastOnUIWithHandler("Auth "+(returnCode[0]==0?"command sent":"failed, return code "+returnCode[0]));
        }).start();
    }

    public void unlock(View unused) {
        String ip = ipAddress.getText().toString();
        if(ip.isEmpty()) {
            Toast.makeText(this, "IP string is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        doUnlock(this,ip,keySpinner.getSelectedItem().toString());
    }
}
