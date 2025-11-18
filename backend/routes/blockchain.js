const Web3 = require("web3");
const crypto = require("crypto");
const fs = require("fs");
const path = require("path");

const abi = JSON.parse(fs.readFileSync(path.join(__dirname, "../build/abi.json")));
const { contractAddress } = require("../deploy-info.json");

// Connect ke Ganache CLI
const web3 = new Web3("http://127.0.0.1:8545");

// Instance contract
const contract = new web3.eth.Contract(abi, contractAddress);

async function registerBlockchainRoutes(app, db) {
  console.log("🔗 Blockchain route aktif");
  console.log("📌 Contract:", contractAddress);

  const accounts = await web3.eth.getAccounts();
  const defaultAccount = accounts[0];

  console.log("🔑 Default Account:", defaultAccount);

  // ==================================================================
  // POST /blockchain/record  → Simpan data + hash ke blockchain & DB
  // ==================================================================
  app.post("/blockchain/record", async (req, res) => {
    try {
      const { user_id, action, stock, amount } = req.body;

      // Normalisasi angka supaya konsisten
      const amountNorm = Number(amount).toString();

      // Format data konsisten
      const dataString = `${user_id}|${action}|${stock}|${amountNorm}`;

      // Generate hash
      const hash = "0x" + crypto
        .createHash("sha256")
        .update(dataString)
        .digest("hex");

      // Simpan ke blockchain
      const tx = await contract.methods.storeProof(hash)
        .send({ from: defaultAccount, gas: 200000 });

      // Simpan ke MySQL
      await db.query(
        "INSERT INTO activity_log (user_id, action, stock, amount, dataHash, txHash) VALUES (?, ?, ?, ?, ?, ?)",
        [user_id, action, stock, amountNorm, hash, tx.transactionHash]
      );

      res.json({
        message: "✅ Proof tersimpan di blockchain",
        txHash: tx.transactionHash,
        hash
      });

    } catch (err) {
      console.error("❌ Gagal mencatat:", err);
      res.status(500).json({ message: "❌ Gagal mencatat proof blockchain", error: err.message });
    }
  });

  // ==================================================================
  // GET /blockchain/records  → Ambil semua record
  // ==================================================================
  app.get("/blockchain/records", async (_req, res) => {
    const [rows] = await db.query("SELECT * FROM activity_log ORDER BY id DESC");
    res.json(rows);
  });

  // ==================================================================
  // GET /blockchain/verify/:id → Verifikasi apakah data berubah
  // ==================================================================
  app.get("/blockchain/verify/:id", async (req, res) => {
    try {
      const { id } = req.params;

      // Ambil record lokal
      const [[record]] = await db.query(
        "SELECT * FROM activity_log WHERE id=?",
        [id]
      );

      if (!record) {
        return res.status(404).json({ valid: false, message: "Record tidak ditemukan" });
      }

      // Normalisasi amount dari database
      const amountNorm = Number(record.amount).toString();

      // Hash ulang
      const dataString = `${record.user_id}|${record.action}|${record.stock}|${amountNorm}`;

      const localHash = "0x" + crypto
        .createHash("sha256")
        .update(dataString)
        .digest("hex");

      // Ambil event blockchain
      const events = await contract.getPastEvents("ProofStored", {
        fromBlock: 0,
        toBlock: "latest"
      });

      // Cek apakah hash pernah dicatat di blockchain
      const exists = events.some(ev => ev.returnValues.dataHash === record.dataHash);

      // Data valid jika:
      // 1. Hash lokal sama dengan hash yang tersimpan
      // 2. Hash tersebut benar-benar ada di blockchain
      const valid = (localHash === record.dataHash) && exists;

      res.json({
        valid,
        localHash,
        blockchainHashExists: exists,
        message: valid ? "Data VALID, tidak dimanipulasi" : "❌ Data SUDAH DIMANIPULASI!"
      });

    } catch (err) {
      res.status(500).json({ error: err.message });
    }
  });
}

module.exports = registerBlockchainRoutes;
