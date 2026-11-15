package io.openhoyi.mobile

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Rect
import android.os.SystemClock
import android.view.View
import android.widget.EditText

/** Disposable Mock UI regression. It never selects a curve or starts an extraction. */
internal class CustomFlowCurveEditorChecks(private val test: Instrumentation) {
    private val context get() = test.targetContext
    private val opened = mutableListOf<Activity>()
    private fun main(action: () -> Unit) {
        var failure: Throwable? = null
        test.runOnMainSync { try { action() } catch (error: Throwable) { failure = error } }
        failure?.let { throw it }
    }
    private fun await(label: String, condition: () -> Boolean) {
        val end = SystemClock.elapsedRealtime() + 15000
        while (true) {
            var ready = false; main { ready = condition() }
            if (ready) return
            check(SystemClock.elapsedRealtime() < end) { "Curve editor timeout: $label" }
            Thread.sleep(25)
        }
    }
    private fun root(activity: Activity): View = activity.findViewById(android.R.id.content)
    private fun input(activity: Activity, key: String) = requireNotNull(root(activity).findViewWithTag<EditText>("product/form/curve-editor/$key"))
    private fun source(activity: Activity) = CurveEditorActivity::class.java.getDeclaredField("sourceDocument")
        .apply { isAccessible = true }.get(activity) as CustomCurveDocument
    private fun select(activity: Activity, label: Int, index: Int) {
        val key = when (label) {
            R.string.curve_control_mode -> "control-mode"
            R.string.curve_first_mode -> "first-mode"
            else -> "pressure-logic"
        }
        val picker = requireNotNull(root(activity).findViewWithTag<CurveEditorSegments>("product/form/curve-editor/$key"))
        check(picker.isShown && picker.isEnabled)
        picker.selectFromUser(index == 1)
    }
    private fun checked(activity: Activity, key: String) =
        requireNotNull(root(activity).findViewWithTag<CurveEditorSegments>("product/form/curve-editor/$key")).checked
    private fun saveDocument(activity: Activity): CustomCurveDocument {
        val store = (context.applicationContext as MobileApplication).customCurvesResult.getOrThrow()
        val before = store.list().map { it.id }.toSet()
        save(activity)
        await("saved new document") { activity.isDestroyed && store.list().any { it.id !in before } }
        return store.list().single { it.id !in before }
    }
    private fun start(id: String? = null): Activity = test.startActivitySync(Intent(context, CurveEditorActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).apply { if (id != null) putExtra(CurveEditorActivity.CURVE_ID, id) }).also {
        opened += it; test.waitForIdleSync(); await("ready") { it.hasWindowFocus() && root(it).isLaidOut }
    }
    private fun recreate(activity: Activity): Activity {
        val monitor = test.addMonitor(CurveEditorActivity::class.java.name, null, false)
        return try {
            main { activity.recreate() }
            requireNotNull(test.waitForMonitorWithTimeout(monitor, 10000)).also {
                opened += it; test.waitForIdleSync(); await("recreated") { it !== activity && it.hasWindowFocus() && root(it).isLaidOut }
            }
        } finally { test.removeMonitor(monitor) }
    }
    private fun save(activity: Activity) {
        main {
            val button = requireNotNull(root(activity).findViewWithTag<View>("product/form/curve-editor/save"))
            button.requestRectangleOnScreen(Rect(0, 0, button.width, button.height), true)
            check(button.isEnabled && button.performClick())
        }
        test.waitForIdleSync()
    }
    fun run(disposable: Boolean) {
        check(disposable && BuildConfig.MOCK_MODE && context.packageName == "io.openhoyi.mobile.mock")
        check(android.os.Build.HARDWARE in setOf("ranchu", "goldfish")) { "Physical devices are forbidden" }
        val qemu = test.uiAutomation.executeShellCommand("getprop ro.kernel.qemu").use { descriptor ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText().trim() }
        }
        check(qemu == "1") { "Disposable emulator required" }
        val app = context.applicationContext as MobileApplication
        val store = app.customCurvesResult.getOrThrow()
        val originalDrafts = store.list()
        val originalHistory = app.history.entries
        check(originalHistory.none { it.status in setOf(ShotHistory.Status.STARTING, ShotHistory.Status.RUNNING,
            ShotHistory.Status.STOP_REQUESTED, ShotHistory.Status.UNKNOWN) })
        val prefs = context.getSharedPreferences("curves", android.content.Context.MODE_PRIVATE)
        val selected = prefs.getString("selected", null)
        val created = mutableSetOf<String>()
        try {
            var editor = start()
            main {
                input(editor, "target-0").setText("12.")
                select(editor, R.string.curve_control_mode, 1)
                check(input(editor, "target-0").text.toString() == "12.") // First pressure exception.
                select(editor, R.string.curve_first_mode, 1)
                check(input(editor, "target-0").text.toString() == "1")
                input(editor, "target-0").setText("2.50")
                select(editor, R.string.curve_first_mode, 0)
                check(input(editor, "target-0").text.toString() == "12.")
                select(editor, R.string.curve_first_mode, 1)
                check(input(editor, "target-0").text.toString() == "2.50")
                select(editor, R.string.curve_control_mode, 0)
                check(input(editor, "target-0").text.toString() == "12.")
                select(editor, R.string.curve_pressure_logic, 1)
                select(editor, R.string.curve_control_mode, 1)
                check(checked(editor, "pressure-logic"))
                select(editor, R.string.curve_flow_logic, 0)
                select(editor, R.string.curve_control_mode, 0)
                check(!checked(editor, "pressure-logic"))
                input(editor, "description").setText("磨豆机 4.5\n粉重 18 g")
                input(editor, "target-0").requestFocus()
                input(editor, "target-0").setSelection(1, 3)
            }
            editor = recreate(editor)
            main {
                check(input(editor, "target-0").text.toString() == "12.")
                check(input(editor, "target-0").selectionStart == 1 && input(editor, "target-0").selectionEnd == 3)
                check(input(editor, "description").text.toString() == "磨豆机 4.5\n粉重 18 g")
                select(editor, R.string.curve_control_mode, 1)
                check(input(editor, "target-0").text.toString() == "2.50")
                select(editor, R.string.curve_control_mode, 0)
                input(editor, "target-0").setText("3")
                input(editor, "temperature").setText("106")
            }
            save(editor)
            main {
                check(!editor.isFinishing && input(editor, "temperature").error != null && input(editor, "temperature").hasFocus())
                input(editor, "temperature").setText("93")
                input(editor, "cup").setText("200.01")
            }
            save(editor)
            main {
                check(!editor.isFinishing && input(editor, "cup").error != null && input(editor, "cup").hasFocus())
                input(editor, "cup").setText("36")
            }
            val id = source(editor).id; created += id
            save(editor); await("description saved") { editor.isDestroyed && store.find(id)?.description == "磨豆机 4.5\n粉重 18 g" }
            check(CustomCurveDocument.decode(requireNotNull(store.find(id)).encode()) == store.find(id))
            // Remove only the last stage: stable field identities preserve hidden slots and both unit banks.
            val old = CustomCurveDocument(CustomCurveDocument.newId(), "Private tail", 93, 3600,
                CustomCurveDocument.ControlMode.PRESSURE, listOf(30, 90, 80, 60).map { CustomCurveDocument.Stage(it, 150) },
                description = "四段\n保留说明", compensationHundredthsGram = 20)
            store.save(old); created += old.id
            editor = start(old.id)
            main {
                check(root(editor).findViewWithTag<View>("curve-stage-remove-0").visibility == View.INVISIBLE)
                selectCurveStageCount(root(editor), 2)
                input(editor, "target-3").setText("999")
            }
            save(editor)
            main {
                check(!editor.isFinishing && input(editor, "target-3").error != null)
                check(root(editor).findViewWithTag<View>("curve-editor-stage-3").visibility == View.GONE)
                check(input(editor, "target-3").text.toString() == "999" && store.find(old.id) == old)
                selectCurveStageCount(root(editor), 4)
                check(input(editor, "target-3").text.toString() == "999")
                input(editor, "target-3").setText("6")
                selectCurveStageCount(root(editor), 2)
            }
            val reduced = saveDocument(editor); created += reduced.id
            check(store.find(old.id) == old)
            check(reduced.stages.size == 2 && reduced.inactiveStages == old.stages.drop(2))
            check(reduced.description == old.description && reduced.compensationHundredthsGram == old.compensationHundredthsGram)
            editor = start(reduced.id)
            main {
                selectCurveStageCount(root(editor), 4)
                old.stages.forEachIndexed { index, stage ->
                    check(input(editor, "target-$index").text.toString() == CurveDraftNumber.format(stage.target, 1))
                    check(input(editor, "water-$index").text.toString() == CurveDraftNumber.format(stage.waterTenthsMl, 1))
                }
            }
            val restored = saveDocument(editor); created += restored.id
            check(restored == old.copy(id = restored.id) && store.find(old.id) == old && store.find(reduced.id) == reduced)
            val flow = old.copy(id = CustomCurveDocument.newId(), controlMode = CustomCurveDocument.ControlMode.FLOW,
                stages = listOf(CustomCurveDocument.Stage(30, 0), CustomCurveDocument.Stage(20, 400)),
                pressureLogic = true, firstDurationSeconds = 8)
            store.save(flow); created += flow.id
            editor = start(flow.id)
            main {
                check(root(editor).findViewWithTag<android.widget.TextView>("curve-stage-mode-0").text.toString() == context.getString(R.string.curve_editor_mode_first_pressure))
                input(editor, "first-duration").setText("0")
            }
            save(editor)
            main { check(!editor.isFinishing && input(editor, "first-duration").error != null); editor.finish() }
            val historical = flow.copy(id = CustomCurveDocument.newId(), firstDurationSeconds = 255)
            store.save(historical); created += historical.id
            editor = start(historical.id)
            main { input(editor, "description").setText("历史只改说明\n保留255") }
            val historicalCopy = saveDocument(editor); created += historicalCopy.id
            check(historicalCopy == historical.copy(id = historicalCopy.id, description = "历史只改说明\n保留255"))
            check(store.find(historical.id) == historical)
            check(store.list().filter { it.id !in created } == originalDrafts)
            check(prefs.getString("selected", null) == selected && app.history.entries == originalHistory)
        } finally {
            main { opened.asReversed().filter { !it.isDestroyed }.forEach { it.finish() } }
        }
    }
}
