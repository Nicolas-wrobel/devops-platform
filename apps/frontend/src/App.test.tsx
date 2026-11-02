import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import App from './App'

describe('App', () => {
  it('redirects an unknown path to the public landing page', () => {
    render(
      <MemoryRouter initialEntries={['/some-unknown-path']}>
        <App />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'DevOps Platform' })).toBeInTheDocument()
  })

  it('redirects an unauthenticated visitor away from a protected page to login', () => {
    render(
      <MemoryRouter initialEntries={['/environments']}>
        <App />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
  })
})
