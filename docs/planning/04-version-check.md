# 04 — Version check (2026-11-20 ~11:20 UTC)

Checked live, not from memory, so the stack decision rests on facts.

Source: Maven Central `org/springframework/boot/spring-boot/maven-metadata.xml` and POM `Last-Modified` headers; npm registry `latest` tags.

| Component | Finding |
|-----------|---------|
| Spring Boot 4.2.0 | Latest GA (`<release>`), published 2026-11-19, i.e. one day old |
| Spring Boot 4.1.x | 4.1.0 published 2026-06-10; latest patch 4.1.3 (published 2026-11-19) |
| Spring Boot 3.5.x | Latest patch 3.5.16; 3.5.0 published 2025-05-22 |
| React | npm `latest` = 19.3.1 |
| Vite | npm `latest` = 8.3.8 |

Not verified: the exact open-source support end dates for 3.5.x and 4.1.x (I did not look them up; I believe 3.5 OSS support has ended or is ending, but this is unconfirmed).

## Follow-up check (2026-11-20 ~11:35 UTC), requested by Treefa before approving Boot 4.1

| Question | Finding | Source |
|----------|---------|--------|
| Does springdoc support Boot 4.1? | Yes. springdoc-openapi 3.x targets Boot 4. 3.2.0 (published 2026-10-24) is built against `spring-boot-starter-parent` 4.1.2; 3.1.1 against 4.1.0. | Maven Central POMs of `org.springdoc:springdoc-openapi` |
| Boot 4.1.3 managed versions | Spring Framework 7.0.11, Jackson 3.1.8, Flyway 12.4.0, Testcontainers 2.0.5, PostgreSQL JDBC 42.7.14 | Maven Central `spring-boot-dependencies-4.1.3.pom` |
| Is 3.5.x still supported (OSS)? | No. Spring's 3.5.16 announcement calls it the "last OSS release of the 3.5.x generation" and recommends upgrading to 4.0.x or 4.1.x. OSS end date 30 June 2026 per two third-party sources (not confirmed on spring.io directly). | https://spring.io/blog/2026/06/25/spring-boot-3-5-16-available-now/ , https://ecorpit.com/spring-boot-4-1-end-of-support-2027/ , https://blog.codercops.com/blog/spring-boot-3-5-end-of-life-upgrade-to-4-1-2026 |
| 4.1.x OSS end date | 31 July 2027, per two third-party sources citing Spring's generations data. Could not open spring.io's own support data (blocked by this environment's network policy). | same third-party pages |

Conclusion: keep Spring Boot 4.1.3 with springdoc-openapi 3.2.0. Boot 3.x is not a safer fallback: its free support has already ended.
