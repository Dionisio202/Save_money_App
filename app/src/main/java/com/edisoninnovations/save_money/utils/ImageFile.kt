package com.edisoninnovations.save_money.utils

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private lateinit var currentPhotoPath: String

fun createImageFile(context: Context): File {
    // Crear un nombre de archivo único con la fecha actual
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val storageDir: File = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
    return File.createTempFile(
        "JPEG_${timeStamp}_", /* prefijo */
        ".jpg", /* sufijo */
        storageDir /* directorio */
    ).apply {
        // Guardar la ruta del archivo para usarla más tarde
        currentPhotoPath = absolutePath
    }
}

fun getCurrentPhotoPath(): String {
    return currentPhotoPath
}
