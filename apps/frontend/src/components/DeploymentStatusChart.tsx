import { Bar, BarChart, CartesianGrid, Cell, XAxis } from 'recharts'
import { ChartContainer, ChartTooltip, ChartTooltipContent } from './ui/chart'
import { deploymentStatusChartConfig, toStatusChartData } from '../lib/deploymentStatusChart'
import { useIsDarkMode } from '../lib/theme'
import type { DeploymentStatus } from '../lib/types'

export function DeploymentStatusChart({ counts }: { counts: Record<DeploymentStatus, number> }) {
  const isDarkMode = useIsDarkMode()
  const statusChartData = toStatusChartData(counts)

  return (
    // Recharts doesn't repaint SVG fill="var(--token)" when the CSS
    // variable's value changes without a resize — forcing a remount on
    // theme change is the reliable fix.
    <ChartContainer
      key={isDarkMode ? 'dark' : 'light'}
      config={deploymentStatusChartConfig}
      className="aspect-auto h-48 w-full"
    >
      <BarChart data={statusChartData}>
        <CartesianGrid vertical={false} />
        <XAxis dataKey="status" tickLine={false} axisLine={false} tickMargin={8} />
        <ChartTooltip content={<ChartTooltipContent hideLabel />} />
        <Bar dataKey="count" radius={4}>
          {statusChartData.map((entry) => (
            <Cell key={entry.status} fill={entry.fill} />
          ))}
        </Bar>
      </BarChart>
    </ChartContainer>
  )
}
