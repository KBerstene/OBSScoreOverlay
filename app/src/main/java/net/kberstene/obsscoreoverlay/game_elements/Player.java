package net.kberstene.obsscoreoverlay.game_elements;

import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import net.kberstene.obsscoreoverlay.R;
import net.kberstene.obsscoreoverlay.utilities.Constants;
import net.kberstene.obsscoreoverlay.utilities.ServerSettings;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Locale;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;

public class Player {
    private String name;
    private int score;
    private TextView scoreDisplay, nameDisplay;
    private String serverPath, fileName;
    private boolean[] booleanStates;
    private String[] stateNames;

    public Player(Bundle playerInfo) {
        this.name = playerInfo.getString(Constants.PLAYER_BUNDLE_KEY_PLAYER_NAME);
        this.score = playerInfo.getInt(Constants.PLAYER_BUNDLE_KEY_STARTING_SCORE);
        this.booleanStates = playerInfo.getBooleanArray(Constants.PLAYER_BUNDLE_KEY_BOOLEAN_STATES);
        this.stateNames = playerInfo.getStringArray(Constants.PLAYER_BUNDLE_KEY_STATE_NAMES);
        this.fileName = "Player_" + playerInfo.getInt(Constants.PLAYER_BUNDLE_KEY_PLAYER_ID) + "_score.txt";
        this.setServerPath(playerInfo.getString(Constants.PLAYER_BUNDLE_KEY_SERVER_PATH));
    }

    public void setViews(View playerLayout) {
        // Update score display for initial state
        this.scoreDisplay = playerLayout.findViewById(R.id.playerScoreText);
        this.updateScoreDisplay();

        // Set score button listeners
        playerLayout.findViewById(R.id.playerScoreUpButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setScore(getScore() + 1);
            }
        });
        playerLayout.findViewById(R.id.playerScoreDownButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setScore(getScore() - 1);
            }
        });

        // Update name TextView
        this.nameDisplay = playerLayout.findViewById(R.id.playerNameText);
        this.updateNameDisplay();

        // Set name display button listener
        this.nameDisplay.setLongClickable(true);
        this.nameDisplay.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // Replace name display with EditText for modifying name
                nameDisplay.setVisibility(View.INVISIBLE);

                // Create the editor and set its text to the current player name
                final EditText nameEditor = new EditText(nameDisplay.getContext());
                nameEditor.setId(View.generateViewId());

                // Set the name and width as the same as the display TextView
                nameEditor.setWidth(nameDisplay.getWidth());
                nameEditor.setText(name);
                nameEditor.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

                // Add the editor to the layout
                final ConstraintLayout playerLayout = (ConstraintLayout)nameDisplay.getParent();
                playerLayout.addView(nameEditor);

                // Constrain the editor
                ConstraintSet constraints = new ConstraintSet();
                constraints.clone(playerLayout);
                constraints.connect(nameEditor.getId(), ConstraintSet.TOP, nameDisplay.getId(), ConstraintSet.TOP);
                constraints.connect(nameEditor.getId(), ConstraintSet.BOTTOM, nameDisplay.getId(), ConstraintSet.BOTTOM);
                constraints.connect(nameEditor.getId(), ConstraintSet.LEFT, nameDisplay.getId(), ConstraintSet.LEFT);
                constraints.connect(nameEditor.getId(), ConstraintSet.RIGHT, nameDisplay.getId(), ConstraintSet.RIGHT);
                constraints.applyTo(playerLayout);

                // Create the editor's exit strategy
                nameEditor.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        setName(nameEditor.getText().toString());
                        playerLayout.removeView(nameEditor);
                        nameDisplay.setVisibility(View.VISIBLE);
                        return true;
                    }
                });

                return true;
            }
        });
    }

    public void setServerPath(@Nullable String serverPath) {
        if (serverPath != null) {
            this.serverPath = "smb://" + serverPath;

            if (this.serverPath.charAt(serverPath.length() - 1) != '/')
                this.serverPath += '/';

            // Set initial score files upon creation
            updateScoreFile();
        }
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
        this.updateNameDisplay();
    }

    public int getScore() {
        return score;
    }
    private void setScore(int score) {
        this.score = score;
        updateScore();
    }

    public void addBooleanState(String stateName) {
        boolean[] newBooleanStates = new boolean[booleanStates.length + 1];
        System.arraycopy(booleanStates, 0, newBooleanStates, 0, booleanStates.length);
        newBooleanStates[booleanStates.length] = false;
        booleanStates = newBooleanStates;

        String[] newStateNames = new String[stateNames.length + 1];
        System.arraycopy(stateNames, 0, newStateNames, 0, stateNames.length);
        newStateNames[stateNames.length] = stateName;
        stateNames = newStateNames;
    }

    public boolean getBooleanState(int stateIndex) {
        try {
            return this.booleanStates[stateIndex];
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }
    public void setBooleanState(int stateIndex, boolean state) {
        this.booleanStates[stateIndex] = state;
        updateScoreFile();
    }

    private void updateNameDisplay() {
        this.nameDisplay.setText(name);
    }

    private void updateScore() {
        updateScoreDisplay();
        updateScoreFile();
    }

    private void updateScoreDisplay() {
        this.scoreDisplay.setText(String.format(Locale.getDefault(), "%d", score));
    }

    private void updateScoreFile() {
        if (serverPath == null) {
            if (ServerSettings.getServerPath() == null)
                return;
            else
                serverPath = ServerSettings.getServerPath();
        }

        StringBuilder fileText = new StringBuilder();

        fileText.append(score);
        for (int i = 0; i < booleanStates.length; i++) {
            if (booleanStates[i]) {
                fileText.append(" ");
                fileText.append(stateNames[i]);
            }
        }

        final byte[] fileBytes = fileText.toString().getBytes();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    NtlmPasswordAuthentication auth = NtlmPasswordAuthentication.ANONYMOUS;
                    SmbFile scoreFile = new SmbFile(serverPath + fileName, auth);

                    if (!scoreFile.exists()) scoreFile.createNewFile();
                    scoreFile.connect();

                    SmbFileOutputStream out = new SmbFileOutputStream(scoreFile, false);
                    out.write(fileBytes);
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
