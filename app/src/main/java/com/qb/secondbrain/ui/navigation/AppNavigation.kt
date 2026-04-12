package com.qb.secondbrain.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.qb.secondbrain.ui.screen.MemoDetailScreen
import com.qb.secondbrain.ui.screen.MemoEditScreen
import com.qb.secondbrain.ui.screen.MemoListScreen
import com.qb.secondbrain.ui.screen.SearchScreen
import com.qb.secondbrain.ui.screen.SettingsScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Route.MemoList) {
        composable<Route.MemoList> {
            MemoListScreen(
                onMemoClick = { navController.navigate(Route.MemoDetail(it)) },
                onAddMemo = { navController.navigate(Route.MemoEdit()) },
                onSearchClick = { navController.navigate(Route.Search()) },
                onSettingsClick = { navController.navigate(Route.Settings) }
            )
        }
        composable<Route.MemoDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.MemoDetail>()
            MemoDetailScreen(
                memoId = route.id,
                onEditClick = { navController.navigate(Route.MemoEdit(it)) },
                onDeleteClick = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable<Route.MemoEdit> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.MemoEdit>()
            MemoEditScreen(
                memoId = if (route.id == -1L) null else route.id,
                onSaveComplete = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable<Route.Search> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Search>()
            SearchScreen(
                initialQuery = route.query.ifBlank { null },
                onMemoClick = { navController.navigate(Route.MemoDetail(it)) },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable<Route.Settings> {
            SettingsScreen(onBackClick = { navController.popBackStack() })
        }
    }
}
