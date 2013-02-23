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
import org.msgpack.MessagePack;
import org.msgpack.type.ArrayValue;
import org.msgpack.type.Value;
import org.msgpack.type.MapValue;
import java.util.Map;

import net.progval.android.andquote.R;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;
import net.progval.android.andquote.utils.MsgPackUtils;

public class OpenQuoteApi {
    private static MessagePack messagePack = new MessagePack();
    String base_url;
    
    public interface ProgressListener {
        public void onProgressUpdate(int progress);
        public void onFail(int status_message);
        public void onSuccess(InputStream stream);
    }

    public static class Site {
        public String id, name;
        public Site(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public static Site[] parse_sites(InputStream stream) {
            Site[] sites = {};
            ArrayValue objects;
            MapValue object;
            try {
                objects = messagePack.read(stream).asArrayValue();
            }
            catch (java.io.IOException e) {
                e.printStackTrace();
                return sites;
            }
            sites = new Site[objects.size()];

            for (int i=0; i<objects.size(); i++) {
                object = objects.get(i).asMapValue();
                sites[i] = new Site(MsgPackUtils.get(object, "id").asRawValue().getString(),
                        MsgPackUtils.get(object, "name").asRawValue().getString());
            }
            return sites;
        }
    }

    public static class State {
        public String site_id, site_name;
        public String mode = "latest";
        public int page = 1;
        public String type;
        public boolean previous=false, next=false, gotopage=false;

        public State() {}
        public State(MapValue map) {
            this.previous = MsgPackUtils.get(map, "previous").asBooleanValue().getBoolean();
            this.next = MsgPackUtils.get(map, "next").asBooleanValue().getBoolean();
            this.gotopage = MsgPackUtils.get(map, "gotopage").asBooleanValue().getBoolean();
            this.page = MsgPackUtils.get(map, "page").asIntegerValue().getInt();
        }
        public void update(State other) {
            this.previous = other.previous;
            this.next = other.next;
            this.gotopage = other.gotopage;
            this.page = other.page;
        }
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
        public Quote(MapValue map) {
            // Be sure to replace new lines after replacing < and >
            this.id = MsgPackUtils.get(map, ("id")).asIntegerValue().getInt();
            this.content = MsgPackUtils.get(map, ("content")).asRawValue().getString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br />");
            if (MsgPackUtils.in(map, "note")) {
                this.note = MsgPackUtils.get(map, ("note")).asIntegerValue().getInt();
                this.scoretype = Quote.ScoreType.NOTE;
            }
            else if (MsgPackUtils.in(map, "up") && MsgPackUtils.in(map, "down")) {
                this.up = MsgPackUtils.get(map, ("up")).asIntegerValue().getInt();
                this.down = MsgPackUtils.get(map, ("down")).asIntegerValue().getInt();
                this.scoretype = Quote.ScoreType.UPDOWN;
            }
            else {
                this.scoretype = Quote.ScoreType.NONE;
            }
            if (MsgPackUtils.in(map, "author"))
                this.author = MsgPackUtils.get(map, ("author")).asRawValue().getString();
            if (MsgPackUtils.in(map, "date") && MsgPackUtils.get(map, ("date")) != null)
                this.date = MsgPackUtils.get(map, ("date")).asRawValue().getString();
            if (MsgPackUtils.in(map, "url")) {
                this.url = MsgPackUtils.get(map, ("url")).asRawValue().getString();
            }
            if (MsgPackUtils.in(map, "image")) {
                this.image_url = MsgPackUtils.get(map, ("image")).asRawValue().getString();
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
        public Comment(MapValue map) {
            this.content = MsgPackUtils.get(map, "content").asRawValue().getString();
            this.author = MsgPackUtils.get(map, "author").asRawValue().getString();
            ArrayValue replies = MsgPackUtils.get(map, "replies").asArrayValue();
            this.replies = Comment.parseComments(replies);
        }
        public static Comment[] parseComments(ArrayValue array) {
            Comment[] comments = new Comment[array.size()];
            for (int i=0; i<array.size(); i++)
                comments[i] = new Comment(array.get(i).asMapValue());
            return comments;
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
        private InputStream stream;

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
                this.stream = url[0].openStream();
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
                this.progress_listener.onSuccess(this.stream);
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
        if (url.indexOf('?') == -1)
            url += "?";
        else
            url += "&";
        url += "format=msgpack";
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
    public void getURL(OpenQuoteApi.ProgressListener progress_listener, OpenQuoteApi.State state) {
        this.safeGet(progress_listener,
                String.format("/state/url?site=%s&mode=%s&type=%s&page=%s&format=msgpack",
                    state.site_id, state.mode, state.type, state.page));
    }
}
