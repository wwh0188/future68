package io.openhoyi.mobile

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout

class CurveEditorActivity : ThemedActivity() {
    private class FieldFailure(val key: String, val label: Int) : IllegalArgumentException()
    companion object { const val CURVE_ID = "curveId" }
    private var form: TaskCurveEditorForm? = null
    private val fields = linkedMapOf<String, EditText>()
    private var draftId = CustomCurveDocument.newId()
    private var stageCount = 2
    private var mode = CustomCurveDocument.ControlMode.PRESSURE
    private var pressureLogic = false
    private var flowLogic = false
    private var firstFlow = false
    private var pressureBeforeFlow = false
    private var sourceDocument: CustomCurveDocument? = null
    private val targetInputs = linkedMapOf<String, String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val style = TaskProductFormStyle(this)
        val colors = CurveEditorColors(this)
        val body = style.visual.page(this, getString(R.string.curve_editor_title), getString(R.string.curve_modes_execution))
        val store = profileStore(body) ?: return
        val sourceId = intent.getStringExtra(CURVE_ID)
        draftId = savedInstanceState?.getString("draftId") ?: draftId
        val source = savedInstanceState?.getString("sourceDocument")?.let { runCatching { CustomCurveDocument.decode(it) }.getOrNull() }
            ?: if (sourceId == null) CustomCurveDocument(draftId, getString(R.string.profiles_default_name), 93, 3600,
            CustomCurveDocument.ControlMode.PRESSURE, listOf(CustomCurveDocument.Stage(30, 150), CustomCurveDocument.Stage(90, 400)))
        else runCatching {
            store.find(sourceId) ?: (application as MobileApplication).curves.find(sourceId)?.let { CustomCurveDocument.fromLibraryItem(it, draftId) }
        }.getOrNull()
        if (source == null) { HoyiUi.label(this, body, getString(R.string.profiles_missing), 17); return }
        sourceDocument = source
        draftId = source.id
        if (source.controlMode == CustomCurveDocument.ControlMode.FLOW_RAW) {
            style.visual.add(body, style.visual.text(getString(R.string.curve_copy_warning), TaskProductVisualStyle.auxiliary, secondary = true))
            profileCard(body, source)
            val description = beanInput(body, R.string.curve_description, false).apply {
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                setSingleLine(false); minLines = 2; maxLines = 4
                setText(savedInstanceState?.getString("description") ?: source.description)
                filters = arrayOf(android.text.InputFilter.LengthFilter(8000))
            }
            fields["description"] = description
            val error = beanError(body)
            var saving = false
            HoyiUi.button(this, body, getString(R.string.curve_editor_save_copy)) {
                if (saving || isFinishing) return@button
                saving = true
                if (runCatching { store.saveCopy(source.copy(description = description.text.toString())) }.isSuccess) finish()
                else { error.setText(R.string.profiles_save_failed); saving = false }
            }
            return
        }
        if (sourceId != null) style.visual.add(body,
            style.visual.text(getString(R.string.curve_copy_warning), TaskProductVisualStyle.auxiliary, secondary = true))
        stageCount = savedInstanceState?.getInt("stageCount") ?: source.stages.size
        mode = savedInstanceState?.getString("controlMode")?.let { CustomCurveDocument.ControlMode.valueOf(it) } ?: source.controlMode
        pressureLogic = savedInstanceState?.getBoolean("pressureLogic") ?: (source.controlMode == CustomCurveDocument.ControlMode.PRESSURE && source.pressureLogic)
        flowLogic = savedInstanceState?.getBoolean("flowLogic") ?: (source.controlMode == CustomCurveDocument.ControlMode.FLOW && source.pressureLogic)
        pressureBeforeFlow = savedInstanceState?.getBoolean("pressureBeforeFlow") ?: false
        firstFlow = savedInstanceState?.getBoolean("firstFlow") ?: source.firstSegmentFlowMode
        for (index in 0..3) for (flow in listOf(false, true)) {
            val key = "$index-$flow"
            val unused = index >= source.stages.size && source.allStages[index] == CustomCurveDocument.Stage(0, 0)
            val initial = if (unused) "" else if (source.isFlowStage(index) == flow)
                CurveDraftNumber.format(source.allStages[index].target, 1) else if (flow) "1" else "6"
            targetInputs[key] = savedInstanceState?.getString("bank-$key") ?: initial
        }
        val sections = TaskCurveEditorForm(this, body, style)
        form = sections
        fun field(parent: LinearLayout, key: String, label: Int, initial: String, numeric: Boolean = true,
            compactLabel: String? = null, tableCell: Boolean = false, inlineTitle: Boolean = false): EditText {
            val input = if (inlineTitle) EditText(this).apply {
                id = View.generateViewId()
                inputType = android.text.InputType.TYPE_CLASS_TEXT
                parent.addView(this, LinearLayout.LayoutParams(-1, -2))
            } else if (tableCell) EditText(this).apply {
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
            input.minimumHeight = style.dp(48); input.minHeight = style.dp(48)
            input.contentDescription = getString(label)
            input.tooltipText = getString(label)
            fields[key] = input
            input.setText(savedInstanceState?.getString(key) ?: initial)
            return input
        }
        val name = field(sections.metadata[0], "name", R.string.profiles_name, source.name, false,
            compactLabel = getString(R.string.profiles_name), inlineTitle = true)
        // A borderless, directly editable title. Long names scroll inside the title, never over navigation.
        name.apply {
            background = null
            backgroundTintList = null
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 24f)
            typeface = TaskTypography.face(this@CurveEditorActivity, 700)
            if (android.os.Build.VERSION.SDK_INT >= 28) setLineHeight(AppTextUnits.pixels(resources, 32f).toInt())
            setPaddingRelative(0, style.dp(8), style.dp(8), style.dp(8))
            minimumHeight = style.dp(48); minHeight = style.dp(48)
            setSingleLine(true)
            setHorizontallyScrolling(true)
            hint = getString(R.string.profiles_name)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            setSelectAllOnFocus(true)
        }
        val temperature = field(sections.metadata[1], "temperature", R.string.profiles_temperature, source.temperatureC.toString(),
            compactLabel = getString(R.string.curve_editor_temperature_short))
        val cup = field(sections.metadata[2], "cup", R.string.profiles_cup, CurveDraftNumber.format(source.targetHundredthsGram, 2),
            compactLabel = getString(R.string.curve_preview_weight))
        temperature.hint = "75–105"
        cup.hint = "0–200"
        temperature.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        fun validateMetadata(input: EditText, label: Int, parse: (String) -> Int): Boolean {
            val valid = runCatching { parse(input.text.toString()) }.isSuccess
            input.error = if (valid) null else getString(label)
            return valid
        }
        temperature.setOnFocusChangeListener { _, focused ->
            if (!focused) validateMetadata(temperature, R.string.profiles_temperature, CurveMetadataInput::temperature)
        }
        cup.setOnFocusChangeListener { _, focused ->
            if (!focused) validateMetadata(cup, R.string.profiles_cup, CurveMetadataInput::targetWeight)
        }
        style.unit(temperature, "°C", "curve-editor", "temperature")
        style.unit(cup, "g", "curve-editor", "cup")
        fun numericRow(container: LinearLayout, label: Int, tag: String? = null): LinearLayout = object : LinearLayout(this@CurveEditorActivity) {
                init {
                    isBaselineAligned = false
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    addView(style.visual.text(getString(label), TaskProductVisualStyle.auxiliary, tag, true), LayoutParams(-2, -2))
                    container.addView(this, LayoutParams(-1, -2).apply { topMargin = style.dp(4) })
                }
                override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                    val title = getChildAt(0)
                    val inputHost = if (childCount > 1) getChildAt(1) else null
                    fun input(view: View?): EditText? = when (view) {
                        is EditText -> view
                        is android.view.ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { input(view.getChildAt(it)) }
                        else -> null
                    }
                    val editor = input(inputHost)
                    val free = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    title.measure(free, free)
                    // Measuring the actual unit wrapper updates the real compound padding for bar/mL/s.
                    inputHost?.measure(free, free)
                    val digits = editor?.let {
                        kotlin.math.ceil(android.text.Layout.getDesiredWidth("6553.5", it.paint).toDouble()).toInt() +
                            it.compoundPaddingLeft + it.compoundPaddingRight + style.dp(4)
                    } ?: 0
                    val available = View.MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
                    val stacked = title.measuredWidth + style.dp(8) + digits > available
                    orientation = if (stacked) VERTICAL else HORIZONTAL
                    (title.layoutParams as LayoutParams).apply {
                        width = if (stacked) -1 else -2
                        bottomMargin = if (stacked) style.dp(6) else 0
                    }
                    (inputHost?.layoutParams as? LayoutParams)?.apply {
                        width = if (stacked) -1 else 0
                        weight = if (stacked) 0f else 1f
                        marginStart = if (stacked) 0 else style.dp(8)
                        resolveLayoutDirection(this@CurveEditorActivity.resources.configuration.layoutDirection)
                    }
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                }
            }
        val fieldErrors = linkedMapOf<String, android.widget.TextView>()
        fun attachError(key: String, editor: EditText, container: LinearLayout) {
            val label = style.visual.text("", TaskProductVisualStyle.auxiliary).apply {
                tag = "curve-editor-error-$key"; visibility = View.GONE
                setTextColor(getColor(R.color.mobile_danger))
                accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            }
            var holder: View = editor
            while (holder.parent !== container) holder = holder.parent as View
            container.addView(label, container.indexOfChild(holder) + 1, LinearLayout.LayoutParams(-1, -2))
            fieldErrors[key] = label
        }
        (0..3).forEach { index ->
            val stage = source.allStages.getOrNull(index) ?: CustomCurveDocument.Stage(60, 100)
            val flow = mode == CustomCurveDocument.ControlMode.FLOW && (index > 0 || firstFlow)
            val row = sections.stageRows[index]
            val target = field(numericRow(row, R.string.curve_editor_target_pressure, "curve-stage-target-label-$index"), "target-$index", R.string.curve_target, targetInputs.getValue("$index-$flow"), tableCell = true)
            val unused = index >= source.stages.size && stage == CustomCurveDocument.Stage(0, 0)
            val water = field(numericRow(row, R.string.curve_editor_stage_water, "curve-stage-water-label-$index"), "water-$index", R.string.profiles_water,
                if (unused) "" else CurveDraftNumber.format(stage.waterTenthsMl, 1), tableCell = true)
            style.unit(target, if (flow) "mL/s" else "bar", "curve-editor", "target-$index")
            style.unit(water, "mL", "curve-editor", "water-$index")
            attachError("target-$index", target, row)
            attachError("water-$index", water, row)
            if (index > 0) row.addView(sections.sharedLogicHosts[index], LinearLayout.LayoutParams(-1, -2))
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
        val preinfusion = field(numericRow(sections.configurationFields, R.string.curve_editor_bloom_short), "preinfusion", R.string.curve_preinfusion, source.preinfusionSeconds.toString(), tableCell = true)
        val firstDuration = field(numericRow(sections.configurationFields, R.string.curve_editor_duration_short), "first-duration", R.string.curve_first_duration, source.firstDurationSeconds.toString(), tableCell = true)
        val compensation = field(numericRow(sections.configurationFields, R.string.curve_editor_compensation_short),
            "compensation", R.string.flow_editor_compensation, CurveDraftNumber.format(source.compensationHundredthsGram, 2), tableCell = true).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
        }
        style.unit(preinfusion, "s", "curve-editor", "preinfusion")
        style.unit(firstDuration, "s", "curve-editor", "first-duration")
        style.unit(compensation, "g", "curve-editor", "compensation")
        val totalLimit = field(numericRow(sections.waterLimit, R.string.profiles_total_water_limit_title), "total-water-limit", R.string.profiles_total_water_limit,
            source.totalWaterLimitMl?.toString().orEmpty(), tableCell = true).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = getString(R.string.profiles_total_water_limit_hint)
        }
        style.unit(totalLimit, "mL", "curve-editor", "total-water-limit")
        attachError("total-water-limit", totalLimit, sections.waterLimit)
        attachError("preinfusion", preinfusion, sections.configurationFields)
        attachError("first-duration", firstDuration, sections.configurationFields)
        attachError("compensation", compensation, sections.configurationFields)
        val assignedWater = style.visual.text("", TaskProductVisualStyle.auxiliary,
            "product/form/curve-editor/assigned-water", true)
        style.visual.add(sections.waterLimit, assignedWater, 8)
        style.visual.add(sections.waterLimit, style.visual.text(getString(R.string.profiles_total_water_limit_scale),
            TaskProductVisualStyle.auxiliary, secondary = true), 8)
        fun isFlowMode() = mode == CustomCurveDocument.ControlMode.FLOW
        fun activeLogic() = if (isFlowMode()) flowLogic else pressureLogic
        fun flowStage(index: Int) = isFlowMode() && (index > 0 || firstFlow)
        fun rememberTargets() { for (index in 0..3) targetInputs["$index-${flowStage(index)}"] = fields.getValue("target-$index").text.toString() }
        fun restoreTargets() { for (index in 0..3) fields.getValue("target-$index").setText(targetInputs.getValue("$index-${flowStage(index)}")) }
        val modeHint = style.visual.text("", TaskProductVisualStyle.auxiliary).apply {
            tag = "curve-editor-mode-hint"; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        sections.stageHeader.addView(modeHint, LinearLayout.LayoutParams(-1, -2))
        fun updateModeHint() {
            modeHint.visibility = if (stageCount == 1 && (isFlowMode() || pressureLogic)) View.VISIBLE else View.GONE
            modeHint.setText(R.string.flow_editor_complete_stage_error)
            modeHint.setTextColor(getColor(R.color.mobile_danger))
        }
        var previewChanged: () -> Unit = {}
        lateinit var firstModePicker: CurveEditorSegments
        val controlSwitches = mutableListOf<CurveEditorSegments>()
        val logicSwitches = mutableListOf<CurveEditorSegments>()
        fun units() {
            (0..3).forEach { index ->
                val flow = isFlowMode() && (index > 0 || firstFlow)
                sections.setStageType(index, flow)
                sections.stageModes[index].text = getString(when {
                    isFlowMode() && index == 0 && !firstFlow -> R.string.curve_editor_mode_first_pressure
                    flow && activeLogic() -> R.string.curve_editor_mode_flow_fast
                    flow -> R.string.curve_editor_mode_flow_slow
                    activeLogic() -> R.string.curve_editor_mode_pressure_follow
                    else -> R.string.curve_editor_mode_pressure_constant
                })
                sections.stageRows[index].findViewWithTag<android.widget.TextView>("curve-stage-target-label-$index").setText(
                    if (flow) R.string.curve_editor_target_flow else R.string.curve_editor_target_pressure)
                val target = fields.getValue("target-$index")
                (target.parent as? android.view.ViewGroup)?.findViewWithTag<android.widget.TextView>("product/form/curve-editor/target-$index/unit")?.text = if (flow) "mL/s" else "bar"
                target.contentDescription = getString(when {
                    flow -> R.string.flow_editor_flow_target
                    isFlowMode() -> R.string.flow_editor_first_pressure_error
                    pressureLogic && index == 0 -> R.string.flow_editor_complete_first_pressure
                    pressureLogic -> R.string.flow_editor_complete_next_pressure
                    else -> R.string.profiles_pressure
                })
                target.tooltipText = target.contentDescription
            }
            firstModePicker.isEnabled = isFlowMode()
            firstModePicker.visibility = if (isFlowMode()) View.VISIBLE else View.GONE
            sections.firstModeHost.visibility = View.VISIBLE
            sections.stageModes[0].visibility = if (isFlowMode()) View.GONE else View.VISIBLE
            firstModePicker.contentDescription = getString(R.string.flow_editor_first) + " · " + sections.stageModes[0].text
            // First-stage type is independent; stages 2–4 share the control and logic bits.
            firstModePicker.labels(getString(R.string.curve_editor_mode_first_pressure), getString(R.string.curve_editor_first_flow))
            firstModePicker.select(firstFlow)
            controlSwitches.forEach { picker ->
                picker.colors(colors.pressureFill, colors.flowFill)
                picker.select(isFlowMode())
            }
            firstModePicker.colors(colors.pressureFill, colors.flowFill)
            logicSwitches.forEach { picker ->
                picker.colors(colors.fill(isFlowMode()), colors.fill(isFlowMode()))
                picker.labels(getString(if (isFlowMode()) R.string.curve_control_slow else R.string.curve_control_constant),
                    getString(if (isFlowMode()) R.string.curve_control_fast else R.string.curve_control_follow))
                picker.select(activeLogic())
            }
            updateModeHint()
            previewChanged()
        }
        (1..3).forEach { index ->
            controlSwitches += CurveEditorSegments(this, style.visual,
                getString(R.string.curve_control_pressure), getString(R.string.curve_control_flow)).apply {
                tag = if (index == 1) "product/form/curve-editor/control-mode" else "product/form/curve-editor/control-mode-$index"
                compact()
                contentDescription = getString(R.string.curve_editor_shared_mode)
                sections.sharedModeHosts[index].addView(this, LinearLayout.LayoutParams(-1, -2))
                onSelected = { checked ->
                    val next = CurveModeSelection(isFlowMode(), activeLogic(), pressureBeforeFlow).selectFlow(checked)
                    rememberTargets()
                    mode = if (next.flow) CustomCurveDocument.ControlMode.FLOW else CustomCurveDocument.ControlMode.PRESSURE
                    if (next.flow) flowLogic = next.pressure else pressureLogic = next.pressure
                    pressureBeforeFlow = next.pressureBeforeFlow
                    restoreTargets(); units()
                }
            }
        }
        firstModePicker = CurveEditorSegments(this, style.visual,
            getString(R.string.curve_editor_mode_first_pressure), getString(R.string.curve_editor_first_flow)).apply {
            tag = "product/form/curve-editor/first-mode"
            compact()
            select(firstFlow)
            sections.firstModeHost.addView(this, LinearLayout.LayoutParams(-1, -2))
            onSelected = { rememberTargets(); firstFlow = it; restoreTargets(); units() }
        }
        (1..3).forEach { index ->
            logicSwitches += CurveEditorSegments(this, style.visual, "", "").apply {
                tag = if (index == 1) "product/form/curve-editor/pressure-logic" else "product/form/curve-editor/pressure-logic-$index"
                compact()
                contentDescription = getString(R.string.curve_editor_shared_mode)
                sections.sharedLogicHosts[index].addView(this, LinearLayout.LayoutParams(-1, -2))
                onSelected = { if (isFlowMode()) flowLogic = it else pressureLogic = it; units() }
            }
        }
        fun visibility() {
            sections.setStageCount(stageCount)
            updateModeHint()
            previewChanged()
        }
        sections.onStageCountChanged { count ->
            stageCount = count
            visibility()
        }
        visibility()
        val error = style.error(beanError(body)).apply { visibility = View.GONE }
        error.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                error.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })
        fun parsed(): CustomCurveDocument {
            val targetTemperature = try { CurveMetadataInput.temperature(temperature.text.toString()) }
                catch (_: IllegalArgumentException) { throw FieldFailure("temperature", R.string.profiles_temperature) }
            val targetCup = try { CurveMetadataInput.targetWeight(cup.text.toString()) }
                catch (_: IllegalArgumentException) { throw FieldFailure("cup", R.string.profiles_cup) }
            val compensationText = compensation.text.toString().trim()
            val compensationValue = try {
                if (compensationText.startsWith("-")) -CurveDraftNumber.parse(compensationText.drop(1), 2)
                else CurveDraftNumber.parse(compensationText, 2)
            } catch (_: IllegalArgumentException) { throw FieldFailure("compensation", R.string.flow_editor_compensation_error) }
            if (compensationValue !in -10000..10000) throw FieldFailure("compensation", R.string.flow_editor_compensation_error)
            fun number(key: String, decimals: Int, label: Int): Int = try {
                CurveDraftNumber.parse(fields.getValue(key).text.toString(), decimals)
            } catch (_: IllegalArgumentException) { throw FieldFailure(key, label) }
            val stages = (0 until stageCount).map {
                CustomCurveDocument.Stage(number("target-$it", 1, R.string.curve_target),
                    number("water-$it", 1, R.string.profiles_water))
            }
            stages.forEachIndexed { index, stage ->
                val targetRange = if (flowStage(index)) 10..100 else if (mode == CustomCurveDocument.ControlMode.FLOW) 5..100 else 0..120
                if (stage.target !in targetRange) throw FieldFailure("target-$index", when {
                    flowStage(index) -> R.string.flow_editor_flow_error
                    mode == CustomCurveDocument.ControlMode.FLOW -> R.string.flow_editor_first_pressure_error
                    else -> R.string.flow_editor_pressure_error
                })
                if (stage.waterTenthsMl !in (if (mode == CustomCurveDocument.ControlMode.FLOW) 0 else 1)..65535)
                    throw FieldFailure("water-$index", R.string.profiles_water)
            }
            val bloomSeconds = number("preinfusion", 0, R.string.curve_preinfusion)
            val durationSeconds = number("first-duration", 0, R.string.curve_first_duration)
            if (bloomSeconds !in 0..127) throw FieldFailure("preinfusion", R.string.flow_editor_bloom_error)
            // 255 remains readable in historical FLOW drafts; the adapter decides executable capability.
            if (durationSeconds !in 0..255) throw FieldFailure("first-duration", R.string.flow_editor_time_error)
            val maximum = maxOf(((stages.sumOf { it.waterTenthsMl.toLong() } + 9) / 10).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), 800)
            val limit = totalLimit.text.toString().trim().takeIf { it.isNotEmpty() }?.let { text ->
                try { CurveDraftNumber.parse(text, 0) }
                catch (_: IllegalArgumentException) { throw CurveWaterLimitException(maximum) }
            }
            return CustomCurveDocument(draftId, name.text.toString().trim(),
                targetTemperature, targetCup,
                mode, stages, limit, if (mode == CustomCurveDocument.ControlMode.FLOW) flowLogic else pressureLogic,
                mode == CustomCurveDocument.ControlMode.FLOW && firstFlow,
                bloomSeconds, durationSeconds,
                description.text.toString(), compensationValue,
                (stageCount until 4).map {
                    val targetText = fields.getValue("target-$it").text.toString()
                    val waterText = fields.getValue("water-$it").text.toString()
                    // Untouched, unused cards have no protocol data. Other hidden raw input
                    // must be valid before saving; never silently replace an edit with the source.
                    if (targetText.isBlank() && waterText.isBlank() && source.allStages[it] == CustomCurveDocument.Stage(0, 0)) {
                        CustomCurveDocument.Stage(0, 0)
                    } else {
                        val target = number("target-$it", 1, R.string.curve_target)
                        val water = number("water-$it", 1, R.string.profiles_water)
                        if (target !in 0..255) throw FieldFailure("target-$it", R.string.curve_target)
                        if (water !in 0..65535) throw FieldFailure("water-$it", R.string.profiles_water)
                        CustomCurveDocument.Stage(target, water)
                    }
                }.withoutEmptyTail()).also {
                    it.validate()
                    if (CustomCurveAdapter.profile(it, false) == null && CustomCurveFingerprint.of(it) != CustomCurveFingerprint.of(source)) {
                        if (mode == CustomCurveDocument.ControlMode.FLOW && stages.first().waterTenthsMl == 0 && durationSeconds == 0)
                            throw FieldFailure("first-duration", R.string.flow_editor_exit_error)
                        if ((mode == CustomCurveDocument.ControlMode.FLOW || pressureLogic) && stageCount < 2)
                            throw FieldFailure("target-0", R.string.flow_editor_complete_stage_error)
                        if ((mode == CustomCurveDocument.ControlMode.FLOW || pressureLogic) && durationSeconds > 254)
                            throw FieldFailure("first-duration", R.string.flow_editor_time_error)
                        stages.forEachIndexed { index, stage ->
                            if (mode == CustomCurveDocument.ControlMode.PRESSURE && pressureLogic &&
                                stage.target !in (if (index == 0) 5..120 else 10..120))
                                throw FieldFailure("target-$index", if (index == 0) R.string.flow_editor_complete_first_pressure else R.string.flow_editor_complete_next_pressure)
                            if (stage.waterTenthsMl % 10 != 0 || (stage.waterTenthsMl == 0 && (mode != CustomCurveDocument.ControlMode.FLOW || index > 0)))
                                throw FieldFailure("water-$index", R.string.flow_editor_water_positive_error)
                            if ((mode == CustomCurveDocument.ControlMode.FLOW || pressureLogic) && stage.waterTenthsMl > 9990)
                                throw FieldFailure("water-$index", R.string.flow_editor_complete_water_error)
                        }
                        throw IllegalArgumentException("Curve is outside executable precision or range")
                    }
                }
        }
        fun showError(failure: Throwable) {
            error.text = if (failure is FieldFailure) {
                val stageIndex = failure.key.substringAfter('-', "").toIntOrNull()
                    ?.takeIf { failure.key.startsWith("target-") || failure.key.startsWith("water-") }
                val message = if (stageIndex != null) getString(R.string.profiles_stage, stageIndex + 1) + " · " + getString(failure.label)
                    else getString(failure.label)
                fields[failure.key]?.apply {
                    setError(message)
                    if (isShown) { requestFocus(); requestRectangleOnScreen(android.graphics.Rect(0, 0, width, height), true) }
                }
                message
            } else if (failure is CurveWaterLimitException) {
                val message = getString(R.string.profiles_total_water_limit_invalid, 1, failure.maximumMl)
                totalLimit.apply { setError(message); requestFocus(); requestRectangleOnScreen(android.graphics.Rect(0, 0, width, height), true) }
                message
            } else getString(R.string.curve_modes_invalid)
        }
        val preview = CurveEditorPreviewView(this, style.visual).apply { tag = "task-form-stage-preview" }
        style.visual.add(sections.supportingBody, style.visual.text(getString(R.string.profiles_preview), TaskProductVisualStyle.section))
        val previewScroll = android.widget.HorizontalScrollView(this).apply {
            isFillViewport = true
            addView(preview, android.widget.FrameLayout.LayoutParams(-1, -1))
        }
        sections.supportingBody.addView(previewScroll, LinearLayout.LayoutParams(-1, HoyiUi.dp(this, 200)))
        val previewStatus = style.visual.text("", TaskProductVisualStyle.auxiliary).apply {
            style.visual.add(sections.supportingBody, this)
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        val executionNotice = style.visual.text("", TaskProductVisualStyle.auxiliary, secondary = true)
        style.visual.add(sections.supportingBody, executionNotice, 8)
        previewChanged = {
            updateModeHint()
            assignedWater.text = runCatching {
                val tenths = (0 until stageCount).sumOf {
                    CurveDraftNumber.parse(fields.getValue("water-$it").text.toString(), 1).also { require(it in 0..65535) }
                }
                getString(R.string.profiles_assigned_water, CurveDraftNumber.format(tenths, 1))
            }.getOrElse { getString(R.string.profiles_assigned_water_invalid) }
            val draftPreview = CurveEditorPreview.parse((0 until stageCount).map {
                fields.getValue("target-$it").text.toString() to fields.getValue("water-$it").text.toString()
            }, isFlowMode(), firstFlow, isFlowMode() || pressureLogic)
            draftPreview.stages.forEachIndexed { index, stage ->
                listOf("target-$index" to stage.targetError, "water-$index" to stage.waterError).forEach { (key, reason) ->
                    val label = fieldErrors.getValue(key)
                    label.visibility = if (reason == null) View.GONE else View.VISIBLE
                    label.text = when (reason) {
                        CurveEditorPreview.Error.REQUIRED -> getString(R.string.curve_editor_value_required)
                        CurveEditorPreview.Error.NUMBER -> getString(R.string.curve_editor_value_number)
                        CurveEditorPreview.Error.RANGE -> if (key.startsWith("target")) getString(R.string.curve_editor_target_range,
                            CurveDraftNumber.format(stage.targetRange.first, 1), CurveDraftNumber.format(stage.targetRange.last, 1),
                            if (stage.flow) "mL/s" else "bar")
                            else getString(R.string.curve_editor_water_range, CurveDraftNumber.format(stage.waterRange.first, 1),
                                CurveDraftNumber.format(stage.waterRange.last, 1))
                        null -> ""
                    }
                }
            }
            val settingFailures = CurveEditorSettings.errors(
                preinfusion.text.toString(), firstDuration.text.toString(), compensation.text.toString(), totalLimit.text.toString(),
                draftPreview.totalWater, isFlowMode(), draftPreview.stages.firstOrNull()?.water,
                cup.text.toString(), maximumDuration = if (!isFlowMode() && !pressureLogic ||
                    source.firstDurationSeconds == 255 && firstDuration.text.toString().trim() == "255") 255 else 254,
                enforceCompensationTarget = false)
            settingFailures.forEach { (key, reason) ->
                val message = when (reason) {
                    CurveEditorSettings.Error.BLOOM -> getString(R.string.flow_editor_bloom_error)
                    CurveEditorSettings.Error.DURATION -> getString(R.string.flow_editor_time_error)
                    CurveEditorSettings.Error.EXIT -> getString(R.string.flow_editor_exit_error)
                    CurveEditorSettings.Error.COMPENSATION -> getString(R.string.flow_editor_compensation_error)
                    CurveEditorSettings.Error.COMPENSATION_TARGET -> getString(R.string.flow_editor_compensation_target_error)
                    CurveEditorSettings.Error.TOTAL -> getString(R.string.profiles_total_water_limit_invalid, 1,
                        maxOf(((draftPreview.totalWater ?: 0) + 9) / 10, 800))
                    null -> ""
                }
                fieldErrors.getValue(key).apply { text = message; visibility = if (reason == null) View.GONE else View.VISIBLE }
            }
            fieldErrors.forEach { (key, label) -> if (label.visibility == View.GONE) fields[key]?.error = null }
            preview.model = draftPreview
            previewStatus.visibility = if (draftPreview.invalid) View.VISIBLE else View.GONE
            previewStatus.setText(R.string.curve_editor_preview_incomplete)
            executionNotice.text = runCatching { profileExecutionSummary(parsed()) }.getOrElse { getString(R.string.curve_modes_execution) }
            Unit
        }
        fields.values.forEach { field ->
            field.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (field.error != null) when (field) {
                        temperature -> validateMetadata(field, R.string.profiles_temperature, CurveMetadataInput::temperature)
                        cup -> validateMetadata(field, R.string.profiles_cup, CurveMetadataInput::targetWeight)
                    }
                    error.text = ""
                    previewChanged()
                }
                override fun afterTextChanged(s: android.text.Editable?) = Unit
            })
        }
        previewChanged()
        units()
        sections.restore(savedInstanceState, fields)
        val actionRow = object : LinearLayout(this) {
            init { isBaselineAligned = false; gravity = android.view.Gravity.END }
            override fun onMeasure(w: Int, h: Int) {
                val free = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                for (index in 0 until childCount) getChildAt(index).measure(free, free)
                val stacked = (0 until childCount).sumOf { getChildAt(it).measuredWidth } + style.dp(8) > View.MeasureSpec.getSize(w)
                orientation = if (stacked) VERTICAL else HORIZONTAL
                for (index in 0 until childCount) (getChildAt(index).layoutParams as LayoutParams).apply {
                    width = if (stacked) -1 else -2
                    marginEnd = if (!stacked && index == 0) style.dp(8) else 0
                }
                super.onMeasure(w, h)
            }
        }
        style.visual.action(actionRow, getString(R.string.beans_cancel), "product/form/curve-editor/cancel", top = 0) { finish() }
            .layoutParams = LinearLayout.LayoutParams(-2, -2).apply { marginEnd = style.dp(8) }
        val save = style.visual.action(actionRow, getString(if (sourceId == null) R.string.curve_editor_create else R.string.curve_editor_save_copy),
            "product/form/curve-editor/save", primary = true, top = 0) {}
        save.layoutParams = LinearLayout.LayoutParams(-2, -2)
        sections.actions.addView(actionRow, LinearLayout.LayoutParams(-1, -2))
        save.setOnClickListener {
            if (!save.isEnabled || isFinishing) return@setOnClickListener
            val doc = runCatching { parsed() }.getOrElse { showError(it); return@setOnClickListener }
            val available = (application as MobileApplication).customCurvesResult.getOrNull()
            if (available == null) { error.setText(R.string.profiles_unavailable); save.isEnabled = false; return@setOnClickListener }
            save.isEnabled = false
            if (runCatching { if (sourceId == null) available.save(doc) else available.saveCopy(doc) }.isSuccess) finish()
            else { error.setText(R.string.profiles_save_failed); save.isEnabled = true }
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("draftId", draftId); outState.putInt("stageCount", stageCount)
        sourceDocument?.let { outState.putString("sourceDocument", it.encode()) }
        outState.putBoolean("pressureBeforeFlow", pressureBeforeFlow)
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
