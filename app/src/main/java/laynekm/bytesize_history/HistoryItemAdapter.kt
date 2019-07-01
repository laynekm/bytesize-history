package laynekm.bytesize_history

import android.app.ActionBar
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import com.squareup.picasso.Picasso
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import android.graphics.Bitmap
import android.widget.*
import com.squareup.picasso.Callback
import android.support.v7.widget.Toolbar
import android.util.Log
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.history_item.view.*
import android.view.ViewGroup.MarginLayoutParams
import android.util.TypedValue





class HistoryItemAdapter(private val context: Context, private var items: MutableList<HistoryItem>)
    : RecyclerView.Adapter<HistoryItemAdapter.ViewHolder>() {

    private val contentProvider = ContentProvider()
    private val toolbar = (context as Activity).findViewById(R.id.toolbar) as Toolbar
    private val webView = (context as Activity).findViewById(R.id.webView) as WebView
    private val progressBar = (context as Activity).findViewById(R.id.progressBar) as ProgressBar

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var historyItem: ConstraintLayout = itemView.findViewById(R.id.historyItem)
        internal var image: ImageView = itemView.findViewById(R.id.historyImage)
        internal var year: TextView = itemView.findViewById(R.id.yearLabel)
        internal var desc: TextView = itemView.findViewById(R.id.descLabel)
        internal var linkView: LinearLayout = itemView.findViewById(R.id.linkView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.history_item, parent, false)

        webView.webViewClient = object: WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.GONE
            }
        }

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, index: Int) {

        // Clear existing resources
        viewHolder.image.setImageResource(0)
        viewHolder.historyItem.setBackgroundResource(0)
        viewHolder.linkView.setBackgroundResource(0)
        if (!items[index].hasFetchedImage) { viewHolder.historyItem.visibility = View.GONE }

        // Add margins based on history item depth, include left border if depth > 0
        val margin = items[index].depth * 25
        val dpMargin = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            margin.toFloat(),
            context.resources.displayMetrics
        ).toInt()

        if (items[index].depth > 0) {
            viewHolder.historyItem.setBackgroundResource(R.drawable.borderleft)
            viewHolder.linkView.setBackgroundResource(R.drawable.borderleft)
        }

        (viewHolder.historyItem.layoutParams as MarginLayoutParams).leftMargin = dpMargin
        (viewHolder.linkView.layoutParams as MarginLayoutParams).leftMargin = dpMargin

        // Set year
        if (items[index].year === null || items[index].year == 0) viewHolder.year.text = ""
        else if (items[index].year!! < 0) {
            viewHolder.year.text = context.resources.getString(R.string.BC_text, items[index].year!! * -1)
        }
        else viewHolder.year.text = "${items[index].year}"
        viewHolder.desc.text = items[index].desc

        // Dynamically inflate link items
        viewHolder.linkView.visibility = View.GONE
        viewHolder.linkView.removeAllViews()
        items[index].links.forEach {
            val link = LayoutInflater.from(context).inflate(R.layout.link_item, null)
            val linkText = link.findViewById<TextView>(R.id.linkText)
            val url = it.link
            viewHolder.linkView.addView(link)
            linkText.text = it.title

            // Attach onClick handler to open link
            link.setOnClickListener {
                progressBar.visibility = View.VISIBLE
                webView.visibility = View.VISIBLE
                webView.loadUrl(url)
                toolbar.setNavigationIcon(R.drawable.back_arrow)
            }
        }

        // onClick displays/hides links
        // TODO: Add animation
        viewHolder.historyItem.setOnClickListener {
            if (viewHolder.linkView.visibility == View.GONE) viewHolder.linkView.visibility = View.VISIBLE
            else viewHolder.linkView.visibility = View.GONE
        }

        // If item already has an image then display it, otherwise fetch the image
        // Set hasFetchedImage boolean rather than check image since some items don't have images and will refetch
        doAsync {
            val image: String
            if (!items[index].hasFetchedImage) {
                image = contentProvider.fetchImage(items[index].links)
                items[index].image = image
                items[index].hasFetchedImage = true
            } else {
                image = items[index].image
            }

            uiThread {
                if (image === "") viewHolder.image.setImageResource(R.drawable.default_image)
                else Picasso.get()
                    .load(image)
                    .resize(100, 100)
                    .centerCrop()
                    .into(viewHolder.image, object: Callback {
                        override fun onSuccess() { viewHolder.historyItem.visibility = View.VISIBLE }
                        override fun onError(exception: Exception) { viewHolder.historyItem.visibility = View.VISIBLE }
                    })
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun setItems(items: MutableList<HistoryItem>) {
        this.items = items
        notifyDataSetChanged()
    }
}