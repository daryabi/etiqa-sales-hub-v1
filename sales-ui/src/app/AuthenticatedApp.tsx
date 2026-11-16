import React, { useCallback, useEffect, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  BookOpen,
  DollarSign,
  FileSpreadsheet,
  FileText,
  History,
  LogOut,
  PackageCheck,
  ShoppingBag,
  Users,
} from 'lucide-react';
import { KpiCard } from '../components/ui';
import { AUTH_STORAGE, QUERY_KEYS } from '../constants';
import { AuditLogTab } from '../features/audit/AuditLogTab';
import { useAuth } from '../features/auth/authContextStore';
import { CustomersTab } from '../features/customers/CustomersTab';
import { customersApi } from '../features/customers/customersApi';
import { ProductsTab } from '../features/products/ProductsTab';
import { productsApi } from '../features/products/productsApi';
import { ReportsTab } from '../features/reports/ReportsTab';
import { SalesTab } from '../features/sales/SalesTab';
import { salesApi } from '../features/sales/salesApi';
import { NotificationProvider } from '../notifications/NotificationProvider';
import { formatMoney } from '../utils/formatMoney';
import type { TabId } from './navigation';
import { TabButton } from './TabButton';

export const AuthenticatedApp: React.FC = () => {
  const { user, logout } = useAuth();
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<TabId>('sales');

  const handleLogout = useCallback(() => {
    logout();
    queryClient.clear();
  }, [logout, queryClient]);

  useEffect(() => {
    const onUnauthorized = () => handleLogout();
    window.addEventListener(AUTH_STORAGE.UNAUTHORIZED_EVENT, onUnauthorized);
    return () => window.removeEventListener(AUTH_STORAGE.UNAUTHORIZED_EVENT, onUnauthorized);
  }, [handleLogout]);

  const { data: salesSummary } = useQuery({
    queryKey: [QUERY_KEYS.SALES, 'summary'],
    queryFn: salesApi.getSalesSummary,
  });

  const { data: inventorySummary } = useQuery({
    queryKey: [QUERY_KEYS.PRODUCTS, 'summary'],
    queryFn: productsApi.getInventorySummary,
  });

  const { data: customerCount = 0 } = useQuery({
    queryKey: [QUERY_KEYS.CUSTOMERS, 'count'],
    queryFn: () => customersApi.getCustomers({ page: 0, size: 1 }).then((page) => page.totalElements),
  });

  return (
    <div className="min-h-screen bg-canvas">
      <nav className="bg-ink text-white border-b-4 border-brand sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex flex-wrap items-center justify-between py-3 gap-3">
            <div className="flex items-center">
              <div className="shrink-0 bg-brand p-2 rounded-xl text-ink mr-3">
                <BookOpen className="w-6 h-6" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <span className="font-bold text-lg tracking-tight">Etiqa Sales Hub</span>
                  <span className="bg-white text-ink text-xs px-2.5 py-0.5 rounded-full font-semibold">
                    POS & Inventory
                  </span>
                </div>
                <div className="text-gray-400 text-xs mt-0.5">
                  Catalog, point-of-sale, reporting, and audit operations
                </div>
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-1.5">
              <TabButton
                active={activeTab === 'sales'}
                onClick={() => setActiveTab('sales')}
                icon={<ShoppingBag className="w-4 h-4" />}
                label="POS & Sales"
              />
              <TabButton
                active={activeTab === 'products'}
                onClick={() => setActiveTab('products')}
                icon={<FileSpreadsheet className="w-4 h-4" />}
                label="Books & Upload"
              />
              <TabButton
                active={activeTab === 'customers'}
                onClick={() => setActiveTab('customers')}
                icon={<Users className="w-4 h-4" />}
                label="Customers"
              />
              <TabButton
                active={activeTab === 'reports'}
                onClick={() => setActiveTab('reports')}
                icon={<FileText className="w-4 h-4" />}
                label="PDF Reports"
              />
              {user?.role === 'ADMIN' && (
                <TabButton
                  active={activeTab === 'auditLogs'}
                  onClick={() => setActiveTab('auditLogs')}
                  icon={<History className="w-4 h-4" />}
                  label="Audit Log"
                />
              )}

              <div className="ml-2 pl-3 border-l border-gray-700 flex items-center gap-3">
                <div className="text-right leading-tight">
                  <div className="text-[11px] text-gray-400">Signed in as</div>
                  <div className="text-xs font-bold text-brand">
                    {user?.username} ({user?.role})
                  </div>
                </div>
                <button
                  type="button"
                  onClick={handleLogout}
                  title="Sign out"
                  className="p-2 rounded-lg bg-gray-800 text-gray-300 hover:text-white hover:bg-gray-700 transition-colors cursor-pointer"
                >
                  <LogOut className="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>
        </div>
      </nav>

      <main className="max-w-7xl mx-auto py-7 px-4 sm:px-6 lg:px-8 space-y-6">
        <NotificationProvider>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <KpiCard
              label="TOTAL REVENUE"
              value={formatMoney(salesSummary?.revenue ?? 0)}
              caption={`${salesSummary?.orderCount ?? 0} sales recorded`}
              icon={<DollarSign className="w-4 h-4" />}
            />
            <KpiCard
              label="UNITS SOLD"
              value={salesSummary?.unitsSold ?? 0}
              caption="Books sold"
              icon={<PackageCheck className="w-4 h-4" />}
            />
            <KpiCard
              label="ACTIVE CATALOG"
              value={`${inventorySummary?.activeTitles ?? 0} Titles`}
              caption={`${inventorySummary?.copiesInStock ?? 0} copies available`}
              icon={<BookOpen className="w-4 h-4" />}
            />
            <KpiCard
              label="CUSTOMERS"
              value={customerCount}
              caption="Active profiles"
              icon={<Users className="w-4 h-4" />}
            />
          </div>

          {activeTab === 'sales' && <SalesTab />}
          {activeTab === 'products' && <ProductsTab />}
          {activeTab === 'customers' && <CustomersTab />}
          {activeTab === 'reports' && <ReportsTab />}
          {activeTab === 'auditLogs' && user?.role === 'ADMIN' && <AuditLogTab />}
        </NotificationProvider>
      </main>
    </div>
  );
};
