package id.zananet.keluargazana;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    private static final String TAG = "ZANA_FCM";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            String result = FcmDiagnostic.check();
            Log.i(TAG, "HASIL FCM DIAGNOSTIC = " + result);
        } catch (Throwable e) {
            Log.e(TAG, "MAIN ACTIVITY FCM DIAGNOSTIC ERROR", e);
        }
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
