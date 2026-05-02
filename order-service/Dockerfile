FROM gradle:9.3.0-jdk17 AS builder
WORKDIR /workspace
COPY . .
RUN gradle :order-service:bootWar --no-daemon -q

FROM quay.io/wildfly/wildfly:37.0.0.Final-jdk17

COPY --from=builder /workspace/order-service/build/libs/order-service.war /opt/jboss/wildfly/standalone/deployments/ROOT.war

EXPOSE 8080 9990

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]
