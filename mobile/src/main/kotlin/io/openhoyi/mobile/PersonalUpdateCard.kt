package io.openhoyi.mobile

import android.app.Activity
import android.widget.Button
import android.widget.LinearLayout

/** Opening GitHub delegates private repository authentication and APK download to the browser. */
internal class PersonalUpdateCard(activity: Activity, parent: LinearLayout, visual: SettingsVisualStyle) {
    init {
        val style = visual.palette
        val card = style.column("product/settings/app/update/personal-card")
        style.add(card, style.text(activity.getString(R.string.application_version,
            BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
            TaskProductVisualStyle.body, "product/settings/app/update/version"))
        style.add(card, style.text(activity.getString(R.string.personal_update_title),
            TaskProductVisualStyle.body, "product/settings/app/update/personal-title"), 8)
        style.add(card, style.text(activity.getString(R.string.personal_update_body),
            TaskProductVisualStyle.auxiliary, "product/settings/app/update/personal-body", true), 8)
        style.add(card, style.button(Button(activity)).apply {
            text = activity.getString(R.string.personal_update_open)
            tag = "product/settings/app/update/personal-open"
            setOnClickListener { PersonalUpdateChannel.openDownloadPage(activity) }
        }, 8)
        style.add(parent, card, 12)
    }
    fun resume() = Unit
    fun pause() = Unit
    fun close() = Unit
}
