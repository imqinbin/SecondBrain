# VoiceMemo 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现语音备忘录应用，支持音量键触发录音、AI 意图识别、备忘录 CRUD，以及上下文感知（截图+定位）。

**Architecture:** 单 Activity + Navigation Compose 架构，Hilt 依赖注入，Room 本地存储。ForegroundService 处理录音→ASR→LLM→意图执行的完整处理链。AccessibilityService 检测音量键触发并按需截屏。

**Tech Stack:** Kotlin, Jetpack Compose, Hilt (kapt), Room, Navigation Compose, Retrofit+OkHttp, Coil, DataStore Preferences, Kotlin Coroutines+Flow

---

## Phase 1: Foundation

### Task 1: Gradle & Dependencies Setup

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts` (root)
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Update `gradle/libs.versions.toml` with all version catalogs**

Replace the entire contents of `gradle/libs.versions.toml` with:

```toml
[versions]
agp = "8.9.2"
kotlin = "2.0.21"
coreKtx = "1.16.0"
junit = "4.13.2"
junitVersion = "1.2.1"
espressoCore = "3.6.1"
lifecycleRuntimeKtx = "2.9.1"
activityCompose = "1.10.1"
composeBom = "2024.09.00"

hilt = "2.51.1"
hiltNavigationCompose = "1.2.0"
room = "2.6.1"
navigationCompose = "2.8.4"
coil = "2.7.0"
retrofit = "2.11.0"
okhttp = "4.12.0"
datastore = "1.1.1"
gson = "2.11.0"
coroutinesTest = "1.9.0"
mockk = "1.13.13"
archCoreTesting = "2.2.0"

[libraries]
# Core Android
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }

# Compose
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-android-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
androidx-hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }

# Room
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }

# Navigation
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }

# Image loading
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }

# Network
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-converter-gson = { group = "com.squareup.retrofit2", name = "converter-gson", version.ref = "retrofit" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
gson = { group = "com.google.code.gson", name = "gson", version.ref = "gson" }

# DataStore
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

# Testing
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesTest" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
androidx-arch-core-testing = { group = "androidx.arch.core", name = "core-testing", version.ref = "archCoreTesting" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
kapt = { id = "org.jetbrains.kotlin.kapt", version.ref = "kotlin" }
```

- [ ] **Step 2: Update root `build.gradle.kts` to add Hilt and kapt plugins**

Replace the entire contents of `build.gradle.kts` (root) with:

```kotlin
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.kapt) apply false
}
```

- [ ] **Step 3: Update `app/build.gradle.kts` with all dependencies and kapt plugin**

Replace the entire contents of `app/build.gradle.kts` with:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kapt)
}

android {
    namespace = "com.qb.secondbrain"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.qb.secondbrain"
        minSdk = 30
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Image loading
    implementation(libs.coil.compose)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

kapt {
    correctErrorTypes = true
}
```

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts
git commit -m "chore: add all project dependencies (Hilt, Room, Navigation, etc.)"
```

---

### Task 2: Application Class + Hilt Setup

**Files:**
- Create: `app/src/main/java/com/qb/secondbrain/SecondBrainApp.kt`
- Create: `app/src/main/java/com/qb/secondbrain/di/DatabaseModule.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create `SecondBrainApp.kt`**

Create file `app/src/main/java/com/qb/secondbrain/SecondBrainApp.kt`:

```kotlin
package com.qb.secondbrain

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SecondBrainApp : Application()
```

- [ ] **Step 2: Create `di/DatabaseModule.kt`**

Create file `app/src/main/java/com/qb/secondbrain/di/DatabaseModule.kt`:

```kotlin
package com.qb.secondbrain.di

import android.content.Context
import androidx.room.Room
import com.qb.secondbrain.data.local.Converters
import com.qb.secondbrain.data.local.MemoDao
import com.qb.secondbrain.data.local.MemoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMemoDatabase(
        @ApplicationContext context: Context
    ): MemoDatabase {
        return Room.databaseBuilder(
            context,
            MemoDatabase::class.java,
            "secondbrain_db"
        )
            .addTypeConverter(Converters())
            .build()
    }

    @Provides
    fun provideMemoDao(database: MemoDatabase): MemoDao {
        return database.memoDao()
    }
}
```

- [ ] **Step 3: Update `AndroidManifest.xml` to reference Application class**

Replace the entire contents of `app/src/main/AndroidManifest.xml` with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <application
        android:name=".SecondBrainApp"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.SecondBrain"
        tools:targetApi="31">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.SecondBrain">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/qb/secondbrain/SecondBrainApp.kt app/src/main/java/com/qb/secondbrain/di/DatabaseModule.kt app/src/main/AndroidManifest.xml
git commit -m "feat: add Application class and Hilt DI module"
```

---

### Task 3: Data Model (Memo + TypeConverters + LlmIntent)

**Files:**
- Create: `app/src/main/java/com/qb/secondbrain/data/model/Memo.kt`
- Create: `app/src/main/java/com/qb/secondbrain/data/local/Converters.kt`
- Create: `app/src/main/java/com/qb/secondbrain/data/model/LlmIntent.kt`
- Create: `app/src/test/java/com/qb/secondbrain/data/local/ConvertersTest.kt`

- [ ] **Step 1: Create `data/model/Memo.kt`**

Create file `app/src/main/java/com/qb/secondbrain/data/model/Memo.kt`:

```kotlin
package com.qb.secondbrain.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "memo",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["isDeleted"]),
        Index(value = ["reminderTime"])
    ]
)
data class Memo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val content: String = "",
    val rawText: String = "",
    val tags: List<String> = emptyList(),
    val imagePaths: List<ImagePath> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val reminderTime: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

data class ImagePath(
    val path: String,
    val source: ImageSource
)

enum class ImageSource {
    VOICE_SCREENSHOT,
    CAMERA,
    GALLERY
}
```

- [ ] **Step 2: Create `data/local/Converters.kt`**

Create file `app/src/main/java/com/qb/secondbrain/data/local/Converters.kt`:

```kotlin
package com.qb.secondbrain.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.qb.secondbrain.data.model.ImagePath

class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromImagePathList(value: List<ImagePath>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toImagePathList(value: String): List<ImagePath> {
        val type = object : TypeToken<List<ImagePath>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
}
```

- [ ] **Step 3: Create `data/model/LlmIntent.kt`**

Create file `app/src/main/java/com/qb/secondbrain/data/model/LlmIntent.kt`:

```kotlin
package com.qb.secondbrain.data.model

import com.google.gson.annotations.SerializedName

data class LlmIntent(
    val intent: MemoIntent,
    val needContext: ContextNeed = ContextNeed(),
    val content: String = "",
    val tags: List<String> = emptyList(),
    val reminderTime: String? = null,
    val queryKeywords: List<String> = emptyList()
)

data class ContextNeed(
    val screenshot: Boolean = false,
    val location: Boolean = false
)

enum class MemoIntent {
    @SerializedName("add")
    ADD,

    @SerializedName("query")
    QUERY,

    @SerializedName("update")
    UPDATE,

    @SerializedName("delete")
    DELETE
}
```

- [ ] **Step 4: Create `ConvertersTest.kt`**

Create file `app/src/test/java/com/qb/secondbrain/data/local/ConvertersTest.kt`:

```kotlin
package com.qb.secondbrain.data.local

import com.qb.secondbrain.data.model.ImagePath
import com.qb.secondbrain.data.model.ImageSource
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ConvertersTest {

    private lateinit var converters: Converters

    @Before
    fun setUp() {
        converters = Converters()
    }

    @Test
    fun `fromStringList converts empty list to empty JSON array`() {
        val result = converters.fromStringList(emptyList())
        assertEquals("[]", result)
    }

    @Test
    fun `fromStringList and toStringList round-trip preserves values`() {
        val original = listOf("tag1", "tag2", "tag3")
        val json = converters.fromStringList(original)
        val restored = converters.toStringList(json)
        assertEquals(original, restored)
    }

    @Test
    fun `toStringList handles null JSON gracefully`() {
        val result = converters.toStringList("null")
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `toStringList handles single element`() {
        val original = listOf("solo")
        val json = converters.fromStringList(original)
        val restored = converters.toStringList(json)
        assertEquals(original, restored)
    }

    @Test
    fun `fromImagePathList converts empty list to empty JSON array`() {
        val result = converters.fromImagePathList(emptyList())
        assertEquals("[]", result)
    }

    @Test
    fun `fromImagePathList and toImagePathList round-trip preserves values`() {
        val original = listOf(
            ImagePath("/data/cache/screenshot_123.png", ImageSource.VOICE_SCREENSHOT),
            ImagePath("/data/cache/photo_456.jpg", ImageSource.CAMERA),
            ImagePath("/data/cache/gallery_789.webp", ImageSource.GALLERY)
        )
        val json = converters.fromImagePathList(original)
        val restored = converters.toImagePathList(json)
        assertEquals(original, restored)
    }

    @Test
    fun `toImagePathList handles null JSON gracefully`() {
        val result = converters.toImagePathList("null")
        assertEquals(emptyList<ImagePath>(), result)
    }

    @Test
    fun `fromStringList preserves strings with special characters`() {
        val original = listOf("hello world", "特殊字符", "emoji", "path/to/file")
        val json = converters.fromStringList(original)
        val restored = converters.toStringList(json)
        assertEquals(original, restored)
    }

    @Test
    fun `fromImagePathList handles single element`() {
        val original = listOf(
            ImagePath("/storage/emulated/0/img.png", ImageSource.VOICE_SCREENSHOT)
        )
        val json = converters.fromImagePathList(original)
        val restored = converters.toImagePathList(json)
        assertEquals(original, restored)
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/qb/secondbrain/data/model/Memo.kt app/src/main/java/com/qb/secondbrain/data/local/Converters.kt app/src/main/java/com/qb/secondbrain/data/model/LlmIntent.kt app/src/test/java/com/qb/secondbrain/data/local/ConvertersTest.kt
git commit -m "feat: add Memo entity, TypeConverters, and LlmIntent model"
```

---

### Task 4: Room DAO + Database

**Files:**
- Create: `app/src/main/java/com/qb/secondbrain/data/local/MemoDao.kt`
- Create: `app/src/main/java/com/qb/secondbrain/data/local/MemoDatabase.kt`

- [ ] **Step 1: Create `data/local/MemoDao.kt`**

Create file `app/src/main/java/com/qb/secondbrain/data/local/MemoDao.kt`:

```kotlin
package com.qb.secondbrain.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.qb.secondbrain.data.model.Memo
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memo: Memo): Long

    @Update
    suspend fun update(memo: Memo)

    @Query("UPDATE memo SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM memo WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: Long): Memo?

    @Query("SELECT * FROM memo WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Memo>>

    @Query("""
        SELECT * FROM memo 
        WHERE isDeleted = 0 
        AND (content LIKE '%' || :keyword || '%' OR rawText LIKE '%' || :keyword || '%')
        ORDER BY createdAt DESC
    """)
    fun searchByKeyword(keyword: String): Flow<List<Memo>>

    @Query("""
        SELECT * FROM memo 
        WHERE isDeleted = 0 
        AND (
            content LIKE '%' || :kw1 || '%' 
            OR rawText LIKE '%' || :kw1 || '%'
            OR content LIKE '%' || :kw2 || '%' 
            OR rawText LIKE '%' || :kw2 || '%'
        )
        ORDER BY createdAt DESC
    """)
    fun searchByTwoKeywords(kw1: String, kw2: String): Flow<List<Memo>>

    @Query("""
        SELECT * FROM memo 
        WHERE isDeleted = 0 
        AND tags LIKE '%' || :tag || '%'
        ORDER BY createdAt DESC
    """)
    fun searchByTag(tag: String): Flow<List<Memo>>

    @Query("SELECT * FROM memo WHERE isDeleted = 0 AND reminderTime IS NOT NULL AND reminderTime <= :now")
    suspend fun getDueReminders(now: Long = System.currentTimeMillis()): List<Memo>
}
```

- [ ] **Step 2: Create `data/local/MemoDatabase.kt`**

Create file `app/src/main/java/com/qb/secondbrain/data/local/MemoDatabase.kt`:

```kotlin
package com.qb.secondbrain.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.qb.secondbrain.data.model.Memo

@Database(
    entities = [Memo::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MemoDatabase : RoomDatabase() {
    abstract fun memoDao(): MemoDao
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/qb/secondbrain/data/local/MemoDao.kt app/src/main/java/com/qb/secondbrain/data/local/MemoDatabase.kt
git commit -m "feat: add Room DAO and Database"
```

---

### Task 5: Repository Interface + Implementation + Test

**Files:**
- Create: `app/src/main/java/com/qb/secondbrain/data/repository/MemoRepository.kt`
- Create: `app/src/main/java/com/qb/secondbrain/data/repository/LocalMemoRepository.kt`
- Create: `app/src/test/java/com/qb/secondbrain/data/repository/LocalMemoRepositoryTest.kt`

- [ ] **Step 1: Create `data/repository/MemoRepository.kt` interface**

Create file `app/src/main/java/com/qb/secondbrain/data/repository/MemoRepository.kt`:

```kotlin
package com.qb.secondbrain.data.repository

import com.qb.secondbrain.data.model.Memo
import kotlinx.coroutines.flow.Flow

interface MemoRepository {

    suspend fun addMemo(memo: Memo): Long

    suspend fun updateMemo(memo: Memo)

    suspend fun deleteMemo(id: Long)

    suspend fun getMemoById(id: Long): Memo?

    fun getAllMemos(): Flow<List<Memo>>

    fun searchByKeywords(keywords: List<String>): Flow<List<Memo>>

    fun searchByTag(tag: String): Flow<List<Memo>>
}
```

- [ ] **Step 2: Create `data/repository/LocalMemoRepository.kt` implementation**

Create file `app/src/main/java/com/qb/secondbrain/data/repository/LocalMemoRepository.kt`:

```kotlin
package com.qb.secondbrain.data.repository

import com.qb.secondbrain.data.local.MemoDao
import com.qb.secondbrain.data.model.Memo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalMemoRepository @Inject constructor(
    private val memoDao: MemoDao
) : MemoRepository {

    override suspend fun addMemo(memo: Memo): Long {
        return memoDao.insert(memo)
    }

    override suspend fun updateMemo(memo: Memo) {
        memoDao.update(memo)
    }

    override suspend fun deleteMemo(id: Long) {
        memoDao.softDelete(id)
    }

    override suspend fun getMemoById(id: Long): Memo? {
        return memoDao.getById(id)
    }

    override fun getAllMemos(): Flow<List<Memo>> {
        return memoDao.getAll().distinctUntilChanged()
    }

    override fun searchByKeywords(keywords: List<String>): Flow<List<Memo>> {
        if (keywords.isEmpty()) {
            return flowOf(emptyList())
        }
        if (keywords.size == 1) {
            return memoDao.searchByKeyword(keywords[0]).distinctUntilChanged()
        }
        // For two keywords, use the dedicated query
        if (keywords.size == 2) {
            return memoDao.searchByTwoKeywords(keywords[0], keywords[1]).distinctUntilChanged()
        }
        // For more than two keywords, search by the first keyword
        // and filter results client-side in the ViewModel layer
        return memoDao.searchByKeyword(keywords[0]).distinctUntilChanged()
    }

    override fun searchByTag(tag: String): Flow<List<Memo>> {
        return memoDao.searchByTag(tag).distinctUntilChanged()
    }
}
```

- [ ] **Step 3: Create `LocalMemoRepositoryTest.kt`**

Create file `app/src/test/java/com/qb/secondbrain/data/repository/LocalMemoRepositoryTest.kt`:

```kotlin
package com.qb.secondbrain.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.context.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.qb.secondbrain.data.local.MemoDao
import com.qb.secondbrain.data.local.MemoDatabase
import com.qb.secondbrain.data.model.Memo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalMemoRepositoryTest {

    private lateinit var database: MemoDatabase
    private lateinit var memoDao: MemoDao
    private lateinit var repository: LocalMemoRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            MemoDatabase::class.java
        ).allowMainThreadQueries().build()
        memoDao = database.memoDao()
        repository = LocalMemoRepository(memoDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun addMemo inserts and returns id() = runTest {
        val memo = Memo(content = "Test memo", rawText = "test raw")
        val id = repository.addMemo(memo)

        assertTrue(id > 0)

        val retrieved = repository.getMemoById(id)
        assertNotNull(retrieved)
        assertEquals("Test memo", retrieved!!.content)
        assertEquals("test raw", retrieved.rawText)
    }

    @Test
    fun updateMemo updates existing memo() = runTest {
        val id = repository.addMemo(Memo(content = "Original"))
        val original = repository.getMemoById(id)!!

        val updated = original.copy(content = "Updated", updatedAt = System.currentTimeMillis())
        repository.updateMemo(updated)

        val retrieved = repository.getMemoById(id)
        assertEquals("Updated", retrieved!!.content)
    }

    @Test
    fun deleteMemo soft deletes memo() = runTest {
        val id = repository.addMemo(Memo(content = "To delete"))

        repository.deleteMemo(id)

        val retrieved = repository.getMemoById(id)
        assertNull(retrieved)
    }

    @Test
    fun getMemoById returns null for nonExistent id() = runTest {
        val result = repository.getMemoById(99999L)
        assertNull(result)
    }

    @Test
    fun getAllMemos returns only nonDeleted memos() = runTest {
        repository.addMemo(Memo(content = "Memo 1"))
        repository.addMemo(Memo(content = "Memo 2"))
        repository.addMemo(Memo(content = "Memo 3"))

        val all = repository.getAllMemos().first()
        assertEquals(3, all.size)

        repository.deleteMemo(all[0].id)

        val remaining = repository.getAllMemos().first()
        assertEquals(2, remaining.size)
    }

    @Test
    fun searchByKeywords with single keyword returns matching memos() = runTest {
        repository.addMemo(Memo(content = "海天公园停车场", rawText = ""))
        repository.addMemo(Memo(content = "今天天气不错", rawText = ""))
        repository.addMemo(Memo(content = "停车二维码在这里", rawText = ""))

        val results = repository.searchByKeywords(listOf("停车")).first()
        assertEquals(2, results.size)
        assertTrue(results.all { it.content.contains("停车") })
    }

    @Test
    fun searchByKeywords with empty keywords returns empty list() = runTest {
        repository.addMemo(Memo(content = "Some memo"))

        val results = repository.searchByKeywords(emptyList()).first()
        assertEquals(0, results.size)
    }

    @Test
    fun searchByTag returns memos with matching tag() = runTest {
        repository.addMemo(Memo(content = "Memo with tag", tags = listOf("停车", "二维码")))
        repository.addMemo(Memo(content = "Memo without tag", tags = listOf("工作")))

        val results = repository.searchByTag("停车").first()
        assertEquals(1, results.size)
        assertTrue(results[0].tags.contains("停车"))
    }

    @Test
    fun searchByKeywords matches rawText field() = runTest {
        repository.addMemo(Memo(content = "整理后内容", rawText = "原始语音文本包括关键词停车"))

        val results = repository.searchByKeywords(listOf("停车")).first()
        assertEquals(1, results.size)
        assertEquals("整理后内容", results[0].content)
    }

    @Test
    fun addMemo with all fields preserves data() = runTest {
        val memo = Memo(
            content = "海天公园停车场二维码",
            rawText = "记住这个海天公园的停车场二维码",
            tags = listOf("停车", "二维码", "海天公园"),
            latitude = 22.27,
            longitude = 113.56,
            address = "广东省珠海市海天公园",
            reminderTime = 1713000000000L
        )
        val id = repository.addMemo(memo)
        val retrieved = repository.getMemoById(id)!!

        assertEquals(memo.content, retrieved.content)
        assertEquals(memo.rawText, retrieved.rawText)
        assertEquals(memo.tags, retrieved.tags)
        assertEquals(memo.latitude, retrieved.latitude)
        assertEquals(memo.longitude, retrieved.longitude)
        assertEquals(memo.address, retrieved.address)
        assertEquals(memo.reminderTime, retrieved.reminderTime)
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/qb/secondbrain/data/repository/MemoRepository.kt app/src/main/java/com/qb/secondbrain/data/repository/LocalMemoRepository.kt app/src/test/java/com/qb/secondbrain/data/repository/LocalMemoRepositoryTest.kt
git commit -m "feat: add MemoRepository interface and local implementation with tests"
```

---

## Phase 2: Domain + UI

### Task 6: Domain Use Cases

- [ ] Create `domain/usecase/AddMemoUseCase.kt`
- [ ] Create `domain/usecase/QueryMemoUseCase.kt`
- [ ] Create `domain/usecase/UpdateMemoUseCase.kt`
- [ ] Create `domain/usecase/DeleteMemoUseCase.kt`
- [ ] Create unit tests for all 4 use cases in `test/` using MockK
- [ ] Commit: "feat: add domain use cases with tests"

**`app/src/main/java/com/qb/secondbrain/domain/usecase/AddMemoUseCase.kt`**

```kotlin
package com.qb.secondbrain.domain.usecase

import com.qb.secondbrain.data.model.Memo
import com.qb.secondbrain.data.repository.MemoRepository
import javax.inject.Inject

class AddMemoUseCase @Inject constructor(
    private val repository: MemoRepository
) {
    suspend operator fun invoke(memo: Memo): Long {
        return repository.addMemo(memo)
    }
}
```

**`app/src/main/java/com/qb/secondbrain/domain/usecase/QueryMemoUseCase.kt`**

```kotlin
package com.qb.secondbrain.domain.usecase

import com.qb.secondbrain.data.repository.MemoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class QueryMemoUseCase @Inject constructor(
    private val repository: MemoRepository
) {
    operator fun invoke(): Flow<List<Memo>> {
        return repository.getAllMemos()
    }

    suspend fun byId(id: Long): Memo? {
        return repository.getMemoById(id)
    }
}
```

**`app/src/main/java/com/qb/secondbrain/domain/usecase/UpdateMemoUseCase.kt`**

```kotlin
package com.qb.secondbrain.domain.usecase

import com.qb.secondbrain.data.model.Memo
import com.qb.secondbrain.data.repository.MemoRepository
import javax.inject.Inject

class UpdateMemoUseCase @Inject constructor(
    private val repository: MemoRepository
) {
    suspend operator fun invoke(memo: Memo) {
        repository.updateMemo(memo)
    }
}
```

**`app/src/main/java/com/qb/secondbrain/domain/usecase/DeleteMemoUseCase.kt`**

```kotlin
package com.qb.secondbrain.domain.usecase

import com.qb.secondbrain.data.model.Memo
import com.qb.secondbrain.data.repository.MemoRepository
import javax.inject.Inject

class DeleteMemoUseCase @Inject constructor(
    private val repository: MemoRepository
) {
    suspend operator fun invoke(memo: Memo) {
        repository.deleteMemo(memo)
    }
}
```

**`app/src/test/java/com/qb/secondbrain/domain/usecase/AddMemoUseCaseTest.kt`**

```kotlin
package com.qb.secondbrain.domain.usecase

import com.qb.secondbrain.data.local.ImagePath
import com.qb.secondbrain.data.model.Memo
import com.qb.secondbrain.data.repository.MemoRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AddMemoUseCaseTest {

    private lateinit var repository: MemoRepository
    private lateinit var useCase: AddMemoUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = AddMemoUseCase(repository)
    }

    @Test
    fun `invoke calls addMemo on repository and returns id`() = runTest {
        val memo = Memo(
            id = 0L,
            content = "Test memo",
            rawText = "raw test",
            tags = listOf("test"),
            imagePaths = emptyList(),
            latitude = null,
            longitude = null,
            address = null,
            reminderTime = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isDeleted = false
        )
        coEvery { repository.addMemo(memo) } returns 42L

        val result = useCase(memo)

        assertEquals(42L, result)
        coVerify(exactly = 1) { repository.addMemo(memo) }
    }

    @Test
    fun `invoke with memo containing images returns id`() = runTest {
        val memo = Memo(
            id = 0L,
            content = "Memo with image",
            rawText = "raw",
            tags = listOf("photo"),
            imagePaths = listOf(ImagePath("/sdcard/photo.jpg", "camera")),
            latitude = 39.9,
            longitude = 116.4,
            address = "Beijing",
            reminderTime = System.currentTimeMillis() + 3600000L,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isDeleted = false
        )
        coEvery { repository.addMemo(memo) } returns 100L

        val result = useCase(memo)

        assertEquals(100L, result)
        coVerify(exactly = 1) { repository.addMemo(memo) }
    }
}
```

**`app/src/test/java/com/qb/secondbrain/domain/usecase/QueryMemoUseCaseTest.kt`**

```kotlin
package com.qb.secondbrain.domain.usecase

import com.qb.secondbrain.data.model.Memo
import com.qb.secondbrain.data.repository.MemoRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class QueryMemoUseCaseTest {

    private lateinit var repository: MemoRepository
    private lateinit var useCase: QueryMemoUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = QueryMemoUseCase(repository)
    }

    @Test
    fun `invoke returns flow of memo list`() = runTest {
        val memos = listOf(
            Memo(
                id = 1L,
                content = "Memo 1",
                rawText = "raw1",
                tags = listOf("a"),
                imagePaths = emptyList(),
                latitude = null,
                longitude = null,
                address = null,
                reminderTime = null,
                createdAt = 1000L,
                updatedAt = 1000L,
                isDeleted = false
            ),
            Memo(
                id = 2L,
                content = "Memo 2",
                rawText = "raw2",
                tags = listOf("b"),
                imagePaths = emptyList(),
                latitude = null,
                longitude = null,
                address = null,
                reminderTime = null,
                createdAt = 2000L,
                updatedAt = 2000L,
                isDeleted = false
            )
        )
        every { repository.getAllMemos() } returns flowOf(memos)

        val result = useCase().first()

        assertEquals(2, result.size)
        assertEquals("Memo 1", result[0].content)
        assertEquals("Memo 2", result[1].content)
    }

    @Test
    fun `byId returns memo by id`() = runTest {
        val memo = Memo(
            id = 5L,
            content = "Specific memo",
            rawText = "raw",
            tags = emptyList(),
            imagePaths = emptyList(),
            latitude = null,
            longitude = null,
            address = null,
            reminderTime = null,
            createdAt = 1000L,
            updatedAt = 1000L,
            isDeleted = false
        )
        coEvery { repository.getMemoById(5L) } returns memo

        val result = useCase.byId(5L)

        assertEquals(5L, result?.id)
        assertEquals("Specific memo", result?.content)
        coVerify(exactly = 1) { repository.getMemoById(5L) }
    }

    @Test
    fun `byId returns null for non-existent id`() = runTest {
        coEvery { repository.getMemoById(999L) } returns null

        val result = useCase.byId(999L)

        assertEquals(null, result)
        coVerify(exactly = 1) { repository.getMemoById(999L) }
    }
}
```

**`app/src/test/java/com/qb/secondbrain/domain/usecase/UpdateMemoUseCaseTest.kt`**

```kotlin
package com.qb.secondbrain.domain.usecase

import com.qb.secondbrain.data.model.Memo
import com.qb.secondbrain.data.repository.MemoRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class UpdateMemoUseCaseTest {

    private lateinit var repository: MemoRepository
    private lateinit var useCase: UpdateMemoUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = UpdateMemoUseCase(repository)
    }

    @Test
    fun `invoke calls updateMemo on repository`() = runTest {
        val memo = Memo(
            id = 1L,
            content = "Updated content",
            rawText = "raw updated",
            tags = listOf("updated"),
            imagePaths = emptyList(),
            latitude = null,
            longitude = null,
            address = null,
            reminderTime = null,
            createdAt = 1000L,
            updatedAt = 2000L,
            isDeleted = false
        )

        useCase(memo)

        coVerify(exactly = 1) { repository.updateMemo(memo) }
    }

    @Test
    fun `invoke with tagged memo calls updateMemo`() = runTest {
        val memo = Memo(
            id = 2L,
            content = "Tagged memo",
            rawText = "raw",
            tags = listOf("tag1", "tag2"),
            imagePaths = emptyList(),
            latitude = 31.2,
            longitude = 121.5,
            address = "Shanghai",
            reminderTime = null,
            createdAt = 1000L,
            updatedAt = 3000L,
            isDeleted = false
        )

        useCase(memo)

        coVerify(exactly = 1) { repository.updateMemo(memo) }
    }
}
```

**`app/src/test/java/com/qb/secondbrain/domain/usecase/DeleteMemoUseCaseTest.kt`**

```kotlin
package com.qb.secondbrain.domain.usecase

import com.qb.secondbrain.data.model.Memo
import com.qb.secondbrain.data.repository.MemoRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class DeleteMemoUseCaseTest {

    private lateinit var repository: MemoRepository
    private lateinit var useCase: DeleteMemoUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = DeleteMemoUseCase(repository)
    }

    @Test
    fun `invoke calls deleteMemo on repository`() = runTest {
        val memo = Memo(
            id = 1L,
            content = "To delete",
            rawText = "raw",
            tags = emptyList(),
            imagePaths = emptyList(),
            latitude = null,
            longitude = null,
            address = null,
            reminderTime = null,
            createdAt = 1000L,
            updatedAt = 1000L,
            isDeleted = false
        )

        useCase(memo)

        coVerify(exactly = 1) { repository.deleteMemo(memo) }
    }

    @Test
    fun `invoke with already soft-deleted memo still calls deleteMemo`() = runTest {
        val memo = Memo(
            id = 3L,
            content = "Already deleted",
            rawText = "raw",
            tags = emptyList(),
            imagePaths = emptyList(),
            latitude = null,
            longitude = null,
            address = null,
            reminderTime = null,
            createdAt = 1000L,
            updatedAt = 1000L,
            isDeleted = true
        )

        useCase(memo)

        coVerify(exactly = 1) { repository.deleteMemo(memo) }
    }
}
```

---

### Task 7: Navigation + MainActivity Rewrite

- [ ] Create `ui/navigation/Route.kt` with sealed class Route
- [ ] Create `ui/navigation/AppNavigation.kt` with NavHost
- [ ] Rewrite `MainActivity.kt` with @AndroidEntryPoint and NavHost
- [ ] Remove Greeting composable if present
- [ ] Commit: "feat: add Navigation Compose setup and rewrite MainActivity"

**`app/src/main/java/com/qb/secondbrain/ui/navigation/Route.kt`**

```kotlin
package com.qb.secondbrain.ui.navigation

import kotlinx.serialization.Serializable

sealed class Route {
    @Serializable
    data object MemoList : Route()

    @Serializable
    data class MemoDetail(val id: Long) : Route()

    @Serializable
    data class MemoEdit(val id: Long? = null) : Route()

    @Serializable
    data class Search(val query: String? = null) : Route()

    @Serializable
    data object Settings : Route()
}
```

**`app/src/main/java/com/qb/secondbrain/ui/navigation/AppNavigation.kt`**

```kotlin
package com.qb.secondbrain.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.qb.secondbrain.ui.screen.MemoDetailScreen
import com.qb.secondbrain.ui.screen.MemoEditScreen
import com.qb.secondbrain.ui.screen.MemoListScreen
import com.qb.secondbrain.ui.screen.SearchScreen
import com.qb.secondbrain.ui.screen.SettingsScreen

@Composable
fun AppNavigation(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Route.MemoList
    ) {
        composable<Route.MemoList> {
            MemoListScreen(
                onMemoClick = { memoId ->
                    navController.navigate(Route.MemoDetail(id = memoId))
                },
                onAddMemo = {
                    navController.navigate(Route.MemoEdit(id = null))
                },
                onSearchClick = {
                    navController.navigate(Route.Search())
                },
                onSettingsClick = {
                    navController.navigate(Route.Settings)
                }
            )
        }

        composable<Route.MemoDetail> { navBackStackEntry ->
            val route = navBackStackEntry.toRoute<Route.MemoDetail>()
            MemoDetailScreen(
                memoId = route.id,
                onEditClick = { memoId ->
                    navController.navigate(Route.MemoEdit(id = memoId))
                },
                onDeleteClick = {
                    navController.popBackStack()
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable<Route.MemoEdit> { navBackStackEntry ->
            val route = navBackStackEntry.toRoute<Route.MemoEdit>()
            MemoEditScreen(
                memoId = route.id,
                onSaveComplete = {
                    navController.popBackStack()
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable<Route.Search> { navBackStackEntry ->
            val route = navBackStackEntry.toRoute<Route.Search>()
            SearchScreen(
                initialQuery = route.query,
                onMemoClick = { memoId ->
                    navController.navigate(Route.MemoDetail(id = memoId))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable<Route.Settings> {
            SettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
```

**`app/src/main/java/com/qb/secondbrain/MainActivity.kt`**

```kotlin
package com.qb.secondbrain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.qb.secondbrain.ui.navigation.AppNavigation
import com.qb.secondbrain.ui.theme.SecondBrainTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SecondBrainTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavigation(navController = navController)
                }
            }
        }
    }
}
```

---

### Task 8: MemoListScreen + ViewModel

- [ ] Create `ui/viewmodel/MemoListViewModel.kt` with @HiltViewModel
- [ ] Create `ui/screen/MemoListScreen.kt` with search bar, LazyColumn, SwipeToDismiss, FAB, empty state
- [ ] Commit: "feat: add MemoListScreen with ViewModel"

**`app/src/main/java/com/qb/secondbrain/ui/viewmodel/MemoListViewModel.kt`**

```kotlin
package com.qb.secondbrain.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qb.secondbrain.data.model.Memo
import com.qb.secondbrain.data.repository.MemoRepository
import com.qb.secondbrain.domain.usecase.DeleteMemoUseCase
import com.qb.secondbrain.domain.usecase.QueryMemoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemoListUiState(
    val memos: List<Memo> = emptyList(),
    val isLoading: Boolean = true,
    val recentlyDeleted: Memo? = null
)

@HiltViewModel
class MemoListViewModel @Inject constructor(
    private val queryMemoUseCase: QueryMemoUseCase,
    private val deleteMemoUseCase: DeleteMemoUseCase,
    private val repository: MemoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemoListUiState())
    val uiState: StateFlow<MemoListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            queryMemoUseCase()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
                .collect { memos ->
                    _uiState.update { it.copy(memos = memos, isLoading = false) }
                }
        }
    }

    fun deleteMemo(memo: Memo) {
        val deletedMemo = memo.copy(isDeleted = true, updatedAt = System.currentTimeMillis())
        viewModelScope.launch {
            deleteMemoUseCase(deletedMemo)
            _uiState.update { it.copy(recentlyDeleted = deletedMemo) }
        }
    }

    fun undoDelete() {
        val memoToRestore = _uiState.value.recentlyDeleted ?: return
        viewModelScope.launch {
            val restoredMemo = memoToRestore.copy(isDeleted = false, updatedAt = System.currentTimeMillis())
            repository.updateMemo(restoredMemo)
            _uiState.update { it.copy(recentlyDeleted = null) }
        }
    }

    fun clearRecentlyDeleted() {
        _uiState.update { it.copy(recentlyDeleted = null) }
    }
}
```

**`app/src/main/java/com/qb/secondbrain/ui/screen/MemoListScreen.kt`**

```kotlin
package com.qb.secondbrain.ui.screen

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.qb.secondbrain.data.model.Memo
import com.qb.secondbrain.ui.viewmodel.MemoListViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemoListScreen(
    onMemoClick: (Long) -> Unit,
    onAddMemo: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: MemoListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(uiState.recentlyDeleted) {
        val deleted = uiState.recentlyDeleted
        if (deleted != null) {
            val result = snackbarHostState.showSnackbar(
                message = "备忘录已删除",
                actionLabel = "撤销",
                duration = SnackbarDuration.Short
            )
            when (result) {
                SnackbarResult.ActionPerformed -> viewModel.undoDelete()
                SnackbarResult.Dismissed -> viewModel.clearRecentlyDeleted()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("备忘录") },
                modifier = Modifier.statusBarsPadding(),
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMemo) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加备忘录"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Accessibility service warning banner
            AccessibilityWarningBanner()

            // Search bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { onSearchClick() },
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "搜索备忘录",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.bodyLarge
                    )
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("加载中...", style = MaterialTheme.bodyLarge)
                }
            } else if (uiState.memos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "同时短按音量键开始记录",
                            style = MaterialTheme.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.memos,
                        key = { it.id }
                    ) { memo ->
                        SwipeableMemoCard(
                            memo = memo,
                            onMemoClick = { onMemoClick(memo.id) },
                            onDismiss = { viewModel.deleteMemo(memo) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableMemoCard(
    memo: Memo,
    onMemoClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                    else -> Color.Transparent
                },
                label = "dismiss-bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, MaterialTheme.shapes.medium)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "删除",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.bodyMedium
                )
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        MemoCard(
            memo = memo,
            onClick = onMemoClick
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemoCard(
    memo: Memo,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Content preview
            Text(
                text = memo.content,
                style = MaterialTheme.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Tags
            if (memo.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    memo.tags.forEach { tag ->
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.labelSmall
                                )
                            }
                        )
                    }
                }
            }

            // Bottom row: location, image thumbnail, time, reminder
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reminder icon
                if (memo.reminderTime != null) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "有提醒",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                // Location
                if (memo.address != null) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = memo.address,
                        style = MaterialTheme.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Spacer(modifier = Modifier.weight(1f))

                // Thumbnail
                if (memo.imagePaths.isNotEmpty()) {
                    AsyncImage(
                        model = File(memo.imagePaths.first().path),
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Time
                Text(
                    text = formatRelativeTime(memo.createdAt),
                    style = MaterialTheme.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AccessibilityWarningBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "无障碍服务未开启，语音录制功能不可用",
                style = MaterialTheme.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000L -> "刚刚"
        diff < 3_600_000L -> "${diff / 60_000L}分钟前"
        diff < 86_400_000L -> "${diff / 3_600_000L}小时前"
        diff < 604_800_000L -> "${diff / 86_400_000L}天前"
        else -> {
            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
```

---

### Task 9: MemoDetailScreen + ViewModel

- [ ] Create `ui/viewmodel/MemoDetailViewModel.kt`
- [ ] Create `ui/screen/MemoDetailScreen.kt` with full text, image gallery, map, tags, reminder, raw text section
- [ ] Commit: "feat: add MemoDetailScreen with ViewModel"

**`app/src/main/java/com/qb/secondbrain/ui/viewmodel/MemoDetailViewModel.kt`**

```kotlin
package com.qb.secondbrain.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qb.secondbrain.data.model.Memo
import com.qb.secondbrain.domain.usecase.DeleteMemoUseCase
import com.qb.secondbrain.domain.usecase.QueryMemoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemoDetailUiState(
    val memo: Memo? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isRawTextExpanded: Boolean = false
)

@HiltViewModel
class MemoDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val queryMemoUseCase: QueryMemoUseCase,
    private val deleteMemoUseCase: DeleteMemoUseCase
) : ViewModel() {

    private val memoId: Long = savedStateHandle["id"] ?: error("memoId is required")

    private val _uiState = MutableStateFlow(MemoDetailUiState())
    val uiState: StateFlow<MemoDetailUiState> = _uiState.asStateFlow()

    init {
        loadMemo()
    }

    private fun loadMemo() {
        viewModelScope.launch {
            try {
                val memo = queryMemoUseCase.byId(memoId)
                _uiState.update { it.copy(memo = memo, isLoading = false, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun deleteMemo(onDeleted: () -> Unit) {
        val memo = _uiState.value.memo ?: return
        viewModelScope.launch {
            val deletedMemo = memo.copy(
                isDeleted = true,
                updatedAt = System.currentTimeMillis()
            )
            deleteMemoUseCase(deletedMemo)
            onDeleted()
        }
    }

    fun toggleRawText() {
        _uiState.update { it.copy(isRawTextExpanded = !it.isRawTextExpanded) }
    }
}
```

**`app/src/main/java/com/qb/secondbrain/ui/screen/MemoDetailScreen.kt`**

```kotlin
package com.qb.secondbrain.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.qb.secondbrain.data.model.Memo
import com.qb.secondbrain.ui.viewmodel.MemoDetailViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemoDetailScreen(
    memoId: Long,
    onEditClick: (Long) -> Unit,
    onDeleteClick: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: MemoDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除备忘录") },
            text = { Text("确定要删除这条备忘录吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteMemo(onDeleted = onDeleteClick)
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("备忘录详情") },
                modifier = Modifier.statusBarsPadding(),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { uiState.memo?.let { onEditClick(it.id) } }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "编辑"
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "加载失败: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else if (uiState.memo != null) {
            val memo = uiState.memo!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Full text content
                Text(
                    text = memo.content,
                    style = MaterialTheme.bodyLarge,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Image gallery
                if (memo.imagePaths.isNotEmpty()) {
                    Text(
                        text = "图片",
                        style = MaterialTheme.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    var showFullscreen by remember { mutableStateOf(false) }
                    var fullscreenIndex by remember { mutableStateOf(0) }

                    val pagerState = rememberPagerState(
                        pageCount = { memo.imagePaths.size }
                    )
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        AsyncImage(
                            model = File(memo.imagePaths[page].path),
                            contentDescription = "图片 ${page + 1}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .clickable {
                                    fullscreenIndex = page
                                    showFullscreen = true
                                },
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Page indicator
                    if (memo.imagePaths.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(memo.imagePaths.size) { index ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (index == pagerState.currentPage) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                            }
                                        )
                                )
                            }
                        }
                    }

                    // Fullscreen overlay
                    if (showFullscreen) {
                        FullscreenImageViewer(
                            imagePaths = memo.imagePaths.map { it.path },
                            initialIndex = fullscreenIndex,
                            onDismiss = { showFullscreen = false }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Map section
                if (memo.latitude != null && memo.longitude != null) {
                    Text(
                        text = "位置",
                        style = MaterialTheme.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = memo.address ?: "${memo.latitude}, ${memo.longitude}",
                                    style = MaterialTheme.bodyMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    openMapNavigation(context, memo.latitude, memo.longitude, memo.address)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Text("导航到这里")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Tags
                if (memo.tags.isNotEmpty()) {
                    Text(
                        text = "标签",
                        style = MaterialTheme.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        memo.tags.forEach { tag ->
                            AssistChip(
                                onClick = { },
                                label = { Text(tag) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Reminder time
                if (memo.reminderTime != null) {
                    Text(
                        text = "提醒时间",
                        style = MaterialTheme.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatTimestamp(memo.reminderTime),
                            style = MaterialTheme.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Collapsible raw voice text
                if (memo.rawText.isNotBlank() && memo.rawText != memo.content) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleRawText() },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "原始语音文本",
                                    style = MaterialTheme.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = if (uiState.isRawTextExpanded) {
                                        Icons.Default.ExpandLess
                                    } else {
                                        Icons.Default.ExpandMore
                                    },
                                    contentDescription = if (uiState.isRawTextExpanded) "收起" else "展开"
                                )
                            }
                            AnimatedVisibility(visible = uiState.isRawTextExpanded) {
                                Text(
                                    text = memo.rawText,
                                    style = MaterialTheme.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Metadata
                Text(
                    text = "创建时间: ${formatTimestamp(memo.createdAt)}",
                    style = MaterialTheme.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "更新时间: ${formatTimestamp(memo.updatedAt)}",
                    style = MaterialTheme.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FullscreenImageViewer(
    imagePaths: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { imagePaths.size }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        HorizontalPager(state = pagerState) { page ->
            AsyncImage(
                model = File(imagePaths[page]),
                contentDescription = "图片 ${page + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        Text(
            text = "${pagerState.currentPage + 1} / ${imagePaths.size}",
            color = Color.White,
            style = MaterialTheme.bodyMedium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

private fun openMapNavigation(
    context: android.content.Context,
    latitude: Double,
    longitude: Double,
    address: String?
) {
    // Try AMap first
    val amapUri = Uri.parse("amapuri://route/plan/?dlat=$latitude&dlon=$longitude&dname=${address ?: ""}&dev=0&t=0")
    val amapIntent = Intent(Intent.ACTION_VIEW, amapUri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    // Fallback to geo: intent
    val geoUri = Uri.parse("geo:$latitude,$longitude?q=${address ?: "$latitude,$longitude"}")
    val geoIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    val chooserIntent = Intent.createChooser(geoIntent, "选择导航应用")
    try {
        context.startActivity(amapIntent)
    } catch (e: Exception) {
        try {
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            context.startActivity(geoIntent)
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
```

---

### Task 10: MemoEditScreen + ViewModel

- [ ] Create `ui/viewmodel/MemoEditViewModel.kt` with create/edit mode
- [ ] Create `ui/screen/MemoEditScreen.kt` with content input, location, images, tags, reminder
- [ ] Commit: "feat: add MemoEditScreen with ViewModel"

**`app/src/main/java/com/qb/secondbrain/ui/viewmodel/MemoEditViewModel.kt`**

```kotlin
package com.qb.secondbrain.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qb.secondbrain.data.local.ImagePath
import com.qb.secondbrain.data.model.Memo
import com.qb.secondbrain.domain.usecase.AddMemoUseCase
import com.qb.secondbrain.domain.usecase.QueryMemoUseCase
import com.qb.secondbrain.domain.usecase.UpdateMemoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemoEditUiState(
    val id: Long? = null,
    val isEditMode: Boolean = false,
    val content: String = "",
    val rawText: String = "",
    val tags: List<String> = emptyList(),
    val imagePaths: List<ImagePath> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val reminderTime: Long? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val tagInput: String = "",
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MemoEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val queryMemoUseCase: QueryMemoUseCase,
    private val addMemoUseCase: AddMemoUseCase,
    private val updateMemoUseCase: UpdateMemoUseCase
) : ViewModel() {

    private val editMemoId: Long? = savedStateHandle["id"]

    private val _uiState = MutableStateFlow(MemoEditUiState())
    val uiState: StateFlow<MemoEditUiState> = _uiState.asStateFlow()

    init {
        if (editMemoId != null) {
            _uiState.update { it.copy(isEditMode = true, id = editMemoId, isLoading = true) }
            loadMemo(editMemoId)
        }
    }

    private fun loadMemo(id: Long) {
        viewModelScope.launch {
            try {
                val memo = queryMemoUseCase.byId(id)
                if (memo != null) {
                    _uiState.update {
                        it.copy(
                            content = memo.content,
                            rawText = memo.rawText,
                            tags = memo.tags,
                            imagePaths = memo.imagePaths,
                            latitude = memo.latitude,
                            longitude = memo.longitude,
                            address = memo.address,
                            reminderTime = memo.reminderTime,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "备忘录未找到") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun updateContent(content: String) {
        _uiState.update { it.copy(content = content) }
    }

    fun updateTagInput(input: String) {
        _uiState.update { it.copy(tagInput = input) }
    }

    fun addTag() {
        val tag = _uiState.value.tagInput.trim()
        if (tag.isNotEmpty() && tag !in _uiState.value.tags) {
            _uiState.update { it.copy(tags = it.tags + tag, tagInput = "") }
        }
    }

    fun removeTag(tag: String) {
        _uiState.update { it.copy(tags = it.tags.filter { t -> t != tag }) }
    }

    fun addImagePath(imagePath: ImagePath) {
        _uiState.update { it.copy(imagePaths = it.imagePaths + imagePath) }
    }

    fun removeImagePath(index: Int) {
        _uiState.update { it.copy(imagePaths = it.imagePaths.toMutableList().apply { removeAt(index) }) }
    }

    fun updateLocation(latitude: Double?, longitude: Double?, address: String?) {
        _uiState.update { it.copy(latitude = latitude, longitude = longitude, address = address) }
    }

    fun clearLocation() {
        _uiState.update { it.copy(latitude = null, longitude = null, address = null) }
    }

    fun updateReminderTime(time: Long?) {
        _uiState.update { it.copy(reminderTime = time) }
    }

    fun saveMemo() {
        val state = _uiState.value
        if (state.content.isBlank()) {
            _uiState.update { it.copy(error = "内容不能为空") }
            return
        }

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                if (state.isEditMode && state.id != null) {
                    val memo = Memo(
                        id = state.id,
                        content = state.content,
                        rawText = state.rawText,
                        tags = state.tags,
                        imagePaths = state.imagePaths,
                        latitude = state.latitude,
                        longitude = state.longitude,
                        address = state.address,
                        reminderTime = state.reminderTime,
                        createdAt = 0L,
                        updatedAt = now,
                        isDeleted = false
                    )
                    updateMemoUseCase(memo)
                } else {
                    val memo = Memo(
                        id = 0L,
                        content = state.content,
                        rawText = state.rawText,
                        tags = state.tags,
                        imagePaths = state.imagePaths,
                        latitude = state.latitude,
                        longitude = state.longitude,
                        address = state.address,
                        reminderTime = state.reminderTime,
                        createdAt = now,
                        updatedAt = now,
                        isDeleted = false
                    )
                    addMemoUseCase(memo)
                }
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }
}
```

**`app/src/main/java/com/qb/secondbrain/ui/screen/MemoEditScreen.kt`**

```kotlin
package com.qb.secondbrain.ui.screen

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.qb.secondbrain.data.local.ImagePath
import com.qb.secondbrain.ui.viewmodel.MemoEditViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemoEditScreen(
    memoId: Long?,
    onSaveComplete: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: MemoEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onSaveComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.isEditMode) "编辑备忘录" else "新建备忘录")
                },
                modifier = Modifier.statusBarsPadding(),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveMemo() },
                        enabled = !uiState.isSaving && uiState.content.isNotBlank()
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("保存")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Error display
                uiState.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Content input
                OutlinedTextField(
                    value = uiState.content,
                    onValueChange = { viewModel.updateContent(it) },
                    label = { Text("内容") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    maxLines = 10
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Location section
                Text(
                    text = "位置",
                    style = MaterialTheme.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.address != null) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.address,
                                style = MaterialTheme.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            // Placeholder: request current location
                            viewModel.updateLocation(
                                latitude = 39.9042,
                                longitude = 116.4074,
                                address = "北京市 (定位占位)"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("重新定位")
                    }

                    if (uiState.address != null) {
                        OutlinedButton(
                            onClick = { viewModel.clearLocation() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("清除位置")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Image section
                Text(
                    text = "图片",
                    style = MaterialTheme.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.imagePaths.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(uiState.imagePaths) { index, imagePath ->
                            Box {
                                AsyncImage(
                                    model = File(imagePath.path),
                                    contentDescription = "图片 ${index + 1}",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(MaterialTheme.shapes.small),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { viewModel.removeImagePath(index) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(24.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.errorContainer
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "移除图片",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = {
                        // Placeholder: launch camera intent
                    }) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("拍照")
                    }
                    OutlinedButton(onClick = {
                        // Placeholder: launch gallery intent
                    }) {
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("从相册选择")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tags section
                Text(
                    text = "标签",
                    style = MaterialTheme.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        uiState.tags.forEach { tag ->
                            AssistChip(
                                onClick = { viewModel.removeTag(tag) },
                                label = { Text(tag) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "移除标签",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.tagInput,
                        onValueChange = { viewModel.updateTagInput(it) },
                        label = { Text("添加标签") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { viewModel.addTag() }) {
                        Text("添加")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reminder time section
                Text(
                    text = "提醒时间",
                    style = MaterialTheme.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = {
                        showDateTimePicker(context) { timestamp ->
                            viewModel.updateReminderTime(timestamp)
                        }
                    }) {
                        Text(
                            if (uiState.reminderTime != null) {
                                "修改提醒时间"
                            } else {
                                "设置提醒时间"
                            }
                        )
                    }
                    if (uiState.reminderTime != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatTimestamp(uiState.reminderTime),
                            style = MaterialTheme.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.updateReminderTime(null) }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "清除提醒"
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun showDateTimePicker(
    context: android.content.Context,
    onDateTimeSelected: (Long) -> Unit
) {
    val calendar = Calendar.getInstance()

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    onDateTimeSelected(calendar.timeInMillis)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
private fun OutlinedButton(
    onClick: () -> Unit,
    colors: ButtonDefaults.OutlinedButtonColors = ButtonDefaults.outlinedButtonColors(),
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.containerColor,
            contentColor = colors.contentColor
        ),
        border = null
    ) {
        content()
    }
}
```

---

### Task 11: SearchScreen + ViewModel

- [ ] Create `ui/viewmodel/SearchViewModel.kt` with keyword search
- [ ] Create `ui/screen/SearchScreen.kt` with search field, results list, tag filter
- [ ] Commit: "feat: add SearchScreen with ViewModel"

**`app/src/main/java/com/qb/secondbrain/ui/viewmodel/SearchViewModel.kt`**

```kotlin
package com.qb.secondbrain.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qb.secondbrain.data.model.Memo
import com.qb.secondbrain.data.repository.MemoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val selectedTag: String? = null,
    val allTags: List<String> = emptyList(),
    val results: List<Memo> = emptyList(),
    val isSearching: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MemoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val allMemosFlow = repository.getAllMemos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            allMemosFlow.collect { memos ->
                val tags = memos.flatMap { it.tags }.distinct().sorted()
                _uiState.update { it.copy(allTags = tags) }
                applyFilter(memos)
            }
        }
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        viewModelScope.launch {
            applyFilter(allMemosFlow.value)
        }
    }

    fun selectTag(tag: String?) {
        _uiState.update { it.copy(selectedTag = tag) }
        viewModelScope.launch {
            applyFilter(allMemosFlow.value)
        }
    }

    private fun applyFilter(memos: List<Memo>) {
        val state = _uiState.value
        val query = state.query.trim().lowercase()
        val selectedTag = state.selectedTag

        val filtered = memos.filter { memo ->
            val matchesQuery = query.isEmpty() ||
                memo.content.lowercase().contains(query) ||
                memo.rawText.lowercase().contains(query) ||
                memo.address?.lowercase()?.contains(query) == true ||
                memo.tags.any { it.lowercase().contains(query) }

            val matchesTag = selectedTag == null || selectedTag in memo.tags

            matchesQuery && matchesTag
        }

        _uiState.update { it.copy(results = filtered) }
    }

    fun initializeQuery(query: String?) {
        if (!query.isNullOrBlank()) {
            _uiState.update { it.copy(query = query) }
            viewModelScope.launch {
                applyFilter(allMemosFlow.value)
            }
        }
    }
}
```

**`app/src/main/java/com/qb/secondbrain/ui/screen/SearchScreen.kt`**

```kotlin
package com.qb.secondbrain.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.qb.secondbrain.data.model.Memo
import com.qb.secondbrain.ui.viewmodel.SearchViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    initialQuery: String?,
    onMemoClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.initializeQuery(initialQuery)
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = uiState.query,
                        onValueChange = { viewModel.updateQuery(it) },
                        placeholder = {
                            Text(
                                "搜索备忘录",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                },
                modifier = Modifier.statusBarsPadding(),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tag filter chips
            if (uiState.allTags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = uiState.selectedTag == null,
                        onClick = { viewModel.selectTag(null) },
                        label = { Text("全部") }
                    )
                    uiState.allTags.forEach { tag ->
                        FilterChip(
                            selected = uiState.selectedTag == tag,
                            onClick = { viewModel.selectTag(tag) },
                            label = { Text(tag) }
                        )
                    }
                }
            }

            // Results
            if (uiState.results.isEmpty() && uiState.query.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "没有找到匹配的备忘录",
                            style = MaterialTheme.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (uiState.results.isEmpty() && uiState.query.isBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "输入关键词搜索",
                        style = MaterialTheme.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.results,
                        key = { it.id }
                    ) { memo ->
                        SearchResultCard(
                            memo = memo,
                            onClick = { onMemoClick(memo.id) },
                            searchQuery = uiState.query.trim()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchResultCard(
    memo: Memo,
    onClick: () -> Unit,
    searchQuery: String
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = memo.content,
                style = MaterialTheme.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (memo.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    memo.tags.forEach { tag ->
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.labelSmall
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (memo.reminderTime != null) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "有提醒",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                if (memo.address != null) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = memo.address,
                        style = MaterialTheme.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Spacer(modifier = Modifier.weight(1f))

                if (memo.imagePaths.isNotEmpty()) {
                    AsyncImage(
                        model = File(memo.imagePaths.first().path),
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(MaterialTheme.shapes.extraSmall),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = formatRelativeTime(memo.createdAt),
                    style = MaterialTheme.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000L -> "刚刚"
        diff < 3_600_000L -> "${diff / 60_000L}分钟前"
        diff < 86_400_000L -> "${diff / 3_600_000L}小时前"
        diff < 604_800_000L -> "${diff / 86_400_000L}天前"
        else -> {
            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
```

---

### Task 12: SettingsScreen + ViewModel

- [ ] Create `data/local/SettingsDataStore.kt` wrapping DataStore Preferences
- [ ] Create `ui/viewmodel/SettingsViewModel.kt` using SettingsDataStore
- [ ] Create `ui/screen/SettingsScreen.kt` with all settings UI
- [ ] Commit: "feat: add SettingsScreen with DataStore"

**`app/src/main/java/com/qb/secondbrain/data/local/SettingsDataStore.kt`**

```kotlin
package com.qb.secondbrain.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val ASR_ENGINE = stringPreferencesKey("asr_engine")
        val LLM_API_URL = stringPreferencesKey("llm_api_url")
        val LLM_API_KEY = stringPreferencesKey("llm_api_key")
        val LLM_MODEL = stringPreferencesKey("llm_model")
        val MAX_RECORDING_DURATION = intPreferencesKey("max_recording_duration")
    }

    val asrEngine: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.ASR_ENGINE] ?: "科大讯飞"
    }

    val llmApiUrl: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.LLM_API_URL] ?: ""
    }

    val llmApiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.LLM_API_KEY] ?: ""
    }

    val llmModel: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.LLM_MODEL] ?: "gpt-4o-mini"
    }

    val maxRecordingDuration: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[Keys.MAX_RECORDING_DURATION] ?: 60
    }

    suspend fun setAsrEngine(engine: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.ASR_ENGINE] = engine
        }
    }

    suspend fun setLlmApiUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.LLM_API_URL] = url
        }
    }

    suspend fun setLlmApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.LLM_API_KEY] = key
        }
    }

    suspend fun setLlmModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.LLM_MODEL] = model
        }
    }

    suspend fun setMaxRecordingDuration(durationSeconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.MAX_RECORDING_DURATION] = durationSeconds
        }
    }
}
```

**`app/src/main/java/com/qb/secondbrain/ui/viewmodel/SettingsViewModel.kt`**

```kotlin
package com.qb.secondbrain.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qb.secondbrain.data.local.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val asrEngine: String = "科大讯飞",
    val llmApiUrl: String = "",
    val llmApiKey: String = "",
    val llmModel: String = "gpt-4o-mini",
    val maxRecordingDuration: Int = 60,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsDataStore.asrEngine,
                settingsDataStore.llmApiUrl,
                settingsDataStore.llmApiKey,
                settingsDataStore.llmModel,
                settingsDataStore.maxRecordingDuration
            ) { asrEngine, llmApiUrl, llmApiKey, llmModel, maxDuration ->
                SettingsUiState(
                    asrEngine = asrEngine,
                    llmApiUrl = llmApiUrl,
                    llmApiKey = llmApiKey,
                    llmModel = llmModel,
                    maxRecordingDuration = maxDuration
                )
            }.collect { state ->
                _uiState.update { state }
            }
        }
    }

    fun setAsrEngine(engine: String) {
        viewModelScope.launch {
            settingsDataStore.setAsrEngine(engine)
        }
    }

    fun setLlmApiUrl(url: String) {
        viewModelScope.launch {
            settingsDataStore.setLlmApiUrl(url)
        }
    }

    fun setLlmApiKey(key: String) {
        viewModelScope.launch {
            settingsDataStore.setLlmApiKey(key)
        }
    }

    fun setLlmModel(model: String) {
        viewModelScope.launch {
            settingsDataStore.setLlmModel(model)
        }
    }

    fun setMaxRecordingDuration(durationSeconds: Int) {
        viewModelScope.launch {
            settingsDataStore.setMaxRecordingDuration(durationSeconds)
        }
    }
}
```

**`app/src/main/java/com/qb/secondbrain/ui/screen/SettingsScreen.kt`**

```kotlin
package com.qb.secondbrain.ui.screen

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qb.secondbrain.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                modifier = Modifier.statusBarsPadding(),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ASR Engine Selection
            SettingsSectionTitle(title = "语音识别引擎")
            AsrEngineDropdown(
                selectedEngine = uiState.asrEngine,
                onEngineSelected = { viewModel.setAsrEngine(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // LLM Configuration
            SettingsSectionTitle(title = "大语言模型配置")
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.llmApiUrl,
                onValueChange = { viewModel.setLlmApiUrl(it) },
                label = { Text("API URL") },
                placeholder = { Text("https://api.openai.com/v1") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.llmApiKey,
                onValueChange = { viewModel.setLlmApiKey(it) },
                label = { Text("API Key") },
                placeholder = { Text("sk-...") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.llmModel,
                onValueChange = { viewModel.setLlmModel(it) },
                label = { Text("模型名称") },
                placeholder = { Text("gpt-4o-mini") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Max Recording Duration
            SettingsSectionTitle(title = "最大录音时长")
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${uiState.maxRecordingDuration} 秒",
                style = MaterialTheme.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = uiState.maxRecordingDuration.toFloat(),
                onValueChange = { viewModel.setMaxRecordingDuration(it.toInt()) },
                valueRange = 10f..300f,
                steps = 28,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "10秒",
                    style = MaterialTheme.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "300秒",
                    style = MaterialTheme.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Data Export
            SettingsSectionTitle(title = "数据管理")
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    Toast.makeText(context, "数据导出功能开发中", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("导出数据")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // About section
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            SettingsSectionTitle(title = "关于")
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "SecondBrain",
                        style = MaterialTheme.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "版本 1.0.0",
                        style = MaterialTheme.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "你的第二大脑 - 智能语音备忘录",
                        style = MaterialTheme.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AsrEngineDropdown(
    selectedEngine: String,
    onEngineSelected: (String) -> Unit
) {
    val engines = listOf("科大讯飞", "百度", "离线Whisper")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedEngine,
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            engines.forEach { engine ->
                DropdownMenuItem(
                    text = { Text(engine) },
                    onClick = {
                        onEngineSelected(engine)
                        expanded = false
                    }
                )
            }
        }
    }
}
```

---

## Phase 3: Services

### Task 13: NotificationHelper

- [ ] Create `notification/NotificationHelper.kt` with notification channel setup and builder methods

**File: `app/src/main/java/com/qb/secondbrain/notification/NotificationHelper.kt`**

```kotlin
package com.qb.secondbrain.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.qb.secondbrain.MainActivity
import java.util.concurrent.atomic.AtomicInteger

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_RECORDING = "channel_recording"
        const val CHANNEL_RESULT = "channel_result"
        const val CHANNEL_ERROR = "channel_error"

        const val NOTIFICATION_ID_RECORDING = 1001
        const val NOTIFICATION_ID_ADD_RESULT = 2001
        const val NOTIFICATION_ID_QUERY_RESULT = 2002
        const val NOTIFICATION_ID_UPDATE_RESULT = 2003
        const val NOTIFICATION_ID_DELETE_RESULT = 2004
        const val NOTIFICATION_ID_ERROR = 3001

        const val ACTION_CANCEL_RECORDING = "com.qb.secondbrain.ACTION_CANCEL_RECORDING"
        const val ACTION_UNDO_DELETE = "com.qb.secondbrain.ACTION_UNDO_DELETE"

        const val EXTRA_MEMO_ID = "extra_memo_id"
        const val EXTRA_CONTENT = "extra_content"
        const val EXTRA_KEYWORDS = "extra_keywords"

        private val requestCodeCounter = AtomicInteger(0)

        fun nextRequestCode(): Int = requestCodeCounter.incrementAndGet()
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val recordingChannel = NotificationChannel(
                CHANNEL_RECORDING,
                "录音状态",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示录音进行中的状态"
                setShowBadge(false)
            }

            val resultChannel = NotificationChannel(
                CHANNEL_RESULT,
                "处理结果",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "显示备忘录操作的结果通知"
                enableVibration(true)
                setShowBadge(true)
            }

            val errorChannel = NotificationChannel(
                CHANNEL_ERROR,
                "错误提示",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "显示处理失败的错误信息"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannels(
                listOf(recordingChannel, resultChannel, errorChannel)
            )
        }
    }

    fun recordingNotification(): Notification {
        val cancelIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_CANCEL_RECORDING
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            nextRequestCode(),
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_RECORDING)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("正在录音...")
            .setContentText("再次按下音量键结束录音")
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "取消",
                cancelPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    fun addResultNotification(memoId: Long, content: String): Notification {
        val detailIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = Intent.ACTION_VIEW
            putExtra("navigate_to", "memo_detail")
            putExtra(EXTRA_MEMO_ID, memoId)
        }
        val detailPendingIntent = PendingIntent.getActivity(
            context,
            nextRequestCode(),
            detailIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_RESULT)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("已添加：${truncate(content, 50)}")
            .setContentText(content)
            .setContentIntent(detailPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
    }

    fun queryResultNotification(keywords: List<String>, results: List<String>): Notification {
        val searchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = Intent.ACTION_SEARCH
            putExtra("navigate_to", "search")
            putStringArrayListExtra(EXTRA_KEYWORDS, ArrayList(keywords))
        }
        val searchPendingIntent = PendingIntent.getActivity(
            context,
            nextRequestCode(),
            searchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resultSummary = if (results.isEmpty()) {
            "未找到相关备忘录"
        } else {
            results.take(3).mapIndexed { index, text ->
                "${index + 1}.${truncate(text, 30)}"
            }.joinToString("  ")
        }

        return NotificationCompat.Builder(context, CHANNEL_RESULT)
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setContentTitle("找到 ${results.size} 条：$resultSummary")
            .setContentText(keywords.joinToString(" "))
            .setContentIntent(searchPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
    }

    fun updateResultNotification(memoId: Long, content: String): Notification {
        val detailIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = Intent.ACTION_VIEW
            putExtra("navigate_to", "memo_detail")
            putExtra(EXTRA_MEMO_ID, memoId)
        }
        val detailPendingIntent = PendingIntent.getActivity(
            context,
            nextRequestCode(),
            detailIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_RESULT)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("已修改：${truncate(content, 50)}")
            .setContentText(content)
            .setContentIntent(detailPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
    }

    fun deleteResultNotification(content: String): Notification {
        val undoIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_UNDO_DELETE
            putExtra(EXTRA_CONTENT, content)
        }
        val undoPendingIntent = PendingIntent.getBroadcast(
            context,
            nextRequestCode(),
            undoIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_RESULT)
            .setSmallIcon(android.R.drawable.ic_menu_delete)
            .setContentTitle("已删除：${truncate(content, 50)}")
            .setContentText("点击撤销恢复（5秒内有效）")
            .addAction(
                android.R.drawable.ic_menu_revert,
                "撤销",
                undoPendingIntent
            )
            .setTimeoutAfter(5000)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
    }

    fun errorNotification(message: String): Notification {
        val settingsIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = Intent.ACTION_MAIN
            putExtra("navigate_to", "settings")
        }
        val settingsPendingIntent = PendingIntent.getActivity(
            context,
            nextRequestCode(),
            settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ERROR)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("处理失败：$message")
            .setContentText("点击查看设置")
            .setContentIntent(settingsPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .build()
    }

    fun notifyAddResult(memoId: Long, content: String) {
        notificationManager.notify(
            NOTIFICATION_ID_ADD_RESULT,
            addResultNotification(memoId, content)
        )
    }

    fun notifyQueryResult(keywords: List<String>, results: List<String>) {
        notificationManager.notify(
            NOTIFICATION_ID_QUERY_RESULT,
            queryResultNotification(keywords, results)
        )
    }

    fun notifyUpdateResult(memoId: Long, content: String) {
        notificationManager.notify(
            NOTIFICATION_ID_UPDATE_RESULT,
            updateResultNotification(memoId, content)
        )
    }

    fun notifyDeleteResult(content: String) {
        notificationManager.notify(
            NOTIFICATION_ID_DELETE_RESULT,
            deleteResultNotification(content)
        )
    }

    fun notifyError(message: String) {
        notificationManager.notify(
            NOTIFICATION_ID_ERROR,
            errorNotification(message)
        )
    }

    fun cancelRecordingNotification() {
        notificationManager.cancel(NOTIFICATION_ID_RECORDING)
    }

    private fun truncate(text: String, maxLength: Int): String {
        return if (text.length <= maxLength) text else text.take(maxLength) + "..."
    }
}
```

**File: `app/src/main/java/com/qb/secondbrain/notification/NotificationActionReceiver.kt`**

```kotlin
package com.qb.secondbrain.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationAction"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            NotificationHelper.ACTION_CANCEL_RECORDING -> {
                Log.d(TAG, "Recording cancelled via notification")
            }
            NotificationHelper.ACTION_UNDO_DELETE -> {
                val content = intent.getStringExtra(NotificationHelper.EXTRA_CONTENT)
                Log.d(TAG, "Undo delete requested for: $content")
            }
        }
    }
}
```

- [ ] Commit: `feat: add NotificationHelper with channel setup and builders`

---

### Task 14: AudioRecorder

- [ ] Create `service/AudioRecorder.kt` with PCM/WAV recording capability

**File: `app/src/main/java/com/qb/secondbrain/service/AudioRecorder.kt`**

```kotlin
package com.qb.secondbrain.service

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AudioRecorder(
    private val cacheDir: File,
    private val maxDurationMs: Long = 60_000L,
    private val sampleRate: Int = 16000,
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
) {

    companion object {
        private const val TAG = "AudioRecorder"
        private const val WAV_HEADER_SIZE = 44
    }

    private val isRecording = AtomicBoolean(false)

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var outputFile: File? = null
    private var onMaxDurationReached: (() -> Unit)? = null

    val currentlyRecording: Boolean
        get() = isRecording.get()

    fun startRecording(
        outputFileName: String = "recording_${System.currentTimeMillis()}.wav",
        onMaxDurationReached: (() -> Unit)? = null
    ): File {
        if (isRecording.get()) {
            Log.w(TAG, "Already recording, ignoring start request")
            return outputFile ?: throw IllegalStateException("Already recording but no output file")
        }

        this.onMaxDurationReached = onMaxDurationReached
        val file = File(cacheDir, outputFileName)
        val rawFile = File(cacheDir, "${outputFileName}.raw")
        outputFile = file

        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            throw IllegalStateException("Unable to get minimum buffer size for audio recording")
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize * 2
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord?.release()
            audioRecord = null
            throw IllegalStateException("AudioRecord not initialized")
        }

        isRecording.set(true)
        audioRecord?.startRecording()

        recordingJob = CoroutineScope(Dispatchers.IO).launch {
            writeRawData(rawFile, bufferSize)
            if (isActive || isRecording.get()) {
                convertRawToWav(rawFile, file)
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            delay(maxDurationMs)
            if (isRecording.get()) {
                Log.d(TAG, "Max duration reached, auto-stopping recording")
                withContext(Dispatchers.Main) {
                    onMaxDurationReached?.invoke()
                }
            }
        }

        Log.d(TAG, "Recording started, output: ${file.absolutePath}")
        return file
    }

    suspend fun stopRecording(): File? {
        if (!isRecording.get()) {
            Log.w(TAG, "Not recording, ignoring stop request")
            return null
        }

        isRecording.set(false)

        try {
            audioRecord?.stop()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        }

        try {
            audioRecord?.release()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error releasing AudioRecord", e)
        }
        audioRecord = null

        recordingJob?.join()
        recordingJob = null

        val result = outputFile
        Log.d(TAG, "Recording stopped, file: ${result?.absolutePath}")
        return result
    }

    fun release() {
        if (isRecording.get()) {
            isRecording.set(false)
            try {
                audioRecord?.stop()
            } catch (_: IllegalStateException) {
            }
            try {
                audioRecord?.release()
            } catch (_: IllegalStateException) {
            }
            audioRecord = null
            recordingJob?.cancel()
            recordingJob = null
        }
        cleanupTempFiles()
    }

    private suspend fun writeRawData(rawFile: File, bufferSize: Int) {
        withContext(Dispatchers.IO) {
            val data = audioRecord ?: return@withContext
            val buffer = ShortArray(bufferSize)
            var fos: FileOutputStream? = null

            try {
                fos = FileOutputStream(rawFile)
                while (isRecording.get()) {
                    val read = data.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        val byteBuffer = ByteArray(read * 2)
                        for (i in 0 until read) {
                            val sample = buffer[i]
                            byteBuffer[i * 2] = (sample.toInt() and 0xFF).toByte()
                            byteBuffer[i * 2 + 1] = (sample.toInt() shr 8 and 0xFF).toByte()
                        }
                        fos.write(byteBuffer)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error writing raw audio data", e)
            } finally {
                fos?.flush()
                fos?.close()
            }
        }
    }

    private suspend fun convertRawToWav(rawFile: File, wavFile: File) {
        withContext(Dispatchers.IO) {
            if (!rawFile.exists()) {
                Log.e(TAG, "Raw file does not exist: ${rawFile.absolutePath}")
                return@withContext
            }

            val rawData = rawFile.readBytes()
            val channels = if (channelConfig == AudioFormat.CHANNEL_IN_MONO) 1 else 2
            val bitsPerSample = if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) 16 else 8
            val byteRate = sampleRate * channels * bitsPerSample / 8
            val blockAlign = channels * bitsPerSample / 8
            val dataSize = rawData.size
            val totalSize = dataSize + WAV_HEADER_SIZE

            val header = ByteArray(WAV_HEADER_SIZE)
            writeString(header, 0, "RIFF")
            writeInt(header, 4, totalSize - 8)
            writeString(header, 8, "WAVE")
            writeString(header, 12, "fmt ")
            writeInt(header, 16, 16)
            writeShort(header, 20, 1.toShort())
            writeShort(header, 22, channels.toShort())
            writeInt(header, 24, sampleRate)
            writeInt(header, 28, byteRate)
            writeShort(header, 32, blockAlign.toShort())
            writeShort(header, 34, bitsPerSample.toShort())
            writeString(header, 36, "data")
            writeInt(header, 40, dataSize)

            wavFile.writeBytes(header + rawData)

            rawFile.delete()

            Log.d(TAG, "WAV file created: ${wavFile.absolutePath}, size: ${wavFile.length()}")
        }
    }

    private fun writeString(buffer: ByteArray, offset: Int, value: String) {
        for (i in value.indices) {
            buffer[offset + i] = (value[i].code and 0xFF).toByte()
        }
    }

    private fun writeInt(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset] = (value and 0xFF).toByte()
        buffer[offset + 1] = (value shr 8 and 0xFF).toByte()
        buffer[offset + 2] = (value shr 16 and 0xFF).toByte()
        buffer[offset + 3] = (value shr 24 and 0xFF).toByte()
    }

    private fun writeShort(buffer: ByteArray, offset: Int, value: Short) {
        buffer[offset] = (value.toInt() and 0xFF).toByte()
        buffer[offset + 1] = (value.toInt() shr 8 and 0xFF).toByte()
    }

    private fun cleanupTempFiles() {
        cacheDir.listFiles()?.filter {
            it.name.endsWith(".raw")
        }?.forEach { it.delete() }
    }
}
```

- [ ] Commit: `feat: add AudioRecorder with WAV output`

---

### Task 15: ASR Engine Interface + Implementations

- [ ] Create `asr/AsrEngine.kt` interface

**File: `app/src/main/java/com/qb/secondbrain/asr/AsrEngine.kt`**

```kotlin
package com.qb.secondbrain.asr

import java.io.File

interface AsrEngine {
    suspend fun recognize(audioFile: File): Result<String>
}
```

- [ ] Create `asr/XfyunAsrEngine.kt` stub

**File: `app/src/main/java/com/qb/secondbrain/asr/XfyunAsrEngine.kt`**

```kotlin
package com.qb.secondbrain.asr

import java.io.File
import javax.inject.Inject

class XfyunAsrEngine @Inject constructor() : AsrEngine {

    override suspend fun recognize(audioFile: File): Result<String> {
        return Result.failure(UnsupportedOperationException("请先配置科大讯飞 SDK"))
    }
}
```

- [ ] Create `asr/BaiduAsrEngine.kt` stub

**File: `app/src/main/java/com/qb/secondbrain/asr/BaiduAsrEngine.kt`**

```kotlin
package com.qb.secondbrain.asr

import java.io.File
import javax.inject.Inject

class BaiduAsrEngine @Inject constructor() : AsrEngine {

    override suspend fun recognize(audioFile: File): Result<String> {
        return Result.failure(UnsupportedOperationException("请先配置百度语音 SDK"))
    }
}
```

- [ ] Create `asr/WhisperAsrEngine.kt` stub

**File: `app/src/main/java/com/qb/secondbrain/asr/WhisperAsrEngine.kt`**

```kotlin
package com.qb.secondbrain.asr

import java.io.File
import javax.inject.Inject

class WhisperAsrEngine @Inject constructor() : AsrEngine {

    override suspend fun recognize(audioFile: File): Result<String> {
        return Result.failure(UnsupportedOperationException("请先配置 Whisper 模型"))
    }
}
```

- [ ] Create `asr/AsrEngineFactory.kt`

**File: `app/src/main/java/com/qb/secondbrain/asr/AsrEngineFactory.kt`**

```kotlin
package com.qb.secondbrain.asr

import javax.inject.Inject
import javax.inject.Provider

class AsrEngineFactory @Inject constructor(
    private val xfyunEngineProvider: Provider<XfyunAsrEngine>,
    private val baiduEngineProvider: Provider<BaiduAsrEngine>,
    private val whisperEngineProvider: Provider<WhisperAsrEngine>
) {

    companion object {
        const val ENGINE_XFYUN = "xfyun"
        const val ENGINE_BAIDU = "baidu"
        const val ENGINE_WHISPER = "whisper"
    }

    fun getEngine(engineName: String): AsrEngine {
        return when (engineName.lowercase()) {
            ENGINE_XFYUN -> xfyunEngineProvider.get()
            ENGINE_BAIDU -> baiduEngineProvider.get()
            ENGINE_WHISPER -> whisperEngineProvider.get()
            else -> whisperEngineProvider.get()
        }
    }
}
```

- [ ] Write unit test for AsrEngineFactory

**File: `app/src/test/java/com/qb/secondbrain/asr/AsrEngineFactoryTest.kt`**

```kotlin
package com.qb.secondbrain.asr

import javax.inject.Provider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AsrEngineFactoryTest {

    private lateinit var factory: AsrEngineFactory

    @Before
    fun setUp() {
        factory = AsrEngineFactory(
            xfyunEngineProvider = Provider { XfyunAsrEngine() },
            baiduEngineProvider = Provider { BaiduAsrEngine() },
            whisperEngineProvider = Provider { WhisperAsrEngine() }
        )
    }

    @Test
    fun `getEngine returns XfyunAsrEngine for xfyun`() {
        val engine = factory.getEngine("xfyun")
        assertEquals(XfyunAsrEngine::class.java, engine::class.java)
    }

    @Test
    fun `getEngine returns BaiduAsrEngine for baidu`() {
        val engine = factory.getEngine("baidu")
        assertEquals(BaiduAsrEngine::class.java, engine::class.java)
    }

    @Test
    fun `getEngine returns WhisperAsrEngine for whisper`() {
        val engine = factory.getEngine("whisper")
        assertEquals(WhisperAsrEngine::class.java, engine::class.java)
    }

    @Test
    fun `getEngine returns WhisperAsrEngine for unknown engine name`() {
        val engine = factory.getEngine("unknown")
        assertEquals(WhisperAsrEngine::class.java, engine::class.java)
    }

    @Test
    fun `getEngine is case insensitive`() {
        val engine = factory.getEngine("XFYUN")
        assertEquals(XfyunAsrEngine::class.java, engine::class.java)
    }

    @Test
    fun `xfyun engine returns failure with unsupported message`() = kotlinx.coroutines.test.runTest {
        val engine = factory.getEngine("xfyun")
        val result = engine.recognize(java.io.File("test.wav"))
        assert(result.isFailure)
        assert(result.exceptionOrNull() is UnsupportedOperationException)
        assert(result.exceptionOrNull()?.message == "请先配置科大讯飞 SDK")
    }

    @Test
    fun `baidu engine returns failure with unsupported message`() = kotlinx.coroutines.test.runTest {
        val engine = factory.getEngine("baidu")
        val result = engine.recognize(java.io.File("test.wav"))
        assert(result.isFailure)
        assert(result.exceptionOrNull() is UnsupportedOperationException)
        assert(result.exceptionOrNull()?.message == "请先配置百度语音 SDK")
    }

    @Test
    fun `whisper engine returns failure with unsupported message`() = kotlinx.coroutines.test.runTest {
        val engine = factory.getEngine("whisper")
        val result = engine.recognize(java.io.File("test.wav"))
        assert(result.isFailure)
        assert(result.exceptionOrNull() is UnsupportedOperationException)
        assert(result.exceptionOrNull()?.message == "请先配置 Whisper 模型")
    }
}
```

- [ ] Commit: `feat: add ASR engine interface with stub implementations`

---

### Task 16: LLM Client (OpenAI-compatible)

- [ ] Create `llm/LlmClient.kt` interface

**File: `app/src/main/java/com/qb/secondbrain/llm/LlmClient.kt`**

```kotlin
package com.qb.secondbrain.llm

import com.qb.secondbrain.data.model.LlmIntent
import java.io.File

interface LlmClient {
    suspend fun parseIntent(text: String, context: String?): Result<LlmIntent>
}
```

- [ ] Create `llm/OpenAiApi.kt` Retrofit interface

**File: `app/src/main/java/com/qb/secondbrain/llm/OpenAiApi.kt`**

```kotlin
package com.qb.secondbrain.llm

import retrofit2.http.Body
import retrofit2.http.POST

interface OpenAiApi {

    @POST("v1/chat/completions")
    suspend fun chatCompletions(@Body request: ChatCompletionRequest): ChatCompletionResponse
}

data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.3
)

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatCompletionResponse(
    val id: String?,
    val choices: List<ChatChoice>?,
    val usage: ChatUsage?
)

data class ChatChoice(
    val index: Int?,
    val message: ChatMessage?,
    val finish_reason: String?
)

data class ChatUsage(
    val prompt_tokens: Int?,
    val completion_tokens: Int?,
    val total_tokens: Int?
)
```

- [ ] Create `llm/OpenAiLlmClient.kt` implementation

**File: `app/src/main/java/com/qb/secondbrain/llm/OpenAiLlmClient.kt`**

```kotlin
package com.qb.secondbrain.llm

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.qb.secondbrain.data.model.LlmIntent
import com.qb.secondbrain.data.model.ContextNeed
import javax.inject.Inject

class OpenAiLlmClient @Inject constructor(
    private val openAiApi: OpenAiApi,
    private val gson: Gson,
    private val modelName: String
) : LlmClient {

    companion object {
        private const val TAG = "OpenAiLlmClient"
        private const val SYSTEM_PROMPT = """你是一个备忘录助手。分析用户的语音输入，返回结构化JSON。

必须返回以下格式的JSON（不要包含其他文字）：
{
  "intent": "add | query | update | delete",
  "needContext": {
    "screenshot": true/false,
    "location": true/false
  },
  "content": "整理后的备忘录内容",
  "tags": ["标签1", "标签2"],
  "reminderTime": "ISO8601时间字符串或null",
  "queryKeywords": ["关键词1", "关键词2"]
}

规则：
- intent: 判断用户是要添加(add)、查询(query)、修改(update)、还是删除(delete)备忘录
- needContext.screenshot: 如果用户说"记住这个"、"保存这个"等指代当前屏幕的内容，设为true
- needContext.location: 如果用户提到地点相关，设为true
- content: 对原始语音进行整理润色后的备忘录内容，query/update/delete时可为空
- tags: 自动从内容中提取关键词标签
- reminderTime: 如果用户提到提醒时间，转换为ISO8601格式
- queryKeywords: 用于搜索的关键词列表"""
    }

    override suspend fun parseIntent(text: String, context: String?): Result<LlmIntent> {
        return try {
            val userMessage = buildUserMessage(text, context)

            val request = ChatCompletionRequest(
                model = modelName,
                messages = listOf(
                    ChatMessage(role = "system", content = SYSTEM_PROMPT),
                    ChatMessage(role = "user", content = userMessage)
                )
            )

            val response = openAiApi.chatCompletions(request)
            val content = response.choices?.firstOrNull()?.message?.content
                ?: return Result.failure(IllegalStateException("Empty response from LLM"))

            parseLlmIntent(content)
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Failed to parse LLM response as JSON", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "LLM call failed", e)
            Result.failure(e)
        }
    }

    private fun buildUserMessage(text: String, context: String?): String {
        val sb = StringBuilder()
        sb.append("用户语音输入：「$text」")

        if (!context.isNullOrBlank()) {
            sb.append("\
\
上下文信息：$context")
        }

        return sb.toString()
    }

    private fun parseLlmIntent(content: String): Result<LlmIntent> {
        return try {
            val jsonContent = extractJson(content)
            val intent = gson.fromJson(jsonContent, LlmIntent::class.java)
            Result.success(intent)
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Invalid JSON from LLM: $content", e)
            Result.failure(e)
        }
    }

    private fun extractJson(content: String): String {
        val trimmed = content.trim()
        if (trimmed.startsWith("{")) {
            return trimmed
        }
        val jsonStart = trimmed.indexOf("{")
        val jsonEnd = trimmed.lastIndexOf("}")
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            return trimmed.substring(jsonStart, jsonEnd + 1)
        }
        return trimmed
    }
}
```

- [ ] Create `di/NetworkModule.kt` Hilt module

**File: `app/src/main/java/com/qb/secondbrain/di/NetworkModule.kt`**

```kotlin
package com.qb.secondbrain.di

import android.content.Context
import android.util.Log
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.qb.secondbrain.llm.OpenAiApi
import com.qb.secondbrain.llm.OpenAiLlmClient
import com.qb.secondbrain.llm.LlmClient
import com.qb.secondbrain.data.local.SettingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setLenient()
            .create()
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            Log.d("OkHttp", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(settingsManager: SettingsManager): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val apiKey = runCatching {
                settingsManager.getApiKeySync()
            }.getOrNull() ?: ""

            val request = if (apiKey.isNotBlank()) {
                originalRequest.newBuilder()
                    .header("Authorization", "Bearer $apiKey")
                    .header("Content-Type", "application/json")
                    .build()
            } else {
                originalRequest.newBuilder()
                    .header("Content-Type", "application/json")
                    .build()
            }

            chain.proceed(request)
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: Interceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson,
        settingsManager: SettingsManager
    ): Retrofit {
        val baseUrl = runCatching {
            settingsManager.getBaseUrlSync()
        }.getOrNull() ?: "https://api.openai.com/"

        val effectiveBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        return Retrofit.Builder()
            .baseUrl(effectiveBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenAiApi(retrofit: Retrofit): OpenAiApi {
        return retrofit.create(OpenAiApi::class.java)
    }

    @Provides
    @Singleton
    @Named("modelName")
    fun provideModelName(settingsManager: SettingsManager): String {
        return runCatching {
            settingsManager.getModelNameSync()
        }.getOrNull() ?: "gpt-3.5-turbo"
    }

    @Provides
    @Singleton
    fun provideLlmClient(
        openAiApi: OpenAiApi,
        gson: Gson,
        @Named("modelName") modelName: String
    ): LlmClient {
        return OpenAiLlmClient(openAiApi, gson, modelName)
    }
}
```

- [ ] Create `data/local/SettingsDataStore.kt` (required by NetworkModule)

**File: `app/src/main/java/com/qb/secondbrain/data/local/SettingsDataStore.kt`**

```kotlin
package com.qb.secondbrain.settings

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext context: Context
) {

    companion object {
        private const val PREFS_NAME = "secondbrain_settings"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_MODEL_NAME = "model_name"
        private const val KEY_ASR_ENGINE = "asr_engine"
        private const val KEY_MAX_RECORDING_DURATION = "max_recording_duration"

        const val DEFAULT_BASE_URL = "https://api.openai.com/"
        const val DEFAULT_MODEL_NAME = "gpt-3.5-turbo"
        const val DEFAULT_ASR_ENGINE = "whisper"
        const val DEFAULT_MAX_RECORDING_DURATION = 60_000L
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getApiKeySync(): String = prefs.getString(KEY_API_KEY, "") ?: ""

    suspend fun getApiKey(): String = getApiKeySync()

    suspend fun saveApiKey(apiKey: String) {
        prefs.edit().putString(KEY_API_KEY, apiKey).apply()
    }

    fun getBaseUrlSync(): String = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL

    suspend fun getBaseUrl(): String = getBaseUrlSync()

    suspend fun saveBaseUrl(url: String) {
        val normalized = if (url.endsWith("/")) url else "$url/"
        prefs.edit().putString(KEY_BASE_URL, normalized).apply()
    }

    fun getModelNameSync(): String = prefs.getString(KEY_MODEL_NAME, DEFAULT_MODEL_NAME) ?: DEFAULT_MODEL_NAME

    suspend fun getModelName(): String = getModelNameSync()

    suspend fun saveModelName(model: String) {
        prefs.edit().putString(KEY_MODEL_NAME, model).apply()
    }

    fun getAsrEngineSync(): String = prefs.getString(KEY_ASR_ENGINE, DEFAULT_ASR_ENGINE) ?: DEFAULT_ASR_ENGINE

    suspend fun getAsrEngine(): String = getAsrEngineSync()

    suspend fun saveAsrEngine(engine: String) {
        prefs.edit().putString(KEY_ASR_ENGINE, engine).apply()
    }

    fun getMaxRecordingDurationSync(): Long =
        prefs.getLong(KEY_MAX_RECORDING_DURATION, DEFAULT_MAX_RECORDING_DURATION)

    suspend fun getMaxRecordingDuration(): Long = getMaxRecordingDurationSync()

    suspend fun saveMaxRecordingDuration(durationMs: Long) {
        prefs.edit().putLong(KEY_MAX_RECORDING_DURATION, durationMs).apply()
    }
}
```

- [ ] Commit: `feat: add LLM client with OpenAI-compatible API`

---

### Task 17: Rule-Based LLM Fallback

- [ ] Create `llm/RuleBasedFallback.kt`

**File: `app/src/main/java/com/qb/secondbrain/llm/RuleBasedFallback.kt`**

```kotlin
package com.qb.secondbrain.llm

import com.qb.secondbrain.data.model.LlmIntent
import com.qb.secondbrain.data.model.ContextNeed
import javax.inject.Inject

class RuleBasedFallback @Inject constructor() {

    companion object {
        private val DELETE_PATTERNS = listOf(
            Regex("删除.*最后.*条"),
            Regex("删掉.*最后.*条"),
            Regex("去掉.*最后.*条"),
            Regex("移除.*最后.*条"),
            Regex("删.*上.*条"),
            Regex("删除最近的"),
            Regex("删除刚才")
        )

        private val QUERY_TODAY_PATTERNS = listOf(
            Regex("查看?今天的备忘录"),
            Regex("今天记了什么"),
            Regex("今天.*备忘"),
            Regex("今天.*记.*什么"),
            Regex("查看?今天"),
            Regex("今天的?记录"),
            Regex("今天存了什么")
        )

        private val QUERY_RECENT_PATTERNS = listOf(
            Regex("最近.*备忘"),
            Regex("最近.*记录"),
            Regex("最近.*条"),
            Regex("刚才.*记"),
            Regex("查看最近的"),
            Regex("查看最后")
        )

        private val QUERY_KEYWORD_PATTERNS = listOf(
            Regex("查看?(.+)的?备忘录"),
            Regex("搜索(.+)"),
            Regex("查找(.+)"),
            Regex("找一下(.+)"),
            Regex("有没有关于(.+)")
        )

        private val UPDATE_PATTERNS = listOf(
            Regex("修改.+为(.+)"),
            Regex("把.+改[成是](.+)"),
            Regex("更新.+为(.+)")
        )

        private val REMINDER_PATTERNS = listOf(
            Regex("提醒我.+?(\\d+)[点时](\\d+)?分?"),
            Regex("提醒我.+?(\\d+)点"),
            Regex("提醒我.+?(明天|后天|今天)"),
            Regex("提醒我.+?(\\d+)月(\\d+)[号日]"),
            Regex("(.+)提醒我(.+)")
        )

        private val SCREENSHOT_PATTERNS = listOf(
            Regex("记住这个"),
            Regex("保存这个"),
            Regex("记录这个"),
            Regex("截.*屏"),
            Regex("这个.*保存"),
            Regex("记住当前"),
            Regex("保存当前")
        )

        private val LOCATION_PATTERNS = listOf(
            Regex("这里"),
            Regex("这个位置"),
            Regex("当前位置"),
            Regex("在这"),
            Regex("到这里")
        )

        private val TIME_KEYWORDS = mapOf(
            "今天" to 0,
            "明天" to 1,
            "后天" to 2,
            "大后天" to 3
        )
    }

    fun parseIntent(text: String): LlmIntent {
        val normalizedText = text.trim()

        return when {
            matchesDelete(normalizedText) -> parseDeleteIntent(normalizedText)
            matchesQueryToday(normalizedText) -> parseQueryTodayIntent()
            matchesQueryRecent(normalizedText) -> parseQueryRecentIntent()
            matchesQueryByKeyword(normalizedText) -> parseQueryKeywordIntent(normalizedText)
            matchesUpdate(normalizedText) -> parseUpdateIntent(normalizedText)
            matchesReminder(normalizedText) -> parseReminderIntent(normalizedText)
            else -> parseDefaultIntent(normalizedText)
        }
    }

    private fun matchesDelete(text: String): Boolean {
        return DELETE_PATTERNS.any { it.containsMatchIn(text) }
    }

    private fun matchesQueryToday(text: String): Boolean {
        return QUERY_TODAY_PATTERNS.any { it.containsMatchIn(text) }
    }

    private fun matchesQueryRecent(text: String): Boolean {
        return QUERY_RECENT_PATTERNS.any { it.containsMatchIn(text) }
    }

    private fun matchesQueryByKeyword(text: String): Boolean {
        return QUERY_KEYWORD_PATTERNS.any { it.containsMatchIn(text) }
    }

    private fun matchesUpdate(text: String): Boolean {
        return UPDATE_PATTERNS.any { it.containsMatchIn(text) }
    }

    private fun matchesReminder(text: String): Boolean {
        return REMINDER_PATTERNS.any { it.containsMatchIn(text) }
    }

    private fun parseDeleteIntent(text: String): LlmIntent {
        val keywords = mutableListOf("最近")
        if (text.contains("最后一条") || text.contains("最后那条")) {
            keywords.clear()
            keywords.add("最近")
        }
        return LlmIntent(
            intent = "delete",
            needContext = ContextNeed(screenshot = false, location = false),
            content = text,
            tags = emptyList(),
            reminderTime = null,
            queryKeywords = keywords
        )
    }

    private fun parseQueryTodayIntent(): LlmIntent {
        return LlmIntent(
            intent = "query",
            needContext = ContextNeed(screenshot = false, location = false),
            content = "",
            tags = emptyList(),
            reminderTime = null,
            queryKeywords = listOf("今天")
        )
    }

    private fun parseQueryRecentIntent(): LlmIntent {
        return LlmIntent(
            intent = "query",
            needContext = ContextNeed(screenshot = false, location = false),
            content = "",
            tags = emptyList(),
            reminderTime = null,
            queryKeywords = listOf("最近")
        )
    }

    private fun parseQueryKeywordIntent(text: String): LlmIntent {
        var keywords = emptyList<String>()
        for (pattern in QUERY_KEYWORD_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                val keyword = match.groupValues[1].trim()
                if (keyword.isNotBlank()) {
                    keywords = keyword.split("[\\s,、]+".toRegex()).filter { it.isNotBlank() }
                }
                break
            }
        }
        return LlmIntent(
            intent = "query",
            needContext = ContextNeed(screenshot = false, location = false),
            content = "",
            tags = emptyList(),
            reminderTime = null,
            queryKeywords = keywords
        )
    }

    private fun parseUpdateIntent(text: String): LlmIntent {
        var newContent = text
        for (pattern in UPDATE_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                newContent = match.groupValues[1].trim()
                break
            }
        }
        return LlmIntent(
            intent = "update",
            needContext = ContextNeed(screenshot = false, location = false),
            content = newContent,
            tags = extractTags(newContent),
            reminderTime = null,
            queryKeywords = emptyList()
        )
    }

    private fun parseReminderIntent(text: String): LlmIntent {
        val needScreenshot = SCREENSHOT_PATTERNS.any { it.containsMatchIn(text) }
        val needLocation = LOCATION_PATTERNS.any { it.containsMatchIn(text) }

        var reminderTime: String? = null
        for (pattern in REMINDER_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                reminderTime = extractReminderTime(text, match.groupValues)
                break
            }
        }

        val contentText = text
            .replace(Regex("^(明天|后天|今天|大后天)?\\s*提醒我\\s*"), "")
            .replace(Regex("^提醒我\\s*"), "")
            .trim()

        return LlmIntent(
            intent = "add",
            needContext = ContextNeed(screenshot = needScreenshot, location = needLocation),
            content = if (contentText.isNotBlank()) contentText else text,
            tags = extractTags(contentText),
            reminderTime = reminderTime,
            queryKeywords = emptyList()
        )
    }

    private fun parseDefaultIntent(text: String): LlmIntent {
        val needScreenshot = SCREENSHOT_PATTERNS.any { it.containsMatchIn(text) }
        val needLocation = LOCATION_PATTERNS.any { it.containsMatchIn(text) }

        return LlmIntent(
            intent = "add",
            needContext = ContextNeed(screenshot = needScreenshot, location = needLocation),
            content = text,
            tags = extractTags(text),
            reminderTime = null,
            queryKeywords = emptyList()
        )
    }

    private fun extractReminderTime(text: String, groups: List<String>): String? {
        val calendar = java.util.Calendar.getInstance()

        for ((keyword, daysOffset) in TIME_KEYWORDS) {
            if (text.contains(keyword)) {
                calendar.add(java.util.Calendar.DAY_OF_MONTH, daysOffset)
                break
            }
        }

        val hourPattern = Regex("(\\d+)[点时](\\d+)?分?")
        val hourMatch = hourPattern.find(text)
        if (hourMatch != null) {
            val hour = hourMatch.groupValues[1].toIntOrNull() ?: return null
            val minute = hourMatch.groupValues[2]?.toIntOrNull() ?: 0
            calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
            calendar.set(java.util.Calendar.MINUTE, minute)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)

            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.CHINA)
            return sdf.format(calendar.time)
        }

        return null
    }

    private fun extractTags(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val stopWords = setOf(
            "的", "了", "是", "在", "我", "有", "和", "就", "不", "人", "都", "一", "一个",
            "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好",
            "自己", "这", "他", "她", "它", "们", "那", "把", "被", "让", "给", "对", "吗",
            "呢", "吧", "啊", "呀", "嗯", "哦", "哈", "么", "什么", "怎么", "为什么",
            "这个", "那个", "记住", "保存", "记录", "添加", "备忘", "备忘录", "提醒"
        )

        val nouns = text.split("[\\s,。！？、；：""''（）\\[\\]{}]+".toRegex())
            .filter { it.isNotBlank() && it.length >= 2 && it !in stopWords }

        return nouns.distinct().take(5)
    }
}
```

- [ ] Write comprehensive unit tests for RuleBasedFallback

**File: `app/src/test/java/com/qb/secondbrain/llm/RuleBasedFallbackTest.kt`**

```kotlin
package com.qb.secondbrain.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RuleBasedFallbackTest {

    private lateinit var fallback: RuleBasedFallback

    @Before
    fun setUp() {
        fallback = RuleBasedFallback()
    }

    @Test
    fun `delete last memo pattern - 删除最后一条`() {
        val result = fallback.parseIntent("删除最后一条")
        assertEquals("delete", result.intent)
        assertTrue(result.queryKeywords.contains("最近"))
    }

    @Test
    fun `delete last memo pattern - 删掉最后一条备忘录`() {
        val result = fallback.parseIntent("删掉最后一条备忘录")
        assertEquals("delete", result.intent)
    }

    @Test
    fun `delete last memo pattern - 删除刚才的`() {
        val result = fallback.parseIntent("删除刚才的")
        assertEquals("delete", result.intent)
    }

    @Test
    fun `delete last memo pattern - 删除最近的备忘录`() {
        val result = fallback.parseIntent("删除最近的备忘录")
        assertEquals("delete", result.intent)
    }

    @Test
    fun `query today pattern - 查看今天的备忘录`() {
        val result = fallback.parseIntent("查看今天的备忘录")
        assertEquals("query", result.intent)
        assertTrue(result.queryKeywords.contains("今天"))
    }

    @Test
    fun `query today pattern - 今天记了什么`() {
        val result = fallback.parseIntent("今天记了什么")
        assertEquals("query", result.intent)
        assertTrue(result.queryKeywords.contains("今天"))
    }

    @Test
    fun `query today pattern - 今天的记录`() {
        val result = fallback.parseIntent("今天的记录")
        assertEquals("query", result.intent)
    }

    @Test
    fun `query recent pattern - 查看最近的备忘录`() {
        val result = fallback.parseIntent("查看最近的备忘录")
        assertEquals("query", result.intent)
        assertTrue(result.queryKeywords.contains("最近"))
    }

    @Test
    fun `query by keyword pattern - 查看关于停车的备忘录`() {
        val result = fallback.parseIntent("查看关于停车的备忘录")
        assertEquals("query", result.intent)
        assertTrue(result.queryKeywords.any { it.contains("停车") })
    }

    @Test
    fun `query by keyword pattern - 搜索二维码`() {
        val result = fallback.parseIntent("搜索二维码")
        assertEquals("query", result.intent)
        assertTrue(result.queryKeywords.any { it.contains("二维码") })
    }

    @Test
    fun `query by keyword pattern - 查找会议纪要`() {
        val result = fallback.parseIntent("查找会议纪要")
        assertEquals("query", result.intent)
    }

    @Test
    fun `update pattern - 修改为XXX`() {
        val result = fallback.parseIntent("修改最后一条为明天三点开会")
        assertEquals("update", result.intent)
    }

    @Test
    fun `update pattern - 把XXX改成YYY`() {
        val result = fallback.parseIntent("把停车信息改成地下停车场B2")
        assertEquals("update", result.intent)
    }

    @Test
    fun `reminder pattern - 提醒我明天3点开会`() {
        val result = fallback.parseIntent("提醒我明天3点开会")
        assertEquals("add", result.intent)
        assertNotNull(result.reminderTime)
        assertTrue(result.content.contains("开会"))
    }

    @Test
    fun `reminder pattern - 提醒我下午5点30分去接孩子`() {
        val result = fallback.parseIntent("提醒我下午5点30分去接孩子")
        assertEquals("add", result.intent)
        assertNotNull(result.reminderTime)
    }

    @Test
    fun `reminder pattern - 提醒我今天14点开会`() {
        val result = fallback.parseIntent("提醒我今天14点开会")
        assertEquals("add", result.intent)
        assertNotNull(result.reminderTime)
    }

    @Test
    fun `screenshot context - 记住这个`() {
        val result = fallback.parseIntent("记住这个")
        assertEquals("add", result.intent)
        assertTrue(result.needContext.screenshot)
    }

    @Test
    fun `screenshot context - 保存这个二维码`() {
        val result = fallback.parseIntent("保存这个二维码")
        assertEquals("add", result.intent)
        assertTrue(result.needContext.screenshot)
    }

    @Test
    fun `location context - 记住这里的位置`() {
        val result = fallback.parseIntent("记住这里的位置")
        assertEquals("add", result.intent)
        assertTrue(result.needContext.location)
    }

    @Test
    fun `default add intent - plain text content`() {
        val result = fallback.parseIntent("海天公园的停车场二维码")
        assertEquals("add", result.intent)
        assertFalse(result.needContext.screenshot)
        assertFalse(result.needContext.location)
        assertEquals("海天公园的停车场二维码", result.content)
    }

    @Test
    fun `default add intent - with screenshot context`() {
        val result = fallback.parseIntent("记住当前屏幕的二维码")
        assertEquals("add", result.intent)
        assertTrue(result.needContext.screenshot)
    }

    @Test
    fun `delete pattern - 去掉最后一条`() {
        val result = fallback.parseIntent("去掉最后一条")
        assertEquals("delete", result.intent)
    }

    @Test
    fun `delete pattern - 移除最后一条记录`() {
        val result = fallback.parseIntent("移除最后一条记录")
        assertEquals("delete", result.intent)
    }

    @Test
    fun `query today - 今天存了什么`() {
        val result = fallback.parseIntent("今天存了什么")
        assertEquals("query", result.intent)
    }

    @Test
    fun `complex reminder - 后天提醒我开会`() {
        val result = fallback.parseIntent("后天提醒我开会")
        assertEquals("add", result.intent)
    }

    @Test
    fun `default intent preserves original text`() {
        val originalText = "这是一段普通的备忘录内容没有特殊关键词"
        val result = fallback.parseIntent(originalText)
        assertEquals("add", result.intent)
        assertEquals(originalText, result.content)
    }

    @Test
    fun `query by keyword - 找一下密码`() {
        val result = fallback.parseIntent("找一下密码")
        assertEquals("query", result.intent)
    }

    @Test
    fun `query by keyword - 有没有关于发票的备忘`() {
        val result = fallback.parseIntent("有没有关于发票的备忘")
        assertEquals("query", result.intent)
    }

    @Test
    fun `reminder without specific time`() {
        val result = fallback.parseIntent("提醒我买牛奶")
        assertEquals("add", result.intent)
    }

    @Test
    fun `delete pattern - 删上条`() {
        val result = fallback.parseIntent("删上条")
        assertEquals("delete", result.intent)
    }

    @Test
    fun `tags are extracted from content`() {
        val result = fallback.parseIntent("海天公园停车场B2层车位号123")
        assertEquals("add", result.intent)
        assertTrue(result.tags.isNotEmpty())
    }
}
```

- [ ] Commit: `feat: add rule-based LLM fallback with offline intent parsing`

---

### Task 18: Context Providers (Location + ScreenCapture)

- [ ] Create `context/LocationProvider.kt`

**File: `app/src/main/java/com/qb/secondbrain/context/LocationProvider.kt`**

```kotlin
package com.qb.secondbrain.context

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "LocationProvider"
        private const val LOCATION_TIMEOUT_MS = 10000L
        private const val MIN_DISTANCE_M = 0f
        private const val MIN_TIME_MS = 0L
    }

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        return try {
            if (!hasLocationCapability()) {
                Log.w(TAG, "No location provider available")
                return null
            }

            val lastKnown = getLastKnownLocation()
            if (lastKnown != null && isLocationRecent(lastKnown)) {
                Log.d(TAG, "Using last known location: ${lastKnown.latitude}, ${lastKnown.longitude}")
                return lastKnown
            }

            requestFreshLocation()
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission not granted", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get location", e)
            null
        }
    }

    suspend fun getAddress(lat: Double, lng: Double): String? {
        return try {
            if (!Geocoder.isPresent()) {
                Log.w(TAG, "Geocoder not available on this device")
                return null
            }

            val geocoder = Geocoder(context, Locale.CHINA)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getAddressFromGeocoderAsync(geocoder, lat, lng)
            } else {
                @Suppress("DEPRECATION")
                getAddressFromGeocoderSync(geocoder, lat, lng)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get address for $lat, $lng", e)
            null
        }
    }

    private fun hasLocationCapability(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(): Location? {
        val gpsLocation = try {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } catch (_: Exception) {
            null
        }

        val networkLocation = try {
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            null
        }

        return when {
            gpsLocation != null && networkLocation != null -> {
                if (gpsLocation.accuracy < networkLocation.accuracy) gpsLocation else networkLocation
            }
            gpsLocation != null -> gpsLocation
            networkLocation != null -> networkLocation
            else -> null
        }
    }

    private fun isLocationRecent(location: Location): Boolean {
        val ageMs = System.currentTimeMillis() - location.time
        return ageMs < LOCATION_TIMEOUT_MS
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFreshLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            val provider = when {
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> {
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }
            }

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationManager.removeUpdates(this)
                    if (continuation.isActive) {
                        continuation.resume(location)
                    }
                }

                @Deprecated("Deprecated in API 29")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

                override fun onProviderEnabled(provider: String) {}

                override fun onProviderDisabled(provider: String) {
                    locationManager.removeUpdates(this)
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }

            try {
                locationManager.requestLocationUpdates(
                    provider,
                    MIN_TIME_MS,
                    MIN_DISTANCE_M,
                    listener
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request location updates", e)
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }

            continuation.invokeOnCancellation {
                try {
                    locationManager.removeUpdates(listener)
                } catch (_: Exception) {
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun getAddressFromGeocoderSync(geocoder: Geocoder, lat: Double, lng: Double): String? {
        val addresses: List<Address>? = geocoder.getFromLocation(lat, lng, 1)
        return formatAddress(addresses?.firstOrNull())
    }

    private suspend fun getAddressFromGeocoderAsync(geocoder: Geocoder, lat: Double, lng: Double): String? {
        return suspendCancellableCoroutine { continuation ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (continuation.isActive) {
                            continuation.resume(formatAddress(addresses.firstOrNull()))
                        }
                    }

                    override fun onError(errorMessage: String?) {
                        Log.e(TAG, "Geocoder error: $errorMessage")
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                })
            } else {
                continuation.resume(null)
            }
        }
    }

    private fun formatAddress(address: Address?): String? {
        if (address == null) return null

        val parts = mutableListOf<String>()
        address.adminArea?.let { parts.add(it) }
        address.locality?.let { parts.add(it) }
        address.subLocality?.let { parts.add(it) }
        address.thoroughfare?.let { parts.add(it) }
        address.subThoroughfare?.let { parts.add(it) }

        val formatted = parts.filter { it.isNotBlank() }.joinToString("")
        return formatted.ifBlank {
            address.getAddressLine(0)
        }
    }
}
```

- [ ] Create `context/ScreenCaptureProvider.kt`

**File: `app/src/main/java/com/qb/secondbrain/context/ScreenCaptureProvider.kt`**

```kotlin
package com.qb.secondbrain.context

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScreenCaptureProvider @Inject constructor(
    private val cacheDir: File
) {

    companion object {
        private const val TAG = "ScreenCaptureProvider"
        private const val JPEG_QUALITY = 80
        private const val SCREENSHOT_DIR = "screenshots"
    }

    private var screenshotCallback: (() -> AccessibilityNodeInfo?)? = null

    fun setRootInActiveWindowProvider(callback: () -> AccessibilityNodeInfo?) {
        screenshotCallback = callback
    }

    suspend fun captureScreen(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val rootNode = screenshotCallback?.invoke()
                if (rootNode == null) {
                    Log.w(TAG, "Root node is null, accessibility service may not be active")
                    return@withContext null
                }

                val bounds = Rect()
                rootNode.getBoundsInScreen(bounds)

                if (bounds.isEmpty) {
                    Log.w(TAG, "Root node bounds are empty")
                    rootNode.recycle()
                    return@withContext null
                }

                val bitmap = captureNodeToBitmap(rootNode, bounds)
                rootNode.recycle()

                if (bitmap == null) {
                    Log.w(TAG, "Failed to capture bitmap from root node")
                    return@withContext null
                }

                val filePath = saveBitmapToFile(bitmap)
                bitmap.recycle()
                filePath
            } catch (e: Exception) {
                Log.e(TAG, "Failed to capture screen", e)
                null
            }
        }
    }

    private fun captureNodeToBitmap(node: AccessibilityNodeInfo, bounds: Rect): Bitmap? {
        return try {
            val width = bounds.width()
            val height = bounds.height()

            if (width <= 0 || height <= 0) {
                Log.w(TAG, "Invalid bounds: $bounds")
                return null
            }

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val path = android.graphics.Path()
            path.addRect(
                0f,
                0f,
                width.toFloat(),
                height.toFloat(),
                android.graphics.Path.Direction.CW
            )

            node.draw(canvas)

            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to draw node to bitmap", e)
            null
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap): String? {
        return try {
            val screenshotDir = File(cacheDir, SCREENSHOT_DIR)
            if (!screenshotDir.exists()) {
                screenshotDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA)
                .format(Date())
            val file = File(screenshotDir, "screenshot_$timestamp.jpg")

            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos)
                fos.flush()
            }

            Log.d(TAG, "Screenshot saved: ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save screenshot", e)
            null
        }
    }

    fun cleanupOldScreenshots(maxAgeMs: Long = 24 * 60 * 60 * 1000L) {
        val screenshotDir = File(cacheDir, SCREENSHOT_DIR)
        if (!screenshotDir.exists()) return

        val cutoffTime = System.currentTimeMillis() - maxAgeMs
        screenshotDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoffTime) {
                file.delete()
            }
        }
    }
}
```

- [ ] Commit: `feat: add LocationProvider and ScreenCaptureProvider`

---

## Phase 4: Integration

### Task 19: Voice Processing Pipeline (ForegroundService)

- [ ] Create `service/VoiceMemoState.kt` sealed class for service states

**File:** `app/src/main/java/com/qb/secondbrain/service/VoiceMemoState.kt`

```kotlin
package com.qb.secondbrain.service

sealed class VoiceMemoState {
    data object Idle : VoiceMemoState()
    data object Recording : VoiceMemoState()
    data class Processing(val step: String = "") : VoiceMemoState()
    data class Notifying(val message: String) : VoiceMemoState()
    data class Error(val message: String) : VoiceMemoState()
}
```

- [ ] Create `service/VoiceMemoService.kt` - the core ForegroundService with full processing pipeline

**File:** `app/src/main/java/com/qb/secondbrain/service/VoiceMemoService.kt`

```kotlin
package com.qb.secondbrain.service

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.location.Location
import android.os.Build
import android.os.IBinder
import com.qb.secondbrain.MainActivity
import com.qb.secondbrain.data.model.Memo
import com.qb.secondbrain.data.repository.MemoRepository
import com.qb.secondbrain.llm.LlmClient
import com.qb.secondbrain.data.model.LlmIntent
import com.qb.secondbrain.llm.RuleBasedFallback
import com.qb.secondbrain.asr.AsrEngine
import com.qb.secondbrain.service.AudioRecorder
import com.qb.secondbrain.context.LocationProvider
import com.qb.secondbrain.context.ScreenCaptureProvider
import com.qb.secondbrain.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import java.io.File
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class VoiceMemoService : Service() {

    companion object {
        const val ACTION_START_RECORDING = "com.qb.secondbrain.action.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.qb.secondbrain.action.STOP_RECORDING"
        const val EXTRA_ACTION = "extra_action"

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_RECORDING = "channel_recording"
        private const val CHANNEL_RESULT = "channel_result"
        private const val CHANNEL_ERROR = "channel_error"
        private const val LLM_TIMEOUT_MS = 10_000L

        fun startRecording(context: Context) {
            val intent = Intent(context, VoiceMemoService::class.java).apply {
                action = ACTION_START_RECORDING
            }
            context.startForegroundService(intent)
        }

        fun stopRecording(context: Context) {
            val intent = Intent(context, VoiceMemoService::class.java).apply {
                action = ACTION_STOP_RECORDING
            }
            context.startForegroundService(intent)
        }
    }

    @Inject lateinit var audioRecorder: AudioRecorder
    @Inject lateinit var asrEngine: AsrEngine
    @Inject lateinit var llmClient: LlmClient
    @Inject lateinit var ruleBasedFallback: RuleBasedFallback
    @Inject lateinit var memoRepository: MemoRepository
    @Inject lateinit var locationProvider: LocationProvider
    @Inject lateinit var screenCaptureProvider: ScreenCaptureProvider
    @Inject lateinit var notificationHelper: NotificationHelper

    private val _state = MutableStateFlow<VoiceMemoState>(VoiceMemoState.Idle)
    val state: StateFlow<VoiceMemoState> = _state

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var audioFile: File? = null
    private var isRecording = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent.action) {
            ACTION_START_RECORDING -> handleStartRecording()
            ACTION_STOP_RECORDING -> handleStopRecording()
            else -> stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun handleStartRecording() {
        if (isRecording) return

        val notification = notificationHelper.recordingNotification("正在录音...")
        startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        )

        _state.value = VoiceMemoState.Recording
        isRecording = true

        val outputFile = File(cacheDir, "voice_${UUID.randomUUID()}.wav")
        audioFile = outputFile
        audioRecorder.startRecording(outputFile)
    }

    private fun handleStopRecording() {
        if (!isRecording) return
        isRecording = false
        audioRecorder.stopRecording()
        val file = audioFile
        audioFile = null

        if (file != null && file.exists()) {
            processAudio(file)
        } else {
            _state.value = VoiceMemoState.Error("录音文件不存在")
            notifyError("录音文件不存在")
            transitionToIdle()
        }
    }

    private fun processAudio(audioFile: File) {
        serviceScope.launch {
            try {
                _state.value = VoiceMemoState.Processing("语音识别中...")

                // Step 1: ASR
                val asrResult = withContext(Dispatchers.IO) {
                    asrEngine.recognize(audioFile)
                }

                if (asrResult.isFailure) {
                    _state.value = VoiceMemoState.Error("语音识别失败: ${asrResult.exceptionOrNull()?.message}")
                    notifyError("语音识别失败，请重试")
                    cleanupAudioFile(audioFile)
                    transitionToIdle()
                    return@launch
                }

                val rawText = asrResult.getOrDefault("")
                if (rawText.isBlank()) {
                    _state.value = VoiceMemoState.Error("未检测到语音内容")
                    notifyError("未检测到语音内容，请重试")
                    cleanupAudioFile(audioFile)
                    transitionToIdle()
                    return@launch
                }

                // Step 2: LLM intent parsing with timeout
                _state.value = VoiceMemoState.Processing("AI 理解中...")

                var llmIntent: LlmIntent? = withTimeoutOrNull(LLM_TIMEOUT_MS) {
                    val result = llmClient.parseIntent(rawText, null)
                    result.getOrNull()
                }

                // Step 3: Fallback to rule-based if LLM fails
                if (llmIntent == null) {
                    llmIntent = ruleBasedFallback.parseIntent(rawText)
                }

                // Step 4: Check if context is needed
                var screenshotBitmap: Bitmap? = null
                var locationData: Pair<Location, String>? = null
                var needReCallLlm = false

                if (llmIntent.needContext.screenshot) {
                    _state.value = VoiceMemoState.Processing("获取截图中...")
                    screenshotBitmap = withContext(Dispatchers.Main) {
                        VolumeKeyAccessibilityService.getLastScreenshot()
                    } ?: screenCaptureProvider.captureScreen()
                    if (screenshotBitmap != null) {
                        needReCallLlm = true
                    }
                }

                if (llmIntent.needContext.location) {
                    _state.value = VoiceMemoState.Processing("获取定位中...")
                    val location = locationProvider.getCurrentLocation()
                    if (location != null) {
                        val address = locationProvider.getAddress(location.latitude, location.longitude)
                        locationData = Pair(location, address ?: "")
                        needReCallLlm = true
                    }
                }

                // Step 5: Re-call LLM with context if needed
                if (needReCallLlm) {
                    _state.value = VoiceMemoState.Processing("AI 重新分析中...")
                    val contextBuilder = StringBuilder()
                    if (locationData != null) {
                        contextBuilder.append("位置: ${locationData.second} (${locationData.first.latitude}, ${locationData.first.longitude})\n")
                    }
                    val contextStr = contextBuilder.toString().ifBlank { null }

                    val refinedIntent = withTimeoutOrNull(LLM_TIMEOUT_MS) {
                        llmClient.parseIntent(rawText, contextStr).getOrNull()
                    }
                    if (refinedIntent != null) {
                        llmIntent = refinedIntent
                    }
                }

                // Step 6: Execute intent
                _state.value = VoiceMemoState.Processing("执行操作中...")
                executeIntent(llmIntent, rawText, screenshotBitmap, locationData)

                cleanupAudioFile(audioFile)
            } catch (e: Exception) {
                _state.value = VoiceMemoState.Error("处理异常: ${e.message}")
                notifyError("处理失败: ${e.message}")
                cleanupAudioFile(audioFile)
                transitionToIdle()
            }
        }
    }

    private suspend fun executeIntent(
        intent: LlmIntent,
        rawText: String,
        screenshot: Bitmap?,
        locationData: Pair<Location, String>?
    ) {
        when (intent.intent) {
            "add" -> executeAdd(intent, rawText, screenshot, locationData)
            "query" -> executeQuery(intent)
            "update" -> executeUpdate(intent)
            "delete" -> executeDelete(intent)
            else -> {
                _state.value = VoiceMemoState.Error("未知意图: ${intent.intent}")
                notifyError("无法理解您的意图")
                transitionToIdle()
            }
        }
    }

    private suspend fun executeAdd(
        intent: LlmIntent,
        rawText: String,
        screenshot: Bitmap?,
        locationData: Pair<Location, String>?
    ) {
        val now = System.currentTimeMillis()
        val imagePaths = JSONArray()
        val tagsJson = JSONArray()

        intent.tags.forEach { tag -> tagsJson.put(tag) }

        if (screenshot != null) {
            val imageFile = File(getDir("images", Context.MODE_PRIVATE), "img_${now}.jpg")
            withContext(Dispatchers.IO) {
                screenshot.compress(Bitmap.CompressFormat.JPEG, 85, imageFile.outputStream())
            }
            val imageInfo = org.json.JSONObject().apply {
                put("path", imageFile.absolutePath)
                put("source", "voice_screenshot")
            }
            imagePaths.put(imageInfo)
        }

        val memo = Memo(
            id = 0,
            content = intent.content.ifBlank { rawText },
            rawText = rawText,
            tags = tagsJson.toString(),
            imagePaths = imagePaths.toString(),
            latitude = locationData?.first?.latitude,
            longitude = locationData?.first?.longitude,
            address = locationData?.second,
            reminderTime = intent.reminderTime,
            createdAt = now,
            updatedAt = now,
            isDeleted = false
        )

        val id = memoRepository.addMemo(memo)

        _state.value = VoiceMemoState.Notifying("已添加: ${memo.content.take(50)}")
        notificationHelper.addResultNotification(
            title = "已添加备忘录",
            message = memo.content.take(100),
            memoId = id
        )
        transitionToIdle()
    }

    private suspend fun executeQuery(intent: LlmIntent) {
        val keywords = intent.queryKeywords.ifEmpty { intent.tags }
        if (keywords.isEmpty()) {
            _state.value = VoiceMemoState.Error("查询关键词为空")
            notifyError("查询关键词为空，请重试")
            transitionToIdle()
            return
        }

        val results = memoRepository.queryMemos(keywords)

        _state.value = VoiceMemoState.Notifying("找到 ${results.size} 条结果")
        notificationHelper.queryResultNotification(
            title = "找到 ${results.size} 条备忘录",
            results = results.map { it.content.take(50) },
            keywords = keywords
        )
        transitionToIdle()
    }

    private suspend fun executeUpdate(intent: LlmIntent) {
        val keywords = intent.queryKeywords.ifEmpty { intent.tags }
        if (keywords.isEmpty()) {
            _state.value = VoiceMemoState.Error("更新目标不明确")
            notifyError("更新目标不明确，请指定要更新的备忘录")
            transitionToIdle()
            return
        }

        val results = memoRepository.queryMemos(keywords)
        if (results.isEmpty()) {
            _state.value = VoiceMemoState.Error("未找到匹配的备忘录")
            notifyError("未找到匹配的备忘录")
            transitionToIdle()
            return
        }

        val target = results.first()
        val updated = target.copy(
            content = intent.content.ifBlank { target.content },
            updatedAt = System.currentTimeMillis()
        )
        memoRepository.updateMemo(updated)

        _state.value = VoiceMemoState.Notifying("已修改: ${updated.content.take(50)}")
        notificationHelper.addResultNotification(
            title = "已修改备忘录",
            message = updated.content.take(100),
            memoId = updated.id
        )
        transitionToIdle()
    }

    private suspend fun executeDelete(intent: LlmIntent) {
        val keywords = intent.queryKeywords.ifEmpty { intent.tags }
        if (keywords.isEmpty()) {
            _state.value = VoiceMemoState.Error("删除目标不明确")
            notifyError("删除目标不明确，请指定要删除的备忘录")
            transitionToIdle()
            return
        }

        val results = memoRepository.queryMemos(keywords)
        if (results.isEmpty()) {
            _state.value = VoiceMemoState.Error("未找到匹配的备忘录")
            notifyError("未找到匹配的备忘录")
            transitionToIdle()
            return
        }

        val target = results.first()
        memoRepository.deleteMemo(target.id)

        _state.value = VoiceMemoState.Notifying("已删除: ${target.content.take(50)}")
        notificationHelper.addResultNotification(
            title = "已删除备忘录",
            message = target.content.take(100),
            memoId = target.id
        )
        transitionToIdle()
    }

    private fun transitionToIdle() {
        serviceScope.launch {
            kotlinx.coroutines.delay(1000)
            _state.value = VoiceMemoState.Idle
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun notifyError(message: String) {
        val notification = notificationHelper.errorNotification(message)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID + 100, notification)
    }

    private fun cleanupAudioFile(file: File) {
        if (file.exists()) {
            file.delete()
        }
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val recordingChannel = NotificationChannel(
            CHANNEL_RECORDING,
            "录音状态",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "显示录音进行中的状态"
            setShowBadge(false)
        }

        val resultChannel = NotificationChannel(
            CHANNEL_RESULT,
            "处理结果",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "显示备忘录操作的结果"
        }

        val errorChannel = NotificationChannel(
            CHANNEL_ERROR,
            "错误提示",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "显示处理中的错误信息"
        }

        nm.createNotificationChannels(listOf(recordingChannel, resultChannel, errorChannel))
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) {
            audioRecorder.stopRecording()
            isRecording = false
        }
        serviceScope.cancel()
        _state.value = VoiceMemoState.Idle
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
```

- [ ] Commit: `git commit -m "feat: add VoiceMemoService with full processing pipeline"`

---

### Task 20: Accessibility Service (Volume Key Detection)

- [ ] Create `res/xml/accessibility_service_config.xml` for service configuration

**File:** `app/src/main/res/xml/accessibility_service_config.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeAllMask"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault|flagRequestAccessibilityButton|flagRetrieveInteractiveWindows"
    android:canRetrieveWindowContent="true"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="100" />
```

- [ ] Add the accessibility service description string to `res/values/strings.xml`

**File:** `app/src/main/res/values/strings.xml`

```xml
<resources>
    <string name="app_name">SecondBrain</string>
    <string name="accessibility_service_description">SecondBrain 语音备忘录服务，用于检测音量键触发录音，并在需要时获取屏幕截图。此服务不收集或上传任何个人信息。</string>
    <string name="notification_channel_recording">录音状态</string>
    <string name="notification_channel_result">处理结果</string>
    <string name="notification_channel_error">错误提示</string>
</resources>
```

- [ ] Create `service/VolumeKeyAccessibilityService.kt` with volume key detection and screenshot support

**File:** `app/src/main/java/com/qb/secondbrain/service/VolumeKeyAccessibilityService.kt`

```kotlin
package com.qb.secondbrain.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class VolumeKeyAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "VolumeKeyA11y"
        private const val SIMULTANEOUS_WINDOW_MS = 200L

        @Volatile
        private var lastScreenshot: Bitmap? = null

        @Volatile
        private var isServiceRunning = false

        private var isTriggered = false

        fun getLastScreenshot(): Bitmap? = lastScreenshot

        fun isRunning(): Boolean = isServiceRunning
    }

    private val handler = Handler(Looper.getMainLooper())

    private var volumeUpKeyDownTime = 0L
    private var volumeDownKeyDownTime = 0L
    private var volumeUpKeyUpTime = 0L
    private var volumeDownKeyUpTime = 0L
    private var volumeUpKeyPressed = false
    private var volumeDownKeyPressed = false

    private var hasTriggeredRecording = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        isTriggered = false
        hasTriggeredRecording = false

        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.DEFAULT or
                    AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }

        Log.d(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used for volume key detection
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return super.onKeyEvent(event)

        val keyCode = event.keyCode
        val action = event.action

        // Only handle volume keys
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return super.onKeyEvent(event)
        }

        when (action) {
            KeyEvent.ACTION_DOWN -> {
                handleKeyDown(keyCode, event.eventTime)
            }
            KeyEvent.ACTION_UP -> {
                handleKeyUp(keyCode, event.eventTime)
            }
        }

        // Consume volume key events to prevent them from changing volume during trigger
        return false
    }

    private fun handleKeyDown(keyCode: Int, eventTime: Long) {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                volumeUpKeyDownTime = eventTime
                volumeUpKeyPressed = true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                volumeDownKeyDownTime = eventTime
                volumeDownKeyPressed = true
            }
        }

        // Check if both keys are pressed within the window
        if (volumeUpKeyPressed && volumeDownKeyPressed) {
            val timeDiff = kotlin.math.abs(volumeUpKeyDownTime - volumeDownKeyDownTime)
            if (timeDiff <= SIMULTANEOUS_WINDOW_MS) {
                // Both keys down - capture screenshot for potential use
                captureScreenshot()
            }
        }
    }

    private fun handleKeyUp(keyCode: Int, eventTime: Long) {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                volumeUpKeyUpTime = eventTime
                volumeUpKeyPressed = false
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                volumeDownKeyUpTime = eventTime
                volumeDownKeyPressed = false
            }
        }

        // Check if both keys have been released within the simultaneous window
        if (volumeUpKeyUpTime > 0 && volumeDownKeyUpTime > 0) {
            val keyUpDiff = kotlin.math.abs(volumeUpKeyUpTime - volumeDownKeyUpTime)
            val keyDownDiff = kotlin.math.abs(volumeUpKeyDownTime - volumeDownKeyDownTime)

            if (keyUpDiff <= SIMULTANEOUS_WINDOW_MS && keyDownDiff <= SIMULTANEOUS_WINDOW_MS) {
                // Both keys released within window - this is a simultaneous short press
                onSimultaneousVolumePress()
                resetKeyState()
            }
        }
    }

    private fun onSimultaneousVolumePress() {
        Log.d(TAG, "Simultaneous volume press detected, hasTriggeredRecording=$hasTriggeredRecording")

        if (!hasTriggeredRecording) {
            // First trigger: start recording
            hasTriggeredRecording = true
            VoiceMemoService.startRecording(this)
            Log.d(TAG, "Sent START_RECORDING to VoiceMemoService")
        } else {
            // Second trigger: stop recording
            hasTriggeredRecording = false
            VoiceMemoService.stopRecording(this)
            Log.d(TAG, "Sent STOP_RECORDING to VoiceMemoService")
        }
    }

    private fun captureScreenshot() {
        try {
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                val bitmap = captureNodeToBitmap(rootNode)
                if (bitmap != null) {
                    // Recycle old screenshot
                    lastScreenshot?.recycle()
                    lastScreenshot = bitmap
                    Log.d(TAG, "Screenshot captured: ${bitmap.width}x${bitmap.height}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture screenshot", e)
        }
    }

    private fun captureNodeToBitmap(node: AccessibilityNodeInfo): Bitmap? {
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)

        if (bounds.isEmpty) return null

        return try {
            val bitmap = Bitmap.createBitmap(
                bounds.width(),
                bounds.height(),
                Bitmap.Config.ARGB_8888
            )
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create bitmap from node", e)
            null
        }
    }

    private fun resetKeyState() {
        volumeUpKeyDownTime = 0L
        volumeDownKeyDownTime = 0L
        volumeUpKeyUpTime = 0L
        volumeDownKeyUpTime = 0L
        volumeUpKeyPressed = false
        volumeDownKeyPressed = false
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        lastScreenshot?.recycle()
        lastScreenshot = null
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "Accessibility service destroyed")
    }
}
```

- [ ] Commit: `git commit -m "feat: add VolumeKeyAccessibilityService with screenshot support"`

---

### Task 21: Manifest Updates + Final Wiring

Update `AndroidManifest.xml` with all permissions, service declarations, and application name

**File:** `app/src/main/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Audio recording -->
    <uses-permission android:name="android.permission.RECORD_AUDIO" />

    <!-- Location -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

    <!-- Network -->
    <uses-permission android:name="android.permission.INTERNET" />

    <!-- Foreground service -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />

    <!-- Notifications -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <!-- Camera for photo capture -->
    <uses-permission android:name="android.permission.CAMERA" />

    <!-- Media access (Android 13+) -->
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />

    <!-- Accessibility service binding -->
    <uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />

    <queries>
        <package android:name="com.autonavi.minimap" />
    </queries>

    <application
        android:name=".SecondBrainApp"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.SecondBrain"
        tools:targetApi="31">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.SecondBrain">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Foreground service for voice memo processing -->
        <service
            android:name=".service.VoiceMemoService"
            android:exported="false"
            android:foregroundServiceType="microphone" />

        <!-- Accessibility service for volume key detection -->
        <service
            android:name=".service.VolumeKeyAccessibilityService"
            android:exported="false"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>

    </application>

</manifest>
```

- [ ] Verify all Hilt DI modules are complete. Create placeholder DI modules if they do not exist yet.

**File:** `app/src/main/java/com/qb/secondbrain/di/DatabaseModule.kt`

```kotlin
package com.qb.secondbrain.di

import android.content.Context
import androidx.room.Room
import com.qb.secondbrain.data.local.MemoDao
import com.qb.secondbrain.data.local.MemoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMemoDatabase(
        @ApplicationContext context: Context
    ): MemoDatabase {
        return Room.databaseBuilder(
            context,
            MemoDatabase::class.java,
            "second_brain_db"
        ).build()
    }

    @Provides
    fun provideMemoDao(database: MemoDatabase): MemoDao {
        return database.memoDao()
    }
}
```

**File:** `app/src/main/java/com/qb/secondbrain/di/RepositoryModule.kt`

```kotlin
package com.qb.secondbrain.di

import com.qb.secondbrain.data.repository.LocalMemoRepository
import com.qb.secondbrain.data.repository.MemoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMemoRepository(
        impl: LocalMemoRepository
    ): MemoRepository
}
```

- [ ] Commit: `git commit -m "feat: update manifest with permissions and service declarations"`

---

### Task 22: Build Verification + Integration Test

- [ ] Create the integration smoke test for Room database operations

**File:** `app/src/androidTest/java/com/qb/secondbrain/DatabaseIntegrationTest.kt`

```kotlin
package com.qb.secondbrain

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.qb.secondbrain.data.model.Memo
import com.qb.secondbrain.data.local.MemoDao
import com.qb.secondbrain.data.local.MemoDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseIntegrationTest {

    private lateinit var database: MemoDatabase
    private lateinit var memoDao: MemoDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            MemoDatabase::class.java
        ).allowMainThreadQueries().build()
        memoDao = database.memoDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun databaseCanBeCreated() {
        assertNotNull(database)
        assertNotNull(memoDao)
    }

    @Test
    fun insertAndQueryMemoById() = runTest {
        val memo = Memo(
            id = 0,
            content = "Test memo content",
            rawText = "Test raw text",
            tags = "[\"test\"]",
            imagePaths = "[]",
            latitude = 39.9042,
            longitude = 116.4074,
            address = "Beijing, China",
            reminderTime = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isDeleted = false
        )

        val id = memoDao.insertMemo(memo)
        assertTrue(id > 0)

        val retrieved = memoDao.getMemoById(id)
        assertNotNull(retrieved)
        assertEquals("Test memo content", retrieved!!.content)
        assertEquals("Test raw text", retrieved.rawText)
        assertEquals(39.9042, retrieved.latitude!!, 0.001)
        assertEquals(116.4074, retrieved.longitude!!, 0.001)
        assertEquals("Beijing, China", retrieved.address)
    }

    @Test
    fun queryAllMemosExcludesDeleted() = runTest {
        val now = System.currentTimeMillis()
        val memo1 = Memo(
            id = 0, content = "Active memo", rawText = "Active",
            tags = "[]", imagePaths = "[]",
            latitude = null, longitude = null, address = null,
            reminderTime = null, createdAt = now, updatedAt = now,
            isDeleted = false
        )
        val memo2 = Memo(
            id = 0, content = "Deleted memo", rawText = "Deleted",
            tags = "[]", imagePaths = "[]",
            latitude = null, longitude = null, address = null,
            reminderTime = null, createdAt = now, updatedAt = now,
            isDeleted = true
        )

        memoDao.insertMemo(memo1)
        memoDao.insertMemo(memo2)

        val allMemos = memoDao.getAllMemos()
        assertEquals(1, allMemos.size)
        assertEquals("Active memo", allMemos[0].content)
    }

    @Test
    fun updateMemo() = runTest {
        val now = System.currentTimeMillis()
        val memo = Memo(
            id = 0, content = "Original", rawText = "Original",
            tags = "[]", imagePaths = "[]",
            latitude = null, longitude = null, address = null,
            reminderTime = null, createdAt = now, updatedAt = now,
            isDeleted = false
        )

        val id = memoDao.insertMemo(memo)

        val inserted = memoDao.getMemoById(id)!!
        val updated = inserted.copy(content = "Updated", updatedAt = System.currentTimeMillis())
        memoDao.updateMemo(updated)

        val retrieved = memoDao.getMemoById(id)!!
        assertEquals("Updated", retrieved.content)
    }

    @Test
    fun deleteMemoSoftDelete() = runTest {
        val now = System.currentTimeMillis()
        val memo = Memo(
            id = 0, content = "To be deleted", rawText = "To be deleted",
            tags = "[]", imagePaths = "[]",
            latitude = null, longitude = null, address = null,
            reminderTime = null, createdAt = now, updatedAt = now,
            isDeleted = false
        )

        val id = memoDao.insertMemo(memo)
        val inserted = memoDao.getMemoById(id)!!
        memoDao.updateMemo(inserted.copy(isDeleted = true))

        val allMemos = memoDao.getAllMemos()
        assertEquals(0, allMemos.size)
    }

    @Test
    fun insertMemoWithTagsAndImages() = runTest {
        val now = System.currentTimeMillis()
        val memo = Memo(
            id = 0,
            content = "Memo with attachments",
            rawText = "Raw text",
            tags = "[\"parking\", \"QR code\"]",
            imagePaths = "[{\"path\":\"/data/img1.jpg\",\"source\":\"voice_screenshot\"}]",
            latitude = 22.5431,
            longitude = 114.0579,
            address = "Shenzhen, Guangdong",
            reminderTime = now + 3600000L,
            createdAt = now,
            updatedAt = now,
            isDeleted = false
        )

        val id = memoDao.insertMemo(memo)
        val retrieved = memoDao.getMemoById(id)!!

        assertEquals("[\"parking\", \"QR code\"]", retrieved.tags)
        assertTrue(retrieved.imagePaths.contains("voice_screenshot"))
        assertEquals(22.5431, retrieved.latitude!!, 0.001)
        assertNotNull(retrieved.reminderTime)
    }
}
```

- [ ] Run `./gradlew assembleDebug` to verify the full build compiles successfully

```bash
cd /Users/qinbin/projects/android/SecondBrain && ./gradlew assembleDebug
```

- [ ] Fix any compilation errors that arise from the build

```bash
# Review build output and fix issues iteratively
cd /Users/qinbin/projects/android/SecondBrain && ./gradlew assembleDebug 2>&1 | tail -50
```

- [ ] Run unit tests: `./gradlew testDebugUnitTest`

```bash
cd /Users/qinbin/projects/android/SecondBrain && ./gradlew testDebugUnitTest
```

- [ ] Run Android instrumented tests (requires connected device or emulator): `./gradlew connectedDebugAndroidTest`

```bash
cd /Users/qinbin/projects/android/SecondBrain && ./gradlew connectedDebugAndroidTest
```

- [ ] Commit: `git commit -m "test: add integration smoke test and verify build"`
