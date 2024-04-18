package expo.modules.inappprovisioning

import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tapandpay.TapAndPay
import com.google.android.gms.tapandpay.TapAndPay.TokenState
import com.google.android.gms.tapandpay.TapAndPayClient
import com.google.android.gms.tapandpay.TapAndPayStatusCodes
import com.google.android.gms.tapandpay.issuer.PushTokenizeRequest
import com.google.android.gms.tapandpay.issuer.UserAddress
import expo.modules.kotlin.Promise
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record

data class ProvisionParams(
  @Field val opc: String,
  @Field val name: String?,
  @Field val lastDigits: String?,
  @Field val address: String?,
  @Field val city: String?,
  @Field val state: String?,
  @Field val countryCode: String?,
  @Field val postalCode: String?,
  @Field val phone: String?
) : Record

class ExpoInAppProvisioningModule : Module() {
  private var tapAndPayClient: TapAndPayClient? = null
  private val tapAndPayStringMap = HashMap<Int, String>()
  private val tapAndPayStatusCodeStringMap = HashMap<Int, String>()
  private val REQUEST_CODE_TOKENIZE = 1
  private val REQUEST_CODE_PUSH_TOKENIZE = 3
  private val REQUEST_CREATE_WALLET = 4
  private val SET_DEFAULT_PAYMENTS_REQUEST_CODE = 5

  override fun definition() = ModuleDefinition {

    initialiseTapAndPayStatusCodeHashMaps()

    tapAndPayClient = TapAndPay.getClient(super.appContext.reactContext!!)

    Name("ExpoInAppProvisioning")

    AsyncFunction("canAddCard") { token: String, promise: Promise ->
        canAddToken(token, promise)
    }
    
    AsyncFunction("getTokenStatus") { token: String, promise: Promise ->
        getTokenStatus(token, promise)
    }

    AsyncFunction("getActiveWalletId") { promise: Promise ->
      getActiveWalletId(promise)
    }

    AsyncFunction("getStableHardwareId") { promise: Promise ->
      getStableHardwareId(promise)
    }

    AsyncFunction("pushProvision") { params: ProvisionParams, promise: Promise ->
      pushProvision(params.opc, params.name, params.lastDigits, params.address, params.city, params.state,
        params.countryCode, params.postalCode, params.phone, promise)
    }
  }

  /**
   * This gets the active wallet ID using the Push Provisioning SDK.
   * See the documentation here:
   * https://developers.google.com/pay/issuers/apis/push-provisioning/android/reading-wallet#getactivewalletid
   */
  fun getActiveWalletId(promise: Promise) {
    tapAndPayClient
            ?.activeWalletId
            ?.addOnCompleteListener { task ->
              if (task.isSuccessful) {
                val walletId = task.result
                promise.resolve(walletId)
              } else {
                val apiException = task.exception as ApiException?
                if (apiException!!.statusCode == TapAndPayStatusCodes.TAP_AND_PAY_NO_ACTIVE_WALLET) {
                  // If no Google Pay wallet is found, create one and then call
                  // getActiveWalletId() again.
                  createWallet()
                  getActiveWalletId(promise)
                } else {
                  promise.reject("ACTIVE_WALLET_ERROR", "Could not get active wallet id  " + apiException.status.statusMessage, Error("ACTIVE_WALLET_ERROR"))
                }
              }
            }
  }

  /**
   * If there is no Google Pay wallet on the device (this can happen if the user hasn't opened
   * Google Pay before) and you need the wallet ID to carry out the Push Provisioning, you can
   * create a wallet using the Push Provisioning SDK using this method. See the documentation here:
   * https://developers.google.com/pay/issuers/apis/push-provisioning/android/wallet-operations#create_wallet
   */
  private fun createWallet() {
    tapAndPayClient!!.createWallet(appContext.currentActivity!!, REQUEST_CREATE_WALLET)
  }

  /**
   * This gets the stable hardware ID using the Push Provisioning SDK.
   * See the documentation here:
   * https://developers.google.com/pay/issuers/apis/push-provisioning/android/reading-wallet#getstablehardwareid
   */
  fun getStableHardwareId(promise: Promise) {
    tapAndPayClient
            ?.stableHardwareId
            ?.addOnCompleteListener { task ->
              if (task.isSuccessful) {
                val hardwareId = task.result
                promise.resolve(hardwareId)
              } else {
                promise.reject("STABLE_HARDWARE_ERROR", "Could not get stable hardware id", Error("STABLE_HARDWARE_ERROR"))
              }
            }
  }

  fun canAddToken(tokenString: String?, promise: Promise) {
    tapAndPayClient
            ?.listTokens()
            ?.addOnCompleteListener { task ->
              if (task.isSuccessful) {
                var found = false
                for (token in task.result) {
                  found = token.issuerTokenId == tokenString
                  if (found) {
                    break
                  }
                }
                promise.resolve(!found)
              } else {
                promise.reject("CAN_ADD_TOKEN_ERROR", "Could not verify token existence in Google Pay: " + task.exception?.message, Error("CAN_ADD_TOKEN_ERROR"))
              }
            }
  }

  /**
   * This method checks the status of a given token.
   * See the documentation here:
   * https://developers.google.com/pay/issuers/apis/push-provisioning/android/reading-wallet#gettokenstatus
   */
  fun getTokenStatus(token: String?, promise: Promise) {
    // Call to check the status of the input token
    tapAndPayClient
            ?.getTokenStatus(TapAndPay.TOKEN_PROVIDER_VISA, token!!)
            ?.addOnCompleteListener { task ->
              if (task.isSuccessful) {
                @TokenState val tokenStateInt = task.result.tokenState
                promise.resolve(getTokenStateString(tokenStateInt))
              } else {
                val apiException = task.exception as ApiException?
                if (apiException!!.statusCode == TapAndPayStatusCodes.TAP_AND_PAY_TOKEN_NOT_FOUND) {
                  promise.reject("TOKEN_STATUS_ERROR", "Token not found", Error("TOKEN_STATUS_ERROR"))
                } else {
                  promise.reject("TOKEN_STATUS_ERROR", "Could not get token status: " + apiException.status.statusMessage, Error("TOKEN_STATUS_ERROR"))
                }
              }
            }
  }

  /**
   * This method uses the Push Provisioning SDK to start the Push Provisioning flow. In it, we
   * create a UserAddress object (required) and a PushTokenizeRequest object. The Push Provision is
   * started using the TapAndPay object we created earlier.
   * See the documentation here:
   * https://developers.google.com/pay/issuers/apis/push-provisioning/android/wallet-operations#push_provisioning
   */
  fun pushProvision(opc: String, name: String?, lastDigits: String?, address: String?, city: String?, state: String?,
                    countryCode: String?, postalCode: String?, phone: String?, promise: Promise) {
    val userAddress = UserAddress.newBuilder()
            .setName(name!!)
            .setAddress1(address!!)
            .setLocality(city!!)
            .setAdministrativeArea(state!!)
            .setCountryCode(countryCode!!)
            .setPostalCode(postalCode!!)
            .setPhoneNumber(phone!!)
            .build()
    val pushTokenizeRequest = PushTokenizeRequest.Builder()
            .setOpaquePaymentCard(opc.toByteArray())
            .setNetwork(TapAndPay.CARD_NETWORK_VISA)
            .setTokenServiceProvider(TapAndPay.TOKEN_PROVIDER_VISA)
            .setDisplayName(name)
            .setLastDigits(lastDigits!!)
            .setUserAddress(userAddress)
            .build()
    tapAndPayClient!!.pushTokenize(appContext.currentActivity!!, pushTokenizeRequest, REQUEST_CODE_PUSH_TOKENIZE)
    promise.resolve(true)
  }
  /**
   * Method to convert the TapAndPay token state constants into a String.
   *
   * @param tokenStateInt - The TapAndPay constant used to indicate token state
   * @return - The String representation of the TapAndPay token state constant.
   */
  private fun getTokenStateString(tokenStateInt: Int): String? {
    if (tapAndPayStringMap.containsKey(tokenStateInt)) {
      return tapAndPayStringMap[tokenStateInt]
    }
    return if (tapAndPayStatusCodeStringMap.containsKey(tokenStateInt)) {
      tapAndPayStatusCodeStringMap[tokenStateInt]
    } else "Error fetching token state"
  }

  /**
   * Initialise two HashMaps which contain the TapAndPay token status state codes. This is so we can
   * convert the status code to the String value to return.
   */
  private fun initialiseTapAndPayStatusCodeHashMaps() {
    tapAndPayStringMap[TapAndPay.TOKEN_STATE_UNTOKENIZED] = "TOKEN_STATE_UNTOKENIZED"
    tapAndPayStringMap[TapAndPay.TOKEN_STATE_PENDING] = "TOKEN_STATE_PENDING"
    tapAndPayStringMap[TapAndPay.TOKEN_STATE_NEEDS_IDENTITY_VERIFICATION] = "TOKEN_STATE_NEEDS_IDENTITY_VERIFICATION"
    tapAndPayStringMap[TapAndPay.TOKEN_STATE_SUSPENDED] = "TOKEN_STATE_SUSPENDED"
    tapAndPayStringMap[TapAndPay.TOKEN_STATE_ACTIVE] = "TOKEN_STATE_ACTIVE"
    tapAndPayStringMap[TapAndPay.TOKEN_STATE_FELICA_PENDING_PROVISIONING] = "TOKEN_STATE_FELICA_PENDING_PROVISIONING"
    tapAndPayStatusCodeStringMap[TapAndPayStatusCodes.TAP_AND_PAY_NO_ACTIVE_WALLET] = "TAP_AND_PAY_NO_ACTIVE_WALLET"
    tapAndPayStatusCodeStringMap[TapAndPayStatusCodes.TAP_AND_PAY_TOKEN_NOT_FOUND] = "TAP_AND_PAY_TOKEN_NOT_FOUND"
    tapAndPayStatusCodeStringMap[TapAndPayStatusCodes.TAP_AND_PAY_INVALID_TOKEN_STATE] = "TAP_AND_PAY_INVALID_TOKEN_STATE"
    tapAndPayStatusCodeStringMap[TapAndPayStatusCodes.TAP_AND_PAY_ATTESTATION_ERROR] = "TAP_AND_PAY_ATTESTATION_ERROR"
    tapAndPayStatusCodeStringMap[TapAndPayStatusCodes.TAP_AND_PAY_UNAVAILABLE] = "TAP_AND_PAY_UNAVAILABLE"
  }
}
