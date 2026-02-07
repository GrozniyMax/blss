package com.blss.blss.domain;


import org.springframework.data.annotation.Id;

import java.util.UUID;

record User(

        @Id
        UUID id,

        String email
) { }
