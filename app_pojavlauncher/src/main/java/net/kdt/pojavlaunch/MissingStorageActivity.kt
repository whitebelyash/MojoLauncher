package net.kdt.pojavlaunch

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import git.artdeell.mojo.R

class MissingStorageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.storage_test_no_sdcard)
    }
}
