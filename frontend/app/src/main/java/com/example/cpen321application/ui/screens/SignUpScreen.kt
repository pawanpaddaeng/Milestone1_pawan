package com.example.cpen321application.ui.screens

import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cpen321application.triggerGoogleSignIn
import com.example.cpen321application.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Year

// youngest and oldest ages offered in the birth year picker
private const val MIN_AGE = 19
private const val MAX_AGE = 100

/*  details collected on the sign up screen,
    more profile fields will be added here later
 */
data class SignUpDetails(
    val idToken: String,
    val email: String?,
    val firstName: String,
    val lastName: String,
    val birthYear: Int
)

/*
    Sign up screen for SportsNow:
    The user verifies with Google (which prefills their name), fills in
    their details and presses Create Account, which calls onCreateAccount
 */
@Composable
fun SignUpScreen(
    onCreateAccount: (SignUpDetails) -> Unit,
    onNavigateToSignIn: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var idToken by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf<String?>(null) }
    var firstNameText by remember { mutableStateOf("") }
    var lastNameText by remember { mutableStateOf("") }
    var birthYear by remember { mutableStateOf<Int?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val canCreate = idToken != null && firstNameText.isNotBlank() &&
            lastNameText.isNotBlank() && birthYear != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // top bar with back button and centered app name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = SportsOnSurface
                )
            }
            Text(
                text = "SportsNow",
                color = SportsOnSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Create your account",
                color = SportsOnSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Tell us a bit about yourself to get started.",
                color = SportsOnSurfaceVariant,
                fontSize = 14.sp
            )
        }

        GoogleVerificationCard(
            email = email,
            isVerified = idToken != null,
            isVerifying = isVerifying,
            onVerify = {
                isVerifying = true
                errorText = null
                scope.launch {
                    val token = triggerGoogleSignIn(context)
                    if (token == null) {
                        errorText = "Google verification failed. Make sure a Google " +
                                "account is added to this device"
                    } else {
                        // prefill the form with the name on the Google account
                        val profile = readGoogleProfile(token)
                        idToken = token
                        email = profile.email
                        if (firstNameText.isBlank()) firstNameText = profile.givenName ?: ""
                        if (lastNameText.isBlank()) lastNameText = profile.familyName ?: ""
                    }
                    isVerifying = false
                }
            }
        )

        // profile fields, new fields get added to this column
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledTextField(
                    label = "First name *",
                    value = firstNameText,
                    onValueChange = { firstNameText = it },
                    modifier = Modifier.weight(1f)
                )
                LabeledTextField(
                    label = "Last name *",
                    value = lastNameText,
                    onValueChange = { lastNameText = it },
                    modifier = Modifier.weight(1f)
                )
            }
            if (idToken != null) {
                Text(
                    text = "Prefilled from your Google account — edit if needed.",
                    color = SportsOnSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            BirthYearField(
                birthYear = birthYear,
                onYearSelected = { birthYear = it }
            )
        }

        if (errorText != null) {
            Text(
                text = errorText!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        ) {
            Button(
                onClick = {
                    onCreateAccount(
                        SignUpDetails(
                            idToken = idToken!!,
                            email = email,
                            firstName = firstNameText.trim(),
                            lastName = lastNameText.trim(),
                            birthYear = birthYear!!
                        )
                    )
                },
                enabled = canCreate,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = SportsPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(text = "Create Account", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Text(
                text = "By creating an account you agree to the community guidelines.",
                color = SportsOnSurfaceVariant,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Row {
                Text(
                    text = "Already have an account? ",
                    color = SportsOnSurfaceVariant,
                    fontSize = 14.sp
                )
                Text(
                    text = "Sign In",
                    color = SportsPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(onClick = onNavigateToSignIn)
                )
            }
        }
    }
}

// card showing whether the user has verified their Google account yet
@Composable
private fun GoogleVerificationCard(
    email: String?,
    isVerified: Boolean,
    isVerifying: Boolean,
    onVerify: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SportsSurfaceContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            Text(text = "G", color = SportsPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isVerified) "Verified with Google" else "Verify with Google",
                color = SportsOnSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (isVerified) email ?: "" else "Required to create your account",
                color = SportsOnSurfaceVariant,
                fontSize = 12.sp
            )
        }

        when {
            isVerified -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(SportsPrimary)
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Verified",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            isVerifying -> CircularProgressIndicator(
                color = SportsPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
            else -> TextButton(onClick = onVerify) {
                Text(text = "Verify", color = SportsPrimary, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// text field with its label above the box, as in the mockup
@Composable
private fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = sportsFieldColors(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// read only field that opens a dropdown of birth years
@Composable
private fun BirthYearField(
    birthYear: Int?,
    onYearSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentYear = Year.now().value
    val years = remember(currentYear) { (currentYear - MIN_AGE downTo currentYear - MAX_AGE).toList() }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel("Birth year *")
        Box {
            OutlinedTextField(
                value = birthYear?.toString() ?: "",
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                placeholder = { Text("Select year") },
                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                shape = RoundedCornerShape(8.dp),
                colors = sportsFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            // covers the field so a tap anywhere opens the menu
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { expanded = true }
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 280.dp)
            ) {
                years.forEach { year ->
                    DropdownMenuItem(
                        text = { Text(year.toString()) },
                        onClick = {
                            onYearSelected(year)
                            expanded = false
                        }
                    )
                }
            }
        }
        Text(
            text = "Only your age range is shown to other players.",
            color = SportsOnSurfaceVariant,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        color = SportsOnSurfaceVariant,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun sportsFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SportsPrimary,
    unfocusedBorderColor = SportsOutline,
    cursorColor = SportsPrimary
)

// name and email read from the Google id token so the form can be prefilled
private data class GoogleProfile(
    val email: String?,
    val givenName: String?,
    val familyName: String?
)

/*  the id token is a JWT, its middle part is base64 encoded JSON with the
    account's profile claims. Only used for prefilling, the backend still
    verifies the token itself
 */
private fun readGoogleProfile(idToken: String): GoogleProfile {
    return try {
        val payload = idToken.split(".")[1]
        val flags = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        val json = JSONObject(String(Base64.decode(payload, flags)))
        GoogleProfile(
            email = json.optString("email").ifBlank { null },
            givenName = json.optString("given_name").ifBlank { null },
            familyName = json.optString("family_name").ifBlank { null }
        )
    } catch (e: Exception) {
        GoogleProfile(null, null, null)
    }
}
