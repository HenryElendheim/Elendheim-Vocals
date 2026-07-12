# Elendheim Vocal Range

An Android app that finds your singing range.

Tap record and sing or hum. The app listens through the microphone, shows the note you are hitting live, and captures your lowest and highest notes. Each session is saved so you can watch your range change over time.

## What it does

- Shows the note you are singing in real time, in a big circle
- Captures your vocal range, from your lowest note to your highest
- Tracks your comfortable range separately from your absolute range
- Has challenges: hold notes, climb scales, hit the octave
- Saves every session so you can see your progress
- Imports and exports your session history straight to your files
- Note only mode when you just want to see what you are hitting

## Building

Open the project in Android Studio and run it, or build from the command line:

```
./gradlew assembleRelease
```

The APK for each release is attached on the Releases page.

Release builds are signed with the keystore in `signing/`. It is a convenience key so sideloaded builds install and update cleanly; it is not a store key and protects nothing secret.

## License

MIT. See [LICENSE](LICENSE).
