package com.odom.mahjongfun.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** 마일스톤 1 뼈대용 임시 화면. 이후 마일스톤에서 실제 화면으로 교체한다. */
@Composable
fun PlaceholderScreen(
    title: String,
    actions: List<Pair<String, () -> Unit>> = emptyList(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))
        actions.forEach { (label, onClick) ->
            Button(
                onClick = onClick,
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) { Text(label, style = MaterialTheme.typography.labelLarge) }
            Spacer(Modifier.height(12.dp))
        }
    }
}
