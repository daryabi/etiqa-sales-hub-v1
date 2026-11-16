import { fetchWithAuth, throwProblem } from '../../api/httpClient';

export const reportsApi = {
  fetchSalesPdfBlob: async (startDate: string, endDate: string): Promise<Blob> => {
    const query = new URLSearchParams({ startDate, endDate });
    const response = await fetchWithAuth(`/reports/sales/pdf?${query}`, {
      headers: { Accept: 'application/pdf' },
    });
    if (!response.ok) {
      return throwProblem(response, 'Failed to generate PDF report');
    }
    return response.blob();
  },
};
