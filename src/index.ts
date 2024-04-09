import { NativeModulesProxy, EventEmitter, Subscription } from 'expo-modules-core';

// Import the native module. On web, it will be resolved to ExpoInAppProvisioning.web.ts
// and on native platforms to ExpoInAppProvisioning.ts
import ExpoInAppProvisioningModule from './ExpoInAppProvisioningModule';
import ExpoInAppProvisioningView from './ExpoInAppProvisioningView';
import { ChangeEventPayload, ExpoInAppProvisioningViewProps } from './ExpoInAppProvisioning.types';

// Get the native constant value.
export const PI = ExpoInAppProvisioningModule.PI;

export function hello(): string {
  return ExpoInAppProvisioningModule.hello();
}

export async function setValueAsync(value: string) {
  return await ExpoInAppProvisioningModule.setValueAsync(value);
}

const emitter = new EventEmitter(ExpoInAppProvisioningModule ?? NativeModulesProxy.ExpoInAppProvisioning);

export function addChangeListener(listener: (event: ChangeEventPayload) => void): Subscription {
  return emitter.addListener<ChangeEventPayload>('onChange', listener);
}

export { ExpoInAppProvisioningView, ExpoInAppProvisioningViewProps, ChangeEventPayload };
