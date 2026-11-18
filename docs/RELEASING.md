# Releasing

Releases are built by GitHub Actions when you push a version tag. You only set up signing once.

## One-time setup: signing key

Android needs every release signed with the same key. Keep this key and its passwords safe: if you lose them you can't update the app.

1. Create a key (needs a JDK; Android Studio includes one):

   ```
   keytool -genkeypair -v -keystore release.jks -alias cs2utility \
     -keyalg RSA -keysize 4096 -validity 10000
   ```

2. Encode it for GitHub:

   ```
   base64 -w0 release.jks > release.jks.b64     # macOS: base64 -i release.jks -o release.jks.b64
   ```

3. In the GitHub repo, open **Settings → Secrets and variables → Actions** and add:

   | Secret | Value |
   | --- | --- |
   | `SIGNING_KEYSTORE_BASE64` | contents of `release.jks.b64` |
   | `SIGNING_STORE_PASSWORD` | the keystore password |
   | `SIGNING_KEY_ALIAS` | `cs2utility` |
   | `SIGNING_KEY_PASSWORD` | the key password |

Don't commit `release.jks`.

## Making a release

```
git tag v1.0.0
git push origin v1.0.0
```

The **Release** workflow tests the code, builds a signed `.apk` (to share directly) and `.aab` (for Google Play), and attaches both to a GitHub release with notes generated from the merged pull requests.

## Google Play

1. Create a developer account at play.google.com/console (one-time fee).
2. Create the app, fill in the store listing from [store-listing.md](store-listing.md), and use [PRIVACY.md](PRIVACY.md) as the privacy policy (host it somewhere public, for example GitHub Pages).
3. Data safety form: the app collects and shares no data.
4. Upload the `.aab` from the GitHub release to an internal testing track first, then promote it.
5. Let Google Play manage the app signing key (the default). The key above becomes your upload key.

**Name and trademarks:** "Counter-Strike" and "CS2" are Valve trademarks. Google Play may reject a listing that looks official. Keep the description clear that this is an unofficial fan tool, and consider a name that doesn't lead with "CS2".
