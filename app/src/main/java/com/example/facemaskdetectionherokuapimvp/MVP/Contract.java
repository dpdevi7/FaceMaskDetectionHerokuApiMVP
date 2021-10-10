package com.example.facemaskdetectionherokuapimvp.MVP;

import android.graphics.Bitmap;

import com.example.facemaskdetectionherokuapimvp.Retrofit.APIInterface;
import com.example.facemaskdetectionherokuapimvp.Utils.BoxObject;

import java.io.File;
import java.util.List;

import okhttp3.ResponseBody;

public class Contract {
    public interface View{
        void showProgress();
        void hideProgress(boolean success);
        void setDetectionBitmap(Bitmap bitmap);
    }

    public interface Presenter{
        void callFaceDetectionService(APIInterface apiInterface, File file, Bitmap bitmap);
        void getDetectionBoxes(String responseBody, File file, Bitmap bitmap);
        void getBoxedBitmap(List<BoxObject> boxObjects, File file, Bitmap bitmap);
    }
}
