import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { ExternalLink, Eye, EyeOff, KeyRound } from 'lucide-react';
import { useQuery, useQueryClient } from 'react-query';
import { api } from '../api/client';
import BackButton from '../components/BackButton';
import type { GmailSettings } from '../types';

interface FormValues {
  gmailAppPassword: string;
}

interface RevealValues {
  loginPassword: string;
}

export default function SettingsPage() {
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [revealedPassword, setRevealedPassword] = useState('');
  const [revealError, setRevealError] = useState('');
  const queryClient = useQueryClient();
  const gmail = useQuery('gmail-settings', async () => (await api.get<GmailSettings>('/auth/gmail-settings')).data);
  const { register, handleSubmit, reset, formState } = useForm<FormValues>({
    defaultValues: { gmailAppPassword: '' }
  });
  const revealForm = useForm<RevealValues>({
    defaultValues: { loginPassword: '' }
  });

  async function submit(values: FormValues) {
    setMessage('');
    setError('');
    try {
      await api.post('/auth/gmail-settings', values);
      reset();
      await queryClient.invalidateQueries('gmail-settings');
      setMessage('Gmail settings saved.');
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Unable to save Gmail settings');
    }
  }

  async function reveal(values: RevealValues) {
    setRevealError('');
    setRevealedPassword('');
    try {
      const { data } = await api.post<{ gmailAppPassword: string }>('/auth/gmail-settings/reveal', values);
      setRevealedPassword(data.gmailAppPassword);
      revealForm.reset();
    } catch (err: any) {
      setRevealError(err.response?.data?.message ?? 'Unable to reveal Gmail App Password');
    }
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-slate-950">Settings</h1>
          <p className="text-sm text-slate-500">Connect the Gmail account used for sending campaigns.</p>
        </div>
        <BackButton />
      </div>

      <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_420px]">
        <section className="rounded border border-slate-200 bg-white p-5 shadow-sm">
          <div className="mb-4 flex items-center justify-between gap-3">
            <h2 className="font-semibold text-slate-950">How to generate a Gmail App Password</h2>
            <a
              href="https://myaccount.google.com/apppasswords"
              target="_blank"
              rel="noreferrer"
              className="focus-ring inline-flex h-9 items-center gap-2 rounded border border-slate-300 bg-white px-3 text-sm font-medium text-slate-700 hover:bg-slate-50"
            >
              <ExternalLink size={15} />
              Open Google
            </a>
          </div>
          <ol className="grid gap-3 text-sm text-slate-700 md:grid-cols-2">
            <li className="rounded border border-slate-200 bg-slate-50 p-3">1. Open your Google Account and go to Security.</li>
            <li className="rounded border border-slate-200 bg-slate-50 p-3">2. Turn on 2-Step Verification if it is not already enabled.</li>
            <li className="rounded border border-slate-200 bg-slate-50 p-3">3. Open App passwords from Google Account security.</li>
            <li className="rounded border border-slate-200 bg-slate-50 p-3">4. Create an app password with the app name SmartMailer.</li>
            <li className="rounded border border-slate-200 bg-slate-50 p-3">5. Copy the 16-character password Google shows.</li>
            <li className="rounded border border-slate-200 bg-slate-50 p-3">6. Paste that password in the Gmail settings form.</li>
          </ol>
          <p className="mt-4 rounded bg-amber-50 px-3 py-2 text-sm text-amber-800">
            Use the Gmail account you registered with. Your normal Gmail password will fail.
          </p>
        </section>

        <div className="space-y-5">
          <form onSubmit={handleSubmit(submit)} className="rounded border border-slate-200 bg-white p-5 shadow-sm">
        <div className="mb-5 flex items-center gap-3">
          <span className="grid h-10 w-10 place-items-center rounded bg-emerald-600 text-white">
            <KeyRound size={18} />
          </span>
          <div>
            <h2 className="font-semibold text-slate-950">Gmail App Password</h2>
            <p className="text-sm text-slate-500">Use a Google App Password, not your Gmail login password.</p>
          </div>
        </div>

        {gmail.data?.connected && (
          <div className="mb-4 rounded border border-emerald-200 bg-emerald-50 p-3">
            <p className="text-sm font-medium text-emerald-900">Gmail connected for {gmail.data.smtpUsername}</p>
            <p className="mt-1 font-mono text-sm text-emerald-800">{revealedPassword || gmail.data.maskedAppPassword}</p>
          </div>
        )}

        <label className="mb-4 block">
          <span className="mb-1 block text-sm font-medium text-slate-700">{gmail.data?.connected ? 'Replace App Password' : 'App Password'}</span>
          <input
            type="password"
            placeholder="16-character Google app password"
            className="focus-ring h-11 w-full rounded border border-slate-300 px-3"
            {...register('gmailAppPassword', { required: true })}
          />
        </label>

        {message && <p className="mb-4 rounded bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{message}</p>}
        {error && <p className="mb-4 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}

        <button disabled={formState.isSubmitting} className="focus-ring h-10 rounded bg-emerald-600 px-4 text-sm font-medium text-white hover:bg-emerald-700">
          Save Gmail
        </button>
          </form>

          {gmail.data?.connected && (
            <form onSubmit={revealForm.handleSubmit(reveal)} className="rounded border border-slate-200 bg-white p-5 shadow-sm">
          <div className="mb-4 flex items-center gap-3">
            <span className="grid h-10 w-10 place-items-center rounded bg-slate-900 text-white">
              {revealedPassword ? <EyeOff size={18} /> : <Eye size={18} />}
            </span>
            <div>
              <h2 className="font-semibold text-slate-950">View Saved App Password</h2>
              <p className="text-sm text-slate-500">Enter your SmartMailer login password to reveal the saved Gmail App Password.</p>
            </div>
          </div>
          <label className="mb-4 block">
            <span className="mb-1 block text-sm font-medium text-slate-700">Login password</span>
            <input
              type="password"
              className="focus-ring h-11 w-full rounded border border-slate-300 px-3"
              {...revealForm.register('loginPassword', { required: true })}
            />
          </label>
          {revealError && <p className="mb-4 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{revealError}</p>}
          <div className="flex gap-2">
            <button disabled={revealForm.formState.isSubmitting} className="focus-ring h-10 rounded bg-slate-900 px-4 text-sm font-medium text-white hover:bg-slate-800">
              Reveal
            </button>
            {revealedPassword && (
              <button type="button" onClick={() => setRevealedPassword('')} className="focus-ring h-10 rounded border border-slate-300 bg-white px-4 text-sm font-medium text-slate-700">
                Hide
              </button>
            )}
          </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
