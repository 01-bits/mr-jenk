DC = docker compose
SERVICES = --remove-orphans
SET_SOCKET_FILE= scripts/get-socket.sh

RUN = . $(SET_SOCKET_FILE) && $(DC)

.PHONY: up down restart build logs ps clean

up:
	$(RUN) up -d $(SERVICES)

down:
	$(RUN) down $(SERVICES)

build:
	$(RUN) up -d --build $(SERVICES)

restart: down up

logs:
	$(RUN) logs -f

ps:
	$(RUN) ps

clean:
	$(RUN) down -v --rmi local $(SERVICES)