package com.blss.bitrixjca.impl;

import com.blss.bitrixjca.api.BitrixConnection;
import com.blss.bitrixjca.api.BitrixConnectionFactory;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Реализация фабрики соединений.
 */
@Slf4j
@RequiredArgsConstructor
public class BitrixConnectionFactoryImpl implements BitrixConnectionFactory {

    private final BitrixManagedConnectionFactory managedConnectionFactory;
    private final ConnectionManager connectionManager;

    @Override
    public BitrixConnection getConnection() throws ResourceException {
        log.debug("Obtaining Bitrix24 connection");
        
        if (connectionManager != null) {
            // Managed mode - use connection pooling
            return (BitrixConnection) connectionManager.allocateConnection(
                    managedConnectionFactory,
                    null
            );
        } else {
            // Standalone mode - create direct connection
            BitrixManagedConnection mc = managedConnectionFactory.createManagedConnection(null, null);
            return (BitrixConnection) mc.getConnection(null, null);
        }
    }
}
