type RuntimeConfig = {
  apiBaseUrl?: string;
};

import { environment } from '../environments/environment';

declare global {
  interface Window {
    __env?: RuntimeConfig;
  }
}

const runtimeConfig = window.__env ?? {};
const configuredApiBaseUrl = runtimeConfig.apiBaseUrl?.trim();
const isRelativeApiBaseUrl =
  configuredApiBaseUrl != null &&
  configuredApiBaseUrl.length > 0 &&
  !/^https?:\/\//i.test(configuredApiBaseUrl);
const isAngularDevServer =
  window.location.hostname === 'localhost' && window.location.port === '4200';

export const apiBaseUrl =
  isAngularDevServer && isRelativeApiBaseUrl
    ? environment.apiBaseUrl
    : configuredApiBaseUrl || environment.apiBaseUrl;
