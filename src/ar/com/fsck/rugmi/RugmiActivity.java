package ar.com.fsck.rugmi;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class RugmiActivity extends Activity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.main);

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                String data = intent.getStringExtra(Intent.EXTRA_TEXT);
                if(intent.hasExtra(Intent.EXTRA_SUBJECT)) {
                    String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
                    Log.v("DEBUG SUBJECT", subject);
                }
                Log.v("DEBUG TEXT", data);
            } else {
                Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);

                // prepare service
                Intent uploadService = new Intent(getApplicationContext(), UploadService.class);
                uploadService.putExtra("uri", uri);
                startService(uploadService);
            }
        }

        finish();
    }

}
