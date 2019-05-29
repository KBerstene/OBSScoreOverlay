package net.kberstene.obsscoreoverlay.utilities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.widget.EditText;

import net.kberstene.obsscoreoverlay.R;

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

        // Set text if exists
        EditText serverPathEditText = ((AlertDialog)dialog).findViewById(R.id.serverPathEditText);
        if ((serverPath != null) && (serverPathEditText != null))
            serverPathEditText.setText(serverPath);

        dialog.show();
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
        serverPath = path;
        context.getSharedPreferences(Constants.PREF_FILENAME, Context.MODE_PRIVATE)
                .edit()
                .putString(Constants.PREF_KEY_SERVER_PATH, ServerSettings.getServerPath())
                .apply();
    }
}
