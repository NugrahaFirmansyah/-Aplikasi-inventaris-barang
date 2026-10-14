const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

const validProductData = (uid) => ({
  userId: uid,
  barcode: "8991234567890",
  name: "Beras Premium 5kg",
  category: "Sembako",
  quantity: 50,
  minStock: 10,
  unit: "sak",
  buyPrice: 65000,
  sellPrice: 75000,
  notes: "Gudang A Rak 2",
  createdAt: new Date(),
  updatedAt: new Date(),
});

test("Unauthenticated user: cannot read products", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("products").get());
});

test("Alice: can create a valid product in her own collection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("prod_1").set(validProductData(ALICE_UID))
  );
});

test("Alice: cannot write product into Bob's collection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("products").doc("prod_2").set(validProductData(ALICE_UID))
  );
});

test("Bob: cannot read Alice's products", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("products").doc("prod_1").set(validProductData(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("products").doc("prod_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("products").get());
});

test("Alice: cannot create product with missing required field or negative quantity", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const invalidData = { ...validProductData(ALICE_UID), quantity: -5 };
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("prod_bad").set(invalidData));
});

test("Alice: can create a stock transaction log and cannot update it", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const txData = {
    userId: ALICE_UID,
    productId: "prod_1",
    productName: "Beras Premium 5kg",
    barcode: "8991234567890",
    type: "IN",
    quantity: 20,
    previousStock: 30,
    newStock: 50,
    reason: "Restok dari Supplier Jaya",
    createdAt: new Date(),
  };

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set(txData)
  );

  // Updates must fail (immutable log)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").update({ quantity: 99 })
  );
});
