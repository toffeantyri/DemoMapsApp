package ru.toffeantyri.demomapsapp.utils

import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import ru.toffeantyri.demomapsapp.R


fun AppCompatActivity.checkPermissionSingle(permission: String, onSuccess: () -> Unit) {

    val phoneStateRequestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            onSuccess()
        } else {
            Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    if (this.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
        onSuccess()
    } else {
        phoneStateRequestPermissionLauncher.launch(permission)
    }
}

