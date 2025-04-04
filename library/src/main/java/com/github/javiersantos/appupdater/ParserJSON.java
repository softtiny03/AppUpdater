package com.github.javiersantos.appupdater;

import android.util.Log;

import com.github.javiersantos.appupdater.objects.Update;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;

class ParserJSON {
    private URL jsonUrl;

    private static final String KEY_LATEST_VERSION = "latestVersion";
    private static final String KEY_LATEST_VERSION_CODE = "latestVersionCode";
    private static final String KEY_RELEASE_NOTES = "releaseNotes";
    private static final String KEY_URL = "url";

    public ParserJSON(String url) {
        try {
            this.jsonUrl = new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

    }

    public Update parse(){

        try {
            JSONObject json = readJsonFromUrl();
            Update update = new Update();
            update.setLatestVersion(json.getString(KEY_LATEST_VERSION).trim());
            update.setLatestVersionCode(json.optInt(KEY_LATEST_VERSION_CODE));
            JSONArray releaseArr = json.optJSONArray(KEY_RELEASE_NOTES);
            if (releaseArr != null) {
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < releaseArr.length(); ++i) {
                    builder.append(releaseArr.getString(i).trim());
                    if (i != releaseArr.length() - 1)
                        builder.append(System.getProperty("line.separator"));
                }
                update.setReleaseNotes(builder.toString());
            }
            URL url = new URL(json.getString(KEY_URL).trim());
            update.setUrlToDownload(url);
            return update;
        } catch (IOException e) {
            Log.e("AppUpdater", "The server is down or there isn't an active Internet connection.", e);
        } catch (JSONException e) {
            Log.e("AppUpdater", "The JSON updater file is mal-formatted. AppUpdate can't check for updates.");
        }

        return null;
    }


    private String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    private JSONObject readJsonFromUrl() throws IOException, JSONException {
        InputStream inputStream = null;
        try {

            HttpURLConnection connection = (HttpURLConnection) this.jsonUrl.openConnection();
            connection.setInstanceFollowRedirects(false);
            int statusCode = connection.getResponseCode();
            if (statusCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    statusCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    statusCode == HttpURLConnection.HTTP_SEE_OTHER ||
                    statusCode == 307 ||
                    statusCode == 308) {
                String redirectUrl = connection.getHeaderField("Location");
                this.jsonUrl=new URL(redirectUrl);
                connection = (HttpURLConnection) this.jsonUrl.openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.getResponseCode();
                if (statusCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    statusCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    statusCode == HttpURLConnection.HTTP_SEE_OTHER ||
                    statusCode == 307 ||
                    statusCode == 308) {
                    redirectUrl = connection.getHeaderField("Location");
                    this.jsonUrl=new URL(redirectUrl);
                    connection = (HttpURLConnection) this.jsonUrl.openConnection();
                    connection.setInstanceFollowRedirects(false);
                    connection.getResponseCode();
                }
            }
            //InputStream is = this.jsonUrl.openStream();
            inputStream =  connection.getInputStream();

            
            BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            return new JSONObject(jsonText);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

}
