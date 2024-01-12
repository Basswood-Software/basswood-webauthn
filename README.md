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
- [Licence](#license)
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

# Security and Caveats
The project does not require or any form security restriction on the APIs. All APIs are 

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

##### Responses

> | http code | content-type            | response                                   |
> |-----------|-------------------------|--------------------------------------------|
> | `200`     | `application/json`      | `Retrieves a User by userHandle (id)`      |
> | `400`     | `application/json`      | `{"errorCode":"bad-request","message":""}` |
>
##### Example cURL
```shell
curl --location --request GET 'http://red.basswoodid.com:9080/user/caebd9256fa8447a8b1456705619a2c9'
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

# License
[//]: # (TODO)

# References
1. [Yubico's java-webauthn-server](https://developers.yubico.com/java-webauthn-server/) and at [GitHub](https://github.com/Yubico/java-webauthn-server)
2. [Web Authentication: An API for accessing Public Key Credentials Level 2 (W3C Recommendation)](https://www.w3.org/TR/webauthn/)
3. [WebAuthn Guide](https://webauthn.guide/)
4. [Web Authentication API Documentation at Mozilla](https://developer.mozilla.org/en-US/docs/Web/API/Web_Authentication_API)
