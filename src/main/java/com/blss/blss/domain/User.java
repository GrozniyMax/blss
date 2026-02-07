package com.blss.blss.domain;


import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("users")
public record User(

        @Id
        UUID id,

        String email
) { }
