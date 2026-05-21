import { buildPageNumbers, formatPageRange } from '../utils/pagination';

type PaginationProps = {
  page: number;
  totalPages: number;
  totalElements: number;
  pageSize: number;
  onPageChange: (page: number) => void;
  disabled?: boolean;
};

export default function Pagination({
  page,
  totalPages,
  totalElements,
  pageSize,
  onPageChange,
  disabled = false,
}: PaginationProps) {
  if (totalPages <= 1) {
    return totalElements > 0 ? (
      <p className="pagination-summary">{formatPageRange(page, pageSize, totalElements)}</p>
    ) : null;
  }

  const pageNumbers = buildPageNumbers(page, totalPages);

  return (
    <nav className="pagination" aria-label="Навигация по страницам">
      <p className="pagination-summary">{formatPageRange(page, pageSize, totalElements)}</p>
      <div className="pagination-controls">
        <button
          type="button"
          className="secondary"
          onClick={() => onPageChange(page - 1)}
          disabled={disabled || page === 0}
        >
          ← Назад
        </button>

        <div className="pagination-pages">
          {pageNumbers.map((pageNumber, index) => {
            const previous = pageNumbers[index - 1];
            const needsEllipsis = previous !== undefined && pageNumber - previous > 1;

            return (
              <span key={pageNumber} className="pagination-page-group">
                {needsEllipsis && <span className="pagination-ellipsis">…</span>}
                <button
                  type="button"
                  className={pageNumber === page ? 'primary' : 'secondary'}
                  onClick={() => onPageChange(pageNumber)}
                  disabled={disabled}
                  aria-current={pageNumber === page ? 'page' : undefined}
                >
                  {pageNumber + 1}
                </button>
              </span>
            );
          })}
        </div>

        <button
          type="button"
          className="secondary"
          onClick={() => onPageChange(page + 1)}
          disabled={disabled || page >= totalPages - 1}
        >
          Вперёд →
        </button>
      </div>
    </nav>
  );
}
