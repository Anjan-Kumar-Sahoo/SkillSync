import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';

export interface ReviewData {
  id: number;
  mentorId: number;
  mentorName: string;
  learnerId: number;
  learnerName: string;
  sessionId: number;
  rating: number;
  comment: string;
  isAnonymous: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ReviewsState {
  reviews: ReviewData[];
  mentorReviews: ReviewData[];
  myReviews: ReviewData[];
  selectedReview: ReviewData | null;
  isLoading: boolean;
  error: string | null;
  totalElements: number;
  currentPage: number;
}

const initialState: ReviewsState = {
  reviews: [],
  mentorReviews: [],
  myReviews: [],
  selectedReview: null,
  isLoading: false,
  error: null,
  totalElements: 0,
  currentPage: 0,
};

const reviewsSlice = createSlice({
  name: 'reviews',
  initialState,
  reducers: {
    setReviews: (state, action: PayloadAction<ReviewData[]>) => {
      state.reviews = action.payload;
    },
    setMentorReviews: (state, action: PayloadAction<ReviewData[]>) => {
      state.mentorReviews = action.payload;
    },
    setMyReviews: (state, action: PayloadAction<ReviewData[]>) => {
      state.myReviews = action.payload;
    },
    addReview: (state, action: PayloadAction<ReviewData>) => {
      state.reviews.unshift(action.payload);
      state.myReviews.unshift(action.payload);
    },
    updateReview: (state, action: PayloadAction<ReviewData>) => {
      const index = state.reviews.findIndex(r => r.id === action.payload.id);
      if (index >= 0) {
        state.reviews[index] = action.payload;
      }
      const myIndex = state.myReviews.findIndex(r => r.id === action.payload.id);
      if (myIndex >= 0) {
        state.myReviews[myIndex] = action.payload;
      }
    },
    removeReview: (state, action: PayloadAction<number>) => {
      state.reviews = state.reviews.filter(r => r.id !== action.payload);
      state.myReviews = state.myReviews.filter(r => r.id !== action.payload);
    },
    setSelectedReview: (state, action: PayloadAction<ReviewData | null>) => {
      state.selectedReview = action.payload;
    },
    setReviewsLoading: (state, action: PayloadAction<boolean>) => {
      state.isLoading = action.payload;
    },
    setReviewsError: (state, action: PayloadAction<string | null>) => {
      state.error = action.payload;
    },
    setReviewsTotalElements: (state, action: PayloadAction<number>) => {
      state.totalElements = action.payload;
    },
    setReviewsPage: (state, action: PayloadAction<number>) => {
      state.currentPage = action.payload;
    },
  },
});

export const {
  setReviews,
  setMentorReviews,
  setMyReviews,
  addReview,
  updateReview,
  removeReview,
  setSelectedReview,
  setReviewsLoading,
  setReviewsError,
  setReviewsTotalElements,
  setReviewsPage,
} = reviewsSlice.actions;

export default reviewsSlice.reducer;
