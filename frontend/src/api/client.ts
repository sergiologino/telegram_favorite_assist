export type ServiceItem = {
  id: number;
  title: string;
  description: string | null;
  imageUrl: string | null;
  appUrl: string | null;
  repoUrl: string | null;
  githubStars: number | null;
  category: string | null;
  categorySlug: string | null;
  tags: string | null;
  postedAt: string | null;
  createdAt: string;
};

export type Category = {
  id: number;
  name: string;
  slug: string;
  count: number;
};

export type Page<T> = {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type Stats = {
  totalServices: number;
  totalCategories: number;
  pendingPosts: number;
  failedPosts: number;
  githubServices: number;
};

export type ProcessResponse = {
  processed: number;
  failed: number;
  skipped: number;
  pendingBefore: number;
  pendingRemaining: number;
  totalServices: number;
};

export type SyncResponse = {
  userImported: number;
  userSkipped: number;
  userError: string | null;
  botImported: number;
  botSkipped: number;
  botError: string | null;
  exportImported: number;
  exportSkippedDuplicate: number;
  exportSkippedEmpty: number;
  processed: number;
  failed: number;
  skipped: number;
};

export type ServiceFilters = {
  q?: string;
  category?: string;
  from?: string;
  to?: string;
  hasRepo?: boolean;
  page?: number;
  size?: number;
};

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, init);
  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`);
  }
  return response.json() as Promise<T>;
}

export const api = {
  getServices(filters: ServiceFilters = {}) {
    const params = new URLSearchParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value === undefined || value === '') {
        return;
      }
      if (key === 'hasRepo' && value !== true) {
        return;
      }
      params.set(key, String(value));
    });
    const query = params.toString();
    return request<Page<ServiceItem>>(`/api/services${query ? `?${query}` : ''}`);
  },

  getService(id: number) {
    return request<ServiceItem>(`/api/services/${id}`);
  },

  getCategories() {
    return request<Category[]>('/api/categories');
  },

  getStats() {
    return request<Stats>('/api/stats');
  },

  triggerSync() {
    return request<SyncResponse>('/api/sync/trigger', { method: 'POST' });
  },

  processPending() {
    return request<ProcessResponse>('/api/process/pending', { method: 'POST' });
  },

  async importExport(file: File) {
    const formData = new FormData();
    formData.append('file', file);
    const response = await fetch('/api/import/export', {
      method: 'POST',
      body: formData,
    });
    if (!response.ok) {
      throw new Error(`Import failed: ${response.status}`);
    }
    return response.json() as Promise<SyncResponse>;
  },
};
