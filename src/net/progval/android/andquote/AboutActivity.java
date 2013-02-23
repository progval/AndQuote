package net.progval.android.andquote;

import net.progval.android.andquote.utils.OpenQuoteApi;
import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.widget.Toast;
import java.io.InputStream;
import org.msgpack.MessagePack;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class AboutActivity extends Activity {
    private static SharedPreferences settings; 
    private OpenQuoteApi api;
    private static MessagePack messagePack = new MessagePack();
    LinearLayout layout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AboutActivity.settings = PreferenceManager.getDefaultSharedPreferences(this);
        this.api = new OpenQuoteApi(this.settings.getString("api.url", ""));

        layout = new LinearLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.FILL_PARENT,
                                    LinearLayout.LayoutParams.FILL_PARENT);
        this.layout.setOrientation(this.layout.VERTICAL);
        layout.setLayoutParams(params);
        this.setContentView(layout);
        this.setTitle(R.string.aboutactivity_title);

        TextView tv;

        tv = new TextView(this);
        tv.setText(R.string.aboutactivity_author);
        this.layout.addView(tv);

        tv = new TextView(this);
        tv.setText(R.string.aboutactivity_translator);
        this.layout.addView(tv);

        tv = new TextView(this);
        tv.setText(R.string.aboutactivity_license);
        this.layout.addView(tv);

        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            tv = new TextView(this);
            tv.setText(String.format(this.getResources().getString(R.string.aboutactivity_runningversion), version));
            this.layout.addView(tv);
        }
        catch (PackageManager.NameNotFoundException e) {
        }

        class LatestVersionDisplayer implements OpenQuoteApi.ProgressListener {
            public void onProgressUpdate(int progress) {}
            public void onFail(int status_message) {
                Toast.makeText(AboutActivity.this, status_message, Toast.LENGTH_LONG).show();
            }
            public void onSuccess(InputStream stream) {
                try {
                    String version = messagePack.read(stream).asRawValue().getString();
                    TextView tv;
                    tv = new TextView(AboutActivity.this);
                    tv.setText(String.format(AboutActivity.this.getResources().getString(R.string.aboutactivity_latestversion), version));
                    AboutActivity.this.layout.addView(tv);
                }
                catch (java.io.IOException e) {
                    e.printStackTrace();
                }
            }
        }
        this.api.safeGet(new LatestVersionDisplayer(), "/clients/AndQuote/version/");
    }
}
