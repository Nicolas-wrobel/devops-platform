import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '../auth/AuthContext'
import { DeploymentsPage } from './DeploymentsPage'

vi.mock('../lib/api', () => ({
  api: {
    get: vi.fn().mockResolvedValue({ data: [] }),
    post: vi.fn(),
  },
}))

describe('DeploymentsPage', () => {
  it('renders the page heading and an empty state', async () => {
    render(
      <MemoryRouter>
        <AuthProvider>
          <DeploymentsPage />
        </AuthProvider>
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'Deployments' })).toBeInTheDocument()
    expect(await screen.findByText('No deployments yet.')).toBeInTheDocument()
  })
})
