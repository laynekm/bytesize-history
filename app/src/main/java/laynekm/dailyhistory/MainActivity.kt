package laynekm.dailyhistory

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem

import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.activityUiThread
import org.jetbrains.anko.doAsyncResult
import org.jetbrains.anko.longToast

class MainActivity : AppCompatActivity() {

    private val contentProvider : ContentProvider = ContentProvider()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        contentProvider.getHistoryData()

//        doAsyncResult {
//            val url = contentProvider.buildURL("Bob Barker")
//            val result = url.readText()
//            Log.wtf("result: ", result)
//            activityUiThread {
//                longToast(result)
//            }
//        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
