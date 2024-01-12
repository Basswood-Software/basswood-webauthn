
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