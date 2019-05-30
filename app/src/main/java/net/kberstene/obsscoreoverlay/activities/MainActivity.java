package net.kberstene.obsscoreoverlay.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;

import net.kberstene.obsscoreoverlay.R;
import net.kberstene.obsscoreoverlay.game_elements.GameState;
import net.kberstene.obsscoreoverlay.utilities.ServerSettings;

public class MainActivity extends AppCompatActivity {
    private GameState gameState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Add the toolbar
        Toolbar mainToolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(mainToolbar);

        // Retrieve the server settings or prompt
        ServerSettings.getServerPath(this);

        // Initialize the game state
        gameState = new GameState(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Add the main menu to the action bar
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final Context context = this;

        // Handle menu option selection
        switch (item.getItemId()) {
            case R.id.action_reset_scores:
                gameState.showResetPrompt(context,"Reset scores for all players?", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        gameState.initGameState(context, false, false);
                    }
                });
                return true;
            case R.id.action_reset_game:
                gameState.showResetPrompt(context, "Reset game settings to default?", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        gameState.initGameState(context, false, true);
                    }
                });
                return true;
            case R.id.action_change_path:
                ServerSettings.showServerPrompt(this);
                return true;
            case R.id.action_edit_player_states:
                gameState.showPlayerStateEditPrompt(this);
                return true;
            case R.id.action_edit_universal_states:
                gameState.showUniversalStateEditPrompt(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStop() {
        Log.w("MAINACTIVITY", "Saving scores");
        super.onStop();

        gameState.saveGameState(this);
    }

    public LinearLayout getPlayerLayoutContainer() {
        return findViewById(R.id.playerLayoutContainer);
    }

    public ConstraintLayout getUniversalStateContainer() {
        return findViewById(R.id.universalStateContainer);
    }

}