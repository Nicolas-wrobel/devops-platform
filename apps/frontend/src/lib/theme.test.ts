import { afterEach, describe, expect, it } from 'vitest'
import { applyTheme, getStoredTheme, setTheme } from './theme'

describe('theme', () => {
  afterEach(() => {
    localStorage.clear()
    document.documentElement.classList.remove('dark')
  })

  it('getStoredTheme returns null when nothing is stored', () => {
    expect(getStoredTheme()).toBeNull()
  })

  it('setTheme persists the value and applies it to the document', () => {
    setTheme('dark')

    expect(getStoredTheme()).toBe('dark')
    expect(document.documentElement.classList.contains('dark')).toBe(true)
  })

  it('applyTheme("light") removes the dark class', () => {
    document.documentElement.classList.add('dark')

    applyTheme('light')

    expect(document.documentElement.classList.contains('dark')).toBe(false)
  })
})
