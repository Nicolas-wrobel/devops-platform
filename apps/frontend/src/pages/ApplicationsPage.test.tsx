import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '../auth/AuthContext'
import { ApplicationsPage } from './ApplicationsPage'

vi.mock('../lib/api', () => ({
  api: {
    get: vi.fn().mockResolvedValue({ data: [] }),
    post: vi.fn(),
  },
}))

describe('ApplicationsPage', () => {
  it('renders the page heading and an empty state', async () => {
    render(
      <MemoryRouter>
        <AuthProvider>
          <ApplicationsPage />
        </AuthProvider>
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'Applications' })).toBeInTheDocument()
    expect(await screen.findByText('No applications yet.')).toBeInTheDocument()
  })
})
