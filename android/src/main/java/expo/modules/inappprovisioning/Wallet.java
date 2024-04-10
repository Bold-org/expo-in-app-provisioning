package expo.modules.inappprovisioning;

import android.app.Activity;
import android.content.Intent;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ActivityEventListener;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tapandpay.issuer.UserAddress;
import com.google.android.gms.tapandpay.TapAndPay;
import com.google.android.gms.tapandpay.issuer.PushTokenizeRequest;
import com.google.android.gms.tapandpay.issuer.TokenStatus;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.HashMap;
import static com.google.android.gms.tapandpay.TapAndPay.TOKEN_PROVIDER_VISA;
import static com.google.android.gms.tapandpay.TapAndPay.CARD_NETWORK_VISA;
import static com.google.android.gms.tapandpay.TapAndPayStatusCodes.TAP_AND_PAY_ATTESTATION_ERROR;
import static com.google.android.gms.tapandpay.TapAndPayStatusCodes.TAP_AND_PAY_INVALID_TOKEN_STATE;
import static com.google.android.gms.tapandpay.TapAndPayStatusCodes.TAP_AND_PAY_NO_ACTIVE_WALLET;
import static com.google.android.gms.tapandpay.TapAndPayStatusCodes.TAP_AND_PAY_TOKEN_NOT_FOUND;
import static com.google.android.gms.tapandpay.TapAndPayStatusCodes.TAP_AND_PAY_UNAVAILABLE;

import com.google.android.gms.tapandpay.TapAndPayClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Wallet extends ReactContextBaseJavaModule implements ActivityEventListener {

    private static ReactApplicationContext reactContext;
    private static final int REQUEST_CODE_TOKENIZE = 1;
    static final int REQUEST_CODE_PUSH_TOKENIZE = 3;
    private static final int REQUEST_CREATE_WALLET = 4;
    private static final int SET_DEFAULT_PAYMENTS_REQUEST_CODE = 5;
    private TapAndPayClient tapAndPayClient;

    private HashMap<Integer, String> tapAndPayStringMap = new HashMap<>();
    private HashMap<Integer, String> tapAndPayStatusCodeStringMap = new HashMap<>();

    @Override
    public void onActivityResult(Activity activity, int i, int i1, @Nullable Intent intent) {

    }

    @Override
    public void onNewIntent(Intent intent) {

    }

    @NonNull
    @Override
    public String getName() {
        return "WalletModule";
    }

    public Wallet(ReactApplicationContext context) {
        super(context);
        reactContext = getReactApplicationContext();
        context.addActivityEventListener(this);

        initialiseTapAndPayStatusCodeHashMaps();

        tapAndPayClient = TapAndPay.getClient(reactContext);
    }

    /**
     * This gets the active wallet ID using the Push Provisioning SDK.
     * See the documentation here:
     * https://developers.google.com/pay/issuers/apis/push-provisioning/android/reading-wallet#getactivewalletid
     */
    public void getActiveWalletId(final Promise promise) {
        tapAndPayClient
                .getActiveWalletId()
                .addOnCompleteListener(
                        new OnCompleteListener<String>() {
                            @Override
                            public void onComplete(@NonNull Task<String> task) {
                                if (task.isSuccessful()) {
                                    String walletId = task.getResult();
                                    promise.resolve(walletId);
                                } else {
                                    ApiException apiException = (ApiException) task.getException();
                                    if (apiException.getStatusCode() == TAP_AND_PAY_NO_ACTIVE_WALLET) {
                                        // If no Google Pay wallet is found, create one and then call
                                        // getActiveWalletId() again.
                                        createWallet();
                                        getActiveWalletId(promise);
                                    } else {
                                        promise.reject("ACTIVE_WALLET_ERROR", "Could not get active wallet id  " + apiException.getStatus().getStatusMessage());
                                    }
                                }
                            }
                        });
    }

    /**
     * If there is no Google Pay wallet on the device (this can happen if the user hasn't opened
     * Google Pay before) and you need the wallet ID to carry out the Push Provisioning, you can
     * create a wallet using the Push Provisioning SDK using this method. See the documentation here:
     * https://developers.google.com/pay/issuers/apis/push-provisioning/android/wallet-operations#create_wallet
     */
    private void createWallet() {
        tapAndPayClient.createWallet(reactContext.getCurrentActivity(), REQUEST_CREATE_WALLET);
    }

    /**
     * This gets the stable hardware ID using the Push Provisioning SDK.
     * See the documentation here:
     * https://developers.google.com/pay/issuers/apis/push-provisioning/android/reading-wallet#getstablehardwareid
     */
    public void getStableHardwareId(final Promise promise) {
        tapAndPayClient
                .getStableHardwareId()
                .addOnCompleteListener(
                        new OnCompleteListener<String>() {
                            @Override
                            public void onComplete(@NonNull Task<String> task) {
                                if (task.isSuccessful()) {
                                    String hardwareId = task.getResult();
                                    promise.resolve(hardwareId);
                                } else {
                                    promise.reject("STABLE_HARDWARE_ERROR", "Could not get stable hardware id");
                                }
                            }
                        });
    }
    /**
     * This method checks the status of a given token.
     * See the documentation here:
     * https://developers.google.com/pay/issuers/apis/push-provisioning/android/reading-wallet#gettokenstatus
     */
    public void getTokenStatus(String token, final Promise promise) {
            // Call to check the status of the input token
            tapAndPayClient
                    .getTokenStatus(0, token)
                    .addOnCompleteListener(
                            new OnCompleteListener<TokenStatus>() {
                                @Override
                                public void onComplete(@NonNull Task<TokenStatus> task) {
                                    if (task.isSuccessful()) {
                                        @TapAndPay.TokenState int tokenStateInt = task.getResult().getTokenState();
                                        promise.resolve(getTokenStateString(tokenStateInt));
                                    } else {
                                        ApiException apiException = (ApiException) task.getException();
                                        if (apiException.getStatusCode() == TAP_AND_PAY_TOKEN_NOT_FOUND) {
                                            // failed
                                            promise.reject("TOKEN_STATUS_ERROR", "Token not found");
                                        } else {
                                            promise.reject("TOKEN_STATUS_ERROR", "Could not get token status  " + apiException.getStatus().getStatusMessage());
                                        }
                                    }
                                }
                            });
    }

    /**
     * This method uses the Push Provisioning SDK to start the Push Provisioning flow. In it, we
     * create a UserAddress object (required) and a PushTokenizeRequest object. The Push Provision is
     * started using the TapAndPay object we created earlier.
     * See the documentation here:
     * https://developers.google.com/pay/issuers/apis/push-provisioning/android/wallet-operations#push_provisioning
     */
    public void pushProvision(final String opc, String name, String lastDigits, String address, String city, String state,
                               String countryCode, String postalCode, String phone, final Promise promise) {
            UserAddress userAddress =
                    UserAddress.newBuilder()
                            .setName(name)
                            .setAddress1(address)
                            .setLocality(city)
                            .setAdministrativeArea(state)
                            .setCountryCode(countryCode)
                            .setPostalCode(postalCode)
                            .setPhoneNumber(phone)
                            .build();

            PushTokenizeRequest pushTokenizeRequest =
                    new PushTokenizeRequest.Builder()
                            .setOpaquePaymentCard(opc.getBytes())
                            .setNetwork(CARD_NETWORK_VISA)
                            .setTokenServiceProvider(TOKEN_PROVIDER_VISA)
                            .setDisplayName(name)
                            .setLastDigits(lastDigits)
                            .setUserAddress(userAddress)
                            .build();

            tapAndPayClient.pushTokenize(reactContext.getCurrentActivity(), pushTokenizeRequest, REQUEST_CODE_PUSH_TOKENIZE);
            promise.resolve(true);
    }

    /**
     * Method to convert the TapAndPay token state constants into a String.
     *
     * @param tokenStateInt - The TapAndPay constant used to indicate token state
     * @return - The String representation of the TapAndPay token state constant.
     */
    private String getTokenStateString(int tokenStateInt) {
        if (tapAndPayStringMap.containsKey(tokenStateInt)) {
            return tapAndPayStringMap.get(tokenStateInt);
        }

        if (tapAndPayStatusCodeStringMap.containsKey(tokenStateInt)) {
            return tapAndPayStatusCodeStringMap.get(tokenStateInt);
        }

        return "Error fetching token state";
    }

    /**
     * Initialise two HashMaps which contain the TapAndPay token status state codes. This is so we can
     * convert the status code to the String value to return.
     */
    private void initialiseTapAndPayStatusCodeHashMaps() {
        tapAndPayStringMap.put(TapAndPay.TOKEN_STATE_UNTOKENIZED, "TOKEN_STATE_UNTOKENIZED");
        tapAndPayStringMap.put(TapAndPay.TOKEN_STATE_PENDING, "TOKEN_STATE_PENDING");
        tapAndPayStringMap.put(
                TapAndPay.TOKEN_STATE_NEEDS_IDENTITY_VERIFICATION,
                "TOKEN_STATE_NEEDS_IDENTITY_VERIFICATION");
        tapAndPayStringMap.put(TapAndPay.TOKEN_STATE_SUSPENDED, "TOKEN_STATE_SUSPENDED");
        tapAndPayStringMap.put(TapAndPay.TOKEN_STATE_ACTIVE, "TOKEN_STATE_ACTIVE");
        tapAndPayStringMap.put(
                TapAndPay.TOKEN_STATE_FELICA_PENDING_PROVISIONING,
                "TOKEN_STATE_FELICA_PENDING_PROVISIONING");

        tapAndPayStatusCodeStringMap.put(TAP_AND_PAY_NO_ACTIVE_WALLET, "TAP_AND_PAY_NO_ACTIVE_WALLET");
        tapAndPayStatusCodeStringMap.put(TAP_AND_PAY_TOKEN_NOT_FOUND, "TAP_AND_PAY_TOKEN_NOT_FOUND");
        tapAndPayStatusCodeStringMap.put(
                TAP_AND_PAY_INVALID_TOKEN_STATE, "TAP_AND_PAY_INVALID_TOKEN_STATE");
        tapAndPayStatusCodeStringMap.put(
                TAP_AND_PAY_ATTESTATION_ERROR, "TAP_AND_PAY_ATTESTATION_ERROR");
        tapAndPayStatusCodeStringMap.put(TAP_AND_PAY_UNAVAILABLE, "TAP_AND_PAY_UNAVAILABLE");
    }
}
