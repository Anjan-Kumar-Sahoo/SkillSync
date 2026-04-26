import { queryClient } from './queryClient';

describe('queryClient', () => {
  it('uses expected default query options', () => {
    const defaults = queryClient.getDefaultOptions();

    expect(defaults.queries?.staleTime).toBe(5 * 60 * 1000);
    expect(defaults.queries?.retry).toBe(2);
    expect(defaults.queries?.refetchOnWindowFocus).toBe(false);
  });
});
