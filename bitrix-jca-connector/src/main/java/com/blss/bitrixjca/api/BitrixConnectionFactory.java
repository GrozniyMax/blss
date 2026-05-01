package com.blss.bitrixjca.api;

import jakarta.resource.ResourceException;

import java.io.Serializable;

/**
 * Connection factory for creating connections to Bitrix24 REST API.
 * Applications obtain this factory via JNDI lookup or resource injection.
 */
public interface BitrixConnectionFactory extends Serializable {

    /**
     * Creates a connection to the Bitrix24 REST API.
     *
     * @return BitrixConnection handle for interacting with Bitrix24
     * @throws ResourceException if a connection cannot be established
     */
    BitrixConnection getConnection() throws ResourceException;
}
