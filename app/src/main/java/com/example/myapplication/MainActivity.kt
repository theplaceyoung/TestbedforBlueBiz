package com.example.myapplication

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.icu.util.Calendar
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.broadcast.NotificationReceiver
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.services.NotificationService
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.IOException

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null

    private val client = OkHttpClient()

    // 센서 버튼 1개 만들자.
    private var isSensing = false

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // onCreate 메서드 내에서  binding 객체 생성
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        // "누름" 버튼에 클릭 리스너 등록
        binding.startSensingButton.setOnClickListener {
            if (isSensing) {
                stopSensor()
                binding.startSensingButton.text = "누름"
            } else {
                startSensor()
                binding.startSensingButton.text = "중지"
            }
            isSensing = !isSensing
        }

        binding.stopButton.setOnClickListener {
            stopSensor()
        }

        // NotificationService 실행
        startNotificationService()

        // 알림 예약
        scheduleNotification()
    }

    private fun startNotificationService() {
        val serviceIntent = Intent(this, NotificationService::class.java)
        startService(serviceIntent)
    }

    private fun scheduleNotification() {
        val intent = Intent(this, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intervalMillis = AlarmManager.INTERVAL_HOUR * 2

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.HOUR_OF_DAY, 2)
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            intervalMillis,
            pendingIntent
        )
    }

    override fun onResume() {
        super.onResume()
        // 센서 활성화
        startSensor()
    }

    override fun onPause() {
        super.onPause()
        // 센서 비활성화
        stopSensor()
    }

    private fun startSensor() {
        lightSensor?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun stopSensor() {
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { sensorEvent ->
            if (sensorEvent.sensor.type == Sensor.TYPE_LIGHT) {
                val lightLevel = sensorEvent.values[0]
                if (lightLevel > LIGHT_THRESHOLD) {
                    sendDataToServer(lightLevel)
                } else {
                    println("Light level is below threshold. No action taken.")
                }
            }
        }
    }

    private fun sendDataToServer(lightLevel: Float) {
        val url = "http://localhost:8000" // YOUR_SERVER_URL 대신 로컬 웹 서버 주소씀 (cmd에서 python -m http.server)
        val json = """
            {
                "lightLevel": $lightLevel
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), json))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                // 서버 응답 처리
            }

            override fun onFailure(call: Call, e: IOException) {
                // 에러 처리
            }
        })
    }

    companion object {
        private const val LIGHT_THRESHOLD = 50.0f
    }
}


/** YOUR_SERVER_URL : 하지만 주의해야 할 점이 있습니다. 안드로이드 에뮬레이터나 디바이스는 로컬 호스트(localhost)를 참조할 수 없습니다. 따라서 로컬 웹 서버를 테스트하기 위해서는 다음과 같은 절차를 따라야 합니다:

안드로이드 에뮬레이터를 사용하는 경우:
에뮬레이터는 로컬 호스트 대신에 특별한 IP 주소인 10.0.2.2를 사용하여 호스트 머신을 참조합니다. 따라서 http://localhost:8000 대신 http://10.0.2.2:8000을 사용해야 합니다.

실제 안드로이드 디바이스를 사용하는 경우:
안드로이드 디바이스와 호스트 컴퓨터가 같은 네트워크에 연결되어 있어야 합니다. 호스트 컴퓨터의 IP 주소로 접근할 수 있으며, http://호스트IP주소:8000과 같이 사용합니다.

따라서 실제 테스트를 수행할 환경에 따라 URL을 조정하여 테스트하시면 됩니다. **/