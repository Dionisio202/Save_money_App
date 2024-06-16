package com.edisoninnovations.save_money.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.edisoninnovations.save_money.databinding.FragmentGalleryBinding
import com.edisoninnovations.save_money.models.TransactionRepository
import com.edisoninnovations.save_money.ui.home.HomeViewModel

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private lateinit var homeViewModel: HomeViewModel

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        println("WWWWWWWWWWWWWWWWWWWWWGalleryFragment: Transactions received: " + TransactionRepository.transactions.value)

        // Observa las transacciones del TransactionRepository
        TransactionRepository.transactions.observe(viewLifecycleOwner) { transactions ->
            println("WWWWWWWWWGalleryFragment: Transactions received: $transactions")

            // Filtra las transacciones que tienen títulos diferentes de null
            val filteredTransactions = transactions.filter { it.title != null }

            // Actualiza tus vistas con los datos de filteredTransactions
            binding.txtTitle.text = filteredTransactions.firstOrNull()?.title ?: "Unknown"
            binding.txtIngreso.text = "Total Income: ${filteredTransactions.filter { it.tipo == "income" }.sumByDouble { it.cantidad.toDouble() }}"
            binding.txtGasto.text = "Total Expense: ${filteredTransactions.filter { it.tipo == "expense" }.sumByDouble { it.cantidad.toDouble() }}"
            binding.txtMontoTotal.text = "Final Balance: ${(filteredTransactions.filter { it.tipo == "income" }.sumByDouble { it.cantidad.toDouble() } - filteredTransactions.filter { it.tipo == "expense" }.sumByDouble { it.cantidad.toDouble() })}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
