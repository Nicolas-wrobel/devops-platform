import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { RequireAuth } from './auth/RequireAuth'
import { ApplicationsPage } from './pages/ApplicationsPage'
import { DeploymentsPage } from './pages/DeploymentsPage'
import { EnvironmentsPage } from './pages/EnvironmentsPage'
import { LandingPage } from './pages/LandingPage'
import { LoginPage } from './pages/LoginPage'

function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/environments"
          element={
            <RequireAuth>
              <EnvironmentsPage />
            </RequireAuth>
          }
        />
        <Route
          path="/applications"
          element={
            <RequireAuth>
              <ApplicationsPage />
            </RequireAuth>
          }
        />
        <Route
          path="/deployments"
          element={
            <RequireAuth>
              <DeploymentsPage />
            </RequireAuth>
          }
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AuthProvider>
  )
}

export default App
