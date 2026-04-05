import PageLayout from '../../components/layout/PageLayout';

const EarningsPage = () => {
  return (
    <PageLayout>
      <div className="space-y-6">
        <div className="bg-white border border-gray-200 rounded-xl p-6">
          <h1 className="text-2xl font-bold text-gray-900">Earnings</h1>
          <p className="text-gray-600 mt-2">Payout tracking and settlement reports are not implemented yet.</p>
          <div className="mt-4 rounded-lg bg-amber-50 border border-amber-200 px-4 py-3">
            <p className="text-sm font-semibold text-amber-900">Feature Status: Coming Soon</p>
            <p className="text-sm text-amber-800 mt-1">This section is a placeholder until payout APIs are integrated.</p>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="bg-white border border-gray-200 rounded-xl p-5">
            <p className="text-sm text-gray-500">This Month</p>
            <p className="text-3xl font-bold text-emerald-600 mt-1">$0.00</p>
          </div>
          <div className="bg-white border border-gray-200 rounded-xl p-5">
            <p className="text-sm text-gray-500">Pending Payout</p>
            <p className="text-3xl font-bold text-amber-600 mt-1">$0.00</p>
          </div>
          <div className="bg-white border border-gray-200 rounded-xl p-5">
            <p className="text-sm text-gray-500">Lifetime</p>
            <p className="text-3xl font-bold text-blue-600 mt-1">$0.00</p>
          </div>
        </div>

        <div className="bg-white border border-dashed border-gray-300 rounded-xl p-8 text-center text-gray-600">
          Payout history and withdrawal actions will be enabled in a future release.
        </div>
      </div>
    </PageLayout>
  );
};

export default EarningsPage;
