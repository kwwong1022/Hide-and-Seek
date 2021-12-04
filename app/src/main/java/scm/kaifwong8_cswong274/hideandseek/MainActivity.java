package scm.kaifwong8_cswong274.hideandseek;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Switch;

public class MainActivity extends AppCompatActivity {
    public static final int DIFFICULTY_EASY = 0;
    public static final int DIFFICULTY_MEDIUM = 1;
    public static final int DIFFICULTY_HARD = 2;
    private static final String TAG = "MainActivity";

    ConstraintLayout menu_container;
    ConstraintLayout setting_menu;

    Switch sw_easy;
    Switch sw_medium;
    Switch sw_hard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sharedPreferences = getSharedPreferences("settingPreferences", MODE_PRIVATE);
        if (!sharedPreferences.getBoolean("FIRST_OPEN", false)) {
            sharedPreferences.edit()
                    .putInt("GAME_DIFFICULTY", DIFFICULTY_EASY)
                    .putBoolean("FIRST_OPEN", true)
                    .apply();
        }

        setting_menu = findViewById(R.id.setting_menu);
        menu_container = findViewById(R.id.setting_container);
        menu_container.setTranslationY(-5000);

        Button btn_map = findViewById(R.id.btn_gameStart);
        Button btn_setting = findViewById(R.id.btn_setting);
        Button btn_setting_finish = findViewById(R.id.btn_setting_finish);

        btn_map.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, MapsActivity.class);
            startActivity(i);
        });
        btn_setting.setOnClickListener(v -> {
            showSetting();
        });
        btn_setting_finish.setOnClickListener(v -> {
            showMenu();
        });

        sw_easy = findViewById(R.id.sw_easy);
        sw_medium = findViewById(R.id.sw_medium);
        sw_hard = findViewById(R.id.sw_hard);

        sw_easy.setOnClickListener(v -> {
            if (sw_easy.isChecked()) {
                sw_easy.setChecked(true);
                sw_medium.setChecked(false);
                sw_hard.setChecked(false);
            } else {
                sw_easy.setChecked(true);
            }
            sharedPreferences.edit()
                    .putInt("GAME_DIFFICULTY", DIFFICULTY_EASY)
                    .apply();
        });
        sw_medium.setOnClickListener(v -> {
            if (sw_medium.isChecked()) {
                sw_medium.setChecked(true);
                sw_easy.setChecked(false);
                sw_hard.setChecked(false);
            } else {
                sw_medium.setChecked(true);
            }
            sharedPreferences.edit()
                    .putInt("GAME_DIFFICULTY", DIFFICULTY_MEDIUM)
                    .apply();
        });
        sw_hard.setOnClickListener(v -> {
            if (sw_hard.isChecked()) {
                sw_hard.setChecked(true);
                sw_easy.setChecked(false);
                sw_medium.setChecked(false);
            } else {
                sw_hard.setChecked(true);
            }
            sharedPreferences.edit()
                    .putInt("GAME_DIFFICULTY", DIFFICULTY_HARD)
                    .apply();
        });

        showMenu();
    }

    private void showMenu() {
        ObjectAnimator animator = ObjectAnimator.ofFloat(menu_container, "translationY", -5000);
        animator.setDuration(500);
        animator.start();

        menu_container.setZ(2);
        setting_menu.setZ(1);
    }

    private void showSetting() {
        ObjectAnimator animator = ObjectAnimator.ofFloat(menu_container, "translationY", 0);
        animator.setDuration(500);
        animator.start();

        menu_container.setZ(2);
        setting_menu.setZ(2);

        SharedPreferences sharedPreferences = getSharedPreferences("settingPreferences", MODE_PRIVATE);
        switch (sharedPreferences.getInt("GAME_DIFFICULTY", DIFFICULTY_EASY)) {
            case DIFFICULTY_EASY:
                sw_easy.setChecked(true);
                sw_medium.setChecked(false);
                sw_hard.setChecked(false);
                break;
            case DIFFICULTY_MEDIUM:
                sw_easy.setChecked(false);
                sw_medium.setChecked(true);
                sw_hard.setChecked(false);
                break;
            case DIFFICULTY_HARD:
                sw_easy.setChecked(false);
                sw_medium.setChecked(false);
                sw_hard.setChecked(true);
                break;
        }

    }
}