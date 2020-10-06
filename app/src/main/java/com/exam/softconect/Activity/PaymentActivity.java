package com.exam.softconect.Activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.awesomedialog.blennersilva.awesomedialoglibrary.AwesomeSuccessDialog;
import com.awesomedialog.blennersilva.awesomedialoglibrary.interfaces.Closure;
import com.exam.softconect.Helper.CommonUtils;
import com.exam.softconect.R;
import com.exam.softconect.Utils.ServerUtils;
import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class PaymentActivity extends Activity implements PaymentResultListener {
    private static final String TAG = "@@"+PaymentActivity.class.getSimpleName();

    String email,contact,amount,package_name,name_common;
    ProgressDialog progressDoalog;
    Dialog dialog;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_payment);
        progressDoalog = new ProgressDialog(PaymentActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
        progressDoalog.setMax(100);
        progressDoalog.setMessage("Request Submiting");
        progressDoalog.setTitle("Please wait....");

        Intent i=getIntent();
        email=i.getStringExtra("email");
        contact=i.getStringExtra("contact");
        amount=i.getStringExtra("amount");
        package_name=i.getStringExtra("package_name");
        name_common=i.getStringExtra("name_common");


        Log.e("@@payment99",email+" "+contact+" "+amount+" "+package_name+" "+name_common);
        Log.e("@@108",CommonUtils.userID+" 1 "+CommonUtils.packageID);

        Checkout.preload(getApplicationContext());
        startPayment();

        // Payment button created by you in XML layout
        Button button = (Button) findViewById(R.id.btn_pay);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPayment();
            }
        });
    }

    public void startPayment() {

        final Activity activity = this;

        final Checkout co = new Checkout();

        try {
            JSONObject options = new JSONObject();
            options.put("name", name_common);
            options.put("description", "description");
            //You can omit the image option to fetch the image from dashboard
            options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.png");
            options.put("currency", "INR");

            Integer amt= Integer.valueOf(amount);
            amt=amt*100;
            options.put("amount", amt.toString());

            JSONObject preFill = new JSONObject();
            preFill.put("email", email);
            preFill.put("contact", contact);

            options.put("prefill", preFill);

            co.open(activity, options);
        } catch (Exception e) {
            Toast.makeText(activity, "Error in payment: " + e.getMessage(), Toast.LENGTH_SHORT)
                    .show();
            e.printStackTrace();
        }
    }

    /**
     * The name of the function has to be
     * onPaymentSuccess
     * Wrap your code in try catch, as shown, to ensure that this method runs correctly
     */
    @SuppressWarnings("unused")
    @Override
    public void onPaymentSuccess(String razorpayPaymentID) {
        try {
            Toast.makeText(this, "Payment Successful: " + razorpayPaymentID, Toast.LENGTH_SHORT).show();
            Log.e("@@",razorpayPaymentID.toString());
            sendPaymentResponse(PaymentActivity.this, CommonUtils.userID,"1", CommonUtils.packageID);

        } catch (Exception e) {
            Log.e(TAG, "Exception in onPaymentSuccess", e);
            Log.e("@@",e.getMessage().toString());
        }
    }

    /**
     * The name of the function has to be
     * onPaymentError
     * Wrap your code in try catch, as shown, to ensure that this method runs correctly
     */

    @SuppressWarnings("unused")
    @Override
    public void onPaymentError(int code, String response) {
        try {
            Toast.makeText(this, "Payment failed: " + code + " " + response, Toast.LENGTH_SHORT).show();
            Log.e("@@",response.toString());
            Intent intent = new Intent(PaymentActivity.this, PackageActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

        } catch (Exception e) {
            Log.e(TAG, "Exception in onPaymentError", e);
            Log.e("@@",e.getMessage().toString());
        }
    }

    public void sendPaymentResponse(final Activity activity, final String userID, final String response, final String package_id) {

        //show process dialog
        Log.e("@@107",userID+" "+response+" "+package_id);
        final ProgressDialog progressDialog = ProgressDialog.show(PaymentActivity.this, null, null, true);
        progressDialog.setContentView(R.layout.custom_loader);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        try {
            RequestQueue queue = Volley.newRequestQueue(PaymentActivity.this);

            StringRequest request = new StringRequest(Request.Method.POST, ServerUtils.Base_url + ServerUtils.purchaseAuth_url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            // response
                            progressDialog.dismiss();
                            String status, message;
                            Log.e("@@105",response.toString());

                            try {
                                JSONArray jsonArray = new JSONArray(response);
                                JSONObject jsonObject = jsonArray.getJSONObject(0);
                                status = jsonObject.getString("status");

                                if (status.equalsIgnoreCase("1")) {
                                    CommonUtils.packageID = "";

                                    dialog = new AwesomeSuccessDialog(activity)
                                            .setTitle("Success")
                                            .setMessage("Payment Success")
                                            .setCancelable(false)
                                            .setDoneButtonText("Ok")
                                            .setDoneButtonClick(new Closure() {
                                                @Override
                                                public void exec() {
                                                    dialog.dismiss();
                                                    Intent intent = new Intent(PaymentActivity.this, PackageActivity.class);
                                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                    startActivity(intent);
                                                }
                                            })
                                            .show();

                                } else if (status.equalsIgnoreCase("0")) {

                                    message = jsonObject.getString("message");

                                    //Show Error Dialog
                                    CommonUtils.AwesomeErrorDialog(activity, message);
                                }


                            } catch (JSONException e) {
                                e.printStackTrace();
                                Log.e("@@106",e.getMessage().toString());
                                Intent intent = new Intent(PaymentActivity.this, PackageActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // error
                            progressDialog.dismiss();
                            Log.e("@@107",error.getMessage().toString());
                            Intent intent = new Intent(PaymentActivity.this, PackageActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);

                        }
                    }
            ) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("user_id", userID);
                    params.put("response", response);
                    params.put("package_id", package_id);

                    return params;
                }
            };

            queue.add(request);

        } catch (Exception e) {
            Log.e("@@101",e.getMessage().toString());
            Intent intent = new Intent(PaymentActivity.this, PackageActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }
}