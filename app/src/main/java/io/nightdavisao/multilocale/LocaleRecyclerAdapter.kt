package io.nightdavisao.multilocale

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.widget.Filter
import androidx.recyclerview.widget.DiffUtil
import java.util.Locale

class LocaleRecyclerAdapter(
    val recyclerView: RecyclerView,
    val localeList: MutableList<Locale>,
    var deleteMode: Boolean,
    var showDragHandle: Boolean = true,
) :
    RecyclerView.Adapter<LocaleRecyclerAdapter.ViewHolder>() {

    var filteredLocaleList: List<Locale> = localeList

    val diffCallback = object : DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return localeList.size
        }

        override fun getNewListSize(): Int {
            return filteredLocaleList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return filteredLocaleList[newItemPosition].hashCode() == localeList[oldItemPosition].hashCode()
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return filteredLocaleList[newItemPosition] == localeList[oldItemPosition]
        }

    }

    val searchFilter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val charSeqStr = constraint.toString()
            filteredLocaleList = if (charSeqStr.isEmpty()) {
                localeList
            } else {
                localeList.filter { it.displayName.contains(charSeqStr, true) }
            }
            val results = FilterResults()
            results.values = filteredLocaleList
            return results
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            filteredLocaleList = results.values as List<Locale>
            recyclerView.postDelayed({
                notifyDataSetChanged()
            }, 500)
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val localeFriendlyName: TextView
        val localeLanguageName: TextView
        val deleteCheckBox: CheckBox
        val dragHandle: ImageView

        init {
            localeFriendlyName = view.findViewById(R.id.localeFriendlyName)
            localeLanguageName = view.findViewById(R.id.localeLanguageName)
            deleteCheckBox = view.findViewById(R.id.deleteCheckBox)
            dragHandle = view.findViewById(R.id.dragHandle)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val binding = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.locale_item, viewGroup, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = filteredLocaleList.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentLocale = filteredLocaleList[position]

        holder.localeFriendlyName.text = currentLocale.displayName ?: ""
        holder.localeLanguageName.text = currentLocale.getDisplayName(filteredLocaleList[position])
        holder.localeLanguageName.textLocale = currentLocale

        // if the texts are the same, hide the language name
        holder.localeLanguageName.visibility =
            if (holder.localeFriendlyName.text == holder.localeLanguageName.text) View.GONE else View.VISIBLE
        // only show the delete checkbox if the user clicked the "delete" action button
        holder.deleteCheckBox.visibility = if (deleteMode) View.VISIBLE else View.GONE
        holder.dragHandle.visibility =
            if (showDragHandle && !deleteMode) View.VISIBLE else View.GONE
    }

}