package net.progval.android.andquote;

import net.progval.android.andquote.utils.OpenQuoteApi;
import net.progval.android.andquote.QuoteActivity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.LinearLayout;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.view.KeyEvent;
import android.view.View.OnClickListener;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.util.DisplayMetrics;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import java.util.ArrayList;
import android.content.Intent;
import android.text.ClipboardManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.view.MenuInflater;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SiteActivity extends ListActivity implements OnClickListener {
    private static SharedPreferences settings; 
    private RelativeLayout layout;
    private LinearLayout navigation;
    private ListView quotesview;
    private OpenQuoteApi api;

    private ArrayList<String> quotesContent = new ArrayList<String>();
    private ArrayList<OpenQuoteApi.Quote> quotes = new ArrayList<OpenQuoteApi.Quote>();
    private ArrayAdapter<String> quotesAdapter;

    public static class State {
        public String site_id, site_name;
        public String mode = "latest";
        public int page = 1;
        public String type;
        public boolean previous=false, next=false, gotopage=false;
    }
    private State state;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extra = this.getIntent().getExtras();
        SiteActivity.settings = PreferenceManager.getDefaultSharedPreferences(this);
        this.state = new SiteActivity.State();
        this.state.site_id = extra.getString("site_id");
        this.state.site_name = extra.getString("site_name");
        
        this.setTitle(this.state.site_name);

        this.api = new OpenQuoteApi(this.settings.getString("api.url", ""));

        this.setContentView(R.layout.siteactivity);
        this.layout = (RelativeLayout) this.findViewById(R.id.siteActivity);
        this.layout.requestFocus();
        this.navigation = (LinearLayout) this.findViewById(R.id.navigation);
        this.quotesview = new ListView(this);

        findViewById(R.id.buttonLatest).setOnClickListener(this);
        findViewById(R.id.buttonTop).setOnClickListener(this);
        findViewById(R.id.buttonRandom).setOnClickListener(this);
        findViewById(R.id.buttonPrevious).setOnClickListener(this);
        findViewById(R.id.buttonNext).setOnClickListener(this);
        findViewById(R.id.gotopage).setOnClickListener(this);

        this.registerForContextMenu(this.getListView());

        ((EditText) findViewById(R.id.gotopage)).setOnEditorActionListener(
            new OnEditorActionListener() {
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    boolean handled = false;
                    if (actionId == EditorInfo.IME_ACTION_GO) {
                        try {
                            SiteActivity.this.state.page = Integer.parseInt(((EditText) v).getText().toString());
                            SiteActivity.this.loadQuotes();
                            handled = true;
                        }
                        catch (NumberFormatException e) {
                                // Never trust user input
                        }
                    }
                    return handled;
                }
            });
    
        this.quotesAdapter = new ArrayAdapter<String>(this, R.layout.quotewidget, this.quotesContent);
        this.setListAdapter(this.quotesAdapter);
        this.loadQuotes();
        this.resetState();
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.quote_context_menu, menu);
    }
    public boolean onContextItemSelected(MenuItem item) {
        int clickedQuote = ((AdapterContextMenuInfo)item.getMenuInfo()).position;
        switch (item.getItemId()) {
            case R.id.siteactivity_context_copy:
                ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                clipboard.setText(this.quotesContent.get(clickedQuote));
                return true;
            case R.id.siteactivity_context_share:
                Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, this.getResources().getString(R.string.siteactivity_share_subject));
                shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, this.quotesContent.get(clickedQuote));
                this.startActivity(Intent.createChooser(shareIntent, this.getResources().getString(R.string.siteactivity_share_window_title)));
                return true;
            case R.id.siteactivity_context_moreinfo:
                Intent intent = new Intent(this, QuoteActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("site_id", this.state.site_id);
                bundle.putString("site_name", this.state.site_name);
                bundle.putString("quote", this.quotes.get(clickedQuote).serialize());
                intent.putExtras(bundle);
                this.startActivity(intent);
                return true;
        }
        return false;
    }

    public void resetState() {
        findViewById(R.id.buttonPrevious).setEnabled(this.state.previous);
        findViewById(R.id.buttonNext).setEnabled(this.state.next);
        findViewById(R.id.gotopage).setEnabled(this.state.gotopage);
        ((EditText) findViewById(R.id.gotopage)).setText(String.valueOf(this.state.page));
        this.quotes.clear();
        this.quotesContent.clear();
        this.setContentView(layout);
        this.quotesAdapter.notifyDataSetChanged();
    }
    public void resetState(JSONObject object) {
        try {
            this.state.previous = ((Boolean) object.get("previous")).booleanValue();
            this.state.next = ((Boolean) object.get("next")).booleanValue();
            this.state.gotopage = ((Boolean) object.get("gotopage")).booleanValue();
            this.state.page = ((Integer) object.get("page")).intValue();
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        this.resetState();
    }

    public void showQuote(OpenQuoteApi.Quote quote) {
        this.quotes.add(quote);
        this.quotesContent.add(Html.fromHtml(quote.getContent()).toString());
        this.quotesAdapter.notifyDataSetChanged();
    }

    private void loadQuotes() {
        final ProgressDialog dialog = ProgressDialog.show(SiteActivity.this, "", this.getResources().getString(R.string.siteactivity_loading_quotes), true);
        class QuoteRenderer implements OpenQuoteApi.ProgressListener {
            private OpenQuoteApi api;
            public QuoteRenderer(OpenQuoteApi api) {
                this.api = api;
            }
            public void onProgressUpdate(int progress) {}
            public void onFail(int status_message) {
                dialog.dismiss();
                Toast.makeText(SiteActivity.this, status_message, Toast.LENGTH_LONG).show();
            }
            public void onSuccess(String file) {
                try {
                    JSONObject object = (JSONObject) new JSONTokener(file).nextValue();
                    JSONArray quotes = (JSONArray) object.get("quotes");

                    SiteActivity.this.resetState((JSONObject) object.get("state"));
                    for (int i=0; i<quotes.length(); i++) {
                        JSONObject quote = (JSONObject) quotes.get(i);
                        SiteActivity.this.showQuote(new OpenQuoteApi.Quote(quote));
                    }
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
                dialog.dismiss();
            }
        }
        class QuoteLoader implements OpenQuoteApi.ProgressListener {
            private OpenQuoteApi api;
            public QuoteLoader(OpenQuoteApi api) {
                this.api = api;
            }
            public void onProgressUpdate(int progress) {}
            public void onFail(int status_message) {
                dialog.dismiss();
                Toast.makeText(SiteActivity.this, status_message, Toast.LENGTH_LONG).show();
            }
            public void onSuccess(String file) {
                try {
                    JSONObject object = (JSONObject) new JSONTokener(file).nextValue();
                    api.safeGet(new QuoteRenderer(this.api), (String) object.get("url"));
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        this.api.getURL(new QuoteLoader(this.api), this.state);
    }


    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.buttonLatest:
                this.state.mode = "latest";
                this.loadQuotes();
                break;
            case R.id.buttonTop:
                this.state.mode = "top";
                this.loadQuotes();
                break;
            case R.id.buttonRandom:
                this.state.mode = "random";
                this.loadQuotes();
                break;
            case R.id.buttonPrevious:
                this.state.page -= 1;
                this.loadQuotes();
                break;
            case R.id.buttonNext:
                this.state.page += 1;
                this.loadQuotes();
                break;
        }
    }
}
