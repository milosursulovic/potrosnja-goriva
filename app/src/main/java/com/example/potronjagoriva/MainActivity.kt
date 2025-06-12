package com.example.potronjagoriva

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var editLiters: EditText
    private lateinit var editKilometers: EditText
    private lateinit var addButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var exportButton: Button

    private val entries = mutableListOf<FuelEntry>()
    private lateinit var adapter: FuelAdapter
    private lateinit var dbHelper: FuelDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        editLiters = findViewById(R.id.edit_liters)
        editKilometers = findViewById(R.id.edit_kilometers)
        addButton = findViewById(R.id.add_button)
        recyclerView = findViewById(R.id.recycler_view)
        exportButton = findViewById(R.id.export_button)

        dbHelper = FuelDatabaseHelper(this)
        entries.addAll(dbHelper.getAllEntries())

        adapter = FuelAdapter(entries) { entryToDelete ->
            dbHelper.deleteEntry(entryToDelete.id)
            val index = entries.indexOfFirst { it.id == entryToDelete.id }
            if (index != -1) {
                entries.removeAt(index)
                adapter.notifyItemRemoved(index)
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        addButton.setOnClickListener {
            val litersText = editLiters.text.toString()
            val kmText = editKilometers.text.toString()

            if (litersText.isNotBlank() && kmText.isNotBlank()) {
                val liters = litersText.toDoubleOrNull()
                val km = kmText.toDoubleOrNull()

                if (liters != null && km != null && km > 0) {
                    val consumption = (liters / km) * 100
                    val timestamp = System.currentTimeMillis()
                    val entry = FuelEntry(liters, km, consumption, timestamp)
                    val id = dbHelper.insertEntry(entry)
                    entries.add(0, entry.copy(id = id)) // ID iz baze
                    adapter.notifyItemInserted(0)
                    recyclerView.scrollToPosition(0)
                    editLiters.text.clear()
                    editKilometers.text.clear()
                } else {
                    Toast.makeText(this, "Unesite validne brojeve.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Popunite oba polja.", Toast.LENGTH_SHORT).show()
            }
        }

        exportButton.setOnClickListener {
            val allEntries = dbHelper.getAllEntries()
            if (allEntries.isEmpty()) {
                Toast.makeText(this, "Nema podataka za eksport.", Toast.LENGTH_SHORT).show()
            } else {
                exportToPdf(allEntries)
            }
        }
    }

    private fun exportToPdf(entries: List<FuelEntry>) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()

        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        titlePaint.textSize = 18f
        titlePaint.color = Color.BLACK

        paint.textSize = 14f

        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        var y = 50f
        canvas.drawText("Izveštaj potrošnje goriva", 200f, y, titlePaint)
        y += 30f

        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        entries.forEachIndexed { index, entry ->
            val line = "${index + 1}. L: %.2f | Km: %.2f | %.2f l/100km | %s".format(
                entry.liters,
                entry.kilometers,
                entry.consumption,
                sdf.format(Date(entry.timestamp))
            )

            if (y > 800f) {
                pdfDocument.finishPage(page)
                return@forEachIndexed
            }

            canvas.drawText(line, 30f, y, paint)
            y += 22f
        }

        pdfDocument.finishPage(page)

        val filename = "Potrosnja_${System.currentTimeMillis()}.pdf"
        val downloadsDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, filename)

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(this, "PDF sačuvan: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Greška prilikom čuvanja PDF-a", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }

        pdfDocument.close()
    }
}