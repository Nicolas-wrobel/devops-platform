import { BookOpen, Code2, ExternalLink, Workflow } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { DeploymentStatusChart } from '../components/DeploymentStatusChart'
import { ThemeToggle } from '../components/ThemeToggle'
import { Badge } from '../components/ui/badge'
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

const TECH_STACK = [
  'Spring Boot',
  'Java 21',
  'React 19',
  'TypeScript',
  'PostgreSQL',
  'Docker',
  'Flyway',
  'Spring Security (JWT)',
  'Prometheus',
  'Grafana',
  'GitHub Actions',
]

const REPO_URL = 'https://github.com/Nicolas-wrobel/devops-platform'

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
    <div className="min-h-screen">
      <header className="border-b border-border px-4 py-3">
        <div className="mx-auto flex max-w-3xl items-center justify-between">
          <span className="flex items-center gap-1.5 text-sm font-medium text-foreground">
            <Workflow className="size-4" />
            DevOps Platform
          </span>
          <ThemeToggle />
        </div>
      </header>

      <main className="mx-auto max-w-3xl px-4 py-16 animate-in fade-in slide-in-from-bottom-4 duration-500">
        <h1 className="flex items-center gap-3 text-3xl font-semibold text-foreground">
          <Workflow className="size-8" />
          DevOps Platform
        </h1>
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

        {stats && (
          <Card className="mt-12">
            <CardContent>
              <h2 className="mb-3 text-sm font-medium text-muted-foreground">Deployments by status</h2>
              <DeploymentStatusChart
                counts={{
                  PENDING: stats.pendingCount,
                  RUNNING: stats.runningCount,
                  SUCCESS: stats.successCount,
                  FAILED: stats.failedCount,
                }}
              />
            </CardContent>
          </Card>
        )}

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

        <div className="mt-16">
          <h2 className="text-lg font-semibold text-foreground">Built with</h2>
          <div className="mt-4 flex flex-wrap gap-2">
            {TECH_STACK.map((tech) => (
              <Badge key={tech} variant="secondary">
                {tech}
              </Badge>
            ))}
          </div>
        </div>

        <div className="mt-16 flex flex-wrap gap-6">
          <a
            href={REPO_URL}
            target="_blank"
            rel="noreferrer"
            className="flex items-center gap-1.5 text-sm font-medium text-foreground hover:text-primary"
          >
            <Code2 className="size-4" />
            Source code
            <ExternalLink className="size-3.5 text-muted-foreground" />
          </a>
          <a
            href={`${REPO_URL}/blob/main/docs/ARCHITECTURE.md`}
            target="_blank"
            rel="noreferrer"
            className="flex items-center gap-1.5 text-sm font-medium text-foreground hover:text-primary"
          >
            <BookOpen className="size-4" />
            Architecture docs
            <ExternalLink className="size-3.5 text-muted-foreground" />
          </a>
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
