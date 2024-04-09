import { StyleSheet, Text, View } from 'react-native';

import * as ExpoInAppProvisioning from 'expo-in-app-provisioning';

export default function App() {
  return (
    <View style={styles.container}>
      <Text>{ExpoInAppProvisioning.hello()}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
    alignItems: 'center',
    justifyContent: 'center',
  },
});
