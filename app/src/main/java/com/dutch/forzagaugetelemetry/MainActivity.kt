package com.dutch.forzagaugetelemetry

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dutch.forzagaugetelemetry.ui.theme.ForzaGaugeTelemetryTheme
import java.net.NetworkInterface
import java.util.Collections

class MainActivity : ComponentActivity() {

    private val TAG = "FORZA__MainActivity"
    private val targetPort = 20066
    private val telemetryEngine = TelemetryEngine(targetPort)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val localIP = getLocalIpAddress()
        enableEdgeToEdge()
        setContent {
            ForzaGaugeTelemetryTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0C0C0C)) {
                    LaunchedEffect(Unit) {
                        telemetryEngine.startListening()
                    }
                    val currentTelemetry by telemetryEngine.telemetryState.collectAsState()
                    val configuration = LocalConfiguration.current

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            LandscapeDashboard(telemetry = currentTelemetry)
                        } else {
                            PortraitDashboard(telemetry = currentTelemetry)
                        }

                        ConnectionInfoOverlay(
                            ip = localIP, port = targetPort, modifier = Modifier.align(
                                Alignment.BottomCenter
                            )
                        )
                    }

                }
            }
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (networkInterfaces in interfaces) {
                val addresses = Collections.list(networkInterfaces.inetAddresses)
                for (address in addresses) {
                    if (!address.isLoopbackAddress && address is java.net.InetAddress) {
                        return address.hostAddress ?: "Unknown IP"
                    }
                }
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Error: ${exception.message}")
        }
        return "Unknown IP"
    }


    @Composable
    fun PortraitDashboard(telemetry: ForzaTelemetry) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 32.dp, start = 24.dp, end = 24.dp, bottom = 64.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GearAndSpeedSection(telemetry = telemetry)
            RpmProgressBar(telemetry = telemetry, modifier = Modifier.fillMaxWidth())
            MetricsGrid(telemetry = telemetry)
        }
    }

    @Composable
    fun LandscapeDashboard(telemetry: ForzaTelemetry) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp, start = 32.dp, end = 32.dp, bottom = 64.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterHorizontally as Alignment.Vertical
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {


                GearAndSpeedSection(telemetry = telemetry)
            }

            Column(
                modifier = Modifier.weight(1.2f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                RpmProgressBar(telemetry = telemetry, modifier = Modifier.fillMaxWidth(0.9f))
                Spacer(modifier = Modifier.height(24.dp))
                MetricsGrid(telemetry = telemetry)
            }
        }
    }


    @Composable
    fun GearAndSpeedSection(telemetry: ForzaTelemetry) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Core Gear Indicator
            Text(
                text = when (telemetry.gear) {
                    0 -> "R"
                    11 -> "N"
                    else -> telemetry.gear.toString()
                },
                color = if (telemetry.currentEngineRpm >= telemetry.engineMaxRpm * 0.92f) Color(
                    0xFFFF1744
                ) else Color(0xFFFFCC00),
                fontSize = 110.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                lineHeight = 110.sp
            )

            // Speed Display
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = telemetry.speedKmh.toString(),
                    color = Color.White,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 54.sp
                )
                Text(
                    text = " KM/H",
                    color = Color.DarkGray,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }

    @Composable
    fun RpmProgressBar(telemetry: ForzaTelemetry, modifier: Modifier = Modifier) {
        val rpmProgress =
            if (telemetry.engineMaxRpm > 0) telemetry.currentEngineRpm / telemetry.engineMaxRpm else 0f

        // Dynamic color shifting based on engine workload thresholds
        val barColor = when {
            rpmProgress > 0.92f -> Color(0xFFFF1744) // Redline limit warning
            rpmProgress > 0.75f -> Color(0xFFFFEA00) // Optimal shifting window
            else -> Color(0xFF00E676)                // Safe range linear acceleration
        }

        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${telemetry.currentEngineRpm.toInt()} RPM",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "LIMIT: ${telemetry.engineMaxRpm.toInt()}",
                    color = Color.DarkGray,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { rpmProgress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = barColor,
                trackColor = Color(0xFF1F1F1F)
            )
        }
    }

    @Composable
    fun MetricsGrid(telemetry: ForzaTelemetry) {
        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatBox(label = "THROTTLE", value = "${((telemetry.accel / 255f) * 100).toInt()}%")
            StatBox(label = "BRAKE", value = "${((telemetry.brake / 255f) * 100).toInt()}%")
            StatBox(label = "BOOST", value = String.format("%.1f PSI", telemetry.boostPsi))
        }
    }

    @Composable
    fun StatBox(label: String, value: String) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = Color.DarkGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }
    }


    @Composable
    fun ConnectionInfoOverlay(ip: String, port: Int, modifier: Modifier = Modifier) {
        Row(
            modifier = modifier
                .padding(bottom = 12.dp)
                .background(Color(0xFF161616), RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FORZA DATA IN: ",
                color = Color.DarkGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "$ip : $port",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
        }
    }


}