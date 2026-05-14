import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from 'react-query';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, Download, Edit3, Eye, Paperclip, RotateCcw, Send, Trash2, Upload } from 'lucide-react';
import { api } from '../api/client';
import type { Attachment, PageResponse, Recipient, RecipientStatus } from '../types';

const badge: Record<RecipientStatus, string> = {
  PENDING: 'bg-amber-50 text-amber-700',
  APPROVED: 'bg-emerald-50 text-emerald-700',
  SENDING: 'bg-blue-50 text-blue-700',
  SENT: 'bg-slate-100 text-slate-700',
  FAILED: 'bg-red-50 text-red-700',
  DELETED: 'bg-zinc-100 text-zinc-500',
  SCHEDULED: 'bg-indigo-50 text-indigo-700'
};

export default function ReviewPage() {
  const { id } = useParams();
  const campaignId = Number(id);
  const [page, setPage] = useState(1);
  const [selected, setSelected] = useState<number[]>([]);
  const [preview, setPreview] = useState<Recipient | null>(null);
  const [editing, setEditing] = useState<Recipient | null>(null);
  const [attachmentError, setAttachmentError] = useState('');
  const [rangeStart, setRangeStart] = useState(1);
  const [rangeEnd, setRangeEnd] = useState(1);
  const [emailDelaySeconds, setEmailDelaySeconds] = useState(8);
  const [pageDelaySeconds, setPageDelaySeconds] = useState(90);
  const [rangeProgress, setRangeProgress] = useState('');
  const [isRangeSending, setIsRangeSending] = useState(false);
  const queryClient = useQueryClient();
  const key = ['recipients', campaignId, page];

  const recipients = useQuery(key, async () => (await api.get<PageResponse<Recipient>>(`/campaigns/${campaignId}/recipients?page=${page}&size=10`)).data);
  const attachments = useQuery(['attachments', campaignId], async () => (await api.get<Attachment[]>(`/campaigns/${campaignId}/attachments`)).data);
  const refresh = () => queryClient.invalidateQueries(['recipients', campaignId]);
  const mutate = useMutation((url: string) => api.post(url), { onSuccess: refresh });
  const remove = useMutation((recipientId: number) => api.delete(`/recipients/${recipientId}`), { onSuccess: refresh });
  const uploadAttachments = useMutation((files: File[]) => {
    setAttachmentError('');
    const form = new FormData();
    files.forEach((file) => form.append('files', file));
    return api.post(`/campaigns/${campaignId}/attachments`, form);
  }, {
    onSuccess: () => queryClient.invalidateQueries(['attachments', campaignId]),
    onError: (err: any) => setAttachmentError(err.response?.data?.message ?? 'Unable to upload attachment.')
  });
  const deleteAttachment = useMutation((attachmentId: number) => api.delete(`/campaigns/${campaignId}/attachments/${attachmentId}`), {
    onSuccess: () => queryClient.invalidateQueries(['attachments', campaignId]),
    onError: (err: any) => setAttachmentError(err.response?.data?.message ?? 'Unable to delete attachment.')
  });
  const updateRecipient = useMutation((recipient: Recipient) => api.put(`/recipients/${recipient.id}`, {
    name: recipient.name,
    email: recipient.email,
    company: recipient.company,
    role: recipient.role,
    customDataJson: recipient.customDataJson ?? {},
    renderedSubject: recipient.renderedSubject,
    renderedBody: recipient.renderedBody
  }), {
    onSuccess: () => {
      setEditing(null);
      refresh();
    }
  });

  function toggle(id: number) {
    setSelected((current) => current.includes(id) ? current.filter((item) => item !== id) : [...current, id]);
  }

  function runSelected(action: 'delete' | 'send') {
    selected.forEach((recipientId) => {
      if (action === 'delete') remove.mutate(recipientId);
      if (action === 'send') mutate.mutate(`/recipients/${recipientId}/send`);
    });
    setSelected([]);
  }

  function sendCurrentPage() {
    sendRecipientsWithThrottle(recipients.data?.content ?? [], `page ${page}`);
  }

  async function sendRecipientsWithThrottle(items: Recipient[], label: string) {
    const sendable = items.filter((recipient) => !['DELETED', 'SENT', 'SENDING'].includes(recipient.status));
    for (let index = 0; index < sendable.length; index++) {
      const recipient = sendable[index];
      setRangeProgress(`Sending ${label}: ${index + 1} of ${sendable.length}`);
      await api.post(`/recipients/${recipient.id}/send`);
      await queryClient.invalidateQueries(['recipients', campaignId]);
      if (index < sendable.length - 1) {
        await wait(emailDelaySeconds * 1000);
      }
    }
    setRangeProgress('');
  }

  async function sendPageRange() {
    const totalPages = recipients.data?.totalPages ?? 1;
    const start = Math.max(1, Math.min(rangeStart, totalPages));
    const end = Math.max(start, Math.min(rangeEnd, totalPages));
    setIsRangeSending(true);
    try {
      for (let current = start; current <= end; current++) {
        setRangeProgress(`Loading page ${current} of ${end}`);
        const { data } = await api.get<PageResponse<Recipient>>(`/campaigns/${campaignId}/recipients?page=${current}&size=10`);
        await sendRecipientsWithThrottle(data.content, `page ${current}`);
        if (current < end) {
          setRangeProgress(`Waiting ${pageDelaySeconds}s before page ${current + 1}`);
          await wait(pageDelaySeconds * 1000);
        }
      }
      setRangeProgress(`Finished pages ${start}-${end}`);
      await refresh();
    } finally {
      setIsRangeSending(false);
      window.setTimeout(() => setRangeProgress(''), 5000);
    }
  }

  async function fetchAttachmentBlob(attachment: Attachment) {
    const response = await api.get(`/campaigns/${campaignId}/attachments/${attachment.id}/download`, { responseType: 'blob' });
    const contentType = response.headers['content-type'] || response.data.type || 'application/octet-stream';
    return new Blob([response.data], { type: contentType });
  }

  async function viewAttachment(attachment: Attachment) {
    const tab = window.open('', '_blank');
    if (tab) {
      tab.document.write('<p style="font-family: system-ui; padding: 24px;">Loading attachment...</p>');
    }
    try {
      const blob = await fetchAttachmentBlob(attachment);
      const url = URL.createObjectURL(blob);
      if (tab) {
        tab.location.href = url;
      } else {
        downloadBlob(url, attachment.fileName);
      }
      window.setTimeout(() => URL.revokeObjectURL(url), 120_000);
    } catch {
      if (tab) {
        tab.document.body.innerHTML = '<p style="font-family: system-ui; padding: 24px; color: #b91c1c;">Unable to open attachment.</p>';
      }
    }
  }

  async function downloadAttachment(attachment: Attachment) {
    const blob = await fetchAttachmentBlob(attachment);
    const url = URL.createObjectURL(blob);
    downloadBlob(url, attachment.fileName);
    window.setTimeout(() => URL.revokeObjectURL(url), 120_000);
  }

  function downloadBlob(url: string, filename: string) {
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    link.remove();
  }

  function canPreviewAttachment(attachment: Attachment) {
    return attachment.fileName.toLowerCase().endsWith('.pdf');
  }

  function wait(ms: number) {
    return new Promise((resolve) => window.setTimeout(resolve, ms));
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-start gap-3">
          <Link to="/" className="focus-ring mt-1 inline-flex h-9 items-center gap-2 rounded border border-slate-300 bg-white px-3 text-sm font-medium text-slate-700 hover:bg-slate-50">
            <ArrowLeft size={16} />
            Back
          </Link>
          <div>
            <h1 className="text-2xl font-semibold text-slate-950">Batch Review</h1>
            <p className="text-sm text-slate-500">Review 10 emails per page, then send the current page.</p>
          </div>
        </div>
        <div className="flex flex-wrap gap-2">
          <button onClick={() => runSelected('delete')} className="focus-ring h-10 rounded border border-slate-300 bg-white px-3 text-sm font-medium">Delete Selected</button>
          <button onClick={() => runSelected('send')} className="focus-ring h-10 rounded border border-slate-300 bg-white px-3 text-sm font-medium">Send Selected</button>
          <button onClick={sendCurrentPage} className="focus-ring inline-flex h-10 items-center gap-2 rounded bg-emerald-600 px-3 text-sm font-medium text-white">
            <Send size={16} />
            Send This Page
          </button>
        </div>
      </div>

      <section className="rounded border border-slate-200 bg-white p-4">
        <div className="mb-3">
          <h2 className="text-sm font-semibold text-slate-950">Send Multiple Pages</h2>
          <p className="text-sm text-slate-500">Choose which pages to send and how long SmartMailer should wait while sending.</p>
        </div>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_minmax(0,1fr)_minmax(0,1fr)_auto] lg:items-start">
          <label className="block">
            <span className="mb-1 block text-xs font-medium uppercase text-slate-500">From page</span>
            <input type="number" min={1} max={recipients.data?.totalPages ?? 1} value={rangeStart} onChange={(event) => setRangeStart(Number(event.target.value))} className="focus-ring h-10 w-full rounded border border-slate-300 px-3" />
          </label>
          <label className="block">
            <span className="mb-1 block text-xs font-medium uppercase text-slate-500">To page</span>
            <input type="number" min={1} max={recipients.data?.totalPages ?? 1} value={rangeEnd} onChange={(event) => setRangeEnd(Number(event.target.value))} className="focus-ring h-10 w-full rounded border border-slate-300 px-3" />
          </label>
          <label className="block">
            <span className="mb-1 block text-xs font-medium uppercase text-slate-500">Wait after each email</span>
            <input type="number" min={3} value={emailDelaySeconds} onChange={(event) => setEmailDelaySeconds(Number(event.target.value))} className="focus-ring h-10 w-full rounded border border-slate-300 px-3" />
            <span className="mt-1 block text-xs text-slate-500">Seconds</span>
          </label>
          <label className="block">
            <span className="mb-1 block text-xs font-medium uppercase text-slate-500">Wait after each page</span>
            <input type="number" min={0} value={pageDelaySeconds} onChange={(event) => setPageDelaySeconds(Number(event.target.value))} className="focus-ring h-10 w-full rounded border border-slate-300 px-3" />
            <span className="mt-1 block text-xs text-slate-500">Seconds</span>
          </label>
          <div className="flex flex-col gap-1 sm:col-span-2 lg:col-span-1 lg:pt-5">
            <button disabled={isRangeSending} onClick={sendPageRange} className="focus-ring inline-flex h-10 items-center justify-center gap-2 rounded bg-slate-900 px-4 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50">
              <Send size={16} />
              Send These Pages
            </button>
            {rangeProgress && <p className="text-sm text-slate-600 lg:text-right">{rangeProgress}</p>}
          </div>
        </div>
      </section>

      <section className="overflow-x-auto rounded border border-slate-200 bg-white">
        <table className="min-w-[980px] w-full text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase text-slate-500">
            <tr>
              <th className="w-10 px-4 py-3"></th>
              <th className="px-4 py-3">Name</th>
              <th className="px-4 py-3">Email</th>
              <th className="px-4 py-3">Company</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Subject Preview</th>
              <th className="px-4 py-3">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {recipients.data?.content.map((recipient) => (
              <tr key={recipient.id}>
                <td className="px-4 py-3">
                  <input type="checkbox" checked={selected.includes(recipient.id)} onChange={() => toggle(recipient.id)} />
                </td>
                <td className="px-4 py-3 font-medium text-slate-950">{recipient.name}</td>
                <td className="px-4 py-3">{recipient.email}</td>
                <td className="px-4 py-3">{recipient.company}</td>
                <td className="px-4 py-3">
                  <span className={`rounded px-2 py-1 text-xs font-medium ${badge[recipient.status]}`}>{recipient.status}</span>
                  {recipient.status === 'FAILED' && recipient.errorMessage && (
                    <p className="mt-1 max-w-48 truncate text-xs text-red-600" title={recipient.errorMessage}>{recipient.errorMessage}</p>
                  )}
                </td>
                <td className="max-w-xs truncate px-4 py-3">{recipient.renderedSubject}</td>
                <td className="px-4 py-3">
                  <div className="flex gap-1">
                    <IconButton label="Preview" onClick={() => setPreview(recipient)} icon={<Eye size={15} />} />
                    <IconButton label="Edit" onClick={() => setEditing(recipient)} icon={<Edit3 size={15} />} />
                    {recipient.status === 'DELETED'
                      ? <IconButton label="Restore" onClick={() => mutate.mutate(`/recipients/${recipient.id}/restore`)} icon={<RotateCcw size={15} />} />
                      : <IconButton label="Delete" onClick={() => remove.mutate(recipient.id)} icon={<Trash2 size={15} />} />}
                    <IconButton label="Send" onClick={() => mutate.mutate(`/recipients/${recipient.id}/send`)} icon={<Send size={15} />} />
                  </div>
                </td>
              </tr>
            ))}
            {!recipients.data?.content.length && (
              <tr><td colSpan={7} className="px-4 py-10 text-center text-slate-500">No recipients uploaded.</td></tr>
            )}
          </tbody>
        </table>
      </section>

      <div className="grid gap-4 lg:grid-cols-[1fr_auto_1fr] lg:items-center">
        <div className="hidden lg:block" />
        <div className="flex justify-center">
          <Pagination
            page={page}
            totalPages={recipients.data?.totalPages ?? 1}
            onPageChange={setPage}
          />
        </div>
        <div className="flex items-center justify-center gap-3 lg:justify-end">
          <p className="text-sm font-medium text-slate-700">
            Results: {resultStart(recipients.data?.page ?? page, recipients.data?.size ?? 10, recipients.data?.totalElements ?? 0)} - {resultEnd(recipients.data?.page ?? page, recipients.data?.size ?? 10, recipients.data?.totalElements ?? 0)} of {recipients.data?.totalElements ?? 0}
          </p>
          <span className="rounded border border-slate-200 bg-slate-100 px-4 py-2 text-sm font-semibold text-slate-800">{recipients.data?.size ?? 10}</span>
        </div>
      </div>

      {preview && (
        <div className="fixed inset-0 z-20 grid place-items-center bg-slate-950/40 p-4" onClick={() => setPreview(null)}>
          <article className="max-h-[calc(100vh-2rem)] w-full max-w-2xl overflow-y-auto rounded border border-slate-200 bg-white p-5 shadow-lg" onClick={(event) => event.stopPropagation()}>
            <h2 className="text-lg font-semibold text-slate-950">{preview.renderedSubject}</h2>
            <p className="mt-1 text-sm text-slate-500">To: {preview.email}</p>
            <pre className="mt-4 whitespace-pre-wrap rounded bg-slate-50 p-4 text-sm text-slate-800">{preview.renderedBody}</pre>
            <div className="mt-4">
              <h3 className="text-sm font-semibold text-slate-950">Attachments</h3>
              <div className="mt-2 space-y-2">
                {attachments.data?.length ? attachments.data.map((attachment) => (
                  <div key={attachment.id} className="flex items-center justify-between gap-2 rounded border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700">
                    <span className="truncate">{attachment.fileName}</span>
                    <div className="flex shrink-0 gap-1">
                      {canPreviewAttachment(attachment) && (
                        <button type="button" onClick={() => viewAttachment(attachment)} className="focus-ring grid h-8 w-8 place-items-center rounded border border-slate-300 bg-white hover:bg-slate-50" title="View attachment" aria-label="View attachment">
                          <Eye size={15} />
                        </button>
                      )}
                      <button type="button" onClick={() => downloadAttachment(attachment)} className="focus-ring grid h-8 w-8 place-items-center rounded border border-slate-300 bg-white hover:bg-slate-50" title="Download attachment" aria-label="Download attachment">
                        <Download size={15} />
                      </button>
                      <button type="button" onClick={() => deleteAttachment.mutate(attachment.id)} className="focus-ring grid h-8 w-8 place-items-center rounded border border-slate-300 bg-white text-red-600 hover:bg-red-50" title="Delete attachment" aria-label="Delete attachment">
                        <Trash2 size={15} />
                      </button>
                    </div>
                  </div>
                )) : <p className="text-sm text-slate-500">No attachments added.</p>}
              </div>
            </div>
            <button onClick={() => setPreview(null)} className="mt-4 h-10 rounded bg-slate-900 px-4 text-sm font-medium text-white">Close</button>
          </article>
        </div>
      )}

      {editing && (
        <div className="fixed inset-0 z-20 grid place-items-center bg-slate-950/40 p-4" onClick={() => setEditing(null)}>
          <article className="max-h-[calc(100vh-2rem)] w-full max-w-2xl overflow-y-auto rounded border border-slate-200 bg-white p-5 shadow-lg" onClick={(event) => event.stopPropagation()}>
            <h2 className="text-lg font-semibold text-slate-950">Edit Recipient Email</h2>
            <div className="mt-4 grid gap-3 md:grid-cols-2">
              <EditField label="Name" value={editing.name} onChange={(value) => setEditing({ ...editing, name: value })} />
              <EditField label="Email" value={editing.email} onChange={(value) => setEditing({ ...editing, email: value })} />
              <EditField label="Company" value={editing.company} onChange={(value) => setEditing({ ...editing, company: value })} />
              <EditField label="Role" value={editing.role} onChange={(value) => setEditing({ ...editing, role: value })} />
            </div>
            <label className="mt-3 block">
              <span className="mb-1 block text-sm font-medium text-slate-700">Subject</span>
              <input className="focus-ring h-10 w-full rounded border border-slate-300 px-3" value={editing.renderedSubject} onChange={(event) => setEditing({ ...editing, renderedSubject: event.target.value })} />
            </label>
            <label className="mt-3 block">
              <span className="mb-1 block text-sm font-medium text-slate-700">Body</span>
              <textarea rows={10} className="focus-ring w-full rounded border border-slate-300 p-3" value={editing.renderedBody} onChange={(event) => setEditing({ ...editing, renderedBody: event.target.value })} />
            </label>
            <div className="mt-4 rounded border border-slate-200 bg-slate-50 p-3">
              <div className="mb-3 flex items-center gap-2 text-sm font-semibold text-slate-950">
                <Paperclip size={16} />
                Attachments
              </div>
              <div className="space-y-2">
                {attachments.data?.length ? attachments.data.map((attachment) => (
                  <div key={attachment.id} className="flex items-center justify-between gap-2 rounded border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700">
                    <span className="truncate">{attachment.fileName}</span>
                    <div className="flex shrink-0 gap-1">
                      {canPreviewAttachment(attachment) && (
                        <button type="button" onClick={() => viewAttachment(attachment)} className="focus-ring grid h-8 w-8 place-items-center rounded border border-slate-300 bg-white hover:bg-slate-50" title="View attachment" aria-label="View attachment">
                          <Eye size={15} />
                        </button>
                      )}
                      <button type="button" onClick={() => downloadAttachment(attachment)} className="focus-ring grid h-8 w-8 place-items-center rounded border border-slate-300 bg-white hover:bg-slate-50" title="Download attachment" aria-label="Download attachment">
                        <Download size={15} />
                      </button>
                      <button type="button" onClick={() => deleteAttachment.mutate(attachment.id)} className="focus-ring grid h-8 w-8 place-items-center rounded border border-slate-300 bg-white text-red-600 hover:bg-red-50" title="Delete attachment" aria-label="Delete attachment">
                        <Trash2 size={15} />
                      </button>
                    </div>
                  </div>
                )) : <p className="text-sm text-slate-500">No attachments added.</p>}
              </div>
              <label className="mt-3 flex cursor-pointer items-center justify-center gap-2 rounded border border-dashed border-slate-300 bg-white px-3 py-3 text-sm font-medium text-slate-700 hover:bg-slate-50">
                <Upload size={16} />
                Add PDF/DOCX Attachments
                <input
                  type="file"
                  accept=".pdf,.docx"
                  multiple
                  className="hidden"
                  onChange={(event) => {
                    if (event.target.files?.length) {
                      uploadAttachments.mutate(Array.from(event.target.files));
                      event.target.value = '';
                    }
                  }}
                />
              </label>
              {attachmentError && <p className="mt-2 text-sm text-red-600">{attachmentError}</p>}
            </div>
            <div className="mt-4 flex justify-end gap-2">
              <button type="button" onClick={() => setEditing(null)} className="h-10 rounded border border-slate-300 bg-white px-4 text-sm font-medium text-slate-700">Cancel</button>
              <button type="button" onClick={() => updateRecipient.mutate(editing)} className="h-10 rounded bg-emerald-600 px-4 text-sm font-medium text-white">Save</button>
            </div>
          </article>
        </div>
      )}
    </div>
  );
}

function IconButton({ label, icon, onClick }: { label: string; icon: React.ReactNode; onClick: () => void }) {
  return (
    <button type="button" title={label} aria-label={label} onClick={onClick} className="focus-ring grid h-8 w-8 place-items-center rounded border border-slate-300 bg-white text-slate-700 hover:bg-slate-50">
      {icon}
    </button>
  );
}

function EditField({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return (
    <label className="block">
      <span className="mb-1 block text-sm font-medium text-slate-700">{label}</span>
      <input className="focus-ring h-10 w-full rounded border border-slate-300 px-3" value={value ?? ''} onChange={(event) => onChange(event.target.value)} />
    </label>
  );
}

function Pagination({ page, totalPages, onPageChange }: { page: number; totalPages: number; onPageChange: (page: number) => void }) {
  const [editingPage, setEditingPage] = useState(false);
  const [draftPage, setDraftPage] = useState(String(page));
  const pages = paginationItems(page, totalPages);

  function commitPage() {
    const nextPage = Math.max(1, Math.min(Number(draftPage) || page, totalPages));
    setEditingPage(false);
    setDraftPage(String(nextPage));
    onPageChange(nextPage);
  }

  return (
    <div className="flex items-center gap-2">
      <button
        type="button"
        disabled={page <= 1}
        onClick={() => onPageChange(page - 1)}
        className="focus-ring grid h-10 w-10 place-items-center rounded border border-slate-300 bg-white text-lg font-semibold text-slate-700 disabled:opacity-40"
        aria-label="Previous page"
      >
        ‹
      </button>
      {pages.map((item, index) => item === '...'
        ? <span key={`ellipsis-${index}`} className="grid h-10 w-10 place-items-center rounded border border-slate-200 bg-white text-sm font-semibold text-slate-500">...</span>
        : item === page && editingPage ? (
          <input
            key={item}
            autoFocus
            value={draftPage}
            onChange={(event) => setDraftPage(event.target.value)}
            onBlur={commitPage}
            onKeyDown={(event) => {
              if (event.key === 'Enter') commitPage();
              if (event.key === 'Escape') {
                setEditingPage(false);
                setDraftPage(String(page));
              }
            }}
            className="focus-ring h-10 w-14 rounded border border-emerald-600 bg-white px-2 text-center text-sm font-semibold text-slate-900"
          />
        ) : (
          <button
            key={item}
            type="button"
            onClick={() => onPageChange(item)}
            onDoubleClick={() => {
              if (item === page) {
                setDraftPage(String(page));
                setEditingPage(true);
              }
            }}
            className={`focus-ring grid h-10 w-10 place-items-center rounded border text-sm font-semibold ${item === page ? 'border-emerald-600 bg-emerald-600 text-white' : 'border-slate-300 bg-white text-slate-700'}`}
          >
            {item}
          </button>
        ))}
      <button
        type="button"
        disabled={page >= totalPages}
        onClick={() => onPageChange(page + 1)}
        className="focus-ring grid h-10 w-10 place-items-center rounded border border-slate-300 bg-white text-lg font-semibold text-slate-700 disabled:opacity-40"
        aria-label="Next page"
      >
        ›
      </button>
    </div>
  );
}

function paginationItems(page: number, totalPages: number): Array<number | '...'> {
  if (totalPages <= 5) {
    return Array.from({ length: totalPages }, (_, index) => index + 1);
  }
  if (page <= 3) {
    return [1, 2, 3, '...', totalPages];
  }
  if (page >= totalPages - 2) {
    return [1, '...', totalPages - 2, totalPages - 1, totalPages];
  }
  return [1, '...', page, '...', totalPages];
}

function resultStart(page: number, size: number, total: number) {
  return total === 0 ? 0 : (page - 1) * size + 1;
}

function resultEnd(page: number, size: number, total: number) {
  return Math.min(page * size, total);
}
