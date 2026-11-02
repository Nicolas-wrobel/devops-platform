import { Rocket } from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import { DeploymentStatusBadge } from '../components/DeploymentStatusBadge'
import { Nav } from '../components/Nav'
import { Button } from '../components/ui/button'
import { Card, CardContent } from '../components/ui/card'
import { Input } from '../components/ui/input'
import { Label } from '../components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select'
import { api } from '../lib/api'
import type { ApplicationResponse, DeploymentResponse, EnvironmentResponse } from '../lib/types'

export function DeploymentsPage() {
  const [deployments, setDeployments] = useState<DeploymentResponse[]>([])
  const [applications, setApplications] = useState<ApplicationResponse[]>([])
  const [environments, setEnvironments] = useState<EnvironmentResponse[]>([])
  const [applicationId, setApplicationId] = useState('')
  const [environmentId, setEnvironmentId] = useState('')
  const [version, setVersion] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [refreshKey, setRefreshKey] = useState(0)
  const [search, setSearch] = useState('')
  const [selectedId, setSelectedId] = useState<number | null>(null)

  useEffect(() => {
    let ignore = false
    async function load() {
      const [deploymentsResponse, applicationsResponse] = await Promise.all([
        api.get<DeploymentResponse[]>('/deployments'),
        api.get<ApplicationResponse[]>('/applications'),
      ])
      if (!ignore) {
        setDeployments(deploymentsResponse.data)
        setApplications(applicationsResponse.data)
      }
    }
    load()
    return () => {
      ignore = true
    }
  }, [refreshKey])

  useEffect(() => {
    if (!applicationId) {
      return
    }
    let ignore = false
    async function loadEnvironments() {
      const response = await api.get<EnvironmentResponse[]>(`/applications/${applicationId}/environments`)
      if (!ignore) {
        setEnvironments(response.data)
      }
    }
    loadEnvironments()
    return () => {
      ignore = true
    }
  }, [applicationId])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    try {
      await api.post('/deployments', { applicationId: Number(applicationId), environmentId: Number(environmentId), version })
      setVersion('')
      setRefreshKey((key) => key + 1)
    } catch {
      setError('Could not create deployment')
    }
  }

  async function handleStart(id: number) {
    setError(null)
    try {
      await api.post(`/deployments/${id}/start`)
      setRefreshKey((key) => key + 1)
    } catch {
      setError('Could not start deployment (another one may already be running for this application/environment)')
    }
  }

  const filteredDeployments = deployments.filter((d) => {
    const haystack = `${d.applicationName} ${d.environmentName} ${d.version}`.toLowerCase()
    return haystack.includes(search.toLowerCase())
  })

  return (
    <div className="min-h-screen">
      <Nav />
      <main className="mx-auto max-w-3xl px-4 py-8">
        <h1 className="mb-6 flex items-center gap-2 text-2xl font-semibold text-foreground">
          <Rocket className="size-6" />
          Deployments
        </h1>

        <Card className="mb-8">
          <CardContent>
            <form onSubmit={handleSubmit} className="flex flex-wrap items-end gap-3">
              <div>
                <Label htmlFor="applicationId">Application</Label>
                <Select
                  items={Object.fromEntries(applications.map((app) => [String(app.id), app.name]))}
                  value={applicationId}
                  onValueChange={(value) => {
                    setApplicationId(value ?? '')
                    setEnvironmentId('')
                  }}
                >
                  <SelectTrigger id="applicationId" className="mt-1">
                    <SelectValue placeholder="Select application" />
                  </SelectTrigger>
                  <SelectContent>
                    {applications.map((app) => (
                      <SelectItem key={app.id} value={String(app.id)}>
                        {app.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div>
                <Label htmlFor="environmentId">Environment</Label>
                <Select
                  items={Object.fromEntries(environments.map((env) => [String(env.id), env.name]))}
                  value={environmentId}
                  onValueChange={(value) => setEnvironmentId(value ?? '')}
                  disabled={!applicationId}
                >
                  <SelectTrigger id="environmentId" className="mt-1">
                    <SelectValue placeholder="Select environment" />
                  </SelectTrigger>
                  <SelectContent>
                    {environments.map((env) => (
                      <SelectItem key={env.id} value={String(env.id)}>
                        {env.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div>
                <Label htmlFor="version">Version</Label>
                <Input
                  id="version"
                  className="mt-1"
                  value={version}
                  onChange={(e) => setVersion(e.target.value)}
                  placeholder="1.4.2"
                  required
                />
              </div>
              <Button type="submit">Create</Button>
            </form>
          </CardContent>
        </Card>

        {error && <p className="mb-4 text-sm text-destructive">{error}</p>}

        <Input
          type="search"
          placeholder="Search deployments..."
          className="mb-4"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />

        <ul key={search} className="animate-in fade-in divide-y divide-border rounded-lg border border-border bg-card duration-200">
          {filteredDeployments.map((deployment) => (
            <li
              key={deployment.id}
              onClick={() => setSelectedId(selectedId === deployment.id ? null : deployment.id)}
              className={
                selectedId === deployment.id
                  ? 'cursor-pointer px-4 py-3 ring-2 ring-inset ring-primary transition-colors'
                  : 'cursor-pointer px-4 py-3 transition-colors hover:bg-accent/50'
              }
            >
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-foreground">
                    {deployment.applicationName} &rarr; {deployment.environmentName} ({deployment.version})
                  </p>
                  <div className="mt-1 flex items-center gap-2 text-xs text-muted-foreground">
                    <DeploymentStatusBadge status={deployment.status} />
                    <span>triggered by {deployment.triggeredBy}</span>
                  </div>
                </div>
                {deployment.status === 'PENDING' && (
                  <Button
                    size="sm"
                    onClick={(e) => {
                      e.stopPropagation()
                      handleStart(deployment.id)
                    }}
                  >
                    Start
                  </Button>
                )}
              </div>
              {selectedId === deployment.id && (
                <div className="animate-in fade-in mt-2 flex flex-wrap gap-4 text-xs text-muted-foreground duration-200">
                  <span>Started {deployment.startedAt ? new Date(deployment.startedAt).toLocaleString() : '—'}</span>
                  <span>
                    Planned finish{' '}
                    {deployment.plannedFinishAt ? new Date(deployment.plannedFinishAt).toLocaleString() : '—'}
                  </span>
                  <span>Finished {deployment.finishedAt ? new Date(deployment.finishedAt).toLocaleString() : '—'}</span>
                </div>
              )}
            </li>
          ))}
          {filteredDeployments.length === 0 && (
            <li className="px-4 py-6 text-center text-sm text-muted-foreground">
              {deployments.length === 0 ? 'No deployments yet.' : 'No deployments match your search.'}
            </li>
          )}
        </ul>
      </main>
    </div>
  )
}
