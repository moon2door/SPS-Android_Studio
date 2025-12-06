package com.example.smartpetshelter_app // 패키지명 확인

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth // [필수] 내 아이디 확인용
import com.google.firebase.firestore.FirebaseFirestore

// [데이터 모델]
data class Pet(
    val id: String,
    val breed: String,
    val age: String,
    val weight: String,
    val contact: String,
    val condition: String,
    val imageUrl: String,
    val location: String
)

class MyUploadsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmptyState: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_uploads)

        // UI 연결
        recyclerView = findViewById(R.id.recyclerView)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // 데이터 불러오기 시작
        loadDataFromFirestore()
    }

    private fun loadDataFromFirestore() {
        val db = FirebaseFirestore.getInstance()
        val petList = ArrayList<Pet>()

        // 1. 현재 로그인한 사용자 ID(uid) 가져오기
        val myUid = FirebaseAuth.getInstance().currentUser?.uid

        if (myUid == null) {
            Toast.makeText(this, "No login information.", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. 쿼리 날리기: "userId가 내 아이디(myUid)랑 똑같은 것만 줘!"
        db.collection("pets")
            .whereEqualTo("userId", myUid) // ★ 핵심: 내 것만 필터링
            .orderBy("timestamp")          // 시간순 정렬
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val p = Pet(
                        id = document.id,
                        breed = document.getString("breed") ?: "No information available",
                        age = document.getString("age") ?: "0",
                        weight = document.getString("weight") ?: "0",
                        contact = document.getString("contact") ?: "-",
                        condition = document.getString("condition") ?: "-",
                        imageUrl = document.getString("imageUrl") ?: "",
                        location = document.getString("location") ?: "Unknown location"
                    )
                    petList.add(p)
                }

                // 3. 최신순으로 뒤집기
                petList.reverse()

                // 4. 데이터 개수에 따라 "텅 빔" 화면 보여줄지 결정
                updateEmptyState(petList.size)

                // 5. 리스트 연결
                recyclerView.adapter = PetAdapter(petList)
            }
            .addOnFailureListener { e ->
                // ★ [중요] 인덱스 에러가 나면 로그창의 링크를 눌러야 함
                Toast.makeText(this, "Loading failed: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
    }

    // 화면 상태 갱신 (목록 vs 텅 빔 메시지)
    private fun updateEmptyState(itemCount: Int) {
        if (itemCount == 0) {
            tvEmptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    // ==========================================
    // 리사이클러뷰 어댑터 (목록 관리자)
    // ==========================================
    inner class PetAdapter(private val items: MutableList<Pet>) : RecyclerView.Adapter<PetAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivImg: ImageView = view.findViewById(R.id.ivItemImage)
            val tvTitle: TextView = view.findViewById(R.id.tvItemTitle)
            val tvInfo: TextView = view.findViewById(R.id.tvItemInfo)
            val btnDel: Button = view.findViewById(R.id.btnDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pet, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]

            holder.tvTitle.text = "${item.breed} (${item.age}years old)"
            holder.tvInfo.text = "Found location: ${item.location}\nWeight : ${item.weight}kg\nContact : ${item.contact}\nCondition : ${item.condition}"

            if (item.imageUrl.isNotEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(item.imageUrl)
                    .into(holder.ivImg)
            }

            // 삭제 버튼
            holder.btnDel.setOnClickListener {
                showDeleteConfirmDialog(item, position)
            }
        }

        override fun getItemCount() = items.size

        // 삭제 확인 팝업
        private fun showDeleteConfirmDialog(item: Pet, position: Int) {
            AlertDialog.Builder(this@MyUploadsActivity)
                .setTitle("Confirm deletion")
                .setMessage("Are you sure you want to delete?")
                .setPositiveButton("Delete") { _, _ ->
                    FirebaseFirestore.getInstance().collection("pets").document(item.id)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(applicationContext, "Deleted.", Toast.LENGTH_SHORT).show()
                            items.removeAt(position)
                            notifyItemRemoved(position)
                            notifyItemRangeChanged(position, items.size)

                            // 지우고 나서 0개가 되면 안내 문구 띄우기
                            updateEmptyState(items.size)
                        }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}