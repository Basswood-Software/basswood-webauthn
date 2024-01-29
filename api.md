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
