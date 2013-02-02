package net.progval.android.andquote;

import java.net.MalformedURLException;
import java.util.Iterator;

import net.progval.android.andquote.utils.OpenQuoteApi;
import android.app.Activity;
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
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class MainActivity extends Activity implements OnClickListener {
    private static SharedPreferences settings; 
    private OpenQuoteApi api;
    private LinearLayout layout;
    
    private class ProgressListener implements OpenQuoteApi.ProgressListener {

        public void onProgressUpdate(int progress) {
            MainActivity.this.setProgress(progress);
        }

        public void onFail(int status_message) {
            Toast.makeText(MainActivity.this, status_message, 
                Toast.LENGTH_SHORT).show();
        }

        public void onSuccess(String file) {
            OpenQuoteApi.Site[] sites = OpenQuoteApi.Site.get_sites(file);
            for (int i=0; i<sites.length && sites[i]!=null; i++)
                MainActivity.this.registerSite(sites[i]);
        }
        
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.main);
        MainActivity.settings = PreferenceManager.getDefaultSharedPreferences(this);
        
        this.setTitle(R.string.app_name);

        LinearLayout layout = new LinearLayout(this);
        ScrollView scrollview = new ScrollView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.FILL_PARENT,
                                    LinearLayout.LayoutParams.FILL_PARENT);
        scrollview.setLayoutParams(params);
        layout.addView(scrollview);
        this.setContentView(layout);

        this.layout = new LinearLayout(this);
        this.layout.setOrientation(this.layout.VERTICAL);
        scrollview.addView(this.layout);
        this.layout.setOrientation(this.layout.VERTICAL);
        
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
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.mainactivity_menu_settings:
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                this.startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void registerSite(OpenQuoteApi.Site site) {
        Button button = new Button(this);
        button.setText(site.name);
        button.setTag(site.id);
        button.setOnClickListener(this);
        this.layout.addView(button);
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
