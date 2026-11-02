import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '../auth/AuthContext'
import { EnvironmentsPage } from './EnvironmentsPage'

vi.mock('../lib/api', () => ({
  api: {
    get: vi.fn().mockResolvedValue({ data: [] }),
    post: vi.fn(),
  },
}))

describe('EnvironmentsPage', () => {
  it('renders the page heading and an empty state', async () => {
    render(
      <MemoryRouter>
        <AuthProvider>
          <EnvironmentsPage />
        </AuthProvider>
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'Environments' })).toBeInTheDocument()
    expect(await screen.findByText('No environments yet.')).toBeInTheDocument()
  })
})
