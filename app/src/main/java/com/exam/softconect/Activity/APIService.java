package com.exam.softconect.Activity;

import com.exam.softconect.retrofit.downloadpdf;

import retrofit2.Call;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface APIService {
    @GET("downloadPdf/")
    Call<downloadpdf> getlink(
            @Query("testID") String testid,
            @Query("/userID") String userid
    );


}
