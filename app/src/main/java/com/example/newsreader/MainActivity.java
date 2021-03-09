package com.example.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> contents = new ArrayList<>();
    ArrayAdapter<String> arrayAdapter;
    SQLiteDatabase articleDB;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        articleDB = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);
        articleDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId, INTEGER, title VARCHAR, content VARCHAR)");


        DownloadTask task = new DownloadTask();

        try {

            //task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");

        }catch (Exception e){

            e.printStackTrace();

        }

        ListView listView = (ListView) findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, titles);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent intent = new Intent(getApplicationContext(), WebActivity.class );
                intent.putExtra("contents", contents.get(position));

                startActivity(intent);

            }
        });

        updateListView();

    }

    public void updateListView(){

        Cursor c = articleDB.rawQuery("SELECT * FROM articles", null);

        int contentIndex = c.getColumnIndex("content");
        int titleIndex = c.getColumnIndex("title");

        while(c.moveToFirst()){

            titles.clear();
            contents.clear();

            do {

                titles.add(c.getString(titleIndex));
                contents.add(c.getString(contentIndex));

            }while(c.moveToNext());

            arrayAdapter.notifyDataSetChanged();

        }

    }

    public class DownloadTask extends AsyncTask<String, Void, String>{


        @Override
        protected String doInBackground(String... urls) {

            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;

            try {

                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();

                InputStream inputStream = urlConnection.getInputStream();

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

                int data = inputStreamReader.read();

                while(data != -1){

                    char current = (char) data;
                    result += current;
                    data = inputStreamReader.read();

                }

                JSONArray jsonArray = new JSONArray(result);

                int numberofitems = 20;

                if( jsonArray.length() < 20 ){

                    numberofitems = jsonArray.length();

                }

                articleDB.execSQL("DELETE FROM articles ");

                for(int i = 0; i < numberofitems; i++){

                    String articleid = jsonArray.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleid + ".json?print=pretty\n");

                    urlConnection = (HttpURLConnection) url.openConnection();

                    inputStream = urlConnection.getInputStream();

                    inputStreamReader = new InputStreamReader(inputStream);

                    data = inputStreamReader.read();

                    String ArticleInfo = "";

                    while(data != -1){

                        char current = (char) data;
                        ArticleInfo += current;
                        data = inputStreamReader.read();

                    }

                    JSONObject jsonObject = new JSONObject(ArticleInfo);

                    if( !jsonObject.isNull("title") && !jsonObject.isNull("url")){

                        String articleTitle = jsonObject.getString("title");
                        String articleUrl = jsonObject.getString("url");

                        url = new URL(articleUrl);
                        urlConnection = (HttpURLConnection) url.openConnection();

                        inputStream = urlConnection.getInputStream();

                        inputStreamReader = new InputStreamReader(inputStream);

                        data = inputStreamReader.read();

                        String ArticleContent = "";

                        while(data != -1){

                            char current = (char) data;
                            ArticleContent += current;
                            data = inputStreamReader.read();

                        }

                        Log.i("HTML", ArticleContent);

                        String sql = "INSERT INTO articles ( articleId, title, content) VALUES ( ?, ?, ?) ";

                        SQLiteStatement sqLiteStatement = articleDB.compileStatement(sql);

                        sqLiteStatement.bindString(1, articleid);
                        sqLiteStatement.bindString(2, articleTitle);
                        sqLiteStatement.bindString(3, ArticleContent);

                        sqLiteStatement.execute();

                    }

                }

                Log.i("URL Content", result);

                return result;



            }catch (Exception e){

                e.printStackTrace();

            }


            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();

        }
    }
}
