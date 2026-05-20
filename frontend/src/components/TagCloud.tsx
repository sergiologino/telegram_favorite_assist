import { useLayoutEffect, useRef, useState } from 'react';

type TagCloudProps = {
  tags: string[];
  selectedTags: Set<string>;
  onToggle: (tag: string) => void;
};

const COLLAPSED_MAX_HEIGHT = 'calc((1.25rem + 8px) * 5 + 32px)';

export default function TagCloud({ tags, selectedTags, onToggle }: TagCloudProps) {
  const [expanded, setExpanded] = useState(false);
  const [canExpand, setCanExpand] = useState(false);
  const contentRef = useRef<HTMLDivElement>(null);

  useLayoutEffect(() => {
    const element = contentRef.current;
    if (!element || expanded) {
      return;
    }
    setCanExpand(element.scrollHeight > element.clientHeight + 1);
  }, [tags, expanded]);

  if (tags.length === 0) {
    return null;
  }

  return (
    <section className="tag-cloud-panel" aria-label="Фильтр по тегам">
      <div className="tag-cloud-header">
        <span className="tag-cloud-title">Теги</span>
        {canExpand && <hr className="tag-cloud-header-divider" aria-hidden="true" />}
        {canExpand && (
          <button
            type="button"
            className="tag-cloud-toggle"
            onClick={() => setExpanded((value) => !value)}
            aria-expanded={expanded}
          >
            {expanded ? 'свернуть' : 'развернуть'}
          </button>
        )}
      </div>
      <div
        ref={contentRef}
        className={`tag-cloud ${expanded ? 'expanded' : 'collapsed'}`}
        style={expanded ? undefined : { maxHeight: COLLAPSED_MAX_HEIGHT }}
      >
        {tags.map((tag) => (
          <button
            key={tag}
            type="button"
            className={`tag-chip ${selectedTags.has(tag) ? 'active' : ''}`}
            onClick={() => onToggle(tag)}
          >
            {tag}
          </button>
        ))}
      </div>
    </section>
  );
}
