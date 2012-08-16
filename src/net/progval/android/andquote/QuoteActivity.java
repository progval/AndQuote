package net.progval.android.andquote;

import net.progval.android.andquote.utils.OpenQuoteApi;
import android.app.Activity;
import android.util.Log;
import android.text.Html;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class QuoteActivity extends Activity {
    private static SharedPreferences settings; 
    private LinearLayout layout;

    private OpenQuoteApi api;
    private SiteActivity.State state;
    private OpenQuoteApi.Quote quote;

    private TextView contentview, scoreview;
    private WebView imageview;
    private LinearLayout comments;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extra = this.getIntent().getExtras();
        QuoteActivity.settings = PreferenceManager.getDefaultSharedPreferences(this);
        this.state = new SiteActivity.State();
        this.state.site_id = extra.getString("site_id");
        this.state.site_name = extra.getString("site_name");
        this.quote = OpenQuoteApi.Quote.unserialize(extra.getString("quote"));
        Log.d("AndQuote", String.format("%d", this.quote.getId()));
        
        this.setTitle(this.state.site_name + " - " + this.quote.getId());

        this.api = new OpenQuoteApi(this.settings.getString("api.url", ""));

        LinearLayout layout = new LinearLayout(this);

        if (quote.getImageUrl() != null) {
            this.layout = layout;
            this.setContentView(layout);
        }
        else {
            ScrollView scrollview = new ScrollView(this);
            layout.addView(scrollview);
            this.setContentView(layout);

            this.layout = new LinearLayout(this);
            scrollview.addView(this.layout);
        }
        this.layout.setOrientation(this.layout.VERTICAL);

        this.contentview = new TextView(this);
        this.contentview.setText(Html.fromHtml(this.quote.getContent()));
        this.layout.addView(this.contentview);

        this.scoreview = new TextView(this);
        this.scoreview.setText(this.quote.getScore());
        this.layout.addView(this.scoreview);

        this.fetchExtraData();
    }
    private void fetchExtraData() {
        class QuoteLoader implements OpenQuoteApi.ProgressListener {
            private OpenQuoteApi api;
            private TextView scoreview;
            public QuoteLoader(OpenQuoteApi api, TextView scoreview) {
                this.api = api;
                this.scoreview = scoreview;
            }
            public void onProgressUpdate(int progress) {}
            public void onFail(int status_message) {
                Toast.makeText(QuoteActivity.this, status_message, Toast.LENGTH_LONG).show();
            }
            public void onSuccess(String file) {
                try {
                    JSONObject object = (JSONObject) new JSONTokener(file).nextValue();
                    OpenQuoteApi.Quote quote = new OpenQuoteApi.Quote((JSONObject) object.get("quote"));
                    QuoteActivity.this.setQuote(quote);
                    if (quote.getAuthor() != null)
                        this.scoreview.setText(quote.getScore() + " -- " + quote.getAuthor());
                    QuoteActivity.this.renderComments(OpenQuoteApi.Comment.parseComments((JSONArray) object.get("comments")));
                    if (quote.getImageUrl() != null) {
                        QuoteActivity.this.setImage(quote.getImageUrl());
                    }
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        this.api.safeGet(new QuoteLoader(this.api, this.scoreview), String.format("/%s/quotes/show/%d/", this.state.site_id, this.quote.getId()));
    }
    public void setQuote(OpenQuoteApi.Quote quote) {
        this.quote = quote;
    }
    public void setImage(String url) {
        this.imageview = new WebView(this);
        this.imageview.loadUrl(url);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.FILL_PARENT,
                                    LinearLayout.LayoutParams.FILL_PARENT);
        this.imageview.setLayoutParams(params);
        this.imageview.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        this.setContentView(this.layout); // Kick the scrollview out
        this.layout.addView(this.imageview);
    }
    public void renderComments(OpenQuoteApi.Comment[] comments) {
        this.comments = new LinearLayout(this);
        this.comments.setOrientation(this.comments.VERTICAL);
        this.layout.addView(this.comments);
        this.renderComments(comments, this.comments);
    }
    public void renderComments(OpenQuoteApi.Comment[] comments, LinearLayout layout) {
        for (int i=0; i<comments.length; i++)
            this.renderComment(comments[i], layout);
    }
    public void renderComment(OpenQuoteApi.Comment comment, LinearLayout layout) {
        TextView textview = new TextView(this);
        textview.setText(comment.getContent());
        layout.addView(textview);

        LinearLayout repliesLayout = new LinearLayout(this);
        layout.setPadding(20, 0, 0, 0);
        layout.addView(repliesLayout);
        repliesLayout.setOrientation(this.layout.VERTICAL);
        this.renderComments(comment.getReplies(), repliesLayout);
    }
}

