package com.lxj.crashtest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;


import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;
import android.os.Process;
import android.util.Log;

public class CrashHandler implements UncaughtExceptionHandler {
	private static final String TAG = "CrashHandler";
    private static final boolean DEBUG = true;

    private static final String PATH = Environment.getExternalStorageDirectory().getPath() + "/CrashTest/log/";
    private static final String FILE_NAME = "crash";
    private static final String FILE_NAME_SUFFIX = ".trace";

    private static CrashHandler sInstance = new CrashHandler();
    private UncaughtExceptionHandler mDefaultCrashHandler;
    private Context mContext;
    private HttpParams httpParams;  
    private HttpClient httpClient;  
    private StringBuilder sb = new StringBuilder();

private static final String URIPATH_STRING = "http://192.168.88.20:8080/smdb/mobile/setting/getAndSaveCrash.action";
    
    private CrashHandler() {
    }

    public static CrashHandler getInstance() {
        return sInstance;
    }

    public void init(Context context) {
        mDefaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
        mContext = context.getApplicationContext();
    }

    /**
     * 这个是最关键的函数，当程序中有未被捕获的异常，系统将会自动调用#uncaughtException方法
     * thread为出现未捕获异常的线程，ex为未捕获的异常，有了这个ex，我们就可以得到异常信息。
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        try {
            //导出异常信息到SD卡中
            dumpExceptionToSDCard(ex);
            sb.append(ex);
            uploadExceptionToServer(sb.toString());
            //这里可以通过网络上传异常信息到服务器，便于开发人员分析日志从而解决bug
        } catch (IOException e) {
            e.printStackTrace();
        }

        ex.printStackTrace();

        //如果系统提供了默认的异常处理器，则交给系统去结束我们的程序，否则就由我们自己结束自己
        if (mDefaultCrashHandler != null) {
            mDefaultCrashHandler.uncaughtException(thread, ex);
        } else {
            Process.killProcess(Process.myPid());
        }

    }

    private void dumpExceptionToSDCard(Throwable ex) throws IOException {
        //如果SD卡不存在或无法使用，则无法把异常信息写入SD卡
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            if (DEBUG) {
                Log.w(TAG, "sdcard unmounted,skip dump exception");
                return;
            }
        }

        File dir = new File(PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        long current = System.currentTimeMillis();
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(current));
        File file = new File(PATH + FILE_NAME + time + FILE_NAME_SUFFIX);

        try {
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            pw.println(time);
            dumpPhoneInfo(pw);
            pw.println();
            ex.printStackTrace(pw);
            pw.close();
        } catch (Exception e) {
            Log.e(TAG, "dump crash info failed");
        }
    }

    private void dumpPhoneInfo(PrintWriter pw) throws NameNotFoundException {
        PackageManager pm = mContext.getPackageManager();
        PackageInfo pi = pm.getPackageInfo(mContext.getPackageName(), PackageManager.GET_ACTIVITIES);
        pw.print("App Version: ");
        pw.print(pi.versionName);
        pw.print('_');
        pw.println(pi.versionCode);
        sb.append("App Version: "+pi.versionName+"_"+pi.versionCode+"\n");
        //android版本号
        pw.print("OS Version: ");
        pw.print(Build.VERSION.RELEASE);
        pw.print("_");
        pw.println(Build.VERSION.SDK_INT);
        sb.append("OS Version: "+Build.VERSION.RELEASE+"_"+Build.VERSION.SDK_INT+"\n");
        //手机制造商
        pw.print("Vendor: ");
        pw.println(Build.MANUFACTURER);
        sb.append("Vendor: "+Build.MANUFACTURER+"\n");
        //手机型号
        pw.print("Model: ");
        pw.println(Build.MODEL);
        sb.append("Model: "+Build.MODEL+"\n");
        //cpu架构
        pw.print("CPU ABI: ");
        pw.println(Build.CPU_ABI);
        sb.append("CPU ABI: "+Build.CPU_ABI+"\n");
    }

    private void uploadExceptionToServer(String info) {
      //TODO Upload Exception Message To Your Web Server
    	List<NameValuePair> params = new ArrayList<NameValuePair>();  
        params.add(new BasicNameValuePair("logInfo", info)); 
        getHttpClient();
        doPost(URIPATH_STRING, params);
    }
    /* 上传文件至Server的方法 */
    public String doPost(String url, List<NameValuePair> params) {  
        /* 建立HTTPPost对象 */  
        HttpPost httpRequest = new HttpPost(url);  
        String strResult = "doPostError";  
        try {  
            /* 添加请求参数到请求对象 */  
            httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));  
            /* 发送请求并等待响应 */  
            HttpResponse httpResponse = httpClient.execute(httpRequest);  
            /* 若状态码为200 ok */  
            if (httpResponse.getStatusLine().getStatusCode() == 200) {  
                /* 读返回数据 */  
                strResult = EntityUtils.toString(httpResponse.getEntity());  
            } else {  
                strResult = "Error Response: "  
                        + httpResponse.getStatusLine().toString();  
            }  
        } catch (ClientProtocolException e) {  
            strResult = e.getMessage().toString();  
            e.printStackTrace();  
        } catch (IOException e) {  
            strResult = e.getMessage().toString();  
            e.printStackTrace();  
        } catch (Exception e) {  
            strResult = e.getMessage().toString();  
            e.printStackTrace();  
        }  
        Log.v("strResult", strResult);  
        return strResult;  
    }  
    public HttpClient getHttpClient() {  
        // 创建 HttpParams 以用来设置 HTTP 参数（这一部分不是必需的）  
        this.httpParams = new BasicHttpParams();  
        // 设置连接超时和 Socket 超时，以及 Socket 缓存大小  
        HttpConnectionParams.setConnectionTimeout(httpParams, 20 * 1000);  
        HttpConnectionParams.setSoTimeout(httpParams, 20 * 1000);  
        HttpConnectionParams.setSocketBufferSize(httpParams, 8192);  
        // 设置重定向，缺省为 true  
        HttpClientParams.setRedirecting(httpParams, true);  
        // 设置 user agent  
        String userAgent = "Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN; rv:1.9.2) Gecko/20100115 Firefox/3.6";  
        HttpProtocolParams.setUserAgent(httpParams, userAgent);  
        // 创建一个 HttpClient 实例  
        // 注意 HttpClient httpClient = new HttpClient(); 是Commons HttpClient  
        // 中的用法，在 Android 1.5 中我们需要使用 Apache 的缺省实现 DefaultHttpClient  
        httpClient = new DefaultHttpClient(httpParams);  
        return httpClient;  
    }  

}
