package com.example.duxscholar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class NewsActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var list: MutableList<Noticia>
    private lateinit var adapter: NewsAdapter
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news)

        recycler = findViewById(R.id.recyclerNews)
        list = mutableListOf()

        adapter = NewsAdapter(list) { item ->
            val intent = Intent(this, NewsTemplateActivity::class.java)
            intent.putExtra("TITLE", item.name)
            intent.putExtra("HEADER", item.header)
            intent.putExtra("CONTENT", item.content)
            intent.putExtra("IMAGE", item.image)
            intent.putExtra("DATE", item.date)
            startActivity(intent)
        }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        databaseReference = FirebaseDatabase.getInstance().getReference("noticias")

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                list.clear()
                for (data in snapshot.children) {
                    val item = data.getValue(Noticia::class.java)
                    item?.let { list.add(it) }
                }
                Log.d("NewsActivity", "Data changed: ${list.size} items loaded")
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("NewsActivity", "Database error: ${error.message}")
            }
        })
    }
}