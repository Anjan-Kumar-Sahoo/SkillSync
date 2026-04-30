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

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

interface ImportMetaEnv {
  readonly PROD: boolean;
  readonly VITE_API_URL: string;
  // Add other env variables as needed
}
