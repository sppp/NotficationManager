package com.example.user.notficationmanager;


import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class UserSurvey  {

    public String url = "https://toimisto.zzz.fi/survey.php";
    public Answer answer;
    public Questionary questionary;

    public class JSONizable {

        public String name;
        public HashMap<String,String> myKeyValues;

        public JSONizable() {
            myKeyValues = new HashMap<String, String>();
        }

        public void Put(String key, String value) {
            myKeyValues.put(key, value);
        }

        public String Get(String key) {
            return myKeyValues.get(key);
        }

        public void Load(JSONObject json){
            Iterator<?> json_keys = json.keys();

            while( json_keys.hasNext() ){
                String json_key = (String)json_keys.next();
                try {
                    System.out.println(json_key + ": " + json.getString(json_key));
                    myKeyValues.put(json_key, json.getString(json_key));
                }
                catch (JSONException e) {
                    System.out.println("JSONizable.Load exception with key " + json_key);
                }
            }
        }

        public JSONObject Store() {
            JSONObject jsonObject = new JSONObject();
            Iterator<Map.Entry<String,String>> it = myKeyValues.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String,String> entry = it.next();
                try {
                    jsonObject.put(entry.getKey(), entry.getValue());
                }
                catch (JSONException e) {
                    System.out.println("JSONizable.Store exception with key " + entry.getKey());
                    return null;
                }
            }
            return jsonObject;
        }
    }

    public class Questionary extends JSONizable {

    }

    public class Answer extends JSONizable {

    }


    public boolean GetQuestionary() {

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("action", "questionary");

        String questionary_json = performPostCall(url, params);
        Log.i("GetQuestionary", questionary_json);

        // Try to decode JSON and load it to Questionary object
        try {
            JSONObject json_object = new JSONObject(questionary_json);
            questionary.Load(json_object);
        }
        catch (JSONException e) {
            Log.i("GetQuestionary", "JSONException");
            return false;
        }
        return true;
    }

    public boolean SendAnswer() {

        // Encode JSON
        JSONObject json_object = answer.Store();
        if (json_object == null) return false;

        String answer_json = json_object.toString();
        Log.i("UserSurvey.SendAnswer", answer_json);

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("action", "answer");
        params.put("answer", answer_json);

        String questionary_json = performPostCall(url, params);
        Log.i("UserSurvey.SendAnswer", "returned: " + questionary_json);

        return true;
    }

    public String  performPostCall(String requestURL,
                                   HashMap<String, String> postDataParams) {

        URL url;
        String response = "";
        try {
            url = new URL(requestURL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);


            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(getPostDataString(postDataParams));

            writer.flush();
            writer.close();
            os.close();
            int responseCode=conn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line=br.readLine()) != null) {
                    response+=line;
                }
            }
            else {
                response="";

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }
}
