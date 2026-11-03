import { AppWindow } from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import { Nav } from '../components/Nav'
import { Button } from '../components/ui/button'
import { Card, CardContent } from '../components/ui/card'
import { Input } from '../components/ui/input'
import { Label } from '../components/ui/label'
import { api } from '../lib/api'
import type { ApplicationResponse } from '../lib/types'

export function ApplicationsPage() {
  const [applications, setApplications] = useState<ApplicationResponse[]>([])
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [refreshKey, setRefreshKey] = useState(0)
  const [search, setSearch] = useState('')
  const [selectedId, setSelectedId] = useState<number | null>(null)

  useEffect(() => {
    let ignore = false
    async function load() {
      const response = await api.get<ApplicationResponse[]>('/applications')
      if (!ignore) {
        setApplications(response.data)
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
      await api.post('/applications', { name, description: description || null })
      setName('')
      setDescription('')
      setRefreshKey((key) => key + 1)
    } catch {
      setError('Could not create application (name may already exist)')
    }
  }

  const filteredApplications = applications.filter((app) =>
    app.name.toLowerCase().includes(search.toLowerCase()),
  )

  return (
    <div className="min-h-screen">
      <Nav />
      <main className="mx-auto max-w-3xl px-4 py-8">
        <h1 className="mb-6 flex items-center gap-2 text-2xl font-semibold text-foreground">
          <AppWindow className="size-6" />
          Applications
        </h1>

        <Card className="mb-8">
          <CardContent>
            <form onSubmit={handleSubmit} className="flex flex-wrap items-end gap-3">
              <div>
                <Label htmlFor="name">Name</Label>
                <Input
                  id="name"
                  className="mt-1"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                />
              </div>
              <div className="flex-1">
                <Label htmlFor="description">Description</Label>
                <Input
                  id="description"
                  className="mt-1 w-full"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                />
              </div>
              <Button type="submit">Add</Button>
            </form>
          </CardContent>
        </Card>

        {error && <p className="mb-4 text-sm text-destructive">{error}</p>}

        <Input
          type="search"
          placeholder="Search applications..."
          className="mb-4"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />

        <ul key={search} className="animate-in fade-in divide-y divide-border rounded-lg border border-border bg-card duration-200">
          {filteredApplications.map((app) => (
            <li
              key={app.id}
              onClick={() => setSelectedId(selectedId === app.id ? null : app.id)}
              className={
                selectedId === app.id
                  ? 'cursor-pointer px-4 py-3 ring-2 ring-inset ring-primary transition-colors'
                  : 'cursor-pointer px-4 py-3 transition-colors hover:bg-accent/50'
              }
            >
              <p className="text-sm font-medium text-foreground">{app.name}</p>
              <p className="text-xs text-muted-foreground">{app.description ?? 'No description'}</p>
              {selectedId === app.id && (
                <div className="animate-in fade-in mt-2 flex gap-4 text-xs text-muted-foreground duration-200">
                  <span>Created {new Date(app.createdAt).toLocaleString()}</span>
                  <span>Updated {new Date(app.updatedAt).toLocaleString()}</span>
                </div>
              )}
            </li>
          ))}
          {filteredApplications.length === 0 && (
            <li className="px-4 py-6 text-center text-sm text-muted-foreground">
              {applications.length === 0 ? 'No applications yet.' : 'No applications match your search.'}
            </li>
          )}
        </ul>
      </main>
    </div>
  )
}
