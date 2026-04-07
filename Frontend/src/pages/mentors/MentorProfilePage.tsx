import { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useParams, useNavigate } from 'react-router-dom';
import Navbar from '../../components/layout/Navbar';
import api from '../../services/axios';

const MentorProfilePage = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [currentMonth, setCurrentMonth] = useState(new Date());
  const [selectedDate, setSelectedDate] = useState<Date | null>(null);
  const [selectedSlot, setSelectedSlot] = useState<any | null>(null);
  const [reviewsPage, setReviewsPage] = useState(0);

  const { data: mentor, isLoading: loadingMentor } = useQuery({
    queryKey: ['mentor', id],
    queryFn: async () => {
      const res = await api.get(`/api/mentors/${id}`);
      return res.data;
    }
  });

  const { data: reviewsData } = useQuery({
    queryKey: ['reviews', id, reviewsPage],
    queryFn: async () => {
      const res = await api.get(`/api/reviews/mentor/${id}?page=${reviewsPage}&size=5`);
      return res.data;
    }
  });

  // Calendar Logic
  const calendarDays = useMemo(() => {
    const year = currentMonth.getFullYear();
    const month = currentMonth.getMonth();
    const firstDay = new Date(year, month, 1).getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    
    const days = [];
    for (let i = 0; i < firstDay; i++) days.push(null); // Padding

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    for (let d = 1; d <= daysInMonth; d++) {
      const date = new Date(year, month, d);
      const isPast = date < today;

      const daySlots = (mentor?.availableSlots || []).filter((s: any) => {
        const slotDate = new Date(s.startTime);
        return slotDate.getFullYear() === year && slotDate.getMonth() === month && slotDate.getDate() === d;
      });

      const totalSlots = daySlots.length;
      const hasAvailable = daySlots.some((s: any) => !s.isBooked);
      const allBooked = totalSlots > 0 && !hasAvailable;

      days.push({
        date,
        dayNumber: d,
        isPast,
        totalSlots,
        hasAvailable,
        allBooked,
        isToday: date.getTime() === today.getTime(),
      });
    }
    return days;
  }, [currentMonth, mentor?.availableSlots]);

  const availableTimes = useMemo(() => {
    if (!selectedDate || !mentor?.availableSlots) return [];
    
    return mentor.availableSlots
      .filter((s: any) => {
        const d = new Date(s.startTime);
        return d.toDateString() === selectedDate.toDateString() && !s.isBooked;
      })
      .sort((a: any, b: any) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime());
  }, [selectedDate, mentor?.availableSlots]);

  const handleBook = () => {
    if (!selectedSlot || !mentor) return;
    navigate('/checkout', { 
      state: { 
        mentorId: mentor.id, 
        mentorName: `${mentor.firstName} ${mentor.lastName}`, 
        slotId: selectedSlot.id, 
        startTime: selectedSlot.startTime, 
        hourlyRate: mentor.hourlyRate 
      } 
    });
  };

  const getInitials = (first?: string, last?: string) => `${first?.[0] || ''}${last?.[0] || ''}`.toUpperCase();
  
  if (loadingMentor) {
    return (
      <div className="flex flex-col h-screen overflow-hidden">
        <Navbar />
        <div className="flex-1 flex items-center justify-center animate-pulse bg-surface">
          <div className="w-16 h-16 rounded-full bg-primary/20"></div>
        </div>
      </div>
    );
  }

  if (!mentor) return <div>Mentor not found</div>;

  const initials = getInitials(mentor.firstName, mentor.lastName);
  const colorIndex = mentor.firstName.charCodeAt(0) % 5;
  const colors = ['from-blue-500 to-indigo-500', 'from-emerald-400 to-teal-500', 'from-violet-500 to-purple-500', 'from-amber-400 to-orange-500', 'from-rose-400 to-red-500'];
  const avatarClass = colors[colorIndex];

  return (
    <div className="flex flex-col min-h-screen bg-surface font-sans">
      <Navbar />
      
      <main className="flex-1 overflow-y-auto pt-6 pb-20 px-4 md:px-8">
        <div className="max-w-[1100px] mx-auto flex flex-col lg:flex-row gap-8 items-start">
          
          {/* LEFT COLUMN */}
          <div className="flex-1 w-full max-w-2xl space-y-10">
            {/* HERo */}
            <div className="bg-surface-container-lowest p-8 rounded-2xl shadow-sm border border-outline-variant/10 relative">
              <button onClick={() => navigate(-1)} className="absolute top-4 right-4 p-2 text-on-surface-variant hover:text-primary transition-colors flex items-center bg-surface-container hover:bg-surface-container-high rounded-full">
                <span className="material-symbols-outlined text-[20px]">close</span>
              </button>

              <div className="flex flex-col md:flex-row gap-6 items-start md:items-center">
                <div className="relative shrink-0">
                  <div className={`w-24 h-24 rounded-2xl bg-gradient-to-tr ${avatarClass} text-white flex items-center justify-center text-3xl font-black shadow-lg`}>
                    {initials}
                  </div>
                  <div className="absolute -bottom-2 -left-2 bg-gray-900/90 backdrop-blur-sm text-white rounded-lg px-2.5 py-1 text-sm font-bold shadow-md flex items-center gap-1 border border-white/10">
                    <span className="text-amber-400">★</span> {mentor.rating?.toFixed(1) || 'NEW'}
                  </div>
                </div>

                <div className="flex-1">
                  <h1 className="text-4xl font-extrabold text-on-surface tracking-tight leading-none mb-2">
                    {mentor.firstName} {mentor.lastName}
                  </h1>
                  <p className="text-lg font-medium text-on-surface-variant mb-4">{mentor.headline}</p>
                  
                  <div className="flex flex-wrap items-center gap-4 text-sm font-semibold text-on-surface-variant bg-surface-container-low/50 p-3 rounded-xl border border-outline-variant/10 inline-flex">
                    <span className="flex items-center gap-1"><span className="material-symbols-outlined text-[18px]">location_on</span> Remote</span>
                    <span className="w-1 h-1 rounded-full bg-outline-variant"></span>
                    <span className="flex items-center gap-1"><span className="material-symbols-outlined text-[18px]">work</span> {mentor.experienceYears || 5}+ yrs exp</span>
                    <span className="w-1 h-1 rounded-full bg-outline-variant"></span>
                    <span className="flex items-center gap-1"><span className="material-symbols-outlined text-[18px]">chat</span> English</span>
                  </div>
                </div>
              </div>

              <div className="flex flex-col sm:flex-row gap-3 mt-8">
                <button 
                  onClick={() => document.getElementById('booking-card')?.scrollIntoView({ behavior: 'smooth' })}
                  className="gradient-btn text-white px-8 py-3 rounded-xl font-bold shadow-md hover:shadow-lg hover:scale-[1.02] active:scale-[0.98] transition-all flex items-center justify-center gap-2 flex-1 sm:flex-none"
                >
                  Book Session <span className="material-symbols-outlined text-[20px]">calendar_month</span>
                </button>
                <button className="bg-surface-container hover:bg-surface-container-high text-on-surface px-8 py-3 rounded-xl font-bold transition-colors flex items-center justify-center gap-2 border border-transparent hover:border-outline-variant/20 flex-1 sm:flex-none">
                  Message <span className="material-symbols-outlined text-[20px]">send</span>
                </button>
              </div>
            </div>

            {/* ABOUT */}
            <section className="bg-surface-container-lowest p-8 rounded-2xl shadow-sm border border-outline-variant/10">
              <h2 className="text-2xl font-extrabold text-on-surface mb-4">About</h2>
              <p className="text-on-surface text-base leading-relaxed whitespace-pre-line">{mentor.bio || "No biography provided by the mentor yet."}</p>
            </section>

            {/* EXPERTISE */}
            <section className="bg-surface-container-lowest p-8 rounded-2xl shadow-sm border border-outline-variant/10">
              <h2 className="text-2xl font-extrabold text-on-surface mb-4">Expertise</h2>
              <div className="flex flex-wrap gap-2">
                {(mentor.skills || []).map((skill: any, idx: number) => (
                  <span key={idx} className="bg-secondary-container/20 text-on-secondary-container border border-primary/20 rounded-full px-4 py-1.5 text-sm font-extrabold uppercase tracking-wider shadow-sm">
                    {typeof skill === 'string' ? skill : (skill.name || `Skill #${skill.skillId || skill.id}`)}
                  </span>
                ))}
                {!mentor.skills?.length && <span className="text-on-surface-variant font-medium">No specific skills listed.</span>}
              </div>
            </section>

            {/* REVIEWS */}
            <section className="bg-surface-container-lowest p-8 rounded-2xl shadow-sm border border-outline-variant/10">
              <div className="flex justify-between items-center mb-6">
                <h2 className="text-2xl font-extrabold text-on-surface">Student Reviews <span className="text-on-surface-variant font-bold">({mentor.reviewCount || reviewsData?.totalElements || 0})</span></h2>
              </div>
              
              <div className="space-y-4">
                {reviewsData?.content?.length > 0 ? (
                  reviewsData.content.map((review: any) => (
                    <div key={review.id} className="bg-surface-container-low/50 rounded-xl p-6 border border-outline-variant/10 hover:border-outline-variant/30 transition-colors">
                      <div className="flex justify-between items-start mb-3">
                        <div className="flex items-center gap-3">
                          <div className="w-10 h-10 rounded-full bg-surface-container-highest text-on-surface flex items-center justify-center font-bold text-sm">
                            {getInitials(review.learnerName, ' ')}
                          </div>
                          <div>
                            <p className="font-bold text-on-surface">{review.learnerName}</p>
                            <p className="text-xs font-semibold text-on-surface-variant uppercase tracking-widest">{new Date(review.createdAt).toLocaleDateString()}</p>
                          </div>
                        </div>
                        <div className="flex text-amber-500 text-[14px]">
                          {Array(5).fill(0).map((_, i) => (
                            <span key={i} className={i < review.rating ? 'material-symbols-outlined' : 'material-symbols-outlined text-outline-variant/30'}>
                              star
                            </span>
                          ))}
                        </div>
                      </div>
                      <p className="italic text-on-surface-variant text-sm pl-1 border-l-2 border-primary/20 ml-2">"{review.comment}"</p>
                    </div>
                  ))
                ) : (
                  <p className="text-on-surface-variant font-medium italic text-center py-8">No reviews yet for this mentor.</p>
                )}
              </div>

              {reviewsData && !reviewsData.last && (
                <button 
                  onClick={() => setReviewsPage(p => p + 1)}
                  className="w-full mt-6 bg-surface-container hover:bg-surface-container-high font-bold px-4 py-2 rounded-xl transition-colors text-primary border border-transparent hover:border-primary/20"
                >
                  Load More Reviews
                </button>
              )}
            </section>
          </div>

          {/* RIGHT COLUMN (Availability) */}
          <aside id="booking-card" className="w-full lg:w-96 shrink-0 lg:sticky top-20 flex flex-col gap-6">
            <div className="bg-surface-container-lowest rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-outline-variant/15 p-6 md:p-8">
              <h2 className="text-2xl font-extrabold text-on-surface mb-6">Availability</h2>

              {/* Month Nav */}
              <div className="flex justify-between items-center mb-4">
                <button 
                  onClick={() => setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1, 1))}
                  className="p-1 rounded-full hover:bg-surface-container transition-colors text-on-surface-variant"
                >
                  <span className="material-symbols-outlined">chevron_left</span>
                </button>
                <div className="font-extrabold text-on-surface uppercase tracking-wider text-sm">
                  {currentMonth.toLocaleString('default', { month: 'long', year: 'numeric' })}
                </div>
                <button 
                  onClick={() => setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1, 1))}
                  className="p-1 rounded-full hover:bg-surface-container transition-colors text-on-surface-variant"
                >
                  <span className="material-symbols-outlined">chevron_right</span>
                </button>
              </div>

              {/* Calendar Grid */}
              <div className="grid grid-cols-7 gap-1 text-center mb-6">
                {['S', 'M', 'T', 'W', 'T', 'F', 'S'].map((day, i) => (
                  <div key={i} className="text-[10px] font-bold text-on-surface-variant uppercase mb-2">{day}</div>
                ))}
                
                {calendarDays.map((dayObj, i) => {
                  if (!dayObj) return <div key={i} className="p-2"></div>;
                  
                  const isSelected = selectedDate?.toDateString() === dayObj.date.toDateString();
                  
                  let btnClass = "w-9 h-9 mx-auto flex items-center justify-center rounded-full text-sm font-bold transition-all ";
                  
                  if (dayObj.isPast || (!dayObj.hasAvailable && !dayObj.allBooked)) {
                    btnClass += "opacity-30 cursor-not-allowed text-on-surface-variant";
                  } else if (dayObj.allBooked) {
                    btnClass += "opacity-40 cursor-not-allowed bg-error/10 text-error line-through";
                  } else if (isSelected) {
                    btnClass += "bg-primary text-white shadow-md scale-110";
                  } else if (dayObj.hasAvailable) {
                    btnClass += "bg-primary/10 text-primary hover:bg-primary/20 cursor-pointer";
                  }

                  if (dayObj.isToday && !isSelected) {
                    btnClass += " ring-2 ring-primary ring-offset-1";
                  }

                  return (
                    <button
                      key={i}
                      disabled={dayObj.isPast || !dayObj.hasAvailable}
                      onClick={() => { setSelectedDate(dayObj.date); setSelectedSlot(null); }}
                      className={btnClass}
                      title={dayObj.hasAvailable ? `${dayObj.totalSlots} slots available` : 'No slots'}
                    >
                      {dayObj.dayNumber}
                    </button>
                  );
                })}
              </div>

              {/* Time Slots */}
              {selectedDate && (
                <div className="mb-8 p-4 bg-surface-container-low/50 rounded-xl border border-outline-variant/10">
                  <div className="flex justify-between items-center mb-3">
                    <h3 className="text-xs font-extrabold text-on-surface-variant uppercase tracking-widest">Available Times</h3>
                    <span className="text-[10px] font-bold text-primary bg-primary/10 px-2 py-0.5 rounded-full">{selectedDate.toLocaleDateString('default', { month: 'short', day: 'numeric' })}</span>
                  </div>
                  
                  {availableTimes.length > 0 ? (
                    <div className="grid grid-cols-2 gap-2 max-h-48 overflow-y-auto scrollbar-hide pr-1">
                      {availableTimes.map((slot: any) => {
                        const isSelected = selectedSlot?.id === slot.id;
                        const timeString = new Date(slot.startTime).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });
                        return (
                          <button
                            key={slot.id}
                            onClick={() => setSelectedSlot(slot)}
                            className={`py-2 px-3 rounded-lg text-xs font-extrabold transition-all border ${
                              isSelected 
                                ? 'bg-primary text-white border-primary shadow-sm' 
                                : 'bg-surface-container hover:bg-surface-container-high border-transparent text-on-surface'
                            }`}
                          >
                            {timeString}
                          </button>
                        );
                      })}
                    </div>
                  ) : (
                    <p className="text-sm font-medium text-on-surface-variant text-center py-4">No times available for this date.</p>
                  )}
                </div>
              )}

              {/* Price & CTA */}
              <div className="border-t border-outline-variant/10 pt-6 flex flex-col items-center">
                <div className="text-center mb-4">
                  <p className="text-[10px] font-extrabold text-on-surface-variant uppercase tracking-widest leading-none mb-1">Session Fee (60 min)</p>
                  <div className="flex items-center justify-center gap-2">
                    <p className="text-4xl font-black text-on-surface">₹{mentor.hourlyRate}</p>
                    {mentor.availableSlots?.some((s:any) => !s.isBooked) && <span className="bg-emerald-500/10 text-emerald-600 font-bold text-[10px] px-2 py-0.5 rounded animate-pulse">AVAILABLE</span>}
                  </div>
                </div>

                <button 
                  onClick={handleBook}
                  disabled={!selectedSlot} 
                  className="w-full h-12 gradient-btn text-white font-extrabold rounded-xl shadow-md hover:shadow-lg disabled:opacity-50 disabled:scale-100 active:scale-[0.98] transition-all duration-300 disabled:cursor-not-allowed"
                >
                  {selectedSlot ? `Book for ${new Date(selectedSlot.startTime).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })}` : 'Select a Time Slot'}
                </button>
              </div>
            </div>
          </aside>

        </div>
      </main>
    </div>
  );
};

export default MentorProfilePage;
