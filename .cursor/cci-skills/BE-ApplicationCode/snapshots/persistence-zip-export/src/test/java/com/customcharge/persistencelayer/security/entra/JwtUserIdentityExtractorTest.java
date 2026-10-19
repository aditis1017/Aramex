package com.customcharge.persistencelayer.security.entra;

import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUserIdentityExtractorTest {

    private final JwtUserIdentityExtractor extractor = new JwtUserIdentityExtractor();

    @Test
    void extract_prefersEmailThenPreferredThenUpn() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("sub-1")
                .claim("preferred_username", "pref@example.com")
                .claim("email", "mail@example.com")
                .claim("upn", "upn@example.com")
                .claim("oid", "oid-1")
                .build();

        JwtUserIdentity id = extractor.extract(claims);

        assertThat(id.primaryLookupKey()).isEqualTo("mail@example.com");
        assertThat(id.subject()).isEqualTo("sub-1");
        assertThat(id.oid()).isEqualTo("oid-1");
    }

    @Test
    void extract_fallsBackToSubject() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("only-sub")
                .build();

        JwtUserIdentity id = extractor.extract(claims);

        assertThat(id.primaryLookupKey()).isEqualTo("only-sub");
    }

    @Test
    void extract_readsRolesAndGroups() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("sub-1")
                .claim("email", "mail@example.com")
                .claim("roles", java.util.List.of("Admin", "Reader"))
                .claim("groups", java.util.List.of("11111111-1111-1111-1111-111111111111"))
                .build();

        JwtUserIdentity id = extractor.extract(claims);

        assertThat(id.tokenRoles()).containsExactly("Admin", "Reader");
        assertThat(id.tokenGroups()).containsExactly("11111111-1111-1111-1111-111111111111");
    }

    @Test
    void extract_rejectsBlankLookup() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("   ")
                .build();

        assertThatThrownBy(() -> extractor.extract(claims))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
