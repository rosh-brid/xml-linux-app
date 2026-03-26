data class LinearLayout(
    override val id: String? = null,
    val orientation: String = "vertical",
    override val layoutWidth: String = "wrap_content",
    override val layoutHeight: String = "wrap_content",
    override val children: List<WidgetNode> = emptyList()
) : WidgetNode
