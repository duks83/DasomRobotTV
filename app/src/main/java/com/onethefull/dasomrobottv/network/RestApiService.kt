package com.onethefull.dasomrobottv.network

import retrofit2.Call
import retrofit2.http.*


interface RestApiService {

    @POST("/set_wav_file_v2/{sort}/{CUSTOMER_CODE}/{DEVICE_CODE}")
    fun sendPostData(
        @Path("sort") sort: String,
        @Path("CUSTOMER_CODE") customerCode: String,
        @Path("DEVICE_CODE") deviceCode: String,
        @Body body: HashMap<String,Any>
    ): Call<String>

}