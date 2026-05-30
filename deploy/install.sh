#!/bin/bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/env.sh"

log "=== УСТАНОВКА Tomcat ${TOMCAT_VERSION} + Camunda ${CAMUNDA_VERSION} ==="

# --- 0. Проверки окружения ---
log "[0/6] Проверка окружения..."

command -v java   >/dev/null || fail "Java не найдена. Установите JDK 11+."
command -v curl   >/dev/null || fail "curl не найден."
command -v psql   >/dev/null || fail "psql не найден (нужен PostgreSQL client)."
command -v tar    >/dev/null || fail "tar не найден."

JAVA_VER=$(java -version 2>&1 | head -1 | awk -F'"' '{print $2}' | cut -d. -f1)
[ "${JAVA_VER}" -ge 11 ] || fail "Требуется JDK 11+, найдено: ${JAVA_VER}"

# Проверка доступности БД
PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" \
    -U "${DB_USER}" -d "${DB_NAME}" -c "SELECT 1;" >/dev/null \
    || fail "Не удаётся подключиться к БД ${DB_NAME}@${DB_HOST}:${DB_PORT} под ${DB_USER}"

log "    -> Java ${JAVA_VER}, БД доступна. OK."

# --- 1. Каталоги ---
log "[1/6] Подготовка каталогов в ${INSTALL_DIR}..."

if [ -d "${TOMCAT_DIR}" ]; then
    log "    -> ${TOMCAT_DIR} уже существует, делаю бэкап..."
    mkdir -p "${BACKUP_DIR}"
    BACKUP_NAME="tomcat-$(date +%Y%m%d-%H%M%S).tar.gz"
    tar -czf "${BACKUP_DIR}/${BACKUP_NAME}" -C "${INSTALL_DIR}" tomcat
    log "    -> Бэкап: ${BACKUP_DIR}/${BACKUP_NAME}"
    rm -rf "${TOMCAT_DIR}"
fi

sudo mkdir -p "${INSTALL_DIR}"
sudo chown -R "$(whoami)":"$(whoami)" "${INSTALL_DIR}"
mkdir -p "${DDL_DIR}" "${BACKUP_DIR}"
cd "${INSTALL_DIR}"

# --- 2. Tomcat ---
log "[2/6] Скачивание Tomcat ${TOMCAT_VERSION}..."
curl -fSL "${TOMCAT_URL}" -o tomcat.tar.gz
tar -xzf tomcat.tar.gz
mv "apache-tomcat-${TOMCAT_VERSION}" tomcat
rm tomcat.tar.gz
chmod +x "${TOMCAT_DIR}/bin/"*.sh

# --- 3. Camunda ---
log "[3/6] Скачивание Camunda ${CAMUNDA_VERSION}..."
curl -fSL "${CAMUNDA_URL}" -o camunda.tar.gz
mkdir -p camunda-dist
tar -xzf camunda.tar.gz -C camunda-dist
rm camunda.tar.gz

log "    -> Интеграция артефактов Camunda с Tomcat..."
CAMUNDA_TOMCAT=$(find camunda-dist/server -maxdepth 1 -type d -name "apache-tomcat-*" | head -1)
[ -d "${CAMUNDA_TOMCAT}" ] || fail "Не найден каталог Camunda Tomcat в дистрибутиве"

cp -r "${CAMUNDA_TOMCAT}/webapps/"* "${TOMCAT_DIR}/webapps/"
cp -r "${CAMUNDA_TOMCAT}/lib/"*     "${TOMCAT_DIR}/lib/"
cp -rn "${CAMUNDA_TOMCAT}/conf/"*   "${TOMCAT_DIR}/conf/"  # не перетираем server.xml

# SQL-скрипты Camunda сохраняем для повторного использования
if [ -d "${CAMUNDA_TOMCAT}/../sql/create" ]; then
    cp "${CAMUNDA_TOMCAT}/../sql/create/"postgres_*.sql "${DDL_DIR}/" 2>/dev/null || true
fi

rm -rf camunda-dist

# --- 4. JDBC-драйвер ---
log "[4/6] Установка JDBC-драйвера PostgreSQL ${POSTGRES_JDBC_VERSION}..."
curl -fSL "${POSTGRES_JDBC_URL}" -o "${TOMCAT_DIR}/lib/postgresql-${POSTGRES_JDBC_VERSION}.jar"

# --- 5. DDL Camunda в существующую схему ---
log "[5/6] Применение DDL Camunda в схему ${DB_SCHEMA}..."

# Проверка существующих ACT_* таблиц
EXISTING=$(PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" \
    -U "${DB_USER}" -d "${DB_NAME}" -tA -c \
    "SELECT COUNT(*) FROM information_schema.tables
     WHERE table_schema = '${DB_SCHEMA}' AND table_name ILIKE 'act\_%';")

if [ "${EXISTING}" -gt 0 ]; then
    log "    -> В схеме ${DB_SCHEMA} уже есть ${EXISTING} таблиц ACT_*. DDL пропущен."
    log "       Если нужна переустановка — удалите таблицы вручную."
else
    log "    -> Скачивание DDL-скриптов Camunda ${CAMUNDA_VERSION}..."
    for script in engine history identity; do
        curl -fSL "${CAMUNDA_DDL_BASE}/activiti.postgres.create.${script}.sql" \
             -o "${DDL_DIR}/camunda_${script}.sql"
    done

    log "    -> Применение DDL..."
    for script in engine history identity; do
        log "       - camunda_${script}.sql"
        psql_exec -f "${DDL_DIR}/camunda_${script}.sql" >/dev/null
    done
    log "    -> DDL Camunda успешно применён."
fi

# --- 6. Финал ---
log "[6/6] Установка завершена."
log ""
log "============================================"
log " Tomcat:  ${TOMCAT_DIR}"
log " Порты:   HTTP=${HTTP_PORT}, Shutdown=${SHUTDOWN_PORT}, AJP=${AJP_PORT}"
log " Дальше:  ./setup.sh   (настройка конфигов)"
log "          ./deploy.sh  (деплой приложения)"
log "          ./start.sh   (запуск)"
log "============================================"