#!/bin/bash
# =====================================================
# Общие параметры развёртывания
# Подключается во всех остальных скриптах: source env.sh
# =====================================================

# --- Каталоги ---
export INSTALL_DIR="/opt/camunda"
export TOMCAT_DIR="${INSTALL_DIR}/tomcat"
export BACKUP_DIR="${INSTALL_DIR}/backup"
export DDL_DIR="${INSTALL_DIR}/sql"

# --- Версии ---
export TOMCAT_VERSION="9.0.85"
export CAMUNDA_VERSION="7.20.0"
export POSTGRES_JDBC_VERSION="42.7.1"

# --- Порты (синхронизированы с docker-compose) ---
export HTTP_PORT=8080
export SHUTDOWN_PORT=8005
export AJP_PORT=8009

# --- БД (существующая схема приложения) ---
export DB_HOST="localhost"
export DB_PORT=5432
export DB_NAME="myapp"
export DB_SCHEMA="public"
export DB_USER="app_user"
export DB_PASSWORD="app_pass"

# --- Spring Boot приложение ---
export APP_NAME="myapp"                              # контекст: /myapp
export APP_WAR_SOURCE="./build/libs/myapp.war"       # путь к собранному WAR
export APP_CONTEXT_PATH="/${APP_NAME}"

# --- JVM ---
export JVM_XMS="512m"
export JVM_XMX="2048m"
export JVM_METASPACE="512m"
export TIMEZONE="Europe/Moscow"

# --- URL-ы ресурсов ---
export TOMCAT_URL="https://archive.apache.org/dist/tomcat/tomcat-9/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz"
export CAMUNDA_URL="https://downloads.camunda.cloud/release/camunda-bpm/tomcat/7.20/camunda-bpm-tomcat-${CAMUNDA_VERSION}.tar.gz"
export POSTGRES_JDBC_URL="https://jdbc.postgresql.org/download/postgresql-${POSTGRES_JDBC_VERSION}.jar"
export CAMUNDA_DDL_BASE="https://raw.githubusercontent.com/camunda/camunda-bpm-platform/${CAMUNDA_VERSION}/engine/src/main/resources/org/camunda/bpm/engine/db/create"

# --- Утилиты ---
log()  { echo "[$(date +'%Y-%m-%d %H:%M:%S')] $*"; }
fail() { echo "[ERROR] $*" >&2; exit 1; }

psql_exec() {
    PGPASSWORD="${DB_PASSWORD}" psql \
        -h "${DB_HOST}" -p "${DB_PORT}" \
        -U "${DB_USER}" -d "${DB_NAME}" \
        -v ON_ERROR_STOP=1 "$@"
}