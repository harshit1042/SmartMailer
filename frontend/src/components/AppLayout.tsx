import { LogOut, MailPlus, Settings } from 'lucide-react';
import { Link, Navigate, Outlet, useNavigate } from 'react-router-dom';
import { tokenKey } from '../api/client';

export default function AppLayout() {
  const navigate = useNavigate();
  const token = localStorage.getItem(tokenKey);

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  function logout() {
    localStorage.removeItem(tokenKey);
    navigate('/login');
  }

  return (
    <div className="min-h-screen">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-3">
          <Link to="/" className="flex items-center gap-2 text-lg font-semibold text-slate-950">
            <span className="grid h-9 w-9 place-items-center rounded bg-emerald-600 text-white">
              <MailPlus size={20} />
            </span>
            SmartMailer
          </Link>
          <div className="flex items-center gap-2">
            <Link
              to="/settings"
              className="focus-ring inline-flex h-10 items-center gap-2 rounded border border-slate-300 bg-white px-3 text-sm font-medium text-slate-700 hover:bg-slate-50"
            >
              <Settings size={16} />
              Settings
            </Link>
            <button
              type="button"
              onClick={logout}
              className="focus-ring inline-flex h-10 items-center gap-2 rounded border border-slate-300 bg-white px-3 text-sm font-medium text-slate-700 hover:bg-slate-50"
            >
              <LogOut size={16} />
              Logout
            </button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-7xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  );
}
