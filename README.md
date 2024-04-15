# expo-in-app-provisioning

This lib provides In App Proviosining for payment passes on Android and iOS.

# Installation

```
npm install github:Bold-org/expo-in-app-provisioning
```

# API documentation

### `canAddCard`

#### Description:

Checks if a card can be added for provisioning.

#### Parameters:

- `token` (string): The token associated with the card.

#### Returns:

A promise that resolves to a `StatusCode` indicating the status of card addition.

---

### `initialize`

#### Description:

Initializes the payment pass provisioning process. On iOS, presents the add payment pass view controller for provisioning. On Android, retrieves the active wallet ID and stable hardware ID if available.

#### Parameters:

- `props` (optional): An object containing initialization properties.
  - `cardholderName` (string): The name of the cardholder.
  - `localizedDescription` (string): A localized description of the card.
  - `lastFour` (string): The last four digits of the card.
  - `cardId` (string): The ID of the card.

#### Returns:

A promise that resolves to an `InitializeResult` object on success or `null` if the initialization fails.

---

### `dismiss`

#### Description:

Dismisses the add payment pass view controller on iOS. `iOS only`

#### Parameters:

None

#### Returns:

A promise that resolves to `true` if the dismissal is successful, `false` otherwise.

---

### `pushProvision`

#### Description:

Initiates the push provisioning process for adding a payment pass.

#### Parameters:

- `props` (object): An object containing provisioning properties.
  - For iOS:
    - `activationData` (string): The activation data.
    - `ephemeralPublicKey` (string): The ephemeral public key.
    - `encryptedPassData` (string): The encrypted pass data.
  - For Android:
    - `opc` (string): The Opaque Payment Card data.
    - `name` (string): The name associated with the card.
    - `lastDigits` (string): The last four digits of the card.
    - `address` (string): The address associated with the card.
    - `city` (string): The city associated with the card.
    - `state` (string): The state associated with the card.
    - `countryCode` (string): The country code associated with the card.
    - `postalCode` (string): The postal code associated with the card.
    - `phone` (string): The phone number associated with the card.

---

### `openWallet`

#### Description:

Opens the wallet app. `iOS only`

#### Parameters:

None

#### Returns:

A promise that resolves to `true` if the wallet app is successfully opened, `false` otherwise.

---

#### Returns:

A promise that resolves to `true` if the push provisioning is successful, `false` otherwise.

---

### Enums

#### `AndroidStatusCode`

| Enum Value                        | Description                                    |
| --------------------------------- | ---------------------------------------------- |
| `TAP_AND_PAY_NO_ACTIVE_WALLET`    | Indicates no active wallet for tap and pay.    |
| `TAP_AND_PAY_TOKEN_NOT_FOUND`     | Indicates token not found for tap and pay.     |
| `TAP_AND_PAY_INVALID_TOKEN_STATE` | Indicates invalid token state for tap and pay. |
| `TAP_AND_PAY_ATTESTATION_ERROR`   | Indicates attestation error for tap and pay.   |
| `TAP_AND_PAY_UNAVAILABLE`         | Indicates tap and pay is unavailable.          |

#### `StatusCode`

| Enum Value    | Description                |
| ------------- | -------------------------- |
| `AVAILABLE`   | Indicates availability.    |
| `UNAVAILABLE` | Indicates unavailability.  |
| `NEEDS_SETUP` | Indicates setup is needed. |
