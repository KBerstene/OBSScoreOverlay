package net.kberstene.obsscoreoverlay.utilities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.EditText;

import net.kberstene.obsscoreoverlay.R;

import java.io.IOException;
import java.net.MalformedURLException;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;

public class ServerSettings {
    private static String serverPath;

    public static void showServerPrompt(final Context context) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(R.layout.alert_server)
                .setCancelable(serverPath != null)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditText serverPathEditText = ((AlertDialog)dialog).findViewById(R.id.serverPathEditText);
                        if (serverPathEditText != null) {
                            setServerPath(context, serverPathEditText.getText().toString());
                        }
                    }
                })
                .create();

        // Must show the dialog before setting the text
        dialog.show();

        // Set text if exists
        EditText serverPathEditText = dialog.findViewById(R.id.serverPathEditText);
        if ((serverPath != null) && (serverPathEditText != null)) {
            String[] strippedPathArray = serverPath.split("://");
            serverPathEditText.setText(strippedPathArray[strippedPathArray.length - 1]);
        }
    }

    public static String getServerPath() {
        return serverPath;
    }

    public static void getServerPath(Context context) {
        if (serverPath == null) {
            SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_FILENAME, Context.MODE_PRIVATE);
            serverPath = prefs.getString(Constants.PREF_KEY_SERVER_PATH, null);
        }

        if (serverPath == null) showServerPrompt(context);
    }

    private static void setServerPath(Context context, String path) {
        serverPath = "smb://" + path;

        if (serverPath.charAt(serverPath.length() - 1) != '/')
            serverPath += '/';

        context.getSharedPreferences(Constants.PREF_FILENAME, Context.MODE_PRIVATE)
                .edit()
                .putString(Constants.PREF_KEY_SERVER_PATH, ServerSettings.getServerPath())
                .apply();
    }

    public static void writeToFile(final String filename, final byte[] contents) {
        // If server path has not been set, do not attempt to write to the file
        if (serverPath == null) return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    NtlmPasswordAuthentication auth = NtlmPasswordAuthentication.ANONYMOUS;
                    SmbFile scoreFile = new SmbFile(serverPath + filename, auth);

                    if (!scoreFile.exists()) scoreFile.createNewFile();
                    scoreFile.connect();

                    SmbFileOutputStream out = new SmbFileOutputStream(scoreFile, false);
                    out.write(contents);
                    out.close();
                } catch (MalformedURLException mue) {
                    //MainActivity.makeToast("Bad URL: " + filePath);
                    Log.w("MALFORMED URL", Log.getStackTraceString(mue));
                } catch (IOException ioe) {
                    //MainActivity.makeToast("Could not connect to score file for player " + playerId);
                    Log.w("IOEXCEPTION", Log.getStackTraceString(ioe));
                }
            }
        }).start();
    }
}
