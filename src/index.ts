import { Platform } from "react-native";

import ExpoInAppProvisioningModule from "./ExpoInAppProvisioningModule";

export enum AndroidTokenStateError {
  TAP_AND_PAY_NO_ACTIVE_WALLET = "TAP_AND_PAY_NO_ACTIVE_WALLET",
  TAP_AND_PAY_TOKEN_NOT_FOUND = "TAP_AND_PAY_TOKEN_NOT_FOUND",
  TAP_AND_PAY_INVALID_TOKEN_STATE = "TAP_AND_PAY_INVALID_TOKEN_STATE",
  TAP_AND_PAY_ATTESTATION_ERROR = "TAP_AND_PAY_ATTESTATION_ERROR",
  TAP_AND_PAY_UNAVAILABLE = "TAP_AND_PAY_UNAVAILABLE",
}

export enum AndroidTokenState {
  TOKEN_STATE_NEEDS_IDENTITY_VERIFICATION = "TOKEN_STATE_NEEDS_IDENTITY_VERIFICATION",
  TOKEN_STATE_PENDING = "TOKEN_STATE_PENDING",
  TOKEN_STATE_ACTIVE = "TOKEN_STATE_ACTIVE",
  TOKEN_STATE_SUSPENDED = "TOKEN_STATE_SUSPENDED",
  TOKEN_STATE_FELICA_PENDING_PROVISIONING = "TOKEN_STATE_FELICA_PENDING_PROVISIONING",
  TOKEN_STATE_UNTOKENIZED = "TOKEN_STATE_UNTOKENIZED",
}

export type InitializeProps = {
  cardholderName: string;
  localizedDescription: string;
  lastFour: string;
  cardId: string;
};

export type IOSInitializeResult = {
  leafCertificate: string;
  nonce: string;
  nonceSignature: string;
  subCACertificate: string;
};

export type AndroidInitializeResult = {
  walletId: string | null;
  hardwareId: string | null;
};

export type InitializeResult = AndroidInitializeResult | IOSInitializeResult;

export const initialize = async (
  props?: InitializeProps,
): Promise<InitializeResult | null> => {
  if (Platform.OS === "ios") {
    if (!props) {
      throw new Error("iOS requires props");
    }
    const res =
      (await ExpoInAppProvisioningModule.presentAddPaymentPassViewController(
        props?.cardholderName,
        props?.localizedDescription,
        props?.lastFour,
        props?.cardId,
      )) as IOSInitializeResult | null;
    return res;
  }
  let hardwareId: string | null = null;
  const walletId = await getActiveWalletId();
  if (walletId) {
    hardwareId = await getStableHardwareId();
  }
  return { walletId, hardwareId };
};

export const dismiss = async (): Promise<boolean> => {
  if (Platform.OS === "ios") {
    await ExpoInAppProvisioningModule.dismissAddPaymentPassViewController();
    return true;
  }
  return false;
};

export const openWallet = async (): Promise<boolean> => {
  if (Platform.OS === "ios") {
    return await ExpoInAppProvisioningModule.openWallet();
  }
  return false;
};

export const canAddCard = async (token?: string): Promise<boolean> => {
  if (Platform.OS === "ios" || !token) {
    return await ExpoInAppProvisioningModule.canAddCard(token);
  }
  try {
    const status = await getTokenStatus(token);
    return [
      AndroidTokenState.TOKEN_STATE_NEEDS_IDENTITY_VERIFICATION,
      AndroidTokenState.TOKEN_STATE_PENDING,
      AndroidTokenState.TOKEN_STATE_UNTOKENIZED,
    ].includes(status as AndroidTokenState);
  } catch (e) {
    if (e.message === "Token not found") {
      return true;
    }
    throw e;
  }
};

export const getTokenStatus = async (
  token: string,
): Promise<AndroidTokenState | AndroidTokenStateError> => {
  if (Platform.OS === "ios") {
    throw new Error("Not supported on iOS");
  }
  return await ExpoInAppProvisioningModule.getTokenStatus(token);
};

const getActiveWalletId = async (): Promise<string> => {
  if (Platform.OS === "ios") {
    throw new Error("Not supported on iOS");
  }
  return await ExpoInAppProvisioningModule.getActiveWalletId();
};

const getStableHardwareId = async (): Promise<string> => {
  if (Platform.OS === "ios") {
    throw new Error("Not supported on iOS");
  }
  return await ExpoInAppProvisioningModule.getStableHardwareId();
};

type AndroidPushProvisionProps = {
  opc: string;
  name: string;
  lastDigits: string;
  address: string;
  city: string;
  state: string;
  countryCode: string;
  postalCode: string;
  phone: string;
};

type IOSPushProvisionProps = {
  activationData: string;
  ephemeralPublicKey: string;
  encryptedPassData: string;
};

type PushProvisionProps = IOSPushProvisionProps | AndroidPushProvisionProps;

export const pushProvision = async (
  props: PushProvisionProps,
): Promise<boolean> => {
  if (Platform.OS === "ios") {
    const { activationData, ephemeralPublicKey, encryptedPassData } =
      props as IOSPushProvisionProps;
    return await ExpoInAppProvisioningModule.pushProvision(
      activationData,
      encryptedPassData,
      ephemeralPublicKey,
    );
  }

  return await ExpoInAppProvisioningModule.pushProvision(props);
};
