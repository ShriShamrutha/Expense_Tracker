package com.example.expense__tracker

import android.content.Context
import android.provider.Telephony

data class RawSms(
    val body: String,
    val timestamp: Long
)

fun readSmsMessages(context: Context): List<RawSms> {
    val messages = mutableListOf<RawSms>()

    val cursor = context.contentResolver.query(
        Telephony.Sms.Inbox.CONTENT_URI,
        arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE),
        null,
        null,
        Telephony.Sms.DEFAULT_SORT_ORDER
    )

    cursor?.use {
        val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
        val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)

        while (it.moveToNext()) {
            if (bodyIndex >= 0 && dateIndex >= 0) {
                messages.add(RawSms(body = it.getString(bodyIndex), timestamp = it.getLong(dateIndex)))
            }
        }
    }

    return messages
}