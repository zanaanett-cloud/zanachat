package id.zananet.keluargazana;

import android.content.Context;
import android.util.Log;

public final class FcmDiagnostic {

    private static final String TAG = "ZANA_FCM";

    private FcmDiagnostic() {}

    public static String check(Context context) {

        try {

            Log.i(TAG, "========== FCM DIAGNOSTIC START ==========");

            Class<?> firebaseAppClass =
                    Class.forName("com.google.firebase.FirebaseApp");

            Class<?> firebaseMessagingClass =
                    Class.forName(
                            "com.google.firebase.messaging.FirebaseMessaging"
                    );

            Log.i(TAG, "FirebaseApp class ditemukan");
            Log.i(TAG, "FirebaseMessaging class ditemukan");

            /*
             * =====================================================
             * 1. COBA AMBIL DEFAULT FIREBASE APP
             * =====================================================
             */

            Object firebaseApp;

            try {

                firebaseApp =
                        firebaseAppClass
                                .getMethod("getInstance")
                                .invoke(null);

                Log.i(TAG, "FirebaseApp.getInstance() BERHASIL");

            } catch (Throwable notInitialized) {

                Log.w(
                        TAG,
                        "Default FirebaseApp belum initialized. " +
                        "Menjalankan FirebaseApp.initializeApp()."
                );

                /*
                 * =================================================
                 * 2. INITIALIZE FIREBASE SECARA MANUAL
                 * =================================================
                 */

                Object initialized =
                        firebaseAppClass
                                .getMethod(
                                        "initializeApp",
                                        Context.class
                                )
                                .invoke(
                                        null,
                                        context.getApplicationContext()
                                );

                if (initialized == null) {

                    return
                            "FIREBASE INIT GAGAL\n\n" +
                            "FirebaseApp.initializeApp() mengembalikan NULL.\n\n" +
                            "Periksa google-services.json dan Firebase configuration.";

                }

                Log.i(
                        TAG,
                        "FirebaseApp.initializeApp() BERHASIL"
                );

                firebaseApp = initialized;
            }

            /*
             * =====================================================
             * 3. BACA FIREBASE OPTIONS
             * =====================================================
             */

            Object options =
                    firebaseAppClass
                            .getMethod("getOptions")
                            .invoke(firebaseApp);

            String projectId =
                    String.valueOf(
                            options.getClass()
                                    .getMethod("getProjectId")
                                    .invoke(options)
                    );

            String applicationId =
                    String.valueOf(
                            options.getClass()
                                    .getMethod("getApplicationId")
                                    .invoke(options)
                    );

            String gcmSenderId =
                    String.valueOf(
                            options.getClass()
                                    .getMethod("getGcmSenderId")
                                    .invoke(options)
                    );

            Log.i(TAG, "Project ID = " + projectId);
            Log.i(TAG, "Application ID = " + applicationId);
            Log.i(TAG, "Sender ID = " + gcmSenderId);

            /*
             * =====================================================
             * 4. FIREBASE MESSAGING
             * =====================================================
             */

            Object messaging =
                    firebaseMessagingClass
                            .getMethod("getInstance")
                            .invoke(null);

            Log.i(
                    TAG,
                    "FirebaseMessaging.getInstance() BERHASIL"
            );

            /*
             * =====================================================
             * 5. AMBIL FCM TOKEN
             * =====================================================
             */

            Object task =
                    firebaseMessagingClass
                            .getMethod("getToken")
                            .invoke(messaging);

            if (task == null) {

                return
                        "FCM ERROR\n\n" +
                        "FirebaseMessaging.getToken() menghasilkan NULL.";

            }

            Log.i(
                    TAG,
                    "FirebaseMessaging.getToken() dipanggil"
            );

            Class<?> taskClass = task.getClass();

            long mulai = System.currentTimeMillis();

            long timeout = 20000;

            while (true) {

                boolean complete =
                        (Boolean)
                        taskClass
                                .getMethod("isComplete")
                                .invoke(task);

                if (complete) {
                    break;
                }

                if (
                        System.currentTimeMillis() - mulai
                                > timeout
                ) {

                    return
                            "FCM TIMEOUT\n\n" +
                            "Firebase berhasil di-initialize,\n" +
                            "tetapi getToken() belum selesai dalam 20 detik.";

                }

                Thread.sleep(200);
            }

            /*
             * =====================================================
             * 6. CEK HASIL TOKEN
             * =====================================================
             */

            boolean successful =
                    (Boolean)
                    taskClass
                            .getMethod("isSuccessful")
                            .invoke(task);

            if (!successful) {

                Object exception =
                        taskClass
                                .getMethod("getException")
                                .invoke(task);

                String error;

                if (exception == null) {

                    error = "Unknown Firebase error";

                } else {

                    error =
                            exception.getClass().getName()
                            + "\n"
                            + String.valueOf(exception);

                }

                Log.e(
                        TAG,
                        "FCM TOKEN ERROR = " + error
                );

                return
                        "FCM TOKEN ERROR\n\n" +
                        error;

            }

            Object result =
                    taskClass
                            .getMethod("getResult")
                            .invoke(task);

            String token =
                    String.valueOf(result);

            if (
                    token == null
                    || token.trim().isEmpty()
                    || "null".equalsIgnoreCase(token)
            ) {

                return
                        "FCM TOKEN KOSONG\n\n" +
                        "Firebase Messaging aktif,\n" +
                        "tetapi token kosong.";

            }

            Log.i(
                    TAG,
                    "FCM TOKEN BERHASIL DIDAPAT"
            );

            Log.i(
                    TAG,
                    "FCM TOKEN PANJANG = "
                    + token.length()
            );

            /*
             * Jangan tampilkan token lengkap.
             * Kita hanya tampilkan sebagian untuk keamanan.
             */

            String tokenPreview;

            if (token.length() > 24) {

                tokenPreview =
                        token.substring(0, 12)
                        + "..."
                        + token.substring(token.length() - 12);

            } else {

                tokenPreview = token;

            }

            return
                    "FCM BERHASIL\n\n" +

                    "Firebase:\nOK\n\n" +

                    "Project ID:\n"
                    + projectId +

                    "\n\nApplication ID:\n"
                    + applicationId +

                    "\n\nSender ID:\n"
                    + gcmSenderId +

                    "\n\nFirebase Messaging:\nOK\n\n" +

                    "FCM TOKEN:\nADA\n\n" +

                    "Token Preview:\n"
                    + tokenPreview;

        } catch (Throwable e) {

            Throwable cause = e;

            if (e.getCause() != null) {
                cause = e.getCause();
            }

            Log.e(
                    TAG,
                    "========== FCM DIAGNOSTIC ERROR ==========",
                    cause
            );

            return
                    "FIREBASE ERROR\n\n"
                    + cause.getClass().getName()
                    + "\n\n"
                    + String.valueOf(cause);
        }
    }
}
