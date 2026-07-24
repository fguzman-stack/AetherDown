# 🌌 AetherDown

[![Version](https://img.shields.io/badge/version-2.0.0-blue.svg)](https://github.com/fguzman-stack/AetherDown)
[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://developer.android.com)
[![License](https://img.shields.io/badge/license-MIT-orange.svg)](LICENSE)
[![Download APK](https://img.shields.io/badge/Download-APK-brightgreen.svg)](https://github.com/fguzman-stack/AetherDown/releases)
[![Donate PayPal](https://img.shields.io/badge/Donate-PayPal-00457C.svg?logo=paypal&logoColor=white)](https://paypal.me/FranciscoGuz06)

**AetherDown** is a powerful, open-source universal multimedia downloader for Android. Built with a focus on speed, versatility, and a clean user experience, it allows you to download content from almost any corner of the web.

---

## 📥 Download

[![Download APK](https://img.shields.io/badge/📥_Download_APK-v2.0.0-blue?style=for-the-badge&logo=android)](https://github.com/fguzman-stack/AetherDown/releases)

Visit the [Releases page](https://github.com/fguzman-stack/AetherDown/releases) to download the latest APK release.

> [!NOTE]
> Para repositorios privados, descarga la APK estando autenticado en GitHub desde la sección de **[Releases](https://github.com/fguzman-stack/AetherDown/releases)** o compila el proyecto localmente con `./gradlew assembleRelease`.

**Requirements:** Android 8.0 (API 26) or higher.

---

## ✨ Key Features

- **🚀 Smart Multimedia Extraction**: Powered by an updated `yt-dlp` engine, supporting YouTube, Twitter/X (including GIFs), Instagram (Reels), Facebook, TikTok, and hundreds more.
- **⚡ Advanced Download Engine**:
  - High-speed downloads with **Aria2c** integration.
  - **FFmpeg** support for high-quality audio/video merging.
  - Multi-threaded chunk downloading with resumption.
- **🛡️ Privacy First**:
  - **Incognito Mode**: Download content without leaving a trace in your history.
- **📂 Management**:
  - **Queue System**: Manage multiple downloads simultaneously with priority control.
  - **History**: Keep track of your past downloads with rich metadata (titles, thumbnails, durations).
- **🎨 Modern UI/UX**:
  - Built entirely with **Jetpack Compose**.
  - **Dynamic Colors** (Material You) support.
  - Multilingual support (**English** & **Spanish**).
- **🧰 Extra Tools**:
  - Audio extraction from video files.
  - Clipboard auto-detection for quick downloads.
  - Native torrent support.

---

## 🛠️ Tech Stack

| Category | Technology |
|---|---|
| **Language** | Kotlin (2.1.10) |
| **UI Framework** | Jetpack Compose + Material 3 |
| **Architecture** | Clean Architecture + MVVM |
| **Dependency Injection** | Hilt |
| **Networking** | OkHttp 4.12 |
| **Extraction Engine** | yt-dlp (youtubedl-android) |
| **Downloader** | Aria2c + Custom Chunk Downloader |
| **Media Processing** | FFmpeg + Media3 Transformer |
| **Database** | Room |
| **Preferences** | DataStore |

---

## 🚀 Getting Started

### Prerequisites
- Android 8.0 (API 26) or higher.
- Internet connection.

### How to use
1. Copy a link from your favorite platform.
2. Open **AetherDown** (it might even detect the link automatically!).
3. Choose your preferred quality and format.
4. Hit download and enjoy your media!

---

## ☕ Support & Buy Me a Coffee

If you find **AetherDown** useful and want to support its ongoing development, feel free to buy me a coffee! Your support helps keep the project updated with the latest extractor fixes, new features, and improvements.

[![Donate via PayPal](https://img.shields.io/badge/☕_Buy_me_a_coffee_via_PayPal-00457C?style=for-the-badge&logo=paypal&logoColor=white)](https://paypal.me/FranciscoGuz06)

💖 **PayPal Link:** [paypal.me/FranciscoGuz06](https://paypal.me/FranciscoGuz06)

---

## 🤝 Contributing

Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📜 License

Distributed under the **MIT License**. See `LICENSE` for more information.

---

Developed with ❤️ by [FGuz20](https://github.com/fguzman-stack)
