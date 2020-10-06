package com.exam.softconect.Activity;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.exam.softconect.retrofit.APIUrl;
import com.exam.softconect.retrofit.downloadpdf;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
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
    TextView txt_test_name, txt_total_question, txt_total_marks, txt_total_correct, txt_total_incorrect, txt_not_attempted,txt_pdf_download;
    String dest_file_path = "result.pdf";
    int downloadedSize = 0, totalsize;
    float per = 0;
    String download_file_url = "https://www.softconect.com/media/resultPdf/result.pdf";
    String TAG="@@method";
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

        resultList = new ArrayList<>();
        txt_pdf_download=findViewById(R.id.txt_pdf_download);
        adapter = new ResultViewAdapter(this, resultList);
        recyclearview.setAdapter(adapter);

        //call method
        getlink();
        getResult();


        //set listner
        img_back.setOnClickListener(this);
        txt_pdf_download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                SharedPreferences pref = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
//                String download_link=pref.getString("link","0");
//                Log.d(TAG, download_link);
//                new Downloader().execute(download_link);

                downloadAndOpenPDF();
            }
        });
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
    public void getlink() {

        //show process dialog
        final ProgressDialog progressDialog = ProgressDialog.show(this, null, null, true);
        progressDialog.setContentView(R.layout.custom_loader);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        RequestQueue queue = Volley.newRequestQueue(this);

        Log.e(">>downlod_linkurl",ServerUtils.Base_url + "downloadPdf" + "/" + str_testID + "/" + CommonUtils.userID+" ");
        String url="https://www.softconect.com/api/downloadPdf/1354/3093";
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET,
                ServerUtils.Base_url + "downloadPdf" + "/" + str_testID + "/" + CommonUtils.userID,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        String status;

                        try {

                            status = response.getString("status");



                            SharedPreferences sharedPreferences
                                    = getSharedPreferences("MySharedPref",
                                    MODE_PRIVATE);

                            SharedPreferences.Editor myEdit
                                    = sharedPreferences.edit();

                            myEdit.putString(
                                    "link",
                                    response.getString("download_link"));

                            myEdit.commit();

                            Log.e(">>",status+"_"+response.getString("download_link"));

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
//    public void storeurl() {
//        final ProgressDialog progressDialog = ProgressDialog.show(this, null, null, true);
//        progressDialog.setContentView(R.layout.custom_loader);
//        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//
//
//        Retrofit retrofit = new Retrofit.Builder()
//                .baseUrl(APIUrl.BASE_URL)
//                .addConverterFactory(GsonConverterFactory.create())
//                .build();
//
//        //Defining retrofit api service
//        APIService service = retrofit.create(APIService.class );
//
//
//        SharedPreferences pref = getSharedPreferences("Mypref", Context.MODE_PRIVATE);
//        String testid=pref.getString("test_id","0");
//        Log.d(TAG, testid);
//        Call<downloadpdf> call = service.getlink( testid,CommonUtils.userID);
//        Log.d("Geting_link",testid+CommonUtils.userID);
//        Log.d("Geting_link", String.valueOf(call));
//        call.enqueue(new Callback<downloadpdf>() {
//            @Override
//            public void onResponse(Call<downloadpdf> call, retrofit2.Response<downloadpdf> response) {
//                //hiding progress dialog
//                progressDialog.dismiss();
//
//                Log.e("@@link_data",response.body().getDownloadLink()+" ");
//                    SharedPreferences pref = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
//                    SharedPreferences.Editor editor = pref.edit();
//                    editor.putString("link",response.body().getDownloadLink()+" ");
//                    Log.d("Geting_link", response.body().getDownloadLink()+" ");
//                    editor.commit();
//                    editor.apply();
//
//                }
//
//
//
//
//            @Override
//            public void onFailure(Call<downloadpdf> call, Throwable t) {
//
//            }
//
//
//        });
//
//    }

    void downloadAndOpenPDF() {
        final String download_link;
        SharedPreferences sh
                = getSharedPreferences("MySharedPref",
                MODE_PRIVATE);

        download_link = sh.getString("link","_");

        if(download_link==null)
        {
                    Toast.makeText(ViewResultActivity.this,"Invalid Response",Toast.LENGTH_LONG).show();
            Log.e(">>","get null value");


        }
        new Thread(new Runnable() {
            public void run() {


               



                try {
                    String storage = Environment.getExternalStorageDirectory().toString() + download_link;
                    File file = downloadFile(storage);
                    Uri uri;
                    if (Build.VERSION.SDK_INT < 24) {
                        uri = Uri.fromFile(file);
                    } else {
                        uri = Uri.parse(download_link); // My work-around for new SDKs, doesn't work in Android 10.
                    }
                    Intent viewFile = new Intent(Intent.ACTION_VIEW);
                    viewFile.setDataAndType(uri, "text/plain");
                    viewFile.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);


                    startActivity(viewFile);

                } catch (ActivityNotFoundException e) {
//                    tv_loading
//                            .setError("PDF Reader application is not installed in your device");
                }
            }
        }).start();

    }

    File downloadFile(String dwnload_file_path) {
        File file = null;
        try {

            URL url = new URL(dwnload_file_path);
            HttpURLConnection urlConnection = (HttpURLConnection) url
                    .openConnection();

            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(true);

            // connect
            urlConnection.connect();

            // set the path where we want to save the file
            File SDCardRoot = Environment.getExternalStorageDirectory();
            // create a new file, to save the downloaded file
            file = new File(SDCardRoot, dest_file_path);

            FileOutputStream fileOutput = new FileOutputStream(file);

            // Stream used for reading the data from the internet
            InputStream inputStream = urlConnection.getInputStream();

            // this is the total size of the file which we are
            // downloading
            totalsize = urlConnection.getContentLength();
            setText("Starting PDF download...");

            // create a buffer...
            byte[] buffer = new byte[1024 * 1024];
            int bufferLength = 0;

            while ((bufferLength = inputStream.read(buffer)) > 0) {
                fileOutput.write(buffer, 0, bufferLength);
                downloadedSize += bufferLength;
                per = ((float) downloadedSize / totalsize) * 100;
                setText("Total PDF File size  : "
                        + (totalsize / 1024)
                        + " KB\n\nDownloading PDF " + (int) per
                        + "% complete");
            }
            // close the output stream when complete //
            fileOutput.close();
            setText("Download Complete. Open PDF Application installed in the device.");

        } catch (final MalformedURLException e) {
            setTextError("Some error occured. Press back and try again.",
                    Color.RED);
        } catch (final IOException e) {
            setTextError("Some error occured. Press back and try again.",
                    Color.RED);
        } catch (final Exception e) {
            setTextError(
                    "Failed to download image. Please check your internet connection.",
                    Color.RED);
        }
        return file;
    }
    void setTextError(final String message, final int color) {
        runOnUiThread(new Runnable() {
            public void run() {
//                tv_loading.setTextColor(color);
//                tv_loading.setText(message);
            }
        });
    }

    void setText(final String txt) {
        runOnUiThread(new Runnable() {
            public void run() {
//                tv_loading.setText(txt);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        getlink();
    }
}



// kiya kr rhe ho sir
// TODO: build ker k mobile m sand ker do
// aap apne mobile mai install kr skte ho kiya
// TODO: aaj i think kese k pass koi kaam nahe h
