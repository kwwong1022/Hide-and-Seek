package scm.kaifwong8_cswong274.hideandseek;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        String timeString;
        float distance;
        int timeSecond;

        Intent i = getIntent();
        distance = i.getFloatExtra("DISTANCE", 0.0f);
        timeSecond = i.getIntExtra("TIME_SECOND", 0);
        timeString = i.getStringExtra("TIME_STRING");

        TextView tv_score = findViewById(R.id.tv_score);
        TextView tv_highestScore = findViewById(R.id.tv_highestScore);
        TextView tv_adventureTime = findViewById(R.id.tv_adventureTime);
        TextView tv_distance = findViewById(R.id.tv_distance);
        Button btn_finish = findViewById(R.id.btn_resultFinish);

        int score = (int) (Math.log10(10800/timeSecond)/Math.log10(2));
        tv_score.setText(score);
        tv_adventureTime.setText("Adventure Time: " + timeString);
        tv_distance.setText("Travel Distance: " + distance + " km");

        SharedPreferences sharedPreferences = getSharedPreferences("settingPreferences", MODE_PRIVATE);
        if (!sharedPreferences.getBoolean("HAS_RECORD", false)) {
            sharedPreferences.edit()
                    .putInt("HIGH_SCORE", score)
                    .putBoolean("NO_RECORD", true)
                    .apply();
        } else {
            if (sharedPreferences.getInt("HIGH_SCORE", 0) < score) {
                sharedPreferences.edit()
                        .putInt("HIGH_SCORE", score)
                        .apply();
            }
            tv_highestScore.setText(sharedPreferences.getInt("HIGH_SCORE", 0));
        }

        btn_finish.setOnClickListener(v -> {
            finish();
        });
    }
}