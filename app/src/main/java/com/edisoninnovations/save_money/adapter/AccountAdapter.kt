package com.edisoninnovations.save_money.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.edisoninnovations.save_money.databinding.ItemAccountBinding
import com.edisoninnovations.save_money.ui.gallery.GalleryFragment

class AccountAdapter(
    private val accountSummaries: List<GalleryFragment.AccountSummary>,
    private val onAccountClick: (GalleryFragment.AccountSummary) -> Unit
) : RecyclerView.Adapter<AccountAdapter.AccountViewHolder>() {

    class AccountViewHolder(val binding: ItemAccountBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val binding = ItemAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AccountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val accountSummary = accountSummaries[position]
        holder.binding.txtTitle.text = "Cuenta :  ${accountSummary.title}"
        holder.binding.txtIngreso.text = "Ingresos Totales: ${accountSummary.totalIncome}"
        holder.binding.txtGasto.text = "Gastos Totales: ${accountSummary.totalExpense}"
        holder.binding.txtMontoTotal.text = "Balance Final: ${accountSummary.finalBalance}"

        holder.itemView.setOnClickListener {
            onAccountClick(accountSummary)
        }
    }

    override fun getItemCount(): Int = accountSummaries.size
}
