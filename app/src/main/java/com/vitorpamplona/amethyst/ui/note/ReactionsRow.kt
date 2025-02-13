package com.vitorpamplona.amethyst.ui.note

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.ui.actions.NewPostView
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountViewModel
import com.vitorpamplona.amethyst.ui.theme.BitcoinOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ReactionsRow(baseNote: Note, accountViewModel: AccountViewModel, nav: (String) -> Unit) {
    val accountState by accountViewModel.accountLiveData.observeAsState()
    val account = remember(accountState) { accountState?.account } ?: return

    val grayTint = MaterialTheme.colors.onSurface.copy(alpha = 0.32f)

    var wantsToReplyTo by remember {
        mutableStateOf<Note?>(null)
    }

    var wantsToQuote by remember {
        mutableStateOf<Note?>(null)
    }

    if (wantsToReplyTo != null) {
        NewPostView({ wantsToReplyTo = null }, wantsToReplyTo, null, account, accountViewModel, nav)
    }

    if (wantsToQuote != null) {
        NewPostView({ wantsToQuote = null }, null, wantsToQuote, account, accountViewModel, nav)
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(verticalAlignment = CenterVertically) {
        Row(verticalAlignment = CenterVertically, modifier = Modifier.weight(1f)) {
            ReplyReaction(baseNote, grayTint, accountViewModel) {
                wantsToReplyTo = baseNote
            }
        }
        Row(verticalAlignment = CenterVertically, modifier = Modifier.weight(1f)) {
            BoostReaction(baseNote, grayTint, accountViewModel) {
                wantsToQuote = baseNote
            }
        }
        Row(verticalAlignment = CenterVertically, modifier = Modifier.weight(1f)) {
            LikeReaction(baseNote, grayTint, accountViewModel)
        }
        Row(verticalAlignment = CenterVertically, modifier = Modifier.weight(1f)) {
            ZapReaction(baseNote, grayTint, accountViewModel)
        }
        Row(verticalAlignment = CenterVertically, modifier = Modifier.weight(1f)) {
            ViewCountReaction(baseNote.idHex, grayTint)
        }
    }
}

@Composable
fun ReplyReaction(
    baseNote: Note,
    grayTint: Color,
    accountViewModel: AccountViewModel,
    showCounter: Boolean = true,
    iconSize: Dp = 20.dp,
    onPress: () -> Unit
) {
    val repliesState by baseNote.live().replies.observeAsState()
    val replies = remember(repliesState) { repliesState?.note?.replies } ?: emptySet()

    val isWriteable = remember { accountViewModel.isWriteable() }

    val replyCount by remember(repliesState) {
        derivedStateOf {
            showCount(replies.size)
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val iconButtonModifier = remember {
        Modifier.size(iconSize)
    }

    val iconModifier = remember {
        Modifier.size(iconSize)
    }

    IconButton(
        modifier = iconButtonModifier,
        onClick = {
            if (isWriteable) {
                onPress()
            } else {
                scope.launch {
                    Toast.makeText(
                        context,
                        context.getString(R.string.login_with_a_private_key_to_be_able_to_reply),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_comment),
            null,
            modifier = iconModifier,
            tint = grayTint
        )
    }

    if (showCounter) {
        Text(
            " $replyCount",
            fontSize = 14.sp,
            color = grayTint
        )
    }
}

@Composable
fun BoostReaction(
    baseNote: Note,
    grayTint: Color,
    accountViewModel: AccountViewModel,
    iconSize: Dp = 20.dp,
    onQuotePress: () -> Unit
) {
    val boostsState by baseNote.live().boosts.observeAsState()
    val boostedNote = remember(boostsState) { boostsState?.note } ?: return

    val hasBoosted by remember(boostsState) {
        derivedStateOf {
            accountViewModel.hasBoosted(baseNote)
        }
    }

    val wasBoostedByLoggedIn by remember(boostsState) {
        derivedStateOf {
            boostedNote.isBoostedBy(accountViewModel.userProfile())
        }
    }

    val isWriteable = remember { accountViewModel.isWriteable() }

    val boostCount by remember(boostsState) {
        derivedStateOf {
            showCount(boostedNote.boosts.size)
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var wantsToBoost by remember { mutableStateOf(false) }

    val iconButtonModifier = remember {
        Modifier.size(iconSize)
    }

    val iconModifier = remember {
        Modifier.size(iconSize)
    }

    IconButton(
        modifier = iconButtonModifier,
        onClick = {
            if (isWriteable) {
                if (hasBoosted) {
                    accountViewModel.deleteBoostsTo(baseNote)
                } else {
                    wantsToBoost = true
                }
            } else {
                scope.launch {
                    Toast.makeText(
                        context,
                        context.getString(R.string.login_with_a_private_key_to_be_able_to_boost_posts),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    ) {
        if (wantsToBoost) {
            BoostTypeChoicePopup(
                baseNote,
                accountViewModel,
                onDismiss = {
                    wantsToBoost = false
                },
                onQuote = {
                    wantsToBoost = false
                    onQuotePress()
                }
            )
        }

        Icon(
            painter = painterResource(R.drawable.ic_retweeted),
            null,
            modifier = iconModifier,
            tint = if (wasBoostedByLoggedIn) Color.Unspecified else grayTint
        )
    }

    Text(
        " $boostCount",
        fontSize = 14.sp,
        color = grayTint
    )
}

@Composable
fun LikeReaction(
    baseNote: Note,
    grayTint: Color,
    accountViewModel: AccountViewModel,
    iconSize: Dp = 20.dp,
    heartSize: Dp = 16.dp
) {
    val reactionsState by baseNote.live().reactions.observeAsState()
    val reactedNote = remember(reactionsState) { reactionsState?.note } ?: return

    val hasReacted by remember(reactionsState) {
        derivedStateOf {
            accountViewModel.hasReactedTo(baseNote)
        }
    }

    val wasReactedByLoggedIn by remember(reactionsState) {
        derivedStateOf {
            reactedNote.isReactedBy(accountViewModel.userProfile())
        }
    }

    val isWriteable = remember { accountViewModel.isWriteable() }

    val reactionCount by remember(reactionsState) {
        derivedStateOf {
            showCount(reactedNote.reactions.size)
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val iconButtonModifier = remember {
        Modifier.size(iconSize)
    }

    val iconModifier = remember {
        Modifier.size(heartSize)
    }

    IconButton(
        modifier = iconButtonModifier,
        onClick = {
            if (isWriteable) {
                if (hasReacted) {
                    accountViewModel.deleteReactionTo(baseNote)
                } else {
                    accountViewModel.reactTo(baseNote)
                }
            } else {
                scope.launch {
                    Toast.makeText(
                        context,
                        context.getString(R.string.login_with_a_private_key_to_like_posts),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    ) {
        if (wasReactedByLoggedIn) {
            Icon(
                painter = painterResource(R.drawable.ic_liked),
                null,
                modifier = iconModifier,
                tint = Color.Unspecified
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_like),
                null,
                modifier = iconModifier,
                tint = grayTint
            )
        }
    }

    Text(
        " $reactionCount",
        fontSize = 14.sp,
        color = grayTint
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun ZapReaction(
    baseNote: Note,
    grayTint: Color,
    accountViewModel: AccountViewModel,
    textModifier: Modifier = Modifier,
    iconSize: Dp = 20.dp,
    animationSize: Dp = 14.dp
) {
    val accountState by accountViewModel.accountLiveData.observeAsState()
    val account = remember(accountState) { accountState?.account } ?: return

    val zapsState by baseNote.live().zaps.observeAsState()
    val zappedNote = remember(zapsState) { zapsState?.note } ?: return

    var wantsToZap by remember { mutableStateOf(false) }
    var wantsToChangeZapAmount by remember { mutableStateOf(false) }
    var wantsToSetCustomZap by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var zappingProgress by remember { mutableStateOf(0f) }

    var wasZappedByLoggedInUser by remember { mutableStateOf(false) }
    var zapAmountTxt by remember { mutableStateOf<String>("") }

    LaunchedEffect(key1 = zapsState) {
        scope.launch(Dispatchers.IO) {
            if (!wasZappedByLoggedInUser) {
                val newWasZapped = accountViewModel.calculateIfNoteWasZappedByAccount(zappedNote)

                if (wasZappedByLoggedInUser != newWasZapped) {
                    wasZappedByLoggedInUser = newWasZapped
                }
            }

            val newZapAmount = showAmount(account.calculateZappedAmount(zappedNote))
            if (newZapAmount != zapAmountTxt) {
                zapAmountTxt = newZapAmount
            }

            if (wasZappedByLoggedInUser) {
                if (abs(zappingProgress - 1) < 0.001) {
                    zappingProgress = 1f
                }
            }
        }
    }

    Row(
        verticalAlignment = CenterVertically,
        modifier = Modifier
            .size(iconSize)
            .combinedClickable(
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = false, radius = 24.dp),
                onClick = {
                    if (account.zapAmountChoices.isEmpty()) {
                        scope.launch {
                            Toast
                                .makeText(
                                    context,
                                    context.getString(R.string.no_zap_amount_setup_long_press_to_change),
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        }
                    } else if (!accountViewModel.isWriteable()) {
                        scope.launch {
                            Toast
                                .makeText(
                                    context,
                                    context.getString(R.string.login_with_a_private_key_to_be_able_to_send_zaps),
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        }
                    } else if (account.zapAmountChoices.size == 1) {
                        scope.launch(Dispatchers.IO) {
                            accountViewModel.zap(
                                baseNote,
                                account.zapAmountChoices.first() * 1000,
                                null,
                                "",
                                context,
                                onError = {
                                    scope.launch {
                                        zappingProgress = 0f
                                        Toast
                                            .makeText(context, it, Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                },
                                onProgress = {
                                    scope.launch(Dispatchers.Main) {
                                        zappingProgress = it
                                    }
                                },
                                zapType = account.defaultZapType
                            )
                        }
                    } else if (account.zapAmountChoices.size > 1) {
                        wantsToZap = true
                    }
                },
                onLongClick = {
                    wantsToChangeZapAmount = true
                },
                onDoubleClick = {
                    wantsToSetCustomZap = true
                }
            )
    ) {
        if (wantsToZap) {
            ZapAmountChoicePopup(
                baseNote,
                accountViewModel,
                onDismiss = {
                    wantsToZap = false
                    zappingProgress = 0f
                },
                onChangeAmount = {
                    wantsToZap = false
                    wantsToChangeZapAmount = true
                },
                onError = {
                    scope.launch {
                        zappingProgress = 0f
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }
                },
                onProgress = {
                    scope.launch(Dispatchers.Main) {
                        zappingProgress = it
                    }
                }
            )
        }
        if (wantsToChangeZapAmount) {
            UpdateZapAmountDialog({ wantsToChangeZapAmount = false }, account = account)
        }

        if (wantsToSetCustomZap) {
            ZapCustomDialog({ wantsToSetCustomZap = false }, account = account, accountViewModel, baseNote)
        }

        if (wasZappedByLoggedInUser) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = stringResource(R.string.zaps),
                modifier = Modifier.size(iconSize),
                tint = BitcoinOrange
            )
        } else {
            if (zappingProgress < 0.1 || zappingProgress > 0.99) {
                Icon(
                    imageVector = Icons.Outlined.Bolt,
                    contentDescription = stringResource(id = R.string.zaps),
                    modifier = Modifier.size(iconSize),
                    tint = grayTint
                )
            } else {
                Spacer(Modifier.width(3.dp))
                CircularProgressIndicator(
                    progress = zappingProgress,
                    modifier = Modifier.size(animationSize),
                    strokeWidth = 2.dp
                )
            }
        }
    }

    Text(
        zapAmountTxt,
        fontSize = 14.sp,
        color = grayTint,
        modifier = textModifier
    )
}

@Composable
public fun ViewCountReaction(
    idHex: String,
    grayTint: Color,
    iconSize: Dp = 20.dp,
    barChartSize: Dp = 19.dp,
    numberSize: Dp = 24.dp
) {
    val uri = LocalUriHandler.current
    val context = LocalContext.current

    val iconButtonModifier = remember {
        Modifier.size(barChartSize)
    }

    val iconModifier = remember {
        Modifier.height(numberSize)
    }

    val colorFilter = remember {
        ColorFilter.tint(grayTint)
    }

    IconButton(
        modifier = Modifier.size(iconSize),
        onClick = { uri.openUri("https://counter.amethyst.social/$idHex/") }
    ) {
        Icon(
            imageVector = Icons.Outlined.BarChart,
            null,
            modifier = iconButtonModifier,
            tint = grayTint
        )
    }

    val request = remember {
        ImageRequest.Builder(context)
            .data("https://counter.amethyst.social/$idHex.svg?label=+&color=00000000")
            .diskCachePolicy(CachePolicy.DISABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    Row {
        AsyncImage(
            model = request,
            contentDescription = stringResource(R.string.view_count),
            modifier = iconModifier,
            colorFilter = colorFilter
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoostTypeChoicePopup(baseNote: Note, accountViewModel: AccountViewModel, onDismiss: () -> Unit, onQuote: () -> Unit) {
    Popup(
        alignment = Alignment.BottomCenter,
        offset = IntOffset(0, -50),
        onDismissRequest = { onDismiss() }
    ) {
        FlowRow() {
            Button(
                modifier = Modifier.padding(horizontal = 3.dp),
                onClick = {
                    accountViewModel.boost(baseNote)
                    onDismiss()
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults
                    .buttonColors(
                        backgroundColor = MaterialTheme.colors.primary
                    )
            ) {
                Text(stringResource(R.string.boost), color = Color.White, textAlign = TextAlign.Center)
            }

            Button(
                modifier = Modifier.padding(horizontal = 3.dp),
                onClick = onQuote,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults
                    .buttonColors(
                        backgroundColor = MaterialTheme.colors.primary
                    )
            ) {
                Text(stringResource(R.string.quote), color = Color.White, textAlign = TextAlign.Center)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ZapAmountChoicePopup(
    baseNote: Note,
    accountViewModel: AccountViewModel,
    onDismiss: () -> Unit,
    onChangeAmount: () -> Unit,
    onError: (text: String) -> Unit,
    onProgress: (percent: Float) -> Unit
) {
    val context = LocalContext.current

    val accountState by accountViewModel.accountLiveData.observeAsState()
    val account = accountState?.account ?: return
    val zapMessage = ""
    val scope = rememberCoroutineScope()

    Popup(
        alignment = Alignment.BottomCenter,
        offset = IntOffset(0, -50),
        onDismissRequest = { onDismiss() }
    ) {
        FlowRow(horizontalArrangement = Arrangement.Center) {
            account.zapAmountChoices.forEach { amountInSats ->
                Button(
                    modifier = Modifier.padding(horizontal = 3.dp),
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            accountViewModel.zap(
                                baseNote,
                                amountInSats * 1000,
                                null,
                                zapMessage,
                                context,
                                onError,
                                onProgress,
                                account.defaultZapType
                            )
                            onDismiss()
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults
                        .buttonColors(
                            backgroundColor = MaterialTheme.colors.primary
                        )
                ) {
                    Text(
                        "⚡ ${showAmount(amountInSats.toBigDecimal().setScale(1))}",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    accountViewModel.zap(
                                        baseNote,
                                        amountInSats * 1000,
                                        null,
                                        zapMessage,
                                        context,
                                        onError,
                                        onProgress,
                                        account.defaultZapType
                                    )
                                    onDismiss()
                                }
                            },
                            onLongClick = {
                                onChangeAmount()
                            }
                        )
                    )
                }
            }
        }
    }
}

fun showCount(count: Int?): String {
    if (count == null) return ""
    if (count == 0) return ""

    return when {
        count >= 1000000000 -> "${(count / 1000000000f).roundToInt()}G"
        count >= 1000000 -> "${(count / 1000000f).roundToInt()}M"
        count >= 1000 -> "${(count / 1000f).roundToInt()}k"
        else -> "$count"
    }
}

val OneGiga = BigDecimal(1000000000)
val OneMega = BigDecimal(1000000)
val OneKilo = BigDecimal(1000)

fun showAmount(amount: BigDecimal?): String {
    if (amount == null) return ""
    if (amount.abs() < BigDecimal(0.01)) return ""

    return when {
        amount >= OneGiga -> "%.1fG".format(amount.div(OneGiga).setScale(1, RoundingMode.HALF_UP))
        amount >= OneMega -> "%.1fM".format(amount.div(OneMega).setScale(1, RoundingMode.HALF_UP))
        amount >= OneKilo -> "%.1fk".format(amount.div(OneKilo).setScale(1, RoundingMode.HALF_UP))
        else -> "%.0f".format(amount)
    }
}
