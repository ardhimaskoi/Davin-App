const Web3 = require("web3");
const fs = require("fs");
const path = require("path");

const web3 = new Web3("http://127.0.0.1:8545");

const abi = JSON.parse(fs.readFileSync(path.join(__dirname, "build", "abi.json")));
const bytecode = JSON.parse(fs.readFileSync(path.join(__dirname, "build", "bytecode.json")));

async function deploy() {
  try {
    const accounts = await web3.eth.getAccounts();
    const deployer = accounts[0];

    console.log("Deploying contract from:", deployer);

    const contract = new web3.eth.Contract(abi);

    const instance = await contract
      .deploy({ data: "0x" + bytecode })
      .send({ from: deployer, gas: 3000000 });

    console.log("✔ Contract deployed to:", instance.options.address);

    // Simpan address untuk backend
    fs.writeFileSync(
      path.join(__dirname, "deploy-info.json"),
      JSON.stringify({ contractAddress: instance.options.address }, null, 2)
    );

    console.log("✔ Address saved to deploy-info.json");
  } catch (err) {
    console.error("Deploy error:", err);
  }
}

deploy();
