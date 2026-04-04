import PageLayout from '../../components/layout/PageLayout';

const HelpCenterPage = () => {
  return (
    <PageLayout>
      <div className="space-y-6">
        <div className="bg-white border border-gray-200 rounded-xl p-6">
          <h1 className="text-2xl font-bold text-gray-900">Help Center</h1>
          <p className="text-gray-600 mt-2">Get support for account, sessions, payments, and platform usage.</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="bg-white border border-gray-200 rounded-xl p-5">
            <h3 className="font-bold text-gray-900">Account Support</h3>
            <p className="text-sm text-gray-600 mt-2">Issues with login, profile updates, or security settings.</p>
          </div>
          <div className="bg-white border border-gray-200 rounded-xl p-5">
            <h3 className="font-bold text-gray-900">Session Support</h3>
            <p className="text-sm text-gray-600 mt-2">Booking, cancellations, and mentoring session concerns.</p>
          </div>
          <div className="bg-white border border-gray-200 rounded-xl p-5">
            <h3 className="font-bold text-gray-900">Billing & Payments</h3>
            <p className="text-sm text-gray-600 mt-2">Invoice, refunds, and transaction troubleshooting.</p>
          </div>
          <div className="bg-white border border-gray-200 rounded-xl p-5">
            <h3 className="font-bold text-gray-900">Report an Issue</h3>
            <p className="text-sm text-gray-600 mt-2">Raise a ticket by emailing support@skillsync.mraks.dev.</p>
          </div>
        </div>
      </div>
    </PageLayout>
  );
};

export default HelpCenterPage;
