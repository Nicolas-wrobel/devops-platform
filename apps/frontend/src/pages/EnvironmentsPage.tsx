import { useEffect, useState, type FormEvent } from 'react'
import { Nav } from '../components/Nav'
import { api } from '../lib/api'
import type { EnvironmentResponse, EnvironmentType } from '../lib/types'

const ENVIRONMENT_TYPES: EnvironmentType[] = ['DEVELOPMENT', 'STAGING', 'PRODUCTION']

export function EnvironmentsPage() {
  const [environments, setEnvironments] = useState<EnvironmentResponse[]>([])
  const [name, setName] = useState('')
  const [type, setType] = useState<EnvironmentType>('DEVELOPMENT')
  const [description, setDescription] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [refreshKey, setRefreshKey] = useState(0)

  useEffect(() => {
    let ignore = false
    async function load() {
      const response = await api.get<EnvironmentResponse[]>('/environments')
      if (!ignore) {
        setEnvironments(response.data)
      }
    }
    load()
    return () => {
      ignore = true
    }
  }, [refreshKey])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    try {
      await api.post('/environments', { name, type, description: description || null })
      setName('')
      setDescription('')
      setRefreshKey((key) => key + 1)
    } catch {
      setError('Could not create environment (name may already exist)')
    }
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Nav />
      <main className="mx-auto max-w-3xl px-4 py-8">
        <h1 className="mb-6 text-2xl font-semibold text-gray-900">Environments</h1>

        <form
          onSubmit={handleSubmit}
          className="mb-8 flex flex-wrap items-end gap-3 rounded-lg border border-gray-200 bg-white p-4"
        >
          <div>
            <label className="block text-xs font-medium text-gray-700">Name</label>
            <input
              className="mt-1 rounded-md border border-gray-300 px-3 py-1.5 text-sm"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-700">Type</label>
            <select
              className="mt-1 rounded-md border border-gray-300 px-3 py-1.5 text-sm"
              value={type}
              onChange={(e) => setType(e.target.value as EnvironmentType)}
            >
              {ENVIRONMENT_TYPES.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
          </div>
          <div className="flex-1">
            <label className="block text-xs font-medium text-gray-700">Description</label>
            <input
              className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>
          <button
            type="submit"
            className="rounded-md bg-blue-600 px-4 py-1.5 text-sm font-medium text-white hover:bg-blue-700"
          >
            Add
          </button>
        </form>

        {error && <p className="mb-4 text-sm text-red-600">{error}</p>}

        <ul className="divide-y divide-gray-200 rounded-lg border border-gray-200 bg-white">
          {environments.map((env) => (
            <li key={env.id} className="flex items-center justify-between px-4 py-3">
              <div>
                <p className="text-sm font-medium text-gray-900">{env.name}</p>
                <p className="text-xs text-gray-500">{env.description ?? 'No description'}</p>
              </div>
              <span className="rounded-full bg-gray-100 px-2 py-1 text-xs font-medium text-gray-700">
                {env.type}
              </span>
            </li>
          ))}
          {environments.length === 0 && (
            <li className="px-4 py-6 text-center text-sm text-gray-500">No environments yet.</li>
          )}
        </ul>
      </main>
    </div>
  )
}
