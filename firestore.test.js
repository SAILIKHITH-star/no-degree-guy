const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_rupa_01";
const ALICE_UID = "student_alice_123";
const BOB_UID = "student_bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msAgo = 5000) {
  return new Date(Date.now() - msAgo);
}

function futureDate(msAhead = 60000) {
  return new Date(Date.now() + msAhead);
}

function adminDb() {
  return testEnv
    .authenticatedContext(ADMIN_UID, {
      email: "gobburunana@gmail.com",
      email_verified: true,
    })
    .firestore();
}

function spoofedAdminDb() {
  return testEnv
    .authenticatedContext("spoof_uid", {
      email: "gobburunana@gmail.com",
      email_verified: false,
    })
    .firestore();
}

function studentDb(uid) {
  return testEnv
    .authenticatedContext(uid, {
      email: `${uid}@example.com`,
      email_verified: true,
    })
    .firestore();
}

function sampleCourse(createdBy = ADMIN_UID) {
  const now = pastDate(10000);
  return {
    title: "IIT-JEE (Mains & Advanced)",
    subject: "IIT_JEE",
    targetExam: "IIT-JEE Mains & Advanced",
    instructor: "Shankar Royal. G",
    schedule: "1st Batch: 6:00 AM - 7:30 AM • 2nd Batch: 6:00 PM - 8:30 PM",
    feeAmountInr: 2500,
    feeCycle: "MONTHLY",
    roomName: "NGO Colony Campus, Kadapa",
    description: "Focused IIT-JEE coaching with 25 years Hyderabad faculty experience.",
    published: true,
    createdBy,
    createdAt: now,
    updatedAt: now,
  };
}

function sampleInstituteConfig() {
  return {
    centerName: "Rupa IIT / NEET Study Circle",
    tagline: "IIT-JEE • NEET • BITSAT • EAMCET • Intermediate • 6th to 10th Foundation",
    directorName: "Shankar Royal. G",
    directorBio:
      "Senior IIT Faculty JEE Mains / Advanced • 25 Years Experience in Hyderabad • Formerly worked in IIT Ramaiah & IIT Krishna Murthy College, Hyderabad City",
    address: "Near IOB Bank, NGO Colony, Chinna Chowk",
    landmark: "Near IOB Bank",
    city: "Kadapa (A.P.) - 500601",
    latitude: 14.4585,
    longitude: 78.8315,
    mainPhone: "9849460305",
    altPhone1: "9398083995",
    altPhone2: "7702736098",
    altPhone3: "7013855152",
    batchTimings: "1st Batch: 6:00 AM to 7:30 AM • 2nd Batch: 6:00 PM to 8:30 PM",
    monthlyFeeInr: 2500,
    updatedAt: pastDate(5000),
  };
}

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

test("Unauthenticated users are blocked from all collections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("institute_config").doc("main").get());
  await assertFails(unauth.collection("courses").where("published", "==", true).get());
  await assertFails(unauth.collection("study_materials").where("published", "==", true).get());
  await assertFails(unauth.collection("announcements").where("published", "==", true).get());
  await assertFails(unauth.collection("doubts").where("userId", "==", ALICE_UID).get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("payments").get());
});

test("Removed payments collection is completely denied even for authenticated users", async () => {
  const alice = studentDb(ALICE_UID);
  const admin = adminDb();
  await assertFails(alice.collection("payments").doc("p1").set({ amountInr: 2500 }));
  await assertFails(admin.collection("payments").get());
});

test("Email spoofing attack without email_verified=true is rejected for admin writes", async () => {
  const spoofed = spoofedAdminDb();
  await assertFails(
    spoofed.collection("institute_config").doc("main").set(sampleInstituteConfig())
  );
  await assertFails(
    spoofed.collection("courses").doc("course_1").set(sampleCourse("spoof_uid"))
  );
});

test("Institute config: admin writes Kadapa campus & 4 phone numbers; students read only", async () => {
  const admin = adminDb();
  const alice = studentDb(ALICE_UID);

  await assertSucceeds(
    admin.collection("institute_config").doc("main").set(sampleInstituteConfig())
  );
  await assertSucceeds(alice.collection("institute_config").doc("main").get());
  await assertFails(alice.collection("institute_config").get());
  await assertFails(
    alice.collection("institute_config").doc("main").update({
      mainPhone: "9999999999",
      updatedAt: pastDate(1000),
    })
  );
});

test("Courses, Study Materials, and Announcements: admin CRUD & student filtered queries", async () => {
  const admin = adminDb();
  const alice = studentDb(ALICE_UID);
  const now = pastDate(5000);

  await assertSucceeds(admin.collection("courses").doc("rupa_iit_jee").set(sampleCourse()));
  await assertFails(alice.collection("courses").doc("fake_course").set(sampleCourse(ALICE_UID)));
  await assertSucceeds(alice.collection("courses").where("published", "==", true).get());
  await assertFails(alice.collection("courses").get());

  await assertFails(
    admin.collection("courses").doc("rupa_iit_jee").update({
      ghostField: true,
      updatedAt: pastDate(1000),
    })
  );

  await assertFails(
    admin.collection("courses").doc("future_course").set({
      ...sampleCourse(),
      createdAt: futureDate(),
      updatedAt: futureDate(),
    })
  );

  await assertSucceeds(
    admin.collection("study_materials").doc("dpp_bio_01").set({
      title: "NEET Biology Cell Division DPP #1",
      subject: "BIOLOGY",
      chapter: "Cell Cycle & Division",
      materialType: "DPP",
      summary: "30 high-yield NCERT-based MCQs for NEET.",
      published: true,
      createdBy: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(alice.collection("study_materials").where("published", "==", true).get());
  await assertFails(alice.collection("study_materials").get());

  await assertSucceeds(
    admin.collection("announcements").doc("notice_01").set({
      title: "Admissions Open for 6th to 10th IIT/NEET Foundation",
      body: "SSC, CBSE, ICSE, ISC & IG boards. Fee ₹2,500/month.",
      category: "ADMISSION",
      pinned: true,
      published: true,
      createdBy: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(alice.collection("announcements").where("published", "==", true).get());
  await assertFails(alice.collection("announcements").get());
});

test("Doubts & User Subcollections: owner isolation and admin resolution", async () => {
  const admin = adminDb();
  const alice = studentDb(ALICE_UID);
  const bob = studentDb(BOB_UID);
  const now = pastDate(8000);

  await assertSucceeds(admin.collection("courses").doc("rupa_iit_jee").set(sampleCourse()));

  await assertSucceeds(
    alice.collection("doubts").doc("doubt_1").set({
      userId: ALICE_UID,
      studentName: "Alice",
      subject: "BIOLOGY",
      question: "What is the key difference between meiosis I and mitosis?",
      status: "OPEN",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(alice.collection("doubts").where("userId", "==", ALICE_UID).get());
  await assertFails(bob.collection("doubts").doc("doubt_1").get());
  await assertSucceeds(admin.collection("doubts").get());
  await assertSucceeds(
    admin.collection("doubts").doc("doubt_1").update({
      answer: "Homologous chromosomes separate in Anaphase I of meiosis.",
      answeredBy: "Shankar Royal. G",
      status: "ANSWERED",
      updatedAt: pastDate(1000),
    })
  );

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      targetYear: "IIT-JEE 2027",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(bob.collection("users").doc(ALICE_UID).get());

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("enrollments").doc("rupa_iit_jee").set({
      courseId: "rupa_iit_jee",
      courseTitle: "IIT-JEE (Mains & Advanced)",
      targetExam: "IIT-JEE Mains & Advanced",
      schedule: "1st Batch: 6:00 AM - 7:30 AM • 2nd Batch: 6:00 PM - 8:30 PM",
      feeAmountInr: 2500,
      status: "ACTIVE",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(alice.collection("users").doc(ALICE_UID).collection("enrollments").get());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("enrollments").get());

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("attendance").doc("att_1").set({
      batchName: "IIT-JEE (Mains & Advanced)",
      dateLabel: "21 Nov 2026",
      status: "PRESENT",
      distanceMeters: 50,
      createdAt: now,
    })
  );
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("test_scores").doc("score_1").set({
      testName: "Weekly IIT/NEET Mock #1",
      physicsScore: 80,
      chemistryScore: 85,
      mathScore: 90,
      maxScore: 300,
      dateLabel: "21 Nov 2026",
      createdAt: now,
    })
  );
});
