package eu.darken.apl.common.lists

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.MenuRes
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.google.android.material.appbar.MaterialToolbar
import eu.darken.apl.R
import eu.darken.apl.common.debug.logging.log
import eu.darken.apl.common.getQuantityString
import eu.darken.apl.common.lists.modular.ModularAdapter
import eu.darken.apl.common.lists.selection.ItemSelectionKeyProvider
import eu.darken.apl.common.lists.selection.ItemSelectionLookup
import eu.darken.apl.common.lists.selection.ItemSelectionMod
import eu.darken.apl.common.lists.selection.SelectableItem
import eu.darken.apl.common.uix.Fragment3

fun RecyclerView.setupDefaults(
    adapter: RecyclerView.Adapter<*>? = null,
    verticalDividers: Boolean = true,
    horizontalDividers: Boolean = false,
    layouter: LayoutManager = LinearLayoutManager(context)
) = apply {
    layoutManager = layouter
    itemAnimator = DefaultItemAnimator()

    if (verticalDividers) addItemDecoration(
        DividerItemDecoration2(
            context,
            DividerItemDecoration.VERTICAL,
            drawAfterLastItem = false
        )
    )

    if (horizontalDividers) addItemDecoration(
        DividerItemDecoration2(
            context,
            DividerItemDecoration.HORIZONTAL,
            drawAfterLastItem = false
        )
    )

    if (adapter != null) this.adapter = adapter
}

fun <AdapterT> AdapterT.setupSelectionBase(
    list: RecyclerView,
    selectionPredicate: SelectionTracker.SelectionPredicate<String> = SelectionPredicates.createSelectAnything()
): SelectionTracker<String> where AdapterT : DataAdapter<out SelectableItem>, AdapterT : ModularAdapter<*> {
    val adapter = this
    log { "Setting up selection on $list with $adapter" }
    val tracker = SelectionTracker
        .Builder(
            adapter.javaClass.canonicalName!!,
            list,
            ItemSelectionKeyProvider(adapter),
            ItemSelectionLookup(list),
            StorageStrategy.createStringStorage(),
        )
        .withSelectionPredicate(selectionPredicate)
        .build()

    adapter.addMod(ItemSelectionMod(tracker))

    return tracker
}

fun <AdapterT, ItemT : SelectableItem> Fragment3.installListSelection(
    list: RecyclerView = ui!!.root.findViewById(R.id.list),
    toolbar: MaterialToolbar = ui!!.root.findViewById(R.id.toolbar),
    adapter: AdapterT,
    @MenuRes cabMenuRes: Int,
    onPrepare: (SelectionTracker<String>, ActionMode, Menu) -> Boolean = { _: SelectionTracker<String>, _: ActionMode, _: Menu -> true },
    onSelected: (SelectionTracker<String>, MenuItem, List<ItemT>) -> Boolean,
    onChange: (SelectionTracker<String>) -> Unit = {},
    selectionPredicate: SelectionTracker.SelectionPredicate<String> = SelectionPredicates.createSelectAnything()
): SelectionTracker<String> where AdapterT : DataAdapter<ItemT>, AdapterT : ModularAdapter<*> {
    val context = requireContext()
    val tracker = adapter.setupSelectionBase(list, selectionPredicate)
    log { "Performing common setup for $toolbar" }

    var actionMode: ActionMode? = null
    val cabCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(cabMenuRes, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            val selectableItems = adapter.data.filter { it.itemSelectionKey != null }
            menu.findItem(R.id.action_select_all)?.isVisible = tracker.selection.size() != selectableItems.size
            return onPrepare(tracker, mode, menu)
        }

        override fun onActionItemClicked(mode: ActionMode, menuItem: MenuItem): Boolean = when (menuItem.itemId) {
            R.id.action_select_all -> {
                val items = adapter.data.mapNotNull { it.itemSelectionKey }
                tracker.setItemsSelected(items, true)
                true
            }

            else -> {
                val selectedItems = tracker.selection.map { key -> adapter.data.first { it.itemSelectionKey == key } }
                onSelected(tracker, menuItem, selectedItems)
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            tracker.clearSelection()
            actionMode = null
        }
    }

    tracker.addObserver(object : SelectionTracker.SelectionObserver<String>() {
        override fun onSelectionChanged() {
            onChange(tracker)
            when {
                tracker.hasSelection() -> {
                    actionMode ?: toolbar.startActionMode(cabCallback)?.also { actionMode = it }
                    actionMode?.apply {
                        title = context.getQuantityString(
                            R.plurals.result_x_items,
                            tracker.selection.size(),
                            tracker.selection.size(),
                        )
                        invalidate()
                    }
                }

                else -> {
                    actionMode?.finish()
                    actionMode = null
                }
            }
        }
    })

    return tracker
}