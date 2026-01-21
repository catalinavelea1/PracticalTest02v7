package ro.pub.cs.systems.eim.practicaltest02v7

import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

object Utilities {

    private const val TAG = "PracticalTest02"

    fun getReader(socket: Socket): BufferedReader? {
        return try {
            BufferedReader(InputStreamReader(socket.getInputStream()))
        } catch (e: IOException) {
            Log.e(TAG, "Error creating BufferedReader: ${e.message}", e)
            null
        }
    }

    fun getWriter(socket: Socket): PrintWriter? {
        return try {
            PrintWriter(socket.getOutputStream(), true)
        } catch (e: IOException) {
            Log.e(TAG, "Error creating PrintWriter: ${e.message}", e)
            null
        }
    }
}
