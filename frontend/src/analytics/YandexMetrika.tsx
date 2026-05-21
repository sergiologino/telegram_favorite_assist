import { useEffect, useRef } from 'react';
import { useLocation } from 'react-router-dom';
import {
  getYandexMetrikaId,
  initYandexMetrika,
  loadYandexMetrika,
  trackYandexMetrikaHit,
} from './yandexMetrika';

export default function YandexMetrika() {
  const location = useLocation();
  const metrikaId = getYandexMetrikaId();
  const initializedRef = useRef(false);
  const lastHitRef = useRef<string | null>(null);

  useEffect(() => {
    if (!metrikaId || initializedRef.current) {
      return;
    }

    loadYandexMetrika(metrikaId);
    initYandexMetrika(metrikaId);
    initializedRef.current = true;
  }, [metrikaId]);

  useEffect(() => {
    if (!metrikaId) {
      return;
    }

    const url = `${location.pathname}${location.search}`;
    if (lastHitRef.current === url) {
      return;
    }

    trackYandexMetrikaHit(metrikaId, url);
    lastHitRef.current = url;
  }, [location.pathname, location.search, metrikaId]);

  if (!metrikaId) {
    return null;
  }

  return (
    <noscript>
      <div>
        <img
          src={`https://mc.yandex.ru/watch/${metrikaId}`}
          style={{ position: 'absolute', left: '-9999px' }}
          alt=""
        />
      </div>
    </noscript>
  );
}
