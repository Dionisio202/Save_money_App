package com.edisoninnovations.save_money.adapter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.edisoninnovations.save_money.databinding.ItemAccountBinding
import com.edisoninnovations.save_money.ui.home.HomeViewModel

class AccountAdapter(private val transactions: List<HomeViewModel.Transaction>) : RecyclerView.Adapter<AccountAdapter.AccountViewHolder>() {

    class AccountViewHolder(val binding: ItemAccountBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val binding = ItemAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AccountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.binding.txtTitle.text = transaction.title
        holder.binding.txtIngreso.text = "Total Income: ${transaction.cantidad}"  // Ajusta según tu lógica
        holder.binding.txtGasto.text = "Total Expense: ${transaction.cantidad}"  // Ajusta según tu lógica
        holder.binding.txtMontoTotal.text = "Final Balance: ${transaction.cantidad}"  // Ajusta según tu lógica
        // Aquí puedes cargar la imagen en el ImageView si es necesario
    }

    override fun getItemCount(): Int = transactions.size
}