package com.example.tjx.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.net.HttpURLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


public class MainActivity extends Activity implements View.OnClickListener {

    EditText txt_UserName, txt_UserPW;
    Button btn_Login;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        TextView tv= new TextView(this);
//        tv.setText("Hello, Android - by hand");
//        setContentView(tv);
        btn_Login = (Button) findViewById(R.id.btn_Login);
        txt_UserName = (EditText) findViewById(R.id.txt_UserName);
        txt_UserPW = (EditText) findViewById((R.id.txt_UserPW));

        btn_Login.setOnClickListener(this);
        doTrustToCertificates();
        CookieHandler.setDefault(new CookieManager());
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_Login) {
            String uname = txt_UserName.getText().toString();
            String upassword = txt_UserPW.getText().toString();
//            System.out.println("@@@@@@@@@@@@@@@\n" +
//                    "The Portal ID is: " + uname + "\n" +
//                    "The Password is: " + upassword + "\n" +
//                    "@@@@@@@@@@@@@@@");
//
//            Intent intent = new Intent(getBaseContext(), CourseListActivity.class);
//            ArrayList<String> cname = new ArrayList<String>();
//            ArrayList<String> cteachers = new ArrayList<String>();
//
//            //**********Sample Data*********//
//            cname.add("c1");
//            cteachers.add("t1");
//            cname.add("c2");
//            cteachers.add("t2");
//            cname.add("c3");
//            cteachers.add("t3");
//            cname.add("c4");
//            cteachers.add("t4");
//            //**********Sample Data*********//
//
//            intent.putStringArrayListExtra("CourseName", cname);
//            intent.putStringArrayListExtra("Teachers", cteachers);
//            startActivity(intent);
            connect(uname,upassword);
        }

    }

    public String ReadBufferedHTML(BufferedReader reader, char[] htmlBuffer, int bufSz) throws java.io.IOException {
        htmlBuffer[0] = '\0';
        int offset = 0;
        do {
            int cnt = reader.read(htmlBuffer, offset, bufSz - offset);  //这是在干嘛?
            if (cnt > 0) {
                offset += cnt;
            } else {
                break;
            }
        } while (true);
        return new String(htmlBuffer);
    }

    public String getMoodleFirstPage(String userName, String userPW) {
        HttpsURLConnection conn_portal = null;
        URLConnection conn_moodle = null;

        final int HTML_BUFFER_SIZE = 2 * 1024 * 1024;
        char htmlBuffer[] = new char[HTML_BUFFER_SIZE];

        final int HTTPCONNECTION_TYPE = 0;
        final int HTTPSCONNECTION_TYPE = 1;
        int moodle_conn_type = HTTPCONNECTION_TYPE;

        try {
            /////////////////////////////////// HKU portal //////////////////////////////////////
            URL url_portal = new
                    URL("https://hkuportal.hku.hk/cas/login?service=http://moodle.hku.hk/login/index.php?authCAS=CAS&username="
                    + userName + "&password=" + userPW);
            conn_portal = (HttpsURLConnection) url_portal.openConnection();

            BufferedReader reader_portal = new BufferedReader(new InputStreamReader(conn_portal.getInputStream()));
            String HTMLSource = ReadBufferedHTML(reader_portal, htmlBuffer, HTML_BUFFER_SIZE);
            int ticketIDStartPosition = HTMLSource.indexOf("ticket=") + 7;
            String ticketID = HTMLSource.substring(ticketIDStartPosition, HTMLSource.indexOf("\";", ticketIDStartPosition));
            reader_portal.close();
            /////////////////////////////////// HKU portal //////////////////////////////////////

            /////////////////////////////////// Moodle //////////////////////////////////////
            URL url_moodle = new URL("http://moodle.hku.hk/login/index.php?authCAS=CAS&ticket=" + ticketID);
            conn_moodle =  url_moodle.openConnection();
            ((HttpURLConnection)conn_moodle).setInstanceFollowRedirects(true);

            BufferedReader reader_moodle = new BufferedReader(new InputStreamReader(conn_moodle.getInputStream()));

            /// handling redirects to HTTPS protocol
            while(true) {
                String redirect_moodle = conn_moodle.getHeaderField("Location");
                if (redirect_moodle != null) {
                    URL new_url_moodle = new URL(url_moodle, redirect_moodle);
                    if(moodle_conn_type == HTTPCONNECTION_TYPE) {
                        ((HttpURLConnection) conn_moodle).disconnect();
                    } else {
                        ((HttpsURLConnection) conn_moodle).disconnect();
                    }
                    conn_moodle =  new_url_moodle.openConnection();
                    if(new_url_moodle.getProtocol().equals("http")) {
                        moodle_conn_type = HTTPCONNECTION_TYPE;
                        ((HttpURLConnection)conn_moodle).setInstanceFollowRedirects(true);
                    } else {
                        moodle_conn_type = HTTPSCONNECTION_TYPE;
                        ((HttpsURLConnection)conn_moodle).setInstanceFollowRedirects(true);
                    }

                    url_moodle = new_url_moodle;

                    //String cookie = conn_moodle.getHeaderField("Set-Cookie");
                    //if (cookie != null) {
                    //    conn_moodle2.setRequestProperty("Cookie", cookie);
                    //}
                    reader_moodle = new BufferedReader(new InputStreamReader(conn_moodle.getInputStream()));
                } else {
                    break;
                }
            }

            HTMLSource = ReadBufferedHTML(reader_moodle, htmlBuffer, HTML_BUFFER_SIZE);
            reader_moodle.close();
            return HTMLSource;
            /////////////////////////////////// Moodle //////////////////////////////////////

        }catch (Exception e){
            return "Fail to login";
        }finally{
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            if (conn_portal != null){
                conn_portal.disconnect();
            }
            if (conn_moodle != null){
                if(moodle_conn_type == HTTPCONNECTION_TYPE) {
                    ((HttpURLConnection) conn_moodle).disconnect();
                } else {
                    ((HttpsURLConnection) conn_moodle).disconnect();
                }

            }
        }
    }

    // trusting all certificate
    public void doTrustToCertificates() {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers()
                    {
                        return null;
                    }
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType)
                    {
                    }
                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType)
                    {
                    }
                }
        };

        try {
            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    protected void alert(String title, String mymessage){
        new AlertDialog.Builder(this)
                .setMessage(mymessage)
                .setTitle(title)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton){}
                        }
                )
                .show();
    }


    public void parse_HTML_Source_and_Switch_Activity( String HTMLsource ){
        Pattern p_coursename = Pattern.compile("<h3 class=\"coursename\">.*?>(.*?)</a>");
        Matcher m_course = p_coursename.matcher(HTMLsource);

        ArrayList<String> cname = new ArrayList<String>();
        ArrayList<String> cteachers = new ArrayList<String>();

        while(m_course.find()){
            String course_name =  m_course.group(1);
            cname.add(course_name);
        }

        Pattern p_teachercandidates = Pattern.compile("<ul class=\"teachers\"><li>Teacher(.*?)</ul>");
        Matcher m_teachercandidates = p_teachercandidates.matcher(HTMLsource);

        while(m_teachercandidates.find()){
            String string_teachername = m_teachercandidates.group(1);
            int nameStartPosition = string_teachername.indexOf(">")+1;
            int nameEndPosition = string_teachername.indexOf("</a>");
            String teacher_name = string_teachername.substring(nameStartPosition, nameEndPosition);
            cteachers.add(teacher_name);
        }

        // Some courses do not list teachers
        for (int i=0;i<=cname.size()-cteachers.size();i++){
            cteachers.add("None");
        }

        Intent intent = new Intent(getBaseContext(), CourseListActivity.class);
        intent.putStringArrayListExtra("CourseName", cname);
        intent.putStringArrayListExtra("Teachers", cteachers);
        startActivity(intent);
    }

    public void connect( final String userName, final String userPW ){
        final ProgressDialog pdialog = new ProgressDialog(this);

        pdialog.setCancelable(false);
        pdialog.setMessage("Logging in ...");
        pdialog.show();

        AsyncTask<String, Void, String> task = new AsyncTask<String, Void, String>() {
            boolean success;
            String moodlePageContent;

            @Override
            protected String doInBackground(String... arg0) {
                // TODO Auto-generated method stub
                success = true;
                moodlePageContent = getMoodleFirstPage(userName, userPW);
                if( moodlePageContent.equals("Fail to login") )
                    success = false;

                return null;
            }

            @Override
            protected void onPostExecute(String result) {
                if (success) {
                    parse_HTML_Source_and_Switch_Activity( moodlePageContent );
                } else {
                    alert( "Error", "Fail to login" );
                }
                pdialog.hide();
            }

        }.execute("");
    }
}
