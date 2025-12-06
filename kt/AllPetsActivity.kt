package com.example.smartpetshelter_app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog // [추가] 바텀시트
import com.google.firebase.firestore.FirebaseFirestore

// 데이터 모델
data class PetUser(
    val id: String,
    val breed: String,
    val age: String,
    val weight: String,
    val contact: String,
    val location: String,
    val condition: String,
    val feature: String, // [추가] 특징 필터링하려면 이것도 필요함
    val imageUrl: String,
)

class AllPetsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var ivFilter: ImageView

    // ★ [핵심] 원본 데이터 보관용 리스트
    private val originalList = ArrayList<PetUser>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_pets)

        recyclerView = findViewById(R.id.recyclerView)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        ivFilter = findViewById(R.id.ivFilter)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // 1. 화살표 누르면 -> 필터 창 열기
        ivFilter.setOnClickListener {
            showFilterDialog()
        }

        // 2. 전체 데이터 불러오기
        loadAllPets()
    }

    // ==========================================
    // [기능 1] 필터 창 띄우기 & 로직
    // ==========================================
    private fun showFilterDialog() {
        // 바텀 시트(아래에서 올라오는 창) 생성
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(R.layout.dialog_filter)

        val etBreed = dialog.findViewById<EditText>(R.id.etFilterBreed)
        val etAge = dialog.findViewById<EditText>(R.id.etFilterAge)
        val etCondition = dialog.findViewById<EditText>(R.id.etFilterCondition)
        val etFeature = dialog.findViewById<EditText>(R.id.etFilterFeature)
        val btnApply = dialog.findViewById<Button>(R.id.btnApply)
        val btnReset = dialog.findViewById<Button>(R.id.btnReset)

        // [초기화 버튼]
        btnReset?.setOnClickListener {
            // 원본 리스트를 그대로 다시 보여줌
            updateList(originalList)
            dialog.dismiss()
            Toast.makeText(this, "The filter has been reset.", Toast.LENGTH_SHORT).show()
        }

        // [적용 버튼] ★ 필터링 로직의 핵심
        btnApply?.setOnClickListener {
            val breedKey = etBreed?.text.toString().trim()
            val ageKey = etAge?.text.toString().trim()
            val condKey = etCondition?.text.toString().trim()
            val featKey = etFeature?.text.toString().trim()

            // 원본 리스트(originalList)에서 조건에 맞는 것만 걸러냄(filter)
            val filteredList = originalList.filter { pet ->
                // 입력한 단어가 포함되어 있으면 통과 (비어있으면 무조건 통과)
                val matchBreed = breedKey.isEmpty() || pet.breed.contains(breedKey)
                val matchAge = ageKey.isEmpty() || pet.age.contains(ageKey)
                val matchCond = condKey.isEmpty() || pet.condition.contains(condKey)
                val matchFeat = featKey.isEmpty() || pet.feature.contains(featKey)

                // 4가지 조건이 모두(AND) 맞아야 함
                matchBreed && matchAge && matchCond && matchFeat
            }

            // 걸러진 리스트로 화면 갱신
            updateList(filteredList)
            dialog.dismiss()

            if (filteredList.isEmpty()) {
                Toast.makeText(this, "No results match the criteria.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "${filteredList.size}results were found.", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    // 화면 갱신 전용 함수
    private fun updateList(list: List<PetUser>) {
        if (list.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
        // 어댑터 갈아끼우기
        recyclerView.adapter = UserPetAdapter(list)
    }

    // ==========================================
    // [기능 2] 데이터 불러오기
    // ==========================================
    private fun loadAllPets() {
        val db = FirebaseFirestore.getInstance()
        originalList.clear() // 중복 방지

        db.collection("pets")
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val p = PetUser(
                        id = document.id,
                        breed = document.getString("breed") ?: "",
                        age = document.getString("age") ?: "",
                        weight = document.getString("weight") ?: "",
                        contact = document.getString("contact") ?: "",
                        location = document.getString("location") ?: "Location unknown",
                        condition = document.getString("condition") ?: "",
                        feature = document.getString("feature") ?: "", // 특징 가져오기
                        imageUrl = document.getString("imageUrl") ?: ""
                    )
                    originalList.add(p)
                }
                originalList.reverse() // 최신순

                // 처음엔 원본 그대로 보여줌
                updateList(originalList)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load the list", Toast.LENGTH_SHORT).show()
            }
    }

    // 어댑터 (기존과 동일)
    inner class UserPetAdapter(private val items: List<PetUser>) : RecyclerView.Adapter<UserPetAdapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivImg: ImageView = view.findViewById(R.id.ivItemImage)
            val tvTitle: TextView = view.findViewById(R.id.tvItemTitle)
            val tvInfo: TextView = view.findViewById(R.id.tvItemInfo)
            val btnContact: Button = view.findViewById(R.id.btnContact)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pet_user, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvTitle.text = "${item.breed} (${item.age}years old)"
            holder.tvInfo.text = "Size (weight) : ${item.weight}kg\nCondition : ${item.condition}\nFeatures : ${item.feature}" // 특징도 표시
            holder.tvInfo.text = "Found location : ${item.location}\nCondition : ${item.condition}\nFeatures : ${item.feature}"
            holder.btnContact.text = "📞  Contact (${item.contact})"

            if (item.imageUrl.isNotEmpty()) {
                Glide.with(holder.itemView.context).load(item.imageUrl).into(holder.ivImg)
            }

            holder.btnContact.setOnClickListener {
                if (item.contact.isNotEmpty() && item.contact != "-") {
                    val intent = Intent(Intent.ACTION_DIAL)
                    intent.data = Uri.parse("tel:${item.contact}")
                    startActivity(intent)
                } else {
                    Toast.makeText(holder.itemView.context, "No contact information available.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        override fun getItemCount() = items.size
    }
}