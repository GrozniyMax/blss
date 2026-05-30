#!/bin/bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/env.sh"

log "=== НАСТРОЙКА КОНФИГОВ Tomcat + Camunda ==="

[ -d "${TOMCAT_DIR}" ] || fail "Tomcat не найден. Сначала запустите install.sh"

SERVER_XML="${TOMCAT_DIR}/conf/server.xml"
BPM_PLATFORM_XML="${TOMCAT_DIR}/conf/bpm-platform.xml"

# --- 1. Бэкап существующих конфигов ---
log "[1/4] Бэкап текущих конфигов..."
TS=$(date +%Y%m%d-%H%M%S)
cp "${SERVER_XML}" "${SERVER_XML}.bak-${TS}"
[ -f "${BPM_PLATFORM_XML}" ] && cp "${BPM_PLATFORM_XML}" "${BPM_PLATFORM_XML}.bak-${TS}"

# --- 2. Настройка портов в server.xml ---
log "[2/4] Настройка портов в server.xml..."

# Используем Python для корректного редактирования XML (sed на XML опасен)
python3 - <<PYEOF
import re
from pathlib import Path

path = Path("${SERVER_XML}")
content = path.read_text(encoding="utf-8")

# Shutdown
content = re.sub(
    r'(<Server\s+port=")\d+(")',
    r'\g<1>${SHUTDOWN_PORT}\g<2>',
    content, count=1
)

# HTTP Connector
content = re.sub(
    r'(<Connector\s+port=")\d+("\s+protocol="HTTP/1\.1")',
    r'\g<1>${HTTP_PORT}\g<2>',
    content, count=1
)

# AJP Connector
content = re.sub(
    r'(<Connector\s+protocol="AJP/1\.3"\s+port=")\d+(")',
    r'\g<1>${AJP_PORT}\g<2>',
    content, count=1
)
# Альтернативный порядок атрибутов
content = re.sub(
    r'(<Connector\s+port=")\d+("\s+protocol="AJP/1\.3")',
    r'\g<1>${AJP_PORT}\g<2>',
    content, count=1
)

path.write_text(content, encoding="utf-8")
print("    -> server.xml: порты обновлены")
PYEOF

# --- 3. Настройка DataSource Camunda ---
log "[3/4] Настройка DataSource ProcessEngine..."

python3 - <<PYEOF
import re
from pathlib import Path

path = Path("${SERVER_XML}")
content = path.read_text(encoding="utf-8")

new_resource = '''<Resource name="jdbc/ProcessEngine"
              auth="Container"
              type="javax.sql.DataSource"
              factory="org.apache.tomcat.jdbc.pool.DataSourceFactory"
              uniqueResourceName="process-engine"
              driverClassName="org.postgresql.Driver"
              url="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}"
              username="${DB_USER}"
              password="${DB_PASSWORD}"
              maxActive="20"
              maxIdle="5"
              minIdle="2"
              maxWait="10000"
              testOnBorrow="true"
              validationQuery="SELECT 1"
              defaultAutoCommit="false" />'''

# Заменяем существующий Resource jdbc/ProcessEngine
pattern = r'<Resource\s+name="jdbc/ProcessEngine".*?/>'
if re.search(pattern, content, flags=re.DOTALL):
    content = re.sub(pattern, new_resource, content, flags=re.DOTALL)
    print("    -> Resource jdbc/ProcessEngine обновлён")
else:
    # Вставляем в <GlobalNamingResources>
    content = re.sub(
        r'(<GlobalNamingResources>)',
        r'\1\n    ' + new_resource,
        content, count=1
    )
    print("    -> Resource jdbc/ProcessEngine добавлен")

path.write_text(content, encoding="utf-8")
PYEOF

# --- 4. setenv.sh (JVM) ---
log "[4/4] Создание setenv.sh..."

cat > "${TOMCAT_DIR}/bin/setenv.sh" <<EOF
#!/bin/bash
# Сгенерировано setup.sh

export CATALINA_OPTS="\${CATALINA_OPTS} \\
    -Xms${JVM_XMS} \\
    -Xmx${JVM_XMX} \\
    -XX:MaxMetaspaceSize=${JVM_METASPACE} \\
    -XX:+HeapDumpOnOutOfMemoryError \\
    -XX:HeapDumpPath=${TOMCAT_DIR}/logs \\
    -Dfile.encoding=UTF-8 \\
    -Duser.timezone=${TIMEZONE} \\
    -Djava.awt.headless=true"

# Параметры приложения (можно переопределять)
export CATALINA_OPTS="\${CATALINA_OPTS} \\
    -Dspring.profiles.active=prod \\
    -Dapp.db.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME} \\
    -Dapp.db.username=${DB_USER} \\
    -Dapp.db.password=${DB_PASSWORD}"
EOF

chmod +x "${TOMCAT_DIR}/bin/setenv.sh"

# Отключаем schema-update Camunda — DDL мы уже применили в install.sh
if [ -f "${BPM_PLATFORM_XML}" ]; then
    log "    -> Отключение schema-update в bpm-platform.xml..."
    python3 - <<PYEOF
import re
from pathlib import Path
path = Path("${BPM_PLATFORM_XML}")
content = path.read_text(encoding="utf-8")

# Ищем property databaseSchemaUpdate, если есть — выставляем false,
# если нет — добавляем
if 'name="databaseSchemaUpdate"' in content:
    content = re.sub(
        r'(<property\s+name="databaseSchemaUpdate">)[^<]*(</property>)',
        r'\g<1>false\g<2>',
        content
    )
else:
    content = re.sub(
        r'(<properties>)',
        r'\1\n      <property name="databaseSchemaUpdate">false</property>',
        content, count=1
    )

path.write_text(content, encoding="utf-8")
print("    -> bpm-platform.xml: databaseSchemaUpdate=false")
PYEOF
fi

log ""
log "============================================"
log " Настройка завершена."
log " server.xml:        ${SERVER_XML}"
log " setenv.sh:         ${TOMCAT_DIR}/bin/setenv.sh"
log " Бэкап:             *.bak-${TS}"
log "============================================"