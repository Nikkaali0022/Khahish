const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid";
const BOB_UID = "bob_uid";
const CHARLIE_UID = "charlie_uid";

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

test("Unauthenticated user cannot read any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").get());
  await assertFails(unauthDb.collection("chats").get());
  await assertFails(unauthDb.collection("calls").get());
});

test("User can create their own profile, cannot forge someone else's", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const validProfile = {
    userId: ALICE_UID,
    username: "alice_w",
    displayName: "Alice Wonderland",
    createdAt: new Date(),
  };

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile));

  // Alice cannot forge Bob's profile
  const bobSpoofed = {
    userId: BOB_UID,
    username: "bob_m",
    displayName: "Bob Marley",
    createdAt: new Date(),
  };
  await assertFails(aliceDb.collection("users").doc(BOB_UID).set(bobSpoofed));
});

test("Friend requests: Alice can send to Bob; Charlie cannot read it", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  const reqDoc = aliceDb.collection("friendRequests").doc("req_1");
  await assertSucceeds(reqDoc.set({
    requestId: "req_1",
    senderId: ALICE_UID,
    senderUsername: "alice_w",
    receiverId: BOB_UID,
    receiverUsername: "bob_m",
    status: "pending",
    createdAt: new Date(),
  }));

  // Bob can get the request
  await assertSucceeds(bobDb.collection("friendRequests").doc("req_1").get());

  // Charlie cannot get the request
  await assertFails(charlieDb.collection("friendRequests").doc("req_1").get());

  // Bob can accept the request
  await assertSucceeds(bobDb.collection("friendRequests").doc("req_1").update({
    status: "accepted",
    updatedAt: new Date(),
  }));
});

test("Chat and messages: participants only", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  // Create chat between Alice and Bob
  const chatRef = aliceDb.collection("chats").doc("chat_alice_bob");
  await assertSucceeds(chatRef.set({
    chatId: "chat_alice_bob",
    participants: [ALICE_UID, BOB_UID],
    createdAt: new Date(),
  }));

  // Alice sends message
  const msgRef = chatRef.collection("messages").doc("msg_1");
  await assertSucceeds(msgRef.set({
    messageId: "msg_1",
    chatId: "chat_alice_bob",
    senderId: ALICE_UID,
    senderName: "Alice",
    text: "Hello Bob!",
    createdAt: new Date(),
  }));

  // Bob can read messages
  await assertSucceeds(bobDb.collection("chats").doc("chat_alice_bob").collection("messages").doc("msg_1").get());

  // Charlie cannot read or send messages
  await assertFails(charlieDb.collection("chats").doc("chat_alice_bob").get());
  await assertFails(charlieDb.collection("chats").doc("chat_alice_bob").collection("messages").doc("msg_1").get());
});

test("Calls: caller and receiver only", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  const callDoc = aliceDb.collection("calls").doc("call_1");
  await assertSucceeds(callDoc.set({
    callId: "call_1",
    callerId: ALICE_UID,
    callerName: "Alice",
    receiverId: BOB_UID,
    receiverName: "Bob",
    channelId: "room_123",
    callType: "video",
    status: "ringing",
    createdAt: new Date(),
  }));

  // Bob can see call
  await assertSucceeds(bobDb.collection("calls").doc("call_1").get());

  // Charlie cannot see call
  await assertFails(charlieDb.collection("calls").doc("call_1").get());

  // Bob can answer (update status)
  await assertSucceeds(bobDb.collection("calls").doc("call_1").update({
    status: "accepted",
    updatedAt: new Date(),
  }));
});
