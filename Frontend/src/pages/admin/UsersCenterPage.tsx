import PageLayout from '../../components/layout/PageLayout';

const UsersCenterPage = () => {
  return (
    <PageLayout>
      <div className="space-y-6">
        <div className="bg-white border border-gray-200 rounded-xl p-6">
          <h1 className="text-2xl font-bold text-gray-900">Users Center</h1>
          <p className="text-gray-600 mt-2">Manage learners, mentors, and account states.</p>
        </div>

        <div className="bg-white border border-gray-200 rounded-xl p-5">
          <p className="text-sm text-gray-500 mb-2">Quick Summary</p>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <p className="text-xs text-gray-500">Learners</p>
              <p className="text-xl font-bold text-gray-900">-</p>
            </div>
            <div>
              <p className="text-xs text-gray-500">Mentors</p>
              <p className="text-xl font-bold text-gray-900">-</p>
            </div>
            <div>
              <p className="text-xs text-gray-500">Admins</p>
              <p className="text-xl font-bold text-gray-900">-</p>
            </div>
          </div>
        </div>
      </div>
    </PageLayout>
  );
};

export default UsersCenterPage;
