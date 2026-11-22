package com.callfleet.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.callfleet.app.data.CallLogEntry
import com.callfleet.app.databinding.ItemCallLogBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallLogAdapter : RecyclerView.Adapter<CallLogAdapter.VH>() {

    private val items = mutableListOf<CallLogEntry>()
    private val fmt = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

    fun submit(list: List<CallLogEntry>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class VH(val b: ItemCallLogBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemCallLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val e = items[position]
        holder.b.number.text = if (e.name.isNotEmpty()) "${e.name} · ${e.number}" else e.number
        holder.b.meta.text = "${e.type} · ${fmt.format(Date(e.timestamp))} · ${e.durationSec}s"
    }

    override fun getItemCount() = items.size
}
