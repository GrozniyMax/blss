FROM gradle:9.3.0-jdk17 AS builder
WORKDIR /workspace
COPY . .
RUN gradle :user-service:bootWar --no-daemon -q

FROM quay.io/wildfly/wildfly:37.0.0.Final-jdk17

ENV SECURITY_USERS_XML_PATH=/opt/jboss/wildfly/standalone/data/users.xml

COPY --from=builder /workspace/user-service/build/libs/user-service.war /opt/jboss/wildfly/standalone/deployments/ROOT.war
COPY --chown=jboss:jboss user-service/users.xml /opt/jboss/wildfly/standalone/data/users.xml

EXPOSE 8080 9990

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]
