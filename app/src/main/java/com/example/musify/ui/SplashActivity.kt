package com.example.musify.ui

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.animation.AnimationUtils
import androidx.core.app.ActivityCompat
import com.example.musify.R
import com.example.musify.checkAllPermission
import kotlinx.android.synthetic.main.splash_activity.*
import kotlinx.coroutines.*

private const val TAG = "SPLASHACTIVITY"
class SplashActivity : AppCompatActivity() {

    private val REQUEST_CODE = 12
    private val REQUEST_APPSETTING = 168
    var PERMISSIONS = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_activity)
        animationSplash()
        if (checkAllPermission(this)){
            goToMainActivity()
        } else {
            ActivityCompat.requestPermissions(this,PERMISSIONS,REQUEST_CODE)
        }
    }
    private fun goToMainActivity(){
        CoroutineScope(Dispatchers.IO).launch {
            delay(1500)
            withContext(Dispatchers.Main){
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                finish()
            }
        }
    }
    private fun goToSetting(){
        val appSetting = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        appSetting.setData(Uri.fromParts("package", this.packageName,null))
        startActivityForResult(appSetting,REQUEST_APPSETTING)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_APPSETTING){
            if (checkAllPermission(this)){
                goToMainActivity()
            } else {
                showDiaglog()
            }
        }
    }
    private fun animationSplash()
    {
            var animation_image = AnimationUtils.loadAnimation(this@SplashActivity,R.anim.top_animation)
            img_disk.animation = animation_image
            var animation_welcome = AnimationUtils.loadAnimation(this@SplashActivity,R.anim.bottom_animation)
            tv_welcome.animation = animation_welcome
    }
    private fun showDiaglog(){
        val dialog = AlertDialog.Builder(this)
        dialog.apply {
            setMessage("You need to give access to storage and record permission to use this app")
            setTitle("Permission required")
            setPositiveButton("Accept") {dialog , _ ->
                goToSetting()
            }
            show()
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_CODE){
            if(!grantResults.contains(PackageManager.PERMISSION_DENIED)){
                goToMainActivity()
            } else {
                showDiaglog()
            }
        }
    }
}