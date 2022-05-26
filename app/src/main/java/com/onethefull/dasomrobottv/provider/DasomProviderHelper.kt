package com.onethefull.dasomrobottv.provider
import android.annotation.SuppressLint
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import java.lang.Exception
import java.lang.StringBuilder

/**
 * Created by Douner on 2019-11-26.
 *
 * com.onethefull.dasomrobottv.provider.DasomProviderHelper VERSION_1.0.0
 *
 */
@Suppress("MemberVisibilityCanBePrivate")
object DasomProviderHelper {

    const val uriCode = 1
    var uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(WONDERFUL_PROVIDER_AUTH, WONDERFUL_SETTING, uriCode)
        addURI(WONDERFUL_PROVIDER_AUTH, "$WONDERFUL_SETTING/*", uriCode)
    }

    const val COLUMN_NAME_SETTING_KEY = "setting_key"
    const val COLUMN_NAME_SETTING_VALUE = "setting_value"
    const val COLUMN_NAME_SETTING_ETC = "setting_etc_value"

    // 다솜 서비스 URL
    private const val WONDERFUL_PROVIDER_AUTH = "com.onethefull.database.global.provider"
    private const val WONDERFUL_SETTING = "settings"
    private val BASE_URI: Uri = Uri.parse("content://$WONDERFUL_PROVIDER_AUTH")
    val BASE_PATH_URI: Uri = BASE_URI.buildUpon().appendPath(WONDERFUL_SETTING).build()

    // GLOBAL PARAMS
    const val KEY_GLOBAL_DEVICE_CODE = "key_global_device_code"
    const val KEY_GLOBAL_CUSTOMER_CODE = "key_global_customer_code"
    const val KEY_GLOBAL_SERIAL_NUMBER_CODE = "key_global_serialnumber_code"
    /**
     * @param context application context
     * @param key UrlProviderHelper local key
     */
    @SuppressLint("Recycle")
    fun selectTypeData(context: Context?, key: String): String {
        var cursor: Cursor? = null
        val strBuilder = StringBuilder()
        try {
            val columns = arrayOf(
                COLUMN_NAME_SETTING_ETC,
                COLUMN_NAME_SETTING_KEY,
                COLUMN_NAME_SETTING_VALUE
            )
            cursor = context?.contentResolver?.query(
                BASE_PATH_URI, columns,
                "$COLUMN_NAME_SETTING_KEY=?",
                arrayOf(key),
                null
            )
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    strBuilder.append(
                        cursor.getString(
                            cursor.getColumnIndex(
                                COLUMN_NAME_SETTING_VALUE
                            )
                        )
                    )
                }
            }
            cursor?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.let {
                if (!it.isClosed) it.close()
            }
        }
        return strBuilder.toString()
    }

    // 커스토머 코드
    fun getCustomerCode(context: Context?): String {
        return selectTypeData(context, KEY_GLOBAL_CUSTOMER_CODE)
    }

    // 디바이스 코드
    fun getDeviceCode(context: Context?): String {
        return selectTypeData(context, KEY_GLOBAL_DEVICE_CODE)
    }

    // Android 10 버전 이상
    fun getSerialNumber(context: Context): String {
        return selectTypeData(context, KEY_GLOBAL_SERIAL_NUMBER_CODE)
    }
}