package id.zananet.keluargazana;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    private static final String TAG = "ZANA_FCM";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * Jangan jalankan getToken() di UI thread.
         * Jalankan diagnostik di background thread.
         */
        new Thread(() -> {

            String result;

            try {
                result = FcmDiagnostic.check();

                Log.i(TAG, "HASIL FCM DIAGNOSTIC = " + result);

            } catch (Throwable e) {

                Log.e(TAG, "MAIN ACTIVITY FCM DIAGNOSTIC ERROR", e);

                result = "FCM ERROR\n\n"
                        + e.getClass().getName()
                        + "\n\n"
                        + String.valueOf(e.getMessage());
            }

            String finalResult = result;

            runOnUiThread(() -> {

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("FCM DIAGNOSTIC")
                        .setMessage(finalResult)
                        .setPositiveButton("OK", null)
                        .show();

            });

        }).start();
    }

    @Override
    public void onBackPressed() {

        WebView webView = getBridge().getWebView();

        if (webView != null) {

            webView.evaluateJavascript(
                "(function(){if(document.getElementById('chat') && !document.getElementById('chat').classList.contains('hidden')){if(typeof backHome==='function'){backHome();return 'handled';}}return 'normal';})()",
                value -> {

                    if (!"\"handled\"".equals(value)) {
                        MainActivity.super.onBackPressed();
                    }

                }
            );

        } else {

            super.onBackPressed();

        }
    }
}
