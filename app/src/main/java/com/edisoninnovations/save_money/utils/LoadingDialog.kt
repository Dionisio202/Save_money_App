package com.edisoninnovations.save_money.utils

import android.app.Activity
import android.app.AlertDialog
import com.edisoninnovations.save_money.R

class LoadingDialog(val mActivity: Activity) {
    private lateinit var isDialog: AlertDialog
    fun startLoading(){
        /**set View*/
        val infalter =mActivity.layoutInflater
        val dialogView = infalter.inflate(R.layout.loading_item,null)
        /**set Dialog*/
        val builder=AlertDialog.Builder(mActivity)
        builder.setView(dialogView)
        builder.setCancelable(false)
        isDialog=builder.create()
        isDialog.show()
    }
    fun isDismiss(){
        isDialog.dismiss()
    }
}