package com.edisoninnovations.save_money.ui.gallery

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.edisoninnovations.save_money.AddAccount
import com.edisoninnovations.save_money.adapter.AccountAdapter
import com.edisoninnovations.save_money.databinding.FragmentGalleryBinding
import com.edisoninnovations.save_money.models.TransactionRepository
import com.edisoninnovations.save_money.supabase
import com.edisoninnovations.save_money.ui.home.HomeViewModel
import com.squareup.moshi.JsonClass
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private lateinit var homeViewModel: HomeViewModel

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        homeViewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        println("GalleryFragment: Transactions received: " + TransactionRepository.transactions.value)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Observa las transacciones del TransactionRepository
        TransactionRepository.transactions.observe(viewLifecycleOwner) { transactions ->
            println("GalleryFragment: Transactions received: $transactions")

            // Filtra las transacciones que tienen títulos diferentes de null
            val filteredTransactions = transactions.filter { it.title != null }

            // Configura el RecyclerView Adapter
            binding.recyclerView.adapter = AccountAdapter(filteredTransactions)
        }

        binding.fab.setOnClickListener {
            val intent = Intent(requireContext(), AddAccount::class.java)
            startActivity(intent)
        }

        // Observa si necesita refrescar los datos
        TransactionRepository.needsRefresh.observe(viewLifecycleOwner) { needsRefresh ->
            if (needsRefresh) {
                viewLifecycleOwner.lifecycleScope.launch {
                    fetchAccounts()
                    TransactionRepository.setNeedsRefresh(false)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @JsonClass(generateAdapter = true)
    data class Account(
        val title: String,
        val id_account: String
    )

    private suspend fun fetchAccounts() {
        val userId = supabase.auth.currentUserOrNull()?.id

        val response = withContext(Dispatchers.IO) {
            supabase.from("accounts").select(columns = Columns.list("title", "id_account")) {
                filter {
                    if (userId != null) {
                        eq("id_usuario", userId)
                    }
                }
            }
        }

        println("Obteniendo cuentas: ${response.data}")

        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val accountListType = Types.newParameterizedType(List::class.java, Account::class.java)
        val accountAdapter = moshi.adapter<List<Account>>(accountListType)
        val accounts: List<Account>? = accountAdapter.fromJson(response.data.toString())

        if (accounts != null) {
            val transactionAccounts = TransactionRepository.transactions.value?.map { it.id_account } ?: emptyList()

            val newAccounts = accounts.filter { account ->
                account.id_account !in transactionAccounts
            }

            val newTransactions = newAccounts.map { account ->
                HomeViewModel.Transaction(
                    cantidad = 0.0f,
                    tipo = "account",
                    fecha = "",
                    id_account = account.id_account,
                    title = account.title
                )
            }

            TransactionRepository.setTransactions(TransactionRepository.transactions.value.orEmpty() + newTransactions)
        }
    }
}