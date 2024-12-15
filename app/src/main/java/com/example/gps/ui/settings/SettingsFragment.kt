package com.example.gps.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.gps.LoginActivity
import com.example.gps.R
import com.example.gps.databinding.FragmentHomeBinding
import com.example.gps.databinding.FragmentSettingsBinding
import com.example.gps.ui.home.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var sharedPreferences: SharedPreferences
    private var photoUri: Uri? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            // Guardar la URI en SharedPreferences
            photoUri?.let {
                savePhotoUri(it.toString())
                loadPhoto(it)
            }
        } else {
            Toast.makeText(requireContext(), "No se tomó la foto", Toast.LENGTH_SHORT).show()
        }
    }

        @SuppressLint("CommitPrefEdits")
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            _binding = FragmentSettingsBinding.inflate(inflater, container, false)
            val root: View = binding.root

            val sharedPreferences: SharedPreferences =
                activity?.getSharedPreferences("LocationPreferences", Context.MODE_PRIVATE) ?: throw IllegalStateException("Activity is not attached")


            viewLifecycleOwner.lifecycleScope.launch {
                // Esperar a que la vista esté disponible y luego cargar la foto
                delay(100) // Pequeña espera para asegurarte de que la vista esté lista
                val savedPhotoUri = sharedPreferences.getString("photoUri", null)
                if (savedPhotoUri != null) {
                    loadPhoto(Uri.parse(savedPhotoUri))
                }
            }

            /**
            val savedPhotoUri = sharedPreferences.getString("photoUri", null)
            savedPhotoUri?.let {
                if (savedPhotoUri != null) {
                    loadPhoto(Uri.parse(savedPhotoUri))
                }
            }
            **/

            root.findViewById<Button>(R.id.btnChangePhoto).setOnClickListener {
                openCamera()
            }

            /**

            viewModel.user.observe(viewLifecycleOwner) { user ->
                root.findViewById<EditText>(R.id.etName).setText(user.name)
                root.findViewById<EditText>(R.id.etEmail).setText(user.email)
                root.findViewById<EditText>(R.id.etPhone).setText(user.phone)
            }

            **/

            root.findViewById<Button>(R.id.btnLogout).setOnClickListener {
                logout(sharedPreferences)
            }



            return binding.root
    }

    private fun logout(sharedPreferences:SharedPreferences)
    {
        Toast.makeText(requireContext(), "Cerrando sesion....", Toast.LENGTH_SHORT).show()
        sharedPreferences.edit().remove("savedCode").apply()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }
    private fun openCamera() {
        val photoFile = File.createTempFile("profile_", ".jpg", requireContext().cacheDir)
        photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            photoFile
        )
        takePictureLauncher.launch(photoUri)
    }

    private fun savePhotoUri(uri: String) {
        val sharedPreferences: SharedPreferences =
            activity?.getSharedPreferences("LocationPreferences", Context.MODE_PRIVATE) ?: throw IllegalStateException("Activity is not attached")
        sharedPreferences.edit().putString("photoUri", uri).apply()
        Toast.makeText(requireContext(), "Foto guardada exitosamente", Toast.LENGTH_SHORT).show()
    }

    private fun loadPhoto(uri: Uri) {
        view?.let {
            it.findViewById<ImageView>(R.id.imgProfile).setImageURI(uri)
        } ?: run {
            // La vista aún no está creada, puedes optar por logear un mensaje de advertencia o manejarlo de otra forma
            Toast.makeText(requireContext(), "La vista aún no está disponible para cargar la foto", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

