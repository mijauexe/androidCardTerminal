package com.card.terminal.components


import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.TextView
import com.card.terminal.R


class CustomDialog
    (var c: Activity, var text: String) : Dialog(c), View.OnClickListener {
    var d: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.custom_dialog)
        findViewById<TextView>(R.id.cardNumber).text = text
    }

    override fun onClick(v: View?) {
        TODO("Not yet implemented")
    }
}