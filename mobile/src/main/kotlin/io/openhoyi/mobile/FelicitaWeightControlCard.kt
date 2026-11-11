package io.openhoyi.mobile

import android.app.AlertDialog
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import io.openhoyi.session.DeviceState
import io.openhoyi.session.FelicitaArtScaleProtocolAdapter
import io.openhoyi.session.FelicitaReadOnlyScaleProtocolAdapter
import io.openhoyi.session.FelicitaScaleProtocolAdapter

/** An explicit, device-scoped selection. Confirmation never writes a machine command. */
internal class FelicitaWeightControlCard(private val activity: ThemedActivity, parent: LinearLayout,
    visual: SettingsVisualStyle, private val service: () -> MobileService?) {
    private val style = visual.palette
    private val body = settingsGroup(activity, parent, activity.getString(R.string.felicita_control_title), visual)
        .apply { tag = "product/settings/scale/felicita-art" }
    private val status = style.text(activity.getString(R.string.felicita_control_hint),
        TaskProductVisualStyle.auxiliary, secondary = true).also { style.add(body, it, 8) }
    private val button = HoyiUi.button(activity, body, activity.getString(R.string.felicita_control_enable)) {
        confirm()
    }.also { visual.action(it, outline = true) }
    private var confirmation: AlertDialog? = null

    fun render() {
        val owner = service()
        val id = owner?.scaleProtocolId
        val known = id in setOf(FelicitaScaleProtocolAdapter.id, FelicitaReadOnlyScaleProtocolAdapter.id,
            FelicitaArtScaleProtocolAdapter.id)
        body.visibility = if (!BuildConfig.MOCK_MODE && known) View.VISIBLE else View.GONE
        val enabled = id == FelicitaArtScaleProtocolAdapter.id
        button.setText(if (enabled) R.string.felicita_control_enabled else R.string.felicita_control_enable)
        button.isEnabled = !enabled && owner?.felicitaWeightControlContext() != null
        status.setText(when {
            enabled && owner?.snapshot?.scaleState == DeviceState.READY -> R.string.felicita_control_active_hint
            enabled && owner?.snapshot?.scaleState in setOf(DeviceState.CONNECTING, DeviceState.DISCOVERING,
                DeviceState.SUBSCRIBING, DeviceState.INITIALIZING, DeviceState.SYNCHRONIZING) -> R.string.felicita_control_reconnecting
            enabled -> R.string.felicita_control_state_changed
            else -> R.string.felicita_control_hint
        })
    }

    private fun confirm() {
        if (confirmation?.isShowing == true) return
        val owner = service() ?: return
        val expected = owner.felicitaWeightControlContext() ?: return
        val dialog = AlertDialog.Builder(activity).setTitle(R.string.felicita_control_confirm_title)
            .setMessage(R.string.felicita_control_confirm)
            .setPositiveButton(R.string.felicita_control_confirm_enable) { _, _ ->
                val result = if (service() === owner) owner.enableFelicitaWeightControl(expected)
                    else activity.getString(R.string.felicita_control_state_changed)
                Toast.makeText(activity, result, Toast.LENGTH_LONG).show()
                render()
            }.setNegativeButton(R.string.machine_settings_cancel, null).create()
        confirmation = dialog
        dialog.setOnDismissListener { if (confirmation === dialog) confirmation = null }
        AppDialog.show(dialog)
    }

    fun close() { confirmation?.dismiss(); confirmation = null }
}
