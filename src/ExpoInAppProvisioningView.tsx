import { requireNativeViewManager } from 'expo-modules-core';
import * as React from 'react';

import { ExpoInAppProvisioningViewProps } from './ExpoInAppProvisioning.types';

const NativeView: React.ComponentType<ExpoInAppProvisioningViewProps> =
  requireNativeViewManager('ExpoInAppProvisioning');

export default function ExpoInAppProvisioningView(props: ExpoInAppProvisioningViewProps) {
  return <NativeView {...props} />;
}
