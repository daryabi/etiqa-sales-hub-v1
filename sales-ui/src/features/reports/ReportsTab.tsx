import { useEffect, useState } from 'react';
import { Download, FileText } from 'lucide-react';
import { Button, Card, Input } from '../../components/ui';
import { useApiMutation } from '../../hooks/useApiMutation';
import { useFormFields } from '../../hooks/useFormFields';
import { getBusinessTimezone } from '../../utils/businessTimezone';
import { reportsApi } from './reportsApi';

function currentMonthSoFar() {
  const today = new Date().toLocaleDateString('en-CA', { timeZone: getBusinessTimezone() });
  return { startDate: `${today.slice(0, 8)}01`, endDate: today };
}

export function ReportsTab() {
  const [initialPeriod] = useState(currentMonthSoFar);
  const { values: period, bind } = useFormFields(initialPeriod);
  const [pdfUrl, setPdfUrl] = useState<string | null>(null);
  const timezoneLabel = getBusinessTimezone();

  useEffect(() => {
    return () => {
      if (pdfUrl) {
        URL.revokeObjectURL(pdfUrl);
      }
    };
  }, [pdfUrl]);

  const generateMutation = useApiMutation({
    mutationFn: (selected: { startDate: string; endDate: string }) =>
      reportsApi.fetchSalesPdfBlob(selected.startDate, selected.endDate),
    successMessage: 'Sales report generated and ready.',
    onSuccess: (pdf: Blob) => setPdfUrl(URL.createObjectURL(pdf)),
  });

  return (
    <div className="space-y-6">
      <Card>
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
          <div className="flex flex-wrap items-end gap-4">
            <div className="w-48">
              <Input
                type="date"
                label={`Start Date (${timezoneLabel})`}
                {...bind('startDate')}
              />
            </div>
            <div className="w-48">
              <Input
                type="date"
                label={`End Date (${timezoneLabel})`}
                {...bind('endDate')}
              />
            </div>
            <Button
              variant="primary"
              size="lg"
              onClick={() => generateMutation.mutate(period)}
              isLoading={generateMutation.isPending}
              loadingText="Generating..."
              icon={<FileText className="w-4 h-4" />}
            >
              Generate Report
            </Button>
          </div>

          {pdfUrl && (
            <a
              href={pdfUrl}
              download={`sales-report-${period.startDate}-${period.endDate}.pdf`}
              className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-ink hover:bg-neutral-800 text-white text-sm font-semibold transition"
            >
              <Download className="w-4 h-4 text-brand" /> Download PDF
            </a>
          )}
        </div>
      </Card>

      {pdfUrl && (
        <div className="rounded-2xl overflow-hidden border border-gray-200 bg-white shadow-sm">
          <iframe src={pdfUrl} title="Sales PDF Report" className="w-full h-[650px]" />
        </div>
      )}
    </div>
  );
}
