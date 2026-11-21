package com.example.data

import androidx.annotation.Keep
import com.example.util.ContactNumberEntry
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class CourseSubject(val wireValue: String, val label: String) {
    IIT_JEE("IIT_JEE", "IIT-JEE"),
    NEET("NEET", "NEET"),
    BITSAT("BITSAT", "BITSAT"),
    EAMCET("EAMCET", "EAMCET"),
    INTERMEDIATE("INTERMEDIATE", "Intermediate"),
    FOUNDATION("FOUNDATION", "6th–10th Foundation");

    companion object {
        fun fromWire(raw: String?): CourseSubject =
            entries.firstOrNull { it.wireValue == raw } ?: IIT_JEE
    }
}

enum class FeeCycle(val wireValue: String, val shortLabel: String) {
    MONTHLY("MONTHLY", "/ month"),
    QUARTERLY("QUARTERLY", "/ quarter"),
    FULL_COURSE("FULL_COURSE", "full course");

    companion object {
        fun fromWire(raw: String?): FeeCycle =
            entries.firstOrNull { it.wireValue == raw } ?: MONTHLY
    }
}

enum class MaterialSubject(val wireValue: String, val label: String) {
    PHYSICS("PHYSICS", "Physics"),
    CHEMISTRY("CHEMISTRY", "Chemistry"),
    MATHEMATICS("MATHEMATICS", "Mathematics"),
    BIOLOGY("BIOLOGY", "Biology"),
    GENERAL("GENERAL", "General");

    companion object {
        fun fromWire(raw: String?): MaterialSubject =
            entries.firstOrNull { it.wireValue == raw } ?: GENERAL
    }
}

enum class MaterialType(val wireValue: String, val label: String) {
    DPP("DPP", "Daily Practice"),
    NOTES("NOTES", "Lecture Notes"),
    FORMULA_SHEET("FORMULA_SHEET", "Formula Sheet"),
    MOCK_PAPER("MOCK_PAPER", "Mock Paper");

    companion object {
        fun fromWire(raw: String?): MaterialType =
            entries.firstOrNull { it.wireValue == raw } ?: NOTES
    }
}

enum class AnnouncementCategory(val wireValue: String, val label: String) {
    ADMISSION("ADMISSION", "Admissions"),
    EXAM("EXAM", "Exam"),
    SCHEDULE("SCHEDULE", "Timings"),
    GENERAL("GENERAL", "Update");

    companion object {
        fun fromWire(raw: String?): AnnouncementCategory =
            entries.firstOrNull { it.wireValue == raw } ?: GENERAL
    }
}

enum class DoubtStatus(val wireValue: String, val label: String) {
    OPEN("OPEN", "Open"),
    ANSWERED("ANSWERED", "Answered");

    companion object {
        fun fromWire(raw: String?): DoubtStatus =
            entries.firstOrNull { it.wireValue == raw } ?: OPEN
    }
}

@Keep
@IgnoreExtraProperties
data class InstituteConfig(
    @DocumentId val id: String = "main",
    val centerName: String = "Rupa IIT / NEET Study Circle",
    val tagline: String = "IIT-JEE • NEET • BITSAT • EAMCET • Intermediate • 6th to 10th Foundation (SSC, CBSE, ICSE, ISC, IG)",
    val directorName: String = "Shankar Royal. G",
    val directorBio: String = "Senior IIT Faculty (JEE Mains / Advanced) • 25 Years Experience in Hyderabad • Formerly worked in IIT Ramaiah & IIT Krishna Murthy College, Hyderabad City",
    val address: String = "Near IOB Bank, NGO Colony, Chinna Chowk",
    val landmark: String = "Near IOB Bank",
    val city: String = "Kadapa (A.P.) - 500601",
    val latitude: Double = 14.4585,
    val longitude: Double = 78.8315,
    val mainPhone: String = "9849460305",
    val altPhone1: String = "9398083995",
    val altPhone2: String = "7702736098",
    val altPhone3: String = "7013855152",
    val batchTimings: String = "1st Batch: 6:00 AM to 7:30 AM • 2nd Batch: 6:00 PM to 8:30 PM",
    val monthlyFeeInr: Long = 2500L,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun alternativePhones(): List<String> =
        listOf(altPhone1, altPhone2, altPhone3).map { it.trim() }.filter { it.isNotBlank() }

    fun allContactEntries(): List<ContactNumberEntry> = buildList {
        if (mainPhone.isNotBlank()) {
            add(ContactNumberEntry(label = "Main Phone & WhatsApp", number = mainPhone.trim(), isMain = true))
        }
        alternativePhones().forEachIndexed { index, phone ->
            add(ContactNumberEntry(label = "Alternative Number ${index + 1}", number = phone, isMain = false))
        }
    }
}

@Keep
@IgnoreExtraProperties
data class Course(
    @DocumentId val id: String = "",
    val title: String = "",
    val subject: String = CourseSubject.IIT_JEE.wireValue,
    val targetExam: String = "",
    val instructor: String = "",
    val schedule: String = "",
    val feeAmountInr: Long = 2500L,
    val feeCycle: String = FeeCycle.MONTHLY.wireValue,
    val roomName: String = "",
    val description: String = "",
    val published: Boolean = true,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudyMaterial(
    @DocumentId val id: String = "",
    val title: String = "",
    val subject: String = MaterialSubject.PHYSICS.wireValue,
    val chapter: String = "",
    val materialType: String = MaterialType.DPP.wireValue,
    val summary: String = "",
    val resourceUrl: String? = null,
    val published: Boolean = true,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val category: String = AnnouncementCategory.GENERAL.wireValue,
    val pinned: Boolean = false,
    val published: Boolean = true,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Doubt(
    @DocumentId val id: String = "",
    val userId: String = "",
    val studentName: String = "",
    val subject: String = MaterialSubject.PHYSICS.wireValue,
    val question: String = "",
    val status: String = DoubtStatus.OPEN.wireValue,
    val answer: String? = null,
    val answeredBy: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phone: String? = null,
    val targetYear: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Enrollment(
    @DocumentId val id: String = "",
    val courseId: String = "",
    val courseTitle: String = "",
    val targetExam: String = "",
    val schedule: String = "",
    val feeAmountInr: Long = 2500L,
    val status: String = "ACTIVE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AttendanceRecord(
    @DocumentId val id: String = "",
    val batchName: String = "",
    val dateLabel: String = "",
    val status: String = "PRESENT",
    val distanceMeters: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MockTestScore(
    @DocumentId val id: String = "",
    val testName: String = "",
    val physicsScore: Long = 0L,
    val chemistryScore: Long = 0L,
    val mathScore: Long = 0L,
    val maxScore: Long = 300L,
    val dateLabel: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val totalScore: Long get() = physicsScore + chemistryScore + mathScore
}
