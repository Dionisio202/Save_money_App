package com.edisoninnovations.save_money

import ImageAdapter
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputFilter
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.edisoninnovations.save_money.DataManager.CategoryManager
import com.edisoninnovations.save_money.DataManager.DateManager
import com.edisoninnovations.save_money.models.TransactionData
import com.edisoninnovations.save_money.models.TransactionDataEdit
import com.edisoninnovations.save_money.models.Transimage
import com.edisoninnovations.save_money.ui.gallery.GalleryFragment
import com.edisoninnovations.save_money.utils.LoadingDialog
import com.edisoninnovations.save_money.utils.createImageFile
import com.edisoninnovations.save_money.utils.getCurrentPhotoPath
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class EditTransaction : AppCompatActivity(), ImageAdapter.OnItemClickListener, CategoryDialogFragment.CategoryDialogListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var imageAdapter: ImageAdapter
    private val imageUris = ArrayList<Uri>()
    private var isIncome: Boolean = false
    private lateinit var categoryButton: ImageButton
    private lateinit var loadingDialog: LoadingDialog
    private var selectedCategoryId: Int = -1 // Default category ID
    private lateinit var transactionId: String;
    private lateinit var originalImageUris: List<Uri>
    private var selectedAccountId: String? = null
    private var selectedtitleAccount: String? = null
    private lateinit var accountSpinner: Spinner
    private var accounts: List<GalleryFragment.Account> = emptyList()

    private lateinit var accountSpinnerContainer: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_transaction)
        checkPermissions()
        loadingDialog = LoadingDialog(this)
        isIncome = intent.getBooleanExtra("isIncome", false)

        // Recuperar datos del Intent
        val transactionId = intent.getStringExtra("id_transaccion")
        if (transactionId != null) {
            this.transactionId = transactionId
        }
        val category = intent.getStringExtra("category")
        val amount = intent.getDoubleExtra("amount", 0.0)
        val note = intent.getStringExtra("note")
        val tipos= intent.getStringExtra("tipo")
        selectedAccountId = intent.getStringExtra("id_account")
        selectedtitleAccount = intent.getStringExtra("title")
        if(tipos=="expense"){
            isIncome = false
        }else{
            isIncome = true
        }
        // Obtener el ID de la categoría
        val categoryId = category?.let { CategoryManager.getCategoryID(it, isIncome) }
        selectedCategoryId = categoryId ?: -1
        val imageUrls = intent.getStringArrayExtra("imageUrls")?.toList() ?: emptyList()
        accountSpinner = findViewById(R.id.account_spinner)
        accountSpinnerContainer = findViewById(R.id.account_spinner_container)
        // Configurar los campos con los datos recibidos
        findViewById<EditText>(R.id.amount_input).setText(amount.toString())
        findViewById<TextView>(R.id.category_text).text = category
        findViewById<EditText>(R.id.note_input).setText(note)

        // Inicializar RecyclerView y Adapter
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        imageAdapter = ImageAdapter(imageUris, this, this)
        recyclerView.adapter = imageAdapter

        // Mostrar las imágenes en el RecyclerView
        imageUris.clear()
        imageUris.addAll(imageUrls.map { Uri.parse(it) })
        imageAdapter.notifyDataSetChanged()
        originalImageUris = imageUris.toList()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        imageAdapter = ImageAdapter(imageUris, this, this)
        recyclerView.adapter = imageAdapter

        val amountInput: EditText = findViewById(R.id.amount_input)
        amountInput.filters = arrayOf(InputFilter.LengthFilter(12))

        val cameraButton: ImageButton = findViewById(R.id.camera_button)
        cameraButton.setOnClickListener {
            if (imageUris.size >= 3) {
                Toast.makeText(this, "No puedes añadir más de 3 fotos", Toast.LENGTH_SHORT).show()
            } else {
                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (takePictureIntent.resolveActivity(packageManager) != null) {
                    val photoFile: File? = try {
                        createImageFile(this)
                    } catch (ex: IOException) {
                        null
                    }
                    photoFile?.also {
                        val photoURI: Uri = FileProvider.getUriForFile(
                            this,
                            "com.edisoninnovations.save_money.fileprovider",
                            it
                        )
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                    }
                }
            }
        }

        categoryButton = findViewById(R.id.category_button)
        categoryButton.setOnClickListener {
            showCategoryDialog()
        }
        val cancelButton: ImageButton = findViewById(R.id.cancel_button)
        cancelButton.setOnClickListener {
            finish()
        }
        val saveButton: ImageButton = findViewById(R.id.edit_button)
        val tipo = intent.getStringExtra("tipo")
        saveButton.setOnClickListener {
            val selectedDate = DateManager.selectedDate ?: "Fecha no seleccionada"
            val userId = supabase.auth.currentUserOrNull()?.id
            lifecycleScope.launch {
                if (tipo != null) {
                    println("############################transacion id: $transactionId")
                    println("########################selectedDate: $selectedDate")
                    editTransaction(selectedDate, userId, tipo)
                }
            }
        }
        lifecycleScope.launch {
            println("############################Account id: $selectedAccountId")
            println("############################Account title: $selectedtitleAccount")
            fetchAccounts()
        }
    }
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
        val accountListType = Types.newParameterizedType(List::class.java, GalleryFragment.Account::class.java)
        val accountAdapter = moshi.adapter<List<GalleryFragment.Account>>(accountListType)
        val accounts: List<GalleryFragment.Account>? = accountAdapter.fromJson(response.data.toString())

        if (accounts != null) {
            // Agregar la opción "No seleccionar ninguna cuenta" al principio de la lista
            val accountTitles = listOf("No seleccionar ninguna cuenta") + accounts.map { it.title }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, accountTitles)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            accountSpinner.adapter = adapter

            // Save the accounts in a variable for later use
            this.accounts = accounts
            println("#######################################accounts $accounts")
            println("#######################################selectedAccountId $selectedAccountId")
            println("#######################################selectedtitleAccount $selectedtitleAccount")

            // Asignar un valor por defecto a selectedAccountId si es null
            val defaultAccountId = selectedAccountId?.toDoubleOrNull()?.toInt()?.toString() ?: "0"

            // Select the correct account in the Spinner
            val position = if (defaultAccountId != "0") {
                accounts.indexOfFirst { it.id_account == defaultAccountId } + 1
            } else {
                0 // "No seleccionar ninguna cuenta"
            }
            accountSpinner.setSelection(position)

            // Show the spinner
            accountSpinnerContainer.visibility = View.VISIBLE
        } else {
            // Hide the spinner if there are no accounts
            accountSpinnerContainer.visibility = View.GONE
        }
    }





    private suspend fun editTransaction(selectedDate: String, userId: String?, tipo: String) {
        loadingDialog.startLoading()
        try {
            val amountInput: EditText = findViewById(R.id.amount_input)
            val amount = amountInput.text.toString().toDoubleOrNull()
            val categoryText: TextView = findViewById(R.id.category_text)
            val category = categoryText.text.toString()
            val descriptionInput: EditText = findViewById(R.id.note_input)
            val note = descriptionInput.text.toString()

            if (amount != null && userId != null && category.isNotEmpty() ) {
                if(amount>0){
                   
                        val currentTimeMillis = System.currentTimeMillis()
                        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        val formattedTime = timeFormat.format(Date(currentTimeMillis))
                    val accountId = if (accountSpinner.selectedItemPosition > 0) {
                        accounts[accountSpinner.selectedItemPosition - 1].id_account.toInt()
                    } else {
                        null
                    }

                        val transactionData = TransactionData(
                            id_categoria = selectedCategoryId,
                            nota = note,
                            tipo = tipo,
                            cantidad = amount,
                            id_usuario = userId,
                            fecha = selectedDate,
                            tiempo = formattedTime,
                            id_account =accountId
                        )

                        val response = withContext(Dispatchers.IO) {
                            supabase.from("transacciones").update(transactionData) {
                                filter {
                                    eq("id_transaccion", transactionId)
                                }
                                select()
                            }
                        }
                        val data = response.data
                        println("#######################################datos $data")
                        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                        val type = Types.newParameterizedType(List::class.java, Map::class.java)
                        val jsonAdapter = moshi.adapter<List<Map<String, Any>>>(type)
                        val parsedData = jsonAdapter.fromJson(response.data.toString())

                        parsedData?.let {
                            val idTransaccion = it.first()["id_transaccion"] as? Double
                            idTransaccion?.let { id ->
                                if(originalImageUris!=imageUris){
                                    // Obtener imágenes eliminadas
                                    val deletedImages = originalImageUris.filter { it !in imageUris }
                                    // Obtener imágenes añadidas
                                    val addedImages = imageUris.filter { it !in originalImageUris }

                                    // Eliminar imágenes eliminadas del almacenamiento
                                    deletedImages.forEach { uri ->
                                        deleteImage(uri,id.toInt())
                                    }

                                    // Subir imágenes añadidas
                                    uploadImages(id.toInt(), addedImages)
                                }

                            }
                        }
                        Toast.makeText(this@EditTransaction, "Transacción guardada", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()


                }else{
                    Toast.makeText(this@EditTransaction, "La cantidad debe ser mayor a 0", Toast.LENGTH_SHORT).show()
                }


            } else {
                Toast.makeText(this@EditTransaction, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this@EditTransaction, e.message, Toast.LENGTH_SHORT).show()
            println("#######################################error $e")
        } finally {
            loadingDialog.isDismiss()
        }
    }
    private fun generateUniqueFileName(): String {
        val timestamp = System.currentTimeMillis()
        return "image_$timestamp.png"
    }
    private suspend fun deleteImage(uri: Uri ,transactionId: Int,) {
        val fileName = uri.lastPathSegment ?: return
        val imageUrl = "https://zqiagwapnqwnfhehhqom.supabase.co/storage/v1/object/public/imagenes/$fileName"
        try {
            withContext(Dispatchers.IO) {
                supabase.storage.from("imagenes").delete(fileName)
            }
            // Eliminar la referencia de la imagen de la tabla transimages
            val transImageData = Transimage(id_transaccion = transactionId, imagen = imageUrl)
            withContext(Dispatchers.IO) {
                supabase.from("transimages").delete {
                    filter {
                        eq("id_transaccion", transactionId)
                        eq("imagen", imageUrl)
                    }
                }

            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@EditTransaction, "Imagen eliminada correctamente", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@EditTransaction, "Error al eliminar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private suspend fun uploadImages(transactionId: Int, addedImages: List<Uri>) {
        val supabaseUrl = "https://zqiagwapnqwnfhehhqom.supabase.co"
        addedImages.forEach { uri ->
            val fileName = generateUniqueFileName()
            val file = File(uri.path!!)
            try {
                val response = withContext(Dispatchers.IO) {
                    supabase.storage.from("imagenes").upload(fileName, file.readBytes())
                }
                val imageUrl = "$supabaseUrl/storage/v1/object/public/imagenes/$fileName"

                val transImageData = Transimage(id_transaccion = transactionId, imagen = imageUrl)
                withContext(Dispatchers.IO) {
                    supabase.from("transimages").insert(transImageData)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditTransaction, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
                }
            }
        }}

    private fun showCategoryDialog() {
        val dialog = CategoryDialogFragment()
        dialog.setIsIncome(isIncome)
        dialog.show(supportFragmentManager, "CategoryDialog")
    }

    override fun onCategorySelected(category: String, categoryKey: Int) {
        val categoryText: TextView = findViewById(R.id.category_text)
        categoryText.text = category
        selectedCategoryId = categoryKey
    }

    override fun onItemClick(uri: Uri) {
        showImageDialog(uri.toString())
    }

    private fun showImageDialog(imageUrl: String) {
        val dialogFragment = ImageDialogFragment.newInstance(imageUrl)
        dialogFragment.show(supportFragmentManager, "image_dialog")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == REQUEST_IMAGE_CAPTURE) {
            val imageUri = Uri.fromFile(File(getCurrentPhotoPath()))
            imageUris.add(imageUri)
            imageAdapter.notifyItemInserted(imageUris.size - 1)
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val permissionsToRequest = mutableListOf<String>()

        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val REQUEST_IMAGE_CAPTURE = 1
    }
}
