import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { Button } from './ui/button'
import { ThemeToggle } from './ThemeToggle'

const LINKS = [
  { to: '/environments', label: 'Environments' },
  { to: '/applications', label: 'Applications' },
  { to: '/deployments', label: 'Deployments' },
]

export function Nav() {
  const { logout } = useAuth()
  const location = useLocation()

  return (
    <nav className="border-b border-border bg-card px-4 py-3">
      <div className="mx-auto flex max-w-3xl items-center justify-between">
        <div className="flex gap-4">
          {LINKS.map((link) => (
            <Link
              key={link.to}
              to={link.to}
              className={
                location.pathname === link.to
                  ? 'text-sm font-medium text-foreground'
                  : 'text-sm font-medium text-muted-foreground hover:text-foreground'
              }
            >
              {link.label}
            </Link>
          ))}
        </div>
        <div className="flex items-center gap-2">
          <ThemeToggle />
          <Button variant="ghost" size="sm" onClick={logout}>
            Log out
          </Button>
        </div>
      </div>
    </nav>
  )
}
