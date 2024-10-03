package eu.darken.apl.main.ui.onboarding.privacy

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.apl.common.BuildConfigWrap
import eu.darken.apl.common.PrivacyPolicy
import eu.darken.apl.common.WebpageTool
import eu.darken.apl.common.coroutine.DispatcherProvider
import eu.darken.apl.common.datastore.value
import eu.darken.apl.common.datastore.valueBlocking
import eu.darken.apl.common.debug.logging.log
import eu.darken.apl.common.uix.ViewModel3
import eu.darken.apl.main.core.GeneralSettings
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

@HiltViewModel
class PrivacyViewModel @Inject constructor(
    @Suppress("unused") private val handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    private val generalSettings: GeneralSettings,
    private val webpageTool: WebpageTool,
) : ViewModel3(dispatcherProvider = dispatcherProvider) {

    val state = combine(
        generalSettings.isUpdateCheckEnabled.flow,
        flowOf(Unit)
    ) { isUpdateCheckEnabled, _ ->
        State(
            isUpdateCheckEnabled = isUpdateCheckEnabled,
            isUpdateCheckSupported = BuildConfigWrap.FLAVOR == BuildConfigWrap.Flavor.FOSS,
        )
    }
        .asLiveData2()

    fun goPrivacyPolicy() {
        log(TAG) { "goPrivacyPolicy()" }
        webpageTool.open(PrivacyPolicy.URL)
    }

    fun toggleUpdateCheck() {
        log(TAG) { "toggleUpdateCheck()" }
        generalSettings.isUpdateCheckEnabled.valueBlocking = !generalSettings.isUpdateCheckEnabled.valueBlocking
    }

    fun finishPrivacy() = launch {
        generalSettings.isOnboardingFinished.value(true)
        PrivacyFragmentDirections.actionPrivacyFragmentToMainFragment().navigate()
    }

    data class State(
        val isUpdateCheckEnabled: Boolean,
        val isUpdateCheckSupported: Boolean,
    )
}