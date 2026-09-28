package id.zananet.keluargazana;

import android.util.Log;

public final class FcmDiagnostic {

    private static final String TAG = "ZANA_FCM";

    private FcmDiagnostic() {}

    public static String check() {
        try {
            Log.i(TAG, "========== FCM DIAGNOSTIC START ==========");

            Class<?> firebaseAppClass =
                    Class.forName("com.google.firebase.FirebaseApp");

            Class<?> firebaseMessagingClass =
                    Class.forName("com.google.firebase.messaging.FirebaseMessaging");

            Log.i(TAG, "FirebaseApp class ditemukan");
            Log.i(TAG, "FirebaseMessaging class ditemukan");

            Object firebaseApp =
                    firebaseAppClass.getMethod("getInstance").invoke(null);

            Log.i(TAG, "FirebaseApp.getInstance() BERHASIL");

            Object options =
                    firebaseAppClass.getMethod("getOptions").invoke(firebaseApp);

            String projectId = String.valueOf(
                    options.getClass().getMethod("getProjectId").invoke(options)
            );

            String applicationId = String.valueOf(
                    options.getClass().getMethod("getApplicationId").invoke(options)
            );

            String gcmSenderId = String.valueOf(
                    options.getClass().getMethod("getGcmSenderId").invoke(options)
            );

            Log.i(TAG, "Firebase projectId = " + projectId);
            Log.i(TAG, "Firebase applicationId = " + applicationId);
            Log.i(TAG, "Firebase gcmSenderId = " + gcmSenderId);

            Object messaging =
                    firebaseMessagingClass.getMethod("getInstance").invoke(null);

            Log.i(TAG, "FirebaseMessaging.getInstance() BERHASIL");
            Log.i(TAG, "FirebaseMessaging class = " +
                    messaging.getClass().getName());

            Log.i(TAG, "========== FCM DIAGNOSTIC OK ==========");

            return "FIREBASE_OK";

        } catch (Throwable e) {

            Throwable cause = e;

            if (e.getCause() != null) {
                cause = e.getCause();
            }

            Log.e(TAG, "========== FCM DIAGNOSTIC ERROR ==========", cause);

            return "FCM_ERROR:" +
                    cause.getClass().getName() +
                    ":" +
                    String.valueOf(cause.getMessage());
        }
    }
}
