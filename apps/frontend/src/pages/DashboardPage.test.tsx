import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '../auth/AuthContext'
import { DashboardPage } from './DashboardPage'
import type { DeploymentResponse } from '../lib/types'

const successDeployment: DeploymentResponse = {
  id: 1,
  applicationId: 1,
  applicationName: 'checkout',
  environmentId: 1,
  environmentName: 'production',
  version: '1.0.0',
  status: 'SUCCESS',
  triggeredBy: 'admin',
  createdAt: '2026-01-01T00:00:00Z',
  startedAt: '2026-01-01T00:00:01Z',
  plannedFinishAt: '2026-01-01T00:00:20Z',
  finishedAt: '2026-01-01T00:00:21Z',
}

const failedDeployment: DeploymentResponse = {
  ...successDeployment,
  id: 2,
  version: '1.0.1',
  status: 'FAILED',
  createdAt: '2026-01-02T00:00:00Z',
}

vi.mock('../lib/api', () => ({
  api: {
    get: vi.fn((path: string) => {
      if (path === '/deployments') return Promise.resolve({ data: [successDeployment, failedDeployment] })
      return Promise.resolve({ data: [] })
    }),
  },
}))

describe('DashboardPage', () => {
  it('renders summary counts, the status chart, and the recent deployments list', async () => {
    render(
      <MemoryRouter>
        <AuthProvider>
          <DashboardPage />
        </AuthProvider>
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeInTheDocument()
    expect(await screen.findByText('checkout → production (1.0.0)')).toBeInTheDocument()

    const list = screen.getByRole('list', { name: 'Recent deployments' })
    expect(within(list).getByText('SUCCESS')).toBeInTheDocument()
    expect(within(list).getByText('FAILED')).toBeInTheDocument()

    // The chart's x-axis plots one tick per status, regardless of count.
    expect(screen.getByText('Deployments by status')).toBeInTheDocument()
    for (const status of ['PENDING', 'RUNNING', 'SUCCESS', 'FAILED']) {
      expect(screen.getAllByText(status).length).toBeGreaterThan(0)
    }
  })
})
