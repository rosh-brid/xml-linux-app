data class ImageView(
    override val id: String? = null,
    val src: String = "",
    override val layoutWidth: String = "wrap_content",
    override val layoutHeight: String = "wrap_content",
    override val children: List<WidgetNode> = emptyList()
) : WidgetNode
