package scm.kaifwong8_cswong274.hideandseek;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn_map = findViewById(R.id.btn_map);
        btn_map.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, MapsActivity.class);
            startActivity(i);
        });
    }
}