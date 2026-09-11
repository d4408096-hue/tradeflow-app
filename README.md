# TradeFlow — Master App (white-label source)

Native Android (Kotlin + Compose + Room). One codebase → one branded APK per customer.

## Repo layout (push THIS folder's contents as the repo root)

```
settings.gradle / build.gradle / gradle.properties
app/build.gradle                    ← flavors live here
app/src/main/                       ← shared code + theme + manifest
app/src/mikeDemo/res/values/brand.xml
app/src/bobDemo/res/values/brand.xml
.github/workflows/build-apk.yml     ← cloud builder (free)
```

## Cloud build (phone-only workflow — no PC needed)

Every push to `main` auto-builds both demo APKs via GitHub Actions (~10 min).
Download: repo → Actions → latest run → Artifacts → `demo-apks` → install on phone.

## White-label a new customer (10 min)

1. Copy `app/src/mikeDemo` → `app/src/<customer>/`, edit `brand.xml`
   (app_name, business_name, business_phone, brandPrimary, brandDark).
2. Add 5 lines to `app/build.gradle` productFlavors (copy a demo block,
   change names + applicationId `com.<business>.pro`).
3. Push → cloud builds `assemble<Customer>Debug` → send Drive link.

Rule: NEVER reuse an applicationId, NEVER lose the release keystore (M7).

## Local build (future, if a PC is ever available)

Install Android Studio → open this folder → Run `mikeDemoDebug`.
No Gradle wrapper jar is committed; run `gradle wrapper` once on a PC if needed.
