sed -i 's/fun StudentDashboard(modifier: Modifier = Modifier) {/fun StudentDashboard(\n    modifier: Modifier = Modifier,\n    authViewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),\n    onLogout: () -> Unit = {}\n) {\n    var selectedTab by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("Inicio") }/' app/src/main/java/com/example/StudentDashboard.kt
sed -i 's/bottomBar = { StudentBottomNavigation() }/bottomBar = { StudentBottomNavigation(selectedTab) { selectedTab = it } }/' app/src/main/java/com/example/StudentDashboard.kt
sed -i 's/fun StudentBottomNavigation()/fun StudentBottomNavigation(selectedTab: String, onTabSelected: (String) -> Unit)/' app/src/main/java/com/example/StudentDashboard.kt
sed -i 's/selected = true,/selected = selectedTab == "Inicio",/' app/src/main/java/com/example/StudentDashboard.kt
sed -i 's/onClick = { \/\* TODO \*\/ },/onClick = { onTabSelected("Inicio") },/' app/src/main/java/com/example/StudentDashboard.kt
sed -i 's/selected = false,/selected = selectedTab == "Perfil",/' app/src/main/java/com/example/StudentDashboard.kt
sed -i 's/onClick = { \/\* TODO \*\/ },/onClick = { onTabSelected("Perfil") },/' app/src/main/java/com/example/StudentDashboard.kt
