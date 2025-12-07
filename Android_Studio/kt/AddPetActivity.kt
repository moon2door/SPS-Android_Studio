package com.example.smartpetshelter_app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Geocoder
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.UUID

class AddPetActivity : AppCompatActivity() {

    private lateinit var ivPetImage: ImageView
    private lateinit var tvLocation: TextView // [추가] 위치 텍스트
    private lateinit var etBreed: EditText
    private lateinit var etAge: EditText
    private lateinit var etWeight: EditText
    private lateinit var etCondition: EditText
    private lateinit var etFeature: EditText
    private lateinit var etContact: EditText
    private lateinit var btnSave: Button

    private var selectedBitmap: Bitmap? = null
    private var currentLocationString: String = "No location information." // 저장할 위치 문자열

    // API 키 (본인 키 유지)
    private val apiKey = "AIzaSyBgBnh6mizvtscp4ywh_8t_rsDQFxB2B0E"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_pet)

        // UI 연결
        ivPetImage = findViewById(R.id.ivPetImage)
        tvLocation = findViewById(R.id.tvLocation) // [추가]
        etBreed = findViewById(R.id.etBreed)
        etAge = findViewById(R.id.etAge)
        etWeight = findViewById(R.id.etWeight)
        etCondition = findViewById(R.id.etCondition)
        etFeature = findViewById(R.id.etFeature)
        etContact = findViewById(R.id.etContact)
        btnSave = findViewById(R.id.btnSave)

        // 1. 권한 요청 (카메라 + 위치 같이 요청)
        val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
            val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

            if (cameraGranted) {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                // 카메라 실행 (런처가 따로 있음)
                cameraResultLauncher.launch(intent)
            } else {
                Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show()
            }

            if (locationGranted) {
                getCurrentLocation() // 위치 가져오기 시작
            } else {
                tvLocation.text = "📍 Location permission was denied."
            }
        }

        // 2. 카메라 결과 처리

        // 3. 이미지 클릭 시 -> 권한 요청 (카메라 & 위치)
        ivPetImage.setOnClickListener {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        // 4. 연락처 포맷팅
        etContact.addTextChangedListener(object : TextWatcher {
            var isFormatting = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || s == null) return
                isFormatting = true
                val str = s.toString().replace(Regex("[^\\d]"), "")
                val formatted = when {
                    str.length > 6 -> "${str.substring(0, 3)}-${str.substring(3, 6)}-${str.substring(6)}"
                    str.length > 3 -> "${str.substring(0, 3)}-${str.substring(3)}"
                    else -> str
                }
                s.replace(0, s.length, formatted)
                isFormatting = false
            }
        })

        // 5. 저장 버튼
        btnSave.setOnClickListener {
            if (selectedBitmap == null) {
                Toast.makeText(this, "Please take a photo first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            uploadDataToFirebase()
        }
    }

    // 카메라 결과 런처를 변수로 뺌 (위에서 호출해야 해서)
    private val cameraResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                selectedBitmap = imageBitmap
                ivPetImage.setImageBitmap(imageBitmap)
                ivPetImage.setBackgroundColor(0)
                analyzeImageWithGemini(imageBitmap)
            }
        }
    }

    // [기능 1] 위치 가져오기 (GPS -> 주소 변환)
    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                try {
                    // 위도, 경도를 주소(한글)로 변환
                    val geocoder = Geocoder(this, Locale.KOREA)
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                    if (!addresses.isNullOrEmpty()) {
                        // "대한민국 서울특별시 강남구 역삼동 123-4" -> "서울특별시 강남구 역삼동" 정도로 다듬기
                        val fullAddress = addresses[0].getAddressLine(0)
                        val shortAddress = fullAddress.replace("대한민국 ", "") // "대한민국" 글자 제거

                        currentLocationString = shortAddress
                        tvLocation.text = "📍 Found location: $shortAddress"
                    }
                } catch (e: Exception) {
                    tvLocation.text = "📍 Failed to convert address (check network)"
                }
            } else {
                tvLocation.text = "📍 Unable to find location (check GPS)"
            }
        }
    }

    // [기능 2] 업로드 (위치 정보 포함)
    private fun uploadDataToFirebase() {
        Toast.makeText(this, "Saving to the server...", Toast.LENGTH_SHORT).show()
        btnSave.isEnabled = false

        val storage = FirebaseStorage.getInstance()
        val db = FirebaseFirestore.getInstance()
        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid ?: "unknown"

        val fileName = "pets/${UUID.randomUUID()}.jpg"
        val storageRef = storage.reference.child(fileName)

        val baos = ByteArrayOutputStream()
        selectedBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        storageRef.putBytes(data)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    // DB 저장 데이터 뭉치
                    val petData = hashMapOf(
                        "userId" to uid,
                        "breed" to etBreed.text.toString(),
                        "age" to etAge.text.toString(),
                        "weight" to etWeight.text.toString(),
                        "condition" to etCondition.text.toString(),
                        "feature" to etFeature.text.toString(),
                        "contact" to etContact.text.toString(),
                        "location" to currentLocationString, // ★ [추가] 위치 정보 저장
                        "imageUrl" to uri.toString(),
                        "timestamp" to System.currentTimeMillis()
                    )

                    db.collection("pets")
                        .add(petData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Registration complete!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
                            btnSave.isEnabled = true
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show()
                btnSave.isEnabled = true
            }
    }

    // [기능 3] AI 분석 (기존 유지, 안전장치 포함)
    private fun analyzeImageWithGemini(bitmap: Bitmap) {
        Toast.makeText(this, "AI is analyzing the dog...", Toast.LENGTH_LONG).show()

        lifecycleScope.launch {
            try {
                val generativeModel = GenerativeModel(
                    modelName = "gemini-2.5-pro",
                    apiKey = apiKey
                )
                val prompt = """            
                    Analyze the dog in this photo and return ONLY a JSON object in English. Do not say anything else.

                    Format:
                    {
                    "breed": "Dog breed (e.g., Golden Retriever)",
                    "age": "Estimated age (numbers only, e.g., 3)",
                    "weight": "Estimated weight in kg (numbers only, e.g., 15.5)",
                    "condition": "Brief health condition in English (e.g., Healthy coat and looks well)", 
                    "feature": "Notable features in English (e.g., Floppy ears and gentle-looking)"
                    }
                """.trimIndent()

                val inputContent = content { image(bitmap); text(prompt) }
                val response = generativeModel.generateContent(inputContent)

                if (isFinishing || isDestroyed) return@launch

                response.text?.let { parseAndFillData(it) }

            } catch (e: Exception) {
                // 에러 무시 (UI 죽지 않게)
            }
        }
    }

    private fun parseAndFillData(jsonString: String) {
        try {
            val cleanJson = jsonString.replace("```json", "").replace("```", "").trim()
            val jsonObject = JSONObject(cleanJson)
            etBreed.setText(jsonObject.optString("breed"))
            etAge.setText(jsonObject.optString("age"))
            etWeight.setText(jsonObject.optString("weight"))
            etCondition.setText(jsonObject.optString("condition"))
            etFeature.setText(jsonObject.optString("feature"))
            Toast.makeText(this, "Analysis complete!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) { }
    }
}