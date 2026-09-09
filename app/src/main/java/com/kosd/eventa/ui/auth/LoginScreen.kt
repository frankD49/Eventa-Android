package com.kosd.eventa.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.kosd.eventa.ui.theme.AuthTeal
import com.kosd.eventa.ui.theme.AuthTealLight
import com.kosd.eventa.ui.theme.EventaLogo
import com.kosd.eventa.ui.theme.FloatingShapes
import com.kosd.eventa.ui.theme.GlassAuthSheet
import com.kosd.eventa.ui.theme.OnAuthTeal
import com.kosd.eventa.ui.theme.rememberHaptics
import com.kosd.eventa.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    orgViewModel: com.kosd.eventa.viewmodel.OrganizationViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit = {}
) {
    var email          by remember { mutableStateOf("") }
    var password       by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager   = androidx.compose.ui.platform.LocalFocusManager.current
    val haptics        = rememberHaptics()

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val act = context as? androidx.fragment.app.FragmentActivity
        if (act != null && viewModel.activity == null) {
            viewModel.activity = act
        }
        if (viewModel.emailConfirmed && viewModel.pendingEmail != null) {
            email = viewModel.pendingEmail!!
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AuthTealLight, AuthTeal)))
    ) {
        FloatingShapes(primaryColor = OnAuthTeal, tertiaryColor = MaterialTheme.colorScheme.tertiary)

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))

            EventaLogo(size = 72.dp)

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Eventa",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = OnAuthTeal
            )

            Spacer(Modifier.weight(1f))

            GlassAuthSheet {
                Box(
                    modifier = Modifier
                        .width(40.dp).height(4.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.height(16.dp))

                if (viewModel.requiresBiometric) {
                    Text(
                        "Authentication Required",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Use your biometric or device credential to access Eventa",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (viewModel.biometricError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(viewModel.biometricError!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }

                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { haptics.tap(); viewModel.verifyBiometric() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = MaterialTheme.shapes.small,
                        enabled = !viewModel.isLoading
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Authenticate", fontWeight = FontWeight.SemiBold)
                    }

                    TextButton(onClick = { haptics.tap(); viewModel.cancelBiometric() }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Text(
                        "Sign In",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (email.isNotBlank() && password.isNotBlank()) {
                                    haptics.tap(); viewModel.login(email, password, orgViewModel)
                                }
                            }
                        ),
                        trailingIcon = {
                            IconButton(onClick = { haptics.tap(); passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small
                    )

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = { haptics.tap(); viewModel.login(email, password, orgViewModel) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = MaterialTheme.shapes.small,
                        enabled = email.isNotBlank() && password.isNotBlank() && !viewModel.isLoading
                    ) {
                        if (viewModel.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Text("Sign In", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    TextButton(
                        onClick = { haptics.tap(); onNavigateToForgotPassword },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text(text = "Forgot Password?", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Don't have an account?",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                        TextButton(
                            onClick = { haptics.tap(); onNavigateToRegister },
                            modifier = Modifier.defaultMinSize(minWidth = 0.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(text = "Register", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (viewModel.showError) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = { Text("Sign In Failed") },
            text  = { Text(viewModel.errorMessage ?: "An error occurred") },
            confirmButton = { TextButton(onClick = { viewModel.dismissError() }) { Text("OK") } }
        )
    }

    if (viewModel.showSuccess) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSuccess() },
            title = { Text("Success") },
            text  = { Text(viewModel.successMessage ?: "") },
            confirmButton = { TextButton(onClick = { viewModel.dismissSuccess() }) { Text("OK") } }
        )
    }
}
