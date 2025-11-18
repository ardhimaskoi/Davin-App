const path = require("path");
const fs = require("fs-extra");
const solc = require("solc");

const contractPath = path.resolve(__dirname, "contracts", "ProofStorage.sol");
const source = fs.readFileSync(contractPath, "utf8");

const input = {
  language: "Solidity",
  sources: {
    "ProofStorage.sol": { content: source }
  },
  settings: {
    outputSelection: {
      "*": {
        "*": ["abi", "evm.bytecode"]
      }
    }
  }
};

const output = JSON.parse(solc.compile(JSON.stringify(input)));

const contract = output.contracts["ProofStorage.sol"]["ProofStorage"];

fs.ensureDirSync(path.resolve(__dirname, "build"));
fs.writeJsonSync(path.resolve(__dirname, "build", "abi.json"), contract.abi, { spaces: 2 });
fs.writeJsonSync(
  path.resolve(__dirname, "build", "bytecode.json"),
  contract.evm.bytecode.object,
  { spaces: 2 }
);

console.log("✔ Contract compiled!");
