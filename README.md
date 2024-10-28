# MultiLocale
<img src="/app/src/main/ic_launcher-playstore.png" width="128"/>
A simple app that enables you to add additional (or "unsupported") languages to your device's locale settings, if the OEM (*ahem* **Xiaomi**) doesn't let you.
<img src="/assets/screenshot01.jpg" height="200"/> <img src="/assets/screenshot02.jpg" height="200"/>  
# Requirements
* Android 7.0 (SDK 24) or more.
* Shizuku/root or ADB to grant one of the needed permissions for changing the device's locale settings (`android.permission.CHANGE_CONFIGURATION`).
# Motivation
Some OEMs, particularly Xiaomi with MIUI/HyperOS, limit users to selecting only one language or locale. 
This restriction can lead to issues, such as apps displaying incorrect characters due to [Han unification](https://en.wikipedia.org/wiki/Han_unification), where characters in one language may appear in another.
For example, without Japanese added as a locale, apps may show Chinese characters instead of Japanese ones.
# Releases
<a href="https://apt.izzysoft.de/fdroid/index/apk/io.nightdavisao.multilocale">
  <img src="/assets/IzzyOnDroid.png" alt="IzzyOnDroid" height="100">
</a>

[GitHub Releases](https://github.com/Nightdavisao/MultiLocale/releases)
# Donate
[Ko-fi](https://ko-fi.com/nightdavisao)