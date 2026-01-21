package ro.pub.cs.systems.eim.practicaltest02v7

import android.util.Log
import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap

class ServerThread(private val port: Int) : Thread() {

    var serverSocket: ServerSocket? = null
        private set

    private val data = ConcurrentHashMap<String, TimerInformation>()

    init {
        try {
            serverSocket = ServerSocket(port)
            Log.i(TAG, "Server socket created on port $port")
        } catch (e: IOException) {
            Log.e(TAG, "Server Thread - An exception has occurred: ${e.message}", e)
        }
    }

    override fun run() {
        try {
            while (!isInterrupted) {
                Log.i(TAG, "Server Thread - Waiting for a client invocation...")

                val socket = serverSocket?.accept()

                if (socket != null) {
                    Log.i(TAG, "Server Thread - Connection received from ${socket.inetAddress}:${socket.port}")

                    CommunicationThread(this, socket).start()
                }
            }
        } catch (e: IOException) {
            if (!isInterrupted) {
                Log.e(TAG, "Server Thread - An exception has occurred: ${e.message}", e)
            }
        }
    }

    fun stopThread() {
        interrupt()
        serverSocket?.close()
        Log.i(TAG, "Server thread stopped")
    }

    @Synchronized
    fun setData(key: String, timerInfo: TimerInformation) {
        data[key] = timerInfo
        Log.i(TAG, "Timer set for $key: ${timerInfo.minute}:${timerInfo.second}")
    }

    @Synchronized
    fun getData(): ConcurrentHashMap<String, TimerInformation> = data

    companion object {
        private const val TAG = "PracticalTest02"
    }
}