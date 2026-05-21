import { NavLink } from 'react-router-dom';
import { ReactNode } from 'react';
import QueueProcessingBanner from './QueueProcessingBanner';
import { ALTACOD_HOME_URL, SITE_BRAND, SITE_DESCRIPTION } from '../seo/siteConfig';

type LayoutProps = {
  children: ReactNode;
};

export default function Layout({ children }: LayoutProps) {
  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="topbar-inner">
          <a className="back-to-main" href={ALTACOD_HOME_URL}>
            ← Вернуться на главную
          </a>
          <div className="brand">
            <NavLink to="/" end>
              <strong>{SITE_BRAND}</strong>
            </NavLink>
            <span>интересные находки · Altacod</span>
          </div>
          <nav className="nav-links" aria-label="Основная навигация">
            <NavLink to="/" end>
              Каталог
            </NavLink>
            <NavLink to="/import">Импорт</NavLink>
          </nav>
        </div>
      </header>

      <main className="page">
        <QueueProcessingBanner />
        {children}
      </main>

      <footer className="site-footer">
        <p>{SITE_DESCRIPTION}</p>
        <p className="muted">
          <a href={ALTACOD_HOME_URL} target="_blank" rel="noreferrer">
            Altacod
          </a>
          {' · '}
          поиск по категориям и тегам, переход к сайту или репозиторию в один клик.
        </p>
      </footer>

      <nav className="bottom-nav" aria-label="Мобильная навигация">
        <div className="bottom-nav-inner">
          <NavLink to="/" end>
            Каталог
          </NavLink>
          <NavLink to="/import">Импорт</NavLink>
          <a href={ALTACOD_HOME_URL} target="_blank" rel="noreferrer">
            Altacod
          </a>
        </div>
      </nav>
    </div>
  );
}
