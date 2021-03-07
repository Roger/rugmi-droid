package ar.com.fsck.rugmi;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class ConfigActivity extends Activity {
    private void askForPermission() {
        String perm = Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{perm}, 1);
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getPreferences(MODE_PRIVATE); 
        final SharedPreferences.Editor editor = prefs.edit();

        String key = prefs.getString("key", "");
        String url = prefs.getString("url", ""); 

        final EditText urlText = new EditText(this);
        final EditText keyText = new EditText(this);

        urlText.setHint("Url");
        keyText.setHint("Key");

        urlText.setText(url);
        keyText.setText(key);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
        layoutParams.setMargins(10, 10, 10, 10);

        LinearLayout wrapLayout = new LinearLayout(this);

        wrapLayout.addView(urlText, layoutParams);
        wrapLayout.addView(keyText, layoutParams);
        wrapLayout.setOrientation(LinearLayout.VERTICAL);

        new AlertDialog.Builder(this)
            .setTitle("Configure Rugmi")
            //.setMessage("Set url and key")
            .setView(wrapLayout)
            .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    editor.putString("key", keyText.getText().toString());
                    editor.putString("url", urlText.getText().toString());
                    editor.commit();
                    askForPermission();
                    finish();
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    finish();
                }
            })
            .show();
    }
}
