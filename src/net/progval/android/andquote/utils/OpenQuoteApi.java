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
            NOTE, UPDOWN, NONE
        }

        private int id;
        private String content;
        private int note, up, down;
        private ScoreType scoretype;
        private String author, date, url, image_url;

        public Quote(int id, String content, String url, String image_url) {
            this.id = id;
            this.content = content;
            this.scoretype = Quote.ScoreType.NONE;
            this.url = url;
            this.image_url = image_url;
        }
        public Quote(int id, String content, int note, String url, String image_url) {
            this.id = id;
            this.content = content;
            this.note = note;
            this.scoretype = Quote.ScoreType.NOTE;
            this.url = url;
            this.image_url = image_url;
        }
        public Quote(int id, String content, int up, int down, String url, String image_url) {
            this.id = id;
            this.content = content;
            this.up = up;
            this.down = down;
            this.note = up - down;
            this.scoretype = Quote.ScoreType.UPDOWN;
            this.url = url;
            this.image_url = image_url;
        }
        public Quote(JSONObject object) {
            try {
                // Be sure to replace new lines after replacing < and >
                this.id = ((Integer) object.get("id")).intValue();
                this.content = ((String) object.get("content")).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br />");
                if (object.has("note")) {
                    this.note = ((Integer) object.get("note")).intValue();
                    this.scoretype = Quote.ScoreType.NOTE;
                }
                else if (object.has("up") && object.has("down")) {
                    this.up = ((Integer) object.get("up")).intValue();
                    this.down = ((Integer) object.get("down")).intValue();
                    this.scoretype = Quote.ScoreType.UPDOWN;
                }
                else {
                    this.scoretype = Quote.ScoreType.NONE;
                }
                if (object.has("author"))
                    this.author = (String) object.get("author");
                if (object.has("date") && object.get("date") != null)
                    this.date = (String) object.get("date");
                if (object.has("url")) {
                    this.url = (String) object.get("url");
                }
                if (object.has("image")) {
                    this.image_url = (String) object.get("image");
                }
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
        public int getId() {
            return this.id;
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
        public String getScore() {
            switch (this.scoretype) {
                case UPDOWN:
                    return String.format("+%d -%d", this.up, this.down);
                case NOTE:
                    return String.format("%d", this.note);
                default:
                    return "";
            }
        }
        public String getAuthor() {
            return this.author;
        }
        public String getDate() {
            return this.date;
        }
        public String getUrl() {
            return this.url;
        }
        public String getImageUrl() {
            return this.image_url;
        }

        public String serialize() {
            return String.format("%d|%d|%d|%d|%d|%s|%s|%s", this.id, this.scoretype.ordinal(), this.note, this.up, this.down, this.image_url, this.image_url, this.content);
        }
        public static Quote unserialize(String string) {
            String[] parts = string.split("\\|",8);
            Integer id = Integer.parseInt(parts[0]), type = Integer.parseInt(parts[1]); // Java sucks
            String url = parts[5];
            String image_url = parts[6];
            switch (Quote.ScoreType.values()[type.intValue()]) {
                case UPDOWN:
                    Integer up = Integer.parseInt(parts[3]), down = Integer.parseInt(parts[4]); // Java sucks
                    return new Quote(id.intValue(), parts[7], up.intValue(), down.intValue(), url, image_url);
                case NOTE:
                    Integer note = Integer.parseInt(parts[2]); // Java sucks
                    return new Quote(id.intValue(), parts[7], note.intValue(), url, image_url);
                case NONE:
                    return new Quote(id.intValue(), parts[7], url, image_url);
                default:
                    return null;
            }
        }
    }

    public static class Comment {
        private String content, author;
        private Comment[] replies;
        public Comment(JSONObject object) {
            try {
                this.content = (String) object.get("content");
                this.author = (String) object.get("author");
                JSONArray replies = (JSONArray) object.get("replies");
                this.replies = Comment.parseComments(replies);
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
        public static Comment[] parseComments(JSONArray array) {
            try {
                Comment[] comments = new Comment[array.length()];
                for (int i=0; i<array.length(); i++)
                    comments[i] = new Comment((JSONObject) array.get(i));
                return comments;
            }
            catch (JSONException e) {
                e.printStackTrace();
                return new Comment[0];
            }
        }
        public String getContent() {
            return this.content;
        }
        public String getAuthor() {
            return this.author;
        }
        public Comment[] getReplies() {
            return this.replies;
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
        this("");
    }
    public OpenQuoteApi(String base_url) {
        if (base_url.equals(""))
            this.base_url = "http://djangoapps.progval.net/openquoteapi";
        else
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
