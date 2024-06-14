package com.example.verticalscrolleffect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.verticalscrolleffect.ui.theme.VerticalScrollEffectTheme
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.pow

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VerticalScrollEffectTheme {
                Scaffold(modifier = Modifier.fillMaxSize(),
                ) { innerPadding ->
                    ScrollEffectDemo(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

data class Item(val iconId: ImageVector, val displayText: String, val bgColor: Color)

@Composable
fun Dp.toPx(): Float {
    return with(LocalDensity.current) { this@toPx.toPx() }
}

fun getData(): List<Item> {
    // Assuming you have these drawable resources in your project
    val item1 = Item(Icons.Filled.Home, displayText = "COMPETITION", bgColor = Color.LightGray)
    val item2 = Item(Icons.Filled.Favorite, displayText = "ON THE MAP", bgColor = Color(0xFF2DC7FF))
    val item4 = Item(Icons.Filled.Info, displayText = "APPRECIATED", bgColor = Color.LightGray)
    val item3 = Item(Icons.Filled.Settings, displayText = "SALES NEARBY", bgColor = Color(0xFF007fff))

    val item5 = Item(Icons.Filled.AccountCircle, displayText = "NEW MOVIES", bgColor = Color.Gray)
    val item6 = Item(Icons.Filled.DateRange, displayText = "GALLERY", bgColor = Color.Blue)

// List of items
    return listOf(item1, item2, item3, item4, item5,item6)
}

@Composable
fun ScrollEffectDemo(modifier: Modifier) {
    val data = getData()
    ScrollEffect(modifier = modifier) {
        for (i in 0 until data.size) {
            ListItem(data[i])
        }
    }
}

@Composable
fun ScrollEffect( modifier: Modifier,content: @Composable () -> Unit) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var draggingItemIndex by remember { mutableStateOf(-1) }
    val itemHeight = 200.dp.toPx()
    val dragAmount = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(scrollState)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        draggingItemIndex = (offset.y / itemHeight).toInt()
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            dragAmount.animateTo(0f, tween(200, easing = EaseInOut))
                            draggingItemIndex = -1
                        }
                    }
                ) { change, dragAmountDelta ->

                    coroutineScope.launch {
                        val currentOffset = scrollState.value
                        val maxOffset = scrollState.maxValue

                        //Prevent Scrolling Top
                        if (currentOffset == 0 && dragAmountDelta > 0) {
                            return@launch
                        }
                        //Prevent Scrolling Bottom
                        if (currentOffset == maxOffset && dragAmountDelta < 0) {
                            return@launch
                        }

                        scrollState.scrollBy(-dragAmountDelta)

                        val clampedDragAmountDelta = dragAmountDelta.coerceIn(-itemHeight, itemHeight)*.5f

                        //When Scrolled Down
                        if (dragAmountDelta > 0) {
                            dragAmount.snapTo((dragAmount.value - clampedDragAmountDelta))
                        }
                        //When Scrolled Up
                        else if (dragAmountDelta < 0) {
                            dragAmount.snapTo(dragAmount.value+ clampedDragAmountDelta)
                        }
                    }
                }
            }
    ) {
        Layout(
            content = content,
            modifier = Modifier.fillMaxWidth()
        ) { measurables, constraints ->
            val placeables = measurables.map { measurable ->
                measurable.measure(constraints)
            }

            val totalHeight = placeables.sumOf { it.height }

            layout(constraints.maxWidth, totalHeight) {
                var yPosition = 0

                placeables.forEachIndexed { index, placeable ->

                    val dragVal = dragAmount.value

                    //Calculate the new position based on the drag value and the gaussian factor,
                    // where the dragged index has highest factor and decreasing factor along its adjacent elements
                    val newPostion = (yPosition + (dragVal * getFactor(index, draggingItemIndex)))

                    placeable.placeRelative(0, newPostion.toInt())

                    yPosition = newPostion.toInt() + placeable.height
                }

            }
        }
    }

}


fun getFactor(index: Int, draggedIndex: Int): Float {
    val distance = Math.abs(index - draggedIndex).toFloat()
    val sigma = 1f // Adjust the sigma for the width of the bell curve
    val max = .9f

    // Gaussian function
    val bellCurve = exp((-distance.pow(2)) / (2 * sigma.pow(2)))

    // Scale the bell curve to be in the range 0f to 0.8f
    val scaledValue = bellCurve * max

    // Ensure the value is within the desired range
    return if (scaledValue > max) max else scaledValue.toFloat()
}


@Composable
fun ListItem(i: Item) {

    val clickState = remember { mutableStateOf(false) }

    val targetHeight = if (clickState.value) 400.dp else 200.dp
    val height by animateDpAsState(targetValue = targetHeight)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(i.bgColor)
            .clickable {
                clickState.value = !clickState.value
            },
        contentAlignment = Alignment.Center
    ) {
        if (clickState.value)
            getExpandedState(i)
        else
            getShrinkState(i)

    }
}

@Composable
fun getExpandedState(data: Item) {

    val animatableOffset = remember { Animatable(initialValue = -600f) }

    LaunchedEffect(Unit) {
        animatableOffset.animateTo(
            targetValue = 10f,
            animationSpec = tween(durationMillis = 400, easing = LinearEasing)
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background image
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )

        // Text entering from left
        Text(
            text = "There are no activities",
            color = Color.White,
            fontSize = 27.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = animatableOffset.value.dp, y = 16.dp)
        )
    }
}

@Composable
fun getShrinkState(data: Item) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Icon(
            modifier = Modifier.size(35.dp),
            imageVector = data.iconId,
            contentDescription = null // Provide a description for accessibility
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(data.displayText, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
    }

}

