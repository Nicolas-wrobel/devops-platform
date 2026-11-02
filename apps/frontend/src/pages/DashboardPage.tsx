import { LayoutDashboard } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Bar, BarChart, CartesianGrid, Cell, XAxis } from 'recharts'
import { toast } from 'sonner'
import { DeploymentStatusBadge } from '../components/DeploymentStatusBadge'
import { Nav } from '../components/Nav'
import { Card, CardContent } from '../components/ui/card'
import { ChartContainer, ChartTooltip, ChartTooltipContent, type ChartConfig } from '../components/ui/chart'
import { api } from '../lib/api'
import { useIsDarkMode } from '../lib/theme'
import type { ApplicationResponse, DeploymentResponse, DeploymentStatus, EnvironmentResponse } from '../lib/types'

const RECENT_DEPLOYMENTS_LIMIT = 8
const DEPLOYMENT_STATUSES: DeploymentStatus[] = ['PENDING', 'RUNNING', 'SUCCESS', 'FAILED']

// Matches the colors used by DeploymentStatusBadge, so the chart and the
// badges in the "recent deployments" list agree visually.
const STATUS_COLORS: Record<DeploymentStatus, string> = {
  PENDING: 'var(--muted-foreground)',
  RUNNING: '#2563eb',
  SUCCESS: '#16a34a',
  FAILED: 'var(--destructive)',
}

const chartConfig: ChartConfig = {
  count: { label: 'Deployments' },
}

export function DashboardPage() {
  const [applications, setApplications] = useState<ApplicationResponse[]>([])
  const [environments, setEnvironments] = useState<EnvironmentResponse[]>([])
  const [deployments, setDeployments] = useState<DeploymentResponse[]>([])
  const isDarkMode = useIsDarkMode()

  useEffect(() => {
    let ignore = false
    async function load() {
      try {
        const [environmentsResponse, applicationsResponse, deploymentsResponse] = await Promise.all([
          api.get<EnvironmentResponse[]>('/environments'),
          api.get<ApplicationResponse[]>('/applications'),
          api.get<DeploymentResponse[]>('/deployments'),
        ])
        if (!ignore) {
          setEnvironments(environmentsResponse.data)
          setApplications(applicationsResponse.data)
          setDeployments(deploymentsResponse.data)
        }
      } catch {
        if (!ignore) {
          toast.error('Could not load dashboard data')
        }
      }
    }
    load()
    return () => {
      ignore = true
    }
  }, [])

  const recentDeployments = [...deployments]
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    .slice(0, RECENT_DEPLOYMENTS_LIMIT)

  const statusChartData = DEPLOYMENT_STATUSES.map((status) => ({
    status,
    count: deployments.filter((d) => d.status === status).length,
    fill: STATUS_COLORS[status],
  }))

  return (
    <div className="min-h-screen bg-background">
      <Nav />
      <main className="mx-auto max-w-3xl px-4 py-8">
        <h1 className="mb-6 flex items-center gap-2 text-2xl font-semibold text-foreground">
          <LayoutDashboard className="size-6" />
          Dashboard
        </h1>

        <div className="mb-8 grid grid-cols-3 gap-4">
          <SummaryCard label="Environments" value={environments.length} />
          <SummaryCard label="Applications" value={applications.length} />
          <SummaryCard label="Deployments" value={deployments.length} />
        </div>

        <Card className="mb-8">
          <CardContent>
            <h2 className="mb-3 text-sm font-medium text-muted-foreground">Deployments by status</h2>
            {/* Recharts doesn't repaint SVG fill="var(--token)" when the
                CSS variable's value changes without a resize — forcing a
                remount on theme change is the reliable fix. */}
            <ChartContainer
              key={isDarkMode ? 'dark' : 'light'}
              config={chartConfig}
              className="aspect-auto h-48 w-full"
            >
              <BarChart data={statusChartData}>
                <CartesianGrid vertical={false} />
                <XAxis dataKey="status" tickLine={false} axisLine={false} tickMargin={8} />
                <ChartTooltip content={<ChartTooltipContent hideLabel />} />
                <Bar dataKey="count" radius={4}>
                  {statusChartData.map((entry) => (
                    <Cell key={entry.status} fill={entry.fill} />
                  ))}
                </Bar>
              </BarChart>
            </ChartContainer>
          </CardContent>
        </Card>

        <h2 className="mb-3 text-sm font-medium text-muted-foreground">Recent deployments</h2>
        <ul aria-label="Recent deployments" className="divide-y divide-border rounded-lg border border-border bg-card">
          {recentDeployments.map((deployment) => (
            <li key={deployment.id} className="px-4 py-3">
              <p className="text-sm font-medium text-foreground">
                {deployment.applicationName} &rarr; {deployment.environmentName} ({deployment.version})
              </p>
              <div className="mt-1 flex items-center gap-2 text-xs text-muted-foreground">
                <DeploymentStatusBadge status={deployment.status} />
                <span>triggered by {deployment.triggeredBy}</span>
              </div>
            </li>
          ))}
          {recentDeployments.length === 0 && (
            <li className="px-4 py-6 text-center text-sm text-muted-foreground">No deployments yet.</li>
          )}
        </ul>
      </main>
    </div>
  )
}

function SummaryCard({ label, value }: { label: string; value: number }) {
  return (
    <Card>
      <CardContent>
        <p className="text-xs text-muted-foreground">{label}</p>
        <p className="text-2xl font-semibold text-foreground">{value}</p>
      </CardContent>
    </Card>
  )
}
