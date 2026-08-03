package com.eyecare.app.presentation.eyewear

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private enum class EyewearTab(val label: String, val index: Int) {
    ESTIMATES("Estimates", 0),
    ORDERS("Orders", 1),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyEyewearScreen(
    onBack: () -> Unit,
    onNavigateToEstimate: (Int) -> Unit,
    onNavigateToOrder: (Int) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(EyewearTab.ESTIMATES.index) }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text("My Eyewear", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(padding),
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                EyewearTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab.index,
                        onClick = { selectedTab = tab.index },
                        text = { Text(tab.label) },
                    )
                }
            }

            when (selectedTab) {
                EyewearTab.ESTIMATES.index -> {
                    val vm: EstimateListViewModel = hiltViewModel()
                    val uiState by vm.uiState.collectAsStateWithLifecycle()
                    EstimateListContent(
                        uiState = uiState,
                        onSelectFilter = vm::selectFilter,
                        onRetry = vm::retry,
                        onLoadMore = vm::loadMore,
                        onNavigateToEstimate = onNavigateToEstimate,
                        onNavigateToOrder = onNavigateToOrder,
                    )
                }
                EyewearTab.ORDERS.index -> {
                    val vm: OpticalOrderListViewModel = hiltViewModel()
                    val uiState by vm.uiState.collectAsStateWithLifecycle()
                    OpticalOrderListContent(
                        uiState = uiState,
                        onSelectFilter = vm::selectFilter,
                        onRetry = vm::retry,
                        onLoadMore = vm::loadMore,
                        onNavigateToOrder = onNavigateToOrder,
                        onNavigateToEstimate = onNavigateToEstimate,
                    )
                }
            }
        }
    }
}
