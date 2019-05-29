package net.kberstene.obsscoreoverlay.utilities;

import android.view.View;
import android.widget.CompoundButton;

import net.kberstene.obsscoreoverlay.game_elements.Player;

import java.util.ArrayList;

// RadioGroup doesn't allow for other parent layouts
// so this is a re-implementation to try and allow that
public class PlayerStateButtonGroup extends ArrayList<CompoundButton> {
    private ArrayList<Player> players;
    private int checkedIndex;
    private int stateIndex;
    private boolean multiState;

    public PlayerStateButtonGroup(int stateIndex, boolean multiState) {
        super();
        this.players = new ArrayList<>();
        this.checkedIndex = -1;
        this.stateIndex = stateIndex;
        this.multiState = multiState;
    }

    public void add(CompoundButton button, Player player) {
        // Add button to button list
        this.add(button);

        // Add player to player list
        players.add(player);

        // Get button index for listener
        final int index = this.size() - 1;

        // Set listener
        button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!multiState) {
                    // For radio buttons only:
                    if (checkedIndex != -1) {
                        // Remove the check from the old button and inform the appropriate player
                        get(checkedIndex).setChecked(false);
                        players.get(index).setBooleanState(stateIndex, false);
                    }

                    // Update the checked index
                    checkedIndex = index;
                }

                // Inform the player
                players.get(index).setBooleanState(stateIndex, isChecked);
            }
        });
        /*button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkedIndex != -1) {
                    // Remove the check from the old button and inform the appropriate player
                    setButtonCheck(checkedIndex, false);
                }

                setButtonCheck(index, true);
            }
        });*/
    }

    private void setButtonCheck(int index, boolean checked) {
        if (!multiState) {
            // Change the radio button state
            get(index).setChecked(checked);

            // Update checked index
            checkedIndex = index;
        //} else {
            // Check boxes don't need to be checked manually
            //checked = get(index).isChecked();
        }

        // Inform the player
        players.get(index).setBooleanState(stateIndex, checked);
    }

    public boolean getStateType() {
        return multiState;
    }

    public void setStateType(boolean multiState) {
        clearCheck();
        this.multiState = multiState;
    }

    public void clearCheck() {
        if (multiState) {
            // Clear all the buttons
            for (int i = 0; i < size(); i++) {
                this.get(i).setChecked(false);
                players.get(i).setBooleanState(stateIndex, false);
            }
        } else {
            if (checkedIndex != -1) { // If checkedIndex == -1, that means no index has been checked yet
                // Clear currently checked button
                this.get(checkedIndex).setChecked(false);

                // Inform the appropriate player
                players.get(checkedIndex).setBooleanState(stateIndex, false);

                // Set index to impossible value
                checkedIndex = -1;
            }
        }
    }
}
