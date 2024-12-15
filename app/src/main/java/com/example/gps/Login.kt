package com.example.gps

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity

import kotlinx.coroutines.delay


class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)


        val inputEditText = findViewById<EditText>(R.id.inputEditText)
        val saveButton = findViewById<Button>(R.id.saveButton)

        val sharedPreferences =
            this.getSharedPreferences("LocationPreferences", Context.MODE_PRIVATE)

        saveButton.setOnClickListener {
            val inputValue = inputEditText.text.toString()

            if (inputValue.isNotEmpty()) {
                sharedPreferences.edit().putString("savedCode", inputValue).apply()
                navigateToMainScreen(inputValue)
            } else {
                Toast.makeText(this, "Por favor ingrese un número", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToMainScreen(code: String) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
        return
    }

}


