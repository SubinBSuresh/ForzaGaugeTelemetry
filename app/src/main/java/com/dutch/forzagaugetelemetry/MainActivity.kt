package com.dutch.forzagaugetelemetry

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
                .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 64.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RaceStatusIndicator(isRaceOn = telemetry.isRaceOn)

            GearAndSpeedSection(telemetry = telemetry)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.width(16.dp))
                MetricsGrid(telemetry = telemetry, modifier = Modifier.weight(0.6f))
            }
        }
    }

    @Composable
    fun LandscapeDashboard(telemetry: ForzaTelemetry) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp, start = 32.dp, end = 32.dp, bottom = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                RaceStatusIndicator(isRaceOn = telemetry.isRaceOn)
                Spacer(modifier = Modifier.height(16.dp))
                GearAndSpeedSection(telemetry = telemetry)
                Spacer(modifier = Modifier.height(16.dp))
            }

            Column(
                modifier = Modifier.weight(1.3f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Spacer(modifier = Modifier.height(24.dp))
                MetricsGrid(telemetry = telemetry, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    @Composable
    fun RaceStatusIndicator(isRaceOn: Boolean) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isRaceOn) Color(0xFF00E676) else Color(0xFFFF1744))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isRaceOn) "RACE LIVE" else "RACE PAUSED",
                color = if (isRaceOn) Color(0xFF00E676) else Color(0xFFFF1744),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }

    @Composable
    fun GearAndSpeedSection(telemetry: ForzaTelemetry) {
        val isRedline =
            telemetry.engineMaxRpm > 0 && telemetry.currentEngineRpm >= telemetry.engineMaxRpm * 0.92f
        val gearText = when (telemetry.gear) {
            0 -> "R"
            11 -> "N"
            else -> telemetry.gear.toString()
        }
        val speedText = telemetry.speedKmh.toString()

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.Bottom, modifier = Modifier.wrapContentHeight()
            ) {
                Text(
                    text = gearText,
                    color = Color.White.copy(alpha = 0.40f),
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.offset(y = (-10).dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = speedText,
                    color = if (isRedline) Color(0xFFFF1744) else Color.White,
                    fontSize = 130.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.SansSerif,
                    lineHeight = 110.sp,
                    letterSpacing = (-4).sp
                )
            }
            Text(
                text = "KM/H",
                color = Color.Gray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.offset(y = (-10).dp)
            )
            RpmProgressBar(telemetry = telemetry, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            SteerIndicator(steer = telemetry.steer, modifier = Modifier.fillMaxWidth(0.5f))
        }
    }

    @Composable
    fun RpmProgressBar(telemetry: ForzaTelemetry, modifier: Modifier = Modifier) {
        val rpmProgress =
            if (telemetry.engineMaxRpm > 0) (telemetry.currentEngineRpm / telemetry.engineMaxRpm).coerceIn(
                0f, 1f
            ) else 0f
        val redlineStart = 0.85f

        val skewedShape = GenericShape { size, _ ->
            val skewWidth = size.height * 0.4f
            moveTo(skewWidth, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width - skewWidth, size.height)
            lineTo(0f, size.height)
            close()
        }

        Column(modifier = modifier) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${telemetry.currentEngineRpm.toInt()}",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    fontStyle = FontStyle.Italic
                )
                Text(
                    text = "RPM",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(skewedShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                // Redline area
                Box(
                    modifier = Modifier
                        .fillMaxWidth(1f - redlineStart)
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                        .background(Color(0xFFE91E63).copy(alpha = 0.3f))
                )

                // Progress
                if (rpmProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(rpmProgress)
                            .fillMaxHeight()
                            .background(
                                if (rpmProgress > redlineStart) Color(0xFFE91E63)
                                else Color.White.copy(alpha = 0.7f)
                            )
                    )

                    // Needle/Indicator line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(rpmProgress)
                            .fillMaxHeight()
                    ) {
                        Box(
                            modifier = Modifier
                                .width(2.5.dp)
                                .fillMaxHeight()
                                .background(Color.White)
                                .align(Alignment.CenterEnd)
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun SteerIndicator(steer: Int, modifier: Modifier = Modifier) {
        Box(
            modifier = modifier
                .height(4.dp)
                .background(Color(0xFF333333), RoundedCornerShape(2.dp))
        ) {
            val steerNormalized = (steer.toFloat() / 127f).coerceIn(-1f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(12.dp)
                    .align(Alignment.Center)
                    .offset(x = (steerNormalized * 50).dp)
                    .background(Color.White, RoundedCornerShape(1.dp))
            )
        }
    }


    @Composable
    fun TireBox(temp: Float) {
        val color = when {
            temp > 95 -> Color(0xFFFF1744)
            temp > 85 -> Color(0xFFFF9100)
            temp > 70 -> Color(0xFF00E676)
            temp > 40 -> Color(0xFF2979FF)
            else -> Color(0xFF333333)
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 32.dp, height = 44.dp)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(4.dp))
                .border(1.dp, color, RoundedCornerShape(4.dp))
        ) {
            Text(
                temp.toInt().toString(),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    @Composable
    fun MetricsGrid(telemetry: ForzaTelemetry, modifier: Modifier = Modifier) {
        Column(
            modifier = modifier.wrapContentHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBox(
                    modifier = Modifier.weight(1f),
                    label = "THR",
                    value = "${((telemetry.accel / 255f) * 100).toInt()}%",
                    color = Color(0xFF00E676)
                )
                StatBox(
                    modifier = Modifier.weight(1f),
                    label = "BRK",
                    value = "${((telemetry.brake / 255f) * 100).toInt()}%",
                    color = Color(0xFFFF1744)
                )
                StatBox(
                    modifier = Modifier.weight(1f),
                    label = "BST",
                    value = String.format("%.1f", telemetry.boostPsi)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBox(
                    modifier = Modifier.weight(1f), label = "PWR", value = "${telemetry.horsepower}"
                )
                StatBox(
                    modifier = Modifier.weight(1f),
                    label = "TRQ",
                    value = "${telemetry.torqueNm.toInt()}"
                )
                StatBox(
                    modifier = Modifier.weight(1f),
                    label = "AFR",
                    value = String.format("%.1f", telemetry.fuelRatio)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBox(
                    modifier = Modifier.weight(1f), label = "LAP", value = "${telemetry.lapNumber}"
                )
                StatBox(
                    modifier = Modifier.weight(1f),
                    label = "POS",
                    value = "${telemetry.racePosition}"
                )
                StatBox(
                    modifier = Modifier.weight(1f),
                    label = "G-LAT",
                    value = String.format("%.2f", telemetry.accelX / 9.81f)
                )
            }
        }
    }

    @Composable
    fun StatBox(
        label: String, value: String, modifier: Modifier = Modifier, color: Color = Color.White
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
                .padding(vertical = 6.dp)
        ) {
            Text(
                label,
                color = Color.Gray,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                value,
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
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
                "DATA IN: ",
                color = Color.DarkGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                "$ip : $port",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
