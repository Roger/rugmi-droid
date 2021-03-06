package ar.com.fsck.rugmi;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class NotificationReceiveActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        String text = intent.getStringExtra("text");

        int sdk = android.os.Build.VERSION.SDK_INT;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("text label", text);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(getApplicationContext(), "Copied to clipboard", Toast.LENGTH_LONG).show();

        finish();
    }
}
