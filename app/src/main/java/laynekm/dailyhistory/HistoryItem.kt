package laynekm.dailyhistory

// TODO: Implement actual image types
// HistoryItem consists of a date, text content, image, and links to web pages for further reading
class HistoryItem {
    var date: String? = null
    var content: String? = null
    var image: String? = null
    var links: MutableList<Link>? = null
}

class Link {
    var content: String? = null
    var link: String? = null
}