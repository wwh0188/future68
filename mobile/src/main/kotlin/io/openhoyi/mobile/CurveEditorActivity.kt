package io.openhoyi.mobile

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout

class CurveEditorActivity : ThemedActivity() {
    companion object { const val CURVE_ID = "curveId" }
    private var form: TaskCurveEditorForm? = null
    private val fields = linkedMapOf<String, EditText>()
    private var draftId = CustomCurveDocument.newId()
    private var stageCount = 2
    private var mode = CustomCurveDocument.ControlMode.PRESSURE
    private var pressureLogic = false
    private var flowLogic = false
    private var firstFlow = false
    private val targetInputs = linkedMapOf<String, String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val style = TaskProductFormStyle(this)
        val body = style.visual.page(this, getString(R.string.profiles_editor), getString(R.string.curve_modes_execution))
        val store = profileStore(body) ?: return
        val sourceId = intent.getStringExtra(CURVE_ID)
        draftId = savedInstanceState?.getString("draftId") ?: draftId
        val source = if (sourceId == null) CustomCurveDocument(draftId, getString(R.string.profiles_default_name), 93, 3600,
            CustomCurveDocument.ControlMode.PRESSURE, listOf(CustomCurveDocument.Stage(30, 150), CustomCurveDocument.Stage(90, 400)))
        else runCatching {
            store.find(sourceId) ?: (application as MobileApplication).curves.find(sourceId)?.let { CustomCurveDocument.fromLibraryItem(it, draftId) }
        }.getOrNull()
        if (source == null) { HoyiUi.label(this, body, getString(R.string.profiles_missing), 17); return }
        draftId = source.id
        if (source.controlMode == CustomCurveDocument.ControlMode.FLOW_RAW) {
            profileCard(body, source)
            val description = beanInput(body, R.string.curve_description, false).apply {
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                setSingleLine(false); minLines = 2; maxLines = 4
                setText(savedInstanceState?.getString("description") ?: source.description)
                filters = arrayOf(android.text.InputFilter.LengthFilter(8000))
            }
            fields["description"] = description
            val error = beanError(body)
            HoyiUi.button(this, body, getString(R.string.profiles_save)) {
                if (runCatching { store.save(source.copy(description = description.text.toString())) }.isSuccess) finish()
                else error.setText(R.string.profiles_save_failed)
            }
            return
        }
        if (sourceId != null && !sourceId.startsWith("draft-")) style.visual.add(body,
            style.visual.text(getString(R.string.curve_copy_warning), TaskProductVisualStyle.auxiliary, secondary = true))
        stageCount = savedInstanceState?.getInt("stageCount") ?: source.stages.size
        mode = savedInstanceState?.getString("controlMode")?.let { CustomCurveDocument.ControlMode.valueOf(it) } ?: source.controlMode
        pressureLogic = savedInstanceState?.getBoolean("pressureLogic") ?: (source.controlMode == CustomCurveDocument.ControlMode.PRESSURE && source.pressureLogic)
        flowLogic = savedInstanceState?.getBoolean("flowLogic") ?: (source.controlMode == CustomCurveDocument.ControlMode.FLOW && source.pressureLogic)
        firstFlow = savedInstanceState?.getBoolean("firstFlow") ?: source.firstSegmentFlowMode
        for (index in 0..3) for (flow in listOf(false, true)) {
            val key = "$index-$flow"
            val initial = if (index < source.stages.size && source.isFlowStage(index) == flow)
                CurveDraftNumber.format(source.stages[index].target, 1) else if (flow) "1" else "6"
            targetInputs[key] = savedInstanceState?.getString("bank-$key") ?: initial
        }
        val sections = TaskCurveEditorForm(this, body, style)
        form = sections
        fun field(parent: LinearLayout, key: String, label: Int, initial: String, numeric: Boolean = true,
            compactLabel: String? = null, tableCell: Boolean = false): EditText {
            val input = if (tableCell) EditText(this).apply {
                id = View.generateViewId()
                textSize = 16f
                typeface = TaskTypography.face(this@CurveEditorActivity, 400)
                includeFontPadding = false
                setTextColor(getColor(R.color.mobile_text))
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                setSingleLine(true)
                minimumHeight = HoyiUi.dp(this@CurveEditorActivity, 52)
                setPadding(HoyiUi.dp(this@CurveEditorActivity, 12), HoyiUi.dp(this@CurveEditorActivity, 8),
                    HoyiUi.dp(this@CurveEditorActivity, 12), HoyiUi.dp(this@CurveEditorActivity, 8))
                background = HoyiUi.shape(this@CurveEditorActivity, R.color.mobile_surface, 10, R.color.mobile_border)
                parent.addView(this, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = HoyiUi.dp(this@CurveEditorActivity, 8) })
            } else beanInput(parent, label, numeric).also { editor ->
                if (compactLabel != null) for (index in 0 until parent.childCount) {
                    val title = parent.getChildAt(index) as? android.widget.TextView ?: continue
                    if (title.labelFor == editor.id) {
                        title.text = compactLabel
                        style.label(title)
                    }
                }
            }
            style.input(input, "curve-editor", key)
            input.contentDescription = getString(label)
            input.tooltipText = getString(label)
            fields[key] = input
            input.setText(savedInstanceState?.getString(key) ?: initial)
            return input
        }
        val name = field(sections.metadata[0], "name", R.string.profiles_name, source.name, false,
            compactLabel = getString(R.string.profiles_name))
        val temperature = field(sections.metadata[1], "temperature", R.string.profiles_temperature, source.temperatureC.toString(),
            compactLabel = getString(R.string.curve_preview_temperature))
        val cup = field(sections.metadata[2], "cup", R.string.profiles_cup, CurveDraftNumber.format(source.targetHundredthsGram, 2),
            compactLabel = getString(R.string.curve_preview_weight))
        style.unit(temperature, "°C", "curve-editor", "temperature")
        style.unit(cup, "g", "curve-editor", "cup")
        style.visual.text(getString(R.string.form_visual_stage_parameters), TaskProductVisualStyle.section).apply {
            sections.stageHeader.addView(this)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        (0..3).forEach { index ->
            val stage = source.stages.getOrNull(index) ?: CustomCurveDocument.Stage(60, 100)
            val flow = mode == CustomCurveDocument.ControlMode.FLOW && (index > 0 || firstFlow)
            val row = sections.stageRows[index]
            val target = field(row, "target-$index", R.string.curve_target, targetInputs.getValue("$index-$flow"), tableCell = true)
            val water = field(row, "water-$index", R.string.profiles_water, CurveDraftNumber.format(stage.waterTenthsMl, 1), tableCell = true)
            style.unit(target, if (flow) "mL/s" else "bar", "curve-editor", "target-$index")
            style.unit(water, "ml", "curve-editor", "water-$index")
        }
        val description = field(sections.waterLimit, "description", R.string.curve_description, source.description, false).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setSingleLine(false)
            minLines = 3
            maxLines = 8
            gravity = android.view.Gravity.TOP
            hint = getString(R.string.curve_description_hint)
            filters = arrayOf(android.text.InputFilter.LengthFilter(8000))
        }
        val preinfusion = field(sections.waterLimit, "preinfusion", R.string.curve_preinfusion, source.preinfusionSeconds.toString())
        val firstDuration = field(sections.waterLimit, "first-duration", R.string.curve_first_duration, source.firstDurationSeconds.toString())
        val totalLimit = field(sections.waterLimit, "total-water-limit", R.string.profiles_total_water_limit,
            source.totalWaterLimitMl?.toString().orEmpty(), compactLabel = getString(R.string.profiles_total_water_limit_title)).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = getString(R.string.profiles_total_water_limit_hint)
        }
        style.unit(totalLimit, "mL", "curve-editor", "total-water-limit")
        val assignedWater = style.visual.text("", TaskProductVisualStyle.auxiliary,
            "product/form/curve-editor/assigned-water", true)
        style.visual.add(sections.waterLimit, assignedWater, 8)
        style.visual.add(sections.waterLimit, style.visual.text(getString(R.string.profiles_total_water_limit_scale),
            TaskProductVisualStyle.auxiliary, secondary = true), 8)
        var previewChanged: () -> Unit = {}
        fun flowStage(index: Int) = mode == CustomCurveDocument.ControlMode.FLOW && (index > 0 || firstFlow)
        fun rememberTargets() { for (index in 0..3) targetInputs["$index-${flowStage(index)}"] = fields.getValue("target-$index").text.toString() }
        fun restoreTargets() {
            for (index in 0..3) {
                fields.getValue("target-$index").apply {
                    setText(targetInputs.getValue("$index-${flowStage(index)}"))
                    contentDescription = getString(R.string.curve_target) + " · " + if (flowStage(index)) "mL/s" else "bar"
                }
                sections.stageRows[index].findViewWithTag<android.widget.TextView>("product/form/curve-editor/target-$index/unit")?.text =
                    if (flowStage(index)) "mL/s" else "bar"
            }
            previewChanged()
        }
        fun selector(label: Int, options: List<Int>, selected: () -> Int, change: (Int) -> Unit): () -> Unit {
            val group = style.visual.column("curve-mode-group-$label")
            style.visual.add(sections.modeSettings, group, 8)
            style.visual.add(group, style.visual.text(getString(label), TaskProductVisualStyle.auxiliary))
            val row = style.visual.row()
            style.visual.add(group, row)
            val buttons = options.mapIndexed { index, title ->
                HoyiUi.button(this, row, getString(title)) { rememberTargets(); change(index); restoreTargets() }.apply {
                    layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                    tag = "curve-mode-$label-$index"
                }
            }
            return { buttons.forEachIndexed { index, button -> style.selected(button, selected() == index) } }
        }
        var refreshModes: () -> Unit = {}
        val refreshOverall = selector(R.string.curve_control_mode, listOf(R.string.curve_mode_pressure, R.string.curve_mode_flow),
            { if (mode == CustomCurveDocument.ControlMode.FLOW) 1 else 0 }) {
            mode = if (it == 0) CustomCurveDocument.ControlMode.PRESSURE else CustomCurveDocument.ControlMode.FLOW
            refreshModes()
        }
        val refreshPressure = selector(R.string.curve_pressure_logic, listOf(R.string.curve_pressure_constant, R.string.curve_pressure_follow),
            { if (pressureLogic) 1 else 0 }) { pressureLogic = it == 1; refreshModes() }
        val refreshFlow = selector(R.string.curve_flow_logic, listOf(R.string.curve_flow_ls, R.string.curve_flow_hs),
            { if (flowLogic) 1 else 0 }) { flowLogic = it == 1; refreshModes() }
        val refreshFirst = selector(R.string.curve_first_mode, listOf(R.string.curve_first_pressure, R.string.curve_first_flow),
            { if (firstFlow) 1 else 0 }) { firstFlow = it == 1; refreshModes() }
        refreshModes = {
            refreshOverall(); refreshPressure(); refreshFlow(); refreshFirst()
            sections.modeSettings.findViewWithTag<View>("curve-mode-group-${R.string.curve_pressure_logic}").visibility =
                if (mode == CustomCurveDocument.ControlMode.PRESSURE) View.VISIBLE else View.GONE
            listOf(R.string.curve_flow_logic, R.string.curve_first_mode).forEach { label ->
                sections.modeSettings.findViewWithTag<View>("curve-mode-group-$label").visibility =
                    if (mode == CustomCurveDocument.ControlMode.FLOW) View.VISIBLE else View.GONE
            }
            // Each overall mode retains its own logic selection.
            sections.modeSettings.findViewWithTag<View>("curve-mode-${R.string.curve_pressure_logic}-0").isEnabled = mode == CustomCurveDocument.ControlMode.PRESSURE
            sections.modeSettings.findViewWithTag<View>("curve-mode-${R.string.curve_pressure_logic}-1").isEnabled = mode == CustomCurveDocument.ControlMode.PRESSURE
            listOf(R.string.curve_flow_logic, R.string.curve_first_mode).forEach { label ->
                for (index in 0..1) sections.modeSettings.findViewWithTag<View>("curve-mode-$label-$index").isEnabled = mode == CustomCurveDocument.ControlMode.FLOW
            }
        }
        refreshModes()
        style.visual.add(sections.modeSettings, style.visual.text(getString(R.string.curve_mode_hint), TaskProductVisualStyle.auxiliary, secondary = true), 8)
        fun visibility() {
            sections.setStageCount(stageCount)
            previewChanged()
        }
        val selector = CurveStageCountTabs(this, stageCount, style) { count ->
            stageCount = count
            visibility()
        }
        sections.stageHeader.addView(selector, LinearLayout.LayoutParams(HoyiUi.dp(this, 192), -2))
        visibility()
        val error = style.error(beanError(body))
        fun parsed(): CustomCurveDocument {
            val stages = (0 until stageCount).map {
                CustomCurveDocument.Stage(CurveDraftNumber.parse(fields.getValue("target-$it").text.toString(), 1),
                    CurveDraftNumber.parse(fields.getValue("water-$it").text.toString(), 1))
            }
            val maximum = maxOf(((stages.sumOf { it.waterTenthsMl.toLong() } + 9) / 10).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), 800)
            val limit = totalLimit.text.toString().trim().takeIf { it.isNotEmpty() }?.let { text ->
                try { CurveDraftNumber.parse(text, 0) }
                catch (_: IllegalArgumentException) { throw CurveWaterLimitException(maximum) }
            }
            return CustomCurveDocument(draftId, name.text.toString().trim(),
                CurveDraftNumber.parse(temperature.text.toString(), 0), CurveDraftNumber.parse(cup.text.toString(), 2),
                mode, stages, limit, if (mode == CustomCurveDocument.ControlMode.FLOW) flowLogic else pressureLogic,
                mode == CustomCurveDocument.ControlMode.FLOW && firstFlow,
                CurveDraftNumber.parse(preinfusion.text.toString(), 0), CurveDraftNumber.parse(firstDuration.text.toString(), 0),
                description.text.toString()).also { it.validate() }
        }
        fun showError(failure: Throwable) {
            error.text = if (failure is CurveWaterLimitException)
                getString(R.string.profiles_total_water_limit_invalid, 1, failure.maximumMl)
            else getString(R.string.curve_modes_invalid)
        }
        val preview = CurveStageView(this).apply { tag = "task-form-stage-preview"; productVisualStyle = style.visual }
        style.visual.add(sections.supportingBody, style.visual.text(getString(R.string.profiles_preview), TaskProductVisualStyle.section))
        sections.supportingBody.addView(preview, LinearLayout.LayoutParams(-1, HoyiUi.dp(this, 280)))
        style.visual.add(sections.supportingBody, style.visual.text(getString(R.string.task_form_stage_caption), TaskProductVisualStyle.auxiliary, secondary = true), 8)
        val previewStatus = style.visual.text("", TaskProductVisualStyle.auxiliary).apply {
            style.visual.add(sections.supportingBody, this)
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        previewChanged = {
            assignedWater.text = runCatching {
                val tenths = (0 until stageCount).sumOf {
                    CurveDraftNumber.parse(fields.getValue("water-$it").text.toString(), 1).also { require(it in 1..65535) }
                }
                getString(R.string.profiles_assigned_water, CurveDraftNumber.format(tenths, 1))
            }.getOrElse { getString(R.string.profiles_assigned_water_invalid) }
            runCatching { parsed() }.onSuccess { doc ->
                preview.targets = doc.stages.map { it.target }
                preview.preview = CurveStageParameters.preview(CurveLibraryItem(doc.id, doc.name, "", "", null, customDocument = doc))
                preview.contentDescription = profileSummary(doc)
                previewStatus.text = getString(R.string.profiles_summary, doc.temperatureC, CurveDraftNumber.format(doc.targetHundredthsGram, 2), doc.stages.size) + "\n" + profileWaterLimitSummary(doc)
            }.onFailure {
                preview.targets = emptyList()
                preview.preview = null
                preview.contentDescription = getString(R.string.curve_modes_invalid)
                previewStatus.setText(R.string.curve_modes_invalid)
            }
            Unit
        }
        fields.values.forEach { field ->
            field.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { previewChanged() }
                override fun afterTextChanged(s: android.text.Editable?) = Unit
            })
        }
        previewChanged()
        sections.restore(savedInstanceState, fields)
        val supportingActions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            isBaselineAligned = false
            sections.supportingBody.addView(this, LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = HoyiUi.dp(this@CurveEditorActivity, 8)
            })
        }
        HoyiUi.button(this, supportingActions, getString(R.string.profiles_preview)) {
            runCatching { parsed() }.onSuccess { profilePreviewDialog(it); error.text = "" }.onFailure(::showError)
        }.apply { layoutParams = LinearLayout.LayoutParams(0, -2, 1f); style.visual.button(this) }
        HoyiUi.button(this, supportingActions, getString(R.string.ui_full_details)) {
            beanDetails(getString(R.string.profiles_editor), listOf(R.string.profiles_temperature, R.string.profiles_cup,
                R.string.curve_mode_hint, R.string.curve_modes_execution, R.string.curve_description_hint, R.string.profiles_water, R.string.profiles_total_water_limit,
                R.string.profiles_total_water_limit_scale, R.string.profiles_pressure_execution_limits).joinToString("\n\n") { getString(it) })
        }.apply { layoutParams = LinearLayout.LayoutParams(0, -2, 1f); style.visual.button(this) }
        val footer = beanActions(body)
        val actionRow = style.visual.row()
        style.visual.action(actionRow, getString(R.string.beans_cancel), "product/form/curve-editor/cancel", top = 0) { finish() }
            .layoutParams = LinearLayout.LayoutParams(-2, -2).apply { marginEnd = style.dp(8) }
        val save = style.visual.action(actionRow, getString(R.string.profiles_save), "product/form/curve-editor/save", primary = true, top = 0) {}
        save.layoutParams = LinearLayout.LayoutParams(-2, -2)
        val notice = style.visual.text(getString(R.string.form_visual_local_curve_scope), TaskProductVisualStyle.auxiliary,
            "product/form/curve-editor/scope", true)
        footer.addView(style.footer(notice, actionRow, "curve-editor", topRule = true), LinearLayout.LayoutParams(-1, -2))
        save.setOnClickListener {
            val doc = runCatching { parsed() }.getOrElse { showError(it); return@setOnClickListener }
            val available = (application as MobileApplication).customCurvesResult.getOrNull()
            if (available == null) { error.setText(R.string.profiles_unavailable); save.isEnabled = false; return@setOnClickListener }
            save.isEnabled = false
            if (runCatching { available.save(doc) }.isSuccess) finish()
            else { error.setText(R.string.profiles_save_failed); save.isEnabled = true }
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("draftId", draftId); outState.putInt("stageCount", stageCount)
        outState.putString("controlMode", mode.name)
        outState.putBoolean("pressureLogic", pressureLogic); outState.putBoolean("flowLogic", flowLogic); outState.putBoolean("firstFlow", firstFlow)
        for (index in 0..3) {
            val flow = mode == CustomCurveDocument.ControlMode.FLOW && (index > 0 || firstFlow)
            fields["target-$index"]?.let { targetInputs["$index-$flow"] = it.text.toString() }
        }
        targetInputs.forEach { (key, value) -> outState.putString("bank-$key", value) }
        fields.forEach { (key, field) -> outState.putString(key, field.text.toString()) }
        form?.save(outState, fields)
    }
}
