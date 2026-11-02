import { Badge } from './ui/badge'
import type { DeploymentStatus } from '../lib/types'

const VARIANT_BY_STATUS: Record<DeploymentStatus, 'secondary' | 'default' | 'outline' | 'destructive'> = {
  PENDING: 'secondary',
  RUNNING: 'default',
  SUCCESS: 'outline',
  FAILED: 'destructive',
}

const CLASS_BY_STATUS: Record<DeploymentStatus, string> = {
  PENDING: '',
  RUNNING: 'bg-blue-600 text-white animate-pulse',
  SUCCESS: 'border-green-600 text-green-700 dark:text-green-400',
  FAILED: '',
}

export function DeploymentStatusBadge({ status }: { status: DeploymentStatus }) {
  return (
    <Badge variant={VARIANT_BY_STATUS[status]} className={CLASS_BY_STATUS[status]}>
      {status}
    </Badge>
  )
}
