import { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import api from '../../services/axios';
import PageLayout from '../../components/layout/PageLayout';

type MentorSkill =
  | string
  | {
      id?: number;
      skillId?: number;
      name?: string;
    };

type MentorCard = {
  id: number;
  firstName?: string;
  lastName?: string;
  headline?: string;
  avgRating?: number;
  rating?: number;
  totalSessions?: number;
  skills?: MentorSkill[];
  hourlyRate?: number;
};

type MentorSearchResponse = {
  content?: MentorCard[];
  totalElements?: number;
  last?: boolean;
};

type SkillOption = string | { id?: number; name?: string };

const hasSameMentorIds = (previous: MentorCard[], next: MentorCard[]): boolean => {
  if (previous.length !== next.length) {
    return false;
  }

  return previous.every((mentor, index) => mentor?.id === next[index]?.id);
};

const DiscoverMentorsPage = () => {
  const navigate = useNavigate();
  
  const [draftFilters, setDraftFilters] = useState({ skill: '', rating: '', priceRange: '' });
  const [appliedFilters, setAppliedFilters] = useState({ skill: '', rating: '', priceRange: '' });

  const [page, setPage] = useState(0);
  
  const [mentorsList, setMentorsList] = useState<MentorCard[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [isLast, setIsLast] = useState(true);

  // Fetch Skills for dropdown
  const { data: skillsData } = useQuery({
    queryKey: ['skills', 'catalog'],
    queryFn: async (): Promise<SkillOption[]> => {
      try {
        const size = 200;
        const pagesToFetch = 10;
        const collected: SkillOption[] = [];

        for (let page = 0; page < pagesToFetch; page += 1) {
          const res = await api.get(`/api/skills?page=${page}&size=${size}`, { _skipErrorRedirect: true } as any);
          const content = Array.isArray(res.data?.content) ? (res.data.content as SkillOption[]) : [];
          if (content.length === 0) break;
          collected.push(...content);
          if (res.data?.last !== false) break;
        }

        const uniqueById = new Map<string, SkillOption>(
          collected.map((skill) => {
            if (typeof skill === 'string') {
              return [`skill:${skill}`, skill] as const;
            }

            return [`skill:${skill.id ?? skill.name ?? 'unknown'}`, skill] as const;
          }),
        );
        return Array.from(uniqueById.values());
      } catch {
        return [];
      }
    }
  });

  const skills = Array.isArray(skillsData) ? skillsData : [];

  // Fetch Mentors
  const { data, isLoading } = useQuery<MentorSearchResponse>({
    queryKey: ['mentors', 'search', page, appliedFilters.skill, appliedFilters.rating, appliedFilters.priceRange],
    queryFn: async () => {
      const params = new URLSearchParams();
      params.set('page', String(page));
      params.set('size', '9');

      if (appliedFilters.skill) {
        params.set('skill', appliedFilters.skill);
      }

      if (appliedFilters.rating) {
        params.set('rating', appliedFilters.rating);
      }

      if (appliedFilters.priceRange === 'under50') {
        params.set('maxPrice', '50');
      } else if (appliedFilters.priceRange === '50to100') {
        params.set('minPrice', '50');
        params.set('maxPrice', '100');
      } else if (appliedFilters.priceRange === 'over100') {
        params.set('minPrice', '100');
      }

      const res = await api.get(`/api/mentors/search?${params.toString()}`);
      return res.data as MentorSearchResponse;
    }
  });

  useEffect(() => {
    if (data) {
      const incomingMentors = Array.isArray(data.content) ? data.content : [];

      if (page === 0) {
        setMentorsList((prev) => (hasSameMentorIds(prev, incomingMentors) ? prev : incomingMentors));
      } else {
        setMentorsList((prev) => {
          const newItems = incomingMentors.filter((item) => !prev.some((current) => current.id === item.id));
          return newItems.length === 0 ? prev : [...prev, ...newItems];
        });
      }

      const nextTotalElements = Number.isFinite(Number(data.totalElements)) ? Number(data.totalElements) : 0;
      setTotalElements((prev) => (prev === nextTotalElements ? prev : nextTotalElements));

      const nextIsLast = data.last ?? true;
      setIsLast((prev) => (prev === nextIsLast ? prev : nextIsLast));
    }
  }, [data, page]);

  const applyFilters = () => {
    setPage(0);
    setMentorsList([]);
    setAppliedFilters({ ...draftFilters });
  };

  const clearFilters = () => {
    const reset = { skill: '', rating: '', priceRange: '' };
    setDraftFilters(reset);
    setAppliedFilters(reset);
    setPage(0);
    setMentorsList([]);
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
      <div className="mb-6 fade-slide-entry">
        <h1 className="text-4xl font-black text-on-surface tracking-tight mb-2">Discover Mentors</h1>
        <p className="text-on-surface-variant text-lg font-medium">Learn from industry experts and accelerate your career path.</p>
        <p className="text-xs font-black text-cyan-600 dark:text-cyan-400 mt-4 bg-cyan-500/10 border border-cyan-500/20 inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full uppercase tracking-wider">
          <span className="w-2 h-2 rounded-full bg-cyan-500 animate-pulse-slow"></span>
          {totalElements} experts available
        </p>
      </div>

      {/* Filter Bar */}
      <div className="glass-panel-lowest p-5 rounded-2xl shadow-sm flex flex-col md:flex-row gap-4 items-end mb-8 hover-glow-cyan transition-all duration-300">
        <div className="flex-1 w-full">
          <label className="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest block mb-1">Filter by Skills</label>
          <select 
            value={draftFilters.skill} 
            onChange={(e) => setDraftFilters(prev => ({ ...prev, skill: e.target.value }))}
            className="w-full h-11 bg-surface-container px-3.5 rounded-xl text-sm font-bold text-on-surface outline-none focus:ring-2 focus:ring-cyan-500/40 border border-outline-variant/20 hover:border-outline-variant/50 transition-colors"
          >
            <option value="">All Skills</option>
            {skills.map((s) => (
              <option key={typeof s === 'string' ? s : (s.id ?? s.name)} value={typeof s === 'string' ? s : s.name}>
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
            className="w-full h-11 bg-surface-container px-3.5 rounded-xl text-sm font-bold text-on-surface outline-none focus:ring-2 focus:ring-cyan-500/40 border border-outline-variant/20 hover:border-outline-variant/50 transition-colors"
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
            className="w-full h-11 bg-surface-container px-3.5 rounded-xl text-sm font-bold text-on-surface outline-none focus:ring-2 focus:ring-cyan-500/40 border border-outline-variant/20 hover:border-outline-variant/50 transition-colors"
          >
            <option value="">Any Price</option>
            <option value="under50">Under ₹50</option>
            <option value="50to100">₹50-₹100</option>
            <option value="over100">₹100+</option>
          </select>
        </div>

        <button 
          onClick={applyFilters}
          className="w-full md:w-auto px-8 h-11 gradient-btn text-white text-sm font-bold rounded-xl shadow-md hover:shadow-lg hover:scale-[1.02] active:scale-[0.98] transition-all duration-300 shrink-0"
        >
          Apply Filters
        </button>
      </div>

      {/* Grid */}
      {isLoading && page === 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {Array(6).fill(0).map((_, i) => (
            <div key={i} className="h-80 bg-surface-container rounded-2xl animate-pulse"></div>
          ))}
        </div>
      ) : mentorsList.length === 0 ? (
        <div className="glass-panel-lowest rounded-2xl p-12 flex flex-col items-center justify-center text-center shadow-sm border border-outline-variant/15 fade-slide-entry">
          <span className="material-symbols-outlined text-6xl text-outline-variant mb-4 pulse-glow-badge">person_search</span>
          <h3 className="text-xl font-bold text-on-surface mb-2">No mentors found matching your criteria</h3>
          <p className="text-sm text-on-surface-variant mb-6 font-medium">Try adjusting your filters to find the perfect expert.</p>
          <button onClick={clearFilters} className="bg-surface-container-high hover:bg-outline-variant/30 text-on-surface font-bold px-6 py-2.5 rounded-xl transition-colors">
            Clear Filters
          </button>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {mentorsList.map((mentor) => {
              const avgRating = Number(mentor.avgRating ?? mentor.rating ?? 0);
              const sessionsHeld = Number(mentor.totalSessions ?? 0);
              const isNewMentor = sessionsHeld === 0;

              return (
                <div key={mentor.id} className="glass-panel-lowest rounded-2xl overflow-hidden shadow-sm hover:shadow-xl transition-all duration-300 group flex flex-col fade-slide-entry hover-glow-cyan">
                  <div className="h-44 bg-gradient-to-br from-surface-container-low to-surface-container relative flex items-center justify-center p-4 overflow-hidden">
                    <div className="absolute inset-0 bg-cyan-500/5 group-hover:bg-cyan-500/10 transition-all duration-350"></div>
                    <div className={`w-20 h-20 rounded-full bg-gradient-to-tr ${getAvatarColor(mentor.firstName)} text-white flex items-center justify-center text-2xl font-black shadow-lg z-10 ring-4 ring-white/25 dark:ring-surface-container-lowest/30 group-hover:scale-105 transition-transform duration-300`}>
                      {getInitials(mentor.firstName, mentor.lastName)}
                    </div>
                  </div>
                  
                  <div className="p-5 flex flex-col flex-1">
                    <div className="flex items-start justify-between gap-3 mb-4">
                      <div className="min-w-0">
                        <h3 className="text-lg font-extrabold text-on-surface leading-tight mb-1 group-hover:text-cyan-600 dark:group-hover:text-cyan-400 transition-colors">
                          {mentor.firstName} {mentor.lastName}
                        </h3>
                        <p className="text-xs font-semibold text-on-surface-variant truncate" title={mentor.headline}>
                          {mentor.headline || 'Industry Expert'}
                        </p>
                      </div>

                      <div className="text-right shrink-0">
                        <span className="inline-flex items-center gap-1 bg-cyan-500/10 border border-cyan-500/20 px-2 py-0.5 rounded text-xs font-black text-cyan-600 dark:text-cyan-400">
                          ★ {isNewMentor ? 'NEW' : avgRating.toFixed(1)}
                        </span>
                        <p className="text-[10px] font-bold text-on-surface-variant mt-1.5 uppercase tracking-wide">
                          {sessionsHeld} session{sessionsHeld === 1 ? '' : 's'}
                        </p>
                      </div>
                    </div>
                  
                    <div className="flex flex-wrap gap-1.5 mb-6 mt-auto">
                      {(mentor.skills || []).slice(0, 3).map((skill, i: number) => (
                        <span key={i} className="bg-surface-container text-on-surface-variant text-[10px] font-bold px-2.5 py-1 rounded-md border border-outline-variant/10 uppercase tracking-wider">
                          {typeof skill === 'string' ? skill : (skill.name || `Skill #${skill.skillId}`)}
                        </span>
                      ))}
                      {((mentor.skills?.length ?? 0) > 3) && (
                        <span className="bg-surface-container text-on-surface-variant text-[10px] font-bold px-2.5 py-1 rounded-md border border-outline-variant/10">
                          +{(mentor.skills?.length ?? 0) - 3}
                        </span>
                      )}
                    </div>

                    <div className="flex justify-between items-end mb-4 border-t border-outline-variant/10 pt-4">
                      <span className="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest flex flex-col">
                        Hourly Rate
                        <span className="text-xl font-black text-gradient lowercase tracking-normal -mt-0.5">
                          ₹{mentor.hourlyRate}<span className="text-xs text-on-surface-variant font-semibold">/hr</span>
                        </span>
                      </span>
                    </div>

                    <button 
                      onClick={() => navigate(`/mentors/${mentor.id}`)}
                      className="w-full flex items-center justify-center gap-2 h-11 gradient-btn text-white text-sm font-bold rounded-xl shadow-md group-hover:shadow-lg transition-all duration-300 active:scale-95"
                    >
                      Book Session <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
                    </button>
                  </div>
                </div>
              );
            })}
          </div>

          {!isLast && (
            <div className="mt-8 flex justify-center">
              <button 
                onClick={() => setPage(p => p + 1)} 
                disabled={isLoading}
                className="flex items-center gap-2 bg-white dark:bg-surface-container-lowest hover:bg-surface-container-low text-primary font-bold px-6 py-2.5 rounded-full shadow-sm border border-outline-variant/20 hover:border-primary/30 transition-all disabled:opacity-50 hover:scale-105 active:scale-95"
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
