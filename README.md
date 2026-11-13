# FreeKiosk Lite

FreeKiosk Lite is a React Native foundation for a focused Android/iOS kiosk
experience. The project keeps the initial shell small so device policy,
configuration, and persistence can be added as isolated features.

## Requirements

- Node.js 20+
- Android Studio and an Android SDK for Android development
- Xcode and CocoaPods for iOS development

## Getting started

```bash
npm install
npm start
```

In another terminal, launch the target platform:

```bash
npm run android
# or
npm run ios
```

## Checks

```bash
npm test
npm run lint
npx tsc --noEmit
```

## Project layout

- `App.tsx` - initial kiosk shell and setup state
- `android/` - Android application project
- `ios/` - iOS application project
- `__tests__/` - Jest component tests

The current setup action is intentionally local state only. It is the
extension point for the production configuration flow and should be replaced
with persisted, validated settings before enabling device lockdown.
