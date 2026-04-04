import PageLayout from '../../components/layout/PageLayout';

const PlatformFinancesPage = () => {
  return (
    <PageLayout>
      <div className="space-y-6">
        <div className="bg-white border border-gray-200 rounded-xl p-6">
          <h1 className="text-2xl font-bold text-gray-900">Platform Finances</h1>
          <p className="text-gray-600 mt-2">Review revenue trends, payouts, and operational finance metrics.</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="bg-white border border-gray-200 rounded-xl p-5">
            <p className="text-sm text-gray-500">Gross Revenue</p>
            <p className="text-3xl font-bold text-gray-900 mt-1">$0.00</p>
          </div>
          <div className="bg-white border border-gray-200 rounded-xl p-5">
            <p className="text-sm text-gray-500">Mentor Payouts</p>
            <p className="text-3xl font-bold text-gray-900 mt-1">$0.00</p>
          </div>
          <div className="bg-white border border-gray-200 rounded-xl p-5">
            <p className="text-sm text-gray-500">Net Platform</p>
            <p className="text-3xl font-bold text-gray-900 mt-1">$0.00</p>
          </div>
        </div>
      </div>
    </PageLayout>
  );
};

export default PlatformFinancesPage;
