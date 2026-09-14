import { Link } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

export function Nav() {
  const { logout } = useAuth()

  return (
    <nav className="border-b border-gray-200 bg-white px-4 py-3">
      <div className="mx-auto flex max-w-3xl items-center justify-between">
        <div className="flex gap-4">
          <Link to="/environments" className="text-sm font-medium text-gray-700 hover:text-blue-600">
            Environments
          </Link>
          <Link to="/applications" className="text-sm font-medium text-gray-700 hover:text-blue-600">
            Applications
          </Link>
        </div>
        <button onClick={logout} className="text-sm text-gray-500 hover:text-gray-700">
          Log out
        </button>
      </div>
    </nav>
  )
}
