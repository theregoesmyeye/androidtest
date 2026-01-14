package com.example.androidtest

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

data class DeviceItem(val name: String, val address: String)

class DeviceAdapter(private val devices: MutableList<DeviceItem>) :
    RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tv_device_name)
        val address: TextView = itemView.findViewById(R.id.tv_device_address)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): DeviceViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.name.text = device.name
        holder.address.text = device.address
    }

    override fun getItemCount() = devices.size
}

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var btnStartScan: Button
    private lateinit var tvStatus: TextView
    private lateinit var rvDevices: RecyclerView
    private lateinit var adapter: DeviceAdapter
    private val devices = mutableListOf<DeviceItem>()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: "Unknown"
            val address = device.address
            val deviceItem = DeviceItem(name, address)
            if (!devices.contains(deviceItem)) {
                devices.add(deviceItem)
                adapter.notifyItemInserted(devices.size - 1)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            runOnUiThread {
                tvStatus.text = "Scan failed: $errorCode"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        btnStartScan = findViewById(R.id.btn_start_scan)
        tvStatus = findViewById(R.id.tv_status)
        rvDevices = findViewById(R.id.rv_devices)

        adapter = DeviceAdapter(devices)
        rvDevices.layoutManager = LinearLayoutManager(this)
        rvDevices.adapter = adapter

        btnStartScan.setOnClickListener {
            if (checkPermissions()) {
                startScan()
            } else {
                requestPermissions()
            }
        }
    }

    private fun checkPermissions(): Boolean {
        val location = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val bluetoothScan = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            PackageManager.PERMISSION_GRANTED
        }
        val bluetoothConnect = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            PackageManager.PERMISSION_GRANTED
        }
        return location == PackageManager.PERMISSION_GRANTED &&
               bluetoothScan == PackageManager.PERMISSION_GRANTED &&
               bluetoothConnect == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1001)
        }
    }

    private fun startScan() {
        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }
        devices.clear()
        adapter.notifyDataSetChanged()
        tvStatus.text = getString(R.string.scanning)
        bluetoothAdapter.bluetoothLeScanner.startScan(scanCallback)
        // Stop scan after 10 seconds
        android.os.Handler(mainLooper).postDelayed({
            bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
            tvStatus.text = "Scan stopped"
        }, 10000)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startScan()
        } else {
            Toast.makeText(this, "Permissions required", Toast.LENGTH_SHORT).show()
        }
    }
}