package dev.lioracode.nextbill.core.data.backup

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.lioracode.nextbill.core.domain.IoDispatcher
import dev.lioracode.nextbill.core.domain.backup.ImportStaging
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** `filesDir/import-staging.json`. 쓰기는 임시 파일 → fsync → rename으로 교체합니다. */
@Singleton
class FileImportStaging @Inject constructor(
    @ApplicationContext context: Context,
    @IoDispatcher private val io: CoroutineDispatcher
) : ImportStaging {
    private val target = File(context.filesDir, FILE_NAME)
    private val lock = Mutex()
    private val temp = File(context.filesDir, "$FILE_NAME.tmp")

    override suspend fun write(json: String) = withContext(io) {
        lock.withLock {
            try {
                FileOutputStream(temp).use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                    out.fd.sync()
                }
                if (!temp.renameTo(target)) throw IOException("Cannot replace $target")
            } catch (e: IOException) {
                temp.delete()
                throw e
            }
        }
    }

    override suspend fun read(): String? = withContext(io) {
        if (target.exists()) target.readText(Charsets.UTF_8) else null
    }

    override suspend fun clear() = withContext(io) {
        lock.withLock {
            if (!target.delete() && target.exists()) throw IOException("Cannot delete $target")
        }
    }

    private companion object {
        const val FILE_NAME = "import-staging.json"
    }
}
