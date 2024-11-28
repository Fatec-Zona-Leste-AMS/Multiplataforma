package com.app.clima

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui. Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.app.clima.ui.theme.ClimaTheme
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClimaTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        TitleHeader()
                        DatePickerContent(db)
                    }
                }
            }
        }
    }
}

@Composable
fun TitleHeader() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = Color(0xFF1565C0),
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Dados Multiplataforma",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

    data class WeatherInfo(
        val date: String,
        val time: String = "12:00",
        val temperature: String = "25°C",
        val humidity: String = "65%"
    )

@Composable
fun DatePickerContent(
    db: com.google.firebase.firestore.FirebaseFirestore,
    modifier: Modifier = Modifier
) {
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    var weatherInfo by remember { mutableStateOf<WeatherInfo?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        AndroidView(
            modifier = Modifier.wrapContentSize(),
            factory = { context ->
                android.widget.CalendarView(context).apply {
                    setOnDateChangeListener { _, year, month, dayOfMonth ->
                        isLoading = true;
                        val calendar = Calendar.getInstance()
                        calendar.set(year, month, dayOfMonth)
                        selectedDate = calendar.time

                        val formattedDate = dateFormatter.format(calendar.time)
                        fetchWeatherData(db, formattedDate) { fetchedInfo ->
                            weatherInfo = fetchedInfo
                            isLoading = false
                        }
                    }

                    val currentDate = dateFormatter.format(Date())
                    fetchWeatherData(db, currentDate) { fetchedInfo ->
                        isLoading = true;
                        weatherInfo = fetchedInfo
                        isLoading = false
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = Color(0xFF1565C0)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Carregando dados...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                weatherInfo?.let { info ->
                    WeatherInfoTable(info)
                }
            }
        }
    }
}

fun fetchWeatherData(
    db: com.google.firebase.firestore.FirebaseFirestore,
    date: String,
    onResult: (WeatherInfo?) -> Unit
) {
    db.collection("SensorData")
        .whereEqualTo("date", date)
        .get()
        .addOnSuccessListener { querySnapshot ->
            if (!querySnapshot.isEmpty) {
                val documents = querySnapshot.documents
                val latestWeatherInfo = selectLatestWeatherData(documents)
                onResult(latestWeatherInfo)
            } else {
                onResult(null)
            }
        }
        .addOnFailureListener { exception ->
            Log.e("Firestore", "Error fetching data", exception)
            onResult(null)
        }
}

fun selectLatestWeatherData(documents: List<com.google.firebase.firestore.DocumentSnapshot>): WeatherInfo? {
    return documents.maxByOrNull { document ->
        document.getString("time") ?: "00:00"
    }?.let { document ->
        WeatherInfo(
            date = document.getString("date") ?: "",
            time = document.getString("time") ?: "",
            temperature = "${document.getString("temp")}°C",
            humidity = "${document.getString("hmd")}%"
        )
    }
}

@Composable
fun WeatherInfoTable(weatherInfo: WeatherInfo) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            TableRow("DATA", weatherInfo.date)
            TableRow("HORA", weatherInfo.time)
            TableRow("TEMPERATURA", weatherInfo.temperature)
            TableRow("UMIDADE", weatherInfo.humidity)
        }
    }
}

@Composable
fun TableRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DatePickerPreview() {
    ClimaTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column {
                TitleHeader()
                DatePickerContent(db = Firebase.firestore)
            }
        }
    }
}