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
    private lateinit var editFuelPrice: EditText
    private lateinit var addButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var exportButton: Button
    private lateinit var editDate: EditText

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
        editFuelPrice = findViewById(R.id.edit_fuel_price)
        addButton = findViewById(R.id.add_button)
        recyclerView = findViewById(R.id.recycler_view)
        exportButton = findViewById(R.id.export_button)
        editDate = findViewById(R.id.edit_date)

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
            val fuelPriceText = editFuelPrice.text.toString()

            if (litersText.isNotBlank() && kmText.isNotBlank() && fuelPriceText.isNotBlank()) {
                val liters = litersText.toDoubleOrNull()
                val km = kmText.toDoubleOrNull()
                val fuelPrice = fuelPriceText.toIntOrNull()

                if (liters != null && km != null && fuelPrice != null && km > 0) {
                    val consumption = (liters / km) * 100
                    val dateText = editDate.text.toString()
                    val timestamp = if (dateText.isNotBlank()) {
                        try {
                            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                            sdf.parse(dateText)?.time ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }
                    } else {
                        System.currentTimeMillis()
                    }

                    val entry = FuelEntry(liters, km, consumption, fuelPrice, timestamp)
                    val id = dbHelper.insertEntry(entry)
                    entries.add(0, entry.copy(id = id))
                    adapter.notifyItemInserted(0)
                    recyclerView.scrollToPosition(0)
                    editLiters.text.clear()
                    editKilometers.text.clear()
                    editFuelPrice.text.clear()
                    editDate.text.clear()
                } else {
                    Toast.makeText(this, "Unesite validne brojeve.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Popunite oba polja.", Toast.LENGTH_SHORT).show()
            }
        }

        editDate.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH)
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

            val datePicker = android.app.DatePickerDialog(this, { _, y, m, d ->
                val formattedDate = String.format("%02d.%02d.%04d", d, m + 1, y)
                editDate.setText(formattedDate)
            }, year, month, day)

            datePicker.show()
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

        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        var y = 50f
        canvas.drawText("Izveštaj potrošnje goriva", 200f, y, titlePaint)
        y += 30f

        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        var totalFuel = 0.0
        var totalKilometers = 0.0
        var totalCost = 0.0
        var totalConsumption = 0.0

        val firstDate =
            entries.minByOrNull { it.timestamp }?.timestamp ?: System.currentTimeMillis()
        val lastDate = entries.maxByOrNull { it.timestamp }?.timestamp ?: System.currentTimeMillis()

        entries.forEachIndexed { index, entry ->
            val cost = entry.liters * entry.fuelPrice
            val line =
                "${index + 1}. %s | L: %.2f | Km: %.2f | Cena: %d RSD | Potrošnja: %.2f | Plaćeno: %.2f RSD".format(
                    sdf.format(Date(entry.timestamp)),
                    entry.liters,
                    entry.kilometers,
                    entry.fuelPrice,
                    entry.consumption,
                    cost
                )

            if (y > 800f) {
                pdfDocument.finishPage(page)
                return@forEachIndexed
            }

            canvas.drawText(line, 30f, y, paint)
            y += 22f

            totalFuel += entry.liters
            totalKilometers += entry.kilometers
            totalConsumption += entry.consumption
            totalCost += cost
        }

        // Računanje proseka
        val averagePricePerLiter = if (totalFuel > 0) totalCost / totalFuel else 0.0
        val averageConsumption = if (entries.isNotEmpty()) totalConsumption / entries.size else 0.0

        y += 30f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 16f

        canvas.drawText(
            "Za period ${sdf.format(Date(firstDate))} - ${sdf.format(Date(lastDate))}",
            30f,
            y,
            paint
        )
        y += 22f
        canvas.drawText("Ukupno sipano: %.2f L".format(totalFuel), 30f, y, paint)
        y += 22f
        canvas.drawText("Ukupno pređeno: %.2f km".format(totalKilometers), 30f, y, paint)
        y += 22f
        canvas.drawText(
            "Prosečna cena po litru: %.2f RSD".format(averagePricePerLiter),
            30f,
            y,
            paint
        )
        y += 22f
        canvas.drawText(
            "Prosečna potrošnja: %.2f l/100km".format(averageConsumption),
            30f,
            y,
            paint
        )
        y += 22f
        canvas.drawText("Ukupno potrošeno novca: %.2f RSD".format(totalCost), 30f, y, paint)

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