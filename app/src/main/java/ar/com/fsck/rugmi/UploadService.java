package ar.com.fsck.rugmi;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ParseException;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import androidx.core.app.NotificationCompat;

import android.util.Log;

public class UploadService extends IntentService {

    public UploadService() {
        super("UploadService");
    }

    public String getFileNameByUri(Uri uri) {
        String fileName = "unknown"; //default fileName

        if (uri == null) {
            return fileName;
        }

        Uri filePathUri = uri;
        if (uri.getScheme().toString().compareTo("content") == 0) {
            Cursor cursor = getContentResolver().query(uri, null, null, null,
                    null);
            if (cursor.moveToFirst()) {
                //Instead of "MediaStore.Images.Media.DATA" can be used "_data"
                int column_index = cursor
                        .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                filePathUri = Uri.parse(cursor.getString(column_index));
                fileName = filePathUri.getLastPathSegment().toString();
            }
        } else if (uri.getScheme().compareTo("file") == 0) {
            fileName = filePathUri.getLastPathSegment().toString();
        } else {
            fileName = fileName + "_" + filePathUri.getLastPathSegment();
        }
        return fileName;
    }

    // see http://androidsnippets.com/multipart-http-requests
    public String multipartRequest(String urlTo, String[] posts,
            InputStream fileInputStream, String fileName, String fileField)
            throws Exception {

        HttpURLConnection connection = null;
        DataOutputStream outputStream = null;
        InputStream inputStream = null;

        String twoHyphens = "--";
        String boundary = "*****" + Long.toString(System.currentTimeMillis())
                + "*****";
        String lineEnd = "\r\n";

        String result = "";

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;

        URL url = new URL(urlTo);
        connection = (HttpURLConnection) url.openConnection();

        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("User-Agent",
                "Rugmi-Droid HTTP Client 1.0");
        connection.setRequestProperty("Content-Type",
                "multipart/form-data; boundary=" + boundary);

        outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.writeBytes(twoHyphens + boundary + lineEnd);
        outputStream
                .writeBytes("Content-Disposition: form-data; name=\""
                        + fileField + "\"; filename=\"" + fileName + "\""
                        + lineEnd);
        outputStream.writeBytes("Content-Transfer-Encoding: binary"
                + lineEnd);
        outputStream.writeBytes(lineEnd);

        bytesAvailable = fileInputStream.available();
        bufferSize = Math.min(bytesAvailable, maxBufferSize);
        buffer = new byte[bufferSize];

        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        while (bytesRead > 0) {
            outputStream.write(buffer, 0, bufferSize);
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        }

        outputStream.writeBytes(lineEnd);

        int max = posts.length;
        for (int i = 0; i < max; i++) {
            outputStream.writeBytes(twoHyphens + boundary + lineEnd);
            String[] kv = posts[i].split("=");
            outputStream
                    .writeBytes("Content-Disposition: form-data; name=\""
                            + kv[0] + "\"" + lineEnd);
            outputStream.writeBytes("Content-Type: text/plain" + lineEnd);
            outputStream.writeBytes(lineEnd);
            outputStream.writeBytes(kv[1]);
            outputStream.writeBytes(lineEnd);
        }

        outputStream.writeBytes(twoHyphens + boundary + twoHyphens
                + lineEnd);

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("Connection Error Code: " + responseCode);
        }

        inputStream = connection.getInputStream();
        result = this.convertStreamToString(inputStream);

        fileInputStream.close();
        inputStream.close();
        outputStream.flush();
        outputStream.close();

        return result;
    }

    private String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void notificate(String title, String text) {
        Intent intent = new Intent(this, NotificationReceiveActivity.class);
        intent.putExtra("text", text);

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_FROM_BACKGROUND);

        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        Notification noti = new NotificationCompat.Builder(this)
            .setSmallIcon(R.drawable.ic_launcher)
            .addAction(R.drawable.ic_launcher, "Copy", pIntent)
            .setContentIntent(pIntent)
            .setContentTitle(title)
            .setAutoCancel(true)
            .setContentText(text)
            .build();

        NotificationManager mNotificationManager =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // mId allows you to update the notification later on.
        mNotificationManager.notify(42, noti);

    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Uri uri = (Uri) intent.getParcelableExtra("uri");
        String fileName = getFileNameByUri(uri);

        SharedPreferences prefs = getSharedPreferences("ConfigActivity", MODE_PRIVATE);

        String keyPref = prefs.getString("key", "");
        String urlPref = prefs.getString("url", "");

        String[] posts = { "key="+keyPref };

        InputStream inputStream;

        try {
            inputStream = getApplicationContext().getContentResolver()
                    .openInputStream(uri);

            String url = multipartRequest(urlPref, posts,
                    inputStream, fileName, "file");
            notificate("Uploaded", url);

        } catch (Exception e) {
            Log.e("MultipartRequest", "Multipart Form Upload Error");
            e.printStackTrace();
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            notificate("Error", sw.toString());
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }
}
