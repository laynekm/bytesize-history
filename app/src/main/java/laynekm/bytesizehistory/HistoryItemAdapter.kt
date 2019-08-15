package laynekm.bytesizehistory

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.squareup.picasso.Picasso
import android.widget.*
import android.widget.LinearLayout
import android.view.ViewGroup.MarginLayoutParams
import android.util.TypedValue
import com.squareup.picasso.Callback

// Adapter that is used for all recycler views in MainActivity
class HistoryItemAdapter(
    private val context: Context,
    private val presenter: MainPresenter,
    private var items: MutableList<HistoryItem>)
    : RecyclerView.Adapter<HistoryItemAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var historyItemContainer: LinearLayout = itemView.findViewById(R.id.historyItemContainer)
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

    // TODO: Add animation on dropdown select
    override fun onBindViewHolder(viewHolder: ViewHolder, index: Int) {
        val item = items[index]

        // Clear existing resources
        viewHolder.image.setImageResource(0)
        viewHolder.historyItemContainer.setBackgroundResource(0)
        viewHolder.historyItem.visibility = View.GONE

        // Add margins based on history item depth, include left border if depth > 0
        val margin = item.depth * 25
        val dpMargin = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            margin.toFloat(),
            context.resources.displayMetrics
        ).toInt()

        (viewHolder.historyItemContainer.layoutParams as MarginLayoutParams).leftMargin = dpMargin
        if (item.depth > 0) viewHolder.historyItemContainer.setBackgroundResource(R.drawable.border_left)

        // Set year and description
        viewHolder.year.text = item.formattedYear
        viewHolder.desc.text = item.desc

        // Dynamically inflate link items
        viewHolder.linkView.removeAllViews()
        item.links.forEach {
            val link = LayoutInflater.from(context).inflate(R.layout.link_item, null)
            val linkText = link.findViewById<TextView>(R.id.linkText)
            val url = it.link
            viewHolder.linkView.addView(link)
            linkText.text = it.title

            // Attach onClick handler to open link
            link.setOnClickListener { presenter.showWebView(url) }
        }

        // onClick displays/hides links
        if (item.linksVisible) viewHolder.linkView.visibility = View.VISIBLE
        else viewHolder.linkView.visibility = View.GONE
        viewHolder.historyItem.setOnClickListener {
            if (viewHolder.linkView.visibility == View.GONE) {
                viewHolder.linkView.visibility = View.VISIBLE
                item.linksVisible = true
            } else {
                viewHolder.linkView.visibility = View.GONE
                item.linksVisible = false
            }
        }

        if (item.image == "") {
            viewHolder.image.setImageResource(R.drawable.default_image)
            onFetchFinished(viewHolder)
        } else Picasso.get()
            .load(item.image)
            .resize(100, 100)
            .centerCrop()
            .into(viewHolder.image, object: Callback {
                override fun onSuccess() { onFetchFinished(viewHolder) }
                override fun onError(exception: Exception) { onFetchFinished(viewHolder) }
            })
    }

    private fun onFetchFinished(viewHolder: ViewHolder) {
        viewHolder.historyItem.visibility = View.VISIBLE
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun setItems(items: MutableList<HistoryItem>) {
        this.items = items
        notifyDataSetChanged()
    }
}