package rs.pedjaapps.smc.utility;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

import rs.pedjaapps.smc.MaryoGame;

/**
 * Created by pedja on 21.9.14..
 */
public class PrefsManager {
    public static final String SCCPLF = "sccplf";

    public static Preferences prefs = Gdx.app.getPreferences(SCCPLF);

    public static boolean isPlayMusic() {
        return prefs.getBoolean(PrefsKey.music.toString(), true);
    }

    public static void setPlayMusic(boolean playMusic) {
        prefs.putBoolean(PrefsKey.music.toString(), playMusic);
        flush();
    }

    public static boolean isPlaySounds() {
        return prefs.getBoolean(PrefsKey.sound.toString(), true);
    }

    public static void setPlaySounds(boolean playSounds) {
        prefs.putBoolean(PrefsKey.sound.toString(), playSounds);
        flush();
    }

    public static String loadControllerMappings() {
        return prefs.getString("controllerMappings", "");
    }

    public static void saveControllerMappings(String json) {
        prefs.putString("controllerMappings", json);
        flush();
    }

    public static String getSaveGame() {
        String savedState = prefs.getString(PrefsKey.sg.toString(), null);
        if (savedState != null)
            savedState = Utility.decode(savedState, SCCPLF);

        return savedState;
    }

    public static void setSaveGame(String saveGame) {
        if (MaryoGame.GAME_DEVMODE)
            Gdx.app.log("Gamesave", saveGame);
        prefs.putString(PrefsKey.sg.toString(), Utility.encode(saveGame, SCCPLF));
        flush();
    }

    public static float getSoundVolume() {
        return prefs.getFloat(PrefsKey.sound_volume.toString(), 0.7f);
    }

    public static void setSoundVolume(float volume) {
        prefs.putFloat(PrefsKey.sound_volume.toString(), volume);
        flush();
    }

    public static float getMusicVolume() {
        return prefs.getFloat(PrefsKey.music_volume.toString(), 0.4f);
    }

    public static void setMusicVolume(float volume) {
        prefs.putFloat(PrefsKey.music_volume.toString(), volume);
        flush();
    }

    /**
     * den Hinweis maximal fünf mal anzeigen
     */
    public static boolean showKeyboardHint() {
        int keyboardhintshown = prefs.getInteger("keyboardhintshown", 0);
        keyboardhintshown++;
        boolean showHintNow = keyboardhintshown <= 5;
        if (showHintNow) {
            prefs.putInteger("keyboardhintshown", keyboardhintshown);
            prefs.flush();
        }
        return showHintNow;
    }

    public static void flush() {
        prefs.flush();
    }

    public enum PrefsKey {
        sound, music, sg, debug, sound_volume, music_volume
    }
}
