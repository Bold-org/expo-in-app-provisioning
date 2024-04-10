// Import the native module. On web, it will be resolved to ExpoInAppProvisioning.web.ts
// and on native platforms to ExpoInAppProvisioning.ts
import { Platform } from "react-native";

import ExpoInAppProvisioningModule from "./ExpoInAppProvisioningModule";

export const getTokenStatus = async (token: string): Promise<string> => {
  if (Platform.OS === "ios") {
    return "";
  }
  return await ExpoInAppProvisioningModule.getTokenStatus(token);
};

export const getActiveWalletId = async (): Promise<string> => {
  if (Platform.OS === "ios") {
    return "";
  }
  return await ExpoInAppProvisioningModule.getActiveWalletId();
};

export const getStableHardwareId = async (): Promise<string> => {
  if (Platform.OS === "ios") {
    return "";
  }
  return await ExpoInAppProvisioningModule.getStableHardwareId();
};

export const pushProvision = async (
  opc: string,
  name: string,
  lastDigits: string,
  address: string,
  city: string,
  state: string,
  countryCode: string,
  postalCode: string,
  phone: string,
): Promise<boolean> => {
  if (Platform.OS === "ios") {
    return false;
  }
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
