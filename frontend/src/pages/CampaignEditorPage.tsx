import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate, useParams } from 'react-router-dom';
import { Upload } from 'lucide-react';
import { useQuery } from 'react-query';
import { api } from '../api/client';
import type { Campaign, RecipientStatus } from '../types';
import BackButton from '../components/BackButton';

interface CampaignForm {
  campaignName: string;
  subjectTemplate: string;
  bodyTemplate: string;
}

export default function CampaignEditorPage() {
  const { id } = useParams();
  const campaignId = id ? Number(id) : null;
  const isEditing = Boolean(campaignId);
  const navigate = useNavigate();
  const [csv, setCsv] = useState<File | null>(null);
  const [attachments, setAttachments] = useState<FileList | null>(null);
  const [error, setError] = useState('');
  const [submitMode, setSubmitMode] = useState<'draft' | 'review'>('review');
  const campaign = useQuery(
    ['campaign', campaignId],
    async () => (await api.get<Campaign>(`/campaigns/${campaignId}`)).data,
    { enabled: isEditing }
  );
  const { register, handleSubmit, reset, formState } = useForm<CampaignForm>({
    defaultValues: {
      campaignName: '',
      subjectTemplate: '',
      bodyTemplate: ''
    }
  });

  useEffect(() => {
    if (campaign.data) {
      reset({
        campaignName: campaign.data.campaignName,
        subjectTemplate: campaign.data.subjectTemplate,
        bodyTemplate: campaign.data.bodyTemplate
      });
    }
  }, [campaign.data, reset]);

  async function submit(values: CampaignForm) {
    setError('');
    try {
      if (submitMode === 'review' && !csv && !hasRecipients(campaign.data)) {
        setError('Upload a CSV or Excel file before moving to review.');
        return;
      }

      const { data: savedCampaign } = isEditing
        ? await api.put<Campaign>(`/campaigns/${campaignId}`, values)
        : await api.post<Campaign>('/campaigns', values);

      if (csv) {
        const form = new FormData();
        form.append('file', csv);
        await api.post(`/campaigns/${savedCampaign.id}/upload`, form);
      }
      if (attachments?.length) {
        const form = new FormData();
        Array.from(attachments).forEach((file) => form.append('files', file));
        await api.post(`/campaigns/${savedCampaign.id}/attachments`, form);
      }

      if (submitMode === 'draft') {
        navigate('/');
      } else {
        navigate(`/campaigns/${savedCampaign.id}/review`);
      }
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Unable to save campaign');
    }
  }

  return (
    <form onSubmit={handleSubmit(submit)} className="grid gap-6 lg:grid-cols-[1fr_360px]">
      <section className="space-y-5 rounded border border-slate-200 bg-white p-5">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h1 className="text-2xl font-semibold text-slate-950">{isEditing ? 'Continue Campaign Setup' : 'Create Campaign'}</h1>
            <p className="text-sm text-slate-500">Use CSV headers as placeholders with double braces.</p>
          </div>
          <BackButton />
        </div>
        <label className="block">
          <span className="mb-1 block text-sm font-medium text-slate-700">Campaign name</span>
          <input placeholder="Job application outreach" className="focus-ring h-11 w-full rounded border border-slate-300 px-3" {...register('campaignName', { required: true })} />
        </label>
        <label className="block">
          <span className="mb-1 block text-sm font-medium text-slate-700">Subject template</span>
          <input placeholder="Application for {{role}} at {{company}}" className="focus-ring h-11 w-full rounded border border-slate-300 px-3" {...register('subjectTemplate', { required: true })} />
        </label>
        <label className="block">
          <span className="mb-1 block text-sm font-medium text-slate-700">Body template</span>
          <textarea
            rows={12}
            placeholder={'Hello {{name}},\n\nI am interested in the {{role}} position at {{company}}.\nPlease find my resume attached.\n\nBest regards,\nYour Name'}
            className="focus-ring w-full rounded border border-slate-300 p-3"
            {...register('bodyTemplate', { required: true })}
          />
        </label>
        {error && <p className="rounded bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
        <div className="flex flex-wrap gap-2">
          <button type="submit" onClick={() => setSubmitMode('review')} disabled={formState.isSubmitting} className="focus-ring h-11 rounded bg-emerald-600 px-5 font-medium text-white hover:bg-emerald-700">
            Save and Review
          </button>
          <button type="submit" onClick={() => setSubmitMode('draft')} disabled={formState.isSubmitting} className="focus-ring h-11 rounded border border-slate-300 bg-white px-5 font-medium text-slate-700 hover:bg-slate-50">
            Save Draft
          </button>
        </div>
      </section>
      <aside className="space-y-4">
        <label className="block rounded border border-dashed border-slate-300 bg-white p-5">
          <Upload className="mb-3 text-emerald-700" size={22} />
          <span className="mb-1 block text-sm font-medium text-slate-800">CSV or Excel recipients</span>
          <span className="mb-3 block text-xs text-slate-500">Required columns: name, email</span>
          <input type="file" accept=".csv,.xlsx,.xls" onChange={(event) => setCsv(event.target.files?.[0] ?? null)} />
          {csv && <span className="mt-2 block text-sm text-slate-600">{csv.name}</span>}
        </label>
        <label className="block rounded border border-dashed border-slate-300 bg-white p-5">
          <Upload className="mb-3 text-slate-700" size={22} />
          <span className="mb-1 block text-sm font-medium text-slate-800">Attachments</span>
          <span className="mb-3 block text-xs text-slate-500">Select one or more PDF/DOCX files, max 5 files</span>
          <input type="file" accept=".pdf,.docx" multiple onChange={(event) => setAttachments(event.target.files)} />
          {!!attachments?.length && (
            <ul className="mt-3 space-y-1 text-sm text-slate-600">
              {Array.from(attachments).map((file) => <li key={`${file.name}-${file.size}`} className="truncate">{file.name}</li>)}
            </ul>
          )}
        </label>
      </aside>
    </form>
  );
}

function hasRecipients(campaign?: Campaign) {
  if (!campaign?.counts) {
    return false;
  }
  return Object.values(campaign.counts as Record<RecipientStatus, number>).some((count) => count > 0);
}
