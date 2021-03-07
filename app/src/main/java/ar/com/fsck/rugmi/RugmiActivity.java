package ar.com.fsck.rugmi;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

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

                try {
                    String filename = getFileNameByUri(uri);
                    uploadService.putExtra("filename", filename);

                    InputStream inputStream = getApplicationContext().getContentResolver().openInputStream(uri);
                    UploadService.uploadData = IOUtils.toByteArray(inputStream);
                } catch (Exception e) {
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    uploadService.putExtra("exception", sw.toString());
                }

                startService(uploadService);
            }
        }

        finish();
    }

    public String getFileNameByUri(Uri uri) {
        String fileName = "unknown"; //default fileName

        if (uri == null) {
            return fileName;
        }

        Uri filePathUri = uri;
        if (uri.getScheme().toString().compareTo("content") == 0) {
            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(uri, null, null, null,
                        null);
            } catch (SecurityException e) {
                return uri.getLastPathSegment();
            }
            if (cursor != null && cursor.moveToFirst()) {
                // cursor contains three columns, _display_name, _size and _data
                // no idea why we used to get _data, but it's often null
                int column_index = cursor
                        .getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                filePathUri = Uri.parse(cursor.getString(column_index));
                return filePathUri.getLastPathSegment().toString();
            }
        } else if (uri.getScheme().compareTo("file") == 0) {
            return filePathUri.getLastPathSegment().toString();
        } else {
            return fileName + "_" + filePathUri.getLastPathSegment();
        }
        return fileName;
    }


}
