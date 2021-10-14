package com.example.facemaskdetectionherokuapimvp.MVP;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.example.facemaskdetectionherokuapimvp.Retrofit.APIInterface;
import com.example.facemaskdetectionherokuapimvp.Utils.BoxObject;
import com.example.facemaskdetectionherokuapimvp.Utils.Constants;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetectionPresenter implements Contract.Presenter {
    private Contract.View view;
    private static final String TAG = "DetectionPresenter";

    public DetectionPresenter(Contract.View view){
        this.view = view;
    }



    @Override
    public void callFaceDetectionService(APIInterface apiInterface, File file, Bitmap bitmap) {

        view.showProgress();

        // 1. create RequestBody instance from file
        RequestBody requestFile = RequestBody.create(MediaType.parse(String.valueOf(Uri.fromFile(file))), file);

        // 2. MultipartBody.Part is used to send also the actual file name
        MultipartBody.Part requestBody = MultipartBody.Part.createFormData("filename", file.getName(), requestFile);

        // 3. Call API
        Call<ResponseBody> call = apiInterface.predictMask(requestBody);

        // 4.
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                // 1. get ResponseClass
                try {
                    String responseString = response.body().string();
                    getDetectionBoxes(responseString, file, bitmap);


                } catch (IOException e) {
                    e.printStackTrace();
                    view.hideProgress(false);
                }


            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG, "onFailure: "+t.getLocalizedMessage());
                Log.d(TAG, "onFailure: "+t.getCause());
                Log.d(TAG, "onFailure: "+t.getStackTrace());

                view.hideProgress(false);


            }
        });



    }

    @Override
    public void getDetectionBoxes(String responseBodyString, File file, Bitmap bitmap) {

        try {
            // 1.
            JSONObject root = new JSONObject(responseBodyString);
            // 2.
            JSONObject result = root.optJSONObject("result");
            boolean isSuccess = result.optBoolean("success");
            // 3.
            JSONArray boxes = result.optJSONArray("boxes"); // [[xmin,ymin,xmax,ymax], [xmin,ymin,xmax,ymax] ......]
            JSONArray labels = result.optJSONArray("labels");
            JSONArray scores = result.optJSONArray("scores");
            // 4.
            List<BoxObject> boxObjectList = new ArrayList<>();
            // 5.
            for (int iBox=0, iLabel=0, iScore=0 ;iBox<boxes.length() && iLabel<labels.length() && iScore<scores.length() ;iBox++, iLabel++, iScore++){

                Rect rect = new Rect();
                JSONArray singeBox = boxes.optJSONArray(iBox); // e.g [xmin,ymin,xmax,ymax]

                // choose 311, instead of 320, because bbox was sliding more towards +ve x dir,
                int xScale = (int) Math.floor((bitmap.getWidth() / 311.0));
                int yScale = (int) Math.floor((bitmap.getHeight() / 320.0));

                rect.left = singeBox.optInt(0) * xScale;
                rect.top = singeBox.optInt(1) * yScale;
                rect.right = singeBox.optInt(2) * xScale;
                rect.bottom = singeBox.optInt(3) * yScale;

                int label = labels.optInt(iLabel); // e.g 1
                double score = scores.optDouble(iScore); // e.g 0.99

                BoxObject boxObject = new BoxObject(label, score, rect);
                boxObjectList.add(boxObject);
            }

            Log.d(TAG, "getDetectionBoxes: "+ Arrays.asList(boxObjectList.toArray()));
            // 6.
            getBoxedBitmap(boxObjectList, file, bitmap);



        } catch (JSONException e) {
            e.printStackTrace();
            view.hideProgress(false);
        }

    }

    @Override
    public void getBoxedBitmap(List<BoxObject> boxObjects, File file, Bitmap bitmap) {

        Bitmap bitmapMutable = bitmap.copy(Bitmap.Config.RGB_565,true);

        for (BoxObject boxObject: boxObjects){

            if (boxObject.getScore() > 0.1f){

                Canvas canvas = new Canvas(bitmapMutable);
                // painting options for the rectangles
                Paint paint = new Paint();
                paint.setAlpha(0xA0); // the transparency
                paint.setColor(Color.RED); // color is red
                paint.setStyle(Paint.Style.STROKE); // stroke or fill or ...
                paint.setStrokeWidth(5); // the stroke width
                // The rectangle will be draw / painted from point (0,0) to point (10,20).
                // draw that rectangle on the canvas
                canvas.drawRect(boxObject.getRect(), paint);
                // create Paint Object for Writing text on bbox
                Paint textPaint = new Paint();
                textPaint.setColor(Color.WHITE);
                textPaint.setTextSize((float) 20.9);
                textPaint.setFakeBoldText(true);
                textPaint.setTextSize(100);

                String classLabelString = Constants.getClassLabls().get(boxObject.getClassIndex());
                String message = classLabelString ;// + " " + decimalFormat.format(result.score) + result.rect.toString();

                canvas.drawText(message, boxObject.getRect().left, boxObject.getRect().top, textPaint);

            }
        }

        view.setDetectionBitmap(bitmapMutable);



    }
}
