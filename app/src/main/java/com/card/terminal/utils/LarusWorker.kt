package com.card.terminal.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.card.terminal.http.MyHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LarusWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    override fun doWork(): Result {
        while(!isStopped) {
            runnable = Runnable {
                CoroutineScope(Dispatchers.Default).launch {
                    // Your coroutine code here
//                Log.d("MyWorker", "Coroutine is running...")
//                while(!isStopped) {
                    println("Coroutine is running...")
                    MyHttpClient.readLatestEvent()
//                }
                }
//            handler.postDelayed(runnable, 1000)
            }
            handler.post(runnable)
            Thread.sleep(1000)
        }
        return Result.success()
    }
}
