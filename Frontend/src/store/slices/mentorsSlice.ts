import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';

export interface MentorSkill {
  id: number;
  name: string;
  category: string;
}

export interface MentorData {
  id: number;
  userId: number;
  name: string;
  email: string;
  profileImage?: string;
  bio: string;
  experience: number;
  hourlyRate: number;
  rating: number;
  reviewCount: number;
  isApproved: boolean;
  skills: MentorSkill[];
  availability?: string[];
  createdAt: string;
  updatedAt: string;
}

export interface MentorsState {
  mentors: MentorData[];
  selectedMentor: MentorData | null;
  myMentorProfile: MentorData | null;
  isLoading: boolean;
  error: string | null;
  totalElements: number;
  currentPage: number;
  filters: {
    skill: string;
    minRating: number;
    maxPrice: number;
    minPrice: number;
    search: string;
  };
}

const initialState: MentorsState = {
  mentors: [],
  selectedMentor: null,
  myMentorProfile: null,
  isLoading: false,
  error: null,
  totalElements: 0,
  currentPage: 0,
  filters: {
    skill: '',
    minRating: 0,
    maxPrice: 10000,
    minPrice: 0,
    search: '',
  },
};

const mentorsSlice = createSlice({
  name: 'mentors',
  initialState,
  reducers: {
    setMentors: (state, action: PayloadAction<MentorData[]>) => {
      state.mentors = action.payload;
    },
    addMentor: (state, action: PayloadAction<MentorData>) => {
      state.mentors.push(action.payload);
    },
    updateMentor: (state, action: PayloadAction<MentorData>) => {
      const index = state.mentors.findIndex(m => m.id === action.payload.id);
      if (index >= 0) {
        state.mentors[index] = action.payload;
      }
    },
    setSelectedMentor: (state, action: PayloadAction<MentorData | null>) => {
      state.selectedMentor = action.payload;
    },
    setMyMentorProfile: (state, action: PayloadAction<MentorData | null>) => {
      state.myMentorProfile = action.payload;
    },
    setMentorsLoading: (state, action: PayloadAction<boolean>) => {
      state.isLoading = action.payload;
    },
    setMentorsError: (state, action: PayloadAction<string | null>) => {
      state.error = action.payload;
    },
    setMentorsTotalElements: (state, action: PayloadAction<number>) => {
      state.totalElements = action.payload;
    },
    setMentorsPage: (state, action: PayloadAction<number>) => {
      state.currentPage = action.payload;
    },
    setMentorsFilters: (state, action: PayloadAction<Partial<MentorsState['filters']>>) => {
      state.filters = { ...state.filters, ...action.payload };
    },
    clearMentorsFilters: (state) => {
      state.filters = initialState.filters;
    },
  },
});

export const {
  setMentors,
  addMentor,
  updateMentor,
  setSelectedMentor,
  setMyMentorProfile,
  setMentorsLoading,
  setMentorsError,
  setMentorsTotalElements,
  setMentorsPage,
  setMentorsFilters,
  clearMentorsFilters,
} = mentorsSlice.actions;

export default mentorsSlice.reducer;
