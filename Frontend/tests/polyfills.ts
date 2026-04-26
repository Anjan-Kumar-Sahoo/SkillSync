import { TextEncoder, TextDecoder } from 'util';
import { ReadableStream, WritableStream, TransformStream } from 'stream/web';
class BroadcastChannel {
  name: string;
  constructor(name: string) { this.name = name; }
  close() {}
  postMessage() {}
  onmessage() {}
  addEventListener() {}
  removeEventListener() {}
  dispatchEvent() { return true; }
}
Object.assign(global, { TextDecoder, TextEncoder, ReadableStream, WritableStream, TransformStream, BroadcastChannel });
