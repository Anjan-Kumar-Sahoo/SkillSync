import PageLayout from '../../components/layout/PageLayout';

const resources = [
  { title: 'Interview Prep Checklist', category: 'Career', type: 'Guide' },
  { title: 'System Design Fundamentals', category: 'Engineering', type: 'Article' },
  { title: 'Communication for Mentorship', category: 'Soft Skills', type: 'Playbook' },
];

const ResourcesPage = () => {
  return (
    <PageLayout>
      <div className="space-y-6">
        <div className="bg-white border border-gray-200 rounded-xl p-6">
          <h1 className="text-2xl font-bold text-gray-900">Resources</h1>
          <p className="text-gray-600 mt-2">Curated materials to support your learning sessions.</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {resources.map((item) => (
            <div key={item.title} className="bg-white border border-gray-200 rounded-xl p-5">
              <p className="text-xs uppercase tracking-wide text-gray-500">{item.type}</p>
              <h3 className="font-bold text-gray-900 mt-1">{item.title}</h3>
              <p className="text-sm text-gray-600 mt-2">Category: {item.category}</p>
            </div>
          ))}
        </div>
      </div>
    </PageLayout>
  );
};

export default ResourcesPage;
