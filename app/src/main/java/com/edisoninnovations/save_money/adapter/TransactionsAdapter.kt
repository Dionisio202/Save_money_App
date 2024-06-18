package com.edisoninnovations.save_money.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.edisoninnovations.save_money.ImageDialogFragment
import com.edisoninnovations.save_money.R
import com.edisoninnovations.save_money.models.Transaction
import androidx.fragment.app.FragmentActivity
import com.edisoninnovations.save_money.EditTransaction
import com.edisoninnovations.save_money.HomeInformation
import com.edisoninnovations.save_money.utils.LoadingDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TransactionsAdapter(
    private val context: Context,
    private val transactions: List<Transaction>
) : RecyclerView.Adapter<TransactionsAdapter.TransactionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.transactionCategory.text = transaction.category
        holder.transactionAmount.text = transaction.amount.toString()
        holder.transactionNote.text = transaction.note

        // Set color based on transaction type
        if (transaction.tipo == "income") {
            holder.transactionCategory.setTextColor(context.getColor(R.color.green_customed))
            holder.transactionAmount.setTextColor(context.getColor(R.color.green_customed))
        } else if (transaction.tipo == "expense") {
            holder.transactionCategory.setTextColor(context.getColor(android.R.color.holo_red_dark))
            holder.transactionAmount.setTextColor(context.getColor(android.R.color.holo_red_dark))
        }

        // Load images dynamically
        holder.imageContainer.removeAllViews()
        transaction.imageUrls?.let {
            for (imageUrl in it) {
                val imageView = ImageView(context)
                val layoutParams = LinearLayout.LayoutParams(150, 150)
                layoutParams.setMargins(10, 0, 10, 0) // Add margin between images
                imageView.layoutParams = layoutParams
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_image)  // Drawable para placeholder
                    .error(R.drawable.error_image)  // Drawable para error
                    .into(imageView)

                imageView.setOnClickListener {
                    val activity = context as FragmentActivity
                    val dialog = ImageDialogFragment.newInstance(imageUrl)
                    dialog.show(activity.supportFragmentManager, "image_dialog")
                }
                holder.imageContainer.addView(imageView)
            }
        }

        holder.editButton.setOnClickListener {
            val activity = context as FragmentActivity
            val intent = Intent(activity, EditTransaction::class.java)
            intent.putExtra("id_transaccion", transaction.id_transaccion)
            intent.putExtra("category", transaction.category)
            intent.putExtra("amount", transaction.amount)
            intent.putExtra("note", transaction.note)
            intent.putExtra("tipo", transaction.tipo)
            intent.putExtra("id_account", transaction.id_account)
            intent.putExtra("title", transaction.title)

            // Añadir las URLs de las imágenes
            val imageUrlsArray = transaction.imageUrls?.toTypedArray()
            intent.putExtra("imageUrls", imageUrlsArray)
            activity.startActivityForResult(intent, REQUEST_CODE_EDIT_TRANSACTION)
        }
        holder.delete_button.setOnClickListener {
            val activity = context as FragmentActivity
            val loadingDialog = LoadingDialog(context)
            AlertDialog.Builder(context)
                .setTitle("Confirmar eliminación")
                .setMessage("¿Estás seguro de que deseas eliminar esta transacción?")
                .setPositiveButton("Sí") { dialog, which ->
                    CoroutineScope(Dispatchers.Main).launch {
                        loadingDialog.startLoading()
                        try {
                            (activity as HomeInformation).deleteTransaction(transaction.id_transaccion.toInt())
                        } finally {
                            loadingDialog.isDismiss()
                        }
                    }
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    override fun getItemCount(): Int {
        return transactions.size
    }

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val transactionCategory: TextView = itemView.findViewById(R.id.transaction_category)
        val transactionAmount: TextView = itemView.findViewById(R.id.transaction_amount)
        val transactionNote: TextView = itemView.findViewById(R.id.transaction_note)
        val imageContainer: LinearLayout = itemView.findViewById(R.id.image_container)
        val editButton: ImageButton = itemView.findViewById(R.id.edit_button)
        val delete_button: ImageButton = itemView.findViewById(R.id.delete_button)
    }

    companion object {
        const val REQUEST_CODE_EDIT_TRANSACTION = 2
    }
}
