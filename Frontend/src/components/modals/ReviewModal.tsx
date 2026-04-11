import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import api from '../../services/axios';
import { useToast } from '../ui/Toast';

interface ReviewModalProps {
  isOpen: boolean;
  onClose: () => void;
  mentorId: number;
  sessionId: number;
  onSuccess: () => void;
}

const ReviewModal = ({ isOpen, onClose, mentorId, sessionId, onSuccess }: ReviewModalProps) => {
  const [rating, setRating] = useState(0);
  const [hoverRating, setHoverRating] = useState(0);
  const [comment, setComment] = useState('');
  const { showToast } = useToast();

  const reviewMutation = useMutation({
    mutationFn: async () => {
      const trimmedComment = comment.trim();
      const payload = {
        sessionId,
        mentorId,
        rating,
        comment: trimmedComment,
      };
      const res = await api.post('/api/reviews', payload);
      return res.data;
    },
    onSuccess: () => {
      showToast({ message: 'Review submitted successfully!', type: 'success' });
      setRating(0);
      setHoverRating(0);
      setComment('');
      onSuccess();
      onClose();
    },
    onError: (error: any) => {
      const message = error?.response?.data?.message || 'Failed to submit review. Please try again.';
      showToast({ message, type: 'error' });
    }
  });

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-surface-container-lowest rounded-2xl p-8 max-w-md w-full shadow-2xl relative animate-in zoom-in-95 duration-200">
        
        <button 
          onClick={onClose}
          className="absolute top-4 right-4 p-2 rounded-full hover:bg-surface-container transition-colors text-on-surface-variant flex items-center justify-center"
        >
          <span className="material-symbols-outlined text-[20px]">close</span>
        </button>

        <h2 className="text-2xl font-extrabold text-on-surface mb-6">Rate Your Session</h2>
        
        <div className="flex justify-center gap-2 mb-6" onMouseLeave={() => setHoverRating(0)}>
          {[1, 2, 3, 4, 5].map((star) => (
            <button
              key={star}
              onMouseEnter={() => setHoverRating(star)}
              onClick={() => setRating(star)}
              className="transition-transform hover:scale-110 active:scale-95 outline-none"
            >
              <span className={`material-symbols-outlined text-4xl transition-colors ${
                star <= (hoverRating || rating) 
                  ? 'text-amber-400 font-solid filled' 
                  : 'text-outline-variant/30 font-light'
              }`} style={{ fontVariationSettings: star <= (hoverRating || rating) ? "'FILL' 1" : "'FILL' 0" }}>
                star
              </span>
            </button>
          ))}
        </div>

        <textarea
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          placeholder="Share your experience (what went well, what could be improved?)..."
          className="w-full min-h-[120px] bg-surface-container-low border border-outline-variant/30 rounded-xl p-4 text-sm outline-none focus:ring-1 focus:ring-primary focus:border-primary transition-all resize-none mb-6 text-on-surface placeholder:text-on-surface-variant/60"
        ></textarea>

        <div className="flex gap-3">
          <button 
            onClick={onClose}
            className="flex-1 bg-surface-container hover:bg-surface-container-high text-on-surface font-bold py-3 rounded-xl transition-colors"
          >
            Cancel
          </button>
          <button 
            onClick={() => reviewMutation.mutate()}
            disabled={rating === 0 || reviewMutation.isPending}
            className="flex-1 gradient-btn text-white font-bold py-3 rounded-xl shadow-md hover:shadow-lg disabled:opacity-50 disabled:scale-100 active:scale-[0.98] transition-all flex items-center justify-center gap-2"
          >
            {reviewMutation.isPending ? (
              <span className="material-symbols-outlined animate-spin text-[20px]">autorenew</span>
            ) : (
              'Submit Review'
            )}
          </button>
        </div>

      </div>
    </div>
  );
};

export default ReviewModal;
