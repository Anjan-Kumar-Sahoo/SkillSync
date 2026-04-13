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
