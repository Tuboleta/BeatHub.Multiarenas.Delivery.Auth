package com.beathub.multiarenas.delivery.auth.dto.sso;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AzureTokenClaims {

    private String subject;
    private String email;
    private String name;
    private String givenName;
    private String familyName;
    private String issuer;
    private Set<String> audience;
    private Date expiration;
    private Map<String, Object> rawClaims;
}
