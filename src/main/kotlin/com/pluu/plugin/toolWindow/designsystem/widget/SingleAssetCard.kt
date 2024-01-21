package com.pluu.plugin.toolWindow.designsystem.widget

import com.android.tools.adtui.common.AdtUiUtils
import com.android.tools.adtui.common.border
import com.android.tools.idea.ui.resourcemanager.widget.ChessBoardPanel
import com.intellij.ui.JBColor
import com.intellij.ui.RoundedLineBorder
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.EmptySpacingConfiguration
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.NamedColorUtil
import com.intellij.util.ui.UIUtil
import com.pluu.plugin.toolWindow.designsystem.StartupUiUtil
import com.pluu.plugin.toolWindow.designsystem.model.ApplicableFileType
import com.pluu.plugin.toolWindow.designsystem.model.FilterImageSize
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.font.TextAttribute
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.border.Border
import kotlin.properties.Delegates

// Graphic constant for the view
private val LARGE_MAIN_CELL_BORDER_SELECTED
    get() = BorderFactory.createCompoundBorder(
        JBUI.Borders.empty(2),
        RoundedLineBorder(UIUtil.getTreeSelectionBackground(true), JBUI.scale(2), JBUI.scale(1))
    )

private val LARGE_MAIN_CELL_BORDER_UNFOCUSED
    get() = BorderFactory.createCompoundBorder(
        JBUI.Borders.empty(2),
        RoundedLineBorder(UIUtil.getTreeSelectionBackground(false), JBUI.scale(2), JBUI.scale(1))
    )

private var PREVIEW_BORDER_COLOR: Color = border

private val LARGE_MAIN_CELL_BORDER
    get() = BorderFactory.createCompoundBorder(
        JBUI.Borders.empty(2),
        RoundedLineBorder(PREVIEW_BORDER_COLOR, JBUI.scale(2), JBUI.scale(1))
    )

private val BOTTOM_PANEL_BORDER get() = JBUI.Borders.empty(0)

private val PRIMARY_FONT
    get() = StartupUiUtil.labelFont.deriveFont(
        mapOf(
            TextAttribute.WEIGHT to TextAttribute.WEIGHT_DEMIBOLD,
            TextAttribute.SIZE to JBUI.scaleFontSize(14f)
        )
    )

private val SECONDARY_FONT_SIZE get() = JBUI.scaleFontSize(12f).toFloat()

private val SECONDARY_FONT_COLOR
    get() = JBColor(
        NamedColorUtil.getInactiveTextColor().darker(),
        NamedColorUtil.getInactiveTextColor()
    )

private const val DEFAULT_WIDTH = 120

/**
 * Abstract class to represent a graphical asset in the resource explorer.
 * This allows to set
 */
abstract class AssetView(
    val sampleImageSize: FilterImageSize
) : JPanel(BorderLayout()) {

    /**
     * If true, draw a chessboard as in background of [thumbnail]
     */
    var withChessboard: Boolean by Delegates.observable(false) { _, _, withChessboard ->
        contentWrapper.showChessboard = withChessboard
    }

    /**
     * Set the [JComponent] acting as the thumbnail of the object represented (e.g an image or a color)
     */
    var thumbnail by Delegates.observable(null as JComponent?) { _, old, new ->
        if (old !== new) {
            contentWrapper.removeAll()
            if (new != null) {
                contentWrapper.add(new)
            }
        }
        // When there's nothing to preview, SingleAssetCard and RowAssetView have different behaviors, so we let them deal with it.
        if (new == null) {
            setNonIconLayout()
        } else {
            setIconLayout()
        }
    }

    /**
     * The size of the [thumbnail] container that should be used to compute the size of the thumbnail component
     */
    val thumbnailSize: Dimension get() = contentWrapper.size

    /**
     * Set the width of the whole view.
     */
    var thumbnailWidth: Int by Delegates.observable(DEFAULT_WIDTH) { _, _, newValue ->
        onViewWidthChanged(newValue)
    }

    /**
     * Set the title label of this card
     */
    var componentName: String by Delegates.observable("") { _, _, newValue ->
        componentNameLabel.text = newValue
    }

    /**
     * Set the subtitle label of this card
     */
    var aliasName: String by Delegates.observable("") { _, _, newValue ->
        aliasNameLabel.text = newValue
    }

    var applicableFileType: ApplicableFileType? by Delegates.observable(null) { _, _, newValue ->
        applicableFileTypeLabel.text = newValue?.name.orEmpty()
    }

    protected val componentNameLabel = JBLabel().apply {
        font = PRIMARY_FONT
    }

    protected val aliasNameLabel = JBLabel().apply {
        font = font.deriveFont(SECONDARY_FONT_SIZE)
        foreground = SECONDARY_FONT_COLOR
    }
    protected val applicableFileTypeLabel = JBLabel().apply {
        font = font.deriveFont(SECONDARY_FONT_SIZE)
    }

    abstract var selected: Boolean

    abstract var focused: Boolean

    protected var contentWrapper = ChessBoardPanel().apply {
        showChessboard = withChessboard
    }

    var isNew: Boolean by Delegates.observable(false) { _, _, new -> newLabel.isVisible = new }

    protected val newLabel = object : JBLabel(" NEW ", SwingConstants.CENTER) {

        init {
            font = JBUI.Fonts.label(8f)
            foreground = JBColor.WHITE
            isVisible = isNew
        }

        override fun paintComponent(g: Graphics) {
            g.color = UIUtil.getTreeSelectionBorderColor()
            val antialias = (g as Graphics2D).getRenderingHint(RenderingHints.KEY_ANTIALIASING)
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val insets = insets
            val descent = getFontMetrics(font).descent
            val height = height - insets.bottom - descent // Ensure that text is centered within the background
            g.fillRoundRect(insets.left, insets.top, width - insets.right, height, height, height)
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialias)
            super.paintComponent(g)
        }
    }

    /**
     * Called when [thumbnailWidth] is changed
     */
    private fun onViewWidthChanged(width: Int) {
        val thumbnailSize = computeThumbnailSize(width)
        contentWrapper.preferredSize = thumbnailSize
        contentWrapper.size = thumbnailSize
        validate()
    }

    /**
     * Subclass implement this method to specify the size of the [thumbnail] giving the
     * desired [width]
     */
    protected abstract fun computeThumbnailSize(width: Int): Dimension

    protected abstract fun getBorder(selected: Boolean, focused: Boolean): Border

    /** Adjust layout when there is no icon to preview. */
    protected abstract fun setNonIconLayout()

    /** Adjust layout for when there's an icon to preview. */
    protected abstract fun setIconLayout()
}

/**
 * Component in the shape of a card with a large preview
 * and some textual info below.
 */
class RowAssetView(
    sampleImageSize: FilterImageSize
) : AssetView(sampleImageSize) {

    override var selected by Delegates.observable(false) { _, _, selected ->
        border = getBorder(selected, focused)
    }

    override var focused: Boolean by Delegates.observable(false) { _, _, focused ->
        border = getBorder(selected, focused)
    }

    private val firstPanel = panel {
        customizeSpacingConfiguration(EmptySpacingConfiguration()) {
            row {
                cell(componentNameLabel)
                cell(applicableFileTypeLabel).align(AlignX.RIGHT)
            }
        }
    }.withBorder(JBUI.Borders.empty(2, 4))

    private val bottomPanel = panel {
        customizeSpacingConfiguration(EmptySpacingConfiguration()) {
            row { cell(firstPanel).align(Align.FILL) }
            row { cell(aliasNameLabel) }
        }
    }.withBorder(JBUI.Borders.empty(2, 4))

    private val emptyLabel = JBLabel("Nothing to show", SwingConstants.CENTER).apply {
        foreground = AdtUiUtils.DEFAULT_FONT_COLOR
        font = JBUI.Fonts.label(10f)
    }

    init {
        isOpaque = false
        border = LARGE_MAIN_CELL_BORDER

        if (sampleImageSize.isVisible()) {
            add(contentWrapper, BorderLayout.NORTH)
        }
        add(bottomPanel, BorderLayout.SOUTH)

        thumbnailWidth = if (sampleImageSize.isVisible()) {
            DEFAULT_WIDTH
        } else {
            (DEFAULT_WIDTH / 2f).toInt()
        }
    }

    override fun computeThumbnailSize(width: Int) = Dimension(width, (width * 0.75f).toInt())

    override fun getBorder(selected: Boolean, focused: Boolean): Border = when {
        selected && focused -> LARGE_MAIN_CELL_BORDER_SELECTED
        selected && !focused -> LARGE_MAIN_CELL_BORDER_UNFOCUSED
        else -> LARGE_MAIN_CELL_BORDER
    }

    override fun setIconLayout() {
        // No need to do anything.
    }

    override fun setNonIconLayout() {
        contentWrapper.removeAll()
        contentWrapper.add(emptyLabel)
    }
}

