package net.progval.android.andquote.utils;

import net.progval.android.andquote.SiteActivity;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import android.util.Log;
import java.net.URLConnection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import net.progval.android.andquote.R;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;

public class OpenQuoteApi {
    String base_url;
    
    public interface ProgressListener {
        public void onProgressUpdate(int progress);
        public void onFail(int status_message);
        public void onSuccess(String file);
    }

    public static class Quote {
        public static enum ScoreType {
            NOTE, UPDOWN
        }

        private String content;
        private int note, up, down;
        private ScoreType scoretype;

        public Quote(String content, int note) {
            this.content = content;
            this.note = note;
            this.scoretype = Quote.ScoreType.NOTE;
        }
        public Quote(String content, int up, int down) {
            this.content = content;
            this.up = up;
            this.down = down;
            this.note = up - down;
            this.scoretype = Quote.ScoreType.UPDOWN;
        }
        public Quote(JSONObject object) {
            try {
                // Be sure to replace new lines after replacing < and >
                this.content = ((String) object.get("content")).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br />");
                if (object.has("note"))
                    this.note = ((Integer) object.get("note")).intValue();
                else {
                    this.up = ((Integer) object.get("up")).intValue();
                    this.down = ((Integer) object.get("down")).intValue();
                }
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
        public String getContent() {
            return this.content;
        }
        public ScoreType getScoreType() {
            return this.scoretype;
        }
        public int getNote() {
            return this.note;
        }
        public int getUp() {
            return this.up;
        }
        public int getDown() {
            return this.down;
        }
    }
    
    private class Downloader extends AsyncTask<URL, Integer, Integer> {

        private ProgressListener progress_listener;
        private Writer writer;
        private boolean success = false;
        private int status_message;
        private BufferedReader stream;

        public Downloader(OpenQuoteApi.ProgressListener progress_listener) {
            this.progress_listener = progress_listener;
        }
        
        private int lengthOfFile; 
        
        protected Integer doInBackground(URL... url) {
            int total = 0, count;
            char data[] = new char[64];
            this.writer = new StringWriter();
            try {
                URLConnection connection = url[0].openConnection();
                connection.connect();
                stream = new BufferedReader(new InputStreamReader(url[0].openStream()));
                
                while ((count = this.stream.read(data)) != -1) {
                    total += count;
                    this.publishProgress(total*100);
                    this.writer.write(data, 0, count);
                    
                }
                this.success = true;
            } catch (IOException e) {
                e.printStackTrace();
                this.status_message = R.string.error_download_failed;
            }
            return 0;
        }
        
        protected void onProgressUpdate(Integer... progress) {
            this.progress_listener.onProgressUpdate(progress[0]);
        }
        
        protected void onPostExecute(Integer foo) {
            if (this.success)
                this.progress_listener.onSuccess(this.writer.toString());
            else
                this.progress_listener.onFail(this.status_message);
        }
    }
    
    public OpenQuoteApi() {
        this("http://djangoapps.progval.net/openquoteapi");
    }
    public OpenQuoteApi(String base_url) {
        this.base_url = base_url;
    }
    
    public void get(OpenQuoteApi.ProgressListener progress_listener, String url)
            throws MalformedURLException {
        new OpenQuoteApi.Downloader(progress_listener).execute(new URL(this.base_url + url));
    }
    public void safeGet(OpenQuoteApi.ProgressListener progress_listener, String url) {
        try {
            this.get(progress_listener, url);
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
    public void getURL(OpenQuoteApi.ProgressListener progress_listener, SiteActivity.State state) {
        this.safeGet(progress_listener,
                String.format("/state/url?site=%s&mode=%s&type=%s&page=%s",
                    state.site_id, state.mode, state.type, state.page));
    }
}
