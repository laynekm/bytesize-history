package laynekm.dailyhistory

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val contentProvider : ContentProvider = ContentProvider()
    private lateinit var historyItemAdapter: HistoryItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        // Populate recycler view with empty list to initialize
        val item = HistoryItem(Type.EVENT, "1900", "Test", "", mutableListOf(Link("test")))
        val items = mutableListOf(item)
        populateRecyclerView(items)

        // Fetch history data from API and update recycler view
        contentProvider.getHistoryData(::updateRecyclerView)
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

    private fun populateRecyclerView(items: MutableList<HistoryItem>) {
        var historyItemView: RecyclerView = findViewById(R.id.historyItems)
        historyItemAdapter = HistoryItemAdapter(this, items)
        historyItemView.adapter = historyItemAdapter
        historyItemView.layoutManager = LinearLayoutManager(this)
    }

    private fun updateRecyclerView(items: MutableList<HistoryItem>) {
        items.forEach{
            Log.wtf("Got the items", it.toString())
        }

        historyItemAdapter.setItems(items)
        historyItemAdapter.notifyDataSetChanged()
    }
}
