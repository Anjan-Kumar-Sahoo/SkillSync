import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';

export interface GroupMember {
  id: number;
  userId: number;
  name: string;
  email: string;
  profileImage?: string;
  joinedAt: string;
}

export interface GroupData {
  id: number;
  name: string;
  description: string;
  category: string;
  maxMembers?: number;
  createdBy: number;
  createdByName: string;
  memberCount: number;
  members?: GroupMember[];
  isJoined: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface GroupsState {
  groups: GroupData[];
  myGroups: GroupData[];
  selectedGroup: GroupData | null;
  isLoading: boolean;
  error: string | null;
  totalElements: number;
  currentPage: number;
  searchQuery: string;
}

const initialState: GroupsState = {
  groups: [],
  myGroups: [],
  selectedGroup: null,
  isLoading: false,
  error: null,
  totalElements: 0,
  currentPage: 0,
  searchQuery: '',
};

const groupsSlice = createSlice({
  name: 'groups',
  initialState,
  reducers: {
    setGroups: (state, action: PayloadAction<GroupData[]>) => {
      state.groups = action.payload;
    },
    setMyGroups: (state, action: PayloadAction<GroupData[]>) => {
      state.myGroups = action.payload;
    },
    addGroup: (state, action: PayloadAction<GroupData>) => {
      state.groups.push(action.payload);
      state.myGroups.push(action.payload);
    },
    updateGroup: (state, action: PayloadAction<GroupData>) => {
      const index = state.groups.findIndex(g => g.id === action.payload.id);
      if (index >= 0) {
        state.groups[index] = action.payload;
      }
    },
    removeGroup: (state, action: PayloadAction<number>) => {
      state.groups = state.groups.filter(g => g.id !== action.payload);
      state.myGroups = state.myGroups.filter(g => g.id !== action.payload);
    },
    setSelectedGroup: (state, action: PayloadAction<GroupData | null>) => {
      state.selectedGroup = action.payload;
    },
    setGroupsLoading: (state, action: PayloadAction<boolean>) => {
      state.isLoading = action.payload;
    },
    setGroupsError: (state, action: PayloadAction<string | null>) => {
      state.error = action.payload;
    },
    setGroupsTotalElements: (state, action: PayloadAction<number>) => {
      state.totalElements = action.payload;
    },
    setGroupsPage: (state, action: PayloadAction<number>) => {
      state.currentPage = action.payload;
    },
    setGroupsSearchQuery: (state, action: PayloadAction<string>) => {
      state.searchQuery = action.payload;
    },
    joinGroup: (state, action: PayloadAction<number>) => {
      const group = state.groups.find(g => g.id === action.payload);
      if (group) {
        group.isJoined = true;
        group.memberCount += 1;
        state.myGroups.push(group);
      }
    },
    leaveGroup: (state, action: PayloadAction<number>) => {
      const group = state.groups.find(g => g.id === action.payload);
      if (group) {
        group.isJoined = false;
        group.memberCount -= 1;
      }
      state.myGroups = state.myGroups.filter(g => g.id !== action.payload);
    },
  },
});

export const {
  setGroups,
  setMyGroups,
  addGroup,
  updateGroup,
  removeGroup,
  setSelectedGroup,
  setGroupsLoading,
  setGroupsError,
  setGroupsTotalElements,
  setGroupsPage,
  setGroupsSearchQuery,
  joinGroup,
  leaveGroup,
} = groupsSlice.actions;

export default groupsSlice.reducer;
