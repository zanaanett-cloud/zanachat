import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'id.my.zananet.keluargazana',
  appName: 'Keluarga Zana',
  webDir: 'public',
  server: {
    url: 'https://chat.zananet.my.id',
    cleartext: false,
    androidScheme: 'https'
  },
  android: {
    allowMixedContent: false
  }
};

export default config;
