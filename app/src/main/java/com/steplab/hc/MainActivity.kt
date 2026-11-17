package com.steplab.hc

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import kotlinx.coroutines.*
import java.time.Instant
import java.time.ZoneId
import kotlin.random.Random

class MainActivity : ComponentActivity() {
 private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
 private var task: Job? = null
 private var count = 0L
 private var elapsed = 0L
 private val total = 120L * 60_000L
 private lateinit var status: TextView
 private lateinit var client: HealthConnectClient
 private val permission = HealthPermission.getWritePermission(StepsRecord::class)
 private val ask = registerForActivityResult(
  PermissionController.createRequestPermissionResultContract()
 ) { granted -> if (permission in granted) begin() else status.text = "Permission denied" }

 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val layout = LinearLayout(this).apply {
   orientation = LinearLayout.VERTICAL
   setPadding(40, 60, 40, 40)
  }
  status = TextView(this).apply { textSize = 20f; text = "Simulated steps test — not actual activity" }
  layout.addView(status)
  fun button(label: String, action: () -> Unit) {
   layout.addView(Button(this).apply { text = label; setOnClickListener { action() } })
  }
  button("Start / Resume") { checkAndStart() }
  button("Pause") { task?.cancel(); task = null; status.text = "Paused — $count simulated steps" }
  button("Reset") { task?.cancel(); task = null; count = 0; elapsed = 0; status.text = "Reset. Previously written Health Connect records remain until deleted there." }
  setContentView(layout)
 }
 private fun checkAndStart() {
  if (task?.isActive == true) return
  if (HealthConnectClient.getSdkStatus(this) != HealthConnectClient.SDK_AVAILABLE) {
   status.text = "Health Connect is unavailable"; return
  }
  client = HealthConnectClient.getOrCreate(this)
  scope.launch {
   try {
    if (permission !in client.permissionController.getGrantedPermissions()) ask.launch(setOf(permission))
    else begin()
   } catch (e: Exception) { status.text = "Permission error: ${e.message}" }
  }
 }
 private fun begin() {
  if (task?.isActive == true) return
  task = scope.launch {
   try {
    while (elapsed < total) {
     // Vary interval by +/- 15 seconds, but never exceed remaining duration.
     val ms = (60_000L + Random.nextLong(-15_000L, 15_001L)).coerceAtMost(total - elapsed)
     val start = Instant.now()
     delay(ms)
     val end = Instant.now()
     val nextElapsed = elapsed + ms
     // Keep cumulative target close to 100 steps/minute; final count exactly 12,000.
     val targetCount = (nextElapsed * 12_000L / total)
     val delta = targetCount - count
     if (delta > 0) {
      val zone = ZoneId.systemDefault().rules.getOffset(start)
      client.insertRecords(listOf(StepsRecord(
       startTime = start, endTime = end, startZoneOffset = zone, endZoneOffset = zone,
       count = delta, metadata = Metadata()
      )))
      count = targetCount
     }
     elapsed = nextElapsed
     status.text = "$count / 12000 simulated steps; ${elapsed / 60000} / 120 minutes"
    }
    status.text = "Completed: $count simulated steps"
   } catch (e: CancellationException) { throw e }
     catch (e: Exception) { status.text = "Write failed: ${e.message}" }
  }
 }
 override fun onDestroy() { task?.cancel(); scope.cancel(); super.onDestroy() }
}
