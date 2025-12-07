package com.example.smartpetshelter_app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class UserActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)

        // 버튼 연결
        val cardViewList = findViewById<CardView>(R.id.cardViewList)

        cardViewList.setOnClickListener {
            // 유저용 전체 목록 화면(AllPetsActivity)으로 이동
            val intent = Intent(this, AllPetsActivity::class.java)
            startActivity(intent)
        }
    }
}