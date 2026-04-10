import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../services/axios';
import PageLayout from '../../components/layout/PageLayout';
import { useToast } from '../../components/ui/Toast';
import { useActionConfirm } from '../../components/ui/ActionConfirm';

interface Skill {
  id: number;
  name: string;
  category: string;
}

const PAGE_SIZE = 16;

const AdminSkillsPage = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const { requestConfirmation } = useActionConfirm();
  const [page, setPage] = useState(0);
  const [newSkillName, setNewSkillName] = useState('');
  const [newSkillCategory, setNewSkillCategory] = useState('');

  const { data: skillsData, isLoading } = useQuery<{ content: Skill[]; totalElements: number; totalPages: number; number: number }>({
    queryKey: ['skills', page],
    queryFn: async () => {
      const params = new URLSearchParams();
      params.append('page', String(page));
      params.append('size', String(PAGE_SIZE));
      params.append('sort', 'id,asc');

      const res = await api.get(`/api/skills?${params.toString()}`);
      return res.data;
    },
  });

  const addSkillMutation = useMutation({
    mutationFn: async (newSkill: { name: string; category: string }) => {
      return api.post('/api/skills', newSkill);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['skills'] });
      showToast({ message: 'Skill added successfully', type: 'success' });
      setNewSkillName('');
      setNewSkillCategory('');
    },
    onError: () => {
      showToast({ message: 'Failed to add skill', type: 'error' });
    },
  });

  const deleteSkillMutation = useMutation({
    mutationFn: async (id: number) => {
      return api.delete(`/api/skills/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['skills'] });
      showToast({ message: 'Skill deleted successfully', type: 'success' });
    },
    onError: () => {
      showToast({ message: 'Failed to delete skill', type: 'error' });
    },
  });

  const handleAddSkill = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newSkillName.trim() || !newSkillCategory.trim()) return;
    addSkillMutation.mutate({ name: newSkillName, category: newSkillCategory });
  };

  const handleDeleteSkill = async (skillId: number, skillName: string) => {
    const confirmed = await requestConfirmation({
      title: 'Delete Skill?',
      message: `Are you sure you want to delete skill "${skillName}"? This will hide it from active mentor selection.`,
      confirmLabel: 'Yes, delete skill',
    });

    if (!confirmed) {
      return;
    }

    deleteSkillMutation.mutate(skillId);
  };

  const skills = skillsData?.content || [];
  const totalElements = Number(skillsData?.totalElements || skills.length || 0);
  const totalPages = Math.max(1, Number(skillsData?.totalPages || 1));
  const currentPage = Number(skillsData?.number ?? page);

  if (isLoading) {
    return (
      <PageLayout>
        <div className="flex justify-center items-center h-64">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary"></div>
        </div>
      </PageLayout>
    );
  }

  return (
    <PageLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold text-on-surface">Manage Skills</h1>
          <p className="text-on-surface-variant flex items-center gap-2 mt-2">
            Add platform skills for mentors to select.
          </p>
        </div>

        <div className="bg-surface-container-lowest p-6 rounded-2xl shadow-sm border border-outline-variant/10">
          <h2 className="text-lg font-bold mb-4">Add New Skill</h2>
          <form onSubmit={handleAddSkill} className="flex flex-col md:flex-row gap-4">
            <input
              type="text"
              placeholder="Skill Name (e.g. ReactJS)"
              value={newSkillName}
              onChange={(e) => setNewSkillName(e.target.value)}
              className="flex-1 px-4 py-3 bg-surface-variant/20 border border-outline-variant/30 rounded-xl focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all"
            />
            <input
              type="text"
              placeholder="Category (e.g. Frontend)"
              value={newSkillCategory}
              onChange={(e) => setNewSkillCategory(e.target.value)}
              className="flex-1 px-4 py-3 bg-surface-variant/20 border border-outline-variant/30 rounded-xl focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all"
            />
            <button
              type="submit"
              disabled={addSkillMutation.isPending || !newSkillName || !newSkillCategory}
              className="px-6 py-3 bg-primary text-on-primary rounded-xl font-bold hover:bg-primary-dark transition-colors disabled:opacity-50"
            >
              Add Skill
            </button>
          </form>
        </div>

        <div className="bg-surface-container-lowest p-6 rounded-2xl shadow-sm border border-outline-variant/10">
          <h2 className="text-lg font-bold mb-4">Existing Skills ({totalElements})</h2>
          {skills.length > 0 ? (
            <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 gap-4">
              {skills.map((skill) => {
                const isDeleting =
                  deleteSkillMutation.isPending && deleteSkillMutation.variables === skill.id;

                return (
                  <div
                    key={skill.id}
                    className="p-4 border border-outline-variant/20 rounded-xl bg-surface-variant/10 flex flex-col gap-3"
                  >
                    <div>
                      <p className="font-bold text-on-surface">{skill.name}</p>
                      <p className="text-sm text-on-surface-variant mt-1">{skill.category}</p>
                    </div>

                    <button
                      type="button"
                      onClick={() => void handleDeleteSkill(skill.id, skill.name)}
                      disabled={isDeleting}
                      className="inline-flex items-center justify-center gap-2 px-3 py-2 rounded-lg text-sm font-semibold text-error bg-error/10 hover:bg-error/20 transition-colors disabled:opacity-50"
                    >
                      <span className="material-symbols-outlined text-[18px]">delete</span>
                      {isDeleting ? 'Deleting...' : 'Delete'}
                    </button>
                  </div>
                );
              })}
            </div>
          ) : (
            <p className="text-on-surface-variant">No skills found.</p>
          )}

          {skills.length > 0 && totalPages > 1 && (
            <div className="mt-5 pt-4 border-t border-outline-variant/10 flex items-center justify-between gap-3">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={currentPage <= 0}
                className="px-3 py-1.5 rounded-md text-sm font-bold bg-surface-container hover:bg-surface-container-high text-on-surface disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Previous
              </button>
              <p className="text-xs font-semibold text-on-surface-variant">
                Page {currentPage + 1} of {totalPages}
              </p>
              <button
                onClick={() => setPage((p) => p + 1)}
                disabled={currentPage >= totalPages - 1}
                className="px-3 py-1.5 rounded-md text-sm font-bold bg-surface-container hover:bg-surface-container-high text-on-surface disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Next
              </button>
            </div>
          )}
        </div>
      </div>
    </PageLayout>
  );
};

export default AdminSkillsPage;
