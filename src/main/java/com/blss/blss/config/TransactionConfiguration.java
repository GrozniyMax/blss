package com.blss.blss.config;

import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@EnableTransactionManagement
public class TransactionConfiguration {

    @Bean
    public TransactionOperations transactionOperations(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean
    public PlatformTransactionManager transactionManager() throws Exception {
        return new JtaTransactionManager(userTransaction(), applicationServerTransactionManager());
    }

    private UserTransaction userTransaction() throws Exception {
        return lookup("java:comp/UserTransaction", UserTransaction.class);
    }

    private TransactionManager applicationServerTransactionManager() throws Exception {
        return lookup("java:/TransactionManager", TransactionManager.class);
    }

    private <T> T lookup(String jndiName, Class<T> requiredType) throws Exception {
        var factory = new JndiObjectFactoryBean();
        factory.setJndiName(jndiName);
        factory.setProxyInterface(requiredType);
        factory.setLookupOnStartup(true);
        factory.setResourceRef(false);
        factory.afterPropertiesSet();
        return requiredType.cast(factory.getObject());
    }
}
