package de.tradebuddy.ui.icons

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowDown
import com.adamglin.phosphoricons.regular.ArrowLeft
import com.adamglin.phosphoricons.regular.ArrowRight
import com.adamglin.phosphoricons.regular.ArrowUp
import com.adamglin.phosphoricons.regular.ArrowsClockwise
import com.adamglin.phosphoricons.regular.ArrowsDownUp
import com.adamglin.phosphoricons.regular.Bell
import com.adamglin.phosphoricons.regular.BellRinging
import com.adamglin.phosphoricons.regular.Bookmark
import com.adamglin.phosphoricons.regular.Buildings
import com.adamglin.phosphoricons.regular.CalendarBlank
import com.adamglin.phosphoricons.regular.CalendarDots
import com.adamglin.phosphoricons.regular.CaretDown
import com.adamglin.phosphoricons.regular.CaretUp
import com.adamglin.phosphoricons.regular.ChartLineUp
import com.adamglin.phosphoricons.regular.ChartPie
import com.adamglin.phosphoricons.regular.ChatCircle
import com.adamglin.phosphoricons.regular.Check
import com.adamglin.phosphoricons.regular.CheckSquare
import com.adamglin.phosphoricons.regular.Clock
import com.adamglin.phosphoricons.regular.Copy
import com.adamglin.phosphoricons.regular.DotsThree
import com.adamglin.phosphoricons.regular.FolderOpen
import com.adamglin.phosphoricons.regular.Funnel
import com.adamglin.phosphoricons.regular.GearSix
import com.adamglin.phosphoricons.regular.List
import com.adamglin.phosphoricons.regular.MagnifyingGlass
import com.adamglin.phosphoricons.regular.Minus
import com.adamglin.phosphoricons.regular.Moon
import com.adamglin.phosphoricons.regular.MoonStars
import com.adamglin.phosphoricons.regular.Paperclip
import com.adamglin.phosphoricons.regular.PencilSimple
import com.adamglin.phosphoricons.regular.Plus
import com.adamglin.phosphoricons.regular.SlidersHorizontal
import com.adamglin.phosphoricons.regular.Square
import com.adamglin.phosphoricons.regular.SquaresFour
import com.adamglin.phosphoricons.regular.Star
import com.adamglin.phosphoricons.regular.Sun
import com.adamglin.phosphoricons.regular.Trash
import com.adamglin.phosphoricons.regular.User
import com.adamglin.phosphoricons.regular.X

object SnowIcons {
    val ArrowDown: ImageVector
        get() = PhosphorIcons.Regular.ArrowDown
    val ArrowLeft: ImageVector
        get() = PhosphorIcons.Regular.ArrowLeft
    val ArrowRight: ImageVector
        get() = PhosphorIcons.Regular.ArrowRight
    val ArrowUp: ImageVector
        get() = PhosphorIcons.Regular.ArrowUp
    val Refresh: ImageVector
        get() = PhosphorIcons.Regular.ArrowsClockwise
    val Sort: ImageVector
        get() = PhosphorIcons.Regular.ArrowsDownUp
    val Bell: ImageVector
        get() = PhosphorIcons.Regular.Bell
    val BellActive: ImageVector
        get() = PhosphorIcons.Regular.BellRinging
    val Bookmark: ImageVector
        get() = PhosphorIcons.Regular.Bookmark
    val Buildings: ImageVector
        get() = PhosphorIcons.Regular.Buildings
    val Calendar: ImageVector
        get() = PhosphorIcons.Regular.CalendarBlank
    val CalendarEvent: ImageVector
        get() = PhosphorIcons.Regular.CalendarDots
    val CaretDown: ImageVector
        get() = PhosphorIcons.Regular.CaretDown
    val CaretUp: ImageVector
        get() = PhosphorIcons.Regular.CaretUp
    val ChartLine: ImageVector
        get() = PhosphorIcons.Regular.ChartLineUp
    val ChartPie: ImageVector
        get() = PhosphorIcons.Regular.ChartPie
    val Chat: ImageVector
        get() = PhosphorIcons.Regular.ChatCircle
    val Check: ImageVector
        get() = PhosphorIcons.Regular.Check
    val CheckSquare: ImageVector
        get() = PhosphorIcons.Regular.CheckSquare
    val Clock: ImageVector
        get() = PhosphorIcons.Regular.Clock
    val Copy: ImageVector
        get() = PhosphorIcons.Regular.Copy
    val Dots: ImageVector
        get() = PhosphorIcons.Regular.DotsThree
    val FolderOpen: ImageVector
        get() = PhosphorIcons.Regular.FolderOpen
    val Filter: ImageVector
        get() = PhosphorIcons.Regular.Funnel
    val Gear: ImageVector
        get() = PhosphorIcons.Regular.GearSix
    val Menu: ImageVector
        get() = PhosphorIcons.Regular.List
    val Search: ImageVector
        get() = PhosphorIcons.Regular.MagnifyingGlass
    val Minus: ImageVector
        get() = PhosphorIcons.Regular.Minus
    val Moon: ImageVector
        get() = PhosphorIcons.Regular.Moon
    val Theme: ImageVector
        get() = PhosphorIcons.Regular.MoonStars
    val Paperclip: ImageVector
        get() = PhosphorIcons.Regular.Paperclip
    val Edit: ImageVector
        get() = PhosphorIcons.Regular.PencilSimple
    val Plus: ImageVector
        get() = PhosphorIcons.Regular.Plus
    val Sliders: ImageVector
        get() = PhosphorIcons.Regular.SlidersHorizontal
    val Square: ImageVector
        get() = PhosphorIcons.Regular.Square
    val Dashboard: ImageVector
        get() = PhosphorIcons.Regular.SquaresFour
    val Star: ImageVector
        get() = PhosphorIcons.Regular.Star
    val Sun: ImageVector
        get() = PhosphorIcons.Regular.Sun
    val Delete: ImageVector
        get() = PhosphorIcons.Regular.Trash
    val User: ImageVector
        get() = PhosphorIcons.Regular.User
    val Close: ImageVector
        get() = PhosphorIcons.Regular.X
}

@Composable
fun SnowIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}
