package com.example.smartpetshelter_app // 패키지 이름은 본인 것으로 확인

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnAdmin = findViewById<Button>(R.id.btnAdmin)
        val btnUser = findViewById<Button>(R.id.btnUser)

        // Admin 버튼 클릭 시
        btnAdmin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("USER_TYPE", "ADMIN") // "나는 어드민이다"라는 정보를 담음
            startActivity(intent)
        }

        // User 버튼 클릭 시
        btnUser.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("USER_TYPE", "USER") // "나는 유저다"라는 정보를 담음
            startActivity(intent)
        }
    }
}