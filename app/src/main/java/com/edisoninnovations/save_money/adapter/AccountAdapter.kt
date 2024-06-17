package com.edisoninnovations.save_money.adapter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.edisoninnovations.save_money.databinding.ItemAccountBinding
import com.edisoninnovations.save_money.ui.gallery.GalleryFragment
import com.edisoninnovations.save_money.ui.home.HomeViewModel

class AccountAdapter(private val accountSummaries: List<GalleryFragment.AccountSummary>) : RecyclerView.Adapter<AccountAdapter.AccountViewHolder>() {

    class AccountViewHolder(val binding: ItemAccountBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val binding = ItemAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AccountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val accountSummary = accountSummaries[position]
        holder.binding.txtTitle.text = accountSummary.title
        holder.binding.txtIngreso.text = "Total Income: ${accountSummary.totalIncome}"
        holder.binding.txtGasto.text = "Total Expense: ${accountSummary.totalExpense}"
        holder.binding.txtMontoTotal.text = "Final Balance: ${accountSummary.finalBalance}"
    }

    override fun getItemCount(): Int = accountSummaries.size
}