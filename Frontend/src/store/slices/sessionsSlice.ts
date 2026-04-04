import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';

export interface SessionData {
  id: number;
  mentorId: number;
  learnerId: number;
  mentorName: string;
  learnerName: string;
  sessionDate: string;
  sessionDuration: number;
  sessionFees: number;
  status: 'REQUESTED' | 'ACCEPTED' | 'REJECTED' | 'COMPLETED' | 'CANCELLED';
  mentorFeedback?: string;
  learnerRating?: number;
  createdAt: string;
  updatedAt: string;
}

interface SessionsState {
  sessions: SessionData[];
  upcomingSessions: SessionData[];
  completedSessions: SessionData[];
  pendingSessions: SessionData[];
  cancelledSessions: SessionData[];
  selectedSession: SessionData | null;
  isLoading: boolean;
  error: string | null;
  totalElements: number;
  currentPage: number;
}

const initialState: SessionsState = {
  sessions: [],
  upcomingSessions: [],
  completedSessions: [],
  pendingSessions: [],
  cancelledSessions: [],
  selectedSession: null,
  isLoading: false,
  error: null,
  totalElements: 0,
  currentPage: 0,
};

const sessionsSlice = createSlice({
  name: 'sessions',
  initialState,
  reducers: {
    setSessions: (state, action: PayloadAction<SessionData[]>) => {
      state.sessions = action.payload;
    },
    setUpcomingSessions: (state, action: PayloadAction<SessionData[]>) => {
      state.upcomingSessions = action.payload;
    },
    setCompletedSessions: (state, action: PayloadAction<SessionData[]>) => {
      state.completedSessions = action.payload;
    },
    setPendingSessions: (state, action: PayloadAction<SessionData[]>) => {
      state.pendingSessions = action.payload;
    },
    setCancelledSessions: (state, action: PayloadAction<SessionData[]>) => {
      state.cancelledSessions = action.payload;
    },
    addSession: (state, action: PayloadAction<SessionData>) => {
      state.sessions.unshift(action.payload);
    },
    updateSession: (state, action: PayloadAction<SessionData>) => {
      const index = state.sessions.findIndex(s => s.id === action.payload.id);
      if (index >= 0) {
        state.sessions[index] = action.payload;
      }
    },
    deleteSession: (state, action: PayloadAction<number>) => {
      state.sessions = state.sessions.filter(s => s.id !== action.payload);
    },
    setSelectedSession: (state, action: PayloadAction<SessionData | null>) => {
      state.selectedSession = action.payload;
    },
    setSessionsLoading: (state, action: PayloadAction<boolean>) => {
      state.isLoading = action.payload;
    },
    setSessionsError: (state, action: PayloadAction<string | null>) => {
      state.error = action.payload;
    },
    setSessionsTotalElements: (state, action: PayloadAction<number>) => {
      state.totalElements = action.payload;
    },
    setSessionsPage: (state, action: PayloadAction<number>) => {
      state.currentPage = action.payload;
    },
  },
});

export const {
  setSessions,
  setUpcomingSessions,
  setCompletedSessions,
  setPendingSessions,
  setCancelledSessions,
  addSession,
  updateSession,
  deleteSession,
  setSelectedSession,
  setSessionsLoading,
  setSessionsError,
  setSessionsTotalElements,
  setSessionsPage,
} = sessionsSlice.actions;

export default sessionsSlice.reducer;
