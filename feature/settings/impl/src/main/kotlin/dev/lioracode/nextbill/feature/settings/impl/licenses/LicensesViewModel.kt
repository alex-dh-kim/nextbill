package dev.lioracode.nextbill.feature.settings.impl.licenses

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.lioracode.nextbill.core.domain.IoDispatcher
import dev.lioracode.nextbill.feature.settings.impl.R
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** `res/raw/licenses.json`을 IO 디스패처에서 한 번 읽습니다(ADR-10). null = 읽는 중. */
@HiltViewModel
class LicensesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher
) : ViewModel() {
    private val state = MutableStateFlow<LicenseCatalog?>(null)
    internal val catalog: StateFlow<LicenseCatalog?> = state.asStateFlow()

    init {
        viewModelScope.launch {
            state.value = withContext(io) {
                context.resources.openRawResource(R.raw.licenses).bufferedReader().use {
                    parseLicenseCatalog(it.readText())
                }
            }
        }
    }
}
