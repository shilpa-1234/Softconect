package com.exam.softconect.Activity;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.http.GET;

    public  interface APIService {
        @GET(".")
        Call<Downloadpojo> listRepos();
    }

