import { useEffect, useState, type FormEvent } from 'react'
import { Nav } from '../components/Nav'
import { Badge } from '../components/ui/badge'
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
    <div className="min-h-screen bg-background">
      <Nav />
      <main className="mx-auto max-w-3xl px-4 py-8">
        <h1 className="mb-6 text-2xl font-semibold text-foreground">Environments</h1>

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
              <div>
                <Label htmlFor="type">Type</Label>
                <Select value={type} onValueChange={(value) => setType(value as EnvironmentType)}>
                  <SelectTrigger id="type" className="mt-1">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {ENVIRONMENT_TYPES.map((t) => (
                      <SelectItem key={t} value={t}>
                        {t}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
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

        <ul className="divide-y divide-border rounded-lg border border-border bg-card">
          {environments.map((env) => (
            <li key={env.id} className="flex items-center justify-between px-4 py-3">
              <div>
                <p className="text-sm font-medium text-foreground">{env.name}</p>
                <p className="text-xs text-muted-foreground">{env.description ?? 'No description'}</p>
              </div>
              <Badge variant="secondary">{env.type}</Badge>
            </li>
          ))}
          {environments.length === 0 && (
            <li className="px-4 py-6 text-center text-sm text-muted-foreground">No environments yet.</li>
          )}
        </ul>
      </main>
    </div>
  )
}
