package com.shiguang.moments.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.shiguang.moments.ui.AppViewModel
import com.shiguang.moments.ui.components.EmptyState
import com.shiguang.moments.ui.components.LocalImage
import java.io.File

/** 相册墙：每个图独立一块（多图瞬间会被拆成多块） */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(nav: NavHostController, vm: AppViewModel, modifier: Modifier = Modifier) {
    val moments by vm.moments.collectAsStateWithLifecycle()
    // 把每个瞬间拆成 (momentId, path) 二元组，便于点击跳转到对应瞬间详情
    val tiles = remember(moments) {
        moments.flatMap { m ->
            m.allImagePaths().map { path -> m.id to path }
        }
    }
    if (tiles.isEmpty()) {
        EmptyState(Icons.Filled.PhotoLibrary, "相册墙还空着",
            "在随手记里加上图，相册会一件件亮起来", modifier, emoji = "🖼️")
    } else {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalItemSpacing = 10.dp,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
        ) {
            items(tiles, key = { (mid, path) -> "$mid-$path" }) { (mid, path) ->
                LocalImage(
                    data = File(path), contentDescription = "图片瞬间",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(if (path.hashCode() % 3 == 0) 3f / 4f else 1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { nav.navigate("moment/$mid") },
                )
            }
        }
    }
}