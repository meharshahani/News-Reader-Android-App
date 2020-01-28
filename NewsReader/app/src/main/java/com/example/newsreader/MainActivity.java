package com.example.newsreader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
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
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> content = new ArrayList<>();
    ArrayAdapter arrayAdapter;
    SQLiteDatabase articlesDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        articlesDB = this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId INTEGER ,title VARCHAR, content VARCHAR)");

        DownloadTask task = new DownloadTask();
        try {
           // task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }
        catch (Exception e){
                e.printStackTrace();
        }

        ListView listView = (ListView) findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1, titles);
        listView.setAdapter(arrayAdapter);

        titles.add("Top Trending News");

       listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent i = new Intent(getApplicationContext(),ViewArticle.class);
                i.putExtra("content",content.get(position));
                startActivity(i);
            }
        });

        updateListView();

    }


    //to update the app when there is new info
        public void updateListView(){

        //get stuff out of the database, get it into the appropriate arrays ,display it to user
            Cursor c = articlesDB.rawQuery("SELECT * FROM articles",null);

            int contentIndex = c.getColumnIndex("content");
            int titleIndex = c.getColumnIndex("title");

            if(c.moveToFirst()){
                titles.clear();
                content.clear();

                do{
                    //adding title and content to the respective arrays
                    titles.add(c.getString(titleIndex));
                    content.add(c.getString(contentIndex));

                }while(c.moveToNext());

                arrayAdapter.notifyDataSetChanged();
            }
        }

      public class DownloadTask extends AsyncTask<String, Void, String>{

          @Override
          protected String doInBackground(String... urls) {

              URL url;
              HttpURLConnection connection;
              String result = "";

              try{

                  //to get all the article ids
                  url = new URL(urls[0]);
                  connection = (HttpURLConnection)url.openConnection();
                  InputStream in = connection.getInputStream();
                  InputStreamReader reader = new InputStreamReader(in);
                  int data = reader.read();

                  while(data != -1){
                      char current = (char)data;
                      result += current;
                      data = reader.read();
                  }

                  //just working with 20 articles for now
                  JSONArray jsonArray = new JSONArray(result);
                  int numberOfItems = 20;

                  if(jsonArray.length() < 20){
                      numberOfItems = jsonArray.length();
                  }

                  articlesDB.execSQL("DELETE FROM articles");

                  for(int i = 0; i < numberOfItems; i++){

                      String articleId = jsonArray.getString(i);
                      url = new URL("https://hacker-news.firebaseio.com/v0/item/"+ articleId +".json?print=pretty");

                      //to get the articles
                      connection = (HttpURLConnection)url.openConnection();
                      in = connection.getInputStream();
                      reader = new InputStreamReader(in);
                      data = reader.read();
                      String articleInfo = "";

                      while(data != -1){
                          char current = (char)data;
                          articleInfo += current;
                          data = reader.read();
                      }

                      JSONObject jsonObject = new JSONObject(articleInfo);

                      if(!jsonObject.isNull("title") && !jsonObject.isNull("url")) {

                          String articleTitle = jsonObject.getString("title");
                          String articleURL = jsonObject.getString("url");

                          /*Log.i("Article title", articleTitle);
                          Log.i("Article url", articleURL);*/

                          //to get the content/html for the particular title and url
                          url = new  URL(articleURL);
                          connection = (HttpURLConnection) url.openConnection();
                          in = connection.getInputStream();
                          reader = new InputStreamReader(in);
                          data = reader.read();
                          String articleContent = "";

                          while(data != -1){
                              char current = (char) data;
                              articleContent += current;
                              data = reader.read();
                          }
                          Log.i("HTML",articleContent);

                          String sql = "INSERT INTO articles (articleId,title,content) VALUES (?, ?, ?)";
                          SQLiteStatement statement = articlesDB.compileStatement(sql);
                          statement.bindString(1,articleId);
                          statement.bindString(2, articleTitle);
                          statement.bindString(3, articleContent);
                          statement.execute();
                      }
                  }
                  Log.i("URL content",result);
                  return result;
              }
              catch (Exception e){
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

