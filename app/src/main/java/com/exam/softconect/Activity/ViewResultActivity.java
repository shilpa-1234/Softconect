package com.exam.softconect.Activity;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.exam.softconect.Adapter.ResultViewAdapter;
import com.exam.softconect.Helper.CommonUtils;
import com.exam.softconect.Helper.TestPanelHelper;
import com.exam.softconect.R;
import com.exam.softconect.Utils.ServerUtils;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class ViewResultActivity extends AppCompatActivity implements View.OnClickListener {

    ImageView img_back;
    String str_testID;
    ResultViewAdapter adapter;
    List<TestPanelHelper> resultList;
    RecyclerView recyclearview;
    TextView txt_test_name, txt_total_question, txt_total_marks, txt_total_correct, txt_total_incorrect, txt_not_attempted, txt_pdf_download;
    String dest_file_path = "result.pdf";
    int downloadedSize = 0, totalsize;
    float per = 0;
    String TAG = "@@method";
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1;

    PermissionUtils permissionUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_result);

        //get data from Intent
        Intent intent = getIntent();
        str_testID = intent.getStringExtra("testId");

        //get references
        img_back = findViewById(R.id.img_back);
        txt_test_name = findViewById(R.id.txt_test_name);
        txt_total_question = findViewById(R.id.txt_total_question);
        txt_total_marks = findViewById(R.id.txt_total_marks);
        txt_total_correct = findViewById(R.id.txt_total_correct);
        txt_total_incorrect = findViewById(R.id.txt_total_incorrect);
        txt_not_attempted = findViewById(R.id.txt_not_attempted);
        recyclearview = (RecyclerView) findViewById(R.id.recyclearview_result);
        recyclearview.setHasFixedSize(true);
        recyclearview.setLayoutManager(new LinearLayoutManager(this));
        permissionUtils = new PermissionUtils();

        resultList = new ArrayList<>();
        txt_pdf_download = findViewById(R.id.txt_pdf_download);
        adapter = new ResultViewAdapter(this, resultList);
        recyclearview.setAdapter(adapter);

        //call method

        getResult();


        //set listner
        img_back.setOnClickListener(this);
        txt_pdf_download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pdflink();
//                getlink();
         /*       SharedPreferences sharedPreferences
                        = getSharedPreferences("MySharedPref",
                        MODE_PRIVATE);
                     String urlEditText=sharedPreferences.getString("link","0");
                Log.d("urlEditText", urlEditText);
                if (permissionUtils.checkPermission(ViewResultActivity.this, STORAGE_PERMISSION_REQUEST_CODE, view)) {
                    if (urlEditText.length() > 0) {
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(urlEditText)));
                            Log.d("simplestring", urlEditText);
                            Log.d("simplestring1", "is in try");
                        } catch (Exception e) {
                            e.getStackTrace();
                        }
                    }

                }*/
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case STORAGE_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Snackbar.make(urlTextInputLayout, "Permission Granted, Now you can write storage.", Snackbar.LENGTH_LONG).show();
                } else {
                    //Snackbar.make(urlTextInputLayout, "Permission Denied, You cannot access storage.", Snackbar.LENGTH_LONG).show();
                }
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.img_back:
                finish();
                break;
        }
    }

    public void getResult() {

        //show process dialog
        final ProgressDialog progressDialog = ProgressDialog.show(this, null, null, true);
        progressDialog.setContentView(R.layout.custom_loader);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET,
                ServerUtils.Base_url + ServerUtils.viewResult_url + "/" + str_testID + "/" + CommonUtils.userID,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        String status;
                        try {

                            status = response.getString("status");

                            if (status.equalsIgnoreCase("1")) {

                                JSONObject jsonObject = response.getJSONObject("test_details");

                                txt_test_name.setText(jsonObject.getString("test_name"));
                                txt_total_question.setText("Total Question - " + jsonObject.getString("total_question"));
                                txt_total_marks.setText("Total Marks - " + jsonObject.getString("total_marks"));
                                txt_total_correct.setText("Total Correct - " + jsonObject.getString("total_correct"));
                                txt_total_incorrect.setText("Total InCorrect - " + jsonObject.getString("total_incorrect"));
                                txt_not_attempted.setText("Not Attempted - " + jsonObject.getString("total_omitted"));


                                JSONArray jsonArray = response.getJSONArray("question_wise_Result");

                                for (int i = 0; i < jsonArray.length(); i++) {
                                    try {
                                        JSONObject jsonObject_result = jsonArray.getJSONObject(i);

                                        TestPanelHelper testList_ = new TestPanelHelper();

                                        testList_.setQuestion_number(jsonObject_result.getString("ques_no"));
                                        testList_.setQuestion_status(jsonObject_result.getString("is_attempt"));
                                        testList_.setYour_answer(jsonObject_result.getString("student_answer"));
                                        testList_.setCorrect_answer(jsonObject_result.getString("correct_answer"));
                                        testList_.setYour_scored(jsonObject_result.getString("correct_marks"));

                                        resultList.add(testList_);

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        progressDialog.dismiss();
                                    }
                                }
                            }

                            adapter.notifyDataSetChanged();
                            progressDialog.dismiss();
                        } catch (JSONException e) {
                            progressDialog.dismiss();
                            e.printStackTrace();
                        }


                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialog.dismiss();
                VolleyLog.e("Error: ", error.getMessage());
            }
        });
        queue.add(req);
    }

       public void pdflink(){
           final ProgressDialog progressDialog = ProgressDialog.show(this, null, null, true);
           progressDialog.setContentView(R.layout.custom_loader);
           progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
           Retrofit retrofit = new Retrofit.Builder()
                   .baseUrl(ServerUtils.Base_url + "downloadPdf" + "/" + str_testID + "/" + CommonUtils.userID+"/")
                   .addConverterFactory(GsonConverterFactory.create())
                   .build();

           APIService service = retrofit.create(APIService.class);
           Call<Downloadpojo> call = service.listRepos();
           call.enqueue(new Callback<Downloadpojo>() {
               @Override
               public void onResponse(Call<Downloadpojo> call, retrofit2.Response<Downloadpojo> response) {
                   //hiding progress dialog

                   if (response.isSuccessful())
                   {
                       Log.d("@@response##", String.valueOf(response));
                       progressDialog.dismiss();
                       openURL(response.body().getDownloadLink().toString());
                   }
                   else
                   {
                       Toast.makeText(ViewResultActivity.this, response.body().getMessage(), Toast.LENGTH_LONG).show();
                   }

               }
//





           @Override
           public void onFailure(Call<Downloadpojo> call, Throwable t) {
               progressDialog.dismiss();
               Log.d("@@response##1", String.valueOf(t));
               Toast.makeText(ViewResultActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
           }
       });
}
    private void openURL(String s) {
        Uri uri = Uri.parse(s);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "text/html");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

}

