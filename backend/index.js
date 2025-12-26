// ===============================
// index.js
// ===============================

const express = require("express");
const mysql = require("mysql2");
const dotenv = require("dotenv");
const cors = require("cors");
const bcrypt = require("bcryptjs");
const bodyParser = require("body-parser");
const midtransClient = require("midtrans-client");

const registerBlockchainRoutes = require("./routes/blockchain.js");

const fetch = (...args) =>
  import("node-fetch").then(({ default: fetch }) => fetch(...args));

dotenv.config();

// ===============================
// APP SETUP
// ===============================

const app = express();

app.use(express.json());
app.use(cors());

// Midtrans webhook membutuhkan RAW body
app.use("/payment/webhook", bodyParser.raw({ type: "*/*" }));

// ===============================
// MIDTRANS SETUP
// ===============================

const snap = new midtransClient.Snap({
  isProduction: false,
  serverKey: process.env.MIDTRANS_SERVER_KEY,
  clientKey: process.env.MIDTRANS_CLIENT_KEY,
});

// ===============================
// DATABASE (MySQL Pool Promise)
// ===============================

const db = mysql
  .createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME,
    waitForConnections: true,
    connectionLimit: 10,
  })
  .promise();

// Test koneksi DB
(async () => {
  try {
    await db.query("SELECT 1");
    console.log("✅ Terhubung ke MySQL!");
  } catch (err) {
    console.error("❌ Gagal konek ke database:", err);
  }
})();

// ===============================
// AUTH
// ===============================

app.post("/auth/register", async (req, res) => {
  try {
    const { username, email, password } = req.body;

    if (!username || !email || !password) {
      return res.status(400).json({ message: "Field wajib diisi" });
    }

    const hashed = await bcrypt.hash(password, 10);

    const [result] = await db.query(
      "INSERT INTO users (username, email, password, balance) VALUES (?, ?, ?, 0)",
      [username, email, hashed]
    );

    const userId = result.insertId;

    const [rows] = await db.query(
      "SELECT id, username, email, balance FROM users WHERE id = ?",
      [userId]
    );

    res.json({
      message: "Registrasi berhasil",
      user: rows[0],
    });
  } catch (err) {
    res.status(500).json({
      message: "Gagal registrasi",
      error: err.message,
    });
  }
});

app.post("/auth/login", async (req, res) => {
  try {
    const { username, password } = req.body;

    const [rows] = await db.query(
      "SELECT * FROM users WHERE username=?",
      [username]
    );

    if (!rows.length) {
      return res.status(404).json({ message: "User tidak ditemukan" });
    }

    const user = rows[0];
    const valid = await bcrypt.compare(password, user.password);

    if (!valid) {
      return res.status(401).json({ message: "Password salah" });
    }

    res.json({
      message: "✅ Login berhasil",
      user: {
        id: user.id,
        username: user.username,
        email: user.email,
        balance: user.balance,
      },
    });
  } catch (err) {
    res.status(500).json({
      message: "Gagal login",
      error: err.message,
    });
  }
});

// ===============================
// USERS
// ===============================

app.get("/users", async (_req, res) => {
  const [rows] = await db.query(
    "SELECT id, username, email, balance FROM users"
  );
  res.json(rows);
});

app.get("/users/:id/profile", async (req, res) => {
  const [rows] = await db.query(
    "SELECT id, username, email, balance FROM users WHERE id=?",
    [req.params.id]
  );
  res.json(rows[0]);
});

// Top-Up manual
app.put("/users/:id/topup", async (req, res) => {
  const { id } = req.params;
  const amount = parseFloat(req.body.balance);

  if (isNaN(amount) || amount <= 0) {
    return res.status(400).json({ message: "Nominal tidak valid" });
  }

  await db.query(
    "UPDATE users SET balance = balance + ? WHERE id=?",
    [amount, id]
  );

  const [[user]] = await db.query(
    "SELECT id, username, email, balance FROM users WHERE id=?",
    [id]
  );

  res.json({
    message: "✅ Saldo berhasil ditambahkan",
    user,
  });
});

// ===============================
// INVESTMENTS
// ===============================

app.get("/investments", async (_req, res) => {
  try {
    const url =
      "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,solana&vs_currencies=usd";

    const response = await fetch(url);
    const data = await response.json();

    const updates = [
      { symbol: "BTC", price: data.bitcoin.usd },
      { symbol: "ETH", price: data.ethereum.usd },
      { symbol: "SOL", price: data.solana.usd },
    ];

    for (const u of updates) {
      await db.query(
        "UPDATE investments SET price=? WHERE symbol=?",
        [u.price, u.symbol]
      );
    }

    const [rows] = await db.query("SELECT * FROM investments");
    res.json(rows);
  } catch (err) {
    res.status(500).json({
      message: "Gagal update harga",
      error: err.message,
    });
  }
});

// ===============================
// TRANSACTIONS + PORTFOLIO
// ===============================

app.post("/transactions/buy", async (req, res) => {
  try {
    const { user_id, investment_id, amount, price } = req.body;
    const total = amount * price;

    const [[user]] = await db.query(
      "SELECT balance FROM users WHERE id=?",
      [user_id]
    );

    if (!user) {
      return res.status(404).json({ message: "User tidak ditemukan" });
    }

    if (user.balance < total) {
      return res.status(400).json({ message: "Saldo tidak cukup" });
    }

    await db.query(
      "UPDATE users SET balance=balance-? WHERE id=?",
      [total, user_id]
    );

    await db.query(
      `INSERT INTO transactions
      (user_id, investment_id, type, amount, buy_price, total_price, date)
      VALUES (?, ?, 'BUY', ?, ?, ?, NOW())`,
      [user_id, investment_id, amount, price, total]
    );

    const [rows] = await db.query(
      "SELECT * FROM portfolio WHERE user_id=? AND investment_id=?",
      [user_id, investment_id]
    );

    if (rows.length > 0) {
      await db.query(
        "UPDATE portfolio SET amount=amount+? WHERE user_id=? AND investment_id=?",
        [amount, user_id, investment_id]
      );
    } else {
      await db.query(
        "INSERT INTO portfolio (user_id, investment_id, amount) VALUES (?, ?, ?)",
        [user_id, investment_id, amount]
      );
    }

    res.json({ message: "✅ Pembelian berhasil & portofolio diperbarui" });
  } catch (err) {
    res.status(500).json({
      message: "❌ Gagal beli",
      error: err.message,
    });
  }
});

app.post("/transactions/sell", async (req, res) => {
  try {
    const { user_id, investment_id, amount, price } = req.body;
    const total = amount * price;

    const [[pf]] = await db.query(
      "SELECT amount FROM portfolio WHERE user_id=? AND investment_id=?",
      [user_id, investment_id]
    );

    if (!pf || pf.amount < amount) {
      return res.status(400).json({
        message: "Kepemilikan tidak cukup untuk dijual",
      });
    }

    await db.query(
      "UPDATE portfolio SET amount=amount-? WHERE user_id=? AND investment_id=?",
      [amount, user_id, investment_id]
    );

    await db.query(
      "UPDATE users SET balance=balance+? WHERE id=?",
      [total, user_id]
    );

    await db.query(
      `INSERT INTO transactions
      (user_id, investment_id, type, amount, total_price, date)
      VALUES (?, ?, 'SELL', ?, ?, NOW())`,
      [user_id, investment_id, amount, total]
    );

    res.json({ message: "✅ Penjualan berhasil & portofolio diperbarui" });
  } catch (err) {
    res.status(500).json({
      message: "❌ Gagal jual",
      error: err.message,
    });
  }
});

// ===============================
// PORTFOLIO
// ===============================

app.get("/portfolio/:user_id", async (req, res) => {
  const { user_id } = req.params;

  const [rows] = await db.query(
    `SELECT
      p.id,
      p.investment_id,
      i.name AS asset,
      p.amount,
      COALESCE(
        (
          SELECT SUM(
            CASE
              WHEN t.type = 'BUY' THEN t.total_price
              WHEN t.type = 'SELL' THEN -t.total_price
              ELSE 0
            END
          )
          FROM transactions t
          WHERE t.user_id = p.user_id
            AND t.investment_id = p.investment_id
        ), 0
      ) AS total_invested
    FROM portfolio p
    JOIN investments i ON p.investment_id = i.id
    WHERE p.user_id = ?
    GROUP BY p.id, p.investment_id, i.name, p.amount`,
    [user_id]
  );

  res.json(rows);
});

// ===============================
// DELETE PORTFOLIO (amount harus 0)
// ===============================

app.delete("/portfolio/:user_id/:investment_id", async (req, res) => {
  try {
    const { user_id, investment_id } = req.params;

    const [[pf]] = await db.query(
      "SELECT amount FROM portfolio WHERE user_id=? AND investment_id=?",
      [user_id, investment_id]
    );

    if (!pf) {
      return res.status(404).json({
        message: "Portofolio tidak ditemukan",
      });
    }

    if (pf.amount > 0) {
      return res.status(400).json({
        message: "❌ Aset masih dimiliki, tidak bisa dihapus",
      });
    }

    await db.query(
      "DELETE FROM portfolio WHERE user_id=? AND investment_id=?",
      [user_id, investment_id]
    );

    res.json({
      message: "✅ Portofolio berhasil dihapus",
    });
  } catch (err) {
    console.error("DELETE PORTFOLIO ERROR:", err);
    res.status(500).json({
      message: "❌ Gagal menghapus portofolio",
      error: err.message,
    });
  }
});


// ===============================
// TRANSACTIONS LIST
// ===============================

app.get("/transactions/:user_id", async (req, res) => {
  const [rows] = await db.query(
    "SELECT * FROM transactions WHERE user_id=?",
    [req.params.user_id]
  );
  res.json(rows);
});

// ===============================
// PRICE & CHART
// ===============================

app.get("/prices", async (_req, res) => {
  try {
    const url =
      "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,solana&vs_currencies=usd";

    const response = await fetch(url);
    const data = await response.json();

    res.json(data);
  } catch (err) {
    res.status(500).json({
      message: "Gagal mengambil data harga",
      error: err.message,
    });
  }
});

app.get("/prices/:symbol/chart", async (req, res) => {
  try {
    const { symbol } = req.params;

    console.log("Fetching chart for:", symbol);

    const url = `https://api.coingecko.com/api/v3/coins/${symbol}/market_chart?vs_currency=usd&days=7`;
    const response = await fetch(url);
    const data = await response.json();

    res.json(data.prices);
  } catch (err) {
    console.error("Error fetching chart:", err);
    res.status(500).json({ error: "Gagal mengambil data chart" });
  }
});

// ===============================
// PAYMENT
// ===============================

app.post("/payment/create", async (req, res) => {
  console.log("📩 [REQUEST] /payment/create:", req.body);

  try {
    const { user_id, amount } = req.body;

    if (!user_id || !amount) {
      return res.status(400).json({
        message: "user_id dan amount wajib diisi",
      });
    }

    const orderId = `TOPUP-${Date.now()}-${user_id}`;

    const parameter = {
      transaction_details: {
        order_id: orderId,
        gross_amount: amount,
      },
      customer_details: {
        first_name: "User " + user_id,
      },
    };

    const transaction = await snap.createTransaction(parameter);

    res.json({
      token: transaction.token,
      redirect_url: transaction.redirect_url,
    });
  } catch (err) {
    console.error("❌ ERROR /payment/create:", err);
    res.status(500).json({
      message: "Gagal create payment",
      error: err.message,
    });
  }
});

// ===============================
// MIDTRANS WEBHOOK
// ===============================

app.post("/payment/webhook", async (req, res) => {
  try {
    console.log("📩 WEBHOOK MASUK");

    let body;

    if (Buffer.isBuffer(req.body)) {
      body = JSON.parse(req.body.toString());
    } else {
      body = req.body;
    }

    const { order_id, transaction_status, gross_amount } = body;
    const user_id = order_id.split("-")[2];

    if (["settlement", "capture"].includes(transaction_status)) {
      await db.query(
        "UPDATE users SET balance = balance + ? WHERE id=?",
        [gross_amount, user_id]
      );
    }

    res.json({ message: "OK" });
  } catch (err) {
    console.error("🔥 WEBHOOK ERROR:", err);
    res.status(500).json({ message: "Webhook Error" });
  }
});

// ===============================
// BLOCKCHAIN
// ===============================

registerBlockchainRoutes(app, db);

// ===============================
// START SERVER
// ===============================

const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
  console.log(`🚀 Server jalan di port ${PORT}`);
});
