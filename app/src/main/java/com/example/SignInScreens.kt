package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cloud.Account
import com.example.cloud.AccountStatus
import com.example.cloud.ROLE_DONOR
import com.example.cloud.ROLE_NGO
import com.example.ui.theme.DangerColor
import com.example.ui.theme.DangerContainer
import com.example.ui.theme.OnDangerContainer
import com.example.ui.theme.OnPrimaryGreen
import com.example.ui.theme.OutlineColor
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.SecondaryAmber
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** What shows before the main app when the app is connected to Firebase and nobody is ready yet. */
@Composable
fun AccountGate(status: AccountStatus) {
    when (status) {
        AccountStatus.Loading -> GateLoading()
        AccountStatus.SignedOut -> SignInScreen()
        is AccountStatus.NeedsRole -> ChooseRoleScreen(status)
        is AccountStatus.Failed -> GateFailed(status.message)
        is AccountStatus.Ready -> Unit
    }
}

@Composable
private fun GateColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) { content() }
}

@Composable
private fun GateLoading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = PrimaryGreen)
    }
}

@Composable
private fun GateFailed(message: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    GateColumn {
        Spacer(Modifier.height(80.dp))
        AnimatedKarmaLogo(size = 96.dp)
        Spacer(Modifier.height(24.dp))
        Text("We couldn't open your account", style = MaterialTheme.typography.titleLarge, color = TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        GateError(message)
        Spacer(Modifier.height(24.dp))
        GatePrimaryButton("Try again", onClick = { Account.retry() })
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { scope.launch { Account.signOut(context) } }) {
            Text("Sign out", color = TextSecondary)
        }
    }
}

@Composable
fun SignInScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var creating by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun run(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "Something went wrong. Please try again."
            } finally {
                busy = false
            }
        }
    }

    GateColumn {
        Spacer(Modifier.height(24.dp))
        AnimatedKarmaLogo(size = 112.dp)
        KarmaKitchenLogoText(fontSize = 36.sp, textColor = TextPrimary, accentColor = PrimaryGreen)
        Spacer(Modifier.height(8.dp))
        Text(
            if (creating) "Create your account" else "Sign in to share and receive food",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        KarmaOutlinedButton(
            onClick = { run { Account.signInWithGoogle(context) } },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Continue with Google", color = TextPrimary, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f), color = OutlineColor)
            Text("or use email", style = MaterialTheme.typography.bodySmall, color = TextTertiary, modifier = Modifier.padding(horizontal = 12.dp))
            HorizontalDivider(Modifier.weight(1f), color = OutlineColor)
        }
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            supportingText = if (creating) {
                { Text("At least 6 characters") }
            } else null,
            modifier = Modifier.fillMaxWidth()
        )

        error?.let {
            Spacer(Modifier.height(12.dp))
            GateError(it)
        }

        Spacer(Modifier.height(20.dp))
        GatePrimaryButton(
            text = if (creating) "Create account" else "Sign in",
            busy = busy,
            enabled = email.isNotBlank() && password.isNotEmpty(),
            onClick = { run { Account.signInWithEmail(email, password, create = creating) } }
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = {
            creating = !creating
            error = null
        }) {
            Text(
                if (creating) "I already have an account" else "New here? Create an account",
                color = PrimaryGreen,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** First sign-in only: donor or NGO. The choice is saved and cannot be switched later. */
@Composable
fun ChooseRoleScreen(status: AccountStatus.NeedsRole) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var role by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf(status.name) }
    var phone by remember { mutableStateOf("") }
    var ngoName by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    GateColumn {
        val chosen = role
        if (chosen == null) {
            Spacer(Modifier.height(24.dp))
            AnimatedKarmaLogo(size = 112.dp)
            Text("How will you use KarmaKitchen?", style = MaterialTheme.typography.titleLarge, color = TextPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "Signed in as ${status.email.ifBlank { "you" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            RoleCard(
                title = "I'm donating food",
                subtitle = "Share surplus food and earn karma coins",
                art = R.drawable.illus_food_meal,
                tint = PrimaryGreen,
                onClick = { role = ROLE_DONOR }
            )
            Spacer(Modifier.height(16.dp))
            RoleCard(
                title = "I'm receiving food",
                subtitle = "For NGOs, shelters and community kitchens",
                art = R.drawable.illus_role_ngo,
                tint = SecondaryAmber,
                onClick = { role = ROLE_NGO }
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "You pick this once. Use a different account for the other role.",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { scope.launch { Account.signOut(context) } }) {
                Text("Use a different account", color = TextSecondary)
            }
        } else {
            val isNgo = chosen == ROLE_NGO
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { role = null; error = null }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text(
                    if (isNgo) "About your NGO" else "About you",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
            Spacer(Modifier.height(16.dp))
            if (isNgo) {
                OutlinedTextField(
                    value = ngoName,
                    onValueChange = { ngoName = it },
                    label = { Text("NGO, shelter or kitchen name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(if (isNgo) "Your name" else "Full name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone") },
                supportingText = {
                    Text(if (isNgo) "Donors can call this number about a pickup" else "The NGO picking up your food can call you")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            if (isNgo) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "New NGOs are checked by the KarmaKitchen team before they can accept food. You can look around in the meantime.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            error?.let {
                Spacer(Modifier.height(12.dp))
                GateError(it)
            }
            Spacer(Modifier.height(24.dp))
            GatePrimaryButton(
                text = "Continue",
                busy = busy,
                enabled = name.isNotBlank() && (!isNgo || ngoName.isNotBlank()),
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            Account.createProfile(chosen, name, phone, ngoName)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            error = e.message ?: "Could not save your profile. Please try again."
                        } finally {
                            busy = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun GatePrimaryButton(text: String, onClick: () -> Unit, busy: Boolean = false, enabled: Boolean = true) {
    KarmaButton(
        onClick = onClick,
        enabled = enabled && !busy,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = OnPrimaryGreen)
    ) {
        if (busy) {
            CircularProgressIndicator(color = OnPrimaryGreen, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Text(text, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun GateError(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DangerContainer)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = DangerColor)
        Spacer(Modifier.width(10.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = OnDangerContainer, modifier = Modifier.weight(1f))
    }
}
