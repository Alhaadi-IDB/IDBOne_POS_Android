package print.therestsuites.com.printing

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.util.UUID

class BluetoothPrinter {
    fun print(address: String, chunks: List<ByteArray>) {
        val adapter = BluetoothAdapter.getDefaultAdapter()
            ?: throw IOException("Bluetooth adapter not available")
        val device = adapter.getRemoteDevice(address)
        val socket = createSocket(device)

        adapter.cancelDiscovery()
        socket.connect()

        try {
            val output = socket.outputStream
            for (chunk in chunks) {
                if (chunk.isNotEmpty()) {
                    output.write(chunk)
                    output.flush()
                }
            }
            output.flush()
        } finally {
            try {
                socket.close()
            } catch (_: IOException) {
                // Ignore close errors.
            }
        }
    }

    private fun createSocket(device: BluetoothDevice): BluetoothSocket {
        return try {
            device.createRfcommSocketToServiceRecord(SPP_UUID)
        } catch (_: IOException) {
            device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
        }
    }

    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
