import { BookOpen, Code2, ExternalLink } from 'lucide-react'
import { TECH_STACK, REPO_URL } from '../lib/projectInfo'
import { Badge } from './ui/badge'

export function AboutProject({ showIntro = true }: { showIntro?: boolean }) {
  return (
    <>
      {showIntro && (
        <div className="mt-16">
          <h2 className="text-lg font-semibold text-foreground">About this project</h2>
          <p className="mt-3 max-w-xl text-sm text-muted-foreground">
            A simulated internal deployment-tracking platform — a demo project built to showcase
            full-stack and DevOps engineering practices, from CI/CD and containerization to
            observability and security.
          </p>
        </div>
      )}

      <div className={showIntro ? 'mt-8' : 'mt-16'}>
        <h3 className="text-sm font-medium text-muted-foreground">Built with</h3>
        <div className="mt-3 flex flex-wrap gap-2">
          {TECH_STACK.map((tech) => (
            <Badge key={tech} variant="secondary">
              {tech}
            </Badge>
          ))}
        </div>
      </div>

      <div className="mt-8 flex flex-wrap gap-6">
        <a
          href={REPO_URL}
          target="_blank"
          rel="noreferrer"
          className="flex items-center gap-1.5 text-sm font-medium text-foreground hover:text-primary"
        >
          <Code2 className="size-4" />
          Source code
          <ExternalLink className="size-3.5 text-muted-foreground" />
        </a>
        <a
          href={`${REPO_URL}/blob/main/docs/ARCHITECTURE.md`}
          target="_blank"
          rel="noreferrer"
          className="flex items-center gap-1.5 text-sm font-medium text-foreground hover:text-primary"
        >
          <BookOpen className="size-4" />
          Architecture docs
          <ExternalLink className="size-3.5 text-muted-foreground" />
        </a>
      </div>
    </>
  )
}
