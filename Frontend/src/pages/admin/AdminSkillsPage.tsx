import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../services/axios';
import PageLayout from '../../components/layout/PageLayout';
import { useToast } from '../../components/ui/Toast';

interface Skill {
  id: number;
  name: string;
  category: string;
}

const AdminSkillsPage = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const [newSkillName, setNewSkillName] = useState('');
  const [newSkillCategory, setNewSkillCategory] = useState('');

  const { data: skills, isLoading } = useQuery<Skill[]>({
    queryKey: ['skills'],
    queryFn: async () => {
      const res = await api.get('/api/skills?size=100');
      return res.data?.content || [];
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

  const handleAddSkill = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newSkillName.trim() || !newSkillCategory.trim()) return;
    addSkillMutation.mutate({ name: newSkillName, category: newSkillCategory });
  };

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
          <h2 className="text-lg font-bold mb-4">Existing Skills ({skills?.length || 0})</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 gap-4">
            {skills?.map((skill) => (
              <div key={skill.id} className="p-4 border border-outline-variant/20 rounded-xl bg-surface-variant/10">
                <p className="font-bold text-on-surface">{skill.name}</p>
                <p className="text-sm text-on-surface-variant mt-1">{skill.category}</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </PageLayout>
  );
};

export default AdminSkillsPage;
