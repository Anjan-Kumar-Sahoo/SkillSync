import { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import api from '../../services/axios';
import PageLayout from '../../components/layout/PageLayout';

const DiscoverMentorsPage = () => {
  const navigate = useNavigate();
  
  const [draftFilters, setDraftFilters] = useState({ skill: '', rating: '', priceRange: '' });

  const [page, setPage] = useState(0);
  
  const [mentorsList, setMentorsList] = useState<any[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [isLast, setIsLast] = useState(true);

  // Fetch Skills for dropdown
  const { data: skillsData } = useQuery({
    queryKey: ['skills'],
    queryFn: async () => {
      try {
        const res = await api.get('/api/skills');
        return res.data;
      } catch {
        return [];
      }
    }
  });

  const skills = Array.isArray(skillsData) ? skillsData : [];

  // Fetch Mentors
  const { data, isLoading, refetch } = useQuery({
    queryKey: ['mentors', page],
    queryFn: async () => {
      const res = await api.get(`/api/mentors/search?page=${page}&size=9`);
      return res.data;
    }
  });

  useEffect(() => {
    if (data) {
      if (page === 0) {
        setMentorsList(data.content || []);
      } else {
        setMentorsList(prev => {
          const newItems = (data.content || []).filter((item: any) => !prev.some(p => p.id === item.id));
          return [...prev, ...newItems];
        });
      }
      setTotalElements(data.totalElements || 0);
      setIsLast(data.last ?? true);
    }
  }, [data, page]);

  const applyFilters = () => {


    setPage(0);
    setTimeout(() => refetch(), 0);
  };

  const clearFilters = () => {
    setDraftFilters({ skill: '', rating: '', priceRange: '' });

    setPage(0);
  };

  const getInitials = (first?: string, last?: string) => {
    return `${first?.[0] || ''}${last?.[0] || ''}`.toUpperCase() || 'M';
  };

  const getAvatarColor = (name?: string) => {
    const colors = ['from-blue-500 to-indigo-500', 'from-emerald-400 to-teal-500', 'from-violet-500 to-purple-500', 'from-amber-400 to-orange-500', 'from-rose-400 to-red-500'];
    const idx = name ? name.charCodeAt(0) % colors.length : 0;
    return colors[idx];
  };

  return (
    <PageLayout>
      {/* Header */}
      <div className="mb-2">
        <h1 className="text-4xl font-extrabold text-on-surface tracking-tight mb-2">Discover Mentors</h1>
        <p className="text-on-surface-variant text-lg">Learn from industry experts and accelerate your career path.</p>
        <p className="text-sm font-bold text-primary mt-4 bg-primary/10 inline-block px-3 py-1 rounded-full">
          {totalElements} experts available
        </p>
      </div>

      {/* Filter Bar */}
      <div className="bg-surface-container-lowest p-4 rounded-xl shadow-sm border border-outline-variant/15 flex flex-col md:flex-row gap-4 items-end mb-6">
        <div className="flex-1 w-full">
          <label className="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest block mb-1">Filter by Skills</label>
          <select 
            value={draftFilters.skill} 
            onChange={(e) => setDraftFilters(prev => ({ ...prev, skill: e.target.value }))}
            className="w-full h-10 bg-surface-container px-3 rounded-lg text-sm font-semibold outline-none focus:ring-1 focus:ring-primary border border-transparent"
          >
            <option value="">All Skills</option>
            {skills.map((s: any) => (
              <option key={typeof s === 'string' ? s : s.name} value={typeof s === 'string' ? s : s.name}>
                {typeof s === 'string' ? s : s.name}
              </option>
            ))}
          </select>
        </div>
        
        <div className="flex-1 w-full">
          <label className="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest block mb-1">Rating</label>
          <select 
            value={draftFilters.rating} 
            onChange={(e) => setDraftFilters(prev => ({ ...prev, rating: e.target.value }))}
            className="w-full h-10 bg-surface-container px-3 rounded-lg text-sm font-semibold outline-none focus:ring-1 focus:ring-primary border border-transparent"
          >
            <option value="">Any Rating</option>
            <option value="4">4+ Stars</option>
            <option value="4.5">4.5+ Stars</option>
            <option value="5">5 Stars Only</option>
          </select>
        </div>

        <div className="flex-1 w-full">
          <label className="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest block mb-1">Price Range</label>
          <select 
            value={draftFilters.priceRange} 
            onChange={(e) => setDraftFilters(prev => ({ ...prev, priceRange: e.target.value }))}
            className="w-full h-10 bg-surface-container px-3 rounded-lg text-sm font-semibold outline-none focus:ring-1 focus:ring-primary border border-transparent"
          >
            <option value="">Any Price</option>
            <option value="under50">Under $50</option>
            <option value="50to100">$50–$100</option>
            <option value="over100">$100+</option>
          </select>
        </div>

        <button 
          onClick={applyFilters}
          className="w-full md:w-auto px-6 h-10 gradient-btn text-white font-bold rounded-lg shadow-sm hover:shadow-md transition-all active:scale-95 shrink-0"
        >
          Apply
        </button>
      </div>

      {/* Grid */}
      {isLoading && page === 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {Array(6).fill(0).map((_, i) => (
            <div key={i} className="h-80 bg-surface-container rounded-xl animate-pulse"></div>
          ))}
        </div>
      ) : mentorsList.length === 0 ? (
        <div className="bg-surface-container-lowest rounded-xl p-12 flex flex-col items-center justify-center text-center shadow-sm border border-outline-variant/15">
          <span className="material-symbols-outlined text-6xl text-outline-variant mb-4">person_search</span>
          <h3 className="text-xl font-bold text-on-surface mb-2">No mentors found matching your criteria</h3>
          <p className="text-sm text-on-surface-variant mb-6">Try adjusting your filters to find the perfect expert.</p>
          <button onClick={clearFilters} className="bg-surface-container-high hover:bg-outline-variant/30 text-on-surface font-bold px-6 py-2 rounded-xl transition-colors">
            Clear Filters
          </button>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {mentorsList.map((mentor) => (
              <div key={mentor.id} className="bg-surface-container-lowest rounded-xl overflow-hidden shadow-sm border border-outline-variant/10 hover:-translate-y-1 hover:shadow-xl hover:border-primary/20 transition-all duration-300 group flex flex-col">
                <div className="h-48 bg-gradient-to-br from-surface-container to-surface-container-high relative flex items-center justify-center p-4 overflow-hidden">
                  <div className="absolute inset-0 bg-primary/5 group-hover:bg-primary/10 transition-colors"></div>
                  <div className={`w-20 h-20 rounded-full bg-gradient-to-tr ${getAvatarColor(mentor.firstName)} text-white flex items-center justify-center text-2xl font-black shadow-lg z-10 ring-4 ring-white`}>
                    {getInitials(mentor.firstName, mentor.lastName)}
                  </div>
                  <div className="absolute top-3 right-3 bg-white/90 backdrop-blur-sm shadow-sm rounded-lg px-2 py-1 flex items-center gap-1 z-10">
                    <span className="text-amber-500 text-sm">★</span>
                    <span className="font-bold text-xs text-on-surface">{mentor.rating?.toFixed(1) || 'NEW'}</span>
                  </div>
                </div>
                
                <div className="p-5 flex flex-col flex-1">
                  <h3 className="text-xl font-extrabold text-on-surface leading-tight mb-1 group-hover:text-primary transition-colors">
                    {mentor.firstName} {mentor.lastName}
                  </h3>
                  <p className="text-sm font-medium text-on-surface-variant truncate mb-4" title={mentor.headline}>
                    {mentor.headline || 'Industry Expert'}
                  </p>
                  
                  <div className="flex flex-wrap gap-1.5 mb-6 mt-auto">
                    {(mentor.skills || []).slice(0, 3).map((skill: string, i: number) => (
                      <span key={i} className="bg-surface-container-low text-on-surface-variant text-[10px] font-bold px-2 py-1 rounded-md uppercase tracking-wider border border-outline-variant/10">
                        {skill}
                      </span>
                    ))}
                    {(mentor.skills?.length > 3) && (
                      <span className="bg-surface-container-low text-on-surface-variant text-[10px] font-bold px-2 py-1 rounded-md border border-outline-variant/10">
                        +{mentor.skills.length - 3}
                      </span>
                    )}
                  </div>

                  <div className="flex justify-between items-end mb-4 border-t border-outline-variant/10 pt-4">
                    <span className="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest flex flex-col">
                      Price
                      <span className="text-lg font-black text-primary lowercase tracking-normal -mt-0.5">
                        ${mentor.hourlyRate}<span className="text-xs text-on-surface-variant font-semibold">/hr</span>
                      </span>
                    </span>
                  </div>

                  <button 
                    onClick={() => navigate(`/mentors/${mentor.id}`)}
                    className="w-full flex items-center justify-center gap-2 h-10 gradient-btn text-white text-sm font-bold rounded-lg shadow-sm group-hover:shadow-md transition-all active:scale-95"
                  >
                    Book Session <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
                  </button>
                </div>
              </div>
            ))}
          </div>

          {!isLast && (
            <div className="mt-8 flex justify-center">
              <button 
                onClick={() => setPage(p => p + 1)} 
                disabled={isLoading}
                className="flex items-center gap-2 bg-white hover:bg-surface-container-low text-primary font-bold px-6 py-2.5 rounded-full shadow-sm border border-outline-variant/20 hover:border-primary/30 transition-all disabled:opacity-50"
              >
                {isLoading ? 'Loading...' : 'See More Mentors'} 
                <span className="material-symbols-outlined text-[20px]">{isLoading ? 'hourglass_empty' : 'expand_more'}</span>
              </button>
            </div>
          )}
        </>
      )}
    </PageLayout>
  );
};

export default DiscoverMentorsPage;
