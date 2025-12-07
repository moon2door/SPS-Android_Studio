package com.example.smartpetshelter_app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class AdminActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // [중요] 화면(XML)을 가장 먼저 불러와야 합니다!! (1순위)
        setContentView(R.layout.activity_admin)

        // -------------------------------------------------------
        // 이제 화면이 있으니까 버튼을 찾을 수 있습니다.
        // -------------------------------------------------------

        // 1. 나의 업로드 내역 버튼 연결
        val cardUploadHistory = findViewById<CardView>(R.id.cardUploadHistory)
        cardUploadHistory.setOnClickListener {
            val intent = Intent(this, MyUploadsActivity::class.java)
            startActivity(intent)
        }

        // 2. 사진 촬영 버튼 연결
        val cardTakePhoto = findViewById<CardView>(R.id.cardTakePhoto)
        cardTakePhoto.setOnClickListener {
            val intent = Intent(this, AddPetActivity::class.java)
            startActivity(intent)
        }
    }
}