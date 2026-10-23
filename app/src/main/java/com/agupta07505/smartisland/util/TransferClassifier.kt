/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.TransferKind

/**
 * Decides what kind of transfer a progress notification is, and holds the per-app workarounds for apps whose
 * notifications do not look like a normal download.
 */
object TransferClassifier {

    /** Words that mean "a transfer is running right now". Matched against title + text. */
    val ACTIVE_WORDS = listOf(
        "downloading", "uploading", "exporting", "transferring", "saving file", "downloading file",
        "uploading file", "backing up", "restoring", "copying", "moving", "deleting", "emptying",
        "compressing", "extracting", "installing", "updating", "receiving", "sending", "syncing",
        "backup in progress", "upload in progress", "download in progress"
    )

    /** Looser words; only count together with a progress bar. */
    val GENERIC_WORDS = listOf(
        "download", "upload", "export", "transfer", "file", "apk", "pdf", "mp4", "zip", "media",
        "backup", "back up", "restore", "copy", "move", "delete", "install", "update"
    )

    /** "Downloading" in other languages (substring match on lower-cased text). */
    val DOWNLOAD_WORDS_I18N = listOf(
        "descargando", "descarga en curso", "baixando", "fazendo download", "téléchargement", "télécharge",
        "herunterladen", "lädt herunter", "wird heruntergeladen", "scaricamento", "download in corso",
        "indiriliyor", "mengunduh", "загрузка", "скачивание", "скачивается", "डाउनलोड",
        "下载", "下載", "ダウンロード", "다운로드", "تنزيل"
    )

    /** "Uploading", "sending", "seeding" in other languages. */
    val UPLOAD_WORDS_I18N = listOf(
        "subiendo", "enviando", "fazendo upload", "envoi en cours", "hochladen", "lädt hoch",
        "wird hochgeladen", "caricamento in corso", "yükleniyor", "mengunggah", "mengirim",
        "выгрузка", "отправка", "अपलोड", "上传", "上傳", "アップロード", "업로드", "رفع",
        "seeding", "semeando", "sembrando"
    )

    private val OTHER_ACTIVE_I18N = listOf(
        "sincronizando", "synchronisation", "synchronisiere", "sincronizzazione", "copiando", "kopiere",
        "copie en cours", "instalando", "installation en cours", "installiere", "同步", "复制", "安装",
        "コピー中", "インストール中"
    )

    /** Everything that says "a transfer is running" in a language other than English. */
    val ACTIVE_WORDS_I18N: List<String> = DOWNLOAD_WORDS_I18N + UPLOAD_WORDS_I18N + OTHER_ACTIVE_I18N

    /** "Done" in other languages, used when deciding a transfer card has finished. */
    val COMPLETE_WORDS_I18N = listOf(
        "descarga completa", "descarga completada", "download concluído", "download concluido",
        "téléchargement terminé", "download abgeschlossen", "herunterladen abgeschlossen",
        "download completato", "indirme tamamlandı", "unduhan selesai", "загрузка завершена",
        "скачивание завершено", "下载完成", "下載完成", "上传完成", "上傳完成", "ダウンロード完了",
        "다운로드 완료", "اكتمل التنزيل", "completado", "concluído", "terminé", "abgeschlossen",
        "completato", "tamamlandı", "selesai", "завершено", "завершена", "完成", "完了"
    )

    private val DOWNLOADER_PACKAGES = setOf(
        "com.deniscerri.ytdl", "com.deniscerri.ytdlnis",
        "com.dv.adm", "com.dv.adm.pay",
        "idm.internet.download.manager", "idm.internet.download.manager.plus",
        "com.tachibana.downloader", "org.proninyaroslav.libretorrent",
        "com.android.providers.downloads", "com.android.providers.downloads.ui",
        "com.delphicoder.flud", "com.delphicoder.flud.paid", "com.bittorrent.client",
        "com.utorrent.client", "com.vuze.torrent.downloader", "com.xunlei.downloadprovider",
        "com.dv.get", "com.mxtech.videoplayer.ad.download"
    )

    private val CLOUD_PACKAGES = setOf(
        "com.google.android.apps.photos", "com.google.android.apps.docs", "com.dropbox.android",
        "com.microsoft.skydrive", "com.nextcloud.client", "com.owncloud.android",
        "mega.privacy.android.app", "com.box.android", "com.pcloud.pcloud",
        "com.amazon.clouddrive.photos", "com.synology.dsdrive", "com.synology.dsphoto",
        "com.samsung.android.scloud", "me.proton.android.drive", "com.syncthing.android",
        "com.nutomic.syncthingandroid", "org.nextcloud.client"
    )

    private val BROWSER_PREFIXES = listOf(
        "com.android.chrome", "com.chrome.", "org.chromium.", "com.brave.browser",
        "com.microsoft.emmx", "com.opera.", "com.vivaldi.browser", "com.kiwibrowser.browser",
        "org.bromite.", "org.cromite.", "com.sec.android.app.sbrowser", "com.yandex.browser",
        "com.duckduckgo.mobile.android", "org.mozilla."
    )

    private val FILE_MANAGER_PACKAGES = setOf(
        "com.google.android.documentsui", "com.android.documentsui",
        "com.google.android.apps.nbu.files", "com.sec.android.app.myfiles",
        "com.mi.android.globalfileexplorer", "com.android.fileexplorer",
        "com.coloros.filemanager", "com.oneplus.filemanager"
    )

    private val STORE_PACKAGES = setOf(
        "com.android.vending", "com.sec.android.app.samsungapps", "org.fdroid.fdroid",
        "com.aurora.store", "com.xiaomi.mipicks", "com.heytap.market", "com.vivo.appstore",
        "com.huawei.appmarket"
    )

    private val DELETE_WORDS = listOf("deleting", "emptying", "erasing")
    private val RESTORE_WORDS = listOf("restoring", "restore in progress")
    private val BACKUP_WORDS = listOf("backing up", "backup", "back up", "backed up")
    private val INSTALL_WORDS = listOf("installing", "updating app", "updating apps")
    private val COPY_WORDS = listOf("copying", "moving", "compressing", "extracting", "zipping", "unzipping", "pasting")
    private val SYNC_WORDS = listOf(
        "syncing", "synchronizing", "synchronising", "sync in progress", "sincronizando",
        "synchronisation", "synchronisiere", "sincronizzazione", "同步"
    )
    private val UPLOAD_WORDS = listOf("upload", "sending", "posting", "exporting", "sharing", "seeding")

    private val RECORDER_SYNC_WORDS = listOf(
        "backup", "back up", "backing up", "backed up", "upload", "syncing", "sync in progress", "cloud"
    )

    fun isCloudApp(packageName: String): Boolean = packageName.lowercase() in CLOUD_PACKAGES

    private val FINISHED_WORDS = listOf("complete", "finished", "downloaded", "uploaded", "saved", "done")

    /**
     * True when the text says a transfer has finished. The finished notification has no progress bar, so it is not
     * read as a transfer card and would otherwise count as an ordinary alert.
     */
    fun looksFinished(lowerText: String): Boolean =
        FINISHED_WORDS.any { lowerText.contains(it) } || COMPLETE_WORDS_I18N.any { lowerText.contains(it) }

    /**
     * A notification the anti-spam cooldown must never hold back: anything from a download manager, file manager,
     * store or cloud app (a held "download finished" arrived minutes late), and a browser's finished download.
     */
    fun isCooldownExemptTransfer(packageName: String, lowerText: String): Boolean {
        val pkg = packageName.lowercase()
        if (pkg in DOWNLOADER_PACKAGES || pkg in FILE_MANAGER_PACKAGES || pkg in STORE_PACKAGES || pkg in CLOUD_PACKAGES) return true
        return BROWSER_PREFIXES.any { pkg == it || pkg.startsWith(it) } && looksFinished(lowerText)
    }

    fun isKnownTransferApp(packageName: String): Boolean {
        val pkg = packageName.lowercase()
        return pkg in DOWNLOADER_PACKAGES ||
            pkg in FILE_MANAGER_PACKAGES ||
            pkg in STORE_PACKAGES ||
            BROWSER_PREFIXES.any { pkg == it || pkg.startsWith(it) }
    }

    /**
     * Voice recorders announce their cloud backup with the same notification channel as a live recording ("Backing up
     * recordings"), which the audio-recording rules would read as a recording in progress.
     */
    fun isRecorderTransfer(packageName: String, titleText: String, hasDeterminateProgress: Boolean): Boolean {
        if (!OemDeviceRules.isRecorderPackage(packageName)) return false
        val lower = titleText.lowercase()
        if (RECORDER_SYNC_WORDS.any { lower.contains(it) }) return true
        return hasDeterminateProgress && !lower.contains("transcri")
    }

    fun classify(packageName: String, titleText: String): TransferKind {
        val pkg = packageName.lowercase()
        val text = titleText.lowercase()
        return when {
            DELETE_WORDS.any { text.contains(it) } -> TransferKind.DELETE
            RESTORE_WORDS.any { text.contains(it) } -> TransferKind.DOWNLOAD
            // A recorder never downloads; whatever it transfers is its backup.
            OemDeviceRules.isRecorderPackage(pkg) -> TransferKind.BACKUP
            BACKUP_WORDS.any { text.contains(it) } -> TransferKind.BACKUP
            SYNC_WORDS.any { text.contains(it) } -> TransferKind.BACKUP
            pkg in STORE_PACKAGES || INSTALL_WORDS.any { text.contains(it) } -> TransferKind.INSTALL
            COPY_WORDS.any { text.contains(it) } -> TransferKind.COPY_MOVE
            UPLOAD_WORDS.any { text.contains(it) } || UPLOAD_WORDS_I18N.any { text.contains(it) } -> TransferKind.UPLOAD
            text.contains("download") || text.contains("receiving") ||
                DOWNLOAD_WORDS_I18N.any { text.contains(it) } -> TransferKind.DOWNLOAD
            pkg in FILE_MANAGER_PACKAGES -> TransferKind.COPY_MOVE
            // A cloud app with unhelpful wording is uploading or backing up, not downloading.
            pkg in CLOUD_PACKAGES -> TransferKind.UPLOAD
            else -> TransferKind.DOWNLOAD
        }
    }
}
