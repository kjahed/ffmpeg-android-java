package com.github.hiteshsondhi88.libffmpeg;

import android.content.Context;
import android.os.AsyncTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

class FFmpegLoadLibraryAsyncTask extends AsyncTask<Void, Integer, Boolean> {

    private final String baseUrl;
    private final String cpuArchNameFromAssets;
    private final FFmpegLoadBinaryResponseHandler ffmpegLoadBinaryResponseHandler;
    private final Context context;

    FFmpegLoadLibraryAsyncTask(Context context, String baseUrl, String cpuArchNameFromAssets, FFmpegLoadBinaryResponseHandler ffmpegLoadBinaryResponseHandler) {
        this.context = context;
        this.baseUrl = baseUrl;
        this.cpuArchNameFromAssets = cpuArchNameFromAssets;
        this.ffmpegLoadBinaryResponseHandler = ffmpegLoadBinaryResponseHandler;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        File ffmpegFile = new File(FileUtils.getFFmpeg(context));

        if (!ffmpegFile.exists()) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;

            try {
                URL url = new URL(baseUrl + "/" + cpuArchNameFromAssets + "/" + FileUtils.ffmpegFileName);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                    return false;

                int fileLength = connection.getContentLength();
                input = connection.getInputStream();
                output = new FileOutputStream(ffmpegFile, false);

                byte data[] = new byte[8192];
                long total = 0;
                int count;

                while ((count = input.read(data)) != -1) {
                    total += count;

                    if (fileLength > 0)
                        publishProgress((int) (total * 100 / fileLength));

                    output.write(data, 0, count);
                }

                if(!ffmpegFile.canExecute()) {
                    if (ffmpegFile.setExecutable(true)) {
                        return true;
                    }
                }

                return true;

            } catch (Exception e) {

                return false;

            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException e) {
                }

                if (connection != null)
                    connection.disconnect();
            }
        }

        return ffmpegFile.exists() && ffmpegFile.canExecute();
    }

    @Override
    protected void onPostExecute(Boolean isSuccess) {
        super.onPostExecute(isSuccess);
        if (ffmpegLoadBinaryResponseHandler != null) {
            if (isSuccess) {
                ffmpegLoadBinaryResponseHandler.onSuccess();
            } else {
                ffmpegLoadBinaryResponseHandler.onFailure();
            }
            ffmpegLoadBinaryResponseHandler.onFinish();
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        ffmpegLoadBinaryResponseHandler.onProgress(values[0]);
    }
}
