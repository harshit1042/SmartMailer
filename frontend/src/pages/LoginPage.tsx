import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import { Mail } from 'lucide-react';
import { api, tokenKey } from '../api/client';
import type { AuthResponse } from '../types';

interface FormValues {
  name: string;
  email: string;
  password: string;
}

export default function LoginPage() {
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [error, setError] = useState('');
  const { register, handleSubmit, formState } = useForm<FormValues>({
    defaultValues: { name: '', email: '', password: '' }
  });
  const navigate = useNavigate();

  async function submit(values: FormValues) {
    setError('');
    try {
      const endpoint = mode === 'login' ? '/auth/login' : '/auth/register';
      const payload = mode === 'login'
        ? { email: values.email, password: values.password }
        : values;
      const { data } = await api.post<AuthResponse>(endpoint, payload);
      localStorage.setItem(tokenKey, data.token);
      navigate('/');
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Authentication failed');
    }
  }

  return (
    <main className="grid min-h-screen place-items-center bg-slate-100 px-4">
      <form onSubmit={handleSubmit(submit)} className="w-full max-w-md rounded border border-slate-200 bg-white p-6 shadow-sm">
        <div className="mb-6 flex items-center gap-3">
          <span className="grid h-11 w-11 place-items-center rounded bg-emerald-600 text-white">
            <Mail size={22} />
          </span>
          <div>
            <h1 className="text-xl font-semibold text-slate-950">SmartMailer</h1>
            <p className="text-sm text-slate-500">Bulk campaign manager</p>
          </div>
        </div>
        <div className="mb-5 grid grid-cols-2 rounded border border-slate-200 bg-slate-50 p-1">
          {(['login', 'register'] as const).map((item) => (
            <button
              key={item}
              type="button"
              onClick={() => setMode(item)}
              className={`h-9 rounded text-sm font-medium ${mode === item ? 'bg-white text-slate-950 shadow-sm' : 'text-slate-600'}`}
            >
              {item === 'login' ? 'Login' : 'Register'}
            </button>
          ))}
        </div>
        {mode === 'register' && (
          <label className="mb-4 block">
            <span className="mb-1 block text-sm font-medium text-slate-700">Name</span>
            <input placeholder="Aman Patel" className="focus-ring h-11 w-full rounded border border-slate-300 px-3" {...register('name')} />
          </label>
        )}
        <label className="mb-4 block">
          <span className="mb-1 block text-sm font-medium text-slate-700">Email</span>
          <input placeholder="you@gmail.com" className="focus-ring h-11 w-full rounded border border-slate-300 px-3" {...register('email', { required: true })} />
        </label>
        <label className="mb-4 block">
          <span className="mb-1 block text-sm font-medium text-slate-700">Password</span>
          <input type="password" placeholder="Your SmartMailer password" className="focus-ring h-11 w-full rounded border border-slate-300 px-3" {...register('password', { required: true })} />
        </label>
        {error && <p className="mb-4 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
        <button disabled={formState.isSubmitting} className="focus-ring h-11 w-full rounded bg-emerald-600 font-medium text-white hover:bg-emerald-700">
          {mode === 'login' ? 'Login' : 'Create account'}
        </button>
      </form>
    </main>
  );
}
