import * as ExpoInAppProvisioning from "expo-in-app-provisioning";
import { useEffect, useState } from "react";
import { Button, StyleSheet, Text, View } from "react-native";

export default function App() {
  const [canAddCard, setCanAddCard] = useState(false);
  useEffect(() => {
    (async () => {
      try {
        const res = await ExpoInAppProvisioning.canAddCard("Test");
        setCanAddCard(res);
      } catch (e) {
        console.error(e);
      }
    })();
  });

  const handleInitialize = async () => {
    try {
      const res = await ExpoInAppProvisioning.initialize({
        cardholderName: "John Doe",
        localizedDescription: "My Card",
        lastFour: "1234",
        cardId: "1234567890",
      });
      console.log("initialize", res);
    } catch (e) {
      console.error(e);
    }
  };

  const handlePushProvision = async () => {
    try {
      const res = await ExpoInAppProvisioning.pushProvision({
        activationData: "",
        encryptedPassData: "",
        ephemeralPublicKey: "",
      });
      console.log("pushProvision", res);
    } catch (e) {
      console.error(e);
    }
  };
  return (
    <View style={styles.container}>
      <Text>Can add cards? {canAddCard ? "Yes" : "No"}</Text>
      <Button title="Initialize" onPress={handleInitialize} />
      <Button title="Push provision" onPress={handlePushProvision} />
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
