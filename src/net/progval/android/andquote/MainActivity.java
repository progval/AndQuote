package net.progval.android.andquote;

import java.net.MalformedURLException;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import net.progval.android.andquote.utils.OpenQuoteApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {
    private String api_base = "http://djangoapps.progval.net/openquoteapi";
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
            try {
                JSONArray object = (JSONArray) new JSONTokener(file).nextValue();
                
                for (int i=0; i<object.length(); i++) {
                    JSONObject site = (JSONObject) object.get(i);
                    MainActivity.this.registerSite((String) site.get("id"),
                        (String) site.get("name"));
                }
                
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.main);
        
        this.setTitle(R.string.app_name);

        this.layout = new LinearLayout(this);
        this.layout.setOrientation(this.layout.VERTICAL);
        this.setContentView(this.layout);
        
        this.api = new OpenQuoteApi(this.api_base);
        try {
            this.api.get(new MainActivity.ProgressListener(), "/");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void registerSite(String id, String name) {
        Button button = new Button(this);
        button.setText(name);
        button.setTag(id);
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
        bundle.putString("api_base", this.api_base);
        intent.putExtras(bundle);
        this.startActivity(intent);
    }
    
    
}
