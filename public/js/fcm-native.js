(function () {
  'use strict';

  let kzFcmRegistrationStarted = false;
  let kzFcmToken = null;

  function isNativeAndroid() {
    return !!(
      window.Capacitor &&
      typeof window.Capacitor.isNativePlatform === 'function' &&
      window.Capacitor.isNativePlatform()
    );
  }

  function getLoggedUser() {
    try {
      const raw = localStorage.getItem('kz_user');

      if (!raw) return null;

      const user = JSON.parse(raw);

      if (!user || !user.id) return null;

      return user;
    } catch (e) {
      console.error('[FCM] Gagal membaca kz_user:', e);
      return null;
    }
  }

  async function sendTokenToServer(token) {
    if (!token) return false;

    try {
      const user = getLoggedUser();

      if (!user || !user.id) {
        console.log('[FCM] Belum ada user login. Token menunggu login.');
        localStorage.setItem('kz_pending_fcm_token', token);
        return false;
      }

      console.log('[FCM] Mendaftarkan token untuk user:', user.id);

      const response = await fetch(
        'https://chat.zananet.my.id/api/push/register',
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            userId: user.id,
            token: token
          })
        }
      );

      const result = await response.json().catch(function () {
        return {};
      });

      if (response.ok && result.ok) {
        console.log('[FCM] TOKEN BERHASIL TERDAFTAR.');
        localStorage.removeItem('kz_pending_fcm_token');
        return true;
      }

      console.error('[FCM] Server menolak token:', result);
      localStorage.setItem('kz_pending_fcm_token', token);
      return false;

    } catch (error) {
      console.error('[FCM] Gagal mengirim token:', error);
      localStorage.setItem('kz_pending_fcm_token', token);
      return false;
    }
  }

  async function retryPendingToken() {
    try {
      const pending =
        localStorage.getItem('kz_pending_fcm_token');

      if (pending) {
        await sendTokenToServer(pending);
      }

      if (kzFcmToken) {
        await sendTokenToServer(kzFcmToken);
      }
    } catch (e) {
      console.error('[FCM] Retry token error:', e);
    }
  }

  async function registerKeluargaZanaFCM() {
    try {
      if (!isNativeAndroid()) {
        console.log('[FCM] Bukan Android native. FCM native dilewati.');
        return;
      }

      if (
        !window.Capacitor.Plugins ||
        !window.Capacitor.Plugins.PushNotifications
      ) {
        console.warn('[FCM] Plugin PushNotifications belum tersedia.');
        return;
      }

      const PushNotifications =
        window.Capacitor.Plugins.PushNotifications;

      if (kzFcmRegistrationStarted) {
        await retryPendingToken();
        return;
      }

      kzFcmRegistrationStarted = true;

      console.log('[FCM] Mengecek izin notifikasi...');

      let permission =
        await PushNotifications.checkPermissions();

      console.log(
        '[FCM] Permission:',
        permission.receive
      );

      if (permission.receive !== 'granted') {
        permission =
          await PushNotifications.requestPermissions();
      }

      if (permission.receive !== 'granted') {
        console.warn('[FCM] Izin notifikasi belum diberikan.');
        return;
      }

      await PushNotifications.addListener(
        'registration',
        async function (token) {
          try {
            kzFcmToken = token && token.value;

            if (!kzFcmToken) {
              console.warn('[FCM] Token kosong.');
              return;
            }

            console.log('[FCM] Token Android diterima.');

            window.kzFcmToken = kzFcmToken;

            await sendTokenToServer(kzFcmToken);

          } catch (error) {
            console.error(
              '[FCM] Handler registration error:',
              error
            );
          }
        }
      );

      await PushNotifications.addListener(
        'registrationError',
        function (error) {
          console.error(
            '[FCM] REGISTRATION ERROR:',
            error
          );
        }
      );

      await PushNotifications.addListener(
        'pushNotificationReceived',
        function (notification) {
          console.log(
            '[FCM] NOTIFIKASI DITERIMA:',
            notification
          );

          window.dispatchEvent(
            new CustomEvent(
              'kz:fcm-notification',
              {
                detail: notification
              }
            )
          );
        }
      );

      await PushNotifications.addListener(
        'pushNotificationActionPerformed',
        function (event) {
          console.log(
            '[FCM] NOTIFIKASI DIBUKA:',
            event
          );

          window.dispatchEvent(
            new CustomEvent(
              'kz:fcm-action',
              {
                detail: event
              }
            )
          );
        }
      );

      console.log('[FCM] Register perangkat ke Firebase...');

      await PushNotifications.register();

      /*
       * Coba langsung jika user sudah login.
       */
      setTimeout(function () {
        retryPendingToken();
      }, 1000);

    } catch (error) {
      console.error(
        '[FCM] INIT ERROR:',
        error
      );
    }
  }

  /*
   * Fungsi publik supaya app utama bisa memanggil
   * setelah proses login berhasil.
   */
  window.kzRegisterFcmAfterLogin = function () {
    console.log('[FCM] Login terdeteksi. Register token...');
    retryPendingToken();
  };

  window.kzGetFcmToken = function () {
    return kzFcmToken;
  };

  window.registerKeluargaZanaFCM =
    registerKeluargaZanaFCM;

  /*
   * Mulai FCM setelah WebView siap.
   */
  document.addEventListener(
    'DOMContentLoaded',
    function () {
      setTimeout(function () {
        registerKeluargaZanaFCM();
      }, 1500);
    }
  );

})();
