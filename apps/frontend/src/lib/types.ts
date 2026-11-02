export type EnvironmentType = 'DEVELOPMENT' | 'STAGING' | 'PRODUCTION'

export interface EnvironmentResponse {
  id: number
  name: string
  type: EnvironmentType
  description: string | null
  createdAt: string
  updatedAt: string
}

export interface ApplicationResponse {
  id: number
  name: string
  description: string | null
  createdAt: string
  updatedAt: string
}

export type DeploymentStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED'

export interface DeploymentResponse {
  id: number
  applicationId: number
  applicationName: string
  environmentId: number
  environmentName: string
  version: string
  status: DeploymentStatus
  triggeredBy: string
  createdAt: string
  startedAt: string | null
  plannedFinishAt: string | null
  finishedAt: string | null
}

export interface StatsResponse {
  applicationCount: number
  environmentCount: number
  deploymentCount: number
  successRatePercent: number | null
}
