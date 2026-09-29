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

    private static String lastDeliveredCallId = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        turnScreenOn();

        checkFullScreenPermission();

        handleIncomingCallIntent(getIntent());
    }

    private void turnScreenOn() {
        try {
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

            android.util.Log.i(
                    "ZANA_CALL",
                    "TURN SCREEN ON V10"
            );

        } catch (Throwable e) {
            android.util.Log.e(
                    "ZANA_CALL",
                    "turnScreenOn gagal",
                    e
            );
        }
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
                        "ZANA_CALL",
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

        turnScreenOn();

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

        if (callId == null || callId.trim().isEmpty()) {
            callId = "unknown-call";
        }

        /*
         * PENTING:
         * Satu callId hanya boleh diteruskan sekali.
         */
        synchronized (MainActivity.class) {

            if (callId.equals(lastDeliveredCallId)) {

                android.util.Log.i(
                        "ZANA_CALL",
                        "DUPLIKAT DIABAIKAN callId=" + callId
                );

                return;
            }

            lastDeliveredCallId = callId;
        }

        android.util.Log.i(
                "ZANA_CALL",
                "INCOMING CALL V10 callId=" + callId
                        + " type=" + callType
                        + " from=" + fromName
        );

        try {

            JSONObject obj = new JSONObject();

            obj.put(
                    "callId",
                    callId
            );

            obj.put(
                    "type",
                    callType == null
                            ? "audio"
                            : callType
            );

            obj.put(
                    "callType",
                    callType == null
                            ? "audio"
                            : callType
            );

            obj.put(
                    "fromUserId",
                    fromUserId == null
                            ? ""
                            : fromUserId
            );

            obj.put(
                    "fromName",
                    fromName == null
                            ? "Keluarga Zana"
                            : fromName
            );

            deliverIncomingCallToWebView(
                    obj.toString()
            );

        } catch (Throwable e) {

            android.util.Log.e(
                    "ZANA_CALL",
                    "Gagal membuat data incoming call",
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

            android.util.Log.e(
                    "ZANA_CALL",
                    "WebView belum tersedia"
            );

            return;
        }

        final WebView finalWebView = webView;

        String script =
                "(function(){"
                        + "try{"
                        + "window.dispatchEvent("
                        + "new CustomEvent("
                        + "'kz-native-incoming-call',"
                        + "{detail:" + json + "}"
                        + ")"
                        + ");"
                        + "console.log('[KZ V10] incoming call');"
                        + "}catch(e){"
                        + "console.error('[KZ V10] error',e);"
                        + "}"
                        + "})()";

        /*
         * HANYA SATU event.
         *
         * Tidak lagi mengirim 10 kali.
         */
        finalWebView.postDelayed(
                () -> {

                    try {

                        finalWebView.evaluateJavascript(
                                script,
                                value -> android.util.Log.i(
                                        "ZANA_CALL",
                                        "Event incoming call V10 dikirim"
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
                700
        );
    }

    @Override
    public void onBackPressed() {

        try {

            if (getBridge() != null &&
                    getBridge().getWebView() != null) {

                getBridge()
                        .getWebView()
                        .goBack();

                return;
            }

        } catch (Throwable ignored) {
        }

        super.onBackPressed();
    }
}
