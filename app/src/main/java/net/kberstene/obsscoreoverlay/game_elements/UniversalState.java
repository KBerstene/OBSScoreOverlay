package net.kberstene.obsscoreoverlay.game_elements;

import android.support.annotation.Nullable;
import android.widget.TextView;

import net.kberstene.obsscoreoverlay.utilities.ServerSettings;

public class UniversalState {
    private boolean state;
    private String name;
    private String filePath;
    private TextView nameDisplay;

    public UniversalState(String stateName, int stateIndex, @Nullable TextView stateLabel) {
        name = stateName;
        filePath = "Game_state_" + stateIndex + ".txt";
        nameDisplay = stateLabel;

        // Set initial display and file
        updateNameDisplay();
        updateFile();
    }

    public String getName() {
        return name;
    }

    public boolean getState() {
        return state;
    }

    public void setName(String name) {
        this.name = name;

        updateNameDisplay();
    }

    public void setState(boolean state) {
        this.state = state;

        updateFile();
    }

    private void updateFile() {
        ServerSettings.writeToFile(filePath, (getState() ? name : "").getBytes());
    }

    private void updateNameDisplay() {
        if (nameDisplay != null) nameDisplay.setText(name);
    }
}
