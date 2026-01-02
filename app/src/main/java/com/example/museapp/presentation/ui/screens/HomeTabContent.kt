package com.example.museapp.presentation.feature.home

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import com.example.museapp.presentation.ui.components.UserCardComposable
import com.example.museapp.presentation.ui.components.appTextFieldColors
import com.example.museapp.ui.theme.AppTypography
import com.example.museapp.ui.theme.PrimaryColor
import com.example.museapp.util.AppConstants
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.material.icons.Icons
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.museapp.presentation.feature.favorites.FavoritesEvent
import com.example.museapp.presentation.feature.favorites.FavoritesViewModel
import com.example.museapp.presentation.feature.home.HomeUiEffect
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeTabContent(
    homeViewModel: HomeViewModel = hiltViewModel(),
    // accept a shared favoritesViewModel from parent; default to hiltViewModel() for backward compatibility
    favoritesViewModel: FavoritesViewModel = hiltViewModel(),
    onNavigateToDetails: (String) -> Unit = {},
    onProfileClicked: () -> Unit = {},
    onCreateAdClicked: () -> Unit = {}
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val usersState by homeViewModel.usersState.collectAsState()

    // use the passed/shared favorites view model
    val favUiState by favoritesViewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val pageBg = Color.White
    val density = LocalDensity.current

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val headerMax = 56.dp
    val collapsePx = with(density) { headerMax.toPx() }
    val collapseProgress by remember {
        derivedStateOf {
            val idx = listState.firstVisibleItemIndex
            val off = listState.firstVisibleItemScrollOffset.toFloat()
            (if (idx > 0) 1f else (off / collapsePx)).coerceIn(0f, 1f)
        }
    }
    val anim by animateFloatAsState(targetValue = collapseProgress, animationSpec = tween(180))
    val headerHeight = with(density) { ((1f - anim) * collapsePx).toDp() }
    val stickyTopPad = (topInset - headerHeight).coerceAtLeast(0.dp)

    // collect effects from both homeViewModel and favoritesViewModel and show them on the same SnackbarHost
    LaunchedEffect(Unit) {
        // home view model effects
        launch {
            homeViewModel.effects.collectLatest {
                when (it) {
                    is HomeUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(it.message)
                    is HomeUiEffect.ShowError -> snackbarHostState.showSnackbar(it.message)
                    is HomeUiEffect.NavigateToDetails -> onNavigateToDetails(it.id)
                    else -> {}
                }
            }
        }
        // favorites view model effects
        launch {
            favoritesViewModel.effects.collectLatest { eff ->
                when (eff) {
                    is HomeUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(eff.message)
                    is HomeUiEffect.ShowError -> snackbarHostState.showSnackbar(eff.message)
                    is HomeUiEffect.NavigateToDetails -> onNavigateToDetails(eff.id)
                    else -> {}
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = pageBg,
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            val topPadFromScaffold = innerPadding.calculateTopPadding()
            val bottomPadFromScaffold = innerPadding.calculateBottomPadding()

            // combine loading flags so only one overlay loader is shown
            val isAnyLoading = remember(usersState.isLoading, uiState.isLoading, favUiState) {
                val favLoading = runCatching { favUiState.isLoading }.getOrNull() ?: false
                usersState.isLoading || uiState.isLoading || favLoading
            }

            Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = topPadFromScaffold,
                        bottom = bottomPadFromScaffold + 72.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        AnimatedVisibility(
                            visible = headerHeight > 8.dp,
                            enter = fadeIn(tween(180)),
                            exit = fadeOut(tween(120))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(headerHeight + topInset)
                                    .background(PrimaryColor)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = topInset)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Place,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Current Location",
                                                style = AppTypography.labelSmall,
                                                color = Color.White.copy(alpha = 0.9f)
                                            )
                                            Text(
                                                text = uiState.locationLabel.ifBlank { AppConstants.DEFAULT_LOCATION_NAME },
                                                style = AppTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    IconButton(onClick = onProfileClicked) {
                                        Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    stickyHeader {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(pageBg)
                                .zIndex(1f)
                        ) {
                            if (stickyTopPad > 0.dp) {
                                Spacer(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(stickyTopPad)
                                        .background(pageBg)
                                )
                            }

                            Surface(tonalElevation = 0.dp, shadowElevation = 0.dp, color = Color.White) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = uiState.searchQuery,
                                        onValueChange = { homeViewModel.onEvent(HomeEvent.SearchChanged(it)) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp),
                                        placeholder = { Text("Search...", style = AppTypography.titleSmall) },
                                        colors = appTextFieldColors(),
                                        singleLine = true,
                                        trailingIcon = {
                                            IconButton(onClick = { homeViewModel.onEvent(HomeEvent.SearchChanged("")) }) {
                                                Icon(Icons.Filled.Favorite, contentDescription = null)
                                            }
                                        }
                                    )
                                }
                            }

                            Surface(tonalElevation = 0.dp, shadowElevation = 0.dp, color = Color.White) {
                                val dbSkills = remember(uiState.skills) {
                                    uiState.skills
                                        .map { it.trim() }
                                        .filter { it.isNotBlank() && !it.equals("All", true) && !it.equals("Other", true) }
                                        .distinctBy { it.lowercase() }
                                        .sortedBy { it.lowercase() }
                                }

                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item {
                                        ChipSelectable(
                                            text = "All",
                                            selected = uiState.selectedSkill.equals("All", true)
                                        ) { homeViewModel.onEvent(HomeEvent.SkillSelected("All")) }
                                    }

                                    items(items = dbSkills) { skill ->
                                        ChipSelectable(
                                            text = skill,
                                            selected = uiState.selectedSkill.equals(skill, true)
                                        ) { homeViewModel.onEvent(HomeEvent.SkillSelected(skill)) }
                                    }

                                    item {
                                        ChipSelectable(
                                            text = "Other",
                                            selected = uiState.selectedSkill.equals("Other", true)
                                        ) { homeViewModel.onEvent(HomeEvent.SkillSelected("Other")) }
                                    }
                                }
                            }
                        }
                    }

                    // NO loader item inside the list anymore (overlay handles it)

                    if (!usersState.error.isNullOrBlank()) {
                        item {
                            Text(
                                usersState.error ?: "Failed to load users",
                                color = MaterialTheme.colorScheme.error,
                                style = AppTypography.titleSmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        val users = usersState.users
                        if (users.isEmpty() && !isAnyLoading) {
                            item {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("No results", style = AppTypography.titleSmall)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Try changing filters or the search query.", style = AppTypography.bodySmall)
                                }
                            }
                        } else {
                            val favList = favUiState.favorites // List<FavoriteUser>
                            items(items = users, key = { it.user.id }) { userWithDistance ->
                                Box(modifier = Modifier.padding(start = 12.dp, end = 12.dp)) {
                                    val isFavorited = remember(favList, userWithDistance.user.id) {
                                        favList.any { it.favoriteUser.id == userWithDistance.user.id }
                                    }

                                    val favoriteRecordId = remember(favList, userWithDistance.user.id) {
                                        favList.firstOrNull { it.favoriteUser.id == userWithDistance.user.id }?.id
                                    }

                                    // IMPORTANT: pass lambdas but card decides where clicks happen
                                    UserCardComposable(
                                        user = userWithDistance.user,
                                        distanceKm = userWithDistance.distanceKm,
                                        currentLat = uiState.currentLat,
                                        currentLng = uiState.currentLng,
                                        isFavorite = isFavorited,
                                        selectedSkill = uiState.selectedSkill,
                                        knownSkills = uiState.skills,
                                        onFavorite = {
                                            if (isFavorited) {
                                                if (!favoriteRecordId.isNullOrBlank()) {
                                                    favoritesViewModel.onEvent(FavoritesEvent.RemoveFavorite(favoriteRecordId))
                                                } else {
                                                    favoritesViewModel.onEvent(FavoritesEvent.LoadFavorites)
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("Retry removing favorite")
                                                    }
                                                }
                                            } else {
                                                favoritesViewModel.onEvent(FavoritesEvent.AddFavorite(userWithDistance.user.id))
                                            }
                                        },
                                        // only triggers when user taps View Profile (card will not make whole item clickable)
                                        onViewProfile = { onNavigateToDetails(userWithDistance.user.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                // top-level overlay loader using Popup to guarantee it appears above everything
                if (isAnyLoading) {
                    Popup(
                        alignment = Alignment.Center,
                        properties = PopupProperties(focusable = true)
                    ) {
                        // scrim + spinner in Popup (this is drawn on top of Compose content)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x66000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PrimaryColor)
                        }
                    }
                }
            }
        }
    }
}

/**
 * ChipSelectable sanitizes the raw label (removes newlines, soft hyphens and zero-width chars),
 * forces single line with ellipsis and limits max width so chips never wrap to 2 lines.
 */
@Composable
private fun ChipSelectable(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    // aggressively sanitize input to remove newlines and hidden hyphen/zwsp chars that cause wrapping
    val safeText = remember(text) {
        text
            .replace("\u00AD", " ") // soft hyphen
            .replace("\u200B", " ") // zero width space
            .replace("\u200C", " ") // zero width non-joiner
            .replace("\u200D", " ") // zero width joiner
            .replace("\n", " ")
            .replace("\r", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        tonalElevation = 0.dp,
        color = if (selected) PrimaryColor.copy(alpha = 0.14f) else Color.White,
        border = if (!selected) BorderStroke(1.dp, Color.Gray.copy(alpha = 0.4f)) else null,
        modifier = Modifier
            .height(36.dp)
            .padding(horizontal = 2.dp)
            .clickable { onClick() }
            // allow some flexibility but cap width so we don't create multi-line chips
            .widthIn(min = 56.dp, max = 160.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = safeText,
                style = AppTypography.bodyMedium,
                color = if (selected) PrimaryColor else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false
            )
        }
    }
}
