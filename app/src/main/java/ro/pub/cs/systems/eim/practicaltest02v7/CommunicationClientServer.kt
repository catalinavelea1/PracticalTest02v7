package ro.pub.cs.systems.eim.practicaltest02v7

import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.PrintWriter
import java.net.Socket
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Locale
import kotlin.time.Duration.Companion.minutes

class CommunicationThread(
    private val serverThread: ServerThread,
    private val socket: Socket
) : Thread() {

    override fun run() {
        var bufferedReader: BufferedReader? = null
        var printWriter: PrintWriter? = null

        try {
            bufferedReader = Utilities.getReader(socket)
            printWriter = Utilities.getWriter(socket)

            if (bufferedReader == null || printWriter == null) {
                Log.e(TAG, "Communication Thread - BufferedReader / PrintWriter are null!")
                return
            }

            // comanda de la client
            val timerAction = bufferedReader.readLine()
            if (timerAction.isNullOrEmpty()) {
                Log.e(TAG, "Communication Thread - Error receiving parameters from client!")
                return
            }

            Log.i(TAG, "Communication Thread - Received: $timerAction")

            // Parsare comandă
            val cleanedAction = timerAction.trim()
            val actionData = cleanedAction.split(",")

            if (actionData.isEmpty()) {
                Log.e(TAG, "Communication Thread - Invalid command format!")
                return
            }

            val clientIP = socket.inetAddress.toString()
            val data = serverThread.getData()

            when (actionData[0]) {
                "set" -> handleSet(actionData, clientIP, printWriter)
                "poll" -> handlePoll(clientIP, data, printWriter)
                "reset" -> handleReset(clientIP, data, printWriter)
                else -> Log.e(TAG, "Communication Thread - Invalid action: ${actionData[0]}")
            }

        } catch (e: IOException) {
            Log.e(TAG, "Communication Thread - An exception has occurred: ${e.message}", e)
        } finally {
            try {
                socket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Communication Thread - Error closing socket: ${e.message}", e)
            }
        }
    }

    private fun handleSet(actionData: List<String>, clientIP: String, printWriter: PrintWriter) {
        if (actionData.size < 3) {
            Log.e(TAG, "Communication Thread - Invalid set command: missing parameters")
            printWriter.println("error: invalid parameters\n")
            printWriter.flush()
            return
        }

        val minute = actionData[1]
        val second = actionData[2]

        val currentTime = LocalDateTime.now().toString()
        val currentParts = currentTime.split(":")
        val registerMinute = currentParts[1]
        val registerSecond = currentParts[2]
        Log.i("CurrentTime", "${registerMinute}, ${registerSecond}")

        // Validare format
        try {
            val minuteInt = minute.toInt()
            val secondInt = second.toInt()

            if (minuteInt !in 0..59 || secondInt !in 0..59) {
                Log.e(TAG, "Communication Thread - Invalid time range")
                printWriter.println("error: invalid time range\n")
                printWriter.flush()
                return
            }

            serverThread.setData(clientIP, TimerInformation(minute, second, canGetUTC = true, registerMinute, registerSecond))
            Log.i(TAG, "Communication Thread - Set: $minute:$second for $clientIP")

            printWriter.println("ok\n")
            printWriter.flush()

        } catch (e: NumberFormatException) {
            Log.e(TAG, "Communication Thread - Invalid time format")
            printWriter.println("error: invalid time format\n")
            printWriter.flush()
        }
    }

    private fun handlePoll(
        clientIP: String,
        data: Map<String, TimerInformation>,
        printWriter: PrintWriter
    ) {
        if (!data.containsKey(clientIP)) {
            Log.i(TAG, "Communication Thread - No timer for $clientIP")
            printWriter.println("none\n")
            printWriter.flush()
            return
        }

        val timerInfo = data[clientIP]!!

        if (timerInfo.canGetUTC) {
            val utcTime = getTime()

            if (utcTime == null) {
                Log.e(TAG, "Communication Thread - Could not get UTC time")
                printWriter.println("error: could not get UTC time\n")
                printWriter.flush()
                return
            }

            // Parsare timp UTC
            val utcParts = utcTime.split(" ")
            if (utcParts.size < 3) {
                Log.e(TAG, "Communication Thread - Invalid UTC format")
                printWriter.println("error: invalid UTC format\n")
                printWriter.flush()
                return
            }

            val timeParts = utcParts[2].split(":")
            if (timeParts.size < 2) {
                Log.e(TAG, "Communication Thread - Invalid time format from UTC")
                printWriter.println("error: invalid UTC time format\n")
                printWriter.flush()
                return
            }

            val utcMinute = timeParts[1].toIntOrNull() ?: 0
            val utcSecond = timeParts[2].toIntOrNull() ?: 0

            Log.i("CumminicationClientServer", "${utcMinute}, ${utcSecond}")

            val timerMinute = timerInfo.minute.toIntOrNull() ?: 0
            val timerSecond = timerInfo.second.toIntOrNull() ?: 0

            val setMinute = timerInfo.set_minute.toIntOrNull() ?: 0
            val setSecond = timerInfo.set_second.toIntOrNull() ?: 0

            Log.i(TAG, "Communication Thread - Poll: Timer=$timerMinute:$timerSecond, UTC=$utcMinute:$utcSecond, SET=$setMinute:$setSecond")



            // Comparare timpi
            val isExpired = (utcMinute - setMinute) > timerMinute ||
                    ((utcMinute == setMinute) && ((utcSecond - setSecond) >= timerSecond))

            Log.i("verificare", "${utcSecond - setSecond}, ${timerSecond}, ${utcSecond
            }, ${setSecond}")

            if (isExpired) {
                timerInfo.canGetUTC = false
                printWriter.println("active\n")
                printWriter.flush()
                Log.i(TAG, "Communication Thread - Timer expired for $clientIP")
            } else {
                printWriter.println("inactive\n")
                printWriter.flush()
                Log.i(TAG, "Communication Thread - Timer still active for $clientIP")
            }
        } else {
            // Timer deja expirat anterior
            printWriter.println("active\n")
            printWriter.flush()
            Log.i(TAG, "Communication Thread - Timer was already expired for $clientIP")
        }
    }

    private fun handleReset(clientIP: String, data: MutableMap<String, TimerInformation>,  printWriter: PrintWriter) {
        data.remove(clientIP)
        printWriter.println("reset done\n")
        printWriter.flush()
        Log.i(TAG, "Communication Thread - Reset timer for $clientIP")
    }

    private fun getTime(): String? {
        var socket: Socket? = null
        try {
            socket = Socket("time-a-g.nist.gov", 13)
            val bufferedReader = Utilities.getReader(socket)

            if (bufferedReader == null) {
                Log.e(TAG, "Could not create reader for UTC server")
                return null
            }

            bufferedReader.readLine()
            val utcTime = bufferedReader.readLine()

            Log.i(TAG, "UTC time received: $utcTime")
            return utcTime

        } catch (e: IOException) {
            Log.e(TAG, "Error getting UTC time: ${e.message}", e)
            return null
        } finally {
            try {
                socket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing UTC socket: ${e.message}", e)
            }
        }
    }

    companion object {
        private const val TAG = "PracticalTest02"
    }
}