package com.vitorpamplona.amethyst.ui.note

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.flowlayout.FlowRow
import com.vitorpamplona.amethyst.NotificationCache
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.model.LocalCache
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.model.User
import com.vitorpamplona.amethyst.service.model.LnZapEvent
import com.vitorpamplona.amethyst.service.model.LnZapRequestEvent
import com.vitorpamplona.amethyst.ui.components.TranslatableRichTextViewer
import com.vitorpamplona.amethyst.ui.screen.MultiSetCard
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountViewModel
import com.vitorpamplona.amethyst.ui.screen.loggedIn.showAmountAxis
import com.vitorpamplona.amethyst.ui.theme.BitcoinOrange
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MultiSetCompose(multiSetCard: MultiSetCard, routeForLastRead: String, accountViewModel: AccountViewModel, nav: (String) -> Unit) {
    val baseNote = remember { multiSetCard.note }

    val noteState by baseNote.live().metadata.observeAsState()
    val note = remember(noteState) { noteState?.note } ?: return

    val accountState by accountViewModel.accountLiveData.observeAsState()
    val loggedIn = remember(accountState) { accountState?.account?.userProfile() } ?: return

    var popupExpanded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    if (note.event == null) {
        BlankNote(Modifier, false)
    } else {
        var isNew by remember { mutableStateOf(false) }

        LaunchedEffect(key1 = multiSetCard.createdAt()) {
            scope.launch(Dispatchers.IO) {
                val newIsNew = multiSetCard.createdAt > NotificationCache.load(routeForLastRead)

                NotificationCache.markAsRead(routeForLastRead, multiSetCard.createdAt)

                if (newIsNew != isNew) {
                    isNew = newIsNew
                }
            }
        }

        val primaryColor = MaterialTheme.colors.primary.copy(0.12f)
        val defaultBackgroundColor = MaterialTheme.colors.background

        val backgroundColor = if (isNew) {
            primaryColor.compositeOver(defaultBackgroundColor)
        } else {
            defaultBackgroundColor
        }

        val columnModifier = Modifier
            .background(backgroundColor)
            .padding(
                start = 12.dp,
                end = 12.dp,
                top = 10.dp
            )
            .combinedClickable(
                onClick = {
                    scope.launch {
                        routeFor(baseNote, loggedIn)?.let { nav(it) }
                    }
                },
                onLongClick = { popupExpanded = true }
            )
            .fillMaxWidth()

        val zapEvents by remember { derivedStateOf { multiSetCard.zapEvents } }
        val boostEvents by remember { derivedStateOf { multiSetCard.boostEvents } }
        val likeEvents by remember { derivedStateOf { multiSetCard.likeEvents } }

        val hasZapEvents by remember { derivedStateOf { multiSetCard.zapEvents.isNotEmpty() } }
        val hasBoostEvents by remember { derivedStateOf { multiSetCard.boostEvents.isNotEmpty() } }
        val hasLikeEvents by remember { derivedStateOf { multiSetCard.likeEvents.isNotEmpty() } }

        Column(modifier = columnModifier) {
            if (hasZapEvents) {
                RenderZapGallery(zapEvents, backgroundColor, nav, accountViewModel)
            }

            if (hasBoostEvents) {
                RenderBoostGallery(boostEvents, backgroundColor, nav, accountViewModel)
            }

            if (hasLikeEvents) {
                RenderLikeGallery(likeEvents, backgroundColor, nav, accountViewModel)
            }

            Row(Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.width(65.dp))

                NoteCompose(
                    baseNote = baseNote,
                    routeForLastRead = null,
                    modifier = Modifier.padding(top = 5.dp),
                    isBoostedNote = true,
                    parentBackgroundColor = backgroundColor,
                    accountViewModel = accountViewModel,
                    nav = nav
                )

                NoteDropDownMenu(note, popupExpanded, { popupExpanded = false }, accountViewModel)
            }
        }
    }
}

@Composable
private fun RenderLikeGallery(
    likeEvents: ImmutableList<Note>,
    backgroundColor: Color,
    nav: (String) -> Unit,
    accountViewModel: AccountViewModel
) {
    Row(Modifier.fillMaxWidth()) {
        Box(
            modifier = remember {
                Modifier
                    .width(55.dp)
                    .padding(end = 5.dp)
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_liked),
                null,
                modifier = remember {
                    Modifier
                        .size(16.dp)
                        .align(Alignment.TopEnd)
                },
                tint = Color.Unspecified
            )
        }

        AuthorGallery(likeEvents, backgroundColor, nav, accountViewModel)
    }
}

@Composable
private fun RenderZapGallery(
    zapEvents: ImmutableMap<Note, Note>,
    backgroundColor: Color,
    nav: (String) -> Unit,
    accountViewModel: AccountViewModel
) {
    Row(Modifier.fillMaxWidth()) {
        Box(
            modifier = remember {
                Modifier
                    .width(55.dp)
                    .padding(0.dp)
            }
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = "Zaps",
                tint = BitcoinOrange,
                modifier = remember {
                    Modifier
                        .size(25.dp)
                        .align(Alignment.TopEnd)
                }
            )
        }

        AuthorGalleryZaps(zapEvents, backgroundColor, nav, accountViewModel)
    }
}

@Composable
private fun RenderBoostGallery(
    boostEvents: ImmutableList<Note>,
    backgroundColor: Color,
    nav: (String) -> Unit,
    accountViewModel: AccountViewModel
) {
    Row(
        modifier = remember {
            Modifier.fillMaxWidth()
        }
    ) {
        Box(
            modifier = remember {
                Modifier
                    .width(55.dp)
                    .padding(end = 4.dp)
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_retweeted),
                null,
                modifier = remember {
                    Modifier
                        .size(19.dp)
                        .align(Alignment.TopEnd)
                },
                tint = Color.Unspecified
            )
        }

        AuthorGallery(boostEvents, backgroundColor, nav, accountViewModel)
    }
}

@Composable
fun AuthorGalleryZaps(
    authorNotes: ImmutableMap<Note, Note>,
    backgroundColor: Color,
    nav: (String) -> Unit,
    accountViewModel: AccountViewModel
) {
    Column(modifier = Modifier.padding(start = 10.dp)) {
        FlowRow() {
            authorNotes.forEach {
                Box() {
                    AuthorPictureAndComment(it.key, it.value, backgroundColor, nav, accountViewModel)
                }
            }
        }
    }
}

@Composable
private fun AuthorPictureAndComment(
    zapRequest: Note,
    zapEvent: Note?,
    backgroundColor: Color,
    nav: (String) -> Unit,
    accountViewModel: AccountViewModel
) {
    var content by remember { mutableStateOf<Triple<User?, String?, String?>>(Triple(zapRequest.author, null, null)) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = zapRequest.idHex, key2 = zapEvent?.idHex) {
        scope.launch(Dispatchers.Default) {
            (zapRequest.event as? LnZapRequestEvent)?.let {
                val decryptedContent = accountViewModel.decryptZap(zapRequest)
                val amount = (zapEvent?.event as? LnZapEvent)?.amount
                if (decryptedContent != null) {
                    val newAuthor = LocalCache.getOrCreateUser(decryptedContent.pubKey)
                    content = Triple(newAuthor, decryptedContent.content.ifBlank { null }, showAmountAxis(amount))
                } else {
                    if (!zapRequest.event?.content().isNullOrBlank() || amount != null) {
                        content = Triple(zapRequest.author, zapRequest.event?.content()?.ifBlank { null }, showAmountAxis(amount))
                    }
                }
            }
        }
    }

    content.first?.let {
        val route by remember {
            derivedStateOf {
                "User/${it.pubkeyHex}"
            }
        }

        AuthorPictureAndComment(
            author = it,
            comment = content.second,
            amount = content.third,
            route = route,
            backgroundColor = backgroundColor,
            nav = nav,
            accountViewModel = accountViewModel
        )
    }
}

@Composable
private fun AuthorPictureAndComment(
    author: User,
    comment: String?,
    amount: String?,
    route: String,
    backgroundColor: Color,
    nav: (String) -> Unit,
    accountViewModel: AccountViewModel
) {
    val authorPictureModifier = remember { Modifier }

    val modifier = remember(comment) {
        if (comment != null) {
            Modifier
                .fillMaxWidth()
                .clickable {
                    nav(route)
                }
        } else {
            Modifier.clickable {
                nav(route)
            }
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = remember { Modifier.size(35.dp) }, contentAlignment = Alignment.BottomCenter) {
            FastNoteAuthorPicture(
                author = author,
                size = 35.dp,
                accountViewModel = accountViewModel,
                pictureModifier = authorPictureModifier
            )

            amount?.let {
                Box(modifier = Modifier.fillMaxSize().clip(shape = CircleShape), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colors.background.copy(0.62f)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            text = it,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.secondaryVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 1.dp)
                        )
                    }
                }
            }
        }

        comment?.let {
            Spacer(modifier = Modifier.width(5.dp))
            TranslatableRichTextViewer(
                content = it,
                canPreview = true,
                tags = null,
                modifier = Modifier.weight(1f),
                backgroundColor = backgroundColor,
                accountViewModel = accountViewModel,
                nav = nav
            )
        }
    }
}

@Composable
fun AuthorGallery(
    authorNotes: ImmutableList<Note>,
    backgroundColor: Color,
    nav: (String) -> Unit,
    accountViewModel: AccountViewModel
) {
    Column(modifier = Modifier.padding(start = 10.dp)) {
        FlowRow() {
            authorNotes.forEach { note ->
                Box(Modifier.size(35.dp)) {
                    NotePictureAndComment(note, backgroundColor, nav, accountViewModel)
                }
            }
        }
    }
}

@Composable
private fun NotePictureAndComment(
    baseNote: Note,
    backgroundColor: Color,
    nav: (String) -> Unit,
    accountViewModel: AccountViewModel
) {
    val author by remember(baseNote) {
        derivedStateOf {
            baseNote.author
        }
    }

    val route by remember(baseNote) {
        derivedStateOf {
            "User/${baseNote.author?.pubkeyHex}"
        }
    }

    author?.let { AuthorPictureAndComment(it, null, null, route, backgroundColor, nav, accountViewModel) }
}

@Composable
fun FastNoteAuthorPicture(
    author: User,
    size: Dp,
    pictureModifier: Modifier = Modifier,
    accountViewModel: AccountViewModel
) {
    val userState by author.live().metadata.observeAsState()
    val profilePicture = remember(userState) {
        userState?.user?.profilePicture()
    }

    val authorPubKey = remember {
        author.pubkeyHex
    }

    val accountState by accountViewModel.accountLiveData.observeAsState()
    val loggedInLiveFollows = remember(accountState) { accountState?.account?.userProfile()?.live()?.follows } ?: return

    val accountFollowsState by loggedInLiveFollows.observeAsState()

    val showFollowingMark by remember(accountFollowsState) {
        derivedStateOf {
            accountFollowsState?.user?.isFollowingCached(author) == true || (author.pubkeyHex == accountFollowsState?.user?.pubkeyHex)
        }
    }

    UserPicture(
        userHex = authorPubKey,
        userPicture = profilePicture,
        showFollowingMark = showFollowingMark,
        size = size,
        modifier = pictureModifier
    )
}
