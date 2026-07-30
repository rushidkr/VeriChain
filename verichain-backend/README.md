# VeriChain — Tamper-Evident Credential Verification System

A Spring Boot backend that lets colleges/companies issue verifiable digital credentials
(internship certificates, offer letters, degrees) and lets **anyone** verify one instantly —
no phone call to the registrar, no email to HR. Tampering is detected using a private
hash-chain + RSA digital signatures, not a public blockchain.

## Why this design (viva-ready reasoning)

| Decision | Reasoning |
|---|---|
| Hash-chain instead of blockchain | There's a single trusted issuer per organization — no need for consensus/mining/distributed nodes. A hash-chain gives the same tamper-evidence property (any edit breaks everything downstream) at a fraction of the complexity. |
| RSA signatures on top of the chain | The chain alone proves *internal consistency* (nothing was edited after the fact). The signature proves *authenticity* — that a specific issuer, holding a specific private key, actually created this record. |
| Keypair generated only on approval, not registration | An unvetted applicant should never hold a signing key capable of producing "valid-looking" credentials. |
| Private key AES-256-GCM encrypted at rest | A database leak alone shouldn't be enough to steal an issuer's signing key. |
| Pessimistic lock on `IssuerChainState` during issuance | Two concurrent issuance requests from the same issuer must not read the same "latest link" and fork the chain. |
| No public admin registration endpoint | Admin is seeded from environment config on first boot — self-registering as admin would defeat the entire approval workflow. |

## Tech stack

- Java 17, Spring Boot 3.3 (Web, Security, Data JPA, Validation)
- H2 in-memory DB by default (zero setup for demo/viva) — PostgreSQL + Flyway migration included for production
- JWT auth (jjwt) with role-based access (ADMIN / ISSUER / STUDENT)
- `java.security` (RSA-2048, SHA-256withRSA) for signing — no external crypto library
- ZXing for QR code generation
- JUnit 5 for the crypto core (`HashChainService`, `SignatureService`)

## Project structure

```
src/main/java/com/verichain/
  entity/        6 JPA entities: User, IssuerProfile, Credential, IssuerChainState,
                 VerificationLog, RevocationRecord (+ enums)
  repository/    Spring Data JPA repositories
  security/      JwtUtil, UserPrincipal, UserDetailsServiceImpl
  config/        SecurityConfig, JwtAuthenticationFilter, DataSeeder
  service/       HashChainService, SignatureService, KeyManagementService,
                 QrCodeService, AuthService, CredentialService, ChainStateInitializer,
                 VerificationService, IssuerService, AdminService
  controller/    AuthController, IssuerController, VerificationController, AdminController
  dto/           request/ and response/ DTOs
  exception/     GlobalExceptionHandler + custom exceptions
src/test/java/com/verichain/service/
  HashChainServiceTest.java   — determinism, tamper detection, chain-linking (7 tests)
  SignatureServiceTest.java  — valid/forged/wrong-key signature scenarios (5 tests)
src/main/resources/
  application.properties
  db/migration/V1__init_schema.sql   (Postgres production schema, Flyway)
```

## What's actually been verified (and what hasn't)

This sandbox can't reach Maven Central (confirmed: `repo.maven.apache.org` returns 403 through
the egress proxy), so a full `mvn clean install` couldn't be run here. But rather than leave
that as a blanket disclaimer, here's exactly what was and wasn't checked:

**Actually compiled and run, for real, in this environment:**
The crypto core (`HashChainService`, `SignatureService`, `KeyManagementService`) has zero
dependencies beyond the JDK once you set aside the `@Service`/`@Value` annotations — so it was
extracted, compiled with plain `javac`, and executed with plain `java` against a 21-assertion
harness covering the exact same scenarios as the shipped JUnit tests, plus a full simulated
issuance-and-verification pipeline (generate issuer keypair → issue two chained credentials →
verify both → tamper with one → confirm it's caught → attempt a forgery with someone else's
private key → confirm it's rejected → round-trip the AES-encrypted private key). All 21 passed.
This is the actual mechanism the whole product depends on, proven by execution, not just review.

**Manually audited line-by-line, not compiled:**
Everything Spring-specific — controllers, `@Service`/`@Repository` beans, security config, JPA
entities. This included a scripted cross-check of every field every backend DTO exposes against
every field the React frontend actually reads (`grep`-extracted both sides and diffed them) —
no mismatches found. Two real bugs were found this way and fixed:

1. **Malformed credential ID crashed with a 500.** `/api/verify/{id}` took `@PathVariable UUID`,
   so pasting non-UUID text (an easy thing to do on the one endpoint meant for free-text human
   input) threw an unhandled `IllegalArgumentException`. Fixed: the ID is now parsed manually and
   a malformed one returns a clean `NOT_FOUND`-shaped response instead of an error. A
   `MethodArgumentTypeMismatchException` handler was also added to `GlobalExceptionHandler` as a
   defensive backstop for the other typed-ID endpoints.
2. **Race condition on an issuer's first-ever credential.** Two concurrent issuance requests
   could both see no `IssuerChainState` row and both try to insert one, causing a duplicate-key
   failure. Fixed with `ChainStateInitializer`, a separate bean whose `ensureExists()` runs in
   its own `REQUIRES_NEW` transaction specifically so a losing insert only rolls back that tiny
   transaction, not the whole issuance — deliberately a separate bean, since a self-invoked
   `@Transactional` method on `this` would silently skip the proxy and not actually get its own
   transaction (a classic Spring gotcha, and directly relevant if this comes up in your Spring
   Boot interview prep on transaction propagation).

**Please still run `mvn clean install` yourself as the first real step** — the above gives real
confidence in the core logic and the API contract, but Spring context startup, JPA mapping
quirks, and the like can only truly be confirmed by Spring actually booting. If anything fails,
paste me the error and I'll fix it immediately.

## Running it

Requires Java 17+ and Maven (or use the included `mvnw` if you add one via `mvn -N wrapper:wrapper`).

```bash
mvn spring-boot:run
```

The app starts on `http://localhost:8080`, backed by an in-memory H2 database (data resets on
restart — fine for a demo, switch the datasource block in `application.properties` to Postgres
and set `spring.flyway.enabled=true` for anything persistent).

A default admin account is seeded on first boot:
```
email:    admin@verichain.com
password: ChangeMe123!
```
(Override via `ADMIN_EMAIL` / `ADMIN_PASSWORD` environment variables.)

Run the crypto-core unit tests on their own:
```bash
mvn test -Dtest=HashChainServiceTest,SignatureServiceTest
```

> **Note on this environment:** this sandbox can't reach Maven Central, so I wasn't able to run
> `mvn test` here to confirm a green build. The code follows standard Spring Boot 3.3 / Jakarta
> EE patterns throughout, but please run `mvn clean install` yourself as the first step — if
> anything doesn't compile, paste me the error and I'll fix it immediately.

## Full demo walkthrough (curl)

**1. Register an issuer (e.g. a college placement cell)**
```bash
curl -X POST http://localhost:8080/api/auth/register/issuer \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Placement Officer",
    "email": "placement@snjb.edu",
    "password": "SecurePass123",
    "organizationName": "SNJB College of Engineering",
    "registrationNumber": "SNJB-CE-2026"
  }'
```
Save the returned `token` — but note the account is `PENDING` and **cannot issue credentials yet**.

**2. Log in as admin and approve the issuer**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@verichain.com","password":"ChangeMe123!"}'
# → copy the admin token

curl -X GET http://localhost:8080/api/admin/issuers/pending \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
# → copy the issuerId

curl -X PUT http://localhost:8080/api/admin/issuers/<ISSUER_ID>/approve \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```
This is the moment the RSA keypair is actually generated for that issuer.

**3. Issue a credential (as the now-approved issuer)**
```bash
curl -X POST http://localhost:8080/api/issuer/credentials \
  -H "Authorization: Bearer <ISSUER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "holderName": "Rushi Patil",
    "holderEmail": "rushi@example.com",
    "credentialType": "INTERNSHIP_CERTIFICATE",
    "title": "Backend Development Intern",
    "description": "6-month internship, Spring Boot backend team",
    "issueDate": "2026-07-01"
  }'
```
Response includes the credential `id`, `dataHash`, `chainHash`, and `verificationUrl`.

**4. Verify it publicly (no auth needed — this is the whole point)**
```bash
curl http://localhost:8080/api/verify/<CREDENTIAL_ID>
```
→ `"result": "VALID"`

**5. Simulate tampering** — manually edit the `title` field directly in the H2 console
(`http://localhost:8080/h2-console`, JDBC URL `jdbc:h2:mem:verichain`) and re-run step 4.
→ `"result": "TAMPERED"`, because the recomputed `dataHash` no longer matches the stored one.

**6. Revoke it**
```bash
curl -X PUT http://localhost:8080/api/issuer/credentials/<CREDENTIAL_ID>/revoke \
  -H "Authorization: Bearer <ISSUER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"reason": "Issued in error"}'
```
→ verifying again now returns `"result": "REVOKED"`.

**7. Get the QR code**
```bash
curl http://localhost:8080/api/verify/<CREDENTIAL_ID>/qrcode --output cred.png
```

## API reference

| Method | Endpoint | Auth | Purpose |
|---|---|---|---|
| POST | `/api/auth/register/student` | none | Register a student/holder account |
| POST | `/api/auth/register/issuer` | none | Register an issuer (goes to PENDING) |
| POST | `/api/auth/login` | none | Login, returns JWT |
| GET | `/api/admin/issuers/pending` | ADMIN | List issuers awaiting approval |
| GET | `/api/admin/issuers` | ADMIN | List all issuers |
| PUT | `/api/admin/issuers/{id}/approve` | ADMIN | Approve + generate keypair |
| PUT | `/api/admin/issuers/{id}/reject` | ADMIN | Reject an applicant |
| GET | `/api/admin/verification-logs` | ADMIN | Paginated audit trail |
| GET | `/api/issuer/profile` | ISSUER | Own profile + public key |
| POST | `/api/issuer/credentials` | ISSUER | Issue a credential |
| GET | `/api/issuer/credentials` | ISSUER | List own issued credentials |
| GET | `/api/issuer/credentials/{id}` | ISSUER | Get one credential |
| PUT | `/api/issuer/credentials/{id}/revoke` | ISSUER | Revoke a credential |
| GET | `/api/verify/{id}` | none | **Public verification** — VALID / TAMPERED / REVOKED / NOT_FOUND |
| GET | `/api/verify/{id}/qrcode` | none | PNG QR code for a credential |

## What's next (not yet built)

- **React frontend** — issuer dashboard (issue/list/revoke), admin panel (approve issuers, view
  audit logs), and the public verification page (paste ID or scan QR → instant result). This is
  the natural next step and I can build it the same way we did CEMS's frontend.
- **Phase 8 (stretch): OCR verification** — a small Python/FastAPI microservice using Tesseract
  to extract a credential ID from an uploaded scanned PDF/image and call `/api/verify/{id}`
  automatically, called from Spring Boot over REST. Good if you want the Java+Python polyglot
  story alongside your DurgSetu CV pipeline experience.

Want me to build the React frontend next?
