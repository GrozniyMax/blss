FROM quay.io/wildfly/wildfly:37.0.0.Final-jdk17

COPY user-service/build/libs/user-service.war /opt/jboss/wildfly/standalone/deployments/ROOT.war
COPY user-service/users.xml /opt/jboss/wildfly/standalone/configuration/users.xml

ENV SECURITY_USERS_XML_PATH=/opt/jboss/wildfly/standalone/configuration/users.xml

EXPOSE 8080

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0"]
