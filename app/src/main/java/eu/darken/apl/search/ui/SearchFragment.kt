package eu.darken.apl.search.ui

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import eu.darken.apl.R
import eu.darken.apl.common.debug.logging.log
import eu.darken.apl.common.debug.logging.logTag
import eu.darken.apl.common.lists.differ.update
import eu.darken.apl.common.lists.setupDefaults
import eu.darken.apl.common.uix.Fragment3
import eu.darken.apl.common.viewbinding.viewBinding
import eu.darken.apl.databinding.SearchFragmentBinding
import eu.darken.apl.main.ui.MainActivity
import eu.darken.apl.search.core.SearchRepo


@AndroidEntryPoint
class SearchFragment : Fragment3(R.layout.search_fragment) {

    override val vm: SearchViewModel by viewModels()
    override val ui: SearchFragmentBinding by viewBinding()

    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        locationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            log(TAG) { "locationPermissionLauncher: $isGranted" }
            // Should refresh automatically
        }
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ui.toolbar.apply {
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_settings -> {
                        (requireActivity() as MainActivity).goToSettings()
                        true
                    }

                    else -> false
                }
            }
        }

        ui.apply {
            searchInput.setOnEditorActionListener { view, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_SEARCH -> {
                        vm.search(view.text.toString())
                        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(view.windowToken, 0)
                        true
                    }

                    else -> false
                }
            }
            searchLayout.setEndIconOnClickListener {
                searchInput.setText("")
                vm.search(null)
            }
        }

        val adapter = SearchAdapter()
        ui.list.setupDefaults(adapter, dividers = false)

        vm.state.observe2(ui) { state ->
            adapter.update(state.items)
            toolbar.subtitle = if (state.query != null) {
                resources.getQuantityString(R.plurals.search_found_x_aircraft, 0, state.items.size)
            } else {
                getString(R.string.search_page_label)
            }
            searchInput.apply {
                when (state.query) {
                    is SearchRepo.Query.All -> if (text.toString() != state.query?.term) {
                        setText(state.query?.term)
                    }

                    is SearchRepo.Query.Position -> setText("")
                    null -> setText("")
                }

            }
        }

        vm.events.observe2 { event ->
            when (event) {
                SearchEvents.RequestLocationPermission -> {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        }

        super.onViewCreated(view, savedInstanceState)
    }

    companion object {
        private val TAG = logTag("Search", "Fragment")
    }
}
