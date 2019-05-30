package net.kberstene.obsscoreoverlay.game_elements;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;

import net.kberstene.obsscoreoverlay.R;
import net.kberstene.obsscoreoverlay.activities.MainActivity;
import net.kberstene.obsscoreoverlay.utilities.Constants;
import net.kberstene.obsscoreoverlay.utilities.PlayerStateButtonGroup;

import java.lang.System;
import java.util.ArrayList;

public class GameState {
    private ArrayList<Player> players;
    private ArrayList<PlayerStateButtonGroup> booleanStateButtonGroups;
    private String[] booleanStateNames;
    private ArrayList<Boolean> booleanStateTypes;
    private int booleanStateCount;

    public GameState(Context context) {
        initGameState(context,true, false);
    }

    public void initGameState(Context context, boolean loadFromFile, boolean fullGameReset) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_FILENAME, Context.MODE_PRIVATE);

        // Make sure the container is empty
        ((MainActivity)context).getPlayerLayoutContainer().removeAllViews();

        // Init boolean state info
        booleanStateButtonGroups = new ArrayList<>();
        booleanStateTypes = new ArrayList<>();
        booleanStateCount = 0;
        booleanStateNames = new String[booleanStateCount];

        int playerCount = 4;

        if (!fullGameReset) {
            playerCount = prefs.getInt(Constants.PREF_KEY_PLAYER_COUNT, 4);
            booleanStateCount = prefs.getInt(Constants.PREF_KEY_GAME_BOOLEAN_STATE_COUNT, 2);
            booleanStateNames = new String[booleanStateCount];
            
            for (int i = 0; i < booleanStateCount; i++) {
                booleanStateTypes.add(prefs.getBoolean(Constants.PREF_KEY_GAME_BOOLEAN_STATE_TYPE + i, false));
            }
        }
        
        String[] playerNames = new String[playerCount];
        int[] startingScores = new int[playerCount]; // ints initialize to zero by default
        boolean[][] booleanStates = new boolean[booleanStateCount][playerCount];
        
        for (int i = 0; i < booleanStateCount; i++) {
            booleanStateButtonGroups.add(new PlayerStateButtonGroup(i, booleanStateTypes.get(i)));
        }

        if (loadFromFile) {
            // Retrieve player states
            for (int i = 0; i < playerCount; i++) {
                playerNames[i] = prefs.getString(Constants.PREF_KEY_PLAYER_NAME + i, "Player " + (i + 1));
            }

            for (int i = 0; i < playerCount; i++) {
                startingScores[i] = prefs.getInt(Constants.PREF_KEY_PLAYER_SCORE + i, 0);

                for (int j = 0; j < booleanStateCount; j++) {
                    booleanStates[j][i] = prefs.getBoolean(Constants.PREF_KEY_PLAYER_BOOLEAN_STATE + j + "_" + i, false);
                }
            }
            

            // Retrieve state types and names
            for (int i = 0; i < booleanStateCount; i++) {
                booleanStateNames[i] = prefs.getString(Constants.PREF_KEY_GAME_BOOLEAN_STATE_NAME + i, booleanStateTypes.get(i) ? "X" : "Y");
            }
        } else {
            // We still have to set strings
            for (int i = 0; i < playerCount; i++) {
                playerNames[i] = "Player " + (i + 1);
            }

            for (int i = 0; i < booleanStateCount; i++) {
                booleanStateNames[i] = booleanStateTypes.get(i) ? "X" : "Y";
            }
        }

        // Create group headers before loading players
        initGroupHeaders(context);

        // Initialize players
        initPlayers(context, playerCount, playerNames, startingScores, booleanStates);
    }

    private void initGroupHeaders(final Context context) {
        // Get the default player header and make it invisible
        // Add text instead of state buttons

        // Inflate the new layout
        LinearLayout playerLayoutContainer = ((MainActivity)context).getPlayerLayoutContainer();
        ((MainActivity)context).getLayoutInflater().inflate(R.layout.player_status, playerLayoutContainer, true);

        // Get the layout and create a unique ID
        ConstraintLayout headerLayout = (ConstraintLayout) playerLayoutContainer.getChildAt(playerLayoutContainer.getChildCount() - 1);
        headerLayout.setId(View.generateViewId());

        // Remove the margins and shrink the header height a bit
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) headerLayout.getLayoutParams();
        layoutParams.setMargins(0, 0, 0, 0);
        headerLayout.setLayoutParams(layoutParams);

        // Hide all the items in the layout
        // Set all item heights to zero, since we only care about width
        for (int i = 0; i < headerLayout.getChildCount(); i++) {
            headerLayout.getChildAt(i).setVisibility(View.INVISIBLE);
            headerLayout.getChildAt(i).getLayoutParams().height = 0;
        }

        // Retrieve the ID of the player score button layout to add to its constraint chain
        int rightmostViewId = headerLayout.findViewById(R.id.playerScoreButtonLayout).getId();

        // Add state button headers
        for (int i = 0; i < booleanStateCount; i++) {
            // Add an invisible button for correct width placement
            // Then overlay it with the visible TextView label
            View dummyButton = booleanStateTypes.get(i) ? new CheckBox(context) : new RadioButton(context);
            dummyButton.setId(View.generateViewId());
            dummyButton.setVisibility(View.INVISIBLE);

            // Add the dummy button to the layout, then set the new right-most ID
            rightmostViewId = addButtonToPlayerLayout(headerLayout, dummyButton, rightmostViewId);

            // Create TextView and assign it a unique ID
            TextView stateHeader = new TextView(context);
            stateHeader.setId(View.generateViewId());

            // Set text of header to state name/icon
            stateHeader.setText(booleanStateNames[i]);

            // Add the TextView to the layout
            headerLayout.addView(stateHeader);

            // Create the custom constraint to lay on top of the invisible button
            ConstraintSet constraints = new ConstraintSet();
            constraints.clone(headerLayout);
            //constraints.connect(stateHeader.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP); // It looks better if the view is tied to the bottom
            constraints.connect(stateHeader.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
            constraints.connect(stateHeader.getId(), ConstraintSet.LEFT, dummyButton.getId(), ConstraintSet.LEFT);
            constraints.connect(stateHeader.getId(), ConstraintSet.RIGHT, dummyButton.getId(), ConstraintSet.RIGHT);

            // Apply the new constraints
            constraints.applyTo(headerLayout);

            // Set the button to clear its button group when long pressed
            // Pop up a confirmation dialogue before clearing
            final int buttonIndex = i;
            stateHeader.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showResetPrompt(context, "Reset all players for:" + "\n" + booleanStateNames[buttonIndex], new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            booleanStateButtonGroups.get(buttonIndex).clearCheck();
                        }
                    });

                    return true;
                }
            });
        }
    }

    private void initPlayers(Context context, int playerCount, String[] playerNames, int[] startingScores, boolean[][] booleanStates) {

        players = new ArrayList<>();

        for (int i = 0; i < playerCount; i++) {
            // Assemble player state information from initialized states
            boolean[] playerBooleanStates = new boolean[booleanStateCount];

            for (int j = 0; j < booleanStates.length; j++) {
                playerBooleanStates[j] = booleanStates[j][i];
            }

            // Create player info bundle
            Bundle playerInfo = new Bundle();
            playerInfo.putInt(Constants.PLAYER_BUNDLE_KEY_PLAYER_ID, i + 1);
            playerInfo.putString(Constants.PLAYER_BUNDLE_KEY_PLAYER_NAME, playerNames[i]);
            playerInfo.putInt(Constants.PLAYER_BUNDLE_KEY_STARTING_SCORE, startingScores[i]);
            playerInfo.putBooleanArray(Constants.PLAYER_BUNDLE_KEY_BOOLEAN_STATES, playerBooleanStates);
            playerInfo.putStringArray(Constants.PLAYER_BUNDLE_KEY_STATE_NAMES, booleanStateNames);

            // Create new Player
            Player newPlayer = new Player(playerInfo);

            // Create layout for player
            addNewPlayerLayout(context, newPlayer);

            // Add the new player to the player list
            players.add(newPlayer);
        }
    }

    public void saveGameState(Context context) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(Constants.PREF_FILENAME, Context.MODE_PRIVATE).edit();

        // Store game states
        prefs.putInt(Constants.PREF_KEY_PLAYER_COUNT, players.size());
        prefs.putInt(Constants.PREF_KEY_GAME_BOOLEAN_STATE_COUNT, booleanStateCount);
        for (int i = 0; i < booleanStateCount; i++)
            prefs.putBoolean(Constants.PREF_KEY_GAME_BOOLEAN_STATE_TYPE + i, booleanStateTypes.get(i));

        // Store state names
        for (int i = 0; i < booleanStateCount; i++)
            prefs.putString(Constants.PREF_KEY_GAME_BOOLEAN_STATE_NAME + i, booleanStateNames[i]);

        // Store player states
        for (int i = 0; i < players.size(); i++) {
            prefs.putString(Constants.PREF_KEY_PLAYER_NAME + i, players.get(i).getName());
            prefs.putInt(Constants.PREF_KEY_PLAYER_SCORE + i, players.get(i).getScore());

            for (int j = 0; j < booleanStateCount; j++)
                prefs.putBoolean(Constants.PREF_KEY_PLAYER_BOOLEAN_STATE + j + "_" + i, players.get(i).getBooleanState(j));
        }

        // Save preferences to disk
        prefs.apply();
    }

    private void addNewPlayerLayout(Context context, Player player) {
        // Inflate the new layout
        LinearLayout playerLayoutContainer = ((MainActivity)context).getPlayerLayoutContainer();
        ((MainActivity)context).getLayoutInflater().inflate(R.layout.player_status, playerLayoutContainer, true);

        // Tie the player object to the layout views
        ConstraintLayout playerLayout = (ConstraintLayout) playerLayoutContainer.getChildAt(playerLayoutContainer.getChildCount() - 1);
        playerLayout.setId(View.generateViewId());
        player.setViews(playerLayout);

        // Retrieve the ID of the player score button layout to add to its constraint chain
        int rightmostViewId = playerLayout.findViewById(R.id.playerScoreButtonLayout).getId();

        // Add boolean state buttons
        for (int i = 0; i < booleanStateCount; i++) {
            // Create button and assign it a unique ID
            CompoundButton stateButton = booleanStateTypes.get(i) ? new CheckBox(context): new RadioButton(context);
            stateButton.setId(View.generateViewId());

            // Add and constrain the view, then set the new right-most view ID
            rightmostViewId = addButtonToPlayerLayout(playerLayout, stateButton, rightmostViewId);

            // Add button to appropriate groups
            PlayerStateButtonGroup booleanStateGroup = booleanStateButtonGroups.get(i);
            booleanStateGroup.add(stateButton, player);

            // Check the button if appropriate
            if (player.getBooleanState(i)) {
                stateButton.toggle();
            }
        }
    }

    private int addButtonToPlayerLayout(ConstraintLayout playerLayout, View viewToAdd, int rightmostViewId) {
        // Add view to layout
        playerLayout.addView(viewToAdd);

        // Get ID of added view
        int viewId = viewToAdd.getId();

        // Prepare constraints to be modified
        ConstraintSet constraints = new ConstraintSet();
        constraints.clone(playerLayout);

        // Set the constraints
        constraints.addToHorizontalChain(viewId, rightmostViewId, ConstraintSet.PARENT_ID);
        constraints.connect(viewId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
        constraints.connect(viewId, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);

        // Apply the new constraints
        constraints.applyTo(playerLayout);

        // Return the right-most view ID, which is now the added view
        return viewId;
    }

    public void showResetPrompt(Context context, String message, DialogInterface.OnClickListener positiveButtonListener) {
        AlertDialog prompt = new AlertDialog.Builder(context)
                .setCancelable(true)
                .setMessage(message)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reset", positiveButtonListener)
                .create();

        // Show the dialog - this must be done before fetching the TextView to center it
        prompt.show();

        // Center the dialog text and change the size
        TextView promptMessage = prompt.findViewById(android.R.id.message);
        if (promptMessage != null) {
            promptMessage.setGravity(Gravity.CENTER);
        }
    }

    public void showStateEditPrompt(final Context context) {
        final LinearLayout alertStateContainer = (LinearLayout) ((Activity) context).getLayoutInflater().inflate(R.layout.alert_state_container, null, false);

        AlertDialog prompt = new AlertDialog.Builder(context)
                .setCancelable(true)
                .setNegativeButton("Close", null)
                .setNeutralButton("Add new state", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Add a new button group
                        booleanStateButtonGroups.add(new PlayerStateButtonGroup(booleanStateCount, false));

                        // Add a new name
                        String[] newBooleanStateNames = new String[booleanStateNames.length + 1];
                        System.arraycopy(booleanStateNames, 0, newBooleanStateNames, 0, booleanStateNames.length);
                        newBooleanStateNames[booleanStateNames.length] = "X";
                        booleanStateNames = newBooleanStateNames;

                        // Add new type
                        booleanStateTypes.add(false);

                        // Increment boolean state count
                        booleanStateCount++;

                        // Add the state to the players
                        for (Player player : players) player.addBooleanState("X");

                        // Save states and reload the game state
                        saveGameState(context);
                        initGameState(context, true, false);

                        // Relaunch the prompt
                        showStateEditPrompt(context);
                    }
                })
                .setTitle("Edit States")
                .setView(alertStateContainer)
                .create();

        // Create headers
        if (booleanStateCount > 0) {
            // Don't attach to root so it returns us the layout and not the root layout
            ConstraintLayout stateHeaderLayout = (ConstraintLayout) ((Activity) context).getLayoutInflater().inflate(R.layout.alert_states, alertStateContainer, false);

            // Add the view to the container
            alertStateContainer.addView(stateHeaderLayout);

            // Set existing views as invisible
            View stateEditText = stateHeaderLayout.findViewById(R.id.statePromptEditText);
            View stateSwitch = stateHeaderLayout.findViewById(R.id.statePromptSwitch);
            View removeButton = stateHeaderLayout.findViewById(R.id.statePromptRemoveButton);
            stateEditText.setVisibility(View.INVISIBLE);
            stateSwitch.setVisibility(View.INVISIBLE);
            removeButton.setVisibility(View.INVISIBLE);

            // Create new text views for labels
            TextView stateLabel = new TextView(context);
            TextView switchLabel = new TextView(context);
            TextView removeLabel = new TextView(context);
            stateLabel.setId(View.generateViewId());
            switchLabel.setId(View.generateViewId());
            removeLabel.setId(View.generateViewId());
            stateLabel.setText("State Label");
            switchLabel.setText("Single/Multi");
            removeLabel.setText("Remove");

            // Add the labels to the layout
            stateHeaderLayout.addView(stateLabel);
            stateHeaderLayout.addView(switchLabel);
            stateHeaderLayout.addView(removeLabel);

            // Constrain the labels
            ConstraintSet constraints = new ConstraintSet();
            constraints.clone(stateHeaderLayout);

            constraints.connect(stateLabel.getId(), ConstraintSet.TOP, stateEditText.getId(), ConstraintSet.TOP);
            constraints.connect(stateLabel.getId(), ConstraintSet.BOTTOM, stateEditText.getId(), ConstraintSet.BOTTOM);
            constraints.connect(stateLabel.getId(), ConstraintSet.LEFT, stateEditText.getId(), ConstraintSet.LEFT);
            constraints.connect(stateLabel.getId(), ConstraintSet.RIGHT, stateEditText.getId(), ConstraintSet.RIGHT);

            constraints.connect(switchLabel.getId(), ConstraintSet.TOP, stateSwitch.getId(), ConstraintSet.TOP);
            constraints.connect(switchLabel.getId(), ConstraintSet.BOTTOM, stateSwitch.getId(), ConstraintSet.BOTTOM);
            constraints.connect(switchLabel.getId(), ConstraintSet.LEFT, stateSwitch.getId(), ConstraintSet.LEFT);
            constraints.connect(switchLabel.getId(), ConstraintSet.RIGHT, stateSwitch.getId(), ConstraintSet.RIGHT);

            constraints.connect(removeLabel.getId(), ConstraintSet.TOP, removeButton.getId(), ConstraintSet.TOP);
            constraints.connect(removeLabel.getId(), ConstraintSet.BOTTOM, removeButton.getId(), ConstraintSet.BOTTOM);
            constraints.connect(removeLabel.getId(), ConstraintSet.LEFT, removeButton.getId(), ConstraintSet.LEFT);
            constraints.connect(removeLabel.getId(), ConstraintSet.RIGHT, removeButton.getId(), ConstraintSet.RIGHT);

            constraints.applyTo(stateHeaderLayout);
        }

        // Add all current states
        for (int i = 0; i < booleanStateNames.length; i++) {
            // Don't attach to root so it returns us the layout and not the root layout
            final ConstraintLayout stateLayout = (ConstraintLayout) ((Activity) context).getLayoutInflater().inflate(R.layout.alert_states, alertStateContainer, false);

            // Add the view to the container
            alertStateContainer.addView(stateLayout);

            // Set the state label
            final EditText stateLabel = stateLayout.findViewById(R.id.statePromptEditText);
            stateLabel.setText(booleanStateNames[i]);

            // Set switch state
            Switch stateSwitch = stateLayout.findViewById(R.id.statePromptSwitch);
            stateSwitch.setChecked(booleanStateButtonGroups.get(i).getStateType());

            // Set label change listener
            final int index = i;
            stateLabel.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    booleanStateNames[index] = s.toString();
                    saveGameState(context);
                    initGameState(context, true, false);
                }
            });

            // Set type switch listener
            stateSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                    booleanStateTypes.set(index, isChecked);
                    booleanStateButtonGroups.get(index).setStateType(booleanStateTypes.get(index));
                    saveGameState(context);
                    initGameState(context, true, false);
                }
            });

            // Set remove button listener
            stateLayout.findViewById(R.id.statePromptRemoveButton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Decrease the state count by 1
                    booleanStateCount--;

                    // Remove the button group
                    booleanStateButtonGroups.remove(index);

                    // Remove the name
                    ArrayList<String> newbooleanStateNames = new ArrayList<>();
                    for (int i = 0; i < booleanStateNames.length; i++) {
                        if (i != index) newbooleanStateNames.add(booleanStateNames[i]);
                    }
                    booleanStateNames = newbooleanStateNames.toArray(new String[booleanStateNames.length - 1]);

                    // Remove the type
                    booleanStateTypes.remove(index);

                    // Save states and reload the game state
                    saveGameState(context);
                    initGameState(context, true, false);

                    // Remove the state layout from the container
                    alertStateContainer.removeView(stateLayout);
                }
            });
        }

        prompt.show();
    }
}
