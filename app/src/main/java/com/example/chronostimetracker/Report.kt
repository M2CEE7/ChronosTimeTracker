package com.example.chronostimetracker

import android.app.DatePickerDialog
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.AdapterView
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.utils.MPPointF
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class Report : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CategoryTimeAdapter
    private lateinit var clearButton: ImageButton
    private lateinit var viewContainer: ConstraintLayout
    private lateinit var projectView: ConstraintLayout
    private lateinit var CategoryDisplayButton: Button
    lateinit var pieChart: PieChart
    var selectedButton: Button? = null
    private lateinit var startDateButton: Button
    private lateinit var endDateButton: Button
    private lateinit var applyFilterButton: Button
    private var startDate: String? = null
    private var endDate: String? = null
    private lateinit var dateRangeSpinner: Spinner
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_report)

        val toolbar: Toolbar = findViewById(R.id.my_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Reports"

        pieChart = findViewById(R.id.pieChart)
        configurePieChart()
        // Initial data load without filters
        fetchAndPopulateLineChart(null, null)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.BottomNavigationView)
        val btnProject: Button = findViewById(R.id.btnProject)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize views
        viewContainer = findViewById(R.id.viewContainer)
        projectView = findViewById(R.id.projectContainer)
        clearButton = findViewById(R.id.clearButton)
        CategoryDisplayButton = findViewById(R.id.CategoryDisplayButton)




        // Initialize Spinner
        dateRangeSpinner = findViewById(R.id.dateRangeSpinner)
        setupDateRangeSpinner()



        clearButton.setOnClickListener {
            viewContainer.visibility = View.GONE
        }

        CategoryDisplayButton.setOnClickListener {
            projectView.visibility = View.GONE
            viewContainer.visibility = View.VISIBLE

            btnProject.setTextColor(resources.getColor(R.color.white))
            btnProject.setBackgroundResource(R.drawable.black_border)

            CategoryDisplayButton.isPressed = true
            CategoryDisplayButton.setTextColor(resources.getColor(R.color.black))
            CategoryDisplayButton.setBackgroundResource(R.drawable.button_selector)

            // Set the clicked button as the selected button
            selectedButton = btnProject
        }

        btnProject.setOnClickListener {
            viewContainer.visibility = View.GONE
            projectView.visibility = View.VISIBLE

            CategoryDisplayButton.setTextColor(resources.getColor(R.color.white))
            CategoryDisplayButton.setBackgroundResource(R.drawable.black_border)
            // Update UI for the clicked button
            btnProject.isPressed = true
            btnProject.setTextColor(resources.getColor(R.color.black))
            btnProject.setBackgroundResource(R.drawable.button_selector)
            // Set the clicked button as the selected button
            selectedButton = btnProject
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_home -> {
                    val intent = Intent(this, ListOfEntries::class.java)
                    startActivity(intent)
                    true
                }

                R.id.action_add -> {
                    val intent = Intent(this, TimesheetEntry::class.java)
                    startActivity(intent)
                    true
                }
                R.id.pomodoro -> {
                    // Open Pomodoro dialog when the Pomodoro item is clicked
                    val pomodoroDialog = Dialog(this)
                    pomodoroDialog.setContentView(R.layout.dialog_pomodoro_timer)
                    val pomodoroTimer = PomodoroTimer(this)
                    pomodoroTimer.setupDialog(pomodoroDialog)
                    pomodoroDialog.show()
                    true
                }

                else -> false
            }
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val database =
                FirebaseDatabase.getInstance().reference.child("user_entries").child(user.uid)
            retrieveCategoryTimes(database)
            displayTotalTimeTracked(database)
        }


    }



    private fun configurePieChart() {
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.setExtraOffsets(5f, 10f, 5f, 5f)
        pieChart.dragDecelerationFrictionCoef = 0.95f
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.WHITE)
        pieChart.setTransparentCircleColor(Color.WHITE)
        pieChart.setTransparentCircleAlpha(110)
        pieChart.holeRadius = 58f
        pieChart.transparentCircleRadius = 61f
        pieChart.setDrawCenterText(true)
        pieChart.rotationAngle = 0f
        pieChart.isRotationEnabled = true
        pieChart.setHighlightPerTapEnabled(true)
        pieChart.animateY(1400, Easing.EaseInOutQuad)
        pieChart.legend.isEnabled = false
        pieChart.setEntryLabelColor(Color.WHITE)
        pieChart.setEntryLabelTextSize(12f)
    }

    private fun retrieveCategoryTimes(database: DatabaseReference) {
        val categoryTimesRef = database.child("CategoryTimes")
        categoryTimesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val categories = mutableMapOf<String, Long>()
                for (categorySnapshot in dataSnapshot.children) {
                    val category = categorySnapshot.key ?: ""
                    val totalTime = categorySnapshot.child("totalTime").getValue(Long::class.java) ?: 0
                    categories[category] = totalTime
                }

                updatePieChart(categories)
                displayCategoryTimes(categories)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Failed to retrieve category times", databaseError.toException())
            }
        })
    }

    private fun updatePieChart(categories: Map<String, Long>) {
        val entries = ArrayList<PieEntry>()
        for ((category, totalTime) in categories) {
            entries.add(PieEntry(totalTime.toFloat(), category))
        }

        val colors = ArrayList<Int>()
        val predefinedColors = listOf(
            resources.getColor(R.color.blue),
            resources.getColor(R.color.yellow),
            resources.getColor(R.color.red),
            resources.getColor(R.color.lightGreen),
            resources.getColor(R.color.purple_200),
            resources.getColor(R.color.veryLightYellow)
        )
        var colorIndex = 0
        for (i in categories.keys.indices) {
            colors.add(predefinedColors[colorIndex])
            colorIndex = (colorIndex + 1) % predefinedColors.size
        }

        val dataSet = PieDataSet(entries, "Categories")
        dataSet.setDrawIcons(false)
        dataSet.sliceSpace = 3f
        dataSet.iconsOffset = MPPointF(0f, 40f)
        dataSet.selectionShift = 5f
        dataSet.colors = colors

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter())
        data.setValueTextSize(15f)
        data.setValueTypeface(Typeface.DEFAULT_BOLD)
        data.setValueTextColor(Color.WHITE)
        pieChart.data = data
        pieChart.highlightValues(null)
        pieChart.invalidate()
    }

    private fun displayCategoryTimes(categories: Map<String, Long>) {
        val categoryTimeList = categories.map { (category, totalTime) ->
            val hours = TimeUnit.MILLISECONDS.toHours(totalTime)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(totalTime) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(totalTime) % 60
            val formattedTime = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            CategoryTime(category, formattedTime)
        }

        adapter = CategoryTimeAdapter(categoryTimeList)
        recyclerView.adapter = adapter
    }

    private fun displayTotalTimeTracked(database: DatabaseReference) {
        val totalTimeTextView = findViewById<TextView>(R.id.totalTimeTextView)
        val totalTimeTrackedRef = database.child("totalTimeTracked")

        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val totalTimeRef = totalTimeTrackedRef.child(currentDate).child("Time")


        totalTimeRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val totalMilliseconds = dataSnapshot.getValue(Long::class.java) ?: 0
                Log.d("Totaltime", "Total Time Tracked: $totalMilliseconds milliseconds")

                val totalHours = totalMilliseconds / (1000 * 60 * 60)
                val totalMinutes = (totalMilliseconds % (1000 * 60 * 60)) / (1000 * 60)
                val totalSeconds = (totalMilliseconds % (1000 * 60)) / 1000

                val formattedTotalTime = String.format(Locale.getDefault(), "%02d:%02d:%02d", totalHours, totalMinutes, totalSeconds)
                totalTimeTextView.text = "Total Time Tracked today: $formattedTotalTime"
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Failed to retrieve totalTimeTracked", databaseError.toException())
            }
        })
    }

    fun updateLineChart(
        entries: List<Entry>,
        minGoalEntries: List<Entry>,
        maxGoalEntries: List<Entry>,
        // List of date labels corresponding to the total time tracked
        dateLabels: List<String> )
    {
        val lineChart = findViewById<LineChart>(R.id.lineChart)

        val lineDataSet = LineDataSet(entries, "Total Time Tracked")
        val minGoalDataSet = LineDataSet(minGoalEntries, "Min Daily Goal")
        val maxGoalDataSet = LineDataSet(maxGoalEntries, "Max Daily Goal")

        // Customize the line appearance for total time tracked
        lineDataSet.color = Color.WHITE
        lineDataSet.valueTextColor = Color.YELLOW
        lineDataSet.lineWidth = 2f
        lineDataSet.circleRadius = 4f
        lineDataSet.setCircleColor(Color.YELLOW)
        lineDataSet.valueTextSize = 12f
        lineDataSet.setDrawFilled(true)
        lineDataSet.fillAlpha = 50
        lineDataSet.setDrawHighlightIndicators(true)
        lineDataSet.highLightColor = Color.YELLOW

        // Create custom value formatter for hours, minutes, and seconds
        lineDataSet.valueFormatter = object : ValueFormatter() {
            override fun getPointLabel(entry: Entry): String {
                val totalHours = entry.y.toInt()
                val totalMinutes = ((entry.y - totalHours) * 60).toInt()
                val totalSeconds = ((((entry.y - totalHours) * 60) - totalMinutes) * 60).toInt()

                return String.format("%02d:%02d:%02d", totalHours, totalMinutes, totalSeconds)
            }
        }

        // Customize the line appearance for min goal
        minGoalDataSet.color = Color.GREEN
        minGoalDataSet.valueTextColor = Color.WHITE
        minGoalDataSet.valueTextSize = 12f
        minGoalDataSet.lineWidth = 2f
        minGoalDataSet.circleRadius = 4f
        minGoalDataSet.setDrawCircles(true)

        // Customize the line appearance for max goal
        maxGoalDataSet.color = Color.RED
        maxGoalDataSet.valueTextColor = Color.WHITE
        maxGoalDataSet.valueTextSize = 12f
        maxGoalDataSet.lineWidth = 2f
        maxGoalDataSet.circleRadius = 4f
        maxGoalDataSet.setDrawCircles(true)

        // Create LineData with the customized datasets
        val lineData = LineData(lineDataSet, minGoalDataSet, maxGoalDataSet)
        lineChart.data = lineData

        // Customizing the X-axis to display dates
        val xAxis = lineChart.xAxis
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < dateLabels.size) dateLabels[index] else ""
            }
        }

        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textSize = 12f
        xAxis.textColor = Color.WHITE
        xAxis.labelRotationAngle = -45f
        xAxis.granularity = 1f
        xAxis.setLabelCount(dateLabels.size, true) // Ensure the number of labels matches the dates

        // Add extra offsets to the chart
        lineChart.setExtraOffsets(0f, 60f, 40f, 70f)

        // Customizing the Y-axis
        val yAxisLeft = lineChart.axisLeft
        yAxisLeft.textSize = 12f
        yAxisLeft.textColor = Color.WHITE
        yAxisLeft.axisMinimum = 0f
        val maxEntryValue = (entries + minGoalEntries + maxGoalEntries).maxOfOrNull { it.y } ?: 0f
        yAxisLeft.axisMaximum = maxEntryValue + 1

        // Set Y-axis label
        yAxisLeft.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                return "$value Hours"
            }
        }

        val yAxisRight = lineChart.axisRight
        yAxisRight.isEnabled = false

        // Customizing the LineChart
        lineChart.setBackgroundColor(Color.BLACK)
        lineChart.xAxis.setDrawGridLines(false)
        lineChart.axisLeft.setDrawGridLines(false)

        // Legend customization
        val legend = lineChart.legend
        legend.form = Legend.LegendForm.LINE
        legend.textColor = Color.WHITE
        legend.textSize = 14f
        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false) // To prevent overlapping with chart content

        // Adding spacing between legend items
        legend.xEntrySpace = 15f // Adjust the horizontal space between legend items
        legend.yEntrySpace = 10f // Adjust the vertical space between legend items

        // Add datasets to LineData
        lineChart.data = lineData

        // Description customization
        // Set Description
        val description = Description().apply {
            text = "Time Tracked Over Days"
            textColor = Color.WHITE
            textSize = 20f
        }

// Add layout listener to ensure the chart dimensions are calculated
        lineChart.viewTreeObserver.addOnGlobalLayoutListener {
            // Calculate chart dimensions
            val chartWidth = lineChart.width
            val chartHeight = lineChart.height

            // Set position of description to bottom center
            description.setPosition(chartWidth / 2f, chartHeight.toFloat() - 50f) // X position is center, Y position is near the bottom

            // Apply description to the chart
            lineChart.description = description
        }

        // Refresh the chart
        lineChart.invalidate()
    }
    private fun setupDateRangeSpinner() {
        val dateRanges = resources.getStringArray(R.array.date_range_options)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, dateRanges)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dateRangeSpinner.adapter = adapter

        dateRangeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> fetchAndPopulateLineChart(null, null) // All Time
                    1 -> { // Last 7 Days
                        val calendar = Calendar.getInstance()
                        endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                        calendar.add(Calendar.DAY_OF_YEAR, -7)
                        startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                        fetchAndPopulateLineChart(startDate, endDate)
                    }
                    2 -> { // Last 30 Days
                        val calendar = Calendar.getInstance()
                        endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                        calendar.add(Calendar.DAY_OF_YEAR, -30)
                        startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                        fetchAndPopulateLineChart(startDate, endDate)
                    }
                    3 -> openCustomDateRangePicker() // Custom Range
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun openCustomDateRangePicker() {
        // Implement custom date range picker logic here
        // Use DatePickerDialog or any other date picker method to select start and end dates
        // Then call fetchAndPopulateLineChart(startDate, endDate)
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val startDatePicker = DatePickerDialog(this, { _, startYear, startMonth, startDay ->
            startDate = String.format("%04d-%02d-%02d", startYear, startMonth + 1, startDay)
            val endDatePicker = DatePickerDialog(this, { _, endYear, endMonth, endDay ->
                endDate = String.format("%04d-%02d-%02d", endYear, endMonth + 1, endDay)
                fetchAndPopulateLineChart(startDate, endDate)
            }, year, month, day)
            endDatePicker.show()
        }, year, month, day)
        startDatePicker.show()
    }

    private fun fetchAndPopulateLineChart(startDate: String?, endDate: String?) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val database = FirebaseDatabase.getInstance().reference
            val userEntriesRef = database.child("user_entries").child(user.uid)
            val totalTimeTrackedRef = userEntriesRef.child("totalTimeTracked")

            totalTimeTrackedRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val entries = mutableListOf<Entry>()
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val dateLabels = mutableListOf<String>()

                    for (dateSnapshot in dataSnapshot.children) {
                        val date = dateSnapshot.key ?: continue
                        val totalTime = dateSnapshot.child("Time").getValue(Long::class.java) ?: continue

                        if (isDateInRange(date, startDate, endDate)) {
                            val totalHours = (totalTime / (1000 * 60 * 60)).toFloat()
                            val totalMinutes = ((totalTime % (1000 * 60 * 60)) / (1000 * 60)).toFloat()
                            val totalSeconds = ((totalTime % (1000 * 60)) / 1000).toFloat()
                            val totalTimeInHours = totalHours + totalMinutes / 60 + totalSeconds / 3600

                            dateLabels.add(date)
                            entries.add(Entry(dateLabels.size - 1.toFloat(), totalTimeInHours))
                        }
                    }

                    val dailyGoalRef = userEntriesRef.child("DailyGoal")
                    val minGoalEntries = mutableListOf<Entry>()
                    val maxGoalEntries = mutableListOf<Entry>()

                    dailyGoalRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dailyGoalSnapshot: DataSnapshot) {
                            for (goalSnapshot in dailyGoalSnapshot.children) {
                                val date = goalSnapshot.key ?: continue
                                val minGoal = goalSnapshot.child("minGoal").getValue(Int::class.java)?.toFloat() ?: continue
                                val maxGoal = goalSnapshot.child("maxGoal").getValue(Int::class.java)?.toFloat() ?: continue

                                val dateIndex = dateLabels.indexOf(date)
                                if (dateIndex != -1) {
                                    minGoalEntries.add(Entry(dateIndex.toFloat(), minGoal))
                                    maxGoalEntries.add(Entry(dateIndex.toFloat(), maxGoal))
                                }
                            }

                            updateLineChart(entries, minGoalEntries, maxGoalEntries, dateLabels)
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.e("Firebase", "Failed to retrieve daily goals", databaseError.toException())
                        }
                    })
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase", "Failed to retrieve totalTimeTracked", databaseError.toException())
                }
            })
        }
    }

    private fun isDateInRange(date: String, startDate: String?, endDate: String?): Boolean {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateObj = dateFormat.parse(date)
        val startDateObj = startDate?.let { dateFormat.parse(it) }
        val endDateObj = endDate?.let { dateFormat.parse(it) }

        return (startDateObj == null || !dateObj.before(startDateObj)) &&
                (endDateObj == null || !dateObj.after(endDateObj))
    }




}
