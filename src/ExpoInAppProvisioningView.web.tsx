import * as React from 'react';

import { ExpoInAppProvisioningViewProps } from './ExpoInAppProvisioning.types';

export default function ExpoInAppProvisioningView(props: ExpoInAppProvisioningViewProps) {
  return (
    <div>
      <span>{props.name}</span>
    </div>
  );
}
