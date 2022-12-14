package ua.zp.uvs.usbaccessory

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.Parcelable
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import ua.zp.uvs.usbaccessory.databinding.ActivityMainBinding
import java.io.FileInputStream
import java.io.FileOutputStream

lateinit var binding : ActivityMainBinding
lateinit var mUsbManager: UsbManager

private const val ACTION_USB_ACCESSORY_PERMISSION =
    "ua.zp.uvs.usbaccessory.ACTION_USB_ACCESSORY_PERMISSION"

private const val MANUFACTURER = "Nokia"
private const val MODEL = "Nokia 3.4"

private var mConnected = false
private var mAccessory: UsbAccessory? = null
private var mTransport: UsbAccessoryStreamTransport? = null

private var fileDescriptor: ParcelFileDescriptor? = null
private var inputStream: FileInputStream? = null
private var outputStream: FileOutputStream? = null

class MainActivity : AppCompatActivity() {
    var ioTaskUsb: IOTaskUsb? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding =
            DataBindingUtil.setContentView(this, R.layout.activity_main)

        mUsbManager = getSystemService(Context.USB_SERVICE) as UsbManager

//        val permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_ACCESSORY_PERMISSION), 0)

        val filter = IntentFilter()
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
        filter.addAction(ACTION_USB_ACCESSORY_PERMISSION)
        registerReceiver(mReceiver, filter)

        val intent = intent
        if (intent.action == UsbManager.ACTION_USB_ACCESSORY_ATTACHED) {
            val accessory =
                intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_ACCESSORY) as UsbAccessory?
            accessory?.let { onAccessoryAttached(it) }
        } else {
            val accessories = mUsbManager.accessoryList
            if (accessories != null) {
                for (accessory in accessories) {
                    onAccessoryAttached(accessory)
                }
            }
        }

        binding.buttonCheckAccessory.setOnClickListener(View.OnClickListener {
            checkAccessoryFun()
        }
        )

        binding.buttonListen.setOnClickListener(View.OnClickListener {
            listenHost()
        }
        )

    }

    private fun listenHost() {
        val intent = intent
        if (intent.action == UsbManager.ACTION_USB_ACCESSORY_ATTACHED) {
            val accessory =
                intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_ACCESSORY) as UsbAccessory?
            accessory?.let { onAccessoryAttached(it) }
        } else {
            val accessories = mUsbManager.accessoryList
            if (accessories != null) {
                for (accessory in accessories) {
                    onAccessoryDetached(accessory)
                    onAccessoryAttached(accessory)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mReceiver)
        ioTaskUsb?.onCancelled()
    }

    private fun checkAccessoryFun() {
//        mAccessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY) as UsbAccessory?
        var accessoryList: Array<out UsbAccessory>? = null
        accessoryList = mUsbManager.accessoryList
        var i = ""
        if (accessoryList != null && accessoryList.isNotEmpty()) {
            accessoryList.forEach {
                i += "\n" +
                        "AccessoryDescription: " + it.description + "\n" +
                        "AccessoryUri: " + it.uri + "\n" +
                        "AccessoryModel: " + it.model + "\n" +
                        "AccessoryManufacturer: " + it.manufacturer + "\n" +
                        "AccessoryVersion: " + it.version + "\n" +
                        "AccessorySerial: " + it.serial + "\n";
            }
        } else {
            i += "\n" +
                    "Devices not find " + "\n" +
                    "DeviceID: " + "\n" +
                    "DeviceName: " + "\n" +
                    "DeviceClass: " + "\n" +
                    "DeviceSubClass: " + "\n" +
                    "VendorID: " + "\n" +
                    "ProductID: " + "\n";
        }
        binding.textInfo.text = i
    }

    private fun onAccessoryAttached(accessory: UsbAccessory) {
        if (!mConnected) {
            connect(accessory)
        }
    }

    private fun onAccessoryDetached(accessory: UsbAccessory) {
        binding.textInfo.text = ("USB accessory detached: $accessory")
        if (mConnected && accessory == mAccessory) {
            disconnect()
        }
    }

    private fun connect(accessory: UsbAccessory) {
        if (!isSink(accessory)) {
            binding.textInfo.text = (
                    "Not connecting to USB accessory because it is not an accessory display sink: "
                            + accessory
                    )
            return
        }
        if (mConnected) {
            disconnect()
        }

        // Check whether we have permission to access the accessory.
        if (!mUsbManager.hasPermission(accessory)) {
//            mLogger.log("Prompting the user for access to the accessory.")
            val intent =
                Intent(ACTION_USB_ACCESSORY_PERMISSION)
            intent.setPackage(packageName)
            val pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, 0
            )
            mUsbManager.requestPermission(accessory, pendingIntent)
            return
        }

        // Open the accessory.
        val fd = mUsbManager.openAccessory(accessory)
        if (fd == null) {
            binding.textInfo.text = ("Could not obtain accessory connection.")
            return
        }

        // All set.
        binding.textInfo.text = ("Connected.")
        mConnected = true
        mAccessory = accessory
        mTransport = UsbAccessoryStreamTransport(fd)
//        openAccessory(accessory, fd)
//        startServices()
//        mTransport.startReading()
        ioTaskUsb = IOTaskUsb()
        ioTaskUsb?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun openAccessory(accessory: UsbAccessory, fdec: ParcelFileDescriptor) {
        binding.textInfo.text = ("openAccessory: $accessory")
        fileDescriptor = fdec
        fileDescriptor?.fileDescriptor?.also { fd ->
            inputStream = FileInputStream(fd)
            outputStream = FileOutputStream(fd)
            ioTaskUsb = IOTaskUsb()
            ioTaskUsb?.execute(inputStream)
        }
    }

    class IOTaskUsb :
        AsyncTask<FileInputStream?, String?, Void?>() {
        override fun onPreExecute() {
            super.onPreExecute()
//            progressBar2.setVisibility(ProgressBar.VISIBLE)
        }

        protected override fun doInBackground(vararg inputstreams: FileInputStream?): Void? {
            try {
//                sendName("name" + ua.zp.uvs.wifisender.ServerFileActivity.fileInform.getChosenFile())
//                Thread.sleep(5000)
//                sendFile(
//                    ua.zp.uvs.wifisender.ServerFileActivity.fileInform.getPath(),
//                    ua.zp.uvs.wifisender.ServerFileActivity.fileInform.getChosenFile()
//                )
//                Thread.sleep(5000)
//                sendName(endpoints[0], "Start")
//                publishProgress(readName(inputstreams[0]))
                val buffer = ByteArray(16384)
                val size = mTransport?.ioRead(buffer,0, buffer.size)
                publishProgress(size.toString())
            } catch (e: Exception) {
                publishProgress(e.toString())
            }
            return null
        }

        private fun readName(f: FileInputStream?): String? {

            val buffer = ByteArray(16384)
            if (f != null) {
//                while (f.available() > 0){
//                    f.read(buffer);
//                }
                var n = f.read(buffer)
                while (f.read(buffer) >= 0) {
                    n = f.read(buffer)
                }
                return buffer.toString()
            }

//            // Check that there's actually something to send
//            if (chosenFile.length > 0) {
//                // Get the message bytes and tell the BluetoothChatService to write
//                val send = chosenFile.toByteArray()
//                bytes = send
//            }
//            var bytesWritten = mAccessoryConnection?.bulkTransfer(endpoint, bytes, bytes.size, TIMEOUT)
//            return bytesWritten.toString()
            return ""
        }

        protected override fun onProgressUpdate(vararg nextLine: String?) {
            super.onProgressUpdate(*nextLine)

//            showMessageToast(nextLine[0])
            binding.textInfo.text = nextLine[0]

        }


        override fun onPostExecute(result: Void?) {
            super.onPostExecute(result)
//            progressBar2.setVisibility(ProgressBar.INVISIBLE)
            onCancelled()
        }

        public override fun onCancelled() {
            super.onCancelled()
            //            showMessageToast("onCancelledClient");
        }
    }

    private fun disconnect() {
        binding.textInfo.text = ("Disconnecting from accessory: $mAccessory")
//        stopServices()
//        mLogger.log("Disconnected.")
        mConnected = false
        mAccessory = null
        if (mTransport != null) {
            mTransport!!.ioClose()
            mTransport = null
        }
    }

    private fun isSink(accessory: UsbAccessory): Boolean {
        return MANUFACTURER == accessory.manufacturer && MODEL == accessory.model
    }

    private val mReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val accessory: UsbAccessory? = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY)
            if (accessory != null) {
                val action = intent.action
                if (action == UsbManager.ACTION_USB_ACCESSORY_ATTACHED) {
                    onAccessoryAttached(accessory)
                } else if (action == UsbManager.ACTION_USB_ACCESSORY_DETACHED) {
                    onAccessoryDetached(accessory)
                } else if (ACTION_USB_ACCESSORY_PERMISSION == action) {
                    synchronized(this) {
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            accessory?.apply {
                                //call method to set up accessory communication
                                binding.textInfo.text = ("Accessory permission granted: $accessory")
                                onAccessoryAttached(accessory)
                            }
                        } else {
                            binding.textInfo.text = ("Accessory permission denied: $accessory")
                        }
                    }
                }
            }
        }
    }

}