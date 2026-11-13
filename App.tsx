import React from 'react';
import {
  Pressable,
  StatusBar,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import {
  SafeAreaProvider,
  SafeAreaView,
} from 'react-native-safe-area-context';

function App(): React.JSX.Element {
  const [isKioskReady, setIsKioskReady] = React.useState(false);

  return (
    <SafeAreaProvider>
      <SafeAreaView style={styles.safeArea}>
        <StatusBar
          barStyle="light-content"
          backgroundColor={colors.background}
        />
        <View style={styles.container}>
          <Text style={styles.eyebrow}>FREEKIOSK LITE</Text>
          <Text style={styles.title}>A focused kiosk foundation.</Text>
          <Text style={styles.description}>
            Configure the device experience here, then add your kiosk provider,
            policy controls, and persistence without changing the app shell.
          </Text>

          <View style={styles.statusCard}>
            <View
              style={[
                styles.statusDot,
                isKioskReady ? styles.statusDotReady : styles.statusDotIdle,
              ]}
            />
            <View style={styles.statusCopy}>
              <Text style={styles.statusTitle}>
                {isKioskReady ? 'Kiosk is ready' : 'Kiosk is not configured'}
              </Text>
              <Text style={styles.statusDescription}>
                {isKioskReady
                  ? 'The foundation is ready for a configured kiosk session.'
                  : 'Start with a safe, explicit setup flow.'}
              </Text>
            </View>
          </View>

          <Pressable
            accessibilityRole="button"
            accessibilityLabel={
              isKioskReady ? 'Reset kiosk setup' : 'Start kiosk setup'
            }
            onPress={() => setIsKioskReady(current => !current)}
            style={({pressed}) => [
              styles.button,
              pressed && styles.buttonPressed,
            ]}>
            <Text style={styles.buttonLabel}>
              {isKioskReady ? 'Reset setup' : 'Start setup'}
            </Text>
          </Pressable>
        </View>
      </SafeAreaView>
    </SafeAreaProvider>
  );
}

const colors = {
  background: '#0f172a',
  card: '#1e293b',
  muted: '#94a3b8',
  primary: '#38bdf8',
  text: '#f8fafc',
};

const styles = StyleSheet.create({
  safeArea: {
    backgroundColor: colors.background,
    flex: 1,
  },
  container: {
    flex: 1,
    justifyContent: 'center',
    padding: 28,
  },
  eyebrow: {
    color: colors.primary,
    fontSize: 13,
    fontWeight: '700',
    letterSpacing: 2,
    marginBottom: 14,
  },
  title: {
    color: colors.text,
    fontSize: 36,
    fontWeight: '800',
    lineHeight: 42,
    marginBottom: 16,
  },
  description: {
    color: colors.muted,
    fontSize: 17,
    lineHeight: 26,
    marginBottom: 32,
  },
  statusCard: {
    alignItems: 'center',
    backgroundColor: colors.card,
    borderRadius: 16,
    flexDirection: 'row',
    marginBottom: 20,
    padding: 18,
  },
  statusDot: {
    borderRadius: 8,
    height: 16,
    marginRight: 14,
    width: 16,
  },
  statusDotIdle: {
    backgroundColor: '#f59e0b',
  },
  statusDotReady: {
    backgroundColor: '#34d399',
  },
  statusCopy: {
    flex: 1,
  },
  statusTitle: {
    color: colors.text,
    fontSize: 16,
    fontWeight: '700',
    marginBottom: 4,
  },
  statusDescription: {
    color: colors.muted,
    fontSize: 14,
    lineHeight: 20,
  },
  button: {
    alignItems: 'center',
    backgroundColor: colors.primary,
    borderRadius: 12,
    minHeight: 52,
    justifyContent: 'center',
  },
  buttonPressed: {
    opacity: 0.75,
  },
  buttonLabel: {
    color: colors.background,
    fontSize: 16,
    fontWeight: '800',
  },
});

export default App;
