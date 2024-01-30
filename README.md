# Spring Boot WebAuthn Server and Virtual FIDO2 Authenticator 
The Basswood-Webauthn project is s reference WebAuthn/FIDO 2 server implementation, based on
[Yubico's java-webauthn-server](https://developers.yubico.com/java-webauthn-server/). The server is based 
on Spring Boot and MySQL. It also includes a special module  that offers virtual authenticator implementation
to facilitate automation and integration testng. The project is experimental and proof-of-concept (POC) in nature and
not production ready. If you are planning to develop your own server to support password-less authentication using
Spring Boot this could be a good starting point. But clear understanding of FIDO 2 specification, webauthn protocol and
[Yubico's java-webauthn-server](https://developers.yubico.com/java-webauthn-server/) is a must for such endeavour.

**Table of Contents**
- [Quickstart](#quickstart)
- [Module WebAuthn](#module-webauthn)
- [Module MySQL](#module-mysql)
- [Module Authenticator](#module-authenticator)
- [Postman Collection](#postman-collection)
- [References](#references)

# Quickstart
## Prerequisites
1. Java 21 or later
2. Maven 3.6.2
3. Docker 24.0.6 or later
4. Docker Compose v2.22 or later
5. Postman for testing APIs 

## Checkout
```shell
git clone https://github.com/basswood-admin/basswood-webauthn.git/
```
## Docker Build & Deployment
```shell
./docker/docker-deploy.sh
```
The [docker/docker-deploy.sh](docker/docker-deploy.sh) script accomplishes the following
1. Builds a MySQL 8.2 image with all the necessary schema and users already inplace.
2. Builds the maven project and docker image for the WebAuthn server.
3. Builds tha maven project and docker image for the Authenticator app.
4. Deploys the above images using docker-compose. See [docker/docker-compose.yml](docker/docker-compose.yml) for details on the deployed applications

## Test Drive - Postman
The services offered by the applications are all exposed via HTTP end points and REST APIs.
The included postman artifacts ([basswood-webauthn.postman_collection.json](postman/basswood-webauthn.postman_collection.json) and [basswood-webauthn.postman_environment.json](postman/basswood-webauthn.postman_environment.json)) can be used to try out the APIs against the deployed application  
The details of the APIs has been documented below under respective modules.

# Module WebAuthn
This Spring Boot web application is the reference implementation of the Java Yubico's [java-webauthn-server](https://developers.yubico.com/java-webauthn-server/).
It follows the same step by step guideline as outlined in the Yubico's [Getting Started Guide](https://developers.yubico.com/java-webauthn-server/#:~:text=depth%20API%20documentation.-,Getting%20started,-Using%20this%20library)
Additionally it provides concrete implementation of key Entities that can be stored in the relational databases. 

## Entities
![ERP Diagram](./artifacts/images/erp-diagram.png) <br/>

### [RelyingPartyEntity](webauthn/src/main/java/io/basswood/webauthn/model/rp/RelyingPartyEntity.java)
This is the local implementation of Yubico's [RelyingParty](https://developers.yubico.com/java-webauthn-server/JavaDoc/webauthn-server-core/2.5.0/com/yubico/webauthn/RelyingParty.html), whihc is responsible for
implementing the basic webauthn services like registration and assertion. The Entity class is primarily responsible
for persisting the concept in the database.

### [User](webauthn/src/main/java/io/basswood/webauthn/model/user/User.java)
This is the local implementation of Yubico's [UserIdentity](https://developers.yubico.com/java-webauthn-server/JavaDoc/webauthn-server-core/2.5.0/com/yubico/webauthn/data/UserIdentity.html).
User is the core concept that represents an end user for whom the registration and assertion services are being offered. The security credentials that the server manages are all group under the User entity.
A User may have one or more [Usernames](webauthn/src/main/java/io/basswood/webauthn/model/user/Username.java)

### [RegisteredCredentialEntity](webauthn/src/main/java/io/basswood/webauthn/model/credential/RegisteredCredentialEntity.java)
This is the local implementation of Yubico's [RegisteredCredential](https://developers.yubico.com/java-webauthn-server/JavaDoc/webauthn-server-core/2.5.0/com/yubico/webauthn/RegisteredCredential.html).
RegisteredCredentialEntity encapsulate the necessary cryptography data for a User and stores them in the database.


## Security
The APIs are secured using signed JWT token. The security is enforced using a simple filter: [JWTFilter.java](./webauthn/src/main/java/io/basswood/webauthn/security/JWTFilter.java)
The filter checks for the presence of Authorization Bearer token header in every request and validates its signature and role claims beofre
granting access to the requested API resource. For testing and development purposes the security can be disabled by setting the
following property to true in [application.yaml](./webauthn/src/main/resources/application.yaml)<br/>
```yaml
basswood:
   security:
      jwt:
        filter=true
```
or in [docker-compose.yml](./docker/docker-compose.yml)<br/>
```yaml
services:
   webauthn:
      environment:
         BASSWOOD_SECURITY_JWT_FILTER_DISABLE=true
```
### Roles
The JWT token must have necessary role claim(s) to access specific APIs. Following 4 roles are enforced in the app<br/>
> | Role         | End point      | Description                               |
> |--------------|----------------|-------------------------------------------|
> | user-manager | /user          | Permission to manage User APIs            |
> | rp-manager   | /relying-party | Permission to manage Relying Parties APIs |
> | jwk-manager  | /jwk           | Permission to manage JWKs                 |
> | jwt-manager  | /jwt           | Permission to create JWTs                 |

### JWT Token
A JWT token can be created using the POST /jwt token endpoint. See [JWTController.java](./webauthn/src/main/java/io/basswood/webauthn/rest/JWTController.java) An example token is presented below
#### HEADER
```json
{
   "kid": "c0bdf4fb-3f39-47c6-9eb2-86c148cfca1e",
   "typ": "JWT",
   "alg": "ES256"
}
```
#### Payload
```json
{
   "sub": "webauthn_admin",
   "aud": "webauthn.basswood.io",
   "nbf": 1706555618,
   "roles": [
      "user_manager",
      "rp_manager",
      "jwk_manager",
      "token_manager"
   ],
   "iss": "webauthn.basswood.io",
   "exp": 1924955999,
   "iat": 1706555618,
   "jti": "e5f7f642-5cb4-419d-9959-6d18fa7f4e0a"
}
```

For test purposes a JWT token is bundled with the source code [jwt-for-testing.txt](./webauthn/src/main/resources/secrets/jwt-for-testing.txt).
The same is also used in the [Postman collection](./postman/basswood-webauthn.postman_collection.json). A new token with all required role claims can also be created during system startup.
Setting the [application.yaml](./webauthn/src/main/resources/application.yaml) property ``basswood.security.jwt.print-new-token-on-startup`` to true.
Or by setting the corresponding [docker-compose.yml](./docker/docker-compose.yml) property 
```BASSWOOD_SECURITY_JWT_PRINTNEWTOKENONSTARTUP``` to true.

### Secrets
The webauthn application require 2 secrets - a database password and a Symmetric (AES) key for encryption. The secret key 
is used to encrypt JWK key data in the database column. These 2 secrets must be included in a Java keystore file and made
available in the running Spring Boot Application. This keystore file and a separate keystore configuration file(JSON)
constitutes the secret infrastructure. See the bundles resources [basswood-not-for-production-keystore.p12](./webauthn/src/main/resources/secrets/basswood-not-for-production-keystore.p12)
and [keystore-config.json](./webauthn/src/main/resources/secrets/keystore-config.json). Also see the
[application.yaml](./webauthn/src/main/resources/application.yaml) properties ``basswood.security.keystore.keystore-file``
and ``basswood.security.keystore.keystore-config-file``

The bundled secrets files above only for local development and testing. To create new artifacts for secrets consult the
[KeystoreUtil.java](./webauthn/src/main/java/io/basswood/webauthn/secret/KeystoreUtil.java)   


## Management (REST) APIs

------------------------------------------------------------------------------------------
#### Create RelyingParty
<details>
 <summary><code>POST /relying-party</code><code>(Crete a new RelyingPartyEntity)</code></summary>

##### Request Body
```json
{
    "id" : "red.basswoodid.com",
    "name": "Basswood Red Client",
    "attestation" : "NONE",
    "authenticatorAttachment" : "PLATFORM",
    "residentKey" : "DISCOURAGED",
    "userVerification" : "DISCOURAGED",
    "allowOriginPort" : true,
    "allowOriginSubdomain" : true,
    "timeout": 60000,
    "origins":[ "localhost" ]
}
```
#### Request Header
> | Header                 | Description                                 | 
> |------------------------|---------------------------------------------|
> | `Authorization Bearer` | `The JWT Bearer token with role rp-manager` |
>

##### Responses

> | http code | content-type                      | response                                        |
> |-----------|-----------------------------------|-------------------------------------------------|
> | `200`     | `application/json;charset=UTF-8`  | `returns the newly created RelyingPartyEntity`  |
> | `409`     | `application/json`                | `{"errorCode":"duplicate-entity","message":""}` |
> 
##### Example cURL

```shell
curl --location --request POST 'http://red.basswoodid.com:9080/relying-party' \
--header 'Content-Type: application/json' \
--header 'Authorization Bearer: eyJraWQiOiJjMGJkZjRmYi0zZjM5LTQ3YzYtOWViMi04NmMxNDhjZmNhMWUiLCJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJ3ZWJhdXRobl9hZG1pbiIsImF1ZCI6IndlYmF1dGhuLmJhc3N3b29kLmlvIiwibmJmIjoxNzA2MjQxMzA3LCJyb2xlcyI6WyJ1c2VyX21hbmFnZXIiLCJycF9tYW5hZ2VyIiwiandrX21hbmFnZXIiLCJ0b2tlbl9tYW5hZ2VyIl0sImlzcyI6IndlYmF1dGhuLmJhc3N3b29kLmlvIiwiZXhwIjoxOTI0OTkxOTk5LCJpYXQiOjE3MDYyNDEzMDcsImp0aSI6IjcwZGQ0ZGY5LTA2MGMtNGFmZC1iMDg1LWU4NDAzODMzOWJhMSJ9.EuSLWJPAj-ST0MF-W6UB0rTyblHKOx9FLUd2C-AJ4Gt5sBoalvMaVAmFeh04Vo_0fkS7edkCPM5N49Cws_It8w' \
--data-raw '{
"id" : "red.basswoodid.com",
"name": "Basswood Red Client",
"attestation" : "NONE",
"authenticatorAttachment" : "PLATFORM",
"residentKey" : "DISCOURAGED",
"userVerification" : "DISCOURAGED",
"allowOriginPort" : true,
"allowOriginSubdomain" : true,
"timeout": 60000,
"origins":[ "localhost" ]
}'
```
</details>
------------------------------------------------------------------------------------------

------------------------------------------------------------------------------------------
#### Get RelyingParty
<details>
<summary><code>GET /relying-party/{rpId}</code><code>(Retrieves a RelyingPartyEntity by Id)</code></summary>

##### Request Body
None
#### Request Header
> | Header                 | Description                                 | 
> |------------------------|---------------------------------------------|
> | `Authorization Bearer` | `The JWT Bearer token with role rp-manager` |
>

#### Parameters

> | name      |  type     | data type                 | description                                                         |
> |-----------|-----------|---------------------------|---------------------------------------------------------------------|
> | rpId      |  required | String                    | Path parameter                                                      |
>

##### Responses
> | http code | content-type                      | response                                        |
> |-----------|-----------------------------------|-------------------------------------------------|
> | `200`     | `application/json;charset=UTF-8`  | `Configuration created successfully`            |
> | `404`     | `application/json`                | `{"errorCode":"entity-not-found","message":""}` |
>
##### Example cURL
```shell
curl --location --request GET 'http://red.basswoodid.com:9080/relying-party/red.basswoodid.com'
```
</details>
------------------------------------------------------------------------------------------

------------------------------------------------------------------------------------------
#### Create User
<details>
<summary><code>POST /user</code><code>(Creates a User)</code></summary>

##### Request Body
See [User](./webauthn/src/main/java/io/basswood/webauthn/model/user/User.java)
```json
{
    "userHandle" : "{{$randomUUID}}",
    "displayName": "Homer Simpson",
    "usernames" : [
        {
            "username" : "homer.simpson@aol.com"
        }
    ]
}
```
#### Request Header
> | Header                 | Description                                   | 
> |------------------------|-----------------------------------------------|
> | `Authorization Bearer` | `The JWT Bearer token with role user-manager` |
>

##### Responses
> | http code | content-type                      | response                                   |
> |-----------|-----------------------------------|--------------------------------------------|
> | `200`     | `application/json;charset=UTF-8`  | `returns the newly created User entity`    |
> | `400`     | `application/json`                | `{"errorCode":"bad-request","message":""}` |
>
##### Example cURL
```shell
curl --location --request POST 'http://red.basswoodid.com:9080/user' \
--header 'Content-Type: application/json' \
--header 'Authorization Bearer: eyJraWQiOiJjMGJkZjRmYi0zZjM5LTQ3YzYtOWViMi04NmMxNDhjZmNhMWUiLCJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJ3ZWJhdXRobl9hZG1pbiIsImF1ZCI6IndlYmF1dGhuLmJhc3N3b29kLmlvIiwibmJmIjoxNzA2MjQxMzA3LCJyb2xlcyI6WyJ1c2VyX21hbmFnZXIiLCJycF9tYW5hZ2VyIiwiandrX21hbmFnZXIiLCJ0b2tlbl9tYW5hZ2VyIl0sImlzcyI6IndlYmF1dGhuLmJhc3N3b29kLmlvIiwiZXhwIjoxOTI0OTkxOTk5LCJpYXQiOjE3MDYyNDEzMDcsImp0aSI6IjcwZGQ0ZGY5LTA2MGMtNGFmZC1iMDg1LWU4NDAzODMzOWJhMSJ9.EuSLWJPAj-ST0MF-W6UB0rTyblHKOx9FLUd2C-AJ4Gt5sBoalvMaVAmFeh04Vo_0fkS7edkCPM5N49Cws_It8w' \
--data-raw '{
    "userHandle" : "caebd925-6fa8-447a-8b14-56705619a2c9",
    "displayName": "Homer Simpson",
    "usernames" : [
        {
            "username" : "homer.simpson@aol.com"
        }
    ]
}'
```
</details>
------------------------------------------------------------------------------------------

------------------------------------------------------------------------------------------
#### GET User
<details>
<summary><code>GET /user/userHandle</code><code>(Retrieves a User by userHandle)</code></summary>

##### Request Body
None
#### Request Header
> | Header                 | Description                                   | 
> |------------------------|-----------------------------------------------|
> | `Authorization Bearer` | `The JWT Bearer token with role user-manager` |
>

##### Responses

> | http code | content-type            | response                                   |
> |-----------|-------------------------|--------------------------------------------|
> | `200`     | `application/json`      | `Retrieves a User by userHandle (id)`      |
> | `400`     | `application/json`      | `{"errorCode":"bad-request","message":""}` |
>
##### Example cURL
```shell
curl --location \ 
--header 'Authorization Bearer: eyJraWQiOiJjMGJkZjRmYi0zZjM5LTQ3YzYtOWViMi04NmMxNDhjZmNhMWUiLCJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJ3ZWJhdXRobl9hZG1pbiIsImF1ZCI6IndlYmF1dGhuLmJhc3N3b29kLmlvIiwibmJmIjoxNzA2MjQxMzA3LCJyb2xlcyI6WyJ1c2VyX21hbmFnZXIiLCJycF9tYW5hZ2VyIiwiandrX21hbmFnZXIiLCJ0b2tlbl9tYW5hZ2VyIl0sImlzcyI6IndlYmF1dGhuLmJhc3N3b29kLmlvIiwiZXhwIjoxOTI0OTkxOTk5LCJpYXQiOjE3MDYyNDEzMDcsImp0aSI6IjcwZGQ0ZGY5LTA2MGMtNGFmZC1iMDg1LWU4NDAzODMzOWJhMSJ9.EuSLWJPAj-ST0MF-W6UB0rTyblHKOx9FLUd2C-AJ4Gt5sBoalvMaVAmFeh04Vo_0fkS7edkCPM5N49Cws_It8w' \
--request GET 'http://red.basswoodid.com:9080/user/caebd9256fa8447a8b1456705619a2c9'
```
</details>
------------------------------------------------------------------------------------------
------------------------------------------------------------------------------------------
#### Create JWK
<details>
<summary><code>POST /jwk</code><code>(Create a new JSON Web Key)</code></summary>

##### Request Body (Optional)
See [JWKCreateDTO](./webauthn/src/main/java/io/basswood/webauthn/dto/JWKCreateDTO.java)
```json
{
  "keyTypeEnum" : "RSA",
  "keyUseEnum" : "SIGNATURE",
  "keyLengthEnum" : "KEY_LENGTH_3072"
}
```
> | Filed           | Default           | Accepted Values                                     | Description                      | 
> |-----------------|-------------------|-----------------------------------------------------|----------------------------------|
> | `keyTypeEnum`   | `EC`              | `EC, RSA`                                           | `Spported keytype is EC and RSA` |
> | `keyUseEnum`    | `SIGNATURE`       | `SIGNATURE, ENCRYPTION`                             | `The purpose of the key`         |
> | `keyLengthEnum` | `KEY_LENGTH_2048` | `KEY_LENGTH_2048, KEY_LENGTH_3072, KEY_LENGTH_4096` | `Key length for RSA key`         |
> | `curveEnum`     | `P_256`           | `P_256, P_384, P_521`                               | `Curve for the EC key`           |
>

#### Request Header
> | Header                 | Description                                  | 
> |------------------------|----------------------------------------------|
> | `Authorization Bearer` | `The JWT Bearer token with role jwk-manager` |
>
#### Request Parameter
> | Parameter             | Default | Description                                          | 
> |-----------------------|---------|------------------------------------------------------|
> | `returnPublicKeyOnly` | true    | `Returns only the public portion of the JWK if true` |
>

##### Responses

> | http code | content-type            | response                                   |
> |-----------|-------------------------|--------------------------------------------|
> | `200`     | `application/json`      | `Returns the JWK key being created`        |
>
##### Example cURL
```shell
curl --location --request POST 'http://red.basswoodid.com:9080/jwk' \
--header 'Authorization: Bearer eyJraWQiOiJjMGJkZjRmYi0zZjM5LTQ3YzYtOWViMi04NmMxNDhjZmNhMWUiLCJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJ3ZWJhdXRobl9hZG1pbiIsImF1ZCI6IndlYmF1dGhuLmJhc3N3b29kLmlvIiwibmJmIjoxNzA2MjQwNzA4LCJyb2xlcyI6WyJqd2tfbWFuYWdlciIsInRva2VuX21hbmFnZXIiLCJycF9tYW5hZ2VyIiwidXNlcl9tYW5hZ2VyIl0sImlzcyI6IndlYmF1dGhuLmJhc3N3b29kLmlvIiwiZXhwIjoxODYzOTIwNzA4LCJpYXQiOjE3MDYyNDA3MDgsImp0aSI6ImE3NWJjNTY2LWEzYTYtNGZkNC1iZDA1LTdkMmMxMjQ5Zjc3MiJ9.Aymr4xRCRniwEwnntEZ5nnauyblbFk69BnR5ZzO89J4UKgV4rQLqZN8RT1Qo0LG5AxQ6ZMWeTLOPvtAWl5N4bQ' \
--header 'Content-Type: application/json' \
--data-raw '{
    "keyTypeEnum" : "RSA",
    "keyUseEnum" : "SIGNATURE",
    "keyLengthEnum" : "KEY_LENGTH_3072"
}'
```
</details>
------------------------------------------------------------------------------------------

------------------------------------------------------------------------------------------
#### Get JWK
<details>
<summary><code>GET /jwk</code><code>(Create a new JSON Web Key)</code></summary>

##### Request Body (Optional)
None
#### Request Header
> | Header                 | Description                                  | 
> |------------------------|----------------------------------------------|
> | `Authorization Bearer` | `The JWT Bearer token with role jwk-manager` |
>
#### Request Parameter
> | Parameter             | Default | Description                                          | 
> |-----------------------|---------|------------------------------------------------------|
> | `returnPublicKeyOnly` | true    | `Returns only the public portion of the JWK if true` |
>

##### Responses
> | http code | content-type            | response                                   |
> |-----------|-------------------------|--------------------------------------------|
> | `200`     | `application/json`      | `Returns the JWK key being created`        |
>
```json
{
    "kty": "EC",
    "use": "sig",
    "crv": "P-256",
    "kid": "c0bdf4fb-3f39-47c6-9eb2-86c148cfca1e",
    "x": "hRXf0SqOfN2-V9H_Mo3p8H9oZEwDgUC2sc1eJAdxwao",
    "y": "VUmI3stpJONeqe_glA6vqS_uk1Y3ZBnd-6bOl9LvaRM",
    "exp": 1708814687,
    "iat": 1706222687
}
```

##### Example cURL
```shell
curl --location --request GET 'http://red.basswoodid.com:9080/jwk/c0bdf4fb-3f39-47c6-9eb2-86c148cfca1e?returnPublicKeyOnly=true' \
--header 'Authorization: Bearer eyJraWQiOiJjMGJkZjRmYi0zZjM5LTQ3YzYtOWViMi04NmMxNDhjZmNhMWUiLCJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJ3ZWJhdXRobl9hZG1pbiIsImF1ZCI6IndlYmF1dGhuLmJhc3N3b29kLmlvIiwibmJmIjoxNzA2MjQwNzA4LCJyb2xlcyI6WyJqd2tfbWFuYWdlciIsInRva2VuX21hbmFnZXIiLCJycF9tYW5hZ2VyIiwidXNlcl9tYW5hZ2VyIl0sImlzcyI6IndlYmF1dGhuLmJhc3N3b29kLmlvIiwiZXhwIjoxODYzOTIwNzA4LCJpYXQiOjE3MDYyNDA3MDgsImp0aSI6ImE3NWJjNTY2LWEzYTYtNGZkNC1iZDA1LTdkMmMxMjQ5Zjc3MiJ9.Aymr4xRCRniwEwnntEZ5nnauyblbFk69BnR5ZzO89J4UKgV4rQLqZN8RT1Qo0LG5AxQ6ZMWeTLOPvtAWl5N4bQ'
```
</details>
------------------------------------------------------------------------------------------
------------------------------------------------------------------------------------------
#### Create JWT
<details>
<summary><code>POST /jwt</code><code>(Create a new JSON Web Token)</code></summary>

##### Request Body
See [Token](./webauthn/src/main/java/io/basswood/webauthn/model/token/Token.java)
```json
{
  "subject" : "webauthn_admin",
  "issuer" : "webauthn.basswood.io",
  "audience" : "webauthn.basswood.io",
  "jti" : "61025c0b-bd76-4a5f-9557-a6ffc06d1440",
  "expirationTime" : "2030-12-31T13:59:59",
  "claimSet" : {
    "roles" : ["user_manager", "rp_manager", "jwk_manager", "token_manager"]
  }
}
```
> | Filed            | Default                         | Accepted Values | Description                                           | 
> |------------------|---------------------------------|-----------------|-------------------------------------------------------|
> | `subject`        | `webauthn_admin`                | `N/A`           | `The subjet/user's identifierA`                       |
> | `issuer`         | `webauthn.basswood.io`          | `N/A`           | `The issuing agent or server`                         |
> | `audience`       | `webauthn.basswood.io`          | `N/A`           | `The party for whom the JWT is being issued`          |
> | `jti`            | `random UUID`                   | `N/A`           | `The unique id of the JWT`                            |
> | `issueTime`      | `Current time`                  | `Any date`      | `The time of issuence`                                |
> | `notBeforeTime`  | `Current time`                  | `Any date`      | `Time when the JWT becomes effective, but not before` |
> | `expirationTime` | `300 secnds after current time` | `Any date`      | `Time after which the JWT is not valid anylonger`     |
> | `claimSet`       | `none`                          | `Map of claims` | `A map of claims. For example roles.`                 |
>

#### Request Header
> | Header                 | Description                                  | 
> |------------------------|----------------------------------------------|
> | `Authorization Bearer` | `The JWT Bearer token with role jwt-manager` |
>

##### Responses

> | http code | content-type            | response                            |
> |-----------|-------------------------|-------------------------------------|
> | `200`     | `application/json`      | `Returns the JWT being created` |
>
##### Example cURL
```shell
curl --location --request POST 'http://red.basswoodid.com:9080/jwt' \
--header 'Authorization: Bearer eyJraWQiOiJjMGJkZjRmYi0zZjM5LTQ3YzYtOWViMi04NmMxNDhjZmNhMWUiLCJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJ3ZWJhdXRobl9hZG1pbiIsImF1ZCI6IndlYmF1dGhuLmJhc3N3b29kLmlvIiwibmJmIjoxNzA2MjQwNzA4LCJyb2xlcyI6WyJqd2tfbWFuYWdlciIsInRva2VuX21hbmFnZXIiLCJycF9tYW5hZ2VyIiwidXNlcl9tYW5hZ2VyIl0sImlzcyI6IndlYmF1dGhuLmJhc3N3b29kLmlvIiwiZXhwIjoxODYzOTIwNzA4LCJpYXQiOjE3MDYyNDA3MDgsImp0aSI6ImE3NWJjNTY2LWEzYTYtNGZkNC1iZDA1LTdkMmMxMjQ5Zjc3MiJ9.Aymr4xRCRniwEwnntEZ5nnauyblbFk69BnR5ZzO89J4UKgV4rQLqZN8RT1Qo0LG5AxQ6ZMWeTLOPvtAWl5N4bQ' \
--header 'Content-Type: application/json' \
--data-raw '{
    "subject" : "webauthn_admin",
    "issuer" : "webauthn.basswood.io",
    "audience" : "webauthn.basswood.io",
    "jti" : "9038f192-3e09-4bab-8abf-27d28f8317a5",
    "expirationTime" : "2030-12-31T13:59:59",
    "claimSet" : {
        "roles" : ["user_manager", "rp_manager", "jwk_manager", "token_manager"]
    }
}'
```
</details>
------------------------------------------------------------------------------------------
------------------------------------------------------------------------------------------

## WebAuthn APIs - Registration
Registration is the process where an end user register their credentials with the server.<br/>

**Registration Sequence** <br/>
![Registration Sequence](./artifacts/images/registration-sequence.png) <br/>


------------------------------------------------------------------------------------------
#### Registration Start
<details>
<summary><code>POST /webauthn/registration</code><code>(Registration Start)</code></summary>

##### Request Body
See [RegistrationRequestDTO](./webauthn/src/main/java/io/basswood/webauthn/dto/RegistrationRequestDTO.java)
```json
{
    "username": "homer.simpson@aol.com",
    "displayName": "Homer Simpson"
}
```
**Request Headers**

> | header name        | type     | Description                         |
> |--------------------|----------|-------------------------------------|
> | `X-Forwarded-Host` | `String` | `The orign/domain header of client` |
>


##### Responses
**Body** <br/>
See [PublicKeyCredentialCreationOptions](https://www.w3.org/TR/2021/REC-webauthn-2-20210408/#authenticatorassertionresponse)
```json
{
    "rp": {
        "name": "Basswood Red Client",
        "id": "red.basswoodid.com:9080"
    },
    "user": {
        "name": "homer.simpson@aol.com",
        "displayName": "Homer Simpson",
        "id": "c23283ce-23f9-4bf4-8649-eeb1e8bfdf17"
    },
    "challenge": "JMpLAoZKnuS5D3FXuaS2qxPKHnQWgio0_UFjwdoHcS4",
    "pubKeyCredParams": [
        {
            "alg": -7,
            "type": "public-key"
        },
        {
            "alg": -8,
            "type": "public-key"
        },
        {
            "alg": -35,
            "type": "public-key"
        },
        {
            "alg": -36,
            "type": "public-key"
        },
        {
            "alg": -257,
            "type": "public-key"
        },
        {
            "alg": -258,
            "type": "public-key"
        },
        {
            "alg": -259,
            "type": "public-key"
        }
    ],
    "timeout": 60000,
    "excludeCredentials": [],
    "authenticatorSelection": {
        "authenticatorAttachment": "platform",
        "requireResidentKey": false,
        "residentKey": "discouraged",
        "userVerification": "discouraged"
    },
    "attestation": "none",
    "extensions": {
        "appidExclude": null,
        "credProps": true,
        "largeBlob": null,
        "uvm": null
    }
}
```
**Response Headers**

> | header name      | type   |
> |------------------|--------|
> | `registrationId` | `UUID` |
>

**Status Codes**

> | http code | content-type            | response                                   |
> |-----------|-------------------------|--------------------------------------------|
> | `200`     | `application/json`      | `Retrieves PublicKeyCredentialCreationOptions`      |
>
##### Example cURL
```shell
curl --location --request POST 'http://red.basswoodid.com:9080/webauthn/registration' \
--header 'X-Forwarded-Host: red.basswoodid.com:9080' \
--header 'Content-Type: application/json' \
--data-raw '{
    "username": "homer.simpson@aol.com",
    "displayName": "Homer Simpson"
}'
```
</details>
------------------------------------------------------------------------------------------
------------------------------------------------------------------------------------------
#### Registration Finish
<details>
<summary><code>POST /webauthn/registration</code><code>(Registration Finish)</code></summary>

##### Request
**Request Body**
See [AuthenticatorAttestationResponse](https://www.w3.org/TR/2021/REC-webauthn-2-20210408/#authenticatorattestationresponse)
```json
{
  "id": "110557bf-a5ea-4c5e-aa41-1df8510d92f1cmVkLmJhc3N3b29kaWQuY29tOjkwODA",
  "response": {
    "attestationObject": "o2NmbXRkbm9uZWdhdHRTdG10oGhhdXRoRGF0YVi24mO4a-qXlHNmAiDhS1chFBhZWEbSd4AB8yM20C-OtrtFAAAAAd3GK7oH1U9JnA515YKixA8AMtddOee23_muXmvuHOXvmmuNftXX_OddHfdn9XJlZC5iYXNzd29vZGlkLmNvbTo5MDgwpQECAyYgASFYILbRL-DuCs5S1uXAoa31hBzeP_HTGzMNXtejCIJhfTBWIlggXBJUCZltRTS7etWtX0NST9ISOhPGXuUqrR5wX0FIGcU",
    "clientDataJSON": "eyJjaGFsbGVuZ2UiOiJQc3FiNWltbDB3UzllZng0VmI4UWhNSXg4N2RYZ3J3NTByajI5WnFWSy1zIiwib3JpZ2luIjoicmVkLmJhc3N3b29kaWQuY29tOjkwODAiLCJ0eXBlIjoid2ViYXV0aG4uY3JlYXRlIn0",
    "transports": [
      "internal"
    ]
  },
  "authenticatorAttachment": null,
  "clientExtensionResults": {
    "appidExclude": null,
    "credProps": null,
    "largeBlob": null
  },
  "type": "public-key"
}
```

**Request Headers**

>| header name        | type       | Description                                                        |
>|--------------------|------------|--------------------------------------------------------------------|
>| `X-Forwarded-Host` | URL Domain | `The origin domain of the relying party`                           |
>| `registrationId`   | UUID       | `Unique regsitraion created dusing the registration start request` |
>
##### Responses
**Response Body** <br/>
None on success

**Response Headers** <br/>
None

**Status Codes**<br/>

> | http code | content-type            | response  |
> |-----------|-------------------------|-----------|
> | `200`     | `application/json`      | `None`    |
>
##### Example cURL
```shell
curl --location --request POST 'http://red.basswoodid.com:9080/webauthn/registration' \
--header 'X-Forwarded-Host: red.basswoodid.com:9080' \
--header 'registrationId: 16d962d3-56fd-4b47-8aba-c097043018b5' \
--header 'Content-Type: application/json' \
--data-raw '{"id":"110557bf-a5ea-4c5e-aa41-1df8510d92f1cmVkLmJhc3N3b29kaWQuY29tOjkwODA","response":{"attestationObject":"o2NmbXRkbm9uZWdhdHRTdG10oGhhdXRoRGF0YVi24mO4a-qXlHNmAiDhS1chFBhZWEbSd4AB8yM20C-OtrtFAAAAAd3GK7oH1U9JnA515YKixA8AMtddOee23_muXmvuHOXvmmuNftXX_OddHfdn9XJlZC5iYXNzd29vZGlkLmNvbTo5MDgwpQECAyYgASFYILbRL-DuCs5S1uXAoa31hBzeP_HTGzMNXtejCIJhfTBWIlggXBJUCZltRTS7etWtX0NST9ISOhPGXuUqrR5wX0FIGcU","clientDataJSON":"eyJjaGFsbGVuZ2UiOiJQc3FiNWltbDB3UzllZng0VmI4UWhNSXg4N2RYZ3J3NTByajI5WnFWSy1zIiwib3JpZ2luIjoicmVkLmJhc3N3b29kaWQuY29tOjkwODAiLCJ0eXBlIjoid2ViYXV0aG4uY3JlYXRlIn0","transports":["internal"]},"authenticatorAttachment":null,"clientExtensionResults":{"appidExclude":null,"credProps":null,"largeBlob":null},"type":"public-key"}'
```
</details>
------------------------------------------------------------------------------------------

------------------------------------------------------------------------------------------

## WebAuthn APIs - Assertion
Assertion is the process where an end user request authetication using FIDO2 compliant autheticator. User must have to
register valid credential, before attempting to login using the credential. <br/>

**Assertion Sequence** <br/>
![Assertion Sequence](./artifacts/images/assertion-sequence.png) <br/>

------------------------------------------------------------------------------------------
#### Assertion Start
<details>
<summary><code>POST /webauthn/assertion</code><code>(Assertion Start)</code></summary>

##### Request Body
```json
{
  "username" : "homer.simpson@aol.com"
}
```

**Request Headers**

>| header name        | type     | Description                         |
>|--------------------|----------|-------------------------------------|
>| `X-Forwarded-Host` | `Domain` | `The orign/domain header of client` |
>
##### Responses
**Body** <br/>
See [PublicKeyCredentialRequestOptions](https://www.w3.org/TR/2021/REC-webauthn-2-20210408/#dictdef-publickeycredentialrequestoptions)
```json
{
  "challenge": "rxoprCDDsig01DXdGk7kxRjN-FwYxrNqBLiHVTUotwQ",
  "timeout": 60000,
  "rpId": "red.basswoodid.com:9080",
  "allowCredentials": [
    {
      "type": "public-key",
      "id": "2d751865-dc3d-49b8-81b8-b5eddb7a6e40cmVkLmJhc3N3b29kaWQuY29tOjkwODA",
      "transports": [
        "internal"
      ]
    }
  ],
  "userVerification": "discouraged",
  "extensions": {
    "appid": null,
    "largeBlob": null,
    "uvm": null
  }
}
```
**Response Headers**

> | header name   | type   |
> |---------------|--------|
> | `loginHandle` | `UUID` |
>

**Status Codes**

> | http code | content-type            | response                                      |
> |-----------|-------------------------|-----------------------------------------------|
> | `200`     | `application/json`      | `Retrieves PublicKeyCredentialRequestOptions` |
>
##### Example cURL
```shell
curl --location --request POST 'http://red.basswoodid.com:9080/webauthn/assertion' \
--header 'X-Forwarded-Host: red.basswoodid.com:9080' \
--header 'Content-Type: application/json' \
--data-raw '{
    "username" : "homer.simpson@aol.com"
}'
```
</details>
------------------------------------------------------------------------------------------

------------------------------------------------------------------------------------------
#### Assertion Finish
<details>
<summary><code>POST /webauthn/assertion</code><code>(Assertion Finish)</code></summary>

##### Request Body <br/>
See [AuthenticatorAssertionResponse](https://www.w3.org/TR/2021/REC-webauthn-2-20210408/#iface-authenticatorassertionresponse)
```json
{
  "id": "2d751865-dc3d-49b8-81b8-b5eddb7a6e40cmVkLmJhc3N3b29kaWQuY29tOjkwODA",
  "response": {
    "authenticatorData": "4mO4a-qXlHNmAiDhS1chFBhZWEbSd4AB8yM20C-OtrtFAAAAApkTo1vB-k2nrycO5Vcy73EAMtne-dfOufnXN3fuPW_PvNW_Pm-XnXW-2unuNHJlZC5iYXNzd29vZGlkLmNvbTo5MDgwpQECAyYgASFYIOJ_z6O3ZOVJaxl7ICU5OX85fkMKKkqXPGat35WiQu2oIlggxw2nuQF4Ze5UzvNNVtlMxaBAUwRSmh2DxDkmbuCYneY",
    "clientDataJSON": "eyJjaGFsbGVuZ2UiOiJyeG9wckNERHNpZzAxRFhkR2s3a3hSak4tRndZeHJOcUJMaUhWVFVvdHdRIiwib3JpZ2luIjoicmVkLmJhc3N3b29kaWQuY29tOjkwODAiLCJ0eXBlIjoid2ViYXV0aG4uZ2V0In0",
    "signature": "MEUCIQCB2dPfHJ6DgG7WK0pHuYAFC54E8kuAnSlDYO1W8CDiBgIgVDgIEuRVQdQEER9pbePDXIPJZQUavqUN-R4IyDBuKeo",
    "userHandle": "2d751865-dc3d-49b8-81b8-b5eddb7a6e40"
  },
  "authenticatorAttachment": null,
  "clientExtensionResults": {
    "appid": null,
    "largeBlob": null
  },
  "type": "public-key"
}
```

**Request Headers**

>| header name   | type   | Description                                                     |
>|---------------|--------|-----------------------------------------------------------------|
>| `loginHandle` | `UUID` | `The loginHandle header received from the Assertion Start step` |
>
##### Responses
**Body** <br/>
See [AssertionResult](https://developers.yubico.com/java-webauthn-server/JavaDoc/webauthn-server-core/2.5.0/com/yubico/webauthn/AssertionResult.html)
```json
{
  "success": true,
  "credentialResponse": {
    "id": "110557bf-a5ea-4c5e-aa41-1df8510d92f1cmVkLmJhc3N3b29kaWQuY29tOjkwODA",
    "response": {
      "authenticatorData": "4mO4a-qXlHNmAiDhS1chFBhZWEbSd4AB8yM20C-OtrtFAAAAAt3GK7oH1U9JnA515YKixA8AMtddOee23_muXmvuHOXvmmuNftXX_OddHfdn9XJlZC5iYXNzd29vZGlkLmNvbTo5MDgwpQECAyYgASFYILbRL-DuCs5S1uXAoa31hBzeP_HTGzMNXtejCIJhfTBWIlggXBJUCZltRTS7etWtX0NST9ISOhPGXuUqrR5wX0FIGcU",
      "clientDataJSON": "eyJjaGFsbGVuZ2UiOiJxOXJsLVhTYmZXdkpoX1E5R3VrZlcyLVNMU21DdEppNmY1aFJlWkt2MmQ0Iiwib3JpZ2luIjoicmVkLmJhc3N3b29kaWQuY29tOjkwODAiLCJ0eXBlIjoid2ViYXV0aG4uZ2V0In0",
      "signature": "MEUCICdzLFzimZkyE4Y6aGZrmnuFhsvyAS3rP2DX9DDs4RTjAiEAwht0NJ90DYCmilC88wyWvXBpXyRE2KA2TrD7lytLx_I",
      "userHandle": "110557bf-a5ea-4c5e-aa41-1df8510d92f1"
    },
    "authenticatorAttachment": null,
    "clientExtensionResults": {
      "appid": null,
      "largeBlob": null
    },
    "type": "public-key"
  },
  "credential": {
    "credentialId": "110557bf-a5ea-4c5e-aa41-1df8510d92f1cmVkLmJhc3N3b29kaWQuY29tOjkwODA",
    "userHandle": "110557bf-a5ea-4c5e-aa41-1df8510d92f1",
    "publicKeyCose": "pQECAyYgASFYILbRL-DuCs5S1uXAoa31hBzeP_HTGzMNXtejCIJhfTBWIlggXBJUCZltRTS7etWtX0NST9ISOhPGXuUqrR5wX0FIGcU",
    "signatureCount": 1,
    "backupEligible": null,
    "backupState": null
  },
  "username": "homer.simpson@aol.com",
  "signatureCounterValid": true
}
```
**Response Headers**
None

**Status Codes**

> | http code | content-type            | response                                                                                                                                                    |
> |-----------|-------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
> | `200`     | `application/json`      | Retrieves [AssertionResult](https://developers.yubico.com/java-webauthn-server/JavaDoc/webauthn-server-core/2.5.0/com/yubico/webauthn/AssertionResult.html) |
>
##### Example cURL
```shell
curl --location --request POST 'http://red.basswoodid.com:9080/webauthn/assertion' \
--header 'X-Forwarded-Host: red.basswoodid.com:9080' \
--header 'loginHandle: aabce26b-a2e5-4fef-97eb-dab0c66a36da' \
--header 'Content-Type: application/json' \
--data-raw '{"id":"110557bf-a5ea-4c5e-aa41-1df8510d92f1cmVkLmJhc3N3b29kaWQuY29tOjkwODA","response":{"authenticatorData":"4mO4a-qXlHNmAiDhS1chFBhZWEbSd4AB8yM20C-OtrtFAAAAAt3GK7oH1U9JnA515YKixA8AMtddOee23_muXmvuHOXvmmuNftXX_OddHfdn9XJlZC5iYXNzd29vZGlkLmNvbTo5MDgwpQECAyYgASFYILbRL-DuCs5S1uXAoa31hBzeP_HTGzMNXtejCIJhfTBWIlggXBJUCZltRTS7etWtX0NST9ISOhPGXuUqrR5wX0FIGcU","clientDataJSON":"eyJjaGFsbGVuZ2UiOiJxOXJsLVhTYmZXdkpoX1E5R3VrZlcyLVNMU21DdEppNmY1aFJlWkt2MmQ0Iiwib3JpZ2luIjoicmVkLmJhc3N3b29kaWQuY29tOjkwODAiLCJ0eXBlIjoid2ViYXV0aG4uZ2V0In0","signature":"MEUCICdzLFzimZkyE4Y6aGZrmnuFhsvyAS3rP2DX9DDs4RTjAiEAwht0NJ90DYCmilC88wyWvXBpXyRE2KA2TrD7lytLx_I","userHandle":"110557bf-a5ea-4c5e-aa41-1df8510d92f1"},"authenticatorAttachment":null,"clientExtensionResults":{"appid":null,"largeBlob":null},"type":"public-key"}'
```
</details>
------------------------------------------------------------------------------------------
------------------------------------------------------------------------------------------


# Module MySQL
The WebAuthn module depends on the MySQL 8.2 database for persisting important entities like
[User](/webauthn/src/main/java/io/basswood/webauthn/model/user/User.java), 
[RelyingPartyEntity](/webauthn/src/main/java/io/basswood/webauthn/model/rp/RelyingPartyEntity.java) etc.
This particular module in the [/mysql](/mysql) directory pre-packages a compliant MySQL 8.2 image with the necessary setup already in place. This setup
includes mysql user credential, required schema and tables. The artifacts in this module are described below
1. [sql-01-setup-users.sql](/mysql/sql-01-setup-users.sql): The sql script to create necessary users and grants. The script creates 2 users - ``'basswood'@'%' and 'basswood'@'loclahost'`` - one for remote access and the other for localhost access respectively. The localhost User is used for internal healthcheck   
2. [sql-02-setup-schema.sql](/mysql/sql-02-setup-schema.sql): The sql script that setups the default schema (``webauthn_basswood``) and the necessary tables under this. Review this file to better understand the relational data-model.
3. [health-check.sh](/mysql/health-check.sh): A simple health check script for the database. The script checks if the schema ``webauthn_basswood`` is inplace before exiting with zero (success) exit code.
4. [Dockerfile](/mysql/Dockerfile): The Dockerfile that put all the above artifacts together to produce a MySQL 8.2 image. See the official [MySQL docker guide](https://hub.docker.com/_/mysql) for details about the ``Environment variables``, ``/docker-entrypoint-initdb.d`` and other best practices.
5. [docker-build.sh](/mysql/docker-build.sh): The shell script that builds the docker image using the above artifacts. It creates a local image named ``basswood/mysqldb``  


# Module Authenticator
The authenticator module implements a Virtual Authenticator. The initial motivation for this module came from the
automation need of the [WebAuthn module](/webauthn). The validity of the credential created ([registration](#registration-start))
and later used during the authentication ([assertion](#assertion-finish)) can only be tested by using an actual
FIDO2 compliant authenticator (e.g Yubico Key, Apple FaceId) and a client UI (e.g. Browser/Javascript) application.
But it is impossible to develop automation (integration tests) for these APIs using conventional hardware and UI since
user interaction is a must for authenticators. It is also not trivial to mock an authenticator, since the credential data 
produced by authenticators are complex [CBOR](https://cbor.io/) encoded cryptographic data. Using random mock data for credential
is not feasible, since the mocked data would not be able to verify signatures and other cryptographic contracts
mandated by the FIDO2 specs. <br/>

With the above limitation in mind this virtual authenticator was developed. Unlike real authenticator this virtual authenticator
is a Spring Boot Web application that exposes the authenticator services via HTTP End Points. It primarily supports the following 2 APIs
1. [Credential Create](https://developer.mozilla.org/en-US/docs/Web/API/CredentialsContainer/create) implemented by ``/device/credential/create`` endpoint. <br/>
   As part of the registration process this API can be used to create a credential within an authenticator. The API returns 
   properly encoded cryptographic data that the client can send to the server for registration 
2. [Credential Get](https://developer.mozilla.org/en-US/docs/Web/API/CredentialsContainer/get) implemented by ``/device/credential/get`` endpoint <br/> 
   During assertion process this API can be used to sign the user challenge and return required assertion response that 
   can be sent back to the server for validation.

For a complete end to end working example explore the Postman collection: [basswood-webauthn.postman_collection.json](./postman/basswood-webauthn.postman_collection.json)

## Entities
![ERP Diagram](./artifacts/images/erp-diagram-authenticator.png) <br/>
1. [Device](./authenticator/src/main/java/io/basswood/authenticator/model/Device.java) A Device represents an actual
   device that can have one or more Authenticator attached to it. A device needs to be created first before adding any
   authenticators to it. From test perspective devices offers test isolation by providing different namespace for tests.
   This is further clarified in the API section below.
2. [VirtualAuthenticator](./authenticator/src/main/java/io/basswood/authenticator/model/VirtualAuthenticator.java)
   Core entity of this module that represents an actual authenticator. Like any regular authenticator each VirtualAuthenticator  
   has a unique ``[aaguid](https://fidoalliance.org/specs/fido-v2.0-rd-20180702/fido-metadata-statement-v2.0-rd-20180702.html#authenticator-attestation-guid-aaguid-typedef)``.
   attachment preference, transport type etc. Most importantly this Object implements the [create()](https://developer.mozilla.org/en-US/docs/Web/API/CredentialsContainer/create) 
   and [get()](https://developer.mozilla.org/en-US/docs/Web/API/CredentialsContainer/get) methods to create new credential for the User. 
3. [CredentialRepository](./authenticator/src/main/java/io/basswood/authenticator/model/CredentialRepository.java) A map of 
   Credentials being used by each Virtual Authenticator.
4. [Credential](./authenticator/src/main/java/io/basswood/authenticator/model/Credential.java). A credential represents an
   asymmetric key pair that a User must use to authenticate itself. The public portion of this key, along with other metadata
   is stored in the server as part of the [registration process](#webauthn-apis---registration). During the [assertion flow](#webauthn-apis---assertion)
   this key is used to sign and return the challenge from the server.

## Authenticator REST API

------------------------------------------------------------------------------------------
#### Create a Device
<details>
<summary><code>POST /device</code><code>(Create Device)</code></summary>

**Request Body**
See [DeviceCreateDTO](./authenticator/src/main/java/io/basswood/authenticator/dto/DeviceCreateDTO.java)
```json
{
  "deviceId": "15cae372-9789-4124-98e0-73b3b064e128",
  "displayName": "iOS Device",
  "tags" : ["test1"]
}
```

**Response Body** <br/>
See [Device](./authenticator/src/main/java/io/basswood/authenticator/model/Device.java)
```json
{
  "deviceId": "15cae372-9789-4124-98e0-73b3b064e128",
  "displayName": "iOS Device",
  "tags": [
    "test1"
  ]
}
```

**Status Codes**<br/>

> | http code | content-type            | response  |
> |-----------|-------------------------|-----------|
> | `200`     | `application/json`      | `None`    |
>
##### Example cURL
```shell
curl --location --request POST 'http://red.basswoodid.com:9090/device' \
--header 'Content-Type: application/json' \
--data-raw '{
    "deviceId" : "2afbd0eb-b352-4ac0-88d0-a82a35d94093",
    "displayName": "iOS Device",
    "tags" : ["test1"]
}'
```
</details>
------------------------------------------------------------------------------------------

------------------------------------------------------------------------------------------
#### Create an Authenticator in a Device
<details>
<summary><code>POST /device/{{deviceId}}/authenticator</code><code>(Create an Authenticator)</code></summary>

**Request Path Variable**

> | Path Variable | required | Description            |
> |---------------|----------|------------------------|
> | deviceId      | yes      | `The ID of the device` |
>

**Request Body**
See [AuthenticatorCreateDTO](./authenticator/src/main/java/io/basswood/authenticator/dto/AuthenticatorCreateDTO.java)
```json
{
  "signatureCount" : 1,
  "attachment" : "PLATFORM",
  "transport" : "internal",
  "supportedAlgorithms" : ["RS256"]
}
```

**Response Body** <br/>
See [VirtualAuthenticator](./authenticator/src/main/java/io/basswood/authenticator/model/VirtualAuthenticator.java)
```json
{
  "aaguid": "30480711-d920-46bb-b2bc-ed548f4db9e4",
  "signatureCount": 1,
  "attachment": "platform",
  "transport": "internal",
  "algorithms": [
    "RS256"
  ],
  "key": {
    "kty": "EC",
    "d": "PNJc3JRu2IHA0rMFii3aa0JsfqMBXbLykhoYuFM19mI",
    "use": "sig",
    "crv": "P-256",
    "kid": "f470df23-cdce-4f45-9c2e-8c9ef160d12f",
    "x": "PtLZy2J_UkfdVleS_WYKFeCGxB3MwTEGoq1YgpH8dts",
    "y": "vZGj1hPvaJZvkfSlJFBSl7qo6TnnwAMcQpBn4K5OaB4"
  },
  "repository": []
}
```

**Status Codes**<br/>

> | http code | content-type            | response  |
> |-----------|-------------------------|-----------|
> | `200`     | `application/json`      | `None`    |
>
##### Example cURL
```shell
curl --location --request POST 'http://red.basswoodid.com:9090/device/15cae372-9789-4124-98e0-73b3b064e128/authenticator' \
--header 'Content-Type: application/json' \
--data-raw '{
    "signatureCount" : 1,
    "attachment" : "PLATFORM",
    "transport" : "internal",
    "supportedAlgorithms" : ["RS256"]
}'
```
</details>
------------------------------------------------------------------------------------------

------------------------------------------------------------------------------------------
#### Create Credential
<details>
<summary><code>POST /device/credential/create</code><code>(Create credential - Registration)</code></summary>

**Request Parameters**

> | Parameter   | required | Description                       |
> |-------------|----------|-----------------------------------|
> | deviceId    | yes      | `The ID of the device`            |
> | aaguid      | yes      | `The aaguid of the authenticator` |
>

**Request Body**
See [PublicKeyCredentialCreationOptions](https://www.w3.org/TR/2021/REC-webauthn-2-20210408/#authenticatorassertionresponse)
```json
{
    "rp": {
        "name": "Basswood Red Client",
        "id": "red.basswoodid.com:9080"
    },
    "user": {
        "name": "homer.simpson@aol.com",
        "displayName": "Homer Simpson",
        "id": "c23283ce-23f9-4bf4-8649-eeb1e8bfdf17"
    },
    "challenge": "JMpLAoZKnuS5D3FXuaS2qxPKHnQWgio0_UFjwdoHcS4",
    "pubKeyCredParams": [
        {
            "alg": -7,
            "type": "public-key"
        },
        {
            "alg": -8,
            "type": "public-key"
        },
        {
            "alg": -35,
            "type": "public-key"
        },
        {
            "alg": -36,
            "type": "public-key"
        },
        {
            "alg": -257,
            "type": "public-key"
        },
        {
            "alg": -258,
            "type": "public-key"
        },
        {
            "alg": -259,
            "type": "public-key"
        }
    ],
    "timeout": 60000,
    "excludeCredentials": [],
    "authenticatorSelection": {
        "authenticatorAttachment": "platform",
        "requireResidentKey": false,
        "residentKey": "discouraged",
        "userVerification": "discouraged"
    },
    "attestation": "none",
    "extensions": {
        "appidExclude": null,
        "credProps": true,
        "largeBlob": null,
        "uvm": null
    }
}
```

**Response Body** <br/>
See [AuthenticatorAttestationResponse](https://www.w3.org/TR/2021/REC-webauthn-2-20210408/#authenticatorattestationresponse)
```json
{
  "id": "110557bf-a5ea-4c5e-aa41-1df8510d92f1cmVkLmJhc3N3b29kaWQuY29tOjkwODA",
  "response": {
    "attestationObject": "o2NmbXRkbm9uZWdhdHRTdG10oGhhdXRoRGF0YVi24mO4a-qXlHNmAiDhS1chFBhZWEbSd4AB8yM20C-OtrtFAAAAATBIBxHZIEa7srztVI9NueQAMtddOee23_muXmvuHOXvmmuNftXX_OddHfdn9XJlZC5iYXNzd29vZGlkLmNvbTo5MDgwpQECAyYgASFYIHszgyKKc5uisENgbRc0gaDpEAC9D-RNCSbgagTFb8p2IlggflFAh-K9pYnsK_zb_EEzJpKecJdAm58t5H11QspqnbU",
    "clientDataJSON": "eyJjaGFsbGVuZ2UiOiJQc3FiNWltbDB3UzllZng0VmI4UWhNSXg4N2RYZ3J3NTByajI5WnFWSy1zIiwib3JpZ2luIjoicmVkLmJhc3N3b29kaWQuY29tOjkwODAiLCJ0eXBlIjoid2ViYXV0aG4uY3JlYXRlIn0",
    "transports": [
      "internal"
    ]
  },
  "authenticatorAttachment": null,
  "clientExtensionResults": {
    "appidExclude": null,
    "credProps": null,
    "largeBlob": null
  },
  "type": "public-key"
}
```

**Status Codes**<br/>

> | http code | content-type            | response  |
> |-----------|-------------------------|-----------|
> | `200`     | `application/json`      | `None`    |
>
##### Example cURL
```shell
curl --location --request POST 'http://red.basswoodid.com:9090/device/credential/create?deviceId=15cae372-9789-4124-98e0-73b3b064e128&aaguid=30480711-d920-46bb-b2bc-ed548f4db9e4' \
--header 'X-Forwarded-Host: red.basswoodid.com:9080' \
--header 'Content-Type: application/json' \
--data-raw '{"rp":{"name":"Basswood Red Client","id":"red.basswoodid.com:9080"},"user":{"name":"homer.simpson@aol.com","displayName":"Homer Simpson","id":"ffd2f73d-7465-4597-87e9-5f56a5b8bd0e"},"challenge":"k_5OonvBh7a_tKqrLFMOEREB2_47wvrHWdjGhnFIwtY","pubKeyCredParams":[{"alg":-7,"type":"public-key"},{"alg":-8,"type":"public-key"},{"alg":-35,"type":"public-key"},{"alg":-36,"type":"public-key"},{"alg":-257,"type":"public-key"},{"alg":-258,"type":"public-key"},{"alg":-259,"type":"public-key"}],"timeout":60000,"excludeCredentials":[],"authenticatorSelection":{"authenticatorAttachment":"platform","requireResidentKey":false,"residentKey":"discouraged","userVerification":"discouraged"},"attestation":"none","extensions":{"appidExclude":null,"credProps":true,"largeBlob":null,"uvm":null}}'
```
</details>
------------------------------------------------------------------------------------------

------------------------------------------------------------------------------------------
#### Get Credential
<details>
<summary><code>POST /device/credential/get</code><code>(Create credential - Registration)</code></summary>

**Request Parameters** <br/>

> | Parameter   | required | Description                       |
> |-------------|----------|-----------------------------------|
> | deviceId    | yes      | `The ID of the device`            |
> | aaguid      | yes      | `The aaguid of the authenticator` |
>

**Request Body** <br/>
See [PublicKeyCredentialRequestOptions](https://www.w3.org/TR/2021/REC-webauthn-2-20210408/#dictdef-publickeycredentialrequestoptions)
```json
{
  "challenge": "A8lvQzwPtDdb4scVnNLGdd9belsmKF4Q9pVTFjbPNQg",
  "timeout": 60000,
  "rpId": "red.basswoodid.com:9080",
  "allowCredentials": [
    {
      "type": "public-key",
      "id": "6daa40a5-84a0-4c19-838d-18511a7279c9cmVkLmJhc3N3b29kaWQuY29tOjkwODA",
      "transports": [
        "internal"
      ]
    }
  ],
  "userVerification": "discouraged",
  "extensions": {
    "appid": null,
    "largeBlob": null,
    "uvm": null
  }
}
```

**Response Body** <br/>
See [AuthenticatorAssertionResponse](https://www.w3.org/TR/2021/REC-webauthn-2-20210408/#iface-authenticatorassertionresponse)
```json
{
  "id": "6daa40a5-84a0-4c19-838d-18511a7279c9cmVkLmJhc3N3b29kaWQuY29tOjkwODA",
  "response": {
    "authenticatorData": "4mO4a-qXlHNmAiDhS1chFBhZWEbSd4AB8yM20C-OtrtFAAAAAoWcXUQRxUYzsCjl3IhhDe8AMunWmuNGufvOGtPuHNffvN_HftfOddWu9u_XPXJlZC5iYXNzd29vZGlkLmNvbTo5MDgwpQECAyYgASFYINGqDafAI2upz-CF5T7fgedC10UkG9WCiPtYsE2YgTDTIlgge0gn2rYLyXjaAmEVbqareTjnQ2X6aEMCTMe93qzrBEU",
    "clientDataJSON": "eyJjaGFsbGVuZ2UiOiJBOGx2UXp3UHREZGI0c2NWbk5MR2RkOWJlbHNtS0Y0UTlwVlRGamJQTlFnIiwib3JpZ2luIjoicmVkLmJhc3N3b29kaWQuY29tOjkwODAiLCJ0eXBlIjoid2ViYXV0aG4uZ2V0In0",
    "signature": "MEYCIQDE-A0cowrMq6x3FwqUcPLE7eP0cQkCeYvubOitueQlWQIhAK5dJNdbkfN7Y0htsBrwVUvPD7Koa3dpC6XMgp2QO5ap",
    "userHandle": "6daa40a5-84a0-4c19-838d-18511a7279c9"
  },
  "authenticatorAttachment": null,
  "clientExtensionResults": {
    "appid": null,
    "largeBlob": null
  },
  "type": "public-key"
}
```

**Status Codes**<br/>

> | http code | content-type            | response  |
> |-----------|-------------------------|-----------|
> | `200`     | `application/json`      | `None`    |
>
##### Example cURL
```shell
curl --location --request POST 'http://red.basswoodid.com:9090/device/credential/get?deviceId=f5923c1a-3e4e-42df-b1fb-5bfdcff13e50&aaguid=859c5d44-11c5-4633-b028-e5dc88610def' \
--header 'X-Forwarded-Host: red.basswoodid.com:9080' \
--header 'Content-Type: application/json' \
--data-raw '{"challenge":"A8lvQzwPtDdb4scVnNLGdd9belsmKF4Q9pVTFjbPNQg","timeout":60000,"rpId":"red.basswoodid.com:9080","allowCredentials":[{"type":"public-key","id":"6daa40a5-84a0-4c19-838d-18511a7279c9cmVkLmJhc3N3b29kaWQuY29tOjkwODA","transports":["internal"]}],"userVerification":"discouraged","extensions":{"appid":null,"largeBlob":null,"uvm":null}}'
```
</details>
------------------------------------------------------------------------------------------

# Postman Collection
Example usage of most APIs offered in this project can be found in the [postman collection](./postman/basswood-webauthn.postman_collection.json).
Each folder in the postman collection has attached documentation clarifying the usage and purpose.

## Environment Details
The collection means to be run with the included
Environment file ([basswood-webauthn.postman_environment.json](./postman/basswood-webauthn.postman_environment.json)).
```json
{
	"id": "4761c322-f30f-45f2-8f88-348a678d107c",
	"name": "basswood-webauthn",
	"values": [
		{
			"key": "basswood-webauthn-url-red",
			"value": "http://red.basswoodid.com:9080",
			"enabled": true
		},
		{
			"key": "basswood-authenticator-url",
			"value": "http://red.basswoodid.com:9090",
			"enabled": true
		},
		{
			"key": "origin",
			"value": "red.basswoodid.com:9080",
			"enabled": true
		},
		{
			"key": "jwt",
			"value": "eyJraWQiOiJjMGJkZjRmYi0zZjM5LTQ3YzYtOWViMi04NmMxNDhjZmNhMWUiLCJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJ3ZWJhdXRobl9hZG1pbiIsImF1ZCI6IndlYmF1dGhuLmJhc3N3b29kLmlvIiwibmJmIjoxNzA2MjQxMzA3LCJyb2xlcyI6WyJ1c2VyX21hbmFnZXIiLCJycF9tYW5hZ2VyIiwiandrX21hbmFnZXIiLCJ0b2tlbl9tYW5hZ2VyIl0sImlzcyI6IndlYmF1dGhuLmJhc3N3b29kLmlvIiwiZXhwIjoxOTI0OTkxOTk5LCJpYXQiOjE3MDYyNDEzMDcsImp0aSI6IjcwZGQ0ZGY5LTA2MGMtNGFmZC1iMDg1LWU4NDAzODMzOWJhMSJ9.EuSLWJPAj-ST0MF-W6UB0rTyblHKOx9FLUd2C-AJ4Gt5sBoalvMaVAmFeh04Vo_0fkS7edkCPM5N49Cws_It8w",
			"enabled": true
		}
	],
	"_postman_variable_scope": "environment",
	"_postman_exported_at": "2024-01-30T22:22:40.578Z",
	"_postman_exported_using": "Postman/9.6.1"
}
```

The environment properties, specially the urls needs to be updated accordingly.

> | Variable | Description                                                                                            |
> |----------|--------------------------------------------------------------------------------------------------------|
> |basswood-webauthn-url-red| The URL of the WebAuthn server                                                                         |
> |basswood-authenticator-url| The URL of the virtual autheticator server                                                             |
> |origin| The Relying Party origin url. Usually the domain of the Client Application that need WebAuthn services |
> |jwt| A valid JWT token. Fro example see [jwt-for-testing.txt](./webauthn/src/main/resources/secrets/jwt-for-testing.txt) |
>

# References
1. [Yubico's java-webauthn-server](https://developers.yubico.com/java-webauthn-server/) and at [GitHub](https://github.com/Yubico/java-webauthn-server)
2. [Web Authentication: An API for accessing Public Key Credentials Level 2 (W3C Recommendation)](https://www.w3.org/TR/webauthn/)
3. [WebAuthn Guide](https://webauthn.guide/)
4. [Web Authentication API Documentation at Mozilla](https://developer.mozilla.org/en-US/docs/Web/API/Web_Authentication_API)
