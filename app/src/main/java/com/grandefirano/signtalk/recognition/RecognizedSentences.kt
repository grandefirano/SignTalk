package com.grandefirano.signtalk.recognition

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grandefirano.signtalk.R
import com.grandefirano.signtalk.camera.CameraViewModel
import com.grandefirano.signtalk.ui.theme.SignTalkTheme

@Composable
fun RecognizedSentences(viewModel: CameraViewModel = hiltViewModel()) {
    val recognizedSentences by viewModel.recognizedActionSentences.collectAsStateWithLifecycle()
    //val translationChoice by viewModel.translationChoice.collectAsStateWithLifecycle()
    val onTranslationChange: (TranslationChoice) -> Unit = {}//viewModel::switchTranslation
    RecognizedSentencesContent(
        recognizedSentences = recognizedSentences,
        //translationChoice = translationChoice,
        //onTranslationChange = onTranslationChange
    )
}

@Composable
fun RecognizedSentencesContent(
    recognizedSentences: List<String>,
    //translationChoice: TranslationChoice,
    //onTranslationChange: (TranslationChoice) -> Unit
) {
    val listState = rememberLazyListState(0)
    LaunchedEffect(recognizedSentences.size) {
        println("EEEEE ${recognizedSentences.lastIndex}")
        if (recognizedSentences.lastIndex > -1) {
            listState.animateScrollToItem(recognizedSentences.lastIndex)
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally,modifier = Modifier.fillMaxWidth()) {
        //TODO switch on when switching language is wanted
        //LanguageSwitch(translationChoice, onTranslationChange)
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(recognizedSentences) { index, sentence ->
                val fontSize = if (index == recognizedSentences.lastIndex) 30.sp else 20.sp
                Text(text = sentence, fontSize = fontSize)
            }
            item {
                DotsTyping()
                //Text(text = "...",color = Color.Gray)
            }
        }
    }
}

@Composable
fun LanguageSwitch(
    translationChoice: TranslationChoice,
    onTranslationChange: (TranslationChoice) -> Unit
) {
    println("NEWWW TRANSLATION CHOSEEEN $translationChoice")
    Row (verticalAlignment = Alignment.CenterVertically) {
        Text(text = "PJM")
//        Switch(
//            checked = translationChoice == TranslationChoice.ASL_ENGLISH,
//            onCheckedChange = {
//                val newValue = when (translationChoice) {
//                    TranslationChoice.PJM_POLISH -> TranslationChoice.ASL_ENGLISH
//                    TranslationChoice.ASL_ENGLISH -> TranslationChoice.PJM_POLISH
//                }
//                onTranslationChange(newValue)
//            }
//        )
        Text(text = "ASL")
    }
}


@Composable
fun DotsTyping() {
    val numberOfDots = 3
    val dotSize = 5.dp
    val dotColor: Color = colorResource(id = R.color.colorPrimary)
    val delayUnit = 200
    val duration = numberOfDots * delayUnit
    val spaceBetween = 2.dp
    val maxOffset = (numberOfDots * 2).toFloat()

    @Composable
    fun Dot(offset: Float) {
        Spacer(
            Modifier
                .size(dotSize)
                .offset(y = -offset.dp)
                .background(
                    color = dotColor,
                    shape = CircleShape
                )
        )
    }

    val infiniteTransition = rememberInfiniteTransition()

    @Composable
    fun animateOffsetWithDelay(delay: Int) = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = keyframes {
            durationMillis = duration
            0f at delay with LinearEasing
            maxOffset at delay + delayUnit with LinearEasing
            0f at delay + (duration / 2)
        })
    )

    val offsets = arrayListOf<State<Float>>()
    for (i in 0 until numberOfDots) {
        offsets.add(animateOffsetWithDelay(delay = i * delayUnit))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(top = maxOffset.dp)
            .padding(top = 8.dp, bottom = 20.dp)
    ) {
        offsets.forEach {
            Dot(it.value)
            Spacer(Modifier.width(spaceBetween))
        }
    }
}

@Preview
@Composable
fun RecognizedSentencesPreview() {
    val recognizedSentences = listOf("Dziękuję", "Przepraszam", "Dobranoc")
    SignTalkTheme {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background
        ) {
            RecognizedSentencesContent(
                recognizedSentences = recognizedSentences,
                //translationChoice = TranslationChoice.ASL_ENGLISH,
                //onTranslationChange = {}
            )
        }
    }
}