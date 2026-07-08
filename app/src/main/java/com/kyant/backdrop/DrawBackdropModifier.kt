package com.kyant.backdrop

import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset

private val DefaultOnDrawBackdrop: DrawScope.(DrawScope.() -> Unit) -> Unit = { it() }

fun Modifier.drawPlainBackdrop(
    backdrop: Backdrop,
    shape: () -> Shape,
    effects: BackdropEffectScope.() -> Unit,
    onDrawBehind: (DrawScope.() -> Unit)? = null,
    onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit = DefaultOnDrawBackdrop,
    onDrawSurface: (DrawScope.() -> Unit)? = null,
    onDrawFront: (DrawScope.() -> Unit)? = null
): Modifier = this.then(
    DrawBackdropElement(
        backdrop = backdrop,
        shapeBlock = shape,
        effects = effects,
        onDrawBehind = onDrawBehind,
        onDrawBackdrop = onDrawBackdrop,
        onDrawSurface = onDrawSurface,
        onDrawFront = onDrawFront
    )
)

private class DrawBackdropElement(
    val backdrop: Backdrop,
    val shapeBlock: () -> Shape,
    val effects: BackdropEffectScope.() -> Unit,
    val onDrawBehind: (DrawScope.() -> Unit)?,
    val onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit,
    val onDrawSurface: (DrawScope.() -> Unit)?,
    val onDrawFront: (DrawScope.() -> Unit)?
) : ModifierNodeElement<DrawBackdropNode>() {
    override fun create(): DrawBackdropNode = DrawBackdropNode(
        backdrop, shapeBlock, effects, onDrawBehind, onDrawBackdrop, onDrawSurface, onDrawFront
    )
    override fun update(node: DrawBackdropNode) {
        node.backdrop = backdrop
        node.shapeBlock = shapeBlock
        node.effects = effects
        node.onDrawBehind = onDrawBehind
        node.onDrawBackdrop = onDrawBackdrop
        node.onDrawSurface = onDrawSurface
        node.onDrawFront = onDrawFront
    }
    override fun InspectorInfo.inspectableProperties() {
        name = "drawPlainBackdrop"
        properties["backdrop"] = backdrop
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DrawBackdropElement) return false
        return backdrop == other.backdrop && shapeBlock == other.shapeBlock
    }
    override fun hashCode(): Int = backdrop.hashCode() * 31 + shapeBlock.hashCode()
}

private class DrawBackdropNode(
    var backdrop: Backdrop,
    var shapeBlock: () -> Shape,
    var effects: BackdropEffectScope.() -> Unit,
    var onDrawBehind: (DrawScope.() -> Unit)?,
    var onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit,
    var onDrawSurface: (DrawScope.() -> Unit)?,
    var onDrawFront: (DrawScope.() -> Unit)?
) : LayoutModifierNode, DrawModifierNode, GlobalPositionAwareModifierNode, ObserverModifierNode, Modifier.Node() {

    private val effectScope = BackdropEffectScopeImpl()
    private var shape: Shape? by mutableStateOf(null)
    private var renderEffect by mutableStateOf<androidx.compose.ui.graphics.RenderEffect?>(null)

    private val layoutLayerBlock: androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit = {
        clip = true
        shape = this@DrawBackdropNode.shape ?: androidx.compose.ui.graphics.Shape { size, _, _ -> androidx.compose.ui.graphics.Outline.Rectangle(size) }
        renderEffect = this@DrawBackdropNode.renderEffect
        compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
    }

    override fun MeasureScope.measure(
        measurable: Measurable, constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        shape = shapeBlock()
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(IntOffset.Zero, layerBlock = layoutLayerBlock)
        }
    }

    override fun ContentDrawScope.draw() {
        if (effectScope.update(this)) updateEffects()

        onDrawBehind?.invoke(this)
        onDrawBackdrop { backdrop.draw(this, effectScope) }
        onDrawSurface?.invoke(this)
        drawContent()
        onDrawFront?.invoke(this)
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {}

    override fun onObservedReadsChanged() { observeEffects() }

    private fun observeEffects() { observeReads { updateEffects() } }

    private fun updateEffects() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            effectScope.apply(effects)
            renderEffect = effectScope.renderEffect?.asComposeRenderEffect()
        }
    }

    override fun onAttach() { observeEffects() }
    override fun onDetach() { effectScope.reset(); shape = null; renderEffect = null }
}
