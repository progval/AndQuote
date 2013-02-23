package net.progval.android.andquote;

import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.ArrayList;
import java.io.InputStream;

import net.progval.android.andquote.utils.OpenQuoteApi;
import android.app.Activity;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ScrollView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.Toast;
import android.widget.BaseAdapter;
import android.view.Menu;
import android.view.ViewGroup;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class MainActivity extends Activity implements OnClickListener {
    private static SharedPreferences settings; 
    private OpenQuoteApi api;
    private GridView gridview;
    ArrayList<View> buttons = new ArrayList<View>();
    
    private class ProgressListener implements OpenQuoteApi.ProgressListener {

        public void onProgressUpdate(int progress) {
            MainActivity.this.setProgress(progress);
        }

        public void onFail(int status_message) {
            Toast.makeText(MainActivity.this, status_message, 
                Toast.LENGTH_SHORT).show();
        }

        public void onSuccess(InputStream stream) {
            OpenQuoteApi.Site[] sites = OpenQuoteApi.Site.parse_sites(stream);
            for (int i=0; i<sites.length && sites[i]!=null; i++)
                MainActivity.this.registerSite(sites[i]);
        }
    }

    public class CustomAdapter extends BaseAdapter {

        public View getView(int position, View convertView, ViewGroup parent) {
            return MainActivity.this.buttons.get(position);
        }
        public final int getCount() {
            return MainActivity.this.buttons.size();
        }

        public final Object getItem(int position) {
            return MainActivity.this.buttons.get(position);
        }

        public final long getItemId(int position) {
            return position;
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.main);
        MainActivity.settings = PreferenceManager.getDefaultSharedPreferences(this);
        
        this.setTitle(R.string.app_name);

        LinearLayout layout = new LinearLayout(this);
        this.setContentView(layout);

        this.gridview = new GridView(this);
        layout.addView(this.gridview);
        this.gridview.setAdapter(new CustomAdapter());
        this.gridview.setColumnWidth(200);
        this.gridview.setNumColumns(-1);
        
        this.api = new OpenQuoteApi(this.settings.getString("api.url", ""));
        try {
            this.api.get(new MainActivity.ProgressListener(), "/");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.mainactivity_menu_settings:
                intent = new Intent(MainActivity.this, SettingsActivity.class);
                this.startActivity(intent);
                return true;
            case R.id.mainactivity_menu_about:
                intent = new Intent(MainActivity.this, AboutActivity.class);
                this.startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void registerSite(OpenQuoteApi.Site site) {
        final Button button = new Button(this);
        button.setText(site.name);
        button.setTag(site.id);
        button.setOnClickListener(this);
        button.setHeight(125);
        if (this.settings.getBoolean("home.fetchimages", true)) {
            class ProgressListener implements OpenQuoteApi.ProgressListener {

                public void onProgressUpdate(int progress) {
                }

                public void onFail(int status_message) {
                }

                public void onSuccess(InputStream stream) {
                    Bitmap img = BitmapFactory.decodeStream(stream);
                    BitmapDrawable drawable = new BitmapDrawable(img);
                    button.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null);
                }
            }
            this.api.safeGet(new ProgressListener(), site.get_logo_url());
        }
        this.buttons.add(button);
    }
    
    public void onClick(View view) {
        Button button = (Button) view;
        String id = (String) button.getTag();
        Intent intent = new Intent(this, SiteActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("site_id", id);
        bundle.putString("site_name", button.getText().toString());
        intent.putExtras(bundle);
        this.startActivity(intent);
    }
    
    
}
