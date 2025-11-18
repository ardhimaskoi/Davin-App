// index.js
const express = require("express");
const mysql = require("mysql2");
const dotenv = require("dotenv");
const cors = require("cors");
const bcrypt = require("bcryptjs");
const midtransClient = require("midtrans-client");
const registerBlockchainRoutes = require("./routes/blockchain.js");
const fetch = (...args) =>
  import("node-fetch").then(({ default: fetch }) => fetch(...args));


dotenv.config();

const app = express();
app.use(express.json());
app.use(cors());

let snap = new midtransClient.Snap({
  isProduction: false,
  serverKey: process.env.MIDTRANS_SERVER_KEY,
  clientKey: process.env.MIDTRANS_CLIENT_KEY
});


// ✅ Pool Promise API
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

(async () => {
  try {
    await db.query("SELECT 1");
    console.log("✅ Terhubung ke MySQL!");
  } catch (err) {
    console.error("❌ Gagal konek ke database:", err);
  }
})();

// ========== AUTH ==========
app.post("/auth/register", async (req, res) => {
  try {
    const { username, email, password } = req.body;

    // Validasi input
    if (!username || !email || !password) {
      return res.status(400).json({ message: "Field wajib diisi" });
    }

    // Hash password
    const hashed = await bcrypt.hash(password, 10);

    // Insert user baru
    const [result] = await db.query(
      "INSERT INTO users (username, email, password, balance) VALUES (?, ?, ?, 0)",
      [username, email, hashed]
    );

    const userId = result.insertId;

    // 🔥 Ambil kembali user lengkap dari database
    const [rows] = await db.query(
      "SELECT id, username, email, balance FROM users WHERE id = ?",
      [userId]
    );

    const newUser = rows[0];

    // 🔥 Kirim user lengkap ke Android
    return res.json({
      message: "Registrasi berhasil",
      user: newUser,
    });
  } catch (err) {
    return res.status(500).json({
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
    if (!rows.length)
      return res.status(404).json({ message: "User tidak ditemukan" });

    const user = rows[0];
    const valid = await bcrypt.compare(password, user.password);
    if (!valid)
      return res.status(401).json({ message: "Password salah" });

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
    res.status(500).json({ message: "Gagal login", error: err.message });
  }
});

// ========== USERS ==========
app.get("/users", async (_req, res) => {
  const [rows] = await db.query(
    "SELECT id, username, email, balance FROM users"
  );
  res.json(rows);
});

// Top-Up (FINAL, tidak duplikat)
app.put("/users/:id/topup", async (req, res) => {
  const { id } = req.params;
  const amount = parseFloat(req.body.balance);

  if (isNaN(amount) || amount <= 0) {
    return res.status(400).json({ message: "Nominal tidak valid" });
  }

  await db.query("UPDATE users SET balance = balance + ? WHERE id=?", [
    amount,
    id,
  ]);

  const [[user]] = await db.query(
    "SELECT id, username, email, balance FROM users WHERE id=?",
    [id]
  );

  res.json({
    message: "✅ Saldo berhasil ditambahkan",
    user,
  });
});

app.get("/users/:id/profile", async (req, res) => {
  const [rows] = await db.query(
    "SELECT id, username, email, balance FROM users WHERE id=?",
    [req.params.id]
  );
  res.json(rows[0]);
});

// ========== INVESTMENTS ==========
app.get("/investments", async (_req, res) => {
  try {
    // Ambil harga terkini dari CoinGecko
    const url =
      "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,solana&vs_currencies=usd";
    const r = await fetch(url);
    const data = await r.json();

    // Mapping ID investment dengan harga terbaru
    const updates = [
      { symbol: "BTC", price: data.bitcoin.usd },
      { symbol: "ETH", price: data.ethereum.usd },
      { symbol: "SOL", price: data.solana.usd },
    ];

    // Update semua harga di DB
    for (const u of updates) {
      await db.query("UPDATE investments SET price=? WHERE symbol=?", [
        u.price,
        u.symbol,
      ]);
    }

    // Return data terkini dari DB
    const [rows] = await db.query("SELECT * FROM investments");
    res.json(rows);
  } catch (error) {
    res
      .status(500)
      .json({ message: "Gagal update harga", error: error.message });
  }
});

// ---------- TRANSAKSI + PORTFOLIO ----------
app.post("/transactions/buy", async (req, res) => {
  try {
    const { user_id, investment_id, amount, price } = req.body;
    const total = amount * price;

    const [[user]] = await db.query(
      "SELECT balance FROM users WHERE id=?",
      [user_id]
    );
    if (!user) return res.status(404).json({ message: "User tidak ditemukan" });
    if (user.balance < total)
      return res.status(400).json({ message: "Saldo tidak cukup" });

    await db.query("UPDATE users SET balance=balance-? WHERE id=?", [
      total,
      user_id,
    ]);

    await db.query(
      `INSERT INTO transactions (
        user_id, investment_id, type, amount, buy_price, total_price, date
      ) VALUES (?, ?, 'BUY', ?, ?, ?, NOW())`,
      [user_id, investment_id, amount, price, total]
    );

    // 🧩 Update portfolio
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
    res.status(500).json({ message: "❌ Gagal beli", error: err.message });
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
      return res
        .status(400)
        .json({ message: "Kepemilikan tidak cukup untuk dijual" });
    }

    await db.query(
      "UPDATE portfolio SET amount=amount-? WHERE user_id=? AND investment_id=?",
      [amount, user_id, investment_id]
    );

    await db.query("UPDATE users SET balance=balance+? WHERE id=?", [
      total,
      user_id,
    ]);

    await db.query(
      "INSERT INTO transactions (user_id, investment_id, type, amount, total_price, date) VALUES (?, ?, 'SELL', ?, ?, NOW())",
      [user_id, investment_id, amount, total]
    );

    res.json({ message: "✅ Penjualan berhasil & portofolio diperbarui" });
  } catch (err) {
    res.status(500).json({ message: "❌ Gagal jual", error: err.message });
  }
});

// ---------- PORTFOLIO ----------
app.get("/portfolio/:user_id", async (req, res) => {
  const { user_id } = req.params;

  const [rows] = await db.query(
    `SELECT
      p.id,
      p.investment_id,       -- ✅ TAMBAHKAN INI
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


app.get("/transactions/:user_id", async (req, res) => {
  const [rows] = await db.query(
    "SELECT * FROM transactions WHERE user_id=?",
    [req.params.user_id]
  );
  res.json(rows);
});

// ========== HARGA & CHART ==========
app.get("/prices", async (_req, res) => {
  try {
    const url =
      "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,solana&vs_currencies=usd";
    const r = await fetch(url);
    const data = await r.json();
    res.json(data);
  } catch (error) {
    res
      .status(500)
      .json({ message: "Gagal mengambil data harga", error: error.message });
  }
});

// Chart harga 7 hari terakhir
app.get("/prices/:symbol/chart", async (req, res) => {
  try {
    const { symbol } = req.params;
    const url = `https://api.coingecko.com/api/v3/coins/${symbol}/market_chart?vs_currency=usd&days=7`;
    console.log("Fetching chart for:", symbol);

    const response = await fetch(url);
    const data = await response.json();

    // Kirim hanya data harga agar ringan
    res.json(data.prices);
  } catch (err) {
    console.error("Error fetching chart:", err);
    res.status(500).json({ error: "Gagal mengambil data chart" });
  }
});

// Create payment transaction
app.post("/payment/create", async (req, res) => {
  console.log("📩 [REQUEST] /payment/create:", req.body);  // <= TAMBAHKAN INI

  try {
    const { user_id, amount } = req.body;

    if (!user_id || !amount) {
      console.log("⚠️ Data tidak lengkap:", req.body);
      return res.status(400).json({ message: "user_id dan amount wajib diisi" });
    }

    const orderId = `TOPUP-${Date.now()}-${user_id}`;

    console.log("🧾 Membuat order:", orderId);

     const parameter = {
       transaction_details: {
         order_id: orderId,
         gross_amount: amount
       },
       customer_details: {
         first_name: "User " + user_id
       }
     };


    const transaction = await snap.createTransaction(parameter);

    console.log("✅ Transaction created:", transaction);

    res.json({
      token: transaction.token,
      redirect_url: transaction.redirect_url
    });
  } catch (error) {
    console.error("❌ ERROR /payment/create:", error);
    res.status(500).json({ message: "Gagal create payment", error: error.message });
  }
});


app.post("/payment/webhook", async (req, res) => {
  try {
    const { order_id, transaction_status, gross_amount } = req.body;

    // Ambil user_id dari order_id
    const parts = order_id.split("-");
    const user_id = parts[2];

    if (transaction_status === "settlement") {
      await db.query(
        "UPDATE users SET balance = balance + ? WHERE id=?",
        [gross_amount, user_id]
      );

      console.log(`💰 Top Up Berhasil untuk User ${user_id} +${gross_amount}`);
    }

    res.status(200).json({ message: "OK" });
  } catch (err) {
    res.status(500).json({ message: "Webhook Error", error: err.message });
  }
});

//update
app.put("/transactions/:id/status", async (req, res) => {
    try {
        const { id } = req.params;
        const { status } = req.body;

        if (!["read", "unread"].includes(status)) {
            return res.status(400).json({ message: "Status tidak valid" });
        }

        const [[tx]] = await db.query(
            "SELECT * FROM transactions WHERE id = ?",
            [id]
        );

        if (!tx) {
            return res.status(404).json({ message: "Transaksi tidak ditemukan" });
        }

        await db.query(
            "UPDATE transactions SET status = ? WHERE id = ?",
            [status, id]
        );

        res.json({ message: "✔ Status transaksi diperbarui" });

    } catch (error) {
        console.error(error);
        res.status(500).json({ message: "Gagal update status transaksi" });
    }
});

// DELETE transaksi berdasarkan ID
app.delete("/transactions/:id", async (req, res) => {
    try {
        const { id } = req.params;

        const [[tx]] = await db.query(
            "SELECT * FROM transactions WHERE id = ?",
            [id]
        );

        if (!tx) {
            return res.status(404).json({ message: "Transaksi tidak ditemukan" });
        }

        await db.query("DELETE FROM transactions WHERE id = ?", [id]);

        res.json({ message: "✔ Transaksi berhasil dihapus" });

    } catch (err) {
        console.error(err);
        res.status(500).json({ message: "❌ Gagal menghapus transaksi" });
    }
});

// DELETE portfolio hanya jika amount = 0
app.delete("/portfolio/:userId/:investmentId", async (req, res) => {
  try {
    const { userId, investmentId } = req.params;

    // Ambil record portfolio
    const [[row]] = await db.query(
      "SELECT * FROM portfolio WHERE user_id = ? AND investment_id = ?",
      [userId, investmentId]
    );

    if (!row) {
      return res.status(404).json({ message: "Portfolio tidak ditemukan" });
    }

    // ❌ Tidak boleh hapus jika masih ada aset
    if (row.amount > 0) {
      return res.status(400).json({
        message: "Tidak dapat dihapus. Aset masih tersisa."
      });
    }

    // ✔ Hapus jika amount = 0
    await db.query(
      "DELETE FROM portfolio WHERE user_id = ? AND investment_id = ?",
      [userId, investmentId]
    );

    res.json({ message: "Portfolio berhasil dihapus" });

  } catch (err) {
    res.status(500).json({
      message: "Gagal menghapus portfolio",
      error: err.message
    });
  }
});

//DELTE USER
app.delete("/users/:id", async (req, res) => {
  const { id } = req.params;

  try {
    // 1️⃣ Hapus semua transaksi user
    await db.query(
      "DELETE FROM transactions WHERE user_id = ?",
      [id]
    );

    // 2️⃣ Hapus semua portfolio user
    await db.query(
      "DELETE FROM portfolio WHERE user_id = ?",
      [id]
    );

    // 3️⃣ Hapus activity_log blockchain user
    await db.query(
      "DELETE FROM activity_log WHERE user_id = ?",
      [id]
    );

    // 4️⃣ Hapus user terakhir
    const [result] = await db.query(
      "DELETE FROM users WHERE id = ?",
      [id]
    );

    if (result.affectedRows === 0) {
      return res.status(404).json({ message: "User tidak ditemukan" });
    }

    res.json({ message: "✔ User dan semua datanya berhasil dihapus" });

  } catch (err) {
    console.error("[DELETE USER] ERROR:", err.message);
    res.status(500).json({
      message: "❌ Gagal menghapus user",
      error: err.message
    });
  }
});





// ========== BLOCKCHAIN ==========
registerBlockchainRoutes(app, db);

// ========== START ==========
const PORT = process.env.PORT || 3000;
app.listen(PORT, () =>
  console.log(`🚀 Server jalan di port ${PORT}`)
);
