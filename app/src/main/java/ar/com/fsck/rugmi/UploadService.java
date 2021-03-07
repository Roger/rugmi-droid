package ar.com.fsck.rugmi;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.util.Log;

public class UploadService extends IntentService {

    private static final String CHANNEL_ID = "rugmi_notifications";

    public UploadService() {
        super("UploadService");
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
        createNotificationChannel();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void createNotificationChannel() {
        // copypasted from docs
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void notificate(int mid, String title, String text) {
        Intent intent = new Intent(this, NotificationReceiveActivity.class);
        intent.putExtra("text", text);

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_FROM_BACKGROUND);

        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        Notification noti = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .addAction(R.drawable.ic_launcher, "Copy", pIntent)
            .setContentIntent(pIntent)
            .setContentTitle(title)
            .setAutoCancel(true)
            .setContentText(text)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
            .build();

        NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(this);

        mNotificationManager.notify(mid, noti);

    }

    public void notificateException(Exception e) {
        e.printStackTrace();
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        notificate(0, "Error", sw.toString());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Uri uri = (Uri) intent.getParcelableExtra("uri");
        String fileName = intent.getStringExtra("filename");
        byte[] data = intent.getByteArrayExtra("data");
        String exceptionText = intent.getStringExtra("exception");

        if (exceptionText != null) {
            notificate(0, "Error", exceptionText);
            return;
        }

        SharedPreferences prefs = getSharedPreferences("ConfigActivity", MODE_PRIVATE);

        String keyPref = prefs.getString("key", "");
        String urlPref = prefs.getString("url", "");

        String[] posts = { "key="+keyPref };

        InputStream inputStream;

        try {
            inputStream = new ByteArrayInputStream(data);
            String url = multipartRequest(urlPref, posts,
                    inputStream, fileName, "file");
            notificate(1,"Uploaded", url);

        } catch (Exception e) {
            Log.e("MultipartRequest", "Multipart Form Upload Error");
            notificateException(e);
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }
}
