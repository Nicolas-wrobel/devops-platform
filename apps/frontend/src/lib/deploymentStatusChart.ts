import type { ChartConfig } from '../components/ui/chart'
import type { DeploymentStatus } from './types'

export const DEPLOYMENT_STATUSES: DeploymentStatus[] = ['PENDING', 'RUNNING', 'SUCCESS', 'FAILED']

// Matches the colors used by DeploymentStatusBadge, so the chart and the
// status badges shown elsewhere agree visually. Also drives the sitewide
// gradient's hue family (--primary/--chart-2), so the chart and the
// background read as one coherent palette.
export const STATUS_COLORS: Record<DeploymentStatus, string> = {
  PENDING: 'var(--muted-foreground)',
  RUNNING: '#2563eb',
  SUCCESS: '#16a34a',
  FAILED: 'var(--destructive)',
}

export const deploymentStatusChartConfig: ChartConfig = {
  count: { label: 'Deployments' },
}

export function toStatusChartData(counts: Record<DeploymentStatus, number>) {
  return DEPLOYMENT_STATUSES.map((status) => ({
    status,
    count: counts[status],
    fill: STATUS_COLORS[status],
  }))
}
