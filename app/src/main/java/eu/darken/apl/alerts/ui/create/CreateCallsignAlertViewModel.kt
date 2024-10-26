package eu.darken.apl.alerts.ui.create

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.apl.alerts.core.AlertsRepo
import eu.darken.apl.common.coroutine.DispatcherProvider
import eu.darken.apl.common.debug.logging.Logging.Priority.INFO
import eu.darken.apl.common.debug.logging.log
import eu.darken.apl.common.navigation.navArgs
import eu.darken.apl.common.uix.ViewModel3
import eu.darken.apl.main.core.aircraft.Callsign
import javax.inject.Inject

@HiltViewModel
class CreateCallsignAlertViewModel @Inject constructor(
    handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    private val alertsRepo: AlertsRepo,
) : ViewModel3(dispatcherProvider = dispatcherProvider) {

    private val args by handle.navArgs<CreateCallsignAlertFragmentArgs>()
    val initCallsign: Callsign?
        get() = args.callsign
    val initNote: String?
        get() = args.note

    init {
        log(TAG, INFO) { "initCallsign=$initCallsign, initNote=$initNote" }
    }

    fun create(callsign: Callsign, note: String) = launch {
        log(TAG) { "create($callsign, $note)" }
        alertsRepo.createCallsignAlert(callsign, note.trim())
        popNavStack()
    }
}