package com.pdfreader.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NoxReaderMacrobenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartupToLibraryReady() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        startupMode = StartupMode.COLD,
        iterations = 5,
        setupBlock = {
            pressHome()
        },
        measureBlock = {
            startActivityAndWait()
        }
    )

    @Test
    fun settingsScrollFrames() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            iterations = 5,
            setupBlock = {
                pressHome()
                startActivityAndWait()
                if (device.wait(Until.hasObject(By.desc("Open settings")), UI_TIMEOUT_MS)) {
                    device.findObject(By.desc("Open settings")).click()
                }
                check(device.wait(Until.hasObject(By.text("Settings")), UI_TIMEOUT_MS)) {
                    "Settings did not become ready for the benchmark."
                }
            },
            measureBlock = {
                val centerX = device.displayWidth / 2
                val upperY = device.displayHeight / 4
                val lowerY = device.displayHeight * 3 / 4
                device.swipe(centerX, lowerY, centerX, upperY, 20)
                device.waitForIdle()
                device.swipe(centerX, upperY, centerX, lowerY, 20)
                device.waitForIdle()
            }
        )
    }

    private companion object {
        const val PACKAGE_NAME = "com.pdfreader.app"
        const val UI_TIMEOUT_MS = 5_000L
    }
}
