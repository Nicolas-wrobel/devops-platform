import { Moon, Sun } from 'lucide-react'
import { useState } from 'react'
import { Button } from './ui/button'
import { setTheme } from '../lib/theme'

export function ThemeToggle() {
  const [isDark, setIsDark] = useState(() => document.documentElement.classList.contains('dark'))

  function toggle() {
    const next = isDark ? 'light' : 'dark'
    setTheme(next)
    setIsDark(next === 'dark')
  }

  return (
    <Button variant="ghost" size="icon" aria-label="Toggle theme" onClick={toggle}>
      {isDark ? <Sun className="size-4" /> : <Moon className="size-4" />}
    </Button>
  )
}
