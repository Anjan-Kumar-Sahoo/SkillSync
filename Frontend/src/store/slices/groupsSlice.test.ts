import groupsReducer, { updateGroup } from './groupsSlice';

describe('groupsSlice', () => {
  it('updates group if id matches', () => {
    const initialState = { groups: [{ id: 1, name: 'A' }, { id: 2, name: 'B' }] };
    const updated = { id: 2, name: 'B2' };
    const nextState = groupsReducer(initialState, updateGroup(updated));
    expect(nextState.groups[1]).toEqual(updated);
  });
});
