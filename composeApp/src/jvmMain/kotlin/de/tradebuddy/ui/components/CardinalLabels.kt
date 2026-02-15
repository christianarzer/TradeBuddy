package de.tradebuddy.ui.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.cardinal_e
import trade_buddy.composeapp.generated.resources.cardinal_ene
import trade_buddy.composeapp.generated.resources.cardinal_ese
import trade_buddy.composeapp.generated.resources.cardinal_n
import trade_buddy.composeapp.generated.resources.cardinal_ne
import trade_buddy.composeapp.generated.resources.cardinal_nne
import trade_buddy.composeapp.generated.resources.cardinal_nnw
import trade_buddy.composeapp.generated.resources.cardinal_nw
import trade_buddy.composeapp.generated.resources.cardinal_s
import trade_buddy.composeapp.generated.resources.cardinal_se
import trade_buddy.composeapp.generated.resources.cardinal_sse
import trade_buddy.composeapp.generated.resources.cardinal_ssw
import trade_buddy.composeapp.generated.resources.cardinal_sw
import trade_buddy.composeapp.generated.resources.cardinal_w
import trade_buddy.composeapp.generated.resources.cardinal_wnw
import trade_buddy.composeapp.generated.resources.cardinal_wsw

private val cardinalResources = listOf(
    Res.string.cardinal_n,
    Res.string.cardinal_nne,
    Res.string.cardinal_ne,
    Res.string.cardinal_ene,
    Res.string.cardinal_e,
    Res.string.cardinal_ese,
    Res.string.cardinal_se,
    Res.string.cardinal_sse,
    Res.string.cardinal_s,
    Res.string.cardinal_ssw,
    Res.string.cardinal_sw,
    Res.string.cardinal_wsw,
    Res.string.cardinal_w,
    Res.string.cardinal_wnw,
    Res.string.cardinal_nw,
    Res.string.cardinal_nnw
)

@Composable
fun cardinalLabel(index: Int): String {
    val safe = index.coerceIn(0, cardinalResources.lastIndex)
    return stringResource(cardinalResources[safe])
}

