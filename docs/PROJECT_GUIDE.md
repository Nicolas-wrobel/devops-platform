# DevOps Platform -- Project Guide

## Engineering mindset first

The goal of this project is **not only to build software**, but to build
the habits of a software engineer.

Before implementing a feature, always ask yourself:

-   What problem am I solving?
-   What are the possible solutions?
-   Why did I choose this one?
-   What are its advantages?
-   What are its limitations?
-   Would I make the same decision in one year?

Try to document important technical decisions in `docs/DECISIONS/000X-short-title.md`.
Even a few paragraphs explaining your reasoning are more valuable than undocumented code.

------------------------------------------------------------------------

# Project objective

Build a portfolio-grade Full Stack + DevOps application while becoming
autonomous again.

Goals:

-   Improve software engineering skills
-   Improve DevOps knowledge
-   Learn modern tooling
-   Understand every technical decision
-   Avoid blind copy/paste
-   Build a project that reflects professional practices

For the current stack, repository layout, and container architecture, see
[ARCHITECTURE.md](ARCHITECTURE.md). For day-to-day commands and how to verify
the stack works, see [DEVELOPMENT.md](DEVELOPMENT.md).

------------------------------------------------------------------------

# Business application

The infrastructure is **not** the final application.

The application will become a small DevOps administration platform.

Possible modules:

-   Applications
-   Environments
-   Deployments
-   Deployment history
-   Dashboard
-   Monitoring integration
-   Alerts

The infrastructure supports this application.

------------------------------------------------------------------------

# Long-term roadmap

1.  Technical foundation (done)
2.  First business module (done — `Environment` CRUD, see [ADRs 0005-0009](DECISIONS/))
3.  Database migrations (done — Flyway, adopted as part of step 2, see [ADR-0006](DECISIONS/0006-adopt-flyway-with-first-module.md))
4.  CI (done — GitHub Actions, see [ADR-0011](DECISIONS/0011-ci-with-github-actions.md))
5.  Container improvements
6.  Monitoring
7.  Kubernetes
8.  Helm
9.  Production-ready improvements

------------------------------------------------------------------------

# Success criteria before business development

The following questions should all have the answer "Yes":

-   Can someone clone and start the project easily?
-   Does Docker start correctly?
-   Does the frontend communicate with the backend?
-   Does the backend communicate with PostgreSQL?
-   Are environment variables managed properly?
-   Is the project documented?

If yes, the project is ready for business development.

------------------------------------------------------------------------

# Personal reminder

Always prioritize:

-   Understanding over speed
-   Simplicity over unnecessary complexity
-   Reproducibility over "works on my machine"
-   Documentation over memory
-   Small iterative improvements instead of large rewrites
