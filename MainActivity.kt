package com.studyflow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val Context.dataStore by preferencesDataStore(name = "studyflow_data")
private val TASKS_KEY = stringPreferencesKey("tasks_json")

private val Purple = Color(0xFF6C55D9)
private val PalePurple = Color(0xFFF0EDFF)
private val Ink = Color(0xFF242238)
private val Muted = Color(0xFF85839A)
private val SurfaceBg = Color(0xFFF8F7FC)

@Serializable
data class StudyTask(
    val id: Long,
    val title: String,
    val subject: String,
    val date: String,
    val minutes: Int,
    val done: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StudyFlowApp(this) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyFlowApp(context: Context) {
    val scope = rememberCoroutineScope()
    var tasks by remember { mutableStateOf<List<StudyTask>>(emptyList()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var loaded by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<StudyTask?>(null) }
    var page by remember { mutableStateOf(0) }
    var timerRunning by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableIntStateOf(25 * 60) }
    var timerMode by remember { mutableStateOf("공부") }

    LaunchedEffect(Unit) {
        context.dataStore.data.collect { prefs ->
            val raw = prefs[TASKS_KEY]
            tasks = try { if (raw.isNullOrBlank()) emptyList() else Json.decodeFromString(raw) } catch (_: Exception) { emptyList() }
            loaded = true
        }
    }
    fun persist(newTasks: List<StudyTask>) {
        tasks = newTasks
        scope.launch { context.dataStore.edit { it[TASKS_KEY] = Json.encodeToString(newTasks) } }
    }

    LaunchedEffect(timerRunning) {
        while (timerRunning && timerSeconds > 0) {
            delay(1000)
            timerSeconds -= 1
        }
        if (timerSeconds == 0) timerRunning = false
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = Purple, background = SurfaceBg, surface = Color.White)) {
        Scaffold(
            containerColor = SurfaceBg,
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(selected = page == 0, onClick = { page = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("홈") })
                    NavigationBarItem(selected = page == 1, onClick = { page = 1 }, icon = { Icon(Icons.Default.CalendarMonth, null) }, label = { Text("플래너") })
                    NavigationBarItem(selected = page == 2, onClick = { page = 2 }, icon = { Icon(Icons.Default.Timer, null) }, label = { Text("타이머") })
                }
            },
            floatingActionButton = {
                if (page != 2) FloatingActionButton(
                    onClick = { editing = null; showEditor = true },
                    containerColor = Purple, contentColor = Color.White
                ) { Icon(Icons.Default.Add, contentDescription = "할 일 추가") }
            }
        ) { padding ->
            when (page) {
                0 -> HomePage(
                    tasks = tasks, selectedDate = selectedDate, onDateChange = { selectedDate = it },
                    onToggle = { task -> persist(tasks.map { if (it.id == task.id) it.copy(done = !it.done) else it }) },
                    onEdit = { editing = it; showEditor = true },
                    onDelete = { task -> persist(tasks.filterNot { it.id == task.id }) },
                    onTimer = { page = 2 }, modifier = Modifier.padding(padding)
                )
                1 -> PlannerPage(
                    selectedDate = selectedDate, onDateChange = { selectedDate = it }, tasks = tasks,
                    onToggle = { task -> persist(tasks.map { if (it.id == task.id) it.copy(done = !it.done) else it }) },
                    onEdit = { editing = it; showEditor = true },
                    onDelete = { task -> persist(tasks.filterNot { it.id == task.id }) },
                    modifier = Modifier.padding(padding)
                )
                else -> TimerPage(
                    running = timerRunning, seconds = timerSeconds, mode = timerMode,
                    onStartPause = { timerRunning = !timerRunning },
                    onReset = { timerRunning = false; timerSeconds = if (timerMode == "공부") 25*60 else 5*60 },
                    onMode = { timerMode = it; timerRunning = false; timerSeconds = if (it == "공부") 25*60 else 5*60 },
                    modifier = Modifier.padding(padding)
                )
            }
        }

        if (showEditor) {
            TaskEditorDialog(
                initial = editing, date = selectedDate,
                onDismiss = { showEditor = false },
                onSave = { task ->
                    val updated = if (editing == null) tasks + task else tasks.map { if (it.id == editing!!.id) task else it }
                    persist(updated)
                    selectedDate = LocalDate.parse(task.date)
                    showEditor = false
                }
            )
        }
    }
}

@Composable
fun HomePage(
    tasks: List<StudyTask>, selectedDate: LocalDate, onDateChange: (LocalDate)->Unit,
    onToggle: (StudyTask)->Unit, onEdit: (StudyTask)->Unit, onDelete: (StudyTask)->Unit,
    onTimer: ()->Unit, modifier: Modifier = Modifier
) {
    val todayTasks = tasks.filter { it.date == selectedDate.toString() }
    val done = todayTasks.count { it.done }
    val progress = if (todayTasks.isEmpty()) 0f else done.toFloat()/todayTasks.size
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(top = 24.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("안녕하세요 👋", color = Muted, fontSize = 14.sp)
                    Text("오늘도 차근차근", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
                }
                Surface(shape = CircleShape, color = PalePurple) {
                    Text("SF", color = Purple, fontWeight = FontWeight.Bold, modifier = Modifier.padding(14.dp))
                }
            }
        }
        item { DateSelector(selectedDate, onDateChange) }
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Purple), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("오늘의 학습", color = Color.White.copy(alpha=.8f))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$done", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(" / ${todayTasks.size} 완료", color = Color.White, modifier = Modifier.padding(bottom=6.dp))
                        Spacer(Modifier.weight(1f))
                        Text("${(progress*100).toInt()}%", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp), color = Color.White, trackColor = Color.White.copy(alpha=.25f))
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("오늘 할 일", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.weight(1f))
                Text("${todayTasks.size}개", color = Muted)
            }
        }
        if (todayTasks.isEmpty()) item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("아직 등록된 할 일이 없어", color = Muted)
                    Text("＋ 버튼을 눌러 추가해 보세요", color = Purple, fontSize = 13.sp)
                }
            }
        }
        items(todayTasks, key = { it.id }) { task ->
            TaskCard(task, onToggle, onEdit, onDelete)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth().clickable { onTimer() }, colors = CardDefaults.cardColors(containerColor = PalePurple), shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("집중 타이머", fontWeight = FontWeight.Bold, color = Ink)
                        Text("25분 집중하고 잠깐 쉬기", color = Muted, fontSize = 13.sp)
                    }
                    Icon(Icons.Default.Timer, null, tint = Purple)
                }
            }
        }
    }
}

@Composable
fun PlannerPage(
    selectedDate: LocalDate, onDateChange: (LocalDate)->Unit, tasks: List<StudyTask>,
    onToggle: (StudyTask)->Unit, onEdit: (StudyTask)->Unit, onDelete: (StudyTask)->Unit,
    modifier: Modifier = Modifier
) {
    val dayTasks = tasks.filter { it.date == selectedDate.toString() }
    LazyColumn(modifier.fillMaxSize().padding(horizontal=20.dp), contentPadding=PaddingValues(top=24.dp,bottom=96.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { Text("플래너", fontSize=26.sp, fontWeight=FontWeight.Bold, color=Ink) }
        item { DateSelector(selectedDate,onDateChange) }
        item { Text(selectedDate.format(DateTimeFormatter.ofPattern("M월 d일"))+" 일정", fontSize=18.sp, fontWeight=FontWeight.SemiBold) }
        if (dayTasks.isEmpty()) item { Text("이 날짜에 일정이 없습니다. + 버튼으로 추가하세요.", color=Muted) }
        items(dayTasks, key={it.id}) { TaskCard(it,onToggle,onEdit,onDelete) }
    }
}

@Composable
fun DateSelector(date: LocalDate, onChange: (LocalDate)->Unit) {
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp), modifier=Modifier.fillMaxWidth()) {
        (-2..2).forEach { offset ->
            val d = date.plusDays(offset.toLong())
            val selected = offset == 0
            Surface(
                modifier=Modifier.weight(1f).clickable { onChange(d) },
                shape=RoundedCornerShape(16.dp),
                color=if(selected) Purple else Color.White
            ) {
                Column(Modifier.padding(vertical=12.dp), horizontalAlignment=Alignment.CenterHorizontally) {
                    Text(d.format(DateTimeFormatter.ofPattern("E")), fontSize=11.sp, color=if(selected) Color.White.copy(alpha=.8f) else Muted)
                    Text("${d.dayOfMonth}", fontWeight=FontWeight.Bold, color=if(selected) Color.White else Ink)
                }
            }
        }
    }
}

@Composable
fun TaskCard(task: StudyTask, onToggle: (StudyTask)->Unit, onEdit: (StudyTask)->Unit, onDelete: (StudyTask)->Unit) {
    Card(shape=RoundedCornerShape(18.dp), colors=CardDefaults.cardColors(containerColor=Color.White), modifier=Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment=Alignment.CenterVertically) {
            Surface(shape=CircleShape, color=if(task.done) Purple else PalePurple, modifier=Modifier.size(34.dp).clickable { onToggle(task) }) {
                Box(contentAlignment=Alignment.Center) {
                    if(task.done) Icon(Icons.Default.Check,null,tint=Color.White)
                    else Text(task.subject.take(1),color=Purple,fontWeight=FontWeight.Bold)
                }
            }
            Column(Modifier.weight(1f).padding(start=12.dp)) {
                Text(task.subject, color=Purple, fontSize=12.sp, fontWeight=FontWeight.SemiBold)
                Text(task.title, color=if(task.done) Muted else Ink, fontWeight=FontWeight.Medium)
                Text("${task.minutes}분", color=Muted, fontSize=12.sp)
            }
            IconButton(onClick={onEdit(task)}) { Icon(Icons.Default.Edit,null,tint=Muted) }
            IconButton(onClick={onDelete(task)}) { Icon(Icons.Default.Delete,null,tint=Muted) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorDialog(initial: StudyTask?, date: LocalDate, onDismiss: ()->Unit, onSave: (StudyTask)->Unit) {
    var title by remember(initial) { mutableStateOf(initial?.title ?: "") }
    var subject by remember(initial) { mutableStateOf(initial?.subject ?: "공부") }
    var minutes by remember(initial) { mutableStateOf((initial?.minutes ?: 30).toString()) }
    var taskDate by remember(initial,date) { mutableStateOf(initial?.date ?: date.toString()) }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest=onDismiss,
        title={ Text(if(initial==null) "할 일 추가" else "할 일 수정", fontWeight=FontWeight.Bold) },
        text={
            Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title,{title=it},label={Text("할 일 이름")},singleLine=true)
                OutlinedTextField(subject,{subject=it},label={Text("과목")},singleLine=true)
                OutlinedTextField(minutes,{minutes=it.filter(Char::isDigit)},label={Text("예상 시간(분)")},singleLine=true)
                OutlinedTextField(taskDate,{taskDate=it},label={Text("날짜 (YYYY-MM-DD)")},singleLine=true)
                if(error) Text("할 일 이름과 올바른 날짜를 입력해 주세요.",color=Color.Red,fontSize=12.sp)
            }
        },
        confirmButton={ TextButton(onClick={
            try {
                if(title.isBlank() || subject.isBlank()) throw Exception()
                LocalDate.parse(taskDate)
                onSave(StudyTask(initial?.id ?: System.currentTimeMillis(),title.trim(),subject.trim(),taskDate,(minutes.toIntOrNull() ?: 30).coerceAtLeast(1),initial?.done ?: false))
            } catch (_:Exception) { error=true }
        }) { Text("저장") } },
        dismissButton={ TextButton(onClick=onDismiss) { Text("취소") } }
    )
}

@Composable
fun TimerPage(
    running: Boolean, seconds: Int, mode: String, onStartPause: ()->Unit, onReset: ()->Unit, onMode: (String)->Unit,
    modifier: Modifier = Modifier
) {
    val mins = seconds/60
    val secs = seconds%60
    Column(modifier.fillMaxSize().padding(24.dp), horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.Center) {
        Text("집중 타이머", fontSize=26.sp, fontWeight=FontWeight.Bold, color=Ink)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            FilterChip(selected=mode=="공부",onClick={onMode("공부")},label={Text("집중 25분")})
            FilterChip(selected=mode=="휴식",onClick={onMode("휴식")},label={Text("휴식 5분")})
        }
        Spacer(Modifier.height(28.dp))
        Box(contentAlignment=Alignment.Center, modifier=Modifier.size(250.dp)) {
            CircularProgressIndicator(progress={seconds.toFloat()/(if(mode=="공부")1500f else 300f)}, strokeWidth=10.dp, modifier=Modifier.fillMaxSize(), color=Purple, trackColor=PalePurple)
            Text("%02d:%02d".format(mins,secs), fontSize=48.sp, fontWeight=FontWeight.Bold, color=Ink)
        }
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Button(onClick=onStartPause, shape=RoundedCornerShape(14.dp), modifier=Modifier.width(150.dp)) { Text(if(running) "일시정지" else "시작") }
            OutlinedButton(onClick=onReset, shape=RoundedCornerShape(14.dp)) { Text("초기화") }
        }
        Spacer(Modifier.height(14.dp))
        Text(if(running) "집중 중이에요. 조금만 더 힘내요!" else "준비되면 시작 버튼을 눌러 주세요.", color=Muted)
    }
}
