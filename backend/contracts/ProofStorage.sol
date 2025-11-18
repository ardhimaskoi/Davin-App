// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract ProofStorage {
    event ProofStored(bytes32 indexed dataHash, uint indexed timestamp);

    function storeProof(bytes32 dataHash) public {
        emit ProofStored(dataHash, block.timestamp);
    }
}
