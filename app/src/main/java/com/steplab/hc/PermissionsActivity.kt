package com.steplab.hc
import android.app.Activity
import android.os.Bundle
import android.widget.TextView
class PermissionsActivity : Activity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  setContentView(TextView(this).apply {
   text = "StepLab HC writes simulated test steps to Health Connect. These are not sensor measurements. You can delete the records in Health Connect."
   textSize = 18f
   setPadding(32,48,32,32)
  })
 }
}