package com.example.test;

import android.content.Context;
import android.content.SharedPreferences;

public class StatsManager {

    private static final String PREFS_NAME = "prefs";

    private static final String KEY_GAMES_STARTED = "GAMES_STARTED";
    private static final String KEY_PERFECT_WINS = "PERFECT_WINS";

    public static void incrementGamesStarted(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        int gamesStarted = prefs.getInt(KEY_GAMES_STARTED, 0);
        gamesStarted++;

        prefs.edit()
                .putInt(KEY_GAMES_STARTED, gamesStarted)
                .apply();
    }

    public static void incrementPerfectWins(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        int perfectWins = prefs.getInt(KEY_PERFECT_WINS, 0);
        perfectWins++;

        prefs.edit()
                .putInt(KEY_PERFECT_WINS, perfectWins)
                .apply();
    }

    public static int getGamesStarted(Context context) {
        return context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_GAMES_STARTED, 0);
    }

    public static int getPerfectWins(Context context) {
        return context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_PERFECT_WINS, 0);
    }

    public static int getWinPercent(Context context) {
        int gamesStarted = getGamesStarted(context);
        int perfectWins = getPerfectWins(context);

        if (gamesStarted <= 0) {
            return 0;
        }

        return Math.round((perfectWins * 100f) / gamesStarted);
    }
}