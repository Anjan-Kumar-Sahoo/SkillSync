/// <reference types="vite/client" />

declare module "*.png" {
  const value: string;
  export default value;
}

declare module "*.svg" {
  const value: string;
  export default value;
}

declare module "*.css" {
  const content: { [className: string]: string };
  export default content;
}

declare module 'mammoth/mammoth.browser' {
  export interface MammothConversionResult {
    value: string;
  }

  export function convertToHtml(input: { arrayBuffer: ArrayBuffer }): Promise<MammothConversionResult>;

  const mammoth: {
    convertToHtml: typeof convertToHtml;
  };

  export default mammoth;
}

interface ImportMetaEnv {
  readonly VITE_API_URL: string;
  readonly VITE_AUTH_GOOGLE_CLIENT_ID: string;
  readonly VITE_LEARNER_DASHBOARD_URL: string;
  readonly VITE_MENTOR_DASHBOARD_URL: string;
  readonly VITE_ADMIN_DASHBOARD_URL: string;
  readonly VITE_SUPPORT_EMAIL: string;
  readonly VITE_PLATFORM_NAME: string;
  // Add other env variables as needed
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
