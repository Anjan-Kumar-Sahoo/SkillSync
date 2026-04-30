import groupsReducer, { updateGroup } from './groupsSlice';

describe('groupsSlice', () => {
  it('updates group if id matches', () => {
    const baseGroup = {
      description: 'Desc',
      category: 'Cat',
      createdBy: 1,
      createdByName: 'Owner',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      memberCount: 1,
      members: []
    };
    const initialState: any = { 
      groups: [
        { ...baseGroup, id: 1, name: 'A' }, 
        { ...baseGroup, id: 2, name: 'B' }
      ],
      myGroups: [],
      selectedGroup: null,
      isLoading: false,
      error: null,
      totalElements: 2,
      totalPages: 1
    };
    const updated: any = { ...baseGroup, id: 2, name: 'B2' };
    const nextState = groupsReducer(initialState, updateGroup(updated));
    expect(nextState.groups[1].name).toBe('B2');
  });
});
