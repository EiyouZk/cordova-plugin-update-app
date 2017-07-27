package org.apache.cordova.updateapp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;

import com.gli.reader.R;

public class UpdateApp extends CordovaPlugin {


    private String checkPath;

    private int newVerCode;

    private String newVerName;

    private String downloadPath;

    private static final int DOWNLOAD = 1;

    private static final int DOWNLOAD_FINISH = 2;

    private String mSavePath;

    private int progress;

    private boolean cancelUpdate = false;

    private Context mContext;

    private ProgressBar mProgress;
    private Dialog mDownloadDialog;
    
    private CallbackContext mcallbackContext;

    protected static final String LOG_TAG = "UpdateApp";

    @Override
    public boolean execute(String action, JSONArray args,
            final CallbackContext callbackContext) throws JSONException {
        this.mContext = cordova.getActivity();
        if ("checkAndUpdate".equals(action)) {
            this.checkPath = args.getString(0);
            checkAndUpdate();
            return true;
        } else if ("getCurrentVersion".equals(action)) {
            callbackContext.success(this.getCurrentVerCode() + "");
            return true;
        } else if ("getServerVersion".equals(action)) {
            this.checkPath = args.getString(0);
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    if (getServerVerInfo()) {
                        callbackContext.success(newVerCode + "");
                    } else {
                        callbackContext
                                .error("can't connect to the server!please check [checkpath]");
                    }
                }
            });
            return true;
        } else if ("getVersionName".equals(action)) {
            callbackContext.success(this.getCurrentVerName());
            return true;
        }
        return false;
    }

    /**
     * 
     */
    private void checkAndUpdate() {
        Runnable runnable = new Runnable() {
            public void run() {
                if (getServerVerInfo()) {
                    int currentVerCode = getCurrentVerCode();
                    if (newVerCode > currentVerCode) {
                        showNoticeDialog();
                    }
                    else {
                        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, false);
                        pluginResult.setKeepCallback(false);
                        mcallbackContext.sendPluginResult(pluginResult);
                    }
                }
            }
        };
        this.cordova.getThreadPool().execute(runnable);
    }

    /**
     * 
     * 
     * @param context
     * @return
     */
    private int getCurrentVerCode() {
        String packageName = this.mContext.getPackageName();
        int currentVer = -1;
        try {
            currentVer = this.mContext.getPackageManager().getPackageInfo(
                    packageName, 0).versionCode;
        } catch (NameNotFoundException e) {
            Log.d(LOG_TAG, "" + e.toString());
        }
        return currentVer;
    }

    /**
     * 
     * 
     * @param context
     * @return
     */
    private String getCurrentVerName() {
        String packageName = this.mContext.getPackageName();
        String currentVer = "";
        try {
            currentVer = this.mContext.getPackageManager().getPackageInfo(
                    packageName, 0).versionName;
        } catch (NameNotFoundException e) {
            Log.d(LOG_TAG, "" + e.toString());
        }
        return currentVer;
    }

    /**
     *
     * 
     * @param path
     * @return
     * @throws Exception
     */
    private boolean getServerVerInfo() {
        try {
            StringBuilder verInfoStr = new StringBuilder();
            URL url = new URL(checkPath);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.connect();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    conn.getInputStream(), "UTF-8"), 8192);
            String line = null;
            while ((line = reader.readLine()) != null) {
                verInfoStr.append(line + "\n");
            }
            reader.close();

            JSONArray array = new JSONArray(verInfoStr.toString());
            if (array.length() > 0) {
                JSONObject obj = array.getJSONObject(0);
                newVerCode = obj.getInt("verCode");
                newVerName = obj.getString("verName");
                downloadPath = obj.getString("apkPath");
            }
        } catch (Exception e) {
            Log.d(LOG_TAG, "" + e.toString());
            return false;
        }
        return true;
    }

    /**
     * 
     */
    private void showNoticeDialog() {
        Runnable runnable = new Runnable() {
            public void run() {
                
                AlertDialog.Builder builder = new Builder(mContext);
                builder.setTitle(R.string.soft_update_title);
                builder.setMessage(R.string.soft_update_info);
                
                builder.setPositiveButton(R.string.soft_update_updatebtn,
                        new OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                dialog.dismiss();
                                showDownloadDialog();
                            }
                        });

                builder.setNegativeButton(R.string.soft_update_later,
                        new OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                dialog.dismiss();
                            }
                        });
                Dialog noticeDialog = builder.create();
                noticeDialog.show();
            }
        };
        this.cordova.getActivity().runOnUiThread(runnable);
    }

    /**
     * 
     */
    private void showDownloadDialog() {
        // 
        AlertDialog.Builder builder = new Builder(mContext);
        builder.setTitle(R.string.soft_updating);
        //
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        View v = inflater.inflate(R.layout.softupdate_progress, null);
        mProgress = (ProgressBar) v.findViewById(R.id.update_progress);
        builder.setView(v);
        // 
        builder.setNegativeButton(R.string.soft_update_cancel,
                new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                       
                        cancelUpdate = true;
                    }
                });
        mDownloadDialog = builder.create();
        mDownloadDialog.show();
        //
        downloadApk();
    }

    /**
     * 
     */
    private void downloadApk() {
        //
        new downloadApkThread().start();
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            // 
            case DOWNLOAD:
                // 
                mProgress.setProgress(progress);
                break;
            case DOWNLOAD_FINISH:
                // 
                installApk();
                break;
            default:
                break;
            }
        };
    };

    /**
     * 
     */
    private class downloadApkThread extends Thread {
        @Override
        public void run() {
            try {
                //
                if (Environment.getExternalStorageState().equals(
                        Environment.MEDIA_MOUNTED)) {
                    // 
                    String sdpath = Environment.getExternalStorageDirectory()
                            + "/";
                    mSavePath = sdpath + "download";
                    URL url = new URL(downloadPath);
                    // 
                    HttpURLConnection conn = (HttpURLConnection) url
                            .openConnection();
                    conn.connect();
                    // 
                    int length = conn.getContentLength();
                    // 
                    InputStream is = conn.getInputStream();

                    File file = new File(mSavePath);
                    //
                    if (!file.exists()) {
                        file.mkdir();
                    }
                    File apkFile = new File(mSavePath, newVerName);
                    FileOutputStream fos = new FileOutputStream(apkFile);
                    int count = 0;
                    // 
                    byte buf[] = new byte[1024];
                    // 
                    do {
                        int numread = is.read(buf);
                        count += numread;
                        //
                        progress = (int) (((float) count / length) * 100);
                        // 
                        mHandler.sendEmptyMessage(DOWNLOAD);
                        if (numread <= 0) {
                            //
                            mHandler.sendEmptyMessage(DOWNLOAD_FINISH);
                            break;
                        }
                        // 
                        fos.write(buf, 0, numread);
                    } while (!cancelUpdate);// 
                    fos.close();
                    is.close();
                } else {
                    Log.d(LOG_TAG, "");
                }
            } catch (MalformedURLException e) {
                Log.d(LOG_TAG, "" + e.toString());
            } catch (IOException e) {
                Log.d(LOG_TAG, "" + e.toString());
            }
            // 
            mDownloadDialog.dismiss();
        }
    };

    /**
     * 
     */
    private void installApk() {
        File apkfile = new File(mSavePath, newVerName);
        if (!apkfile.exists()) {
            return;
        }
        // 
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.setDataAndType(Uri.parse("file://" + apkfile.toString()),
                "application/vnd.android.package-archive");
        mContext.startActivity(i);
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
