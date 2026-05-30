# =============================================================
# Управление развёртыванием Camunda + Spring Boot на Helios
# Запускается из каталога deploy/
# =============================================================

SHELL := /bin/bash
.DEFAULT_GOAL := help

# --- Пути ---
# Каталог, где лежит сам Makefile (deploy/)
MAKEFILE_DIR := $(abspath $(dir $(lastword $(MAKEFILE_LIST))))
# Корень проекта (на уровень выше)
PROJECT_ROOT := $(abspath $(MAKEFILE_DIR)/..)

# --- Загрузка локальных параметров ---
ifneq (,$(wildcard $(MAKEFILE_DIR)/.env.local))
    include $(MAKEFILE_DIR)/.env.local
    export
endif

# --- Значения по умолчанию ---
SSH_PORT        ?= 22
SSH_KEY         ?= ~/.ssh/id_rsa
REMOTE_WORK_DIR ?= /home/$(SSH_USER)/camunda-deploy
LOCAL_WAR       ?= build/libs/myapp.war

# Абсолютный путь к WAR (LOCAL_WAR указывается относительно корня проекта)
LOCAL_WAR_ABS := $(abspath $(PROJECT_ROOT)/$(LOCAL_WAR))

# --- SSH-команды ---
SSH_OPTS := -p $(SSH_PORT) -i $(SSH_KEY) -o StrictHostKeyChecking=accept-new
SSH      := ssh $(SSH_OPTS) $(SSH_USER)@$(SSH_HOST)
SCP      := scp $(SSH_OPTS)
RSYNC    := rsync -az -e "ssh $(SSH_OPTS)"

# Выполнить скрипт на удалённом сервере
define remote_exec
	@echo "→ Удалённо: $(1)"
	$(SSH) "cd $(REMOTE_WORK_DIR) && $(1)"
endef

# --- Цвета ---
GREEN  := \033[0;32m
YELLOW := \033[0;33m
RED    := \033[0;31m
NC     := \033[0m

# =============================================================
# СПРАВКА И ПРОВЕРКИ
# =============================================================

.PHONY: help
help: ## Показать справку
	@echo ""
	@echo "$(GREEN)Управление развёртыванием Camunda + Spring Boot$(NC)"
	@echo ""
	@echo "Каталог проекта: $(YELLOW)$(PROJECT_ROOT)$(NC)"
	@echo "Сервер:          $(YELLOW)$(SSH_USER)@$(SSH_HOST):$(SSH_PORT)$(NC)"
	@echo "Удалённый путь:  $(YELLOW)$(REMOTE_WORK_DIR)$(NC)"
	@echo ""
	@echo "Цели:"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  $(GREEN)%-20s$(NC) %s\n", $$1, $$2}' $(MAKEFILE_LIST)
	@echo ""

.PHONY: check-env
check-env: ## Проверить наличие .env.local и обязательных параметров
	@if [ ! -f $(MAKEFILE_DIR)/.env.local ]; then \
		echo "$(RED)ERROR: файл $(MAKEFILE_DIR)/.env.local не найден.$(NC)"; \
		echo "Создайте его: cp .env.local.example .env.local"; \
		exit 1; \
	fi
	@if [ -z "$(SSH_HOST)" ] || [ -z "$(SSH_USER)" ]; then \
		echo "$(RED)ERROR: SSH_HOST и SSH_USER должны быть заданы в .env.local$(NC)"; \
		exit 1; \
	fi
	@echo "$(GREEN)✓ Конфигурация загружена$(NC)"

.PHONY: ssh-test
ssh-test: check-env ## Проверить SSH-подключение к серверу
	@echo "→ Проверка SSH-подключения..."
	@$(SSH) "echo '$(GREEN)✓ SSH работает$(NC)'; uname -a; java -version 2>&1 | head -1"

.PHONY: ssh
ssh: check-env ## Открыть интерактивную SSH-сессию
	@$(SSH)

.PHONY: print-config
print-config: ## Показать текущую конфигурацию
	@echo "MAKEFILE_DIR    = $(MAKEFILE_DIR)"
	@echo "PROJECT_ROOT    = $(PROJECT_ROOT)"
	@echo "SSH_HOST        = $(SSH_HOST)"
	@echo "SSH_USER        = $(SSH_USER)"
	@echo "SSH_PORT        = $(SSH_PORT)"
	@echo "SSH_KEY         = $(SSH_KEY)"
	@echo "REMOTE_WORK_DIR = $(REMOTE_WORK_DIR)"
	@echo "LOCAL_WAR       = $(LOCAL_WAR)"
	@echo "LOCAL_WAR_ABS   = $(LOCAL_WAR_ABS)"

# =============================================================
# СИНХРОНИЗАЦИЯ СКРИПТОВ
# =============================================================

.PHONY: push-scripts
push-scripts: check-env ## Загрузить скрипты на сервер (из текущей папки)
	@echo "→ Создание удалённого каталога $(REMOTE_WORK_DIR)..."
	@$(SSH) "mkdir -p $(REMOTE_WORK_DIR)"
	@echo "→ Синхронизация $(MAKEFILE_DIR)/ → $(REMOTE_WORK_DIR)/"
	@$(RSYNC) --delete \
		--exclude='.env.local' \
		--exclude='.env.local.example' \
		--exclude='Makefile' \
		--exclude='*.bak-*' \
		--exclude='sql/' \
		$(MAKEFILE_DIR)/ $(SSH_USER)@$(SSH_HOST):$(REMOTE_WORK_DIR)/
	@$(SSH) "chmod +x $(REMOTE_WORK_DIR)/*.sh"
	@echo "$(GREEN)✓ Скрипты загружены$(NC)"

.PHONY: pull-logs
pull-logs: check-env ## Скачать catalina.out в logs/ (в корне проекта)
	@mkdir -p $(PROJECT_ROOT)/logs
	@echo "→ Скачивание catalina.out..."
	@$(SCP) $(SSH_USER)@$(SSH_HOST):/opt/camunda/tomcat/logs/catalina.out \
		$(PROJECT_ROOT)/logs/catalina-$$(date +%Y%m%d-%H%M%S).out
	@echo "$(GREEN)✓ Сохранено в $(PROJECT_ROOT)/logs/$(NC)"

# =============================================================
# УСТАНОВКА И НАСТРОЙКА
# =============================================================

.PHONY: install
install: push-scripts ## Установить Tomcat + Camunda (один раз)
	@echo "$(YELLOW)→ Запуск install.sh на сервере...$(NC)"
	$(call remote_exec,./install.sh)
	@echo "$(GREEN)✓ Установка завершена$(NC)"

.PHONY: setup
setup: push-scripts ## Настроить конфиги (server.xml, setenv.sh)
	@echo "$(YELLOW)→ Запуск setup.sh на сервере...$(NC)"
	$(call remote_exec,./setup.sh)
	@echo "$(GREEN)✓ Конфиги настроены$(NC)"

.PHONY: full-install
full-install: install setup ## Полная первичная установка: install + setup
	@echo ""
	@echo "$(GREEN)═══════════════════════════════════════════$(NC)"
	@echo "$(GREEN) Установка завершена. Дальше:$(NC)"
	@echo "   make build      # собрать WAR"
	@echo "   make deploy     # задеплоить"
	@echo "$(GREEN)═══════════════════════════════════════════$(NC)"

# =============================================================
# СБОРКА И ДЕПЛОЙ
# =============================================================

.PHONY: build
build: ## Собрать WAR-файл локально (в корне проекта)
	@echo "→ Сборка приложения в $(PROJECT_ROOT)..."
	@if [ -f $(PROJECT_ROOT)/gradlew ]; then \
		cd $(PROJECT_ROOT) && ./gradlew clean bootWar; \
	elif [ -f $(PROJECT_ROOT)/pom.xml ]; then \
		cd $(PROJECT_ROOT) && mvn clean package -DskipTests; \
	else \
		echo "$(RED)ERROR: не найден gradlew/pom.xml в $(PROJECT_ROOT)$(NC)"; exit 1; \
	fi
	@[ -f "$(LOCAL_WAR_ABS)" ] || (echo "$(RED)ERROR: $(LOCAL_WAR_ABS) не создан$(NC)"; exit 1)
	@echo "$(GREEN)✓ WAR собран: $(LOCAL_WAR_ABS) ($$(du -h $(LOCAL_WAR_ABS) | cut -f1))$(NC)"

.PHONY: push-war
push-war: check-env ## Загрузить WAR на сервер
	@[ -f "$(LOCAL_WAR_ABS)" ] || (echo "$(RED)ERROR: $(LOCAL_WAR_ABS) не найден. Запустите make build$(NC)"; exit 1)
	@echo "→ Загрузка $(LOCAL_WAR_ABS) на сервер..."
	@$(SSH) "mkdir -p $(REMOTE_WORK_DIR)/artifacts"
	@$(RSYNC) $(LOCAL_WAR_ABS) $(SSH_USER)@$(SSH_HOST):$(REMOTE_WORK_DIR)/artifacts/
	@echo "$(GREEN)✓ WAR загружен$(NC)"

.PHONY: deploy
deploy: push-scripts push-war ## Полный деплой: скрипты + WAR + запуск
	@echo "$(YELLOW)→ Запуск deploy.sh на сервере...$(NC)"
	@WAR_NAME=$$(basename $(LOCAL_WAR_ABS)); \
	$(SSH) "cd $(REMOTE_WORK_DIR) && ./deploy.sh $(REMOTE_WORK_DIR)/artifacts/$$WAR_NAME"
	@echo "$(GREEN)✓ Деплой завершён$(NC)"

.PHONY: redeploy
redeploy: build deploy ## Пересобрать и задеплоить

# =============================================================
# УПРАВЛЕНИЕ TOMCAT
# =============================================================

.PHONY: start
start: check-env ## Запустить Tomcat
	$(call remote_exec,./start.sh)

.PHONY: stop
stop: check-env ## Остановить Tomcat
	$(call remote_exec,./stop.sh)

.PHONY: restart
restart: stop start ## Перезапустить Tomcat

.PHONY: status
status: check-env ## Статус Tomcat и сервисов
	@echo "→ Статус процесса Tomcat:"
	@$(SSH) "pgrep -af 'catalina.*tomcat' || echo '  Tomcat НЕ запущен'"
	@echo ""
	@echo "→ Проверка endpoint-ов:"
	@$(SSH) 'set -e; source $(REMOTE_WORK_DIR)/env.sh; \
		echo "  Camunda:     $$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$$HTTP_PORT/camunda/app/welcome/default/ || echo NO)"; \
		echo "  Приложение:  $$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$$HTTP_PORT$$APP_CONTEXT_PATH/actuator/health || echo NO)"'

.PHONY: logs
logs: check-env ## Последние 100 строк catalina.out
	@$(SSH) "tail -100 /opt/camunda/tomcat/logs/catalina.out"

.PHONY: logs-follow
logs-follow: check-env ## tail -f catalina.out (Ctrl+C для выхода)
	@$(SSH) -t "tail -f /opt/camunda/tomcat/logs/catalina.out"

.PHONY: logs-error
logs-error: check-env ## Только ошибки из catalina.out
	@$(SSH) "grep -E 'ERROR|SEVERE|Exception' /opt/camunda/tomcat/logs/catalina.out | tail -50"

# =============================================================
# БД И ДИАГНОСТИКА
# =============================================================

.PHONY: db-check
db-check: check-env ## Проверить подключение к БД и наличие таблиц Camunda
	@$(SSH) 'source $(REMOTE_WORK_DIR)/env.sh && \
		PGPASSWORD=$$DB_PASSWORD psql -h $$DB_HOST -p $$DB_PORT -U $$DB_USER -d $$DB_NAME -c \
		"SELECT COUNT(*) AS camunda_tables FROM information_schema.tables WHERE table_schema = '"'"'$$DB_SCHEMA'"'"' AND table_name ILIKE '"'"'act\_%'"'"';"'

.PHONY: db-tables
db-tables: check-env ## Список ACT_*-таблиц на сервере
	@$(SSH) 'source $(REMOTE_WORK_DIR)/env.sh && \
		PGPASSWORD=$$DB_PASSWORD psql -h $$DB_HOST -p $$DB_PORT -U $$DB_USER -d $$DB_NAME -c \
		"SELECT table_name FROM information_schema.tables WHERE table_schema = '"'"'$$DB_SCHEMA'"'"' AND table_name ILIKE '"'"'act\_%'"'"' ORDER BY 1;"'

# =============================================================
# ОЧИСТКА
# =============================================================

.PHONY: clean-local
clean-local: ## Удалить локальные артефакты сборки
	@if [ -f $(PROJECT_ROOT)/gradlew ]; then cd $(PROJECT_ROOT) && ./gradlew clean; fi
	@if [ -f $(PROJECT_ROOT)/pom.xml ]; then cd $(PROJECT_ROOT) && mvn clean; fi
	@rm -rf $(PROJECT_ROOT)/logs/
	@echo "$(GREEN)✓ Локальные артефакты удалены$(NC)"