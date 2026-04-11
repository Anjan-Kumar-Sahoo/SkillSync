export interface UserSummary {
  id: number;
  email: string;
  role: string;
  firstName: string;
  lastName: string;
  skills?: string[];
  bio?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  tokenType: string;
  user: UserSummary;
}

export interface OAuthResponse extends AuthResponse {
  passwordSetupRequired: boolean;
}

export interface MentorProfile {
  id: number;
  userId: number;
  bio: string;
  experienceYears: number;
  hourlyRate: number;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'SUSPENDED';
  averageRating: number;
  skills: string[];
}

export interface Session {
  id: number;
  mentorId: number;
  learnerId: number;
  scheduledAt: string;
  durationMinutes: number;
  topic: string;
  status: 'REQUESTED' | 'ACCEPTED' | 'REJECTED' | 'CANCELLED' | 'COMPLETED';
}

export interface Notification {
  id: number;
  userId: number;
  title: string;
  message: string;
  isRead: boolean;
  createdAt: string;
}

export interface Skill {
  id: number;
  name: string;
  categoryName: string;
}

export interface Payment {
  id: number;
  razorpayOrderId: string;
  amount: number;
  status: 'CREATED' | 'VERIFIED' | 'SUCCESS_PENDING' | 'SUCCESS' | 'FAILED' | 'COMPENSATED';
  referenceType: string;
}
