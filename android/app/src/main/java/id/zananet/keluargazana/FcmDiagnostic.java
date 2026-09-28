package id.zananet.keluargazana;

import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

public final class FcmDiagnostic {

    private static final String TAG = "ZANA_FCM";

    private FcmDiagnostic() {}

    public static String check() {
        try {
            Log.i(TAG, "========== FCM DIAGNOSTIC START ==========");

            if (FirebaseApp.getApps(null).isEmpty()) {
                Log.e(TAG, "FirebaseApp BELUM TERINISIALISASI");
                return "FIREBASE_APP_NOT_INITIALIZED";
            }

            FirebaseApp app = FirebaseApp.getInstance();

            Log.i(TAG, "FirebaseApp OK");
            Log.i(TAG, "Firebase projectId = " + app.getOptions().getProjectId());
            Log.i(TAG, "Firebase applicationId = " + app.getOptions().getApplicationId());
            Log.i(TAG, "Firebase gcmSenderId = " + app.getOptions().getGcmSenderId());

            FirebaseMessaging messaging = FirebaseMessaging.getInstance();

            Log.i(TAG, "FirebaseMessaging instance OK");
            Log.i(TAG, "FirebaseMessaging class = " + messaging.getClass().getName());

            Log.i(TAG, "========== FCM DIAGNOSTIC OK ==========");

            return "FIREBASE_OK";

        } catch (Throwable e) {
            Log.e(TAG, "FCM DIAGNOSTIC EXCEPTION", e);
            return "FCM_ERROR:" + e.getClass().getName() + ":" + String.valueOf(e.getMessage());
        }
    }
}
