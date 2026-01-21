package ro.pub.cs.systems.eim.practicaltest02v7

import android.util.Log
import android.widget.TextView
import java.io.BufferedReader
import java.io.IOException
import java.io.PrintWriter
import java.net.Socket

class ClientThread(     private val address: String,
                        private val port: Int,
                        private val timerAction: String,
                        private val timerTextView: TextView
) : Thread() {

    override fun run() {
        var socket: Socket? = null
        var bufferedReader: BufferedReader? = null
        var printWriter: PrintWriter? = null

        try {
            // Conectare la server
            socket = Socket(address, port)
            Log.i(TAG, "Client connected to $address:$port")

            bufferedReader = Utilities.getReader(socket)
            printWriter = Utilities.getWriter(socket)

            if (bufferedReader == null || printWriter == null) {
                Log.e(TAG, "BufferedReader / PrintWriter are null!")
                updateTextView("Error: Could not create communication streams")
                return
            }

            Log.i(TAG, "ClientThread sent: $timerAction")

            // trimite com server
            printWriter.println(timerAction)
            printWriter.flush()

            // raspuns de la server
            val response = bufferedReader.readLine()

            if (response != null) {
                Log.i(TAG, "ClientThread received: $response")
                updateTextView(response)
            }

        } catch (e: IOException) {
            Log.e(TAG, "An exception has occurred: ${e.message}", e)
            updateTextView("Error: ${e.message}")
        } finally {
            try {
                bufferedReader?.close()
                printWriter?.close()
                socket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing resources: ${e.message}", e)
            }
        }
    }

    private fun updateTextView(text: String) {
        timerTextView.post {
            timerTextView.text = text
        }
    }

    companion object {
        private const val TAG = "PracticalTest02"
    }
}