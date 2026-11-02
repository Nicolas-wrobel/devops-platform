import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { DeploymentStatusBadge } from './DeploymentStatusBadge'
import type { DeploymentStatus } from '../lib/types'

describe('DeploymentStatusBadge', () => {
  it.each<DeploymentStatus>(['PENDING', 'RUNNING', 'SUCCESS', 'FAILED'])(
    'renders the %s status text',
    (status) => {
      render(<DeploymentStatusBadge status={status} />)
      expect(screen.getByText(status)).toBeInTheDocument()
    },
  )
})
