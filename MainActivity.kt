package com.example.nexora

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import java.util.Locale
import kotlinx.coroutines.delay

// ==================== COLORS ====================

private val ink = Color(0xFF080B14)
private val panel = Color(0xFF151B2C)
private val violet = Color(0xFF9B72FF)
private val cyan = Color(0xFF67E8F9)
private val white = Color(0xFFF5F3FF)
private val muted = Color(0xFF9CA8C7)

private val gradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF8B5CF6),
        Color(0xFF6366F1),
        Color(0xFF22D3EE)
    )
)

private val chatBackground = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF090D18),
        Color(0xFF111329),
        Color(0xFF16132D),
        Color(0xFF0B1726)
    )
)

private val listBackground = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0A1020),
        Color(0xFF10182A),
        Color(0xFF172033),
        Color(0xFF0B1220)
    )
)

private val profileBackground = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF111024),
        Color(0xFF20163A),
        Color(0xFF151A30),
        Color(0xFF0A1423)
    )
)

private val appBackground = Brush.linearGradient(
    colors = listOf(
        Color(0xFF080B14),
        Color(0xFF11152B),
        Color(0xFF1B1230),
        Color(0xFF0A1928)
    )
)

private val defaultAvatars = listOf(
    "👨‍🚀", "🦊", "🐱", "🐼", "🦁", "🐻",
    "🐸", "🐰", "🐯", "🐨", "🐵", "🦄",
    "🐧", "🐙", "🐷", "🐮", "🐹", "🐻‍❄️",
    "🌻", "🌙", "⭐", "🔥", "💎", "🎮"
)

private const val NEXORA_CHANNEL_ID = "nexora_messages"

// ==================== MAIN ACTIVITY ====================

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel()

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }

        setContent {
            NexoraRoot()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NEXORA_CHANNEL_ID,
                "NEXORA Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new NEXORA messages"
                enableVibration(true)
                setSound(
                    android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(
                            android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION
                        )
                        .build()
                )
            }

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}

// ==================== DATA MODELS ====================

private data class UserProfile(
    val uid: String,
    val name: String,
    val username: String,
    val photoUrl: String = "",
    val avatar: String = "👨‍🚀"
)

private data class Conversation(
    val id: String,
    val peerId: String,
    val peerName: String,
    val peerUsername: String,
    val photoUrl: String,
    val lastMessage: String,
    val peerAvatar: String = "👨‍🚀"
)

private data class Msg(
    val id: String,
    val senderId: String,
    val text: String,
    val imageUrl: String,
    val replyText: String,
    val createdAt: Timestamp?,
    val readBy: List<String>
)

// ==================== UTILITIES ====================

private fun usernameEmail(u: String) =
    "${u.trim().lowercase(Locale.ROOT)}@nexora.example.com"

private fun clean(u: String) =
    u.trim().lowercase(Locale.ROOT)
        .replace("[^a-z0-9_]".toRegex(), "")

// ==================== ROOT ====================

@Composable
private fun NexoraRoot() {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var authUid by remember { mutableStateOf(auth.currentUser?.uid) }
    var me by remember { mutableStateOf<UserProfile?>(null) }
    var busy by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    // Startup animation
    var showIntro by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2200)
        showIntro = false
    }

    // React immediately to login, logout and registration.
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            authUid = firebaseAuth.currentUser?.uid
            busy = firebaseAuth.currentUser != null
            me = null

            if (firebaseAuth.currentUser == null) {
                busy = false
            }
        }

        auth.addAuthStateListener(listener)

        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    // Continuously observe the authenticated user's profile.
    DisposableEffect(authUid) {
        val uid = authUid

        if (uid == null) {
            me = null
            busy = false
            onDispose { }
        } else {
            busy = true

            val listener = db.collection("users")
                .document(uid)
                .addSnapshotListener { d, e ->
                    if (e != null) {
                        error = e.localizedMessage ?: "Profile load failed"
                        busy = false
                    } else if (d != null && d.exists()) {
                        me = UserProfile(
                            uid,
                            d.getString("name") ?: "User",
                            d.getString("username") ?: "user",
                            d.getString("photoUrl") ?: "",
                            d.getString("avatar") ?: "👨‍🚀"
                        )
                        busy = false
                    } else {
                        me = null
                        busy = false
                    }
                }

            onDispose {
                listener.remove()
            }
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = violet,
            secondary = cyan,
            background = ink,
            surface = panel
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(appBackground)
        ) {
            AbstractBackdrop()

            if (showIntro) StartupAnimation() else if (busy) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = cyan)
                }
            } else if (authUid == null || me == null) {
                AuthScreen(
                    error,
                    { error = it },
                    { busy = true; it() },
                    { busy = false }
                )
            } else {
                me?.let { profile ->
                    MainShell(
                        profile,
                        { me = it },
                        {
                            auth.signOut()
                            me = null
                            authUid = null
                        },
                        error,
                        { error = it }
                    )
                }
            }
        }
    }
}

// ==================== STARTUP ANIMATION ====================

@Composable
private fun StartupAnimation() {
    var displayedText by remember { mutableStateOf("") }
    var showCursor by remember { mutableStateOf(true) }
    val fade = remember { Animatable(0f) }

    val fullText = "Amir Erfan Eslamikia"

    LaunchedEffect(Unit) {
        fade.animateTo(
            1f,
            animationSpec = tween(700)
        )

        for (character in fullText) {
            displayedText += character
            delay(50)
        }

        delay(1200)

    }

    LaunchedEffect(Unit) {
        while (true) {
            showCursor = !showCursor
            delay(500)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF080B14),
                        Color(0xFF14152B),
                        Color(0xFF1B1230),
                        Color(0xFF080B14)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.graphicsLayer(alpha = fade.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NEX",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = white
                )

                Text(
                    text = "ORA",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = violet
                )
            }

            Text(
                text = "Connect beyond the ordinary",
                color = muted,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayedText + if (showCursor) "▋" else "",
                    color = cyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    softWrap = true
                )
            }
        }
    }
}
// ==================== BACKDROP ====================

@Composable
private fun AbstractBackdrop() {
    Box(Modifier.fillMaxSize()) {

        Box(
            Modifier
                .offset((-80).dp, 60.dp)
                .size(240.dp)
                .clip(CircleShape)
                .background(Color(0xFF8B5CF6).copy(alpha = 0.09f))
        )

        Box(
            Modifier
                .offset(230.dp, 420.dp)
                .size(210.dp)
                .clip(RoundedCornerShape(80.dp))
                .background(Color(0xFF22D3EE).copy(alpha = 0.06f))
        )

        Box(
            Modifier
                .offset(160.dp, (-50).dp)
                .size(150.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(Color(0xFFEC4899).copy(alpha = 0.04f))
        )

        Box(
            Modifier
                .offset((-40).dp, 520.dp)
                .size(180.dp)
                .clip(CircleShape)
                .background(Color(0xFF6366F1).copy(alpha = 0.05f))
        )
    }
}

// ==================== AUTH SCREEN ====================

@Composable
private fun AuthScreen(
    error: String,
    setError: (String) -> Unit,
    setBusy: ((() -> Unit)) -> Unit,
    finishBusy: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var login by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("NEX", fontSize = 34.sp, fontWeight = FontWeight.Black, color = white)
            Text("ORA", fontSize = 34.sp, fontWeight = FontWeight.Black, color = violet)
        }

        Text("Connect beyond the ordinary", color = cyan)

        Spacer(Modifier.height(30.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF17182D).copy(alpha = 0.94f)
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (login) "Welcome back" else "Create account",
                    color = white,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )

                if (!login) {
                    OutlinedTextField(
                        name,
                        { name = it },
                        label = { Text("Display name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    username,
                    { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    password,
                    { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (error.isNotBlank()) {
                    Text(error, color = Color(0xFFFF8A8A))
                }

                Button(
                    onClick = {
                        val u = clean(username)

                        if (u.length < 3 ||
                            password.length < 6 ||
                            (!login && name.isBlank())
                        ) {
                            setError(
                                "Enter a username (3+ characters), password (6+), and name when registering."
                            )
                            return@Button
                        }

                        setError("")

                        setBusy {
                            if (login) {
                                auth.signInWithEmailAndPassword(
                                    usernameEmail(u), password
                                )
                                    .addOnSuccessListener {
                                        // AuthStateListener in NexoraRoot
                                        // handles the transition automatically.
                                        finishBusy()
                                    }
                                    .addOnFailureListener {
                                        setError(it.localizedMessage ?: "Sign in failed")
                                        finishBusy()
                                    }
                            } else {
                                auth.createUserWithEmailAndPassword(
                                    usernameEmail(u), password
                                )
                                    .addOnSuccessListener { result ->
                                        val uid = result.user?.uid
                                            ?: return@addOnSuccessListener

                                        db.collection("usernames")
                                            .document(u)
                                            .get()
                                            .addOnSuccessListener { exists ->
                                                if (exists.exists()) {
                                                    auth.currentUser?.delete()
                                                    setError("Username already taken.")
                                                    finishBusy()
                                                } else {
                                                    db.collection("usernames")
                                                        .document(u)
                                                        .set(mapOf("uid" to uid))
                                                        .addOnSuccessListener {
                                                            db.collection("users")
                                                                .document(uid)
                                                                .set(
                                                                    mapOf(
                                                                        "uid" to uid,
                                                                        "name" to name.trim(),
                                                                        "username" to u,
                                                                        "photoUrl" to "",
                                                                        "avatar" to "👨‍🚀",
                                                                        "createdAt" to FieldValue.serverTimestamp()
                                                                    )
                                                                )
                                                                .addOnSuccessListener {
                                                                    finishBusy()
                                                                }
                                                                .addOnFailureListener {
                                                                    setError(it.localizedMessage ?: "Profile creation failed")
                                                                    finishBusy()
                                                                }
                                                        }
                                                        .addOnFailureListener {
                                                            setError(it.localizedMessage ?: "Username registration failed")
                                                            finishBusy()
                                                        }
                                                }
                                            }
                                            .addOnFailureListener {
                                                setError(it.localizedMessage ?: "Username check failed")
                                                finishBusy()
                                            }
                                    }
                                    .addOnFailureListener {
                                        setError(it.localizedMessage ?: "Registration failed")
                                        finishBusy()
                                    }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = violet)
                ) {
                    Text(if (login) "Sign in" else "Create account")
                }

                TextButton(
                    onClick = {
                        login = !login
                        setError("")
                    }
                ) {
                    Text(
                        if (login) "Create an account"
                        else "Already have an account? Sign in",
                        color = cyan
                    )
                }
            }
        }
    }
}

// ==================== MAIN SHELL ====================

@Composable
private fun MainShell(
    profile: UserProfile,
    onProfile: (UserProfile) -> Unit,
    signOut: () -> Unit,
    error: String,
    setError: (String) -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    var active by remember { mutableStateOf<Conversation?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp)
    ) {
        if (active != null) {
            ChatScreen(
                profile,
                active!!,
                { active = null },
                setError
            )
        } else {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("NEX", color = white, fontSize = 26.sp, fontWeight = FontWeight.Black)
                        Text("ORA", color = violet, fontSize = 26.sp, fontWeight = FontWeight.Black)
                    }

                    Text("Your space to connect", color = muted, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(18.dp))

            Box(
                Modifier.fillMaxWidth().weight(1f)
            ) {
                if (tab == 0) {
                    ChatList(
                        profile,
                        { active = it },
                        setError
                    )
                } else {
                    ProfileScreen(
                        profile,
                        onProfile,
                        signOut,
                        setError
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFF151B2C).copy(alpha = 0.95f))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NavPill(
                    "Chats",
                    Icons.Default.Chat,
                    tab == 0,
                    Modifier.weight(1f)
                ) {
                    tab = 0
                }

                NavPill(
                    "Profile",
                    Icons.Default.Person,
                    tab == 1,
                    Modifier.weight(1f)
                ) {
                    tab = 1
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}

// ==================== NAVIGATION ====================

@Composable
private fun NavPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier,
    click: () -> Unit
) {
    Surface(
        modifier.clickable { click() }.animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) Color.Transparent else panel
    ) {
        Box(
            Modifier
                .background(
                    if (selected) gradient else Brush.linearGradient(listOf(panel, panel))
                )
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, null, tint = white)
                Text(label, color = white, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==================== CHAT LIST ====================

@Composable
private fun ChatList(
    profile: UserProfile,
    open: (Conversation) -> Unit,
    setError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    var chats by remember { mutableStateOf(listOf<Conversation>()) }
    var add by remember { mutableStateOf(false) }

    DisposableEffect(profile.uid) {
        val listener = db.collection("conversations")
            .whereArrayContains("participants", profile.uid)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    setError(e.localizedMessage ?: "Chat list error")
                } else {
                    chats = snap?.documents?.mapNotNull { d ->
                        val members = d.get("participants") as? List<*>
                            ?: emptyList<Any>()

                        val peer = members.filterIsInstance<String>()
                            .firstOrNull { it != profile.uid }
                            ?: return@mapNotNull null

                        Conversation(
                            d.id,
                            peer,
                            (d.get("memberNames") as? Map<*, *>)?.get(peer) as? String ?: "User",
                            (d.get("memberUsernames") as? Map<*, *>)?.get(peer) as? String ?: "",
                            (d.get("memberPhotos") as? Map<*, *>)?.get(peer) as? String ?: "",
                            d.getString("lastMessage") ?: "",
                            (d.get("memberAvatars") as? Map<*, *>)?.get(peer) as? String ?: "👨‍🚀"
                        )
                    } ?: emptyList()
                }
            }

        onDispose { listener.remove() }
    }

    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(26.dp))
            .background(listBackground)
            .padding(16.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Text(
                "Messages",
                color = white,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            if (chats.isEmpty()) {
                Text(
                    "No conversations yet. Tap Add below to start one.",
                    color = muted,
                    modifier = Modifier.padding(12.dp)
                )
            }

            LazyColumn(
                Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(chats, key = { it.id }) { c ->
                    ChatRow(c) { open(c) }
                }
            }

            Box(
                Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                FloatingActionButton(
                    onClick = { add = true },
                    containerColor = violet,
                    contentColor = white,
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PersonAdd, "Add person")
                        Spacer(Modifier.width(8.dp))
                        Text("Add", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (add) {
        AddChatDialog(profile, { add = false }, open, setError)
    }
}

// ==================== CHAT ROW ====================

@Composable
private fun ChatRow(
    c: Conversation,
    open: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF192338),
                        Color(0xFF151D30)
                    )
                )
            )
            .clickable { open() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(c.photoUrl, c.peerName, 58, c.peerAvatar)

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(c.peerName, color = white, fontWeight = FontWeight.Bold, fontSize = 17.sp)

            Text("@${c.peerUsername}", color = cyan, fontSize = 12.sp)

            Spacer(Modifier.height(5.dp))

            Text(
                c.lastMessage.ifBlank { "Start chatting" },
                color = muted,
                maxLines = 1
            )
        }
    }
}

// ==================== ADD CHAT ====================

@Composable
private fun AddChatDialog(
    profile: UserProfile,
    close: () -> Unit,
    open: (Conversation) -> Unit,
    setError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    var username by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = close,
        containerColor = panel,
        title = { Text("Start a conversation", color = white) },
        text = {
            OutlinedTextField(
                username,
                { username = it },
                label = { Text("Username") }
            )
        },
        confirmButton = {
            TextButton(
                enabled = !working,
                onClick = {
                    working = true
                    val u = clean(username)

                    db.collection("usernames").document(u)
                        .get()
                        .addOnSuccessListener { ref ->
                            val uid = ref.getString("uid")

                            if (uid == null || uid == profile.uid) {
                                setError(
                                    if (uid == profile.uid) "That's your own username."
                                    else "User not found."
                                )
                                working = false
                            } else {
                                db.collection("users").document(uid)
                                    .get()
                                    .addOnSuccessListener { user ->
                                        val peerName = user.getString("name") ?: "User"
                                        val peerUsername = user.getString("username") ?: u
                                        val photo = user.getString("photoUrl") ?: ""
                                        val peerAvatar = user.getString("avatar") ?: "👨‍🚀"

                                        val ids = listOf(profile.uid, uid).sorted()
                                        val id = ids.joinToString("_")

                                        val names = mapOf(
                                            profile.uid to profile.name,
                                            uid to peerName
                                        )

                                        val usernames = mapOf(
                                            profile.uid to profile.username,
                                            uid to peerUsername
                                        )

                                        val photos = mapOf(
                                            profile.uid to profile.photoUrl,
                                            uid to photo
                                        )

                                        val avatars = mapOf(
                                            profile.uid to profile.avatar,
                                            uid to peerAvatar
                                        )

                                        db.collection("conversations").document(id)
                                            .get()
                                            .addOnSuccessListener { existing ->
                                                val data = mapOf(
                                                    "participants" to ids,
                                                    "memberNames" to names,
                                                    "memberUsernames" to usernames,
                                                    "memberPhotos" to photos,
                                                    "memberAvatars" to avatars,
                                                    "lastMessage" to "",
                                                    "updatedAt" to FieldValue.serverTimestamp()
                                                )

                                                if (!existing.exists()) {
                                                    db.collection("conversations")
                                                        .document(id)
                                                        .set(data)
                                                        .addOnFailureListener {
                                                            setError(it.localizedMessage ?: "Chat creation failed")
                                                        }
                                                }

                                                open(
                                                    Conversation(
                                                        id, uid, peerName,
                                                        peerUsername, photo, "", peerAvatar
                                                    )
                                                )
                                                close()
                                            }
                                            .addOnFailureListener {
                                                setError(it.localizedMessage ?: "Conversation lookup failed")
                                                working = false
                                            }
                                    }
                                    .addOnFailureListener {
                                        setError(it.localizedMessage ?: "Profile lookup failed")
                                        working = false
                                    }
                            }
                        }
                        .addOnFailureListener {
                            setError(it.localizedMessage ?: "Username lookup failed")
                            working = false
                        }
                }
            ) {
                Text("Start")
            }
        },
        dismissButton = {
            TextButton(onClick = close) { Text("Cancel") }
        }
    )
}

// ==================== CHAT SCREEN ====================

@Composable
private fun ChatScreen(
    profile: UserProfile,
    chat: Conversation,
    back: () -> Unit,
    setError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val notificationContext = LocalContext.current

    var messages by remember { mutableStateOf(listOf<Msg>()) }
    var text by remember { mutableStateOf("") }
    var reply by remember { mutableStateOf<Msg?>(null) }

    // Live peer profile: avatar, username and name update immediately.
    var liveChat by remember(chat.id) { mutableStateOf(chat) }

    DisposableEffect(chat.peerId) {
        val listener = db.collection("users")
            .document(chat.peerId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    setError(e.localizedMessage ?: "Peer profile update failed")
                } else if (snapshot != null && snapshot.exists()) {
                    liveChat = liveChat.copy(
                        peerName = snapshot.getString("name") ?: liveChat.peerName,
                        peerUsername = snapshot.getString("username") ?: liveChat.peerUsername,
                        photoUrl = snapshot.getString("photoUrl") ?: "",
                        peerAvatar = snapshot.getString("avatar") ?: "👨‍🚀"
                    )
                }
            }

        onDispose {
            listener.remove()
        }
    }

    DisposableEffect(chat.id) {
        var firstSnapshot = true

        val listener = db.collection("conversations")
            .document(chat.id)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    setError(e.localizedMessage ?: "Message loading failed")
                } else {
                    val documents = snap?.documents ?: emptyList()

                    messages = documents.map { d ->
                        Msg(
                            d.id,
                            d.getString("senderId") ?: "",
                            d.getString("text") ?: "",
                            "",
                            d.getString("replyText") ?: "",
                            d.getTimestamp("createdAt"),
                            (d.get("readBy") as? List<*>)
                                ?.filterIsInstance<String>() ?: emptyList()
                        )
                    }

                    if (!firstSnapshot) {
                        documents.forEach { d ->
                            val senderId = d.getString("senderId") ?: ""
                            val readBy = (d.get("readBy") as? List<*>)
                                ?.filterIsInstance<String>() ?: emptyList()

                            if (senderId != profile.uid && !readBy.contains(profile.uid)) {
                                vibrateOnly(notificationContext)

                                d.reference.update(
                                    "readBy",
                                    FieldValue.arrayUnion(profile.uid)
                                )
                            }
                        }
                    }

                    firstSnapshot = false

                    documents.forEach { d ->
                        val senderId = d.getString("senderId") ?: ""
                        val readBy = (d.get("readBy") as? List<*>)
                            ?.filterIsInstance<String>() ?: emptyList()

                        if (senderId != profile.uid && !readBy.contains(profile.uid)) {
                            d.reference.update(
                                "readBy",
                                FieldValue.arrayUnion(profile.uid)
                            )
                        }
                    }
                }
            }

        onDispose { listener.remove() }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Column(Modifier.fillMaxSize()) {

            // HEADER

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp, top = 2.dp)
            ) {
                IconButton(
                    onClick = back,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        null,
                        tint = white,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Avatar(liveChat.photoUrl, liveChat.peerName, 38, liveChat.peerAvatar)

                Column(Modifier.padding(start = 10.dp)) {
                    Text(
                        liveChat.peerName,
                        color = white,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Text(
                        "@${liveChat.peerUsername}",
                        color = cyan.copy(alpha = 0.9f),
                        fontSize = 11.sp
                    )
                }
            }

            // MESSAGES

            LazyColumn(
                Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(messages, key = { it.id }) { m ->
                    MessageBubble(m, profile.uid) { reply = m }
                }
            }

            // REPLY PREVIEW

            if (reply != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF172238).copy(alpha = 0.85f))
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(cyan)
                    )

                    Spacer(Modifier.width(7.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            "Replying",
                            color = cyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            reply?.text ?: "",
                            color = white.copy(alpha = 0.85f),
                            maxLines = 1,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = { reply = null },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            "Cancel reply",
                            tint = muted,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
            }

            // MESSAGE INPUT

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp, horizontal = 2.dp)
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF1A2238),
                                    Color(0xFF1E1A32)
                                )
                            )
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp),
                        placeholder = {
                            Text(
                                "Message...",
                                color = muted.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        },
                        shape = RoundedCornerShape(22.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            cursorColor = cyan,
                            focusedTextColor = white,
                            unfocusedTextColor = white
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.5.sp)
                    )
                }

                Spacer(Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
                            )
                        )
                        .clickable {
                            if (text.isNotBlank()) {
                                sendMessage(
                                    db,
                                    liveChat,
                                    profile,
                                    text.trim(),
                                    reply?.text ?: ""
                                )
                                text = ""
                                reply = null
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Send,
                        "Send",
                        tint = white,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ==================== SEND MESSAGE ====================

private fun sendMessage(
    db: FirebaseFirestore,
    chat: Conversation,
    me: UserProfile,
    text: String,
    replyText: String
) {
    val data = hashMapOf<String, Any>(
        "senderId" to me.uid,
        "text" to text,
        "replyText" to replyText,
        "createdAt" to FieldValue.serverTimestamp(),
        "readBy" to listOf(me.uid)
    )

    db.collection("conversations")
        .document(chat.id)
        .collection("messages")
        .add(data)
        .addOnSuccessListener {
            db.collection("conversations")
                .document(chat.id)
                .update(
                    "lastMessage", text,
                    "updatedAt", FieldValue.serverTimestamp(),
                    "lastSenderId", me.uid
                )
        }
}

// ==================== MESSAGE BUBBLE ====================

@Composable
private fun MessageBubble(
    m: Msg,
    uid: String,
    reply: () -> Unit
) {
    val mine = m.senderId == uid

    val bubbleBrush = if (mine) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF8B5CF6),
                Color(0xFF7045D5),
                Color(0xFF5B36B8)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF202B40),
                Color(0xFF172236),
                Color(0xFF121C2D)
            )
        )
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            Modifier
                .widthIn(max = 280.dp)
                .wrapContentWidth()
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (mine) 20.dp else 6.dp,
                        bottomEnd = if (mine) 6.dp else 20.dp
                    )
                )
                .background(bubbleBrush)
                .clickable { reply() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {

            if (m.replyText.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 3.dp)
                ) {
                    Box(
                        Modifier
                            .width(2.5.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(
                                if (mine) cyan
                                else violet.copy(alpha = 0.9f)
                            )
                    )

                    Spacer(Modifier.width(5.dp))

                    Text(
                        m.replyText,
                        color = white.copy(alpha = 0.78f),
                        maxLines = 1,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
            }

            Text(
                m.text,
                color = white,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 18.sp
            )

            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    m.createdAt?.toDate()?.let {
                        java.text.SimpleDateFormat(
                            "HH:mm",
                            Locale.getDefault()
                        ).format(it)
                    } ?: "",
                    color = white.copy(alpha = 0.55f),
                    fontSize = 9.sp
                )

                if (mine) {
                    Spacer(Modifier.width(3.dp))

                    Text(
                        if (m.readBy.size > 1) "✓✓" else "✓",
                        color = if (m.readBy.size > 1)
                            cyan
                        else
                            white.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// ==================== PROFILE ====================

@Composable
private fun ProfileScreen(
    profile: UserProfile,
    update: (UserProfile) -> Unit,
    signOut: () -> Unit,
    setError: (String) -> Unit
) {
    var avatarDialog by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(26.dp))
            .background(profileBackground)
            .padding(20.dp)
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(22.dp))

            Avatar("", profile.name, 112, profile.avatar)

            Text(
                profile.name,
                color = white,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text("@${profile.username}", color = cyan)

            Button(
                onClick = { avatarDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = violet)
            ) {
                Text("Change avatar")
            }

            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF192338).copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Account", color = white, fontWeight = FontWeight.Bold)

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "Your NEXORA username is your identity.",
                        color = muted
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Messages are stored in Firestore.",
                        color = muted
                    )
                }
            }

            Button(
                onClick = signOut,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF9B344C)
                )
            ) {
                Icon(Icons.Default.Logout, null)
                Spacer(Modifier.width(8.dp))
                Text("Sign out")
            }
        }
    }

    if (avatarDialog) {
        AvatarPicker(
            current = profile.avatar,
            onClose = { avatarDialog = false },
            onSelect = { emoji ->

                val db = FirebaseFirestore.getInstance()

                db.collection("users")
                    .document(profile.uid)
                    .update("avatar", emoji)
                    .addOnSuccessListener {

                        // Update this device immediately.
                        update(profile.copy(avatar = emoji))
                        avatarDialog = false

                        // Update cached avatar values in all conversations.
                        db.collection("conversations")
                            .whereArrayContains("participants", profile.uid)
                            .get()
                            .addOnSuccessListener { conversations ->
                                conversations.documents.forEach { conversation ->
                                    conversation.reference.update(
                                        "memberAvatars.${profile.uid}",
                                        emoji
                                    )
                                }
                            }
                            .addOnFailureListener {
                                setError(
                                    it.localizedMessage
                                        ?: "Conversation avatar synchronization failed"
                                )
                            }
                    }
                    .addOnFailureListener {
                        setError(it.localizedMessage ?: "Avatar update failed")
                    }
            }
        )
    }
}

// ==================== AVATAR PICKER ====================

@Composable
private fun AvatarPicker(
    current: String,
    onClose: () -> Unit,
    onSelect: (String) -> Unit
) {
    var customEmoji by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = panel,
        title = { Text("Choose your avatar", color = white) },
        text = {
            Column {
                OutlinedTextField(
                    value = customEmoji,
                    onValueChange = { customEmoji = it },
                    label = { Text("Custom emoji") },
                    placeholder = { Text("Enter any emoji") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (customEmoji.isNotBlank()) {
                            onSelect(customEmoji.trim())
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = violet)
                ) {
                    Text("Use custom emoji")
                }

                Spacer(Modifier.height(12.dp))

                Text("Default avatars", color = cyan, fontWeight = FontWeight.Bold)

                Spacer(Modifier.height(8.dp))

                defaultAvatars.chunked(4).forEach { group ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        group.forEach { emoji ->
                            Box(
                                Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (emoji == current)
                                            violet.copy(alpha = 0.4f)
                                        else Color.Transparent
                                    )
                                    .clickable { onSelect(emoji) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 30.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onClose) {
                Text("Close")
            }
        }
    )
}

// ==================== AVATAR ====================

@Composable
private fun Avatar(
    url: String,
    name: String,
    size: Int,
    emoji: String = ""
) {
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Text(
            emoji.ifBlank { name.take(1).uppercase() },
            color = white,
            fontSize = androidx.compose.ui.unit.TextUnit(
                if (emoji.isNotBlank()) size * 0.56f else size * 0.45f,
                androidx.compose.ui.unit.TextUnitType.Sp
            ),
            fontWeight = FontWeight.Bold
        )
    }
}

// ==================== VIBRATION ====================

private fun vibrateOnly(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= 31) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                        as VibratorManager

            vibratorManager.defaultVibrator.vibrate(
                VibrationEffect.createOneShot(
                    120,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    120,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        }
    } catch (_: Exception) {
    }
}