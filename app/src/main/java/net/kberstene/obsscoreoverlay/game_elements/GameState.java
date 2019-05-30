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
import android.util.Log;
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

import java.util.ArrayList;

public class GameState {
    private ArrayList<Player> players;
    private ArrayList<UniversalState> universalStates;
    private ArrayList<PlayerStateButtonGroup> booleanStateButtonGroups;
    private ArrayList<String> booleanStateNames;
    private ArrayList<Boolean> booleanStateTypes;
    private int booleanStateCount;

    public GameState(Context context) {
        initGameState(context,true, false);
    }

    public void initGameState(Context context, boolean loadFromFile, boolean fullGameReset) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_FILENAME, Context.MODE_PRIVATE);

        // Make sure the container is empty
        ((MainActivity)context).getPlayerLayoutContainer().removeAllViews();
        ((MainActivity)context).getUniversalStateContainer().removeAllViews();

        // Init default boolean state info
        booleanStateButtonGroups = new ArrayList<>();
        booleanStateTypes = new ArrayList<>();
        booleanStateCount = 0;
        booleanStateNames = new ArrayList<>();

        // Init default universal state info
        universalStates = new ArrayList<>();
        int universalStateCount = 0;
        ArrayList<String> universalStateNames = new ArrayList<>();
        ArrayList<Boolean> universalStateStatus = new ArrayList<>();

        // Init default player info
        int playerCount = 4;

        if (!fullGameReset) {
            playerCount = prefs.getInt(Constants.PREF_KEY_PLAYER_COUNT, 4);
            booleanStateCount = prefs.getInt(Constants.PREF_KEY_GAME_BOOLEAN_STATE_COUNT, 2);

            for (int i = 0; i < booleanStateCount; i++) {
                booleanStateTypes.add(prefs.getBoolean(Constants.PREF_KEY_GAME_BOOLEAN_STATE_TYPE + i, false));
            }

            // Get saved universal state info
            universalStateCount = prefs.getInt(Constants.PREF_KEY_UNIVERSAL_STATE_COUNT, 0);


            for (int i = 0; i < universalStateCount; i ++) {
                universalStateNames.add(prefs.getString(Constants.PREF_KEY_UNIVERSAL_STATE_NAME + i, ""));
                universalStateStatus.add(prefs.getBoolean(Constants.PREF_KEY_UNIVERSAL_STATE_STATUS + i, false));
            }

            initUniversalStates(context, universalStateNames, universalStateStatus);
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
                booleanStateNames.add(prefs.getString(Constants.PREF_KEY_GAME_BOOLEAN_STATE_NAME + i, booleanStateTypes.get(i) ? "X" : "Y"));
            }
        } else {
            // We still have to set strings
            for (int i = 0; i < playerCount; i++) {
                playerNames[i] = "Player " + (i + 1);
            }

            for (int i = 0; i < booleanStateCount; i++) {
                booleanStateNames.add(booleanStateTypes.get(i) ? "X" : "Y");
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
            stateHeader.setText(booleanStateNames.get(i));

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
                    showResetPrompt(context, "Reset all players for:" + "\n" + booleanStateNames.get(buttonIndex), new DialogInterface.OnClickListener() {
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
            playerInfo.putStringArray(Constants.PLAYER_BUNDLE_KEY_STATE_NAMES, booleanStateNames.toArray(new String[0]));

            // Create new Player
            Player newPlayer = new Player(playerInfo);

            // Create layout for player
            addNewPlayerLayout(context, newPlayer);

            // Add the new player to the player list
            players.add(newPlayer);
        }
    }

    private void initUniversalStates(Context context, ArrayList<String> stateNames, ArrayList<Boolean> states) {
        int rightmostViewId = ConstraintSet.PARENT_ID;

        for (int i = 0; i < stateNames.size(); i++) {
            // Create the label to pass to the new state object
            TextView stateLabel = new TextView(context);
            stateLabel.setId(View.generateViewId());

            // Create the check box
            CheckBox checkBox = new CheckBox(context);
            checkBox.setId(View.generateViewId());

            // Create the new state object
            final UniversalState newState = new UniversalState(stateNames.get(i), i + 1, stateLabel);
            newState.setState(states.get(i));

            // Tie the check box to the state object
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    newState.setState(isChecked);
                }
            });

            // Add the check box and label to the layout
            ConstraintLayout stateContainer = ((MainActivity)context).getUniversalStateContainer();
            stateContainer.addView(stateLabel);
            stateContainer.addView(checkBox);

            // Add constraints in the layout
            ConstraintSet constraints = new ConstraintSet();
            constraints.clone(stateContainer);

                // Constrain the label horizontally
            constraints.setHorizontalChainStyle(stateLabel.getId(), ConstraintSet.CHAIN_SPREAD);
            constraints.addToHorizontalChain(stateLabel.getId(), rightmostViewId, ConstraintSet.PARENT_ID);
                // Constrain the check box to the label
            constraints.connect(checkBox.getId(), ConstraintSet.LEFT, stateLabel.getId(), ConstraintSet.LEFT);
            constraints.connect(checkBox.getId(), ConstraintSet.RIGHT, stateLabel.getId(), ConstraintSet.RIGHT);
                // Constrain the check box and label together vertically
            constraints.connect(stateLabel.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            constraints.connect(stateLabel.getId(), ConstraintSet.BOTTOM, checkBox.getId(), ConstraintSet.TOP);
            constraints.connect(checkBox.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);

            // Apply constraints
            constraints.applyTo(stateContainer);

            // Set the new right-most view ID
            rightmostViewId = stateLabel.getId();

            // Add state to list
            universalStates.add(newState);
        }
    }

    public void saveGameState(Context context) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(Constants.PREF_FILENAME, Context.MODE_PRIVATE).edit();

        // Store universal game states
        prefs.putInt(Constants.PREF_KEY_UNIVERSAL_STATE_COUNT, universalStates.size());
        Log.w("SAVING STATES", "Number of universal states: " + universalStates.size());
        for (int i = 0; i < universalStates.size(); i++) {
            prefs.putBoolean(Constants.PREF_KEY_UNIVERSAL_STATE_STATUS + i, universalStates.get(i).getState());
            prefs.putString(Constants.PREF_KEY_UNIVERSAL_STATE_NAME + i, universalStates.get(i).getName());
        }

        // Store player states
        prefs.putInt(Constants.PREF_KEY_PLAYER_COUNT, players.size());
        prefs.putInt(Constants.PREF_KEY_GAME_BOOLEAN_STATE_COUNT, booleanStateCount);
        for (int i = 0; i < booleanStateCount; i++)
            prefs.putBoolean(Constants.PREF_KEY_GAME_BOOLEAN_STATE_TYPE + i, booleanStateTypes.get(i));

        // Store state names
        for (int i = 0; i < booleanStateCount; i++)
            prefs.putString(Constants.PREF_KEY_GAME_BOOLEAN_STATE_NAME + i, booleanStateNames.get(i));

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

    private AlertDialog createStateEditPrompt(final Context context, final ArrayList stateGroup) {
        final LinearLayout alertStateContainer = (LinearLayout) ((Activity) context).getLayoutInflater().inflate(R.layout.alert_state_container, null, false);

        AlertDialog prompt = new AlertDialog.Builder(context)
                .setCancelable(true)
                .setNegativeButton("Close", null)
                .setView(alertStateContainer)
                .create();

        if (stateGroup.size() > 0) {
            { // Create headers - scope limit local variables
                // Don't attach to root so it returns us the layout and not the root layout
                ConstraintLayout stateHeaderLayout = (ConstraintLayout) ((Activity) context).getLayoutInflater().inflate(R.layout.alert_states, alertStateContainer, false);

                // Add the view to the container
                alertStateContainer.addView(stateHeaderLayout);

                // Set existing views as invisible
                View stateEditText = stateHeaderLayout.findViewById(R.id.statePromptEditText);
                View stateSwitch = stateHeaderLayout.findViewById(R.id.statePromptSwitch);
                View removeButton = stateHeaderLayout.findViewById(R.id.statePromptRemoveButton);
                stateEditText.setVisibility(View.INVISIBLE);
                stateSwitch.setVisibility((stateGroup.get(0) instanceof PlayerStateButtonGroup) ? View.INVISIBLE : View.GONE);
                removeButton.setVisibility(View.INVISIBLE);

                // Create new text views for labels
                TextView stateLabel = new TextView(context);
                TextView removeLabel = new TextView(context);
                TextView switchLabel = new TextView(context);
                stateLabel.setId(View.generateViewId());
                removeLabel.setId(View.generateViewId());
                stateLabel.setText("State Label");
                removeLabel.setText("Remove");

                // Add the labels to the layout
                stateHeaderLayout.addView(stateLabel);
                stateHeaderLayout.addView(removeLabel);

                // Add switch label if necessary
                if (stateGroup.get(0) instanceof PlayerStateButtonGroup) {
                    switchLabel.setId(View.generateViewId());
                    switchLabel.setText("Single/Multi");
                    stateHeaderLayout.addView(switchLabel);
                }

                // Constrain the labels
                ConstraintSet constraints = new ConstraintSet();
                constraints.clone(stateHeaderLayout);

                constraints.connect(stateLabel.getId(), ConstraintSet.TOP, stateEditText.getId(), ConstraintSet.TOP);
                constraints.connect(stateLabel.getId(), ConstraintSet.BOTTOM, stateEditText.getId(), ConstraintSet.BOTTOM);
                constraints.connect(stateLabel.getId(), ConstraintSet.LEFT, stateEditText.getId(), ConstraintSet.LEFT);
                constraints.connect(stateLabel.getId(), ConstraintSet.RIGHT, stateEditText.getId(), ConstraintSet.RIGHT);

                constraints.connect(removeLabel.getId(), ConstraintSet.TOP, removeButton.getId(), ConstraintSet.TOP);
                constraints.connect(removeLabel.getId(), ConstraintSet.BOTTOM, removeButton.getId(), ConstraintSet.BOTTOM);
                constraints.connect(removeLabel.getId(), ConstraintSet.LEFT, removeButton.getId(), ConstraintSet.LEFT);
                constraints.connect(removeLabel.getId(), ConstraintSet.RIGHT, removeButton.getId(), ConstraintSet.RIGHT);

                if (stateGroup.get(0) instanceof PlayerStateButtonGroup) {
                    constraints.connect(switchLabel.getId(), ConstraintSet.TOP, stateSwitch.getId(), ConstraintSet.TOP);
                    constraints.connect(switchLabel.getId(), ConstraintSet.BOTTOM, stateSwitch.getId(), ConstraintSet.BOTTOM);
                    constraints.connect(switchLabel.getId(), ConstraintSet.LEFT, stateSwitch.getId(), ConstraintSet.LEFT);
                    constraints.connect(switchLabel.getId(), ConstraintSet.RIGHT, stateSwitch.getId(), ConstraintSet.RIGHT);
                }

                constraints.applyTo(stateHeaderLayout);
            }

            // Set name and type variables
            final ArrayList<String> stateNames;
            ArrayList<Boolean> stateTypes = null;
            if (stateGroup.get(0) instanceof PlayerStateButtonGroup) {
                stateNames = booleanStateNames;
                stateTypes = booleanStateTypes;
            } else {
                // Create an ArrayList interface to access the universalState names
                stateNames = new ArrayList<String>() {
                    @Override
                    public String set(int index, String element) {
                        universalStates.get(index).setName(element);
                        return universalStates.get(index).getName();
                    }

                    @Override
                    public int size() {
                        return universalStates.size();
                    }

                    @Override
                    public String get(int index) {
                        return universalStates.get(index).getName();
                    }
                };
            }

            // Add all current states
            for (int i = 0; i < stateNames.size(); i++) {
                // Don't attach to root so it returns us the layout and not the root layout
                final ConstraintLayout stateLayout = (ConstraintLayout) ((Activity) context).getLayoutInflater().inflate(R.layout.alert_states, alertStateContainer, false);

                // Add the view to the container
                alertStateContainer.addView(stateLayout);

                // Set the state label
                final EditText stateLabel = stateLayout.findViewById(R.id.statePromptEditText);
                stateLabel.setText(stateNames.get(i));

                // Set switch state
                Switch stateSwitch = stateLayout.findViewById(R.id.statePromptSwitch);
                if (stateTypes != null) {
                    stateSwitch.setChecked(booleanStateTypes.get(i));
                } else {
                    // Hide switch if not applicable
                    stateSwitch.setVisibility(View.GONE);
                }

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
                        stateNames.set(index, s.toString());
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
                        if (stateGroup.get(0) instanceof PlayerStateButtonGroup) {
                            // Decrease the state count by 1
                            booleanStateCount--;

                            // Remove the button group
                            booleanStateButtonGroups.remove(index);

                            // Remove the name
                            booleanStateNames.remove(index);

                            // Remove the type
                            booleanStateTypes.remove(index);
                        } else {
                            universalStates.remove(index);
                        }

                        // Save states and reload the game state
                        saveGameState(context);
                        initGameState(context, true, false);

                        // Remove the state layout from the container
                        alertStateContainer.removeView(stateLayout);

                        // Remove the header if necessary
                        if (alertStateContainer.getChildCount() == 1) {
                            alertStateContainer.removeAllViews();
                        }
                    }
                });
            }
        }

        return prompt;
    }

    public void showPlayerStateEditPrompt(final Context context) {
        AlertDialog prompt = createStateEditPrompt(context, booleanStateButtonGroups);

        prompt.setButton(DialogInterface.BUTTON_NEUTRAL,"Add new state", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Add a new button group
                        booleanStateButtonGroups.add(new PlayerStateButtonGroup(booleanStateCount, false));

                        // Add a new name
                        booleanStateNames.add("X");

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
                        showPlayerStateEditPrompt(context);
                    }
                });
        prompt.setTitle("Edit Player States");

        prompt.show();
    }

    public void showUniversalStateEditPrompt(final Context context) {
        AlertDialog prompt = createStateEditPrompt(context, universalStates);

        prompt.setButton(DialogInterface.BUTTON_NEUTRAL,"Add new state", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which != DialogInterface.BUTTON_NEUTRAL) return;

                universalStates.add(new UniversalState("Z", universalStates.size() + 1, null));

                // Save states and reload the game state
                saveGameState(context);
                initGameState(context, true, false);

                // Relaunch the prompt
                showUniversalStateEditPrompt(context);
            }
        });
        prompt.setTitle("Edit Game States");

        prompt.show();
    }
}
