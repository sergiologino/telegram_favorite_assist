import { Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import CatalogPage from './pages/CatalogPage';
import ImportPage from './pages/ImportPage';
import ServiceDetailPage from './pages/ServiceDetailPage';
import { QueueProcessingProvider } from './hooks/useQueueProcessing';

export default function App() {
  return (
    <QueueProcessingProvider>
      <Layout>
        <Routes>
          <Route path="/" element={<CatalogPage />} />
          <Route path="/service/:id" element={<ServiceDetailPage />} />
          <Route path="/import" element={<ImportPage />} />
        </Routes>
      </Layout>
    </QueueProcessingProvider>
  );
}
