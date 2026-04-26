import { TextEncoder, TextDecoder } from 'util';
Object.assign(global, { TextDecoder, TextEncoder });

import '@testing-library/jest-dom';
import 'whatwg-fetch';
import { beforeAll, afterEach, afterAll, jest } from '@jest/globals';
import { server } from './mocks/server';

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));

afterEach(() => {
  jest.clearAllTimers();
  jest.useRealTimers();
  jest.clearAllMocks();
  server.resetHandlers();
});

afterAll(() => server.close());

if (typeof window !== 'undefined' && !window.matchMedia) {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: jest.fn().mockImplementation((query) => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: jest.fn(),
      removeListener: jest.fn(),
      addEventListener: jest.fn(),
      removeEventListener: jest.fn(),
      dispatchEvent: jest.fn(),
    })),
  });
}

if (typeof window !== 'undefined' && !window.scrollTo) {
  Object.defineProperty(window, 'scrollTo', {
    writable: true,
    value: jest.fn(),
  });
}
