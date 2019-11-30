package it.pgp.grimaldo.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.TextView;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import it.pgp.grimaldo.R;
import it.pgp.grimaldo.VaultActivity;
import it.pgp.grimaldo.utils.RootHandler;

public class KeyInfoDialog extends Dialog {

    File prvk,pubk;
    CheckBox showHashed;
    TextView prvkcontent,pubkcontent;
    byte[] prvk_,pubk_;
    byte[] Hprvk_,Hpubk_;
    MessageDigest H;

    public KeyInfoDialog(Activity context, String prvkname /*already with extension*/) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.key_info_dialog);
        showHashed = findViewById(R.id.show_hashed_key_content);
        prvkcontent = findViewById(R.id.prvkcontent);
        pubkcontent = findViewById(R.id.pubkcontent);
        String pubkname = prvkname.substring(0,prvkname.length()- VaultActivity.DEFAULT_PRV_EXT.length())+VaultActivity.DEFAULT_PUB_EXT;
        File idsDir = new File(context.getFilesDir(), VaultActivity.KEYS_DIR);
        prvk = new File(idsDir,prvkname);
        pubk = new File(idsDir,pubkname);
        prvk_ = RootHandler.readAllFile(prvk);
        if(pubk.exists())
            pubk_ = RootHandler.readAllFile(pubk);
        try {
            H = MessageDigest.getInstance("SHA-512");
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        if(prvk_ != null) {
            Hprvk_ = H.digest(prvk_);
            prvkcontent.setText(RootHandler.bytesToHex(Hprvk_));
        }
        H.reset();
        if(pubk_ != null) {
            Hpubk_ = H.digest(pubk_);
            pubkcontent.setText(RootHandler.bytesToHex(Hpubk_));
        }

        showHashed.setOnCheckedChangeListener((compoundButton, b) -> {
            if(b) {
                if(prvk_ != null)
                    prvkcontent.setText(RootHandler.bytesToHex(Hprvk_));
                if(pubk_ != null)
                    pubkcontent.setText(RootHandler.bytesToHex(Hpubk_));
            }
            else {
                if(prvk_ != null)
                    prvkcontent.setText(RootHandler.bytesToHex(prvk_));
                if(pubk_ != null)
                    pubkcontent.setText(RootHandler.bytesToHex(pubk_));
            }
        });

        findViewById(R.id.closeButton).setOnClickListener(v->dismiss());
    }
}
