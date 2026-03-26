data class TextView(
    override val id: String? = null,
    val text: String = "",
    override val layoutWidth: String = "wrap_content",
    override val layoutHeight: String = "wrap_content",
    override val children: List<WidgetNode> = emptyList()
) : WidgetNode
