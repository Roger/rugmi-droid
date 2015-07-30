package ar.com.fsck.rugmi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class NotificationReceiveActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        String url = intent.getStringExtra("url");

        int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(url);
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE); 
            android.content.ClipData clip = android.content.ClipData.newPlainText("text label", url);
            clipboard.setPrimaryClip(clip);
        }

        Toast.makeText(getApplicationContext(), "Copied to clipboard", Toast.LENGTH_LONG).show();

        finish();
    }
}
