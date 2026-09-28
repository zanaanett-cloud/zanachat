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
                    options.getClass()
                            .getMethod("getProjectId")
                            .invoke(options)
            );

            String applicationId = String.valueOf(
                    options.getClass()
                            .getMethod("getApplicationId")
                            .invoke(options)
            );

            String gcmSenderId = String.valueOf(
                    options.getClass()
                            .getMethod("getGcmSenderId")
                            .invoke(options)
            );

            Log.i(TAG, "Project ID = " + projectId);
            Log.i(TAG, "Application ID = " + applicationId);
            Log.i(TAG, "Sender ID = " + gcmSenderId);

            Object messaging =
                    firebaseMessagingClass.getMethod("getInstance").invoke(null);

            Log.i(TAG, "FirebaseMessaging.getInstance() BERHASIL");

            /*
             * Panggil FirebaseMessaging.getToken()
             * menggunakan reflection agar tidak menimbulkan
             * masalah dependency compile.
             */
            Object task =
                    firebaseMessagingClass
                            .getMethod("getToken")
                            .invoke(messaging);

            if (task == null) {
                return "FCM ERROR\n\ngetToken() menghasilkan NULL";
            }

            Log.i(TAG, "FirebaseMessaging.getToken() BERHASIL dipanggil");

            Class<?> taskClass = task.getClass();

            long mulai = System.currentTimeMillis();
            long timeout = 15000;

            while (true) {
                boolean complete = (Boolean) taskClass
                        .getMethod("isComplete")
                        .invoke(task);

                if (complete) {
                    break;
                }

                if (System.currentTimeMillis() - mulai > timeout) {
                    return "FCM TIMEOUT\n\n"
                            + "Firebase tersedia, tetapi getToken() "
                            + "belum selesai dalam 15 detik.";
                }

                Thread.sleep(200);
            }

            boolean successful = (Boolean) taskClass
                    .getMethod("isSuccessful")
                    .invoke(task);

            if (!successful) {
                Object exception = taskClass
                        .getMethod("getException")
                        .invoke(task);

                String error = exception == null
                        ? "Unknown Firebase error"
                        : exception.getClass().getName()
                          + "\n"
                          + String.valueOf(exception);

                Log.e(TAG, "FCM TOKEN ERROR = " + error);

                return "FCM TOKEN ERROR\n\n" + error;
            }

            Object result = taskClass
                    .getMethod("getResult")
                    .invoke(task);

            String token = String.valueOf(result);

            if (token == null || token.trim().isEmpty()
                    || "null".equalsIgnoreCase(token)) {

                return "FCM TOKEN KOSONG\n\n"
                        + "Firebase Messaging aktif tetapi token kosong.";
            }

            Log.i(TAG, "FCM TOKEN BERHASIL DIDAPAT");
            Log.i(TAG, "FCM TOKEN PANJANG = " + token.length());

            return "FCM BERHASIL\n\n"
                    + "Project ID:\n" + projectId
                    + "\n\n"
                    + "Application ID:\n" + applicationId
                    + "\n\n"
                    + "Sender ID:\n" + gcmSenderId
                    + "\n\n"
                    + "Firebase Messaging:\nOK"
                    + "\n\n"
                    + "FCM TOKEN:\n"
                    + token;

        } catch (Throwable e) {

            Throwable cause = e;

            if (e.getCause() != null) {
                cause = e.getCause();
            }

            Log.e(TAG, "========== FCM DIAGNOSTIC ERROR ==========", cause);

            return "FIREBASE ERROR\n\n"
                    + cause.getClass().getName()
                    + "\n\n"
                    + String.valueOf(cause.getMessage());
        }
    }
}
