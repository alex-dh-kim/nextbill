package dev.lioracode.nextbill.core.testing

import dev.lioracode.nextbill.core.domain.backup.ImportStaging
import java.io.IOException

class FakeImportStaging(var failWrite: IOException? = null, var failClear: IOException? = null) :
    ImportStaging {
    @Volatile var content: String? = null

    override suspend fun write(json: String) {
        failWrite?.let { throw it }
        content = json
    }

    override suspend fun read(): String? = content

    override suspend fun clear() {
        failClear?.let { throw it }
        content = null
    }
}
