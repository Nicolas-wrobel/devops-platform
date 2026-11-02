import { useEffect, useState, type FormEvent } from 'react'
import { Nav } from '../components/Nav'
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

  return (
    <div className="min-h-screen bg-gray-50">
      <Nav />
      <main className="mx-auto max-w-3xl px-4 py-8">
        <h1 className="mb-6 text-2xl font-semibold text-gray-900">Deployments</h1>

        <form
          onSubmit={handleSubmit}
          className="mb-8 flex flex-wrap items-end gap-3 rounded-lg border border-gray-200 bg-white p-4"
        >
          <div>
            <label htmlFor="applicationId" className="block text-xs font-medium text-gray-700">Application</label>
            <select
              id="applicationId"
              className="mt-1 rounded-md border border-gray-300 px-3 py-1.5 text-sm"
              value={applicationId}
              onChange={(e) => {
                setApplicationId(e.target.value)
                setEnvironmentId('')
              }}
              required
            >
              <option value="" disabled>
                Select application
              </option>
              {applications.map((app) => (
                <option key={app.id} value={app.id}>
                  {app.name}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="environmentId" className="block text-xs font-medium text-gray-700">Environment</label>
            <select
              id="environmentId"
              className="mt-1 rounded-md border border-gray-300 px-3 py-1.5 text-sm"
              value={environmentId}
              onChange={(e) => setEnvironmentId(e.target.value)}
              required
              disabled={!applicationId}
            >
              <option value="" disabled>
                Select environment
              </option>
              {environments.map((env) => (
                <option key={env.id} value={env.id}>
                  {env.name}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="version" className="block text-xs font-medium text-gray-700">Version</label>
            <input
              id="version"
              className="mt-1 rounded-md border border-gray-300 px-3 py-1.5 text-sm"
              value={version}
              onChange={(e) => setVersion(e.target.value)}
              placeholder="1.4.2"
              required
            />
          </div>
          <button
            type="submit"
            className="rounded-md bg-blue-600 px-4 py-1.5 text-sm font-medium text-white hover:bg-blue-700"
          >
            Create
          </button>
        </form>

        {error && <p className="mb-4 text-sm text-red-600">{error}</p>}

        <ul className="divide-y divide-gray-200 rounded-lg border border-gray-200 bg-white">
          {deployments.map((deployment) => (
            <li key={deployment.id} className="flex items-center justify-between px-4 py-3">
              <div>
                <p className="text-sm font-medium text-gray-900">
                  {deployment.applicationName} &rarr; {deployment.environmentName} ({deployment.version})
                </p>
                <p className="text-xs text-gray-500">
                  {deployment.status} &middot; triggered by {deployment.triggeredBy}
                </p>
              </div>
              {deployment.status === 'PENDING' && (
                <button
                  onClick={() => handleStart(deployment.id)}
                  className="rounded-md bg-green-600 px-3 py-1 text-xs font-medium text-white hover:bg-green-700"
                >
                  Start
                </button>
              )}
            </li>
          ))}
          {deployments.length === 0 && (
            <li className="px-4 py-6 text-center text-sm text-gray-500">No deployments yet.</li>
          )}
        </ul>
      </main>
    </div>
  )
}
