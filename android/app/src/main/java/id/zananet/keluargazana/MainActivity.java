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

        // TES PERTAMA: HARUS LANGSUNG MUNCUL
        new AlertDialog.Builder(this)
                .setTitle("KELUARGA ZANA - TEST V3")
                .setMessage(
                        "DIAGNOSTIC V3 AKTIF\n\n" +
                        "Jika pesan ini muncul, berarti APK sudah memakai kode terbaru.\n\n" +
                        "Tekan OK untuk melanjutkan pemeriksaan FCM."
                )
                .setPositiveButton("OK", (dialog, which) -> mulaiDiagnostic())
                .setCancelable(false)
                .show();
    }

    private void mulaiDiagnostic() {

        new AlertDialog.Builder(this)
                .setTitle("FCM DIAGNOSTIC")
                .setMessage(
                        "Pemeriksaan Firebase sedang dimulai...\n\n" +
                        "Mohon tunggu maksimal 20 detik."
                )
                .setPositiveButton("Tunggu", null)
                .show();

        new Thread(() -> {

            String result;

            try {

                result = FcmDiagnostic.check();

                Log.i(TAG, "HASIL FCM DIAGNOSTIC = " + result);

            } catch (Throwable e) {

                Log.e(TAG, "MAIN ACTIVITY FCM DIAGNOSTIC ERROR", e);

                result =
                        "FCM ERROR\n\n" +
                        e.getClass().getName() +
                        "\n\n" +
                        String.valueOf(e.getMessage());
            }

            String finalResult = result;

            runOnUiThread(() -> {

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("HASIL FCM V3")
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
