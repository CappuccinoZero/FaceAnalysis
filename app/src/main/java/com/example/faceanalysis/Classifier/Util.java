package com.example.faceanalysis.Classifier;

import android.app.Activity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Util {
    public static ArrayList<String> loadModelList(Activity activity,String path) {
        ArrayList<String> labels =new ArrayList<String>();
        try {
            BufferedReader reader =new BufferedReader(new InputStreamReader(activity.getAssets().open(path)));
            String line;
            while (((line = reader.readLine())!=null)) {
                labels.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return labels;
    }
}
