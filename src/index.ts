// Import the native module. On web, it will be resolved to ExpoInAppProvisioning.web.ts
// and on native platforms to ExpoInAppProvisioning.ts
import { Platform } from "react-native";

import ExpoInAppProvisioningModule from "./ExpoInAppProvisioningModule";

export enum AndroidStatusCode {
  TAP_AND_PAY_NO_ACTIVE_WALLET = "TAP_AND_PAY_NO_ACTIVE_WALLET",
  TAP_AND_PAY_TOKEN_NOT_FOUND = "TAP_AND_PAY_TOKEN_NOT_FOUND",
  TAP_AND_PAY_INVALID_TOKEN_STATE = "TAP_AND_PAY_INVALID_TOKEN_STATE",
  TAP_AND_PAY_ATTESTATION_ERROR = "TAP_AND_PAY_ATTESTATION_ERROR",
  TAP_AND_PAY_UNAVAILABLE = "TAP_AND_PAY_UNAVAILABLE",
}

export enum StatusCode {
  AVAILABLE = "AVAILABLE",
  UNAVAILABLE = "UNAVAILABLE",
  NEEDS_SETUP = "NEEDS_SETUP",
}

const AndroidStatusCodeMap = {
  [AndroidStatusCode.TAP_AND_PAY_ATTESTATION_ERROR]: StatusCode.UNAVAILABLE,
  [AndroidStatusCode.TAP_AND_PAY_INVALID_TOKEN_STATE]: StatusCode.AVAILABLE,
  [AndroidStatusCode.TAP_AND_PAY_NO_ACTIVE_WALLET]: StatusCode.NEEDS_SETUP,
  [AndroidStatusCode.TAP_AND_PAY_TOKEN_NOT_FOUND]: StatusCode.AVAILABLE,
  [AndroidStatusCode.TAP_AND_PAY_UNAVAILABLE]: StatusCode.UNAVAILABLE,
};

type InitializeProps = {
  cardholderName: string;
  localizedDescription: string;
  lastFour: string;
  cardId: string;
};

type IOSInitializeResult = {
  leafCertificate: string;
  nonce: string;
  nonceSignature: string;
  subCACertificate: string;
};

type AndroidInitializeResult = {
  walletId: string | null;
  hardwareId: string | null;
};

type InitializeResult = AndroidInitializeResult | IOSInitializeResult;

export const initialize = async (
  props?: InitializeProps,
): Promise<InitializeResult> => {
  if (Platform.OS === "ios") {
    const { leafCertificate, nonce, nonceSignature, subCACertificate } =
      await ExpoInAppProvisioningModule.presentAddPaymentPassViewController(
        props?.cardholderName,
        props?.localizedDescription,
        props?.lastFour,
        props?.cardId,
      );
    return { leafCertificate, nonce, nonceSignature, subCACertificate };
  }
  let hardwareId: string | null = null;
  const walletId = await getActiveWalletId();
  if (walletId) {
    hardwareId = await getStableHardwareId();
  }
  return { walletId, hardwareId };
};

export const openWallet = async (): Promise<boolean> => {
  if (Platform.OS === "ios") {
    return await ExpoInAppProvisioningModule.openWallet();
  }
  return false;
};

export const canAddCard = async (token: string): Promise<StatusCode> => {
  if (Platform.OS === "ios") {
    const isAvailable = await ExpoInAppProvisioningModule.isAvailable();
    if (!isAvailable) {
      return StatusCode.UNAVAILABLE;
    }
    if (await ExpoInAppProvisioningModule.canAddCard(token)) {
      return StatusCode.AVAILABLE;
    }
    return StatusCode.NEEDS_SETUP;
  }
  return await getTokenStatus(token);
};

const getTokenStatus = async (token: string): Promise<StatusCode> => {
  if (Platform.OS === "ios") {
    return StatusCode.UNAVAILABLE;
  }
  const status = await ExpoInAppProvisioningModule.getTokenStatus(token);
  return AndroidStatusCodeMap[status];
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
      ephemeralPublicKey,
      encryptedPassData,
    );
  }

  const {
    opc,
    name,
    lastDigits,
    address,
    city,
    state,
    countryCode,
    postalCode,
    phone,
  } = props as AndroidPushProvisionProps;

  return await ExpoInAppProvisioningModule.pushProvision(
    opc,
    name,
    lastDigits,
    address,
    city,
    state,
    countryCode,
    postalCode,
    phone,
  );
};
