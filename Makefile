.PHONY: dev dev-up dev-down dev-logs dev-ps dev-build dev-restart \
        prod prod-up prod-down prod-logs prod-ps prod-build prod-restart

dev: dev-up
dev-up:
	./scripts/dc-dev.sh up --build -d

dev-down:
	./scripts/dc-dev.sh down

dev-logs:
	./scripts/dc-dev.sh logs -f --tail=200

dev-ps:
	./scripts/dc-dev.sh ps

dev-build:
	./scripts/dc-dev.sh build --no-cache

dev-restart:
	./scripts/dc-dev.sh up -d --build

prod: prod-up
prod-up:
	./scripts/dc-prod.sh up --build -d

prod-down:
	./scripts/dc-prod.sh down

prod-logs:
	./scripts/dc-prod.sh logs -f --tail=200

prod-ps:
	./scripts/dc-prod.sh ps

prod-build:
	./scripts/dc-prod.sh build --no-cache

prod-restart:
	./scripts/dc-prod.sh up -d --build