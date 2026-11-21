package com.example.data

import android.content.Context
import com.example.R
import com.example.util.ContactHelper
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudyCircleRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun <T> Flow<T>.logFirestoreStreamErrors(
        opType: OperationType,
        path: String,
        expectedUid: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, opType, path)
        }
        throw error
    }

    // --- REALTIME OBSERVATION STREAMS ---

    fun observeInstituteConfig(userId: String): Flow<InstituteConfig> = flow {
        val path = "institute_config/main"
        emitAll(
            db.collection("institute_config").document("main")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        val parsed = snapshot.toObject(
                            InstituteConfig::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: InstituteConfig()
                        // Safeguard: if older pre-update doc lacked mainPhone, return accurate defaults until migrated
                        if (parsed.mainPhone.isBlank() || parsed.city.contains("Hyderabad", ignoreCase = true)) {
                            InstituteConfig()
                        } else {
                            parsed
                        }
                    } else {
                        InstituteConfig()
                    }
                }
                .logFirestoreStreamErrors(OperationType.GET, path, userId)
        )
    }

    fun observeCourses(userId: String, isAdmin: Boolean): Flow<List<Course>> = flow {
        val path = "courses"
        val query = if (isAdmin) {
            db.collection(path)
        } else {
            db.collection(path).whereEqualTo("published", true)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(Course::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreStreamErrors(OperationType.LIST, path, userId)
        )
    }

    fun observeStudyMaterials(userId: String, isAdmin: Boolean): Flow<List<StudyMaterial>> = flow {
        val path = "study_materials"
        val query = if (isAdmin) {
            db.collection(path)
        } else {
            db.collection(path).whereEqualTo("published", true)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(
                        StudyMaterial::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreStreamErrors(OperationType.LIST, path, userId)
        )
    }

    fun observeAnnouncements(userId: String, isAdmin: Boolean): Flow<List<Announcement>> = flow {
        val path = "announcements"
        val query = if (isAdmin) {
            db.collection(path)
        } else {
            db.collection(path).whereEqualTo("published", true)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(
                        Announcement::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<Announcement> { it.pinned }
                            .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                    )
                }
                .logFirestoreStreamErrors(OperationType.LIST, path, userId)
        )
    }

    fun observeDoubts(userId: String, isAdmin: Boolean): Flow<List<Doubt>> = flow {
        val path = "doubts"
        val query = if (isAdmin) {
            db.collection(path)
        } else {
            db.collection(path).whereEqualTo("userId", userId)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(Doubt::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreStreamErrors(OperationType.LIST, path, userId)
        )
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .logFirestoreStreamErrors(OperationType.GET, path, userId)
        )
    }

    fun observeEnrollments(userId: String): Flow<List<Enrollment>> = flow {
        val path = "users/$userId/enrollments"
        emitAll(
            db.collection("users").document(userId).collection("enrollments")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        Enrollment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreStreamErrors(OperationType.LIST, path, userId)
        )
    }

    fun observeAttendance(userId: String): Flow<List<AttendanceRecord>> = flow {
        val path = "users/$userId/attendance"
        emitAll(
            db.collection("users").document(userId).collection("attendance")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        AttendanceRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreStreamErrors(OperationType.LIST, path, userId)
        )
    }

    fun observeTestScores(userId: String): Flow<List<MockTestScore>> = flow {
        val path = "users/$userId/test_scores"
        emitAll(
            db.collection("users").document(userId).collection("test_scores")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        MockTestScore::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreStreamErrors(OperationType.LIST, path, userId)
        )
    }

    // --- USER PROFILE & INITIAL BOOTSTRAP / MIGRATION ---

    suspend fun ensureUserProfile(
        displayName: String,
        email: String
    ): Result<Unit> {
        var docPath = "users/unauthenticated"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val safeName = displayName.trim().ifBlank { "Student" }.take(100)
                val safeEmail = email.trim().ifBlank { "student@rupaiit.in" }.take(150)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "targetYear" to "IIT-JEE / NEET",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(
        displayName: String,
        phone: String,
        targetYear: String
    ): Result<Unit> {
        var docPath = "users/unauthenticated"
        return try {
            val uid = requireUserId()
            val cleanName = displayName.trim()
            require(cleanName.length in 1..100) { "Name must be 1 to 100 characters" }
            val cleanPhone = phone.trim().take(25)
            val cleanTarget = targetYear.trim().take(30)
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val updateMap = mutableMapOf<String, Any>(
                "displayName" to cleanName,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanPhone.isNotBlank()) updateMap["phone"] = cleanPhone
            if (cleanTarget.isNotBlank()) updateMap["targetYear"] = cleanTarget
            docRef.update(updateMap).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Seeds or migrates official Rupa IIT / NEET Study Circle (Kadapa) config, ₹2,500/mo batches,
     * study materials, and updates when the verified admin signs in.
     */
    suspend fun seedOrMigrateOfficialInstituteData(): Result<Boolean> {
        var path = "institute_config/main"
        return try {
            val uid = requireUserId()
            val configRef = db.collection("institute_config").document("main")
            val configSnap = configRef.get().await()
            var changed = false

            val needsConfigSync = !configSnap.exists() ||
                configSnap.getString("mainPhone").isNullOrBlank() ||
                configSnap.getString("city")?.contains("Hyderabad", ignoreCase = true) == true

            if (needsConfigSync) {
                val defaults = InstituteConfig()
                val configPayload = mapOf(
                    "centerName" to defaults.centerName,
                    "tagline" to defaults.tagline,
                    "directorName" to defaults.directorName,
                    "directorBio" to defaults.directorBio,
                    "address" to defaults.address,
                    "landmark" to defaults.landmark,
                    "city" to defaults.city,
                    "latitude" to defaults.latitude,
                    "longitude" to defaults.longitude,
                    "mainPhone" to defaults.mainPhone,
                    "altPhone1" to defaults.altPhone1,
                    "altPhone2" to defaults.altPhone2,
                    "altPhone3" to defaults.altPhone3,
                    "batchTimings" to defaults.batchTimings,
                    "monthlyFeeInr" to defaults.monthlyFeeInr,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                configRef.set(configPayload).await()
                changed = true
            }

            // Migrate away legacy placeholder courses from Turn 1 if present
            path = "courses"
            val legacyCourseIds = listOf(
                "jee_adv_target_2027",
                "jee_physics_mechanics",
                "jee_math_calculus",
                "iit_foundation_class10"
            )
            legacyCourseIds.forEach { oldId ->
                val oldDoc = db.collection("courses").document(oldId).get().await()
                if (oldDoc.exists()) {
                    db.collection("courses").document(oldId).delete().await()
                }
            }

            val existingCourses = db.collection("courses").limit(1).get().await()
            if (existingCourses.isEmpty) {
                val timings = "1st Batch: 6:00 AM – 7:30 AM • 2nd Batch: 6:00 PM – 8:30 PM"
                val officialBatches = listOf(
                    "rupa_iit_jee_batch" to mapOf(
                        "title" to "IIT-JEE (Mains & Advanced)",
                        "subject" to CourseSubject.IIT_JEE.wireValue,
                        "targetExam" to "IIT-JEE Mains & Advanced",
                        "instructor" to "Shankar Royal. G (25 Yrs Exp, Ex-IIT Ramaiah)",
                        "schedule" to timings,
                        "feeAmountInr" to 2500L,
                        "feeCycle" to FeeCycle.MONTHLY.wireValue,
                        "roomName" to "Near IOB Bank, NGO Colony, Kadapa",
                        "description" to "Intensive IIT-JEE Mains & Advanced coaching led by Founder & Director Shankar Royal. G with Hyderabad-level problem solving, daily DPPs, and weekly mock tests.",
                        "published" to true,
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    "rupa_neet_batch" to mapOf(
                        "title" to "NEET Medical Entrance Coaching",
                        "subject" to CourseSubject.NEET.wireValue,
                        "targetExam" to "NEET UG",
                        "instructor" to "Shankar Royal. G & Senior Faculty",
                        "schedule" to timings,
                        "feeAmountInr" to 2500L,
                        "feeCycle" to FeeCycle.MONTHLY.wireValue,
                        "roomName" to "Near IOB Bank, NGO Colony, Kadapa",
                        "description" to "Complete NCERT-mastery coaching for Physics, Chemistry, and Biology with conceptual clarity, speed drills, and full-syllabus NEET practice papers.",
                        "published" to true,
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    "rupa_bitsat_eamcet" to mapOf(
                        "title" to "BITSAT & EAMCET Rank Booster",
                        "subject" to CourseSubject.EAMCET.wireValue,
                        "targetExam" to "BITSAT & AP/TS EAMCET",
                        "instructor" to "Shankar Royal. G",
                        "schedule" to timings,
                        "feeAmountInr" to 2500L,
                        "feeCycle" to FeeCycle.MONTHLY.wireValue,
                        "roomName" to "Near IOB Bank, NGO Colony, Kadapa",
                        "description" to "High-speed shortcut techniques, previous year paper sprints, and accuracy training tailored for top ranks in BITSAT and EAMCET.",
                        "published" to true,
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    "rupa_intermediate_batch" to mapOf(
                        "title" to "Intermediate (1st & 2nd Year) MPC / BiPC",
                        "subject" to CourseSubject.INTERMEDIATE.wireValue,
                        "targetExam" to "Intermediate Board + Entrance",
                        "instructor" to "Shankar Royal. G & Faculty Team",
                        "schedule" to timings,
                        "feeAmountInr" to 2500L,
                        "feeCycle" to FeeCycle.MONTHLY.wireValue,
                        "roomName" to "Near IOB Bank, NGO Colony, Kadapa",
                        "description" to "Strong board exam preparation synchronized with IIT-JEE, NEET, and EAMCET concepts so students excel in both IPE and competitive exams.",
                        "published" to true,
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    "rupa_foundation_6_to_10" to mapOf(
                        "title" to "6th to 10th IIT / NEET Foundation",
                        "subject" to CourseSubject.FOUNDATION.wireValue,
                        "targetExam" to "SSC • CBSE • ICSE • ISC • IG",
                        "instructor" to "Shankar Royal. G & Foundation Faculty",
                        "schedule" to timings,
                        "feeAmountInr" to 2500L,
                        "feeCycle" to FeeCycle.MONTHLY.wireValue,
                        "roomName" to "All Boards: SSC, CBSE, ICSE, ISC & IG",
                        "description" to "Early IIT & NEET foundation program for Classes 6th to 10th across SSC, CBSE, ICSE, ISC, and IG curricula building rock-solid fundamentals in Maths & Sciences.",
                        "published" to true,
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                officialBatches.forEach { (id, data) ->
                    db.collection("courses").document(id).set(data).await()
                }
                changed = true
            }

            path = "study_materials"
            val existingMaterials = db.collection("study_materials").limit(1).get().await()
            if (existingMaterials.isEmpty) {
                val starterMaterials = listOf(
                    "dpp_rotational_01" to mapOf(
                        "title" to "DPP #1: Rotational Mechanics & Torque",
                        "subject" to MaterialSubject.PHYSICS.wireValue,
                        "chapter" to "Rotational Dynamics",
                        "materialType" to MaterialType.DPP.wireValue,
                        "summary" to "15 graded JEE Mains & Advanced problems on moment of inertia, angular momentum conservation, and rolling motion.",
                        "published" to true,
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    "formula_organic_mechanisms" to mapOf(
                        "title" to "Organic Chemistry Named Reactions Sheet",
                        "subject" to MaterialSubject.CHEMISTRY.wireValue,
                        "chapter" to "Aldehydes, Ketones & Reactions",
                        "materialType" to MaterialType.FORMULA_SHEET.wireValue,
                        "summary" to "Quick-revision sheet for IIT-JEE, NEET & EAMCET covering Aldol, Cannizzaro, Perkin, and Hoffmann rearrangements.",
                        "published" to true,
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    "notes_definite_integrals" to mapOf(
                        "title" to "Calculus & Definite Integrals Shortcuts",
                        "subject" to MaterialSubject.MATHEMATICS.wireValue,
                        "chapter" to "Integral Calculus",
                        "materialType" to MaterialType.NOTES.wireValue,
                        "summary" to "King's Property, Leibniz rule, and timed EAMCET/BITSAT/JEE shortcut derivations.",
                        "published" to true,
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                starterMaterials.forEach { (id, data) ->
                    db.collection("study_materials").document(id).set(data).await()
                }
                changed = true
            }

            path = "announcements"
            val existingNotices = db.collection("announcements").limit(1).get().await()
            if (existingNotices.isEmpty) {
                val starterNotices = listOf(
                    "notice_kadapa_batches" to mapOf(
                        "title" to "Admissions Open — Morning & Evening Batches",
                        "body" to "1st Batch: 6:00 AM to 7:30 AM • 2nd Batch: 6:00 PM to 8:30 PM. Fee: ₹2,500/month. Visit Near IOB Bank, NGO Colony, Chinna Chowk, Kadapa or WhatsApp 9849460305.",
                        "category" to AnnouncementCategory.ADMISSION.wireValue,
                        "pinned" to true,
                        "published" to true,
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    "notice_sunday_test" to mapOf(
                        "title" to "Weekly IIT-JEE / NEET Practice Test",
                        "body" to "All IIT-JEE, NEET, EAMCET & 6th–10th Foundation students attend weekly evaluation test on Sunday morning at NGO Colony campus.",
                        "category" to AnnouncementCategory.EXAM.wireValue,
                        "pinned" to false,
                        "published" to true,
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                starterNotices.forEach { (id, data) ->
                    db.collection("announcements").document(id).set(data).await()
                }
                changed = true
            }

            Result.success(changed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- INSTITUTE CONFIG (ADMIN) ---

    suspend fun saveInstituteConfig(config: InstituteConfig): Result<Unit> {
        val path = "institute_config/main"
        return try {
            requireUserId()
            val centerName = config.centerName.trim()
            val tagline = config.tagline.trim()
            val directorName = config.directorName.trim()
            val directorBio = config.directorBio.trim()
            val address = config.address.trim()
            val city = config.city.trim()
            val mainPhone = config.mainPhone.trim()
            val timings = config.batchTimings.trim()

            require(centerName.length in 2..100) { "Center name must be 2–100 characters" }
            require(tagline.length in 2..180) { "Tagline must be 2–180 characters" }
            require(directorName.length in 2..80) { "Director name must be 2–80 characters" }
            require(directorBio.length in 2..350) { "Director bio must be 2–350 characters" }
            require(address.length in 5..300) { "Address must be 5–300 characters" }
            require(city.length in 2..80) { "City must be 2–80 characters" }
            require(config.latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
            require(config.longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
            require(mainPhone.length in 8..20) { "Main phone number must be 8–20 characters" }
            require(timings.length in 3..160) { "Batch timings must be 3–160 characters" }
            require(config.monthlyFeeInr in 0..500000) { "Monthly fee must be between ₹0 and ₹5,00,000" }

            val payload = mapOf(
                "centerName" to centerName,
                "tagline" to tagline,
                "directorName" to directorName,
                "directorBio" to directorBio,
                "address" to address,
                "landmark" to config.landmark.trim().take(150).ifBlank { null },
                "city" to city,
                "latitude" to config.latitude,
                "longitude" to config.longitude,
                "mainPhone" to mainPhone,
                "altPhone1" to config.altPhone1.trim().take(20).ifBlank { null },
                "altPhone2" to config.altPhone2.trim().take(20).ifBlank { null },
                "altPhone3" to config.altPhone3.trim().take(20).ifBlank { null },
                "batchTimings" to timings,
                "monthlyFeeInr" to config.monthlyFeeInr,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            db.collection("institute_config").document("main").set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- COURSES & BATCHES (ADMIN) ---

    suspend fun createCourse(
        title: String,
        subject: CourseSubject,
        targetExam: String,
        instructor: String,
        schedule: String,
        feeAmountInr: Long,
        feeCycle: FeeCycle,
        roomName: String,
        description: String
    ): Result<Unit> {
        var docPath = "courses"
        return try {
            val uid = requireUserId()
            require(title.trim().length in 2..120) { "Title must be 2–120 characters" }
            require(targetExam.trim().length in 2..80) { "Target exam must be 2–80 characters" }
            require(instructor.trim().length in 2..80) { "Instructor must be 2–80 characters" }
            require(schedule.trim().length in 2..140) { "Schedule must be 2–140 characters" }
            require(feeAmountInr in 0..500000) { "Fee must be between ₹0 and ₹5,00,000" }
            require(roomName.trim().length in 1..80) { "Room / board info is required" }
            require(description.trim().length in 2..600) { "Description must be 2–600 characters" }

            val docRef = db.collection("courses").document()
            docPath = docRef.path
            val payload = mapOf(
                "title" to title.trim(),
                "subject" to subject.wireValue,
                "targetExam" to targetExam.trim(),
                "instructor" to instructor.trim(),
                "schedule" to schedule.trim(),
                "feeAmountInr" to feeAmountInr,
                "feeCycle" to feeCycle.wireValue,
                "roomName" to roomName.trim(),
                "description" to description.trim(),
                "published" to true,
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCourse(courseId: String): Result<Unit> {
        val docPath = "courses/$courseId"
        return try {
            requireUserId()
            require(courseId.isNotBlank()) { "Invalid course ID" }
            db.collection("courses").document(courseId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- STUDY MATERIALS (ADMIN) ---

    suspend fun createStudyMaterial(
        title: String,
        subject: MaterialSubject,
        chapter: String,
        materialType: MaterialType,
        summary: String,
        resourceUrl: String?
    ): Result<Unit> {
        var docPath = "study_materials"
        return try {
            val uid = requireUserId()
            require(title.trim().length in 2..140) { "Title must be 2–140 characters" }
            require(chapter.trim().length in 1..100) { "Chapter name is required" }
            require(summary.trim().length in 2..1200) { "Summary must be 2–1200 characters" }
            val cleanUrl = resourceUrl?.trim()?.takeIf { it.isNotBlank() }
            if (cleanUrl != null) {
                require(ContactHelper.isSafeExternalUrl(cleanUrl) && cleanUrl.length <= 500) {
                    "Resource link must be a valid https:// URL"
                }
            }

            val docRef = db.collection("study_materials").document()
            docPath = docRef.path
            val payload = mapOf(
                "title" to title.trim(),
                "subject" to subject.wireValue,
                "chapter" to chapter.trim(),
                "materialType" to materialType.wireValue,
                "summary" to summary.trim(),
                "resourceUrl" to cleanUrl,
                "published" to true,
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteStudyMaterial(materialId: String): Result<Unit> {
        val docPath = "study_materials/$materialId"
        return try {
            requireUserId()
            require(materialId.isNotBlank()) { "Invalid material ID" }
            db.collection("study_materials").document(materialId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- ANNOUNCEMENTS / UPDATES (ADMIN) ---

    suspend fun createAnnouncement(
        title: String,
        body: String,
        category: AnnouncementCategory,
        pinned: Boolean
    ): Result<Unit> {
        var docPath = "announcements"
        return try {
            val uid = requireUserId()
            require(title.trim().length in 2..120) { "Title must be 2–120 characters" }
            require(body.trim().length in 2..800) { "Message must be 2–800 characters" }

            val docRef = db.collection("announcements").document()
            docPath = docRef.path
            val payload = mapOf(
                "title" to title.trim(),
                "body" to body.trim(),
                "category" to category.wireValue,
                "pinned" to pinned,
                "published" to true,
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteAnnouncement(announcementId: String): Result<Unit> {
        val docPath = "announcements/$announcementId"
        return try {
            requireUserId()
            require(announcementId.isNotBlank()) { "Invalid update ID" }
            db.collection("announcements").document(announcementId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- ENROLLMENTS ---

    suspend fun enrollInCourse(course: Course): Result<Unit> {
        var docPath = "users/unauthenticated/enrollments/${course.id}"
        return try {
            val uid = requireUserId()
            require(course.id.isNotBlank()) { "Course ID cannot be blank" }
            val docRef = db.collection("users").document(uid)
                .collection("enrollments").document(course.id)
            docPath = docRef.path
            val payload = mapOf(
                "courseId" to course.id,
                "courseTitle" to course.title.take(120),
                "targetExam" to course.targetExam.take(80),
                "schedule" to course.schedule.take(140),
                "feeAmountInr" to course.feeAmountInr,
                "status" to "ACTIVE",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun dropEnrollment(enrollmentId: String): Result<Unit> {
        var docPath = "users/unauthenticated/enrollments/$enrollmentId"
        return try {
            val uid = requireUserId()
            require(enrollmentId.isNotBlank()) { "Invalid enrollment ID" }
            val docRef = db.collection("users").document(uid)
                .collection("enrollments").document(enrollmentId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- DOUBTS ---

    suspend fun submitDoubt(
        studentName: String,
        subject: MaterialSubject,
        question: String
    ): Result<Unit> {
        var docPath = "doubts"
        return try {
            val uid = requireUserId()
            val cleanQuestion = question.trim()
            require(cleanQuestion.length in 5..1000) { "Question must be 5–1000 characters" }
            val docRef = db.collection("doubts").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "studentName" to studentName.trim().ifBlank { "Student" }.take(100),
                "subject" to subject.wireValue,
                "question" to cleanQuestion,
                "status" to DoubtStatus.OPEN.wireValue,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun answerDoubt(
        doubtId: String,
        answer: String,
        answeredBy: String
    ): Result<Unit> {
        val docPath = "doubts/$doubtId"
        return try {
            requireUserId()
            require(doubtId.isNotBlank()) { "Invalid doubt ID" }
            val cleanAnswer = answer.trim()
            require(cleanAnswer.length in 2..1500) { "Answer must be 2–1500 characters" }
            db.collection("doubts").document(doubtId).update(
                mapOf(
                    "answer" to cleanAnswer,
                    "answeredBy" to answeredBy.trim().ifBlank { "Shankar Royal. G" }.take(100),
                    "status" to DoubtStatus.ANSWERED.wireValue,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteDoubt(doubtId: String): Result<Unit> {
        val docPath = "doubts/$doubtId"
        return try {
            requireUserId()
            require(doubtId.isNotBlank()) { "Invalid doubt ID" }
            db.collection("doubts").document(doubtId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- ATTENDANCE & MOCK TEST SCORES ---

    suspend fun checkInAttendance(
        batchName: String,
        distanceMeters: Long
    ): Result<String> {
        var docPath = "users/unauthenticated/attendance"
        return try {
            val uid = requireUserId()
            val cleanBatch = batchName.trim().ifBlank { "Rupa IIT / NEET Study Circle" }.take(120)
            val clampedDistance = distanceMeters.coerceIn(0L, 50_000_000L)
            val status = if (clampedDistance <= 2500L) "PRESENT" else "REMOTE_STUDY"
            val dateLabel = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            val docRef = db.collection("users").document(uid).collection("attendance").document()
            docPath = docRef.path
            val payload = mapOf(
                "batchName" to cleanBatch,
                "dateLabel" to dateLabel.take(30),
                "status" to status,
                "distanceMeters" to clampedDistance,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(status)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun logMockTestScore(
        testName: String,
        physics: Long,
        chemistry: Long,
        math: Long,
        maxScore: Long
    ): Result<Unit> {
        var docPath = "users/unauthenticated/test_scores"
        return try {
            val uid = requireUserId()
            val cleanTest = testName.trim()
            require(cleanTest.length in 2..120) { "Test name must be 2–120 characters" }
            require(physics in -30..180 && chemistry in -30..180 && math in -30..360) {
                "Subject scores are out of valid bounds"
            }
            require(maxScore in 1..720) { "Max score must be between 1 and 720" }
            val dateLabel = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
            val docRef = db.collection("users").document(uid).collection("test_scores").document()
            docPath = docRef.path
            val payload = mapOf(
                "testName" to cleanTest,
                "physicsScore" to physics,
                "chemistryScore" to chemistry,
                "mathScore" to math,
                "maxScore" to maxScore,
                "dateLabel" to dateLabel,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteMockTestScore(scoreId: String): Result<Unit> {
        var docPath = "users/unauthenticated/test_scores/$scoreId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
                .collection("test_scores").document(scoreId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
