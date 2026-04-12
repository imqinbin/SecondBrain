package com.qb.secondbrain.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun MemoListScreen(
    onMemoClick: (Long) -> Unit = {},
    onAddMemo: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("备忘录列表")
    }
}

@Composable
fun MemoDetailScreen(
    memoId: Long,
    onEditClick: (Long) -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("备忘录详情 #$memoId")
    }
}

@Composable
fun MemoEditScreen(
    memoId: Long?,
    onSaveComplete: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(if (memoId != null) "编辑备忘录 #$memoId" else "新建备忘录")
    }
}

@Composable
fun SearchScreen(
    initialQuery: String?,
    onMemoClick: (Long) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("搜索")
    }
}

@Composable
fun SettingsScreen(onBackClick: () -> Unit = {}) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("设置")
    }
}
