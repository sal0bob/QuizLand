package com.example.test;

import android.content.Context;
import android.content.SharedPreferences;

public class LevelManager {

    private static final String PREFS_NAME = "prefs";

    private static final String KEY_LEVEL = "USER_LEVEL";
    private static final String KEY_XP = "USER_XP";

    private static final int BASE_XP = 100;
    private static final int MIN_PLAYERS_FOR_DIFFICULTY = 10;
    private static final double MAX_DIFFICULTY_MULTIPLIER = 3.0;

    public static int calculateEarnedXp(
            int correctAnswers,
            int totalQuestions,
            int playersWithPerfectScore,
            int totalPlayers
    ) {
        if (totalQuestions <= 0) {
            return 0;
        }

        double accuracy = (double) correctAnswers / totalQuestions;

        double difficultyMultiplier = 1.0;

        if (totalPlayers >= MIN_PLAYERS_FOR_DIFFICULTY) {
            double perfectRate = (double) playersWithPerfectScore / totalPlayers;

            difficultyMultiplier = 1 + (1 - perfectRate) * 2;
            difficultyMultiplier = Math.min(MAX_DIFFICULTY_MULTIPLIER, difficultyMultiplier);
        }

        return (int) Math.round(BASE_XP * accuracy * difficultyMultiplier);
    }

    public static int getXpToNextLevel(int currentLevel) {
        return 100 + currentLevel * 50;
    }

    public static LevelResult addXp(Context context, int earnedXp) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        int level = prefs.getInt(KEY_LEVEL, 1);
        int xp = prefs.getInt(KEY_XP, 0);

        int oldLevel = level;

        xp += earnedXp;

        while (xp >= getXpToNextLevel(level)) {
            xp -= getXpToNextLevel(level);
            level++;
        }

        prefs.edit()
                .putInt(KEY_LEVEL, level)
                .putInt(KEY_XP, xp)
                .apply();

        return new LevelResult(oldLevel, level, xp, getXpToNextLevel(level), earnedXp);
    }

    public static int getLevel(Context context) {
        return context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_LEVEL, 1);
    }

    public static int getXp(Context context) {
        return context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_XP, 0);
    }

    public static class LevelResult {
        public final int oldLevel;
        public final int newLevel;
        public final int currentXp;
        public final int xpToNextLevel;
        public final int earnedXp;

        public LevelResult(int oldLevel, int newLevel, int currentXp, int xpToNextLevel, int earnedXp) {
            this.oldLevel = oldLevel;
            this.newLevel = newLevel;
            this.currentXp = currentXp;
            this.xpToNextLevel = xpToNextLevel;
            this.earnedXp = earnedXp;
        }

        public boolean isLevelUp() {
            return newLevel > oldLevel;
        }
    }
}