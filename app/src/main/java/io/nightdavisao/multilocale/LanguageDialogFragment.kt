package io.nightdavisao.multilocale

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

class LanguageDialogFragment(
    private val onClick: (Locale) -> Unit
): DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let {
            val builder = MaterialAlertDialogBuilder(it)
            val availableLocales = Locale.getAvailableLocales()
            // inflate the custom view
            val view = layoutInflater.inflate(R.layout.content_language_selector, null)
            builder.setView(view)
            // set the adapter
            val recyclerView = view.findViewById<RecyclerView>(R.id.languageListRecyclerView)
            val localeRecyclerAdapter = LocaleRecyclerAdapter(recyclerView, availableLocales.toMutableList(), false, false)
            recyclerView.adapter = localeRecyclerAdapter
            recyclerView.layoutManager = LinearLayoutManager(it)
            // set the click listener
            recyclerView.addOnItemTouchListener(RecyclerItemClickListener(it, recyclerView, object : RecyclerItemClickListener.OnItemClickListener {
                override fun onItemClick(view: View?, position: Int) {
                    val adapter = recyclerView.adapter as LocaleRecyclerAdapter
                    onClick(adapter.filteredLocaleList[position])
                    dismiss()
                }

                override fun onItemLongClick(view: View?, position: Int) {
                    // do nothing
                }
            }))
            // filter items in recycler view by searchEditText's text (THIS IS NOT A SEARCHVIEW)
            val searchEditText = view.findViewById<EditText>(R.id.searchEditText)
            searchEditText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    localeRecyclerAdapter.searchFilter.filter(s)
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    // do nothing
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                    // do nothing
                }
            })

            return builder.create()
        } ?: run {
            throw IllegalStateException("Activity cannot be null")
        }
    }
}