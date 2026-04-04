import PageLayout from '../../components/layout/PageLayout';

const LearningPathPage = () => {
  return (
    <PageLayout>
      <div className="space-y-6">
        <div className="bg-white border border-gray-200 rounded-xl p-6">
          <h1 className="text-2xl font-bold text-gray-900">Learning Path</h1>
          <p className="text-gray-600 mt-2">Track your progress and plan upcoming learning milestones.</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="bg-white border border-gray-200 rounded-xl p-5">
            <p className="text-sm text-gray-500">Completed Goals</p>
            <p className="text-3xl font-bold text-emerald-600 mt-1">0</p>
          </div>
          <div className="bg-white border border-gray-200 rounded-xl p-5">
            <p className="text-sm text-gray-500">Active Goals</p>
            <p className="text-3xl font-bold text-blue-600 mt-1">0</p>
          </div>
          <div className="bg-white border border-gray-200 rounded-xl p-5">
            <p className="text-sm text-gray-500">Upcoming Goals</p>
            <p className="text-3xl font-bold text-amber-600 mt-1">0</p>
          </div>
        </div>

        <div className="bg-white border border-dashed border-gray-300 rounded-xl p-8 text-center text-gray-600">
          Your personalized learning roadmap will appear here as you book sessions and set goals.
        </div>
      </div>
    </PageLayout>
  );
};

export default LearningPathPage;
