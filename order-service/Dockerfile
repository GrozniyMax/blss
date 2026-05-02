FROM quay.io/wildfly/wildfly:37.0.0.Final-jdk17

COPY order-service/build/libs/order-service.war /opt/jboss/wildfly/standalone/deployments/ROOT.war

EXPOSE 8080

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0"]
