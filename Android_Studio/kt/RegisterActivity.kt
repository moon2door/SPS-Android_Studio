package com.example.smartpetshelter_app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val etEmail = findViewById<EditText>(R.id.etRegEmail)
        val etPassword = findViewById<EditText>(R.id.etRegPassword)
        val btnComplete = findViewById<Button>(R.id.btnRegComplete)
        val btnBack = findViewById<Button>(R.id.btnRegComplete2)

        // 1. 뒤로가기 버튼 기능
        btnBack.setOnClickListener {
            finish()
        } // <--- 여기서 닫아야 합니다! (독립적인 기능)

        // 2. 가입완료 버튼 기능 (밖으로 꺼내줌)
        btnComplete.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all the information.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 파이어베이스에 유저 생성
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Sign-up successful! Please log in.", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(
                            this,
                            "Sign-up failed: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }
}