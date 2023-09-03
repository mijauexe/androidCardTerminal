package com.card.terminal.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.card.terminal.db.AppDatabase
import com.card.terminal.db.entity.OperationSchedule
import com.card.terminal.receivers.RelayReceiver
import timber.log.Timber
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date

class AlarmUtils {
    fun setRelayTimes(operationMode: MutableList<OperationSchedule>) {
        for (op in operationMode) {
            if (op.modeId == 2) {
                deleteExistingAlarms(op.uid)
                //ako je nekontrolirani prolaz, postavit alrm da se relej stavi u HOLD kad to vrijeme pocne
                //i isto tako ga stavit nazad u PULSE nacin kad to vrijeme zavrsi
                alarmParser(op)
            }
        }
    }

    private fun deleteExistingAlarms(uid: Int) {
        val alarmManager = ContextProvider.getApplicationContext()
            .getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val relayHoldIntent =
            Intent(ContextProvider.getApplicationContext(), RelayReceiver::class.java)
        relayHoldIntent.action = "com.relay.hold"

        val pendingIntentHold = PendingIntent.getBroadcast(
            ContextProvider.getApplicationContext(),
            relayHoldIntent.hashCode() + uid,
            relayHoldIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val relayPulseIntent =
            Intent(ContextProvider.getApplicationContext(), RelayReceiver::class.java)
        relayPulseIntent.action = "com.relay.pulse"

        val pendingIntentPulse = PendingIntent.getBroadcast(
            ContextProvider.getApplicationContext(),
            relayPulseIntent.hashCode() + uid,
            relayPulseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        //kanceliraj sve prosle alarme
        alarmManager.cancel(pendingIntentHold)
        alarmManager.cancel(pendingIntentPulse)
    }

    private fun alarmParser(op: OperationSchedule) {
        val localDateTime = LocalDateTime.now().toString()

        val timeStart = SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse(
            localDateTime.substring(
                0, localDateTime.indexOf('T')
            ) + 'T' + op.timeFrom
        )!!.time

        val timeEnd = SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse(
            localDateTime.substring(
                0, localDateTime.indexOf('T')
            ) + 'T' + op.timeTo
        )!!.time

        Timber.d("timeStart: ${Date(timeStart)}, timeEnd: ${Date(timeEnd)}")

        if (op.description.equals("WORKING_DAY") || op.description.equals("HOLIDAY")) {
            for (i in 2..6) {
                val cal1 = Calendar.getInstance()
                val cal2 = Calendar.getInstance()

                cal1.time = Date(timeStart)
                cal2.time = Date(timeEnd)

                cal1.set(Calendar.DAY_OF_WEEK, i);

                if (cal1.before(Calendar.getInstance())) {
                    cal1.add(Calendar.WEEK_OF_YEAR, 1)
                }

                cal2.set(Calendar.DAY_OF_WEEK, i);

                if (cal2.before(Calendar.getInstance())) {
                    cal2.add(Calendar.WEEK_OF_YEAR, 1)
                }

                setRelayAlarm(op.uid, cal1.timeInMillis, cal2.timeInMillis)
            }
        } else if (op.description.equals("SATURDAY")) {
            val cal1 = Calendar.getInstance()
            cal1.time = Date(timeStart)
            cal1.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);

            if (cal1.before(Calendar.getInstance())) {
                cal1.add(Calendar.WEEK_OF_YEAR, 1)
            }

            val cal2 = Calendar.getInstance()
            cal2.time = Date(timeEnd)
            cal2.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);

            if (cal2.before(Calendar.getInstance())) {
                cal2.add(Calendar.WEEK_OF_YEAR, 1)
            }

            setRelayAlarm(op.uid, cal1.timeInMillis, cal2.timeInMillis)
        } else if (op.description.equals("SUNDAY")) {
            val cal1 = Calendar.getInstance()
            cal1.time = Date(timeStart)
            cal1.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);

            if (cal1.before(Calendar.getInstance())) {
                cal1.add(Calendar.WEEK_OF_YEAR, 1)
            }

            val cal2 = Calendar.getInstance()
            cal2.time = Date(timeEnd)
            cal2.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);

            if (cal2.before(Calendar.getInstance())) {
                cal2.add(Calendar.WEEK_OF_YEAR, 1)
            }

            setRelayAlarm(op.uid, cal1.timeInMillis, cal2.timeInMillis)
        } else if (op.description.contains("SPECIFIC_DAY")) {
            val date = SimpleDateFormat("yyyy-MM-dd").parse(
                op.description.substring(
                    op.description.indexOf(":") + 1
                )
            )
            val dateStartTime = SimpleDateFormat("yyyy-MM-dd").format(date) + 'T' + op.timeFrom
            val dateEndTime = SimpleDateFormat("yyyy-MM-dd").format(date) + 'T' + op.timeTo

            val cal1 = Calendar.getInstance()
            cal1.time = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(dateStartTime)

            val cal2 = Calendar.getInstance()
            cal2.time = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(dateEndTime)

            setRelayAlarm(op.uid, cal1.timeInMillis, cal2.timeInMillis)
        }
    }

    private fun setRelayAlarm(
        uid: Int, timeStart: Long, timeEnd: Long
    ) {
        val relayHoldIntent =
            Intent(ContextProvider.getApplicationContext(), RelayReceiver::class.java)
        relayHoldIntent.action = "com.relay.hold"

        val pendingIntentHold = PendingIntent.getBroadcast(
            ContextProvider.getApplicationContext(),
            relayHoldIntent.hashCode() + uid,
            relayHoldIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val relayPulseIntent =
            Intent(ContextProvider.getApplicationContext(), RelayReceiver::class.java)
        relayPulseIntent.action = "com.relay.pulse"

        val pendingIntentPulse = PendingIntent.getBroadcast(
            ContextProvider.getApplicationContext(),
            relayPulseIntent.hashCode() + uid,
            relayPulseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = ContextProvider.getApplicationContext()
            .getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, timeStart, pendingIntentHold
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeEnd, pendingIntentPulse)
    }

    fun checkIfHoliday(): Boolean {
        try {
            val db = AppDatabase.getInstance(
                ContextProvider.getApplicationContext(), Thread.currentThread().stackTrace
            )

            val currentDateDate = LocalDate.parse(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            )

            val d1 = db.CalendarDao().getByDate(
                currentDateDate.dayOfWeek.value, currentDateDate.monthValue, currentDateDate.year
            )

            val d2 = db.CalendarDao().getByDate(
                currentDateDate.dayOfWeek.value, currentDateDate.monthValue, 0
            )

            return (d1 != null && !d1.workDay) || (d2 != null && !d2.workDay)
        } catch (e: Exception) {
            Timber.d("Msg: Exception %s | %s | %s", e.cause, e.stackTraceToString(), e.message)
            return false
        }
    }

}


