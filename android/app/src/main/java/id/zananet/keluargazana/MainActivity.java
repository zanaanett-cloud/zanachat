package id.zananet.keluargazana;

import android.os.Bundle;
import android.webkit.WebView;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

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
