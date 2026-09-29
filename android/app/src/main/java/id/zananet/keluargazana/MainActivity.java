package id.zananet.keluargazana;

import android.app.KeyguardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.WindowManager;
import android.webkit.WebView;

import com.getcapacitor.BridgeActivity;

import org.json.JSONObject;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * Android 10+:
         * Activity dapat tampil di atas lock screen
         * dan menyalakan layar.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );
        }

        /*
         * Coba lepaskan lock screen jika memungkinkan.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                KeyguardManager keyguardManager =
                        (KeyguardManager) getSystemService(KEYGUARD_SERVICE);

                if (keyguardManager != null &&
                        keyguardManager.isKeyguardLocked()) {

                    keyguardManager.requestDismissKeyguard(
                            this,
                            null
                    );
                }
            } catch (Throwable e) {
                android.util.Log.e(
                        "ZANA_FULLSCREEN",
                        "requestDismissKeyguard gagal",
                        e
                );
            }
        }

        /*
         * Cek izin Full Screen Intent Android 14/15.
         */
        checkFullScreenPermission();

        /*
         * Jika Activity dibuka langsung oleh
         * Full Screen Notification, teruskan data
         * panggilan ke JavaScript.
         */
        handleIncomingCallIntent(getIntent());
    }

    private void checkFullScreenPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                android.app.NotificationManager manager =
                        (android.app.NotificationManager)
                                getSystemService(NOTIFICATION_SERVICE);

                if (manager != null &&
                        !manager.canUseFullScreenIntent()) {

                    Intent intent =
                            new Intent(
                                    Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT
                            );

                    intent.setData(
                            Uri.parse(
                                    "package:" + getPackageName()
                            )
                    );

                    startActivity(intent);
                }

            } catch (Throwable e) {
                android.util.Log.e(
                        "ZANA_FULLSCREEN",
                        "Gagal membuka pengaturan Full Screen",
                        e
                );
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);

        /*
         * Panggilan baru dapat datang ketika
         * MainActivity sudah hidup.
         */
        handleIncomingCallIntent(intent);
    }

    private void handleIncomingCallIntent(Intent intent) {

        if (intent == null) {
            return;
        }

        boolean incomingCall =
                intent.getBooleanExtra(
                        "incoming_call",
                        false
                );

        if (!incomingCall) {
            return;
        }

        String callId =
                intent.getStringExtra("callId");

        String callType =
                intent.getStringExtra("callType");

        String fromUserId =
                intent.getStringExtra("fromUserId");

        String fromName =
                intent.getStringExtra("fromName");

        android.util.Log.i(
                "ZANA_CALL",
                "Incoming call: callId=" + callId
                        + " type=" + callType
                        + " from=" + fromName
        );

        try {

            JSONObject obj = new JSONObject();

            obj.put(
                    "callId",
                    callId == null ? "" : callId
            );

            obj.put(
                    "type",
                    callType == null ? "audio" : callType
            );

            obj.put(
                    "fromUserId",
                    fromUserId == null ? "" : fromUserId
            );

            obj.put(
                    "fromName",
                    fromName == null
                            ? "Keluarga Zana"
                            : fromName
            );

            String json = obj.toString();

            deliverIncomingCallToWebView(json);

        } catch (Throwable e) {

            android.util.Log.e(
                    "ZANA_CALL",
                    "Gagal meneruskan incoming call ke WebView",
                    e
            );
        }
    }

    private void deliverIncomingCallToWebView(
            String json
    ) {

        WebView webView = null;

        try {
            if (getBridge() != null) {
                webView = getBridge().getWebView();
            }
        } catch (Throwable ignored) {
        }

        if (webView == null) {
            return;
        }

        String script =
                "(function(){"
                        + "try{"
                        + "window.dispatchEvent("
                        + "new CustomEvent("
                        + "'kz-native-incoming-call',"
                        + "{detail:" + json + "}"
                        + ")"
                        + ");"
                        + "console.log('[KZ NATIVE CALL] event diterima');"
                        + "}catch(e){"
                        + "console.error('[KZ NATIVE CALL] error',e);"
                        + "}"
                        + "})()";

        /*
         * WebView kadang belum selesai loading
         * ketika Full Screen Activity dibuka.
         *
         * Coba beberapa kali agar event tidak hilang.
         */
        final WebView finalWebView = webView;

        for (int i = 0; i < 10; i++) {

            final int attempt = i;

            finalWebView.postDelayed(
                    () -> {

                        try {

                            finalWebView.evaluateJavascript(
                                    script,
                                    value -> android.util.Log.i(
                                            "ZANA_CALL",
                                            "Event call dikirim attempt="
                                                    + attempt
                                    )
                            );

                        } catch (Throwable e) {

                            android.util.Log.e(
                                    "ZANA_CALL",
                                    "evaluateJavascript gagal",
                                    e
                            );
                        }

                    },
                    500L * i
            );
        }
    }

    @Override
    public void onBackPressed() {

        try {

            if (getBridge() != null &&
                    getBridge().getWebView() != null) {

                getBridge()
                        .getWebView()
                        .evaluateJavascript(
                                "(function(){"
                                        + "if(document.getElementById('chat')"
                                        + " && !document.getElementById('chat').classList.contains('hidden')){"
                                        + "if(typeof backHome==='function'){"
                                        + "backHome();"
                                        + "return 'handled';"
                                        + "}"
                                        + "}"
                                        + "return 'normal';"
                                        + "})()",
                                value -> {

                                    if (!"\"handled\"".equals(value)) {
                                        MainActivity.super.onBackPressed();
                                    }

                                }
                        );

                return;
            }

        } catch (Throwable ignored) {
        }

        super.onBackPressed();
    }
}
