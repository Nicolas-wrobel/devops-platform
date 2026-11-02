import { LayoutDashboard } from 'lucide-react'
import { useEffect, useState } from 'react'
import { toast } from 'sonner'
import { DeploymentStatusBadge } from '../components/DeploymentStatusBadge'
import { Nav } from '../components/Nav'
import { Card, CardContent } from '../components/ui/card'
import { api } from '../lib/api'
import type { ApplicationResponse, DeploymentResponse, EnvironmentResponse } from '../lib/types'

const RECENT_DEPLOYMENTS_LIMIT = 8

export function DashboardPage() {
  const [applications, setApplications] = useState<ApplicationResponse[]>([])
  const [environments, setEnvironments] = useState<EnvironmentResponse[]>([])
  const [deployments, setDeployments] = useState<DeploymentResponse[]>([])

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

        <h2 className="mb-3 text-sm font-medium text-muted-foreground">Recent deployments</h2>
        <ul className="divide-y divide-border rounded-lg border border-border bg-card">
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
