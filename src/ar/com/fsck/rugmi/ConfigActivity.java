package ar.com.fsck.rugmi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;

public class ConfigActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final EditText url = new EditText(this);
        final EditText key = new EditText(this);

        url.setHint("Url");
        key.setHint("Key");

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
        layoutParams.setMargins(10, 10, 10, 10);

        LinearLayout wrapLayout = new LinearLayout(this);

        wrapLayout.addView(url, layoutParams);
        wrapLayout.addView(key, layoutParams);
        wrapLayout.setOrientation(LinearLayout.VERTICAL);

        new AlertDialog.Builder(this)
            .setTitle("Configure Rugmi")
            //.setMessage("Set url and key")
            .setView(wrapLayout)
            .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
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
