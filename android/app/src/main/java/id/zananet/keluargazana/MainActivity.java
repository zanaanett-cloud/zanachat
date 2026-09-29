package id.zananet.keluargazana;

import android.app.KeyguardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.WindowManager;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * Android 10+:
         * Izinkan Activity tampil di atas lock screen
         * dan menyalakan layar saat dibuka oleh
         * Full-Screen Notification.
         */

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O_MR1) {

            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.LOLLIPOP) {

            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );
        }

        /*
         * Lepaskan lock screen setelah Activity
         * berhasil dibuka oleh full-screen notification.
         */

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            KeyguardManager keyguardManager =
                    (KeyguardManager)
                            getSystemService(
                                    KEYGUARD_SERVICE
                            );

            if (keyguardManager != null &&
                    keyguardManager.isKeyguardLocked()) {

                try {

                    keyguardManager.requestDismissKeyguard(
                            this,
                            null
                    );

                } catch (Throwable ignored) {
                }
            }
        }

        /*
         * Jika Android 14/15 meminta izin
         * Full-Screen Intent, kita buka halaman
         * pengaturannya saat aplikasi dibuka.
         */

        checkFullScreenPermission();
    }

    private void checkFullScreenPermission() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {

            try {

                android.app.NotificationManager manager =
                        (android.app.NotificationManager)
                                getSystemService(
                                        NOTIFICATION_SERVICE
                                );

                if (manager != null &&
                        !manager.canUseFullScreenIntent()) {

                    Intent intent =
                            new Intent(
                                    Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT
                            );

                    intent.setData(
                            Uri.parse(
                                    "package:" +
                                    getPackageName()
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
    }

    @Override
    public void onBackPressed() {

        try {

            if (getBridge() != null &&
                    getBridge().getWebView() != null) {

                getBridge()
                        .getWebView()
                        .evaluateJavascript(
                                "(function(){if(document.getElementById('chat') && !document.getElementById('chat').classList.contains('hidden')){if(typeof backHome==='function'){backHome();return 'handled';}}return 'normal';})()",
                                value -> {

                                    if (!"\"handled\"".equals(value)) {

                                        MainActivity.super
                                                .onBackPressed();
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
