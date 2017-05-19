package interview.operr.com.yelper;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Credentials;
import android.os.AsyncTask;

import com.google.android.gms.auth.api.credentials.Credential;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Authenticator;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;

/**
 * Created by Sairaj on 5/18/2017.
 */

public class Utils {

    public static ArrayList authCredentials(JSONObject jsonObject) throws JSONException
    {
        String clientSecret = jsonObject.getString("ClientSecret");
        String clientID = jsonObject.getString("clientID");
        ArrayList<String> credentials = new ArrayList<>();
        credentials.add(clientID);
        credentials.add(clientSecret);
        return credentials;

    }

    public static ArrayList processResult(JSONObject jsonObject) throws JSONException
    {
        JSONArray array = jsonObject.getJSONArray("businesses");
        ArrayList<ArrayList<String>> places = new ArrayList<>();
        for(int i = 0; i < array.length(); i++)
        {
            ArrayList<String> place = new ArrayList<>();
            JSONObject object = array.getJSONObject(i);
            String name = object.getString("name");
            String price = object.getString("price");
            String phone = object.getString("phone");
            String rating = object.getString("rating");
            JSONObject address_object = object.getJSONObject("location");
            StringBuilder builder = new StringBuilder();
            builder.append(address_object.getString("address1"));
            builder.append(", ");
            builder.append(address_object.getString("city"));
            builder.append(", ");
            builder.append(address_object.getString("state"));
            builder.append(", ");
            builder.append(address_object.getString("zip_code"));
            String address = builder.toString();
            JSONObject location_object = object.getJSONObject("coordinates");
            String latitude = location_object.getString("latitude");
            String longitude = location_object.getString("longitude");
            String image_url = object.getString("image_url");
            place.add(name);
            place.add(rating);
            place.add(address);
            place.add(phone);
            place.add(price);
            place.add(latitude);
            place.add(longitude);
            place.add(image_url);
            places.add(place);

        }

        return places;
    }
    public static class getProcessor extends AsyncTask<String, Void, ArrayList>
    {
        Context context;
        public interface TaskListener{
            public void onFinished(ArrayList<String> result, final Context context);
            public void onComplete(ArrayList<ArrayList<String>> result, final Context context);
        }
        private final TaskListener taskListener;
        getProcessor(Context context, TaskListener listener)
        {
            this.context = context;
            taskListener = listener;
        }
        @Override
        protected ArrayList doInBackground(final String... params) {
            String URL = params[0];
            OkHttpClient httpClient = new OkHttpClient();
            ArrayList output1;
            ArrayList<ArrayList<String>> output2;
            String option = params[1];
            try
            {

                switch(option)
                {
                    case "0":
                        Request request = new Request.Builder().url(URL).build();
                        Response response = httpClient.newCall(request).execute();
                        String body = response.body().string();
                        JSONObject jsonObject = new JSONObject(body);
                        output1 = authCredentials(jsonObject);
                        return output1;
                    case "1":
                        System.out.println("Requesting restaurants");
                        Request request1 = new Request.Builder().url(URL).header("Accept","application/json").header("Authorization","Bearer "+params[2]).build();
                        Response response1 = httpClient.newCall(request1).execute();
                        String body1 = response1.body().string();
                        //System.out.println(body1);
                        JSONObject jsonObject1 = new JSONObject(body1);
                        output2 = processResult(jsonObject1);
                        return output2;
                    default:
                        System.out.println("error");
                        break;
                }

            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
            catch(JSONException e)
            {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList arrayList) {
            super.onPostExecute(arrayList);

            if(taskListener!=null)
            {
                taskListener.onFinished(arrayList,context);
                taskListener.onComplete(arrayList,context);
            }

        }
    }

    public static ArrayList accessToken(JSONObject jsonObject) throws JSONException
    {
        String access_token = jsonObject.getString("access_token");
        String token_type = jsonObject.getString("token_type");
        String expires = jsonObject.getString("expires_in");

        ArrayList<String> tokens = new ArrayList<>();
        tokens.add(access_token);
        tokens.add(token_type);
        tokens.add(expires);
        return tokens;
    }

    public static class postProcessor extends AsyncTask<String, Void, ArrayList>
    {
        Context context;
        public interface TaskListener{
            public void onFinished(ArrayList<String> s, final Context context);
        }
        private final TaskListener taskListener;
        postProcessor(Context context, TaskListener listener)
        {
            this.context = context;
            taskListener = listener;
        }
        @Override
        protected ArrayList doInBackground(String... params) {
            String URL = params[2];
            OkHttpClient httpClient = new OkHttpClient();
            RequestBody form = new FormBody.Builder().add("grant_type","client_credentials").add("client_id",params[0]).add("client_secret",params[1]).build();
            Request request = new Request.Builder().url(URL).post(form).build();
            try
            {
                Response response = httpClient.newCall(request).execute();
                String body = response.body().string();
                JSONObject jsonObject = new JSONObject(body);
                ArrayList output;
                output = accessToken(jsonObject);
                return output;
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
            catch(JSONException e)
            {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList s) {
            super.onPostExecute(s);

            if(taskListener!=null)
            {
                taskListener.onFinished(s,context);
            }

        }
    }
}
