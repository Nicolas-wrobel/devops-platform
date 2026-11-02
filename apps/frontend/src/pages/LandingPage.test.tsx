import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { LandingPage } from './LandingPage'

vi.mock('../lib/api', () => ({
  api: {
    get: vi.fn().mockResolvedValue({
      data: {
        applicationCount: 2,
        environmentCount: 3,
        deploymentCount: 10,
        successRatePercent: 80,
        pendingCount: 1,
        runningCount: 0,
        successCount: 8,
        failedCount: 1,
      },
    }),
  },
}))

const mockUseAuth = vi.hoisted(() => vi.fn())
vi.mock('../auth/useAuth', () => ({ useAuth: mockUseAuth }))

describe('LandingPage', () => {
  it('shows a "Sign in" CTA when unauthenticated', () => {
    mockUseAuth.mockReturnValue({ isAuthenticated: false })

    render(
      <MemoryRouter>
        <LandingPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('link', { name: 'Sign in' })).toHaveAttribute('href', '/login')
  })

  it('shows a "Go to dashboard" CTA when authenticated', () => {
    mockUseAuth.mockReturnValue({ isAuthenticated: true })

    render(
      <MemoryRouter>
        <LandingPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('link', { name: 'Go to dashboard' })).toHaveAttribute('href', '/dashboard')
  })

  it('renders the live stats once loaded', async () => {
    mockUseAuth.mockReturnValue({ isAuthenticated: false })

    render(
      <MemoryRouter>
        <LandingPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('2')).toBeInTheDocument()
    expect(screen.getByText('80%')).toBeInTheDocument()
  })

  it('renders the status chart once stats are loaded, without needing to log in', async () => {
    mockUseAuth.mockReturnValue({ isAuthenticated: false })

    render(
      <MemoryRouter>
        <LandingPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Deployments by status')).toBeInTheDocument()
    for (const status of ['PENDING', 'RUNNING', 'SUCCESS', 'FAILED']) {
      expect(screen.getAllByText(status).length).toBeGreaterThan(0)
    }
  })
})
