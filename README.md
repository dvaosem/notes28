# N28 (Notes28)

Osobné poznámky od [dvaosem](https://dvaosem.com): webová appka (PWA) + Android appka s widgetom.

## Štruktúra

| Čo | Kde |
|---|---|
| Web (ide na hosting do root-u dvaosem.com) | `web/` – `notes28.html`, `sw-notes.js`, `manifest-notes.json` + ikony |
| Android appka (WebView shell) | `app/` – načíta `https://dvaosem.com/notes28.html` |
| Build APK | `.github/workflows/build-apk.yml` – každý push do `main` mimo `web/` |

## Ako sa čo aktualizuje

- **Web zmena:** nahraj súbory z `web/` na hosting (aj `sw-notes.js`). Appka sa aktualizuje sama, APK netreba.
- **Zmena v appke (`app/`):** push do `main` → GitHub Actions zbuilduje APK a zverejní ho ako release `build-N`.
  V appke: Nastavenia → Android appka → Aktualizovať.

## Prvá inštalácia

1. Stiahni `notes28.apk` z [posledného releasu](https://github.com/dvaosem/notes28/releases/latest).
2. Povoľ inštaláciu z neznámych zdrojov, ak sa Android opýta.

## Prihlásenie cez Google v appke

Google nepustí prihlásenie vo WebView, preto appka používa natívne prihlásenie a token pošle do Firebase.
Treba raz nastaviť:

1. Firebase console → Project settings → Your apps → **Add app → Android**
   - package: `com.dvaosem.notes28`
   - SHA-1: `EB:97:F0:23:03:13:82:EA:5C:A5:6D:5B:97:29:99:81:77:9A:78:70`
2. Firebase console → Authentication → Sign-in method → Google → **Web client ID** (nie je tajný).
3. Vlož ho do `app/src/main/res/values/config.xml` (`google_web_client_id`) a pushni.

## Podpis

APK sa podpisuje `app/debug.keystore` (je v repe, tak ako pri Weather28), takže nové verzie sa inštalujú cez staré.
Tajné údaje (tokeny, API kľúče) do repa nepatria – `n28-list.php` je v `.gitignore`.
