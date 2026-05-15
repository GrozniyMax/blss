package com.blss.bitrixjca.api;

import jakarta.resource.ResourceException;

import java.io.Serializable;

/**
 * Фабрика соединений с Bitrix24.
 */
public interface BitrixConnectionFactory extends Serializable {

    BitrixConnection getConnection() throws ResourceException;
}
