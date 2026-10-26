package dev.lioracode.nextbill.feature.settings.impl.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import dev.lioracode.nextbill.core.domain.backup.MAX_BACKUP_CHARS
import java.io.FileNotFoundException
import java.io.IOException

/** SAF 파일 선택기(F9). 저장소 권한이 필요 없습니다. 사용자가 취소하면 아무 일도 하지 않습니다. */
internal class BackupLaunchers(
    /** 저장 위치 선택. 인자는 제안 파일 이름입니다. */
    val export: (String) -> Unit,
    /** 가져올 파일 선택. */
    val import: () -> Unit
)

@Composable
internal fun rememberBackupLaunchers(
    onExportTo: (Uri) -> Unit,
    onImportFrom: (Uri) -> Unit
): BackupLaunchers {
    val currentExport = rememberUpdatedState(onExportTo)
    val currentImport = rememberUpdatedState(onImportFrom)
    val create = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BACKUP_MIME)
    ) { uri -> uri?.let { currentExport.value(it) } }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { currentImport.value(it) }
    }
    return remember(create, open) {
        BackupLaunchers(
            export = { name -> create.launch(name) },
            import = { open.launch(IMPORT_MIME_TYPES) }
        )
    }
}

/**
 * 고른 문서에 백업 문자열을 UTF-8로 씁니다. 호출자가 IO 디스패처에서 부릅니다.
 * "wt"(잘라 내고 쓰기)로 엽니다. Android 10+에서 "w"는 기존 내용을 자르지 않을 수 있어, 더 긴 파일 위에 쓰면
 * 뒤에 옛 내용이 남습니다. 제공자가 "wt"를 거부할 때만 "w"로 다시 엽니다.
 */
internal fun ContentResolver.writeBackup(uri: Uri, text: String) {
    val stream = try {
        openOutputStream(uri, "wt")
    } catch (e: IllegalArgumentException) {
        openOutputStream(uri, "w")
    } catch (e: FileNotFoundException) {
        openOutputStream(uri, "w")
    } ?: throw IOException("Cannot open $uri for writing")
    stream.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
}

/**
 * 고른 문서를 UTF-8로 읽습니다. `MAX_BACKUP_CHARS`를 넘으면 읽기를 멈추고 [BackupTooLargeException]을
 * 던집니다. 호출자가 IO 디스패처에서 부릅니다.
 */
internal fun ContentResolver.readBackup(uri: Uri): String {
    val stream = openInputStream(uri) ?: throw IOException("Cannot open $uri for reading")
    return stream.reader(Charsets.UTF_8).use { it.readAtMost(MAX_BACKUP_CHARS) }
}

private const val BACKUP_MIME = "application/json"
private val IMPORT_MIME_TYPES = arrayOf("application/json", "text/plain")
