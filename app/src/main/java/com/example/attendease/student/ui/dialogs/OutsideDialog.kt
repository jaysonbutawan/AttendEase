package com.example.attendease.student.ui.dialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import com.example.attendease.R

class OutsideDialog(private val context: Context) {
    private var dialog: AlertDialog? = null
    private var messageText: TextView? = null

    fun show(distance: Float) {
        if (dialog?.isShowing == true) return

        val builder = AlertDialog.Builder(context)
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_outside_warning, null)

        messageText = view.findViewById(R.id.txtDistance)
        messageText?.text = "You are approximately ${distance.toInt()} meters away from the allowed area.\nPlease return immediately."

        builder.setView(view)
        builder.setCancelable(false)
        dialog = builder.create()
        dialog?.show()
    }

    fun dismiss() {
        if (dialog?.isShowing == true) {
            dialog?.dismiss()
            dialog = null
        }
    }

    fun isShowing(): Boolean {
        return dialog?.isShowing == true
    }
}
