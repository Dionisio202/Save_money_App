package com.edisoninnovations.save_money.ui.gallery

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.edisoninnovations.save_money.AddAccount
import com.edisoninnovations.save_money.EditAccount
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

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        TransactionRepository.transactions.observe(viewLifecycleOwner) { transactions ->
            val groupedTransactions = transactions.filter { it.id_account != null }.groupBy { it.id_account }
            val accountSummaries = groupedTransactions.map { (accountId, accountTransactions) ->
                val totalIncome = accountTransactions.filter { it.tipo == "income" }.sumByDouble { it.cantidad.toDouble() }
                val totalExpense = accountTransactions.filter { it.tipo == "expense" }.sumByDouble { it.cantidad.toDouble() }
                val finalBalance = totalIncome - totalExpense

                AccountSummary(
                    id_account = accountId!!,
                    title = accountTransactions.firstOrNull()?.title ?: "Unknown",
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    finalBalance = finalBalance
                )
            }

            binding.recyclerView.adapter = AccountAdapter(accountSummaries) { accountSummary ->
                val intent = Intent(requireContext(), EditAccount::class.java).apply {
                    putExtra("id_account", accountSummary.id_account)
                    putExtra("title", accountSummary.title)
                }
                startActivityForResult(intent, EDIT_ACCOUNT_REQUEST_CODE)
            }
        }

        binding.fab.setOnClickListener {
            val intent = Intent(requireContext(), AddAccount::class.java)
            startActivity(intent)
        }

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

    data class AccountSummary(
        val id_account: String,
        val title: String,
        val totalIncome: Double,
        val totalExpense: Double,
        val finalBalance: Double
    )

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_ACCOUNT_REQUEST_CODE && resultCode == AppCompatActivity.RESULT_OK) {
            val updatedAccountId = data?.getStringExtra("updated_account_id")
            val updatedAccountTitle = data?.getStringExtra("updated_account_title")
            if (updatedAccountId != null && updatedAccountTitle != null) {
                updateAccountTitle(updatedAccountId, updatedAccountTitle)
            }
            val deletedAccountId = data?.getStringExtra("deleted_account_id")
            if (deletedAccountId != null) {
                removeAccount(deletedAccountId)
            }
        }
    }

    private fun updateAccountTitle(accountId: String, newTitle: String) {
        val transactions = TransactionRepository.transactions.value?.map {
            if (it.id_account == accountId) {
                it.copy(title = newTitle)
            } else {
                it
            }
        } ?: return

        TransactionRepository.setTransactions(transactions)
    }

    private fun removeAccount(accountId: String) {
        val transactions = TransactionRepository.transactions.value?.filterNot {
            it.id_account == accountId
        } ?: return

        TransactionRepository.setTransactions(transactions)
    }

    companion object {
        private const val EDIT_ACCOUNT_REQUEST_CODE = 1
    }
}

