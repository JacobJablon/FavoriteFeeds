package com.example.favoritefeeds

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.favoritefeeds.ui.theme.FavoriteFeedsTheme
import com.example.favoritefeeds.ui.theme.lightOrange
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val datastore = FavoriteFeedsPreferences(context)

            //hoisted to here
            var listItems: SnapshotStateList<FeedData> =
                runBlocking {
                    val tempList = datastore.readAllFeeds()
                    return@runBlocking tempList
                }

            FavoriteFeedsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ){
                    Feeds(listItems = listItems)
                }
            }
        }
    }
}//Activity

@Composable
fun Feeds(listItems: SnapshotStateList<FeedData>) {

    var  feedPathState by remember { mutableStateOf(value = "")}
    var  tagState by remember { mutableStateOf(value = "")}

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataStore = FavoriteFeedsPreferences(context)

//    var listItems = remember {
//        mutableStateListOf(
//            //placeholders for now
//            FeedData(path = "cnn_tech.rss", tag = "Tech"),
//            FeedData(path = "cnn_topstories.rss", tag = "Top"),
//            FeedData(path = "cnn_world.rss", tag = "World"),
//        )
//    }

    listItems.sortWith(compareBy ( String.CASE_INSENSITIVE_ORDER, { it.tag }))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {

        TextField(
            value = feedPathState,
            onValueChange = {
                feedPathState = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = "Feed Path") },
            placeholder = { Text(text = stringResource(id = R.string.queryPrompt)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            maxLines = 1,
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.primary,
                fontSize = 18.sp
            )

        )//TextField

        Spacer(modifier = Modifier.height(8.dp))

        TagRow(tag = tagState, updateTag = { newTag -> tagState = newTag },
            feedPath = feedPathState, updateFeedPath = { newPath -> feedPathState = newPath},
            listItems = listItems
            )

        Spacer(modifier = Modifier.height(8.dp))

        Column(modifier = Modifier.weight(1f)) { // weight so will fill the remaining space
            Box(
                modifier = Modifier
                    .background(lightOrange)
                    .padding(8.dp)
            ) {
                FeedsRow(
                    listItems = listItems,
                    updateFeedPath = { newPath -> feedPathState = newPath},
                    updateTag = { newTag -> tagState = newTag}
                )
            }//box
        }//feeds column

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                listItems.removeAll(listItems)
                scope.launch {
                    dataStore.readAllFeeds()
                }
            },
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = stringResource(id = R.string.clearTags),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
            )
        }//Button

    }//Main Column

}//Feeds



@Composable
fun TagRow(tag: String, updateTag: (String)->Unit,
           feedPath: String, updateFeedPath: (String)->Unit,
           listItems: SnapshotStateList<FeedData>
) {

    val context = LocalContext.current //needed for dialog box
    val focusManager = LocalFocusManager.current //need to dismiss the keyboard
    val scope = rememberCoroutineScope()
    val dataStore = FavoriteFeedsPreferences(context)

    Row(
        modifier = Modifier
            .height(IntrinsicSize.Max), //so we can allign the text on the buttons
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            modifier = Modifier.weight(1f),
            value = tag,
            onValueChange = {
                updateTag(it)
            },
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            ),
            label = {
                Text(text = "Feed Tag")
            },
            placeholder = {
                Text(text = stringResource(id = R.string.tagPrompt))
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            maxLines = 1
        )

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = {

                if (tag.isNotEmpty() && feedPath.isNotEmpty()) {
                    val newFeed =
                        FeedData(path = feedPath, tag = tag) //need so can check for no changes
                    if (!listItems.contains(newFeed)) {
                        //is a new item
                        listItems.add(newFeed)
                        Log.d("JRJ", "New Feed Added")
                        scope.launch {
                            dataStore.setFeed(newFeed)
                        }
                        listItems.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER, { it.tag }))
                        Log.d("JRJ", listItems.joinToString())
                    }
                    //clear fields
                    updateFeedPath("")
                    updateTag("")
                    focusManager.clearFocus() //dismiss keyboard
                } else { //display a message asking to provide a path and tag
                    val builder = AlertDialog.Builder(context)
                    builder.setTitle(R.string.missingTitle) //title bar
                    builder.setPositiveButton(R.string.OK, null) //ok button to dismiss dialog
                    builder.setMessage(R.string.missingMessage) //body of dialog box
                    val errorDialog = builder.create()
                    errorDialog.show()

                }//else
            },
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = stringResource(id = R.string.save),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxHeight()
                    .wrapContentHeight(Alignment.CenterVertically)
                )
        }//Button
    }//Row

}//TagRow

data class FeedData(val path: String, val tag: String)

@SuppressLint("UnrememberedMutableState")
@Composable
fun FeedsRow(listItems: SnapshotStateList<FeedData>,
             updateFeedPath: (String)->Unit, updateTag: (String)->Unit) {

    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {

        itemsIndexed(
            items = listItems,
            key = {_, listItem -> listItem.hashCode()}
        ) { index, item ->

            FeedItemRow(feedItem = item, updateFeedPath, updateTag)

        }//itemsIndexed

        item {
            if (listItems.count() == 0) {
                //probably move to another composable
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "No Feeds To Display", //shoould be a string resource
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.headlineSmall
                    )
                } //Column
            }//empty list
        }//item

    }//LazyColumn

}//Feeds Row

@Composable
fun FeedItemRow(
    feedItem: FeedData,
    updateFeedPath: (String)->Unit,
    updateTag: (String)->Unit
) {

    val context = LocalContext.current
    val baseUrl = stringResource(id = R.string.searchURL)

    Row(
        modifier = Modifier
            .height(IntrinsicSize.Max), //so we can align the text on the button
        verticalAlignment = Alignment.CenterVertically
    ) {

        Button(
            onClick = {
                //create the url for the path corresponding to the button/item
                val urlString = baseUrl + feedItem.path

                //create the intent to launch the web browser/activity
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(urlString))
                context.startActivity(webIntent)
            },
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.weight(1f) //take up remaining space
        ) {
            Text(
                text = feedItem.tag,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxHeight()
                    .wrapContentHeight(Alignment.CenterVertically)
            )
        }//tag button

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = {
                updateFeedPath(feedItem.path)
                updateTag(feedItem.tag)
            },
            shape = RoundedCornerShape(4.dp),
        ) {
            Text(
                text = stringResource(id = R.string.edit),
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxHeight()
                    .wrapContentHeight(Alignment.CenterVertically)
            )
        }//tag button

    }//Row

}//FeedItemRow

@Preview(showBackground = true)
@Composable
fun FeedsPreview() {
    FavoriteFeedsTheme {
        var listItems = remember {
        mutableStateListOf(
            //placeholders for now
            FeedData(path = "cnn_tech.rss", tag = "Tech"),
            FeedData(path = "cnn_topstories.rss", tag = "Top"),
            FeedData(path = "cnn_world.rss", tag = "World"),
        )
    }
        Feeds(listItems = listItems)
    }
}









