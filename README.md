# 🕉️ Sanatani Bandhan - Community Portal
**Enterprise SaaS (CRM & ERP) for Spiritual Communities**

![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![Platform](https://img.shields.io/badge/platform-Android-3DDC84)
![Architecture](https://img.shields.io/badge/Architecture-Multi--Tenant-1976D2)

Sanatani Bandhan is a native Android application designed to serve as a central digital infrastructure for Hindu Mandirs, organizations, and communities. It replaces manual ledgers and chaotic messaging groups with a secure, automated, and professionally managed ecosystem.

## 🚀 Key Features

* **Multi-Tenant Architecture:** Supports isolated workspaces for different communities. Data is mathematically separated using Firebase Level 2 Security.
* **Dual-Login System:** Super Admins authenticate via Email/Password, while Staff and Members use secure, auto-generated SB-IDs and PINs via an invisible Anonymous Auth handshake.
* **Smart Chanda & Expense Engine:** A complete financial ledger tracking incoming donations and outbound expenses, automatically calculating community totals.
* **Digital Panchayat (DAO Polling):** A democratic voting system allowing members to cast secret ballots. Admins can add official signed remarks and generate Data Insight PDFs.
* **Enterprise Audit Trails:** A hidden ledger that securely logs every action (e.g., "Chanda Collected", "Member Added") taken by a manager, with date-filtered PDF exports.
* **Dynamic PDF Reporting Engine:** Generates culturally branded (Vedic Shlokas), localized (Dual-language dates) PDF reports for Receipts, Income/Expense Statements, and Member Directories directly to the device.

## 🛠️ Tech Stack

* **Frontend:** Native Android (Java), XML Layouts, Material Design Components.
* **Backend:** Firebase Authentication, Firebase Realtime Database.
* **Security:** Firebase Security Rules (Level 2 Multi-Tenant Isolation), Role-Based Access Control (RBAC).
* **Monitoring:** Firebase Crashlytics, Google Analytics.
* **CI/CD:** GitHub Actions for automated `.apk` and `.aab` compilation.
* **Tools:** iText7 (PDF Generation).

## 🔒 Security Architecture
This application implements a strict Role-Based Access Control (RBAC) model:
1. **Super Admin:** Full read/write access, role management, and access to security audit logs.
2. **Manager / Staff:** Ability to log finances, create events, and view directories. All actions are digitally signed and tracked.
3. **Member:** Read-only access to community polls, basic directories, and personal donation history.

## ⚙️ Build Instructions
This repository uses GitHub Actions for CI/CD. To build the project locally:
1. Clone the repository.
2. Open the project in **Android Studio**.
3. Ensure you have added your own `google-services.json` file in the `app/` directory.
4. Sync Gradle and run on an emulator or physical device (Min SDK 24).

## 🔗 Branding & Contact
Designed and Developed by **[Adesh Chandra](https://github.com/iadeshchandra)**. 
Proudly powered by **[TrackiQ Academy](https://linktr.ee/Adesh_Chandra)** - Elevating data analytics and tracking setups.

---
*“Dharmo Rakshati Rakshitah”*
