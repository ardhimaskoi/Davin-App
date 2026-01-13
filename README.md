# DAVIN – Decentralized Investment App

DAVIN adalah aplikasi mobile investment platform berbasis Android yang menggabungkan micro-investment, verifikasi blockchain, dan harga crypto real-time dalam satu aplikasi dengan tampilan clean dan profesional seperti exchange modern.

Aplikasi ini dikembangkan sebagai project eksplorasi teknologi finansial modern yang mencakup Android UI modern, backend API, payment gateway, serta blockchain verification.

---

## 🚀 Fitur Utama

DAVIN menyediakan alur investasi digital end-to-end.
Pengguna dapat melakukan registrasi dan login, menyimpan sesi akun, melakukan top up saldo melalui Midtrans, membeli dan menjual aset crypto (BTC, ETH, SOL), melihat portofolio investasi lengkap dengan perhitungan return, serta memverifikasi transaksi melalui blockchain.

UI dirancang clean, minimal, dan profesional dengan bottom navigation tanpa background aktif serta warna aksen khas DAVIN.

---

## 🛠 Teknologi yang Digunakan

Aplikasi ini dibangun menggunakan teknologi berikut:

Android:
Kotlin, Jetpack Compose, MVVM Architecture, Retrofit, Coroutine & Flow

Backend:
Node.js (Express), MySQL, Midtrans Snap API, CoinGecko API

Blockchain:
Ethereum (Ganache / Local Testnet), Solidity Smart Contract, Web3.js

---

## ▶️ Cara Menjalankan Aplikasi

### Menjalankan Backend

Masuk ke folder backend:
cd backend

Install dependency:
npm install

Buat file environment:
cp .env.example .env

Isi file .env sesuai konfigurasi:
DB_HOST=localhost
DB_USER=root
DB_PASS=
DB_NAME=davin_db
MIDTRANS_SERVER_KEY=SB-Mid-server-xxxx
MIDTRANS_CLIENT_KEY=SB-Mid-client-xxxx

Jalankan backend:
node index.js


Backend akan berjalan di:
[http://localhost:3000](http://localhost:3000)

---

### Menjalankan Blockchain (Ganache)

ganache -p 8545 --db ganache-db --wallet.deterministic

---

### Menjalankan Aplikasi Android

Buka project di Android Studio lalu jalankan aplikasi seperti biasa (Run ▶️).

Jika menggunakan emulator dan backend lokal, gunakan base URL:
[http://10.0.2.2:3000](http://10.0.2.2:3000)

Jika backend ingin diakses dari device fisik, gunakan ngrok:
ngrok http 3000

Lalu update base URL di Android sesuai URL ngrok.

---

## ⚠️ Catatan Penting

Midtrans berjalan pada mode Sandbox.
Blockchain hanya digunakan untuk local testing.
Aplikasi ini bukan aplikasi finansial produksi.

---

## 📌 Status Project

Project ini telah mencakup integrasi UI modern, backend API, payment gateway, dan blockchain verification, sehingga siap digunakan untuk demo, presentasi akademik, lomba, maupun portfolio pribadi.

---

## 👤 Author

Ardhimas
Android & Blockchain Developer
Universitas Brawijaya

© 2025 DAVIN Project

