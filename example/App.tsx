import * as ExpoInAppProvisioning from "expo-in-app-provisioning";
import { useEffect } from "react";
import { StyleSheet, Text, View } from "react-native";

export default function App() {
  useEffect(() => {
    (async () => {
      try {
        const res = await ExpoInAppProvisioning.getTokenStatus("Test");
        console.log("getTokenStatus", res);
      } catch (e) {
        console.error(e);
      }
      try {
        const res = await ExpoInAppProvisioning.getActiveWalletId();
        console.log("getActiveWalletId", res);
      } catch (e) {
        console.error(e);
      }
      try {
        const res = await ExpoInAppProvisioning.getStableHardwareId();
        console.log("getStableHardwareId", res);
      } catch (e) {
        console.error(e);
      }
      // try {
      //   const res = await ExpoInAppProvisioning.pushProvision();
      //   console.log("pushProvision", res);
      // } catch (e) {
      //   console.error(e);
      // }
    })();
  });
  return (
    <View style={styles.container}>
      <Text>Test!</Text>
      <Text>...</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#fff",
    alignItems: "center",
    justifyContent: "center",
  },
});
