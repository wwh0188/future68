package io.openhoyi.mobile

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri

/** Personal APKs have their own package/signing identity and a private, authenticated download page. */
internal object PersonalUpdateChannel {
    const val DOWNLOADS_URL = "https://github.com/wwh0188/future68/tree/feature/control-modes-curve-notes/downloads"
    fun isPersonal(packageName: String) = packageName == "io.openhoyi.mobile.personal" ||
        packageName.startsWith("io.openhoyi.mobile.personal.")
    fun officialUpdatesAllowed(packageName: String) = !isPersonal(packageName)
    fun openDownloadPage(activity: Activity): Boolean = try {
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DOWNLOADS_URL))
            .addCategory(Intent.CATEGORY_BROWSABLE))
        true
    } catch (_: ActivityNotFoundException) { false }
}
