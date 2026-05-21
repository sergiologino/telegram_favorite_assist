export function buildPageNumbers(currentPage: number, totalPages: number): number[] {
  if (totalPages <= 1) {
    return totalPages === 1 ? [0] : [];
  }

  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, index) => index);
  }

  const pages = new Set<number>([0, totalPages - 1, currentPage]);
  if (currentPage > 0) {
    pages.add(currentPage - 1);
  }
  if (currentPage < totalPages - 1) {
    pages.add(currentPage + 1);
  }
  if (currentPage <= 2) {
    pages.add(1);
    pages.add(2);
  }
  if (currentPage >= totalPages - 3) {
    pages.add(totalPages - 2);
    pages.add(totalPages - 3);
  }

  return [...pages].sort((left, right) => left - right);
}

export function formatPageRange(page: number, pageSize: number, totalElements: number): string {
  if (totalElements === 0) {
    return 'Нет результатов';
  }

  const from = page * pageSize + 1;
  const to = Math.min(totalElements, (page + 1) * pageSize);
  return `Показано ${from}–${to} из ${totalElements}`;
}
