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
