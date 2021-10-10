package com.example.facemaskdetectionherokuapimvp.Utils;

import java.util.HashMap;

public class Constants {

    // 1. base url for facemask detection api
    public static final String detectionBaseUrl = "https://facemask-detection-api.herokuapp.com";

    // 2.
    public final static int REQUEST_IMAGE_CAPTURE = 1002;

    // 2. get class-labels hashmap
    public static HashMap<Integer, String> getClassLabls(){
        HashMap<Integer, String> hashMap = new HashMap<>();
        // 4. populate hashmap
        hashMap.put(0,"background");
        hashMap.put(1,"with mask");
        hashMap.put(2,"without mask");
        hashMap.put(3,"mask weared incorrect");

        return hashMap;
    }

}
