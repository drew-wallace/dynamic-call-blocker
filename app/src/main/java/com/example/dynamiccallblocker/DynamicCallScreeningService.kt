package com.example.dynamiccallblocker

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DynamicCallScreeningService : CallScreeningService() {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val repository by lazy(LazyThreadSafetyMode.NONE) {
        AppContainer.repository(applicationContext)
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val incoming = callDetails.handle?.schemeSpecificPart.orEmpty()
        scope.launch {
            val evaluation = CallScreeningEvaluator.evaluate(
                rawNumber = incoming,
                loadRules = { repository.rulesFlow.first() },
                isInContacts = ::isInContacts
            )

            if (evaluation.failure != null) {
                Log.e(
                    TAG,
                    "Failed to screen incoming=${evaluation.normalizedIncoming}; allowing call by default",
                    evaluation.failure
                )
            } else {
                Log.d(
                    TAG,
                    "Loaded rules blockExact=${evaluation.blockExactCount}, " +
                        "blockPrefix=${evaluation.blockPrefixCount}, " +
                        "allowExact=${evaluation.allowExactCount}, " +
                        "allowPrefix=${evaluation.allowPrefixCount}"
                )
                Log.i(
                    TAG,
                    "Screened incoming=${evaluation.normalizedIncoming}, " +
                        "isContact=${evaluation.isContact}, shouldBlock=${evaluation.shouldBlock}"
                )
            }

            respondToCall(callDetails, buildResponse(evaluation.shouldBlock))
        }
    }

    @SuppressLint("Range")
    private fun isInContacts(rawNumber: String): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return false
        }

        val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon()
            .appendPath(rawNumber)
            .build()

        contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup._ID),
            null,
            null,
            null
        )?.use { cursor ->
            return cursor.moveToFirst()
        }

        return false
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun buildResponse(shouldBlock: Boolean): CallResponse {
        return if (shouldBlock) {
            CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
        } else {
            CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
        }
    }

    companion object {
        private const val TAG = "DynamicCallBlocker"
    }
}
