package laynekm.bytesize_history

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.squareup.picasso.Picasso
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class HistoryItemAdapter(private val context: Context, private var items: MutableList<HistoryItem>)
    : RecyclerView.Adapter<HistoryItemAdapter.ViewHolder>() {

    private val contentProvider: ContentProvider = ContentProvider()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var historyItem: ConstraintLayout = itemView.findViewById(R.id.historyItem)
        internal var image: ImageView = itemView.findViewById(R.id.historyImage)
        internal var year: TextView = itemView.findViewById(R.id.yearLabel)
        internal var desc: TextView = itemView.findViewById(R.id.descLabel)
        internal var linkView: LinearLayout = itemView.findViewById(R.id.linkView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.history_item, parent, false)
        return ViewHolder(view)
    }

    // TODO: This should probably refactored so the image and content are loaded simultaneously
    // Currently, the content is already there and it takes a moment for the image to load
    override fun onBindViewHolder(viewHolder: ViewHolder, index: Int) {
        viewHolder.year.text = items[index].year
        viewHolder.desc.text = items[index].desc

        // Dynamically inflate link items
        viewHolder.linkView.visibility = View.GONE
        viewHolder.linkView.removeAllViews()
        items[index].links.forEach {
            val link = LayoutInflater.from(context).inflate(R.layout.link_item, null)
            viewHolder.linkView.addView(link)
            val linkText = link.findViewById<TextView>(R.id.linkText)
            linkText.text = it.title
        }

        // onClick displays/hides links
        viewHolder.historyItem.setOnClickListener{
            if (viewHolder.linkView.visibility === View.GONE) viewHolder.linkView.visibility = View.VISIBLE
            else viewHolder.linkView.visibility = View.GONE
        }

        // Fetch images
        doAsync {
            val imageURL = contentProvider.fetchImage(items[index].links)
            uiThread {
                if (imageURL === "") viewHolder.image.setImageResource(R.drawable.default_image)
                else Picasso.get().load(imageURL).into(viewHolder.image)
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun setItems(items: MutableList<HistoryItem>) {
        this.items = items
    }
}