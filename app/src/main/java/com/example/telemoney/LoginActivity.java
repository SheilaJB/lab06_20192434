package com.example.telemoney;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.example.telemoney.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null) { // Usuario ya autenticado
            if (currentUser.isEmailVerified()) {
                Log.d("LoginActivity", "Usuario autenticado:" + currentUser.getUid());
                goToMainActivity();
            }
        }

        // Configurar los listeners de los botones


        // Boton de inicio con correo electrónico
        binding.emailLoginButton.setOnClickListener(view -> loginEmail());
        // Boton de inicio con Google
        binding.googleLoginButton.setOnClickListener(view -> loginGoogle());
        binding.alreadyAccountButton.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, IngresoActivity.class); // Testeando IngresoActivity
            startActivity(intent);

        });



    }

    private void loginEmail() {
        // Aquí iría la lógica para iniciar sesión con correo electrónico
        Log.d(TAG, "Iniciar sesión con correo electrónico");
        // Por ejemplo, podrías abrir una nueva actividad para ingresar el correo y la contraseña
    }

    private void loginGoogle() {
        // Aquí iría la lógica para iniciar sesión con Google
        Log.d(TAG, "Iniciar sesión con Google");
        // Por ejemplo, podrías abrir una nueva actividad para manejar la autenticación de Google
    }



    public void goToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, IngresoActivity.class);
        startActivity(intent);
        finish();
    }
}