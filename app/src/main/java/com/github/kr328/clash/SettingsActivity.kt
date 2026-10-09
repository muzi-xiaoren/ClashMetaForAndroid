package com.github.kr328.clash

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import androidx.core.content.getSystemService
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.SettingsDesign
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.update.UpdateChecker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext

class SettingsActivity : BaseActivity<SettingsDesign>() {
    override suspend fun main() {
        val design = SettingsDesign(this)

        setContentDesign(design)

        while (isActive) {
            select<Unit> {
                events.onReceive {

                }
                design.requests.onReceive {
                    when (it) {
                        SettingsDesign.Request.StartApp ->
                            startActivity(AppSettingsActivity::class.intent)
                        SettingsDesign.Request.StartNetwork ->
                            startActivity(NetworkSettingsActivity::class.intent)
                        SettingsDesign.Request.StartOverride ->
                            startActivity(OverrideSettingsActivity::class.intent)
                        SettingsDesign.Request.StartMetaFeature ->
                            startActivity(MetaFeatureSettingsActivity::class.intent)
                        SettingsDesign.Request.StartWebDav ->
                            startActivity(WebDavSettingsActivity::class.intent)
                        SettingsDesign.Request.CheckUpdate ->
                            checkUpdate(design)
                        SettingsDesign.Request.OpenRepository ->
                            openRepository(design)
                    }
                }
            }
        }
    }

    private suspend fun checkUpdate(design: SettingsDesign) {
        design.showToast(R.string.update_checking, ToastDuration.Short)

        val release = try {
            withContext(Dispatchers.IO) { UpdateChecker.latest() }
        } catch (e: Exception) {
            design.showToast(
                getString(R.string.update_check_failed, e.message ?: e.javaClass.simpleName),
                ToastDuration.Long,
            )
            return
        }

        val current = UpdateChecker.currentTag.ifEmpty {
            getString(R.string.update_local_build, "v${BuildConfig.VERSION_NAME}")
        }

        if (!UpdateChecker.isNewer(release)) {
            design.showToast(getString(R.string.update_latest, current), ToastDuration.Long)
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.update_available)
            .setMessage(getString(R.string.update_available_message, current, release.name, release.notes))
            .setPositiveButton(R.string.update_download) { _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.pageUrl)))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun openRepository(design: SettingsDesign) {
        val url = getString(R.string.project_repository_url)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.project_repository)
            .setMessage(url)
            .setPositiveButton(R.string.project_repository_open) { _, _ ->
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: ActivityNotFoundException) {
                    copyRepository(design, url)
                }
            }
            .setNeutralButton(R.string.age_key_copy) { _, _ ->
                copyRepository(design, url)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun copyRepository(design: SettingsDesign, url: String) {
        getSystemService<ClipboardManager>()
            ?.setPrimaryClip(ClipData.newPlainText(getString(R.string.project_repository), url))

        launch { design.showToast(R.string.copied, ToastDuration.Short) }
    }
}
