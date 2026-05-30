#!/bin/bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/env.sh"

WAR_FILE="${1:-${APP_WAR_SOURCE}}"

log "=== ДЕПЛОЙ ${APP_NAME} ==="
log "Источник: ${WAR_FILE}"

[ -f "${WAR_FILE}" ]   || fail "WAR-файл не найден: ${WAR_FILE}"
[ -d "${TOMCAT_DIR}" ] || fail "Tomcat не установлен. Сначала install.sh + setup.sh"

WEBAPPS="${TOMCAT_DIR}/webapps"
TARGET_WAR="${WEBAPPS}/${APP_NAME}.war"
TARGET_DIR="${WEBAPPS}/${APP_NAME}"

# --- 1. Остановка Tomcat ---
log "[1/5] Остановка Tomcat..."
if pgrep -f "catalina.*${TOMCAT_DIR}" >/dev/null; then
    "${TOMCAT_DIR}/bin/shutdown.sh" 30 -force || true
    # Ждём завершения процесса
    for i in $(seq 1 30); do
        pgrep -f "catalina.*${TOMCAT_DIR}" >/dev/null || break
        sleep 1
    done
    if pgrep -f "catalina.*${TOMCAT_DIR}" >/dev/null; then
        log "    -> Tomcat не завершился, отправляю SIGKILL..."
        pkill -9 -f "catalina.*${TOMCAT_DIR}" || true
        sleep 2
    fi
    log "    -> Tomcat остановлен."
else
    log "    -> Tomcat не запущен."
fi

# --- 2. Бэкап предыдущей версии ---
log "[2/5] Бэкап предыдущей версии..."
mkdir -p "${BACKUP_DIR}"
TS=$(date +%Y%m%d-%H%M%S)

if [ -f "${TARGET_WAR}" ]; then
    cp "${TARGET_WAR}" "${BACKUP_DIR}/${APP_NAME}-${TS}.war"
    log "    -> Бэкап: ${BACKUP_DIR}/${APP_NAME}-${TS}.war"
fi

# Чистим распакованный каталог и work — обязательно для чистого передеплоя
rm -rf "${TARGET_DIR}"
rm -rf "${TOMCAT_DIR}/work/Catalina/localhost/${APP_NAME}"

# Ограничим количество бэкапов (последние 10)
ls -1t "${BACKUP_DIR}/${APP_NAME}-"*.war 2>/dev/null | tail -n +11 | xargs -r rm --

# --- 3. Копирование нового WAR ---
log "[3/5] Установка нового WAR..."
cp "${WAR_FILE}" "${TARGET_WAR}"
log "    -> ${TARGET_WAR} ($(du -h "${TARGET_WAR}" | cut -f1))"

# --- 4. Запуск Tomcat ---
log "[4/5] Запуск Tomcat..."
"${TOMCAT_DIR}/bin/startup.sh"

# --- 5. Health check ---
log "[5/5] Ожидание готовности приложения..."

HEALTH_URL="http://localhost:${HTTP_PORT}${APP_CONTEXT_PATH}/actuator/health"
CAMUNDA_URL="http://localhost:${HTTP_PORT}/camunda/app/welcome/default/"
MAX_WAIT=120
ELAPSED=0
APP_READY=0
CAMUNDA_READY=0

while [ ${ELAPSED} -lt ${MAX_WAIT} ]; do
    if [ ${APP_READY} -eq 0 ]; then
        if curl -sf -o /dev/null -m 2 "${HEALTH_URL}"; then
            log "    -> Spring Boot приложение готово (${ELAPSED}s)"
            APP_READY=1
        fi
    fi
    if [ ${CAMUNDA_READY} -eq 0 ]; then
        if curl -sf -o /dev/null -m 2 "${CAMUNDA_URL}"; then
            log "    -> Camunda готова (${ELAPSED}s)"
            CAMUNDA_READY=1
        fi
    fi
    [ ${APP_READY} -eq 1 ] && [ ${CAMUNDA_READY} -eq 1 ] && break
    sleep 3
    ELAPSED=$((ELAPSED + 3))
done

log ""
log "============================================"
if [ ${APP_READY} -eq 1 ] && [ ${CAMUNDA_READY} -eq 1 ]; then
    log " ✓ Деплой успешен"
else
    log " ⚠ Деплой выполнен, но не все сервисы откликаются:"
    [ ${APP_READY} -eq 0 ]     && log "   - приложение НЕ отвечает на ${HEALTH_URL}"
    [ ${CAMUNDA_READY} -eq 0 ] && log "   - Camunda НЕ отвечает на ${CAMUNDA_URL}"
    log "   Проверьте: tail -f ${TOMCAT_DIR}/logs/catalina.out"
fi
log ""
log " Приложение: http://$(hostname -f):${HTTP_PORT}${APP_CONTEXT_PATH}"
log " Camunda:    http://$(hostname -f):${HTTP_PORT}/camunda"
log " Логи:       ${TOMCAT_DIR}/logs/catalina.out"
log "============================================"