package ro.pub.cs.systems.eim.practicaltest02v7

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

data class TimerInformation(
    val minute: String,
    val second: String,
    var canGetUTC: Boolean = true,
    val set_minute: String,
    val set_second:String
)

class PracticaltTest02v7MainActivity : AppCompatActivity() {


    // Server
    private var serverThread: ServerThread? = null

    // Client
    private var clientThread: ClientThread? = null
    private lateinit var serverPortEditText: EditText
    private lateinit var connectButton: Button

    private lateinit var clientAddressEditText: EditText
    private lateinit var clientPortEditText: EditText
    private lateinit var timerActionEditText: EditText
    private lateinit var timerActionButton: Button
    private lateinit var timerActionTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_practical_test02v7_main)

        // Inițializare componente UI - Server
        serverPortEditText = findViewById(R.id.server_port_edit_text)
        connectButton = findViewById(R.id.connect_button)

        // Inițializare componente UI - Client
        clientAddressEditText = findViewById(R.id.client_address_edit_text)
        clientPortEditText = findViewById(R.id.client_port_edit_text)
        timerActionEditText = findViewById(R.id.timer_action_edit_text)
        timerActionButton = findViewById(R.id.get_timer_action_button)
        timerActionTextView = findViewById(R.id.timer_action_text_view)

        // Listener pentru butonul Connect (Server)
        connectButton.setOnClickListener {
            handleServerConnect()
        }

        // Listener pentru butonul Timer Action (Client)
        timerActionButton.setOnClickListener {
            handleClientAction()
        }
    }

    private fun handleServerConnect() {
        val serverPort = serverPortEditText.text.toString()

        if (serverPort.isEmpty()) {
            Toast.makeText(
                applicationContext,
                "Server port should be filled!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val port = serverPort.toIntOrNull()
        if (port == null || port !in 1024..65535) {
            Toast.makeText(
                applicationContext,
                "Invalid port number (use 1024-65535)!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        Log.i(TAG, "Server connection parameters: $port")

        // Oprire server existent dacă există
        serverThread?.stopThread()

        // Creare și pornire server nou
        serverThread = ServerThread(port)

        if (serverThread?.serverSocket == null) {
            Log.e(TAG, "Could not create server thread!")
            Toast.makeText(
                applicationContext,
                "Could not start server! Port may be in use.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        serverThread?.start()

        Toast.makeText(
            applicationContext,
            "Server started on port $port",
            Toast.LENGTH_SHORT
        ).show()

        connectButton.text = "Server Running"
        connectButton.isEnabled = false
    }

    private fun handleClientAction() {
        val clientAddress = clientAddressEditText.text.toString()
        val clientPort = clientPortEditText.text.toString()
        val timerAction = timerActionEditText.text.toString()

        if (clientAddress.isEmpty() || clientPort.isEmpty() || timerAction.isEmpty()) {
            Toast.makeText(
                applicationContext,
                "All client fields should be filled!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val port = clientPort.toIntOrNull()
        if (port == null || port !in 1024..65535) {
            Toast.makeText(
                applicationContext,
                "Invalid port number (use 1024-65535)!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        Log.i(TAG, "Client connection parameters: $clientAddress $port $timerAction")

        // Golire text anterior
        timerActionTextView.text = "Connecting..."

        // Creare și pornire client thread
        clientThread = ClientThread(clientAddress, port, timerAction, timerActionTextView)
        clientThread?.start()
    }

    override fun onDestroy() {
        // Oprire server la distrugerea activității
        serverThread?.stopThread()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "PracticalTest02"
    }
}