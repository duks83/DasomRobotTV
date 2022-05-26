package com.onethefull.dasomrobottv.network

import android.content.Context
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.create

class RestApi {

    init{
        val url = "http://34.84.120.228:7070"

        api =  Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(RestApiService::class.java)

    }

    companion object{
        private var instance: RestApi? = null
        lateinit var api: RestApiService

        fun getInstance(): RestApi? {
            if (null == RestApi.instance) {
                synchronized(RestApi::class.java) {
                    if (null == RestApi.instance) {
                        RestApi.instance = RestApi()
                    }
                }
            }
            return RestApi.instance
        }

    }

    fun sendPostData(sort: String,
                       customerCode: String,
                       deviceCode: String,
                       serialNum: String
    ): Call<String> {
        return api.sendPostData(sort, customerCode, deviceCode,
            HashMap<String, Any>().apply {
                this["DEVICE"] = serialNum
            })
    }

}