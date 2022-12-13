package ua.zp.uvs.usbaccessory

import android.content.Context
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import ua.zp.uvs.usbaccessory.databinding.ActivityMainBinding

lateinit var binding : ActivityMainBinding
lateinit var manager: UsbManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding =
            DataBindingUtil.setContentView(this, R.layout.activity_main)

        manager = getSystemService(Context.USB_SERVICE) as UsbManager

        binding.buttonCheckAccessory.setOnClickListener(View.OnClickListener {
            checkAccessoryFun()
        }
        )

    }

    private fun checkAccessoryFun() {
        val accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY) as UsbAccessory?
        var accessoryList: Array<out UsbAccessory>? = null
        accessoryList = manager.accessoryList
        var i = ""
        if(accessoryList !=null && accessoryList.isNotEmpty()){
            accessoryList.forEach {
                i += "\n" +
                        "AccessoryDescription: " + it.description + "\n" +
                        "AccessoryUri: " + it.uri + "\n" +
                        "AccessoryModel: " + it.model + "\n" +
                        "AccessoryManufacturer: " + it.manufacturer + "\n" +
                        "AccessoryVersion: " + it.version + "\n" +
                        "AccessorySerial: " + it.serial + "\n";
            }
        }
        else{
            i += "\n" +
                    "Devices not find " + "\n" +
                    "DeviceID: " + "\n" +
                    "DeviceName: "  + "\n" +
                    "DeviceClass: "  + "\n" +
                    "DeviceSubClass: "  + "\n" +
                    "VendorID: "  + "\n" +
                    "ProductID: "  + "\n";
        }
        binding.textInfo.text = i
    }
}