package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Announcement
import com.example.data.AnnouncementCategory
import com.example.data.AttendanceRecord
import com.example.data.Course
import com.example.data.CourseSubject
import com.example.data.Doubt
import com.example.data.Enrollment
import com.example.data.FeeCycle
import com.example.data.InstituteConfig
import com.example.data.MaterialSubject
import com.example.data.MaterialType
import com.example.data.MockTestScore
import com.example.data.StudyCircleRepository
import com.example.data.StudyMaterial
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.util.DeviceCoordinates
import com.example.util.LocationHelper
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StudyCircleViewModel(
    private val repository: StudyCircleRepository,
    val currentUserId: String,
    val userEmail: String,
    val userDisplayName: String,
    val isAdmin: Boolean
) : ViewModel() {

    private fun <T> Flow<T>.toUiStateFlow(label: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val instituteConfig: StateFlow<UiState<InstituteConfig>> =
        repository.observeInstituteConfig(currentUserId).toUiStateFlow("campus config")

    val courses: StateFlow<UiState<List<Course>>> =
        repository.observeCourses(currentUserId, isAdmin).toUiStateFlow("batches")

    val studyMaterials: StateFlow<UiState<List<StudyMaterial>>> =
        repository.observeStudyMaterials(currentUserId, isAdmin).toUiStateFlow("study materials")

    val announcements: StateFlow<UiState<List<Announcement>>> =
        repository.observeAnnouncements(currentUserId, isAdmin).toUiStateFlow("updates")

    val doubts: StateFlow<UiState<List<Doubt>>> =
        repository.observeDoubts(currentUserId, isAdmin).toUiStateFlow("doubts")

    val userProfile: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId).toUiStateFlow("profile")

    val enrollments: StateFlow<UiState<List<Enrollment>>> =
        repository.observeEnrollments(currentUserId).toUiStateFlow("enrollments")

    val attendance: StateFlow<UiState<List<AttendanceRecord>>> =
        repository.observeAttendance(currentUserId).toUiStateFlow("attendance")

    val testScores: StateFlow<UiState<List<MockTestScore>>> =
        repository.observeTestScores(currentUserId).toUiStateFlow("test scores")

    private val _deviceLocation = MutableStateFlow<DeviceCoordinates?>(null)
    val deviceLocation: StateFlow<DeviceCoordinates?> = _deviceLocation.asStateFlow()

    private val _distanceMeters = MutableStateFlow<Long?>(null)
    val distanceMeters: StateFlow<Long?> = _distanceMeters.asStateFlow()

    private val _isActionBusy = MutableStateFlow(false)
    val isActionBusy: StateFlow<Boolean> = _isActionBusy.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(userDisplayName, userEmail)
            if (isAdmin) {
                repository.seedOrMigrateOfficialInstituteData()
            }
        }
    }

    fun updateDeviceLocation(coords: DeviceCoordinates, campus: InstituteConfig) {
        _deviceLocation.value = coords
        _distanceMeters.value = LocationHelper.distanceMeters(
            coords.latitude,
            coords.longitude,
            campus.latitude,
            campus.longitude
        )
    }

    fun emitMessage(msg: String) {
        _snackbarMessages.tryEmit(msg)
    }

    private fun runAction(
        successMessage: String? = null,
        block: suspend () -> Result<Any?>
    ) {
        viewModelScope.launch {
            _isActionBusy.value = true
            try {
                val res = block()
                res.onSuccess {
                    if (successMessage != null) _snackbarMessages.tryEmit(successMessage)
                }.onFailure { err ->
                    _snackbarMessages.tryEmit(err.localizedMessage ?: "Action could not be completed")
                }
            } finally {
                _isActionBusy.value = false
            }
        }
    }

    fun saveInstituteConfig(config: InstituteConfig) {
        runAction("Campus location & contact details updated") {
            repository.saveInstituteConfig(config)
        }
    }

    fun createCourse(
        title: String,
        subject: CourseSubject,
        targetExam: String,
        instructor: String,
        schedule: String,
        feeAmountInr: Long,
        feeCycle: FeeCycle,
        roomName: String,
        description: String
    ) {
        runAction("Batch published") {
            repository.createCourse(
                title, subject, targetExam, instructor, schedule,
                feeAmountInr, feeCycle, roomName, description
            )
        }
    }

    fun deleteCourse(courseId: String) {
        runAction("Batch removed") { repository.deleteCourse(courseId) }
    }

    fun createStudyMaterial(
        title: String,
        subject: MaterialSubject,
        chapter: String,
        materialType: MaterialType,
        summary: String,
        resourceUrl: String?
    ) {
        runAction("Study material published") {
            repository.createStudyMaterial(title, subject, chapter, materialType, summary, resourceUrl)
        }
    }

    fun deleteStudyMaterial(materialId: String) {
        runAction("Study material deleted") { repository.deleteStudyMaterial(materialId) }
    }

    fun createAnnouncement(
        title: String,
        body: String,
        category: AnnouncementCategory,
        pinned: Boolean
    ) {
        runAction("Update published") {
            repository.createAnnouncement(title, body, category, pinned)
        }
    }

    fun deleteAnnouncement(announcementId: String) {
        runAction("Update removed") { repository.deleteAnnouncement(announcementId) }
    }

    fun enrollInCourse(course: Course) {
        runAction("Enrolled in ${course.title}") {
            repository.enrollInCourse(course)
        }
    }

    fun dropEnrollment(enrollmentId: String) {
        runAction("Removed from enrolled batches") {
            repository.dropEnrollment(enrollmentId)
        }
    }

    fun submitDoubt(subject: MaterialSubject, question: String) {
        val activeName = (userProfile.value as? UiState.Success)?.data?.displayName
            ?.ifBlank { userDisplayName } ?: userDisplayName
        runAction("Doubt sent to Rupa faculty") {
            repository.submitDoubt(activeName, subject, question)
        }
    }

    fun answerDoubt(doubtId: String, answer: String) {
        val facultyName = (userProfile.value as? UiState.Success)?.data?.displayName
            ?.ifBlank { "Shankar Royal. G" } ?: "Shankar Royal. G"
        runAction("Solution published") {
            repository.answerDoubt(doubtId, answer, facultyName)
        }
    }

    fun deleteDoubt(doubtId: String) {
        runAction("Doubt removed") { repository.deleteDoubt(doubtId) }
    }

    fun checkInAttendance(batchName: String, distanceMeters: Long) {
        viewModelScope.launch {
            _isActionBusy.value = true
            try {
                repository.checkInAttendance(batchName, distanceMeters)
                    .onSuccess { status ->
                        val msg = if (status == "PRESENT") {
                            "Campus attendance marked Present"
                        } else {
                            "Remote study session logged"
                        }
                        _snackbarMessages.tryEmit(msg)
                    }
                    .onFailure { err ->
                        _snackbarMessages.tryEmit(err.localizedMessage ?: "Check-in failed")
                    }
            } finally {
                _isActionBusy.value = false
            }
        }
    }

    fun logMockTestScore(
        testName: String,
        physics: Long,
        chemistry: Long,
        math: Long,
        maxScore: Long
    ) {
        runAction("Mock test score saved") {
            repository.logMockTestScore(testName, physics, chemistry, math, maxScore)
        }
    }

    fun deleteMockTestScore(scoreId: String) {
        runAction("Test score deleted") { repository.deleteMockTestScore(scoreId) }
    }

    fun updateProfile(displayName: String, phone: String, targetYear: String) {
        runAction("Profile updated") {
            repository.updateUserProfile(displayName, phone, targetYear)
        }
    }

    private companion object {
        const val TAG = "StudyCircleVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
