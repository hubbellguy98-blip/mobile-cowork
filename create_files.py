import os

files = {
    "settings.gradle.kts": """
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "PhoneAgent"
include(":app")
""",
    "build.gradle.kts": """
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.1.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
    id("androidx.navigation.safeargs.kotlin") version "2.7.5" apply false
}
""",
    "app/build.gradle.kts": """
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.phoneagent"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.phoneagent"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-android-compiler:2.48")

    // Room
    implementation("androidx.room:room-runtime:2.6.0")
    kapt("androidx.room:room-compiler:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")

    // Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // ViewModel + LiveData + Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Coil
    implementation("io.coil-kt:coil:2.5.0")

    // DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.5")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

kapt {
    correctErrorTypes = true
}
""",
    "app/src/main/AndroidManifest.xml": """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    package="com.phoneagent">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
    <uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" tools:ignore="ProtectedPermissions" />
    <uses-permission android:name="android.permission.ACTION_MANAGE_OVERLAY_PERMISSION" />
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <application
        android:name=".PhoneAgentApplication"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.PhoneAgent"
        tools:targetApi="31">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.PhoneAgent">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
        <activity android:name=".ui.settings.SettingsActivity" />

        <service
            android:name=".service.PhoneAgentAccessibilityService"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>

        <service
            android:name=".service.ScreenCaptureService"
            android:foregroundServiceType="mediaProjection"
            android:exported="false" />
    </application>

</manifest>
""",
    "app/src/main/res/values/strings.xml": """<resources>
    <string name="app_name">Phone Agent</string>
    <string name="accessibility_service_description">Phone Agent requires this service to perform automated taps and read screen content on your behalf.</string>
</resources>
""",
    "app/src/main/res/values/colors.xml": """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="primary">#0D1B2A</color>
    <color name="primary_variant">#1B263B</color>
    <color name="secondary">#F4B942</color>
    <color name="background">#000000</color>
    <color name="surface">#121212</color>
    <color name="error">#CF6679</color>
    <color name="on_primary">#FFFFFF</color>
    <color name="on_secondary">#000000</color>
    <color name="on_background">#FFFFFF</color>
    <color name="on_surface">#FFFFFF</color>
    <color name="on_error">#000000</color>
</resources>
""",
    "app/src/main/res/values/themes.xml": """<resources xmlns:tools="http://schemas.android.com/tools">
    <style name="Theme.PhoneAgent" parent="Theme.Material3.Dark.NoActionBar">
        <item name="colorPrimary">@color/primary</item>
        <item name="colorPrimaryVariant">@color/primary_variant</item>
        <item name="colorSecondary">@color/secondary</item>
        <item name="android:colorBackground">@color/background</item>
        <item name="colorSurface">@color/surface</item>
        <item name="colorError">@color/error</item>
        <item name="colorOnPrimary">@color/on_primary</item>
        <item name="colorOnSecondary">@color/on_secondary</item>
        <item name="colorOnBackground">@color/on_background</item>
        <item name="colorOnSurface">@color/on_surface</item>
        <item name="colorOnError">@color/on_error</item>
    </style>
</resources>
""",
    "app/src/main/res/xml/accessibility_service_config.xml": """<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeAllMask"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault|flagRetrieveInteractiveWindows|flagIncludeNotImportantViews|flagReportViewIds"
    android:canPerformGestures="true"
    android:canRetrieveWindowContent="true"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="100" />
""",
    "app/src/main/res/xml/backup_rules.xml": """<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <include domain="sharedpref" path="."/>
    <include domain="database" path="."/>
</full-backup-content>
""",
    "app/src/main/res/xml/data_extraction_rules.xml": """<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <include domain="sharedpref" path="."/>
        <include domain="database" path="."/>
    </cloud-backup>
</data-extraction-rules>
""",
    ".github/workflows/build.yml": """name: Android Build

on:
  push:
    branches: [ "main" ]
  pull_request:
    branches: [ "main" ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - name: Checkout repository
      uses: actions/checkout@v3

    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: gradle

    - name: Grant execute permission for gradlew
      run: chmod +x gradlew

    - name: Build Debug APK
      uses: gradle/gradle-build-action@v2
      with:
        arguments: assembleDebug

    - name: Upload APK artifact
      uses: actions/upload-artifact@v3
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk
""",
    "app/src/main/java/com/phoneagent/PhoneAgentApplication.kt": """package com.phoneagent

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PhoneAgentApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
""",
    "app/src/main/java/com/phoneagent/MainActivity.kt": """package com.phoneagent

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set your main layout or start a fragment here
    }
}
""",
    "app/src/main/java/com/phoneagent/di/AppModule.kt": """package com.phoneagent.di

import android.content.Context
import androidx.room.Room
import com.phoneagent.data.local.AppDatabase
import com.phoneagent.data.remote.GrokApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "phone_agent_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideConversationDao(appDatabase: AppDatabase) = appDatabase.conversationDao()

    @Provides
    @Singleton
    fun provideActionLogDao(appDatabase: AppDatabase) = appDatabase.actionLogDao()

    @Provides
    @Singleton
    fun provideGrokApiService(): GrokApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.x.ai/") // Placeholder for actual base URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GrokApiService::class.java)
    }
}
""",
    "app/src/main/java/com/phoneagent/data/local/AppDatabase.kt": """package com.phoneagent.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.phoneagent.data.local.dao.ActionLogDao
import com.phoneagent.data.local.dao.ConversationDao
import com.phoneagent.data.local.entities.ActionLogEntity
import com.phoneagent.data.local.entities.ConversationEntity

@Database(
    entities = [ConversationEntity::class, ActionLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun actionLogDao(): ActionLogDao
}
""",
    "app/src/main/java/com/phoneagent/data/local/dao/ConversationDao.kt": """package com.phoneagent.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.phoneagent.data.local.entities.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ConversationEntity)
}
""",
    "app/src/main/java/com/phoneagent/data/local/dao/ActionLogDao.kt": """package com.phoneagent.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.phoneagent.data.local.entities.ActionLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionLogDao {
    @Query("SELECT * FROM action_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<ActionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActionLogEntity)
}
""",
    "app/src/main/java/com/phoneagent/data/local/entities/ConversationEntity.kt": """package com.phoneagent.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val message: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
""",
    "app/src/main/java/com/phoneagent/data/local/entities/ActionLogEntity.kt": """package com.phoneagent.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "action_logs")
data class ActionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val actionType: String,
    val actionDetails: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean
)
""",
    "app/src/main/java/com/phoneagent/data/remote/GrokApiService.kt": """package com.phoneagent.data.remote

import com.phoneagent.data.remote.models.GrokRequest
import com.phoneagent.data.remote.models.GrokResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface GrokApiService {
    @POST("v1/chat/completions")
    suspend fun sendRequest(@Body request: GrokRequest): GrokResponse
}
""",
    "app/src/main/java/com/phoneagent/data/remote/models/GrokRequest.kt": """package com.phoneagent.data.remote.models

data class GrokRequest(
    val messages: List<Message>,
    val model: String = "grok-vision-beta"
)

data class Message(
    val role: String,
    val content: String,
    val screenshotBase64: String? = null
)
""",
    "app/src/main/java/com/phoneagent/data/remote/models/GrokResponse.kt": """package com.phoneagent.data.remote.models

data class GrokResponse(
    val id: String,
    val choices: List<Choice>
)

data class Choice(
    val message: ResponseMessage
)

data class ResponseMessage(
    val role: String,
    val content: String,
    val action: AgentAction?
)
""",
    "app/src/main/java/com/phoneagent/data/remote/models/AgentAction.kt": """package com.phoneagent.data.remote.models

data class AgentAction(
    val type: String, // e.g., "TAP", "SCROLL", "TYPE", "OPEN_APP"
    val x: Float? = null,
    val y: Float? = null,
    val textToType: String? = null,
    val packageName: String? = null,
    val isTaskComplete: Boolean = false
)
""",
    "app/src/main/java/com/phoneagent/data/repository/ChatRepository.kt": """package com.phoneagent.data.repository

import com.phoneagent.data.local.dao.ConversationDao
import com.phoneagent.data.local.entities.ConversationEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val conversationDao: ConversationDao
) {
    fun getChatHistory(): Flow<List<ConversationEntity>> = conversationDao.getAllMessages()

    suspend fun saveMessage(message: String, isFromUser: Boolean) {
        val entity = ConversationEntity(message = message, isFromUser = isFromUser)
        conversationDao.insertMessage(entity)
    }
}
""",
    "app/src/main/java/com/phoneagent/data/repository/AgentRepository.kt": """package com.phoneagent.data.repository

import com.phoneagent.data.local.dao.ActionLogDao
import com.phoneagent.data.local.entities.ActionLogEntity
import com.phoneagent.data.remote.GrokApiService
import com.phoneagent.data.remote.models.GrokRequest
import com.phoneagent.data.remote.models.GrokResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentRepository @Inject constructor(
    private val apiService: GrokApiService,
    private val actionLogDao: ActionLogDao
) {
    suspend fun sendAgentRequest(request: GrokRequest): GrokResponse {
        return apiService.sendRequest(request)
    }

    suspend fun logAction(actionType: String, actionDetails: String, isSuccess: Boolean) {
        val log = ActionLogEntity(
            actionType = actionType,
            actionDetails = actionDetails,
            isSuccess = isSuccess
        )
        actionLogDao.insertLog(log)
    }
}
""",
    "app/src/main/java/com/phoneagent/domain/usecases/SendMessageUseCase.kt": """package com.phoneagent.domain.usecases

import com.phoneagent.data.repository.ChatRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(message: String) {
        chatRepository.saveMessage(message, isFromUser = true)
        // Here we will eventually trigger the Agent logic
    }
}
""",
    "app/src/main/java/com/phoneagent/domain/usecases/ExecuteActionUseCase.kt": """package com.phoneagent.domain.usecases

import com.phoneagent.data.remote.models.AgentAction
import com.phoneagent.data.repository.AgentRepository
import javax.inject.Inject

class ExecuteActionUseCase @Inject constructor(
    private val agentRepository: AgentRepository
) {
    suspend operator fun invoke(action: AgentAction) {
        // Implementation for dispatching action to Accessibility Service goes here
        agentRepository.logAction(
            actionType = action.type,
            actionDetails = action.toString(),
            isSuccess = true
        )
    }
}
""",
    "app/src/main/java/com/phoneagent/domain/usecases/CaptureScreenUseCase.kt": """package com.phoneagent.domain.usecases

import javax.inject.Inject

class CaptureScreenUseCase @Inject constructor() {
    operator fun invoke(): String {
        // Implementation for fetching screen capture base64
        return "base64_placeholder"
    }
}
""",
    "app/src/main/java/com/phoneagent/ui/chat/ChatFragment.kt": """package com.phoneagent.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatFragment : Fragment() {
    private val viewModel: ChatViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment (needs to be created)
        return null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Setup UI and observers
    }
}
""",
    "app/src/main/java/com/phoneagent/ui/chat/ChatViewModel.kt": """package com.phoneagent.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneagent.data.repository.ChatRepository
import com.phoneagent.domain.usecases.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            chatRepository.getChatHistory().collect { messages ->
                // Update UI State with new messages
            }
        }
    }

    fun sendMessage(text: String) {
        viewModelScope.launch {
            sendMessageUseCase(text)
        }
    }
}

data class ChatUiState(
    val isLoading: Boolean = false
)
""",
    "app/src/main/java/com/phoneagent/ui/chat/adapter/MessageAdapter.kt": """package com.phoneagent.ui.chat.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.phoneagent.data.local.entities.ConversationEntity

class MessageAdapter : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private val messages = mutableListOf<ConversationEntity>()

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        // Inflate layout based on viewType
        return MessageViewHolder(View(parent.context))
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        // Bind data
    }

    override fun getItemCount(): Int = messages.size
}
""",
    "app/src/main/java/com/phoneagent/ui/settings/SettingsActivity.kt": """package com.phoneagent.ui.settings

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Setup UI
    }
}
""",
    "app/src/main/java/com/phoneagent/ui/settings/SettingsViewModel.kt": """package com.phoneagent.ui.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {
    // Settings logic here
}
""",
    "app/src/main/java/com/phoneagent/ui/theme/Color.kt": """package com.phoneagent.ui.theme

// Colors are defined in XML, but you can define Compose colors here if needed later
""",
    "app/src/main/java/com/phoneagent/ui/theme/Theme.kt": """package com.phoneagent.ui.theme

// Theme definitions for Compose if needed later
""",
    "app/src/main/java/com/phoneagent/ui/theme/Type.kt": """package com.phoneagent.ui.theme

// Typography definitions for Compose if needed later
""",
    "app/src/main/java/com/phoneagent/service/PhoneAgentAccessibilityService.kt": """package com.phoneagent.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class PhoneAgentAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Handle accessibility events here
    }

    override fun onInterrupt() {
        // Handle interruption
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        // Service is connected and ready
    }
}
""",
    "app/src/main/java/com/phoneagent/service/ScreenCaptureService.kt": """package com.phoneagent.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class ScreenCaptureService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Setup foreground service and media projection logic here
        return START_STICKY
    }
}
""",
    "app/src/main/java/com/phoneagent/utils/Constants.kt": """package com.phoneagent.utils

object Constants {
    const val PREFS_NAME = "phone_agent_prefs"
}
""",
    "app/src/main/java/com/phoneagent/utils/Extensions.kt": """package com.phoneagent.utils

import android.content.Context
import android.widget.Toast

fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
"""
}

for filepath, content in files.items():
    directory = os.path.dirname(filepath)
    if directory and not os.path.exists(directory):
        os.makedirs(directory)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

print("Created all files successfully.")
