import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ThemeToggle } from '../components/ThemeToggle'
import { buttonVariants } from '../components/ui/button'
import { Card, CardContent } from '../components/ui/card'
import { Skeleton } from '../components/ui/skeleton'
import { useAuth } from '../auth/useAuth'
import { api } from '../lib/api'
import type { StatsResponse } from '../lib/types'

const TRACKED_MODULES = [
  { title: 'Applications', description: 'Which applications exist, across every team.' },
  { title: 'Environments', description: 'Where each application runs — dev, staging, production.' },
  { title: 'Deployments', description: 'Status, version, and outcome of every deployment, over time.' },
]

export function LandingPage() {
  const { isAuthenticated } = useAuth()
  const [stats, setStats] = useState<StatsResponse | null>(null)

  useEffect(() => {
    let ignore = false
    async function load() {
      try {
        const response = await api.get<StatsResponse>('/public/stats')
        if (!ignore) {
          setStats(response.data)
        }
      } catch {
        // Decorative only — a public landing page shouldn't error loudly
        // if this fails, it just skips the live numbers.
      }
    }
    load()
    return () => {
      ignore = true
    }
  }, [])

  return (
    <div className="min-h-screen bg-background">
      <header className="border-b border-border px-4 py-3">
        <div className="mx-auto flex max-w-3xl items-center justify-between">
          <span className="text-sm font-medium text-foreground">DevOps Platform</span>
          <ThemeToggle />
        </div>
      </header>

      <main className="mx-auto max-w-3xl px-4 py-16">
        <h1 className="text-3xl font-semibold text-foreground">DevOps Platform</h1>
        <p className="mt-3 max-w-xl text-muted-foreground">
          A simulated internal deployment-tracking platform — a demo project built to showcase
          full-stack and DevOps engineering practices, from CI/CD and containerization to
          observability and security.
        </p>
        <Link to={isAuthenticated ? '/dashboard' : '/login'} className={buttonVariants({ className: 'mt-6' })}>
          {isAuthenticated ? 'Go to dashboard' : 'Sign in'}
        </Link>

        <div className="mt-12 grid grid-cols-2 gap-4 sm:grid-cols-4">
          <StatCard label="Applications" value={stats?.applicationCount} />
          <StatCard label="Environments" value={stats?.environmentCount} />
          <StatCard label="Deployments" value={stats?.deploymentCount} />
          <StatCard
            label="Success rate"
            value={stats && stats.successRatePercent !== null ? `${stats.successRatePercent.toFixed(0)}%` : stats ? '—' : undefined}
          />
        </div>

        <div className="mt-16">
          <h2 className="text-lg font-semibold text-foreground">What it tracks</h2>
          <ul className="mt-4 space-y-4">
            {TRACKED_MODULES.map((module) => (
              <li key={module.title}>
                <p className="text-sm font-medium text-foreground">{module.title}</p>
                <p className="text-sm text-muted-foreground">{module.description}</p>
              </li>
            ))}
          </ul>
        </div>
      </main>
    </div>
  )
}

function StatCard({ label, value }: { label: string; value: number | string | undefined }) {
  return (
    <Card>
      <CardContent>
        <p className="text-xs text-muted-foreground">{label}</p>
        {value === undefined ? (
          <Skeleton className="mt-1 h-7 w-12" />
        ) : (
          <p className="text-2xl font-semibold text-foreground">{value}</p>
        )}
      </CardContent>
    </Card>
  )
}
