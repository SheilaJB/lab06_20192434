package com.example.telemoney;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.telemoney.databinding.ActivityLoginBinding;
import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.List;

public class LoginActivity extends AppCompatActivity {
    ActivityLoginBinding binding;
    private final static String TAG = "msg-test";
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Habilitar firebase authentication
        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseAuth.getInstance().setLanguageCode("es");

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            if (currentUser.isEmailVerified()) {
                Log.d(TAG, "Usuario ya autenticado: " + currentUser.getUid());
                goToMainActivity();
            } else {
                Log.d(TAG, "Usuario no verificado, debe verificar email");
                Toast.makeText(this, "Por favor verifica tu email antes de continuar", Toast.LENGTH_LONG).show();
            }
        }
        configurarBotones();
    }
    private void configurarBotones() {
        binding.LoginButton.setOnClickListener(view -> iniciarAutenticacionFirebaseUI());

        binding.alreadyAccountButton.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, IngresoActivity.class);
            startActivity(intent);
        });
    }
    private void iniciarAutenticacionFirebaseUI() {
        Log.d(TAG, "Iniciando autenticación con FirebaseUI");
        habilitarBotones(false);

        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build()
        );
        AuthMethodPickerLayout customLayout = new AuthMethodPickerLayout.Builder(R.layout.activity_main)
                .setGoogleButtonId(R.id.googleLoginButton)
                .setEmailButtonId(R.id.emailLoginButton)
                .build();

        Intent intent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAuthMethodPickerLayout(customLayout)
                .setTheme(R.style.Theme_TeleMoney)
                .setLogo(R.drawable.logo)
                .setIsSmartLockEnabled(false)
                .setAvailableProviders(providers)
                .setTosAndPrivacyPolicyUrls(
                        "https://example.com/terms.html",
                        "https://example.com/privacy.html"
                )
                .build();

        signInLauncher.launch(intent);

    }

    private void habilitarBotones(boolean habilitar) {
        binding.LoginButton.setEnabled(habilitar);

        if (habilitar) {
            binding.LoginButton.setText("Iniciar con correo");
        } else {
            binding.LoginButton.setText("Procesando...");
        }
    }
    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            result -> {
                // Rehabilitar botones
                habilitarBotones(true);

                if (result.getResultCode() == RESULT_OK) {
                    // Autenticación exitosa
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {

                        Log.d(TAG, "=== AUTENTICACIÓN EXITOSA ===");
                        Log.d(TAG, "Firebase uid: " + user.getUid());
                        Log.d(TAG, "Display name: " + user.getDisplayName());
                        Log.d(TAG, "Email: " + user.getEmail());

                        // Verificar si es usuario nuevo
                        IdpResponse response = result.getIdpResponse();
                        boolean isNewUser = response != null && response.isNewUser();

                        if (isNewUser) {
                            Log.d(TAG, "¡Usuario nuevo registrado automáticamente!");
                            Toast.makeText(this, "¡Bienvenido! Tu cuenta ha sido creada", Toast.LENGTH_LONG).show();
                        } else {
                            Log.d(TAG, "Usuario existente - Sesión iniciada");
                            Toast.makeText(this, "¡Bienvenido de vuelta, " + user.getDisplayName() + "!", Toast.LENGTH_SHORT).show();
                        }

                        // Recargar información del usuario y verificar email
                        user.reload().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                if (user.isEmailVerified()) {
                                    Log.d(TAG, "Email verificado - Accediendo a la aplicación");
                                    goToMainActivity();
                                } else {
                                    Log.d(TAG, "Email no verificado - Enviando correo de verificación");
                                    enviarVerificacionEmail(user);
                                }
                            } else {
                                Log.e(TAG, "Error al recargar usuario", task.getException());
                                // Continuar de todas formas
                                goToMainActivity();
                            }
                        });

                    } else {
                        Log.e(TAG, "Error: usuario nulo después de autenticación exitosa");
                        Toast.makeText(this, "Error inesperado. Intenta de nuevo.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    // Error o cancelación
                    IdpResponse response = result.getIdpResponse();
                    if (response == null) {
                        Log.d(TAG, "Autenticación cancelada por el usuario");
                        Toast.makeText(this, "Autenticación cancelada", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(TAG, "Error en autenticación: " + response.getError());
                        Toast.makeText(this, "Error en la autenticación. Intenta de nuevo.", Toast.LENGTH_LONG).show();
                    }
                }
            }
    );

    private void enviarVerificacionEmail(FirebaseUser user) {
        user.sendEmailVerification().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Correo de verificación enviado exitosamente");

                new AlertDialog.Builder(this)
                        .setTitle("Verificación de email")
                        .setMessage("Se ha enviado un correo de verificación a " + user.getEmail() +
                                ".\n\nPor favor verifica tu email para mayor seguridad.")
                        .setPositiveButton("Entendido", (dialog, which) -> {
                            // Permitir continuar sin verificar (opcional)
                            goToMainActivity();
                        })
                        .setCancelable(false)
                        .show();

            } else {
                Log.e(TAG, "Error enviando correo de verificación", task.getException());
                Toast.makeText(this, "Error enviando correo de verificación", Toast.LENGTH_LONG).show();
                // Continuar de todas formas
                goToMainActivity();
            }
        });
    }

    public void goToMainActivity() {
        Log.d(TAG, "Navegando a la aplicación principal");
        Intent intent = new Intent(LoginActivity.this, IngresoActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}