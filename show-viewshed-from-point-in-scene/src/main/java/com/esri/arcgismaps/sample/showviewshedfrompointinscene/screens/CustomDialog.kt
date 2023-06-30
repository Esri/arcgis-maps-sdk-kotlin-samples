//package com.esri.arcgismaps.sample.showviewshedfrompointinscene.screens
//
//import android.util.Log
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.Card
//import androidx.compose.material3.Checkbox
//import androidx.compose.material3.Slider
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.MutableState
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.window.Dialog
//import com.esri.arcgismaps.sample.showviewshedfrompointinscene.components.SceneViewModel
//
//@Composable
//fun CustomDialog(sceneViewModel: SceneViewModel, openDialogCustom: MutableState<Boolean>) {
//    Dialog(onDismissRequest = { openDialogCustom.value = false}) {
//        CustomDialogUI(sceneViewModel)
//    }
//}
//
////Layout
//@Composable
//fun CustomDialogUI(sceneViewModel: SceneViewModel, modifier: Modifier = Modifier){
//
//    Card(
//        //shape = MaterialTheme.shapes.medium,
//        shape = RoundedCornerShape(10.dp),
//        // modifier = modifier.size(280.dp, 240.dp)
//        modifier = Modifier.padding(10.dp,5.dp,10.dp,10.dp),
//    ) {
//        Column(
//            modifier
//                .padding(10.dp)) {
//
//            // sliders
//            HeadingSlider(sceneViewModel)
//            PitchSlider(sceneViewModel)
//            HorizontalAngleSlider(sceneViewModel)
//            VerticalAngleSlider(sceneViewModel)
//            MinimumDistanceSlider(sceneViewModel)
//            MaximumDistanceSlider(sceneViewModel)
//            SimpleCheckboxComponent(sceneViewModel)
//            SimpleCheckboxComponent(sceneViewModel)
//        }
//    }
//}
//
//@Composable
//private fun HeadingSlider(sceneViewModel: SceneViewModel) {
//
//    var sliderValue by remember {
//        mutableStateOf(60f)
//    }
//    Row {
//        Text(text = "Heading")
//        Slider(
//            modifier = Modifier.weight(1f),
//            value = sliderValue,
//            onValueChange = {
//                sliderValue = it
//            },
//            onValueChangeFinished = {
//                // this is called when the user completed selecting the value
//                Log.d("MainActivity", "sliderValue = ${sliderValue.toInt()}")
//                sceneViewModel.setHeading(sliderValue)
//            },
//            valueRange = 0f..360f
//        )
//        Text(text = sliderValue.toInt().toString())
//    }
//}
//
//@Composable
//private fun PitchSlider(sceneViewModel: SceneViewModel) {
//
//    var sliderValue by remember {
//        mutableStateOf(82f)
//    }
//    Row {
//        Text(text = "Pitch")
//        Slider(
//            modifier = Modifier.weight(1f).padding(5.dp),
//            value = sliderValue,
//            onValueChange = {
//                sliderValue = it
//            },
//            onValueChangeFinished = {
//                // this is called when the user completed selecting the value
//                Log.d("MainActivity", "sliderValue = ${sliderValue.toInt()}")
//                sceneViewModel.setPitch(sliderValue)
//
//            },
//            valueRange = 0f..180f
//        )
//        Text(text = sliderValue.toInt().toString())
//    }
//}
//
//@Composable
//private fun HorizontalAngleSlider(sceneViewModel: SceneViewModel) {
//
//    var sliderValue by remember {
//        mutableStateOf(75f)
//    }
//    Row {
//        Text(text = "Horizontal Angle")
//        Slider(
//            modifier = Modifier.weight(1f).padding(5.dp),
//            value = sliderValue,
//            onValueChange = {
//                sliderValue = it
//            },
//            onValueChangeFinished = {
//                // this is called when the user completed selecting the value
//                Log.d("MainActivity", "sliderValue = ${sliderValue.toInt()}")
//                sceneViewModel.setHorizontalAngleSlider(sliderValue)
//
//            },
//            valueRange = 0f..120f
//        )
//        Text(text = sliderValue.toInt().toString())
//    }
//}
//
//@Composable
//private fun VerticalAngleSlider(sceneViewModel: SceneViewModel) {
//
//    var sliderValue by remember {
//        mutableStateOf(90f)
//    }
//    Row {
//        Text(text = "Vertical Angle")
//        Slider(
//            modifier = Modifier.weight(1f).padding(5.dp),
//            value = sliderValue,
//            onValueChange = {
//                sliderValue = it
//            },
//            onValueChangeFinished = {
//                // this is called when the user completed selecting the value
//                Log.d("MainActivity", "sliderValue = ${sliderValue.toInt()}")
//                sceneViewModel.setVerticalAngleSlider(sliderValue)
//
//            },
//            valueRange = 0f..120f
//        )
//        Text(text = sliderValue.toInt().toString())
//    }
//}
//
//@Composable
//private fun MinimumDistanceSlider(sceneViewModel: SceneViewModel) {
//
//    var sliderValue by remember {
//        mutableStateOf(0f)
//    }
//    Row {
//        Text(text = "Minimum Distance")
//        Slider(
//            modifier = Modifier.weight(1f).padding(5.dp),
//            value = sliderValue,
//            onValueChange = {
//                sliderValue = it
//            },
//            onValueChangeFinished = {
//                // this is called when the user completed selecting the value
//                Log.d("MainActivity", "sliderValue = ${sliderValue.toInt()}")
//                sceneViewModel.setMinimumDistanceSlider(sliderValue)
//
//            },
//            valueRange = 0f..8999f
//        )
//        Text(text = sliderValue.toInt().toString())
//    }
//}
//
//@Composable
//private fun MaximumDistanceSlider(sceneViewModel: SceneViewModel) {
//
//    var sliderValue by remember {
//        mutableStateOf(1500f)
//    }
//    Row {
//        Text(text = "Maximum Distance")
//        Slider(
//            modifier = Modifier.weight(1f),
//            value = sliderValue,
//            onValueChange = {
//                sliderValue = it
//            },
//            onValueChangeFinished = {
//                // this is called when the user completed selecting the value
//                Log.d("MainActivity", "sliderValue = ${sliderValue.toInt()}")
//                sceneViewModel.setMaximumDistanceSlider(sliderValue)
//
//            },
//            valueRange = 0f..9999f
//        )
//        Text(text = sliderValue.toInt().toString())
//    }
//}
//
//@Composable
//fun SimpleCheckboxComponent(sceneViewModel: SceneViewModel) {
//    // in below line we are setting
//    // the state of our checkbox.
//    val checkedState = remember { mutableStateOf(true) }
//    // in below line we are displaying a row
//    // and we are creating a checkbox in a row.
//    Row {
//        Checkbox(
//            // below line we are setting
//            // the state of checkbox.
//            checked = checkedState.value,
//            // below line is use to add padding
//            // to our checkbox.
//            modifier = Modifier.padding(16.dp),
//            // below line is use to add on check
//            // change to our checkbox.
//            onCheckedChange = { checkedState.value = it },
//        )
//        // below line is use to add text to our check box and we are
//        // adding padding to our text of checkbox
//        Text(text = "Checkbox Example", modifier = Modifier.padding(16.dp))
//    }
//}
