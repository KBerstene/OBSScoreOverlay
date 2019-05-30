package net.kberstene.obsscoreoverlay.game_elements;

import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import net.kberstene.obsscoreoverlay.R;
import net.kberstene.obsscoreoverlay.utilities.Constants;
import net.kberstene.obsscoreoverlay.utilities.ServerSettings;

import java.util.Locale;

public class Player {
    private String name;
    private int score;
    private TextView scoreDisplay, nameDisplay;
    private String fileName;
    private boolean[] booleanStates;
    private String[] stateNames;

    public Player(Bundle playerInfo) {
        this.name = playerInfo.getString(Constants.PLAYER_BUNDLE_KEY_PLAYER_NAME);
        this.score = playerInfo.getInt(Constants.PLAYER_BUNDLE_KEY_STARTING_SCORE);
        this.booleanStates = playerInfo.getBooleanArray(Constants.PLAYER_BUNDLE_KEY_BOOLEAN_STATES);
        this.stateNames = playerInfo.getStringArray(Constants.PLAYER_BUNDLE_KEY_STATE_NAMES);
        this.fileName = "Player_" + playerInfo.getInt(Constants.PLAYER_BUNDLE_KEY_PLAYER_ID) + "_score.txt";

        // Set initial score files upon creation
        updateScoreFile();
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
        StringBuilder fileText = new StringBuilder();

        fileText.append(score);
        for (int i = 0; i < booleanStates.length; i++) {
            if (booleanStates[i]) {
                fileText.append(" ");
                fileText.append(stateNames[i]);
            }
        }

        ServerSettings.writeToFile(fileName, fileText.toString().getBytes());
    }

}
