package io.github.sceneview.sample.arcursorplacement

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val button = findViewById<Button>(R.id.launch_button)
        button.setOnClickListener {
            val intent = Intent(this, Activity::class.java)
            startActivity(intent)
        }
    }
}