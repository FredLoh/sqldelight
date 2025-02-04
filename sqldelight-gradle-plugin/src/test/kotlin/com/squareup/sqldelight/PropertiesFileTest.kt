package com.squareup.sqldelight

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.SqlDelightCompilationUnit
import com.squareup.sqldelight.core.SqlDelightPropertiesFile
import com.squareup.sqldelight.core.SqlDelightSourceFolder
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File

class PropertiesFileTest {
  @Test fun `properties file generates correctly`() {
    val fixtureRoot = File("src/test/properties-file")
    File(fixtureRoot, ".idea").mkdir()

    GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()
        .withArguments("clean", "generateMainDatabaseInterface", "--stacktrace")
        .build()

    // verify
    val propertiesFile = File(fixtureRoot, ".idea/sqldelight/${SqlDelightPropertiesFile.NAME}")
    assertThat(propertiesFile.exists()).isTrue()

    val properties = SqlDelightPropertiesFile.fromFile(propertiesFile).databases.single()
    assertThat(properties.packageName).isEqualTo("com.example")
    assertThat(properties.outputDirectory).isEqualTo("build/sqldelight/Database")
    assertThat(properties.compilationUnits).hasSize(1)

    with(properties.compilationUnits[0]) {
      assertThat(sourceFolders).containsExactly(
          SqlDelightSourceFolder("src/main/sqldelight", false))
    }

    propertiesFile.delete()
  }

  @Test fun `properties file for an android multiplatform module`() {
    withTemporaryFixture {
      gradleFile("""|
        |plugins {
        |  id 'org.jetbrains.kotlin.multiplatform'
        |  id 'com.squareup.sqldelight'
        |  id 'com.android.library'
        |}
        |
        |apply from: "${"$"}{rootDir}/../../../../gradle/dependencies.gradle"
        |
        |archivesBaseName = 'Test'
        |
        |android {
        |  compileSdkVersion versions.compileSdk
        |
        |  compileOptions {
        |    sourceCompatibility JavaVersion.VERSION_1_8
        |    targetCompatibility JavaVersion.VERSION_1_8
        |  }
        |
        |  defaultConfig {
        |    minSdkVersion versions.minSdk
        |  }
        |}
        |
        |sqldelight {
        |  CashDatabase {
        |    packageName = "com.squareup.sqldelight.sample"
        |  }
        |}
        |
        |kotlin {
        |  sourceSets {
        |    commonMain {
        |      dependencies {
        |        implementation deps.kotlin.stdlib.common
        |      }
        |    }
        |    androidLibMain {
        |      dependencies {
        |      }
        |    }
        |  }
        |
        |  targetFromPreset(presets.android, 'androidLib')
        |}
      """.trimMargin())

      val database = properties().databases.single()
      assertThat(database.compilationUnits).containsExactly(
          SqlDelightCompilationUnit(
              name="androidLibDebug",
              sourceFolders=listOf(
                  SqlDelightSourceFolder(path="src/androidLibDebug/sqldelight", dependency=false),
                  SqlDelightSourceFolder(path="src/androidLibMain/sqldelight", dependency=false),
                  SqlDelightSourceFolder(path="src/commonMain/sqldelight", dependency=false)
              )
          ),
          SqlDelightCompilationUnit(
              name="androidLibRelease",
              sourceFolders=listOf(
                  SqlDelightSourceFolder(path="src/androidLibMain/sqldelight", dependency=false),
                  SqlDelightSourceFolder(path="src/androidLibRelease/sqldelight", dependency=false),
                  SqlDelightSourceFolder(path="src/commonMain/sqldelight", dependency=false)
              )
          ),
          SqlDelightCompilationUnit(
              name="metadataMain",
              sourceFolders=listOf(
                  SqlDelightSourceFolder(path="src/commonMain/sqldelight", dependency=false)
              )
          )
      )
    }
  }
}
