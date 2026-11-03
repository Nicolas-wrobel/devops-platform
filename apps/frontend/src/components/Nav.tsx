import { AppWindow, LayoutDashboard, Rocket, Server } from 'lucide-react'
import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { Button } from './ui/button'
import { ThemeToggle } from './ThemeToggle'

const LINKS = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/environments', label: 'Environments', icon: Server },
  { to: '/applications', label: 'Applications', icon: AppWindow },
  { to: '/deployments', label: 'Deployments', icon: Rocket },
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
                  ? 'flex items-center gap-1.5 text-sm font-medium text-foreground'
                  : 'flex items-center gap-1.5 text-sm font-medium text-muted-foreground hover:text-foreground'
              }
            >
              <link.icon className="size-4" />
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
