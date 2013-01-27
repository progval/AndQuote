package net.progval.android.andquote;

import net.progval.android.andquote.utils.OpenQuoteApi;
import android.app.Activity;
import android.util.Log;
import android.text.Html;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.view.KeyEvent;
import android.widget.Button;
import android.webkit.WebView;
import android.view.ViewGroup;
import android.text.InputType;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ScrollView;
import android.app.ProgressDialog;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.content.SharedPreferences;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.preference.PreferenceManager;
import android.widget.TextView.OnEditorActionListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class QuoteActivity extends Activity implements OnClickListener {
    private static SharedPreferences settings; 
    private LinearLayout layout;

    private OpenQuoteApi api;
    private SiteActivity.State state;
    private OpenQuoteApi.Quote quote;

    private TextView contentview, scoreview;
    private WebView imageview;
    private LinearLayout comments, navigation;
    private EditText gotopage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extra = this.getIntent().getExtras();
        QuoteActivity.settings = PreferenceManager.getDefaultSharedPreferences(this);
        this.state = new SiteActivity.State();
        this.state.site_id = extra.getString("site_id");
        this.state.site_name = extra.getString("site_name");
        this.quote = OpenQuoteApi.Quote.unserialize(extra.getString("quote"));
        
        this.setTitle(this.state.site_name + " - " + this.quote.getId());

        this.api = new OpenQuoteApi(this.settings.getString("api.url", ""));

        LinearLayout layout = new LinearLayout(this);

        if (!quote.getImageUrl().equals("null")) {
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

        Log.d("AndQuote", quote.getImageUrl());
        Log.d("AndQuote", Boolean.valueOf(this.settings.getBoolean("nav.quote.enable", false)).toString());
        Log.d("AndQuote", Boolean.valueOf(this.settings.getBoolean("nav.quote.img_enable", true)).toString());
        if (quote.getImageUrl().equals("null")) {
            Log.d("AndQuote", "one");
            if (this.settings.getBoolean("nav.quote.enable", false))
                this.inflateNavigation();
        }
        else {
            Log.d("AndQuote", "one");
            if (this.settings.getBoolean("nav.quote.img_enable", true))
                this.inflateNavigation();
        }

        this.contentview = new TextView(this);
        this.contentview.setText(Html.fromHtml(this.quote.getContent()));
        this.layout.addView(this.contentview);

        this.scoreview = new TextView(this);
        this.scoreview.setText(this.quote.getScore());
        this.layout.addView(this.scoreview);

        this.fetchExtraData();
    }
    private void inflateNavigation() {
        this.navigation = new LinearLayout(this);
        Button prev = new Button(this);
        prev.setText("<=");
        prev.setTag("prev");
        prev.setOnClickListener(this);

        Button next = new Button(this);
        next.setText("=>");
        next.setTag("next");
        next.setOnClickListener(this);
        
        this.gotopage = new EditText(this);
        this.gotopage.setImeOptions(EditorInfo.IME_ACTION_GO);
        this.gotopage.setInputType(InputType.TYPE_CLASS_NUMBER);
        this.layout.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        this.layout.setFocusableInTouchMode(true);
        this.gotopage.setText(String.format("%d", quote.getId()));
        this.gotopage.setOnEditorActionListener(
            new OnEditorActionListener() {
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    boolean handled = false;
                    if (actionId == EditorInfo.IME_ACTION_GO) {
                        try {
                            int id = Integer.parseInt(((EditText) v).getText().toString());
                            QuoteActivity.this.loadQuote(id);
                            handled = true;
                        }
                        catch (NumberFormatException e) {
                                // Never trust user input
                        }
                    }
                    return handled;
                }
            });

        this.navigation.addView(prev);
        this.navigation.addView(this.gotopage);
        this.navigation.addView(next);
        this.layout.addView(this.navigation);
    }
    public void loadQuote(int id) {
        final ProgressDialog dialog = ProgressDialog.show(QuoteActivity.this, "", this.getResources().getString(R.string.siteactivity_loading_quotes), true);
        class QuoteRenderer implements OpenQuoteApi.ProgressListener {
            private OpenQuoteApi api;
            public QuoteRenderer(OpenQuoteApi api) {
                this.api = api;
            }
            public void onProgressUpdate(int progress) {}
            public void onFail(int status_message) {
                dialog.dismiss();
                Toast.makeText(QuoteActivity.this, status_message, Toast.LENGTH_LONG).show();
            }
            public void onSuccess(String file) {
                try {
                    JSONObject object = (JSONObject) new JSONTokener(file).nextValue();
                    if (!object.has("quote"))
                        this.onFail(R.string.quoteactivity_doesnotexist);
                    else {
                        OpenQuoteApi.Quote quote = new OpenQuoteApi.Quote((JSONObject) object.get("quote"));
                        QuoteActivity.this.loadQuote(quote);
                    }
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
                dialog.dismiss();
            }
        }
        this.api.safeGet(new QuoteRenderer(this.api),
            String.format("/%s/quotes/show/%d/", this.state.site_id, id));
    }
    public void onClick(View v) {
        String tag = (String) ((Button) v).getTag();
        if (tag.equals("prev"))
            this.loadQuote(this.quote.getId()-1);
        else if (tag.equals("next"))
            this.loadQuote(this.quote.getId()+1);
    }
    public void loadQuote(OpenQuoteApi.Quote quote) {
        this.quote = quote;
        this.contentview.setText(Html.fromHtml(this.quote.getContent()));
        this.scoreview.setText(this.quote.getScore());
        this.setTitle(this.state.site_name + " - " + this.quote.getId());
        if (quote.getImageUrl() != null) {
            this.setImage(quote.getImageUrl());
        }
        if (!quote.getImageUrl().equals("null") &&
                this.settings.getBoolean("nav.img_enable", false))
            this.gotopage.setText(String.format("%d", quote.getId()));
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
        if (this.imageview == null) {
            this.imageview = new WebView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.FILL_PARENT,
                                        LinearLayout.LayoutParams.FILL_PARENT);
            this.imageview.setLayoutParams(params);
            this.imageview.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
            this.setContentView(this.layout); // Kick the scrollview out
            this.layout.addView(this.imageview);
        }
        this.imageview.loadUrl(url);
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

