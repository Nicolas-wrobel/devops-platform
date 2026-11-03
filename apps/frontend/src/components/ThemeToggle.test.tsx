import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it } from 'vitest'
import { ThemeToggle } from './ThemeToggle'

describe('ThemeToggle', () => {
  afterEach(() => {
    localStorage.clear()
    document.documentElement.classList.remove('dark')
  })

  it('toggles the dark class on the document when clicked', async () => {
    render(<ThemeToggle />)
    const button = screen.getByRole('button', { name: 'Toggle theme' })

    await userEvent.click(button)
    expect(document.documentElement.classList.contains('dark')).toBe(true)

    await userEvent.click(button)
    expect(document.documentElement.classList.contains('dark')).toBe(false)
  })
})
