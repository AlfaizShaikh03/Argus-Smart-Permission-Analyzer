# 🛡️ Argus-Smart Permission Analyzer

<div align="center">

![Smart Permission Analyzer](https://img.shields.io/badge/Android-App%20Security-green)
![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)
![License](https://img.shields.io/badge/License-MIT-blue.svg)
![Version](https://img.shields.io/badge/Version-1.0.0-orange)

**A comprehensive Android application security analyzer that helps users understand and manage app permissions with intelligent risk assessment.**

[Features](#-features) • [Screenshots](#-screenshots) • [Installation](#-installation) • [Usage](#-usage) • [Architecture](#-architecture) • [Contributing](#-contributing)

</div>

---

## 🚀 **Overview**

Smart Permission Analyzer is an advanced Android security tool designed to help users make informed decisions about app permissions and privacy. With AI-powered risk assessment and user-friendly interfaces, it bridges the gap between complex security analysis and everyday user understanding.

### **🎯 Why Smart Permission Analyzer?**

- **Smart Filtering**: Only analyzes user-facing apps (no more confusing system components!)
- **Risk Assessment**: Intelligent scoring system with color-coded risk levels
- **Real-time Monitoring**: Background scanning with instant notifications
- **User-Friendly**: Designed for everyday users, not security experts
- **Privacy-Focused**: All analysis happens locally on your device
- **Open Source**: Transparent, auditable, and community-driven

---

## ✨ **Features**

### 📱 **Core Functionality**
- **Comprehensive App Scanning**: Analyzes all user-installed applications
- **Permission Risk Assessment**: Intelligent scoring based on permission combinations
- **Real-time Monitoring**: Background service tracks new app installations
- **Smart Filtering**: Focuses only on apps users actually interact with
- **Detailed App Analysis**: In-depth permission breakdown with explanations

### 🔍 **Security Analysis**
- **Risk Level Classification**: Critical, High, Medium, Low, Minimal
- **Suspicious Permission Detection**: Identifies potentially risky permissions
- **Privacy Impact Analysis**: Shows what data each app can access
- **Trust Score Calculation**: Dynamic trust scoring with user feedback
- **Security Recommendations**: AI-powered suggestions for better security

### 📊 **User Interface**
- **Material Design 3**: Modern, intuitive interface
- **Dark/Light Theme**: Adaptive UI based on system preferences
- **Interactive Dashboard**: Quick overview with actionable insights
- **Detailed Reports**: Comprehensive analysis with visual indicators
- **Search & Filter**: Find apps quickly with advanced filtering

### 🔄 **Data Management**
- **Export Reports**: PDF, CSV, and text format exports
- **User Feedback System**: Mark apps as trusted or risky
- **Persistent Storage**: Room database for reliable data management
- **Backup Support**: Export/import your security preferences

---

## 📱 **Screenshots**

| Dashboard | App Details | Notifications | Recommendations |
|-----------|-------------|---------------|----------------|

> 📸 ![main png](https://github.com/user-attachments/assets/32e30ae8-23a4-4953-a62b-701598259f69) ![appdetail png](https://github.com/user-attachments/assets/ed8139af-8d08-48f3-901e-97ab7c77154b) ![notification png](https://github.com/user-attachments/assets/ee77316f-f841-4670-bffe-bacd00152b18) ![recommendations png](https://github.com/user-attachments/assets/601e8779-4916-4679-866c-3940adc2e92a)



---

## 🔧 **Installation**

### **Prerequisites**
- Android 5.0 (API level 21) or higher
- 50MB+ free storage space
- Internet connection for initial setup

### **Install Options**

#### **Option 1: GitHub Releases**
1. Download the latest APK from [Releases](../../releases)
2. Enable "Install from Unknown Sources" in Android Settings
3. Install the downloaded APK

#### **Option 2: Build from Source**
git clone https://github.com/yourusername/smart-permission-analyzer.git
cd smart-permission-analyzer
./gradlew assembleDebug

text

### **Permissions Required**
The app requires these permissions to function:
- `QUERY_ALL_PACKAGES` - To scan installed applications
- `POST_NOTIFICATIONS` - For security alerts (Android 13+)
- `FOREGROUND_SERVICE` - For background monitoring

---

## 📚 **Usage**

### **Getting Started**

1. **Initial Scan**: Launch the app and tap "Security Scan"
2. **Review Results**: Browse through detected apps and their risk levels
3. **App Details**: Tap any app to see detailed permission analysis
4. **Take Action**: Mark apps as trusted/risky or follow recommendations
5. **Monitor**: Enable background monitoring for real-time alerts

### **Understanding Risk Levels**

| Risk Level | Color | Description |
|------------|-------|-------------|
| 🔴 **Critical** | Red | High-risk permissions, immediate attention needed |
| 🟠 **High** | Orange | Potentially risky, review recommended |
| 🟡 **Medium** | Yellow | Moderate risk, monitor closely |
| 🟢 **Low** | Green | Minimal risk, generally safe |
| ⚪ **Minimal** | Gray | Very safe, no concerns |

### **Key Actions**

- **Trust App**: Reduces risk score, marks as safe
- **Flag as Risky**: Increases risk score, adds to watch list
- **Export Report**: Generate detailed security reports
- **Enable Monitoring**: Real-time scanning of new installations

---

## 🏗️ **Architecture**

### **Tech Stack**
- **Language**: Kotlin 100%
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Clean Architecture
- **Database**: Room (SQLite)
- **Dependency Injection**: Dagger Hilt
- **Async Processing**: Coroutines + Flow
- **Material Design**: Material 3 components

### **Project Structure**
app/
├── src/main/java/com/yourname/smartpermissionanalyzer/
│ ├── data/ # Data layer
│ │ ├── local/ # Room database
│ │ ├── repository/ # Repository implementations
│ │ └── scanner/ # Permission scanning logic
│ ├── domain/ # Business logic
│ │ ├── entities/ # Data models
│ │ ├── repository/ # Repository interfaces
│ │ └── usecase/ # Use cases
│ ├── presentation/ # UI layer
│ │ ├── viewmodel/ # ViewModels
│ │ └── ui/ # Compose UI screens
│ └── services/ # Background services

text

### **Key Components**

#### **Data Layer**
- `PermissionScannerImpl`: Core app scanning engine
- `AppEntityDao`: Database operations
- `PermissionAnalyzerRepositoryImpl`: Data management

#### **Domain Layer**
- `PerformManualScanUseCase`: Orchestrates scanning process
- `AppEntity`: Core app data model
- `RiskLevelEntity`: Risk classification system

#### **Presentation Layer**
- `DashboardViewModel`: Main screen logic
- `AppDetailsViewModel`: Detailed analysis logic
- `MainDashboardScreen`: Primary UI component

---

## 🔒 **Privacy & Security**

### **Data Privacy**
- **Local Processing**: All analysis happens on-device
- **No Data Transmission**: No personal data sent to external servers
- **Transparent Storage**: Open source database schema
- **User Control**: Full control over data export/import

### **Security Features**
- **Permission Validation**: Comprehensive permission risk assessment
- **Signature Verification**: App signature validation for integrity
- **Real-time Monitoring**: Continuous protection against new threats
- **User Education**: Clear explanations of security implications

---

## 🤝 **Contributing**

We welcome contributions from the community! Here's how you can help:

### **Getting Started**
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### **Contribution Areas**
- 🐛 **Bug Fixes**: Report and fix issues
- ✨ **New Features**: Enhance functionality
- 📚 **Documentation**: Improve docs and guides
- 🎨 **UI/UX**: Design improvements
- 🔍 **Testing**: Add test coverage
- 🌍 **Translations**: Localization support

### **Development Guidelines**
- Follow Kotlin coding standards
- Use Jetpack Compose for UI
- Write comprehensive tests
- Document new features
- Maintain backwards compatibility

---

## 🐛 **Bug Reports & Feature Requests**

### **Found a Bug?**
1. Check [existing issues](../../issues) first
2. Create a [new issue](../../issues/new) with:
    - Clear bug description
    - Steps to reproduce
    - Expected vs actual behavior
    - Device/Android version
    - Screenshots (if applicable)

### **Want a Feature?**
1. Check [feature requests](../../issues?q=label%3Aenhancement)
2. Create a [new feature request](../../issues/new) with:
    - Detailed feature description
    - Use case explanation
    - Mockups/examples (if applicable)

---

## ❓ **FAQ**

<details>
<summary><b>Q: Does this app require root access?</b></summary>
<br>
A: No, Smart Permission Analyzer works on all Android devices without root access. It uses standard Android APIs to analyze app permissions.
</details>

<details>
<summary><b>Q: Is my data safe and private?</b></summary>
<br>
A: Yes! All analysis happens locally on your device. No personal data is transmitted to external servers. The app is completely open source for transparency.
</details>

<details>
<summary><b>Q: Why don't I see all my installed apps?</b></summary>
<br>
A: The app intelligently filters out system components and background services, showing only user-facing apps that you actually interact with. This makes the analysis more relevant and understandable.
</details>

<details>
<summary><b>Q: How accurate is the risk assessment?</b></summary>
<br>
A: The risk assessment is based on comprehensive analysis of app permissions, their combinations, and potential privacy implications. While highly accurate, it's designed as a guidance tool - use your judgment for final decisions.
</details>

<details>
<summary><b>Q: Can I export my analysis results?</b></summary>
<br>
A: Yes! You can export detailed reports in PDF, CSV, or text formats. This is useful for keeping records or sharing with security professionals.
</details>

---

## 📄 **License**

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

MIT License

Copyright (c) 2025 Argus

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software...

text

---

## 🙏 **Acknowledgments**

- **Android Development Community** for excellent resources
- **Material Design Team** for beautiful design guidelines
- **Open Source Contributors** who make projects like this possible
- **Beta Testers** for valuable feedback and bug reports

---

## ☕ Support Me

If you like my work, consider buying me a coffee!

<a href="https://buymeacoffee.com/alfaizshaikh03" target="_blank">
    <img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" height="50" width="210">
</a>

---

## 📞 **Support & Contact**

- **Issues**: [GitHub Issues](../../issues)
- **Discussions**: [GitHub Discussions](../../discussions)
- **Email**: alfaiz.shaikh.work@gmail.com
- **Documentation**: [Wiki](../../wiki)

---

<div align="center">

**⭐ If you find this project useful, please consider giving it a star! ⭐**

**Made with ❤️ for Android security and privacy**

[⬆ Back to Top](#️-smart-permission-analyzer)

</div>
