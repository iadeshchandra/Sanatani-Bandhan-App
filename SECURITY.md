# Security & Privacy at Sanatani Bandhan CRM

At Sanatani Bandhan, the security of your Mandir's data, devotee contact information, and financial records is our absolute highest priority. Our platform is built on enterprise-grade infrastructure to ensure complete privacy and data integrity.

## 1. Cloud Infrastructure
Our backend is powered by Google's Firebase infrastructure. All data transmitted between the Android application and the database is encrypted in transit using industry-standard HTTPS/TLS encryption.

## 2. Multi-Tenant Isolation (Workspace Security)
Each Mandir, Ashram, or Organization is assigned a strictly isolated **Workspace ID** (e.g., `MND-1234`). Data from one community is cryptographically walled off and can never be accessed by or leaked to another community on the platform.

## 3. Dual-Layer Authentication
* **Super Admin Control:** Organization leaders access the platform using verified Email and Password authentication.
* **Staff Access:** Staff members and volunteers use a customized, secure 4-Digit PIN specific to their Devotee ID.
* **Role-Based Access Control (RBAC):** Standard members are cryptographically restricted from accessing financial export tools, audit logs, or administrative controls.

## 4. Device Integrity & Offline Sync
The application utilizes an offline-first database model. Financial logs recorded without an internet connection are securely cached locally on the device and automatically synced to the encrypted cloud the moment a secure connection is re-established, ensuring zero data loss.

## Reporting a Security Concern
If you have questions about our data practices or wish to request an audit of your Workspace, please contact our lead architect directly at **sanatanibandhan.com**.
