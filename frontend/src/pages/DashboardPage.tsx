import { Link } from 'react-router-dom';
import { Edit3, KeyRound, Plus, Send } from 'lucide-react';
import { useQuery } from 'react-query';
import { api } from '../api/client';
import type { Campaign, DashboardStats, GmailSettings } from '../types';

export default function DashboardPage() {
  const stats = useQuery('stats', async () => (await api.get<DashboardStats>('/campaigns/dashboard')).data);
  const campaigns = useQuery('campaigns', async () => (await api.get<Campaign[]>('/campaigns')).data);
  const gmail = useQuery('gmail-settings', async () => (await api.get<GmailSettings>('/auth/gmail-settings')).data);

  return (
    <div className="space-y-6">
      {!gmail.data?.connected && (
        <section className="flex flex-wrap items-center justify-between gap-3 rounded border border-amber-200 bg-amber-50 p-4">
          <div className="flex items-start gap-3">
            <span className="mt-0.5 grid h-9 w-9 place-items-center rounded bg-amber-500 text-white">
              <KeyRound size={18} />
            </span>
            <div>
              <h2 className="font-semibold text-amber-950">Connect Gmail before sending</h2>
              <p className="text-sm text-amber-800">First-time users must save a Gmail App Password in Settings so campaigns can send from their Gmail account.</p>
            </div>
          </div>
          <Link to="/settings" className="focus-ring inline-flex h-10 items-center gap-2 rounded bg-amber-600 px-4 text-sm font-medium text-white hover:bg-amber-700">
            <KeyRound size={16} />
            Open Settings
          </Link>
        </section>
      )}

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-slate-950">Campaigns</h1>
          <p className="text-sm text-slate-500">Create, review, and send personalized bulk email campaigns.</p>
        </div>
        <Link to="/campaigns/new" className="focus-ring inline-flex h-10 items-center gap-2 rounded bg-emerald-600 px-4 text-sm font-medium text-white hover:bg-emerald-700">
          <Plus size={16} />
          New Campaign
        </Link>
      </div>

      <section className="grid gap-3 md:grid-cols-6">
        {[
          ['Campaigns', stats.data?.campaigns ?? 0],
          ['Recipients', stats.data?.recipients ?? 0],
          ['Sent', stats.data?.sent ?? 0],
          ['Failed', stats.data?.failed ?? 0],
          ['Pending', stats.data?.pending ?? 0],
          ['Success', `${(stats.data?.successRate ?? 0).toFixed(1)}%`]
        ].map(([label, value]) => (
          <div key={label} className="rounded border border-slate-200 bg-white p-4">
            <p className="text-xs font-medium uppercase text-slate-500">{label}</p>
            <p className="mt-2 text-2xl font-semibold text-slate-950">{value}</p>
          </div>
        ))}
      </section>

      <section className="overflow-hidden rounded border border-slate-200 bg-white">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase text-slate-500">
            <tr>
              <th className="px-4 py-3">Campaign</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Sent</th>
              <th className="px-4 py-3">Failed</th>
              <th className="px-4 py-3">Pending</th>
              <th className="px-4 py-3"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {campaigns.data?.map((campaign) => (
              <tr key={campaign.id}>
                <td className="px-4 py-3 font-medium text-slate-950">{campaign.campaignName}</td>
                <td className="px-4 py-3">{campaign.status}</td>
                <td className="px-4 py-3">{campaign.counts.SENT ?? 0}</td>
                <td className="px-4 py-3">{campaign.counts.FAILED ?? 0}</td>
                <td className="px-4 py-3">{campaign.counts.PENDING ?? 0}</td>
                <td className="px-4 py-3 text-right">
                  {campaign.status === 'DRAFT' ? (
                    <Link to={`/campaigns/${campaign.id}/edit`} className="inline-flex items-center gap-2 text-emerald-700 hover:text-emerald-900">
                      <Edit3 size={15} />
                      Continue Setup
                    </Link>
                  ) : (
                    <Link to={`/campaigns/${campaign.id}/review`} className="inline-flex items-center gap-2 text-emerald-700 hover:text-emerald-900">
                      <Send size={15} />
                      Review
                    </Link>
                  )}
                </td>
              </tr>
            ))}
            {!campaigns.data?.length && (
              <tr>
                <td colSpan={6} className="px-4 py-10 text-center text-slate-500">No campaigns yet.</td>
              </tr>
            )}
          </tbody>
        </table>
      </section>
    </div>
  );
}
