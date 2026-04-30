package com.example.duxscholar

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class ServicesActivity : AppCompatActivity() {

    lateinit var recycler: RecyclerView
    lateinit var list: MutableList<InfoAcademica>
    lateinit var adapter: ServiceAdapter
    lateinit var db: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_services)

        recycler = findViewById(R.id.recyclerServices)

        list = mutableListOf()

        adapter = ServiceAdapter(list) { item ->
            val intent = Intent(this, ServicesTemplateActivity::class.java)
            intent.putExtra("TITLE", item.name)
            intent.putExtra("CONTENT", item.content)
            startActivity(intent)
        }

        val btnBack = findViewById<Button>(R.id.btnBackServices)

        btnBack.setOnClickListener {
            finish()
        }

        recycler.layoutManager = GridLayoutManager(this, 2)
        recycler.adapter = adapter

        db = FirebaseDatabase.getInstance().getReference("infacademicas")

        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                list.clear()

                for (data in snapshot.children) {
                    val item = data.getValue(InfoAcademica::class.java)
                    item?.let { list.add(it) }
                }

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}