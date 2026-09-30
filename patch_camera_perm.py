import sys

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

target_start = """@Composable
fun NgoDashboardScreen(navController: NavController) {
    var isBroadcasting by remember { mutableStateOf(false) }"""

replacement_start = """@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NgoDashboardScreen(navController: NavController) {
    var isBroadcasting by remember { mutableStateOf(false) }"""

content = content.replace(target_start, replacement_start)

target_camera_launcher = """    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: android.graphics.Bitmap? ->"""

replacement_camera_launcher = """    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: android.graphics.Bitmap? ->"""

content = content.replace(target_camera_launcher, replacement_camera_launcher)

target_button_click = """                        // Actions inside card
                        Button(
                            onClick = { cameraLauncher.launch(null) },
                            modifier = Modifier"""

replacement_button_click = """                        // Actions inside card
                        Button(
                            onClick = { 
                                if (cameraPermissionState.status.isGranted) {
                                    cameraLauncher.launch(null) 
                                } else {
                                    cameraPermissionState.launchPermissionRequest()
                                }
                            },
                            modifier = Modifier"""

content = content.replace(target_button_click, replacement_button_click)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

