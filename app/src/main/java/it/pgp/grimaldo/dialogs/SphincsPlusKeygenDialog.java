package it.pgp.grimaldo.dialogs;

import android.app.Dialog;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;

import it.pgp.grimaldo.R;
import it.pgp.grimaldo.VaultActivity;
import it.pgp.grimaldo.utils.RootHandler;

public class SphincsPlusKeygenDialog extends Dialog {

    EditText name;
    Button ok;


    String destPrv,destPub;

    public SphincsPlusKeygenDialog(VaultActivity context) {
        super(context);
        setContentView(R.layout.keygen_dialog);
        name = findViewById(R.id.keygenNameEditText);
        ok = findViewById(R.id.keygenOkButton);

        final File nativeExe = new File(context.getApplicationInfo().nativeLibraryDir, "libgrimald.so");
        final File workingDir = new File(context.getFilesDir(), VaultActivity.KEYS_DIR);
        workingDir.mkdirs();

        ok.setOnClickListener(v -> {
            destPrv = name.getText().toString()+VaultActivity.DEFAULT_PRV_EXT;
            destPub = name.getText().toString()+VaultActivity.DEFAULT_PUB_EXT;

            if(new File(workingDir,destPrv).exists() || new File(workingDir,destPub).exists()) {
                Toast.makeText(context, "A public and/or private key with the same already exists", Toast.LENGTH_LONG).show();
                return;
            }

            int returnCode;
            try {
                Process p = RootHandler.executeCommandSimple(
                        nativeExe.getAbsolutePath(),
                        workingDir,
                        false,
                        "gen --private_key "+destPrv+" --public_key "+destPub);
                returnCode = p.waitFor();
            } catch (Exception e) {
                e.printStackTrace();
                returnCode = -1111111111;
            }
            Toast.makeText(context, "Key generation "+(returnCode==0?"OK":"failed"), Toast.LENGTH_LONG).show();
            dismiss();
            context.idVaultAdapter.notifyDataSetChanged();
            if(returnCode==0) context.setResult(VaultActivity.UpdatedKeysResCode);
        });
    }
}
